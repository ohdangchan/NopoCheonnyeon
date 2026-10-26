package com.nopo.cheonnyeon.data.remote

import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.nopo.cheonnyeon.core.common.AppError
import com.nopo.cheonnyeon.core.common.AppResult
import com.nopo.cheonnyeon.core.common.asFailure
import com.nopo.cheonnyeon.core.common.asSuccess
import com.nopo.cheonnyeon.domain.model.SessionState
import com.nopo.cheonnyeon.domain.rule.AuthValidation
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout

/**
 * 가입·로그인·닉네임 (FR-AUTH, 아키텍처 3·9장). 서버 호출은 15초 제한을 두고, 예외는 [AppError]로 바꿔 돌려준다.
 * 이메일·비밀번호는 기록하지 않는다.
 */
class AuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
) {
    private val profileRefresh = MutableStateFlow(0)

    private val authUid: Flow<String?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { trySend(it.currentUser?.uid) }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }.distinctUntilChanged()

    /** 로그인 게이트 상태: 로그인했지만 users/{uid}가 없으면 NeedsNickname (FR-AUTH-07) */
    val sessionState: Flow<SessionState> = combine(authUid, profileRefresh) { uid, _ -> uid }.mapLatest { uid ->
        when {
            uid == null -> SessionState.SignedOut
            hasProfile(uid) -> SessionState.SignedIn
            else -> SessionState.NeedsNickname
        }
    }

    /** 확인할 수 없으면(오프라인 등) 막지 않고 있는 것으로 본다 */
    private suspend fun hasProfile(uid: String): Boolean = try {
        withTimeout(PROFILE_TIMEOUT_MS) { userDoc(uid).get().await().exists() }
    } catch (e: CancellationException) {
        if (e is TimeoutCancellationException) true else throw e
    } catch (_: Exception) {
        true
    }

    val currentUid: String? get() = auth.currentUser?.uid

    private fun userDoc(uid: String): DocumentReference = db.collection(USERS).document(uid)

    private fun nicknameDoc(nickname: String): DocumentReference = db.collection(NICKNAMES).document(AuthValidation.nicknameKey(nickname))

    /** true = 사용 가능. 네트워크 오류면 null (가입 시도 때 다시 확인된다) */
    suspend fun isNicknameAvailable(nick: String): Boolean? = try {
        withTimeout(TIMEOUT_MS) { !nicknameDoc(nick).get().await().exists() }
    } catch (e: CancellationException) {
        if (e is TimeoutCancellationException) null else throw e
    } catch (_: Exception) {
        null
    }

    suspend fun signUp(email: String, password: String, nickname: String): AppResult<Unit> = call {
        if (isNicknameAvailable(nickname) == false) return@call AppError.AUTH_NICK_TAKEN.asFailure()
        auth.createUserWithEmailAndPassword(email.trim(), password).await()
        // 프로필 쓰기가 실패하면 로그인은 된 채로 NeedsNickname 화면에서 복구한다
        createProfile(nickname)
    }

    /** users/{uid}와 nicknames/{닉네임}을 한 묶음으로 만든다 (가입·복구 공용) */
    suspend fun createProfile(nickname: String): AppResult<Unit> = call {
        val uid = auth.currentUser?.uid ?: return@call AppError.E_AUTH_REQUIRED.asFailure()
        if (isNicknameAvailable(nickname) == false) return@call AppError.AUTH_NICK_TAKEN.asFailure()
        val batch = db.batch()
        batch.set(nicknameDoc(nickname), mapOf("uid" to uid))
        batch.set(
            userDoc(uid),
            mapOf(
                "nickname" to nickname,
                "nicknameLower" to AuthValidation.nicknameKey(nickname),
                "titleId" to "SPROUT",
                "totalYears" to 0,
                "visitCount" to 0,
                "reviewCount" to 0,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp(),
            ),
        )
        batch.commit().await()
        profileRefresh.value += 1
        Unit.asSuccess()
    }

    suspend fun signIn(email: String, password: String): AppResult<Unit> = call {
        auth.signInWithEmailAndPassword(email.trim(), password).await()
        Unit.asSuccess()
    }

    /** 계정이 없어도 성공으로 돌려준다 (계정 존재 여부 노출 금지, FR-AUTH-04). 네트워크 오류만 알린다. */
    suspend fun sendPasswordReset(email: String): AppResult<Unit> {
        val r = call { auth.sendPasswordResetEmail(email.trim()).await(); Unit.asSuccess() }
        return if (r.errorOrNull == AppError.E_NETWORK) r else Unit.asSuccess()
    }

    /** 내 닉네임 (프로필 문서). 못 읽으면 null */
    suspend fun currentNickname(): String? = try {
        val me = auth.currentUser?.uid ?: return null
        withTimeout(TIMEOUT_MS) { userDoc(me).get().await().getString("nickname") }
    } catch (e: CancellationException) {
        if (e is TimeoutCancellationException) null else throw e
    } catch (_: Exception) {
        null
    }

    /** 구글 ID 토큰으로 로그인한다. 처음 쓰는 계정이면 프로필이 없어 세션 상태가 NeedsNickname 이 되고, 닉네임 화면으로 이어진다. */
    suspend fun signInWithGoogle(idToken: String): AppResult<Unit> = call {
        auth.signInWithCredential(GoogleAuthProvider.getCredential(idToken, null)).await()
        profileRefresh.value++
        Unit.asSuccess()
    }

    /** 닉네임 변경 (S-408): 새 닉네임 확보 → 옛 닉네임 해제 → 내 리뷰의 닉네임 일괄 갱신, 한 묶음으로 */
    suspend fun changeNickname(newNick: String): AppResult<Unit> = call {
        val me = auth.currentUser?.uid ?: return@call AppError.E_AUTH_REQUIRED.asFailure()
        val old = userDoc(me).get().await().getString("nickname").orEmpty()
        val keyChanged = AuthValidation.nicknameKey(old) != AuthValidation.nicknameKey(newNick)
        if (keyChanged && isNicknameAvailable(newNick) == false) {
            return@call AppError.AUTH_NICK_TAKEN.asFailure()
        }
        val batch = db.batch()
        if (keyChanged) {
            if (old.isNotEmpty()) batch.delete(nicknameDoc(old))
            batch.set(nicknameDoc(newNick), mapOf("uid" to me))
        }
        batch.update(
            userDoc(me),
            mapOf(
                "nickname" to newNick,
                "nicknameLower" to AuthValidation.nicknameKey(newNick),
                "updatedAt" to FieldValue.serverTimestamp()
            ),
        )
        db.collection(REVIEWS).whereEqualTo("uid", me).get().await().documents.forEach { d ->
            batch.update(d.reference, mapOf("nickname" to newNick))
        }
        batch.commit().await()
        Unit.asSuccess()
    }

    /** 회원 탈퇴 (S-408): 비밀번호로 다시 인증 → 내 리뷰·방문·프로필·닉네임 삭제 → 계정 삭제 */
    suspend fun deleteAccount(password: String): AppResult<Unit> = call {
        val user = auth.currentUser ?: return@call AppError.E_AUTH_REQUIRED.asFailure()
        val email = user.email ?: return@call AppError.E_AUTH_REQUIRED.asFailure()
        user.reauthenticate(EmailAuthProvider.getCredential(email, password)).await()
        val me = user.uid
        val nick = userDoc(me).get().await().getString("nickname")
        val batch = db.batch()
        db.collection(REVIEWS).whereEqualTo("uid", me).get().await().documents.forEach { batch.delete(it.reference) }
        userDoc(me).collection("visits").get().await().documents.forEach { batch.delete(it.reference) }
        nick?.let { batch.delete(nicknameDoc(it)) }
        batch.delete(userDoc(me))
        batch.commit().await()
        user.delete().await()
        Unit.asSuccess()
    }

    fun signOut() = auth.signOut()

    private suspend fun call(block: suspend () -> AppResult<Unit>): AppResult<Unit> = try {
        withTimeout(TIMEOUT_MS) { block() }
    } catch (e: CancellationException) {
        if (e is TimeoutCancellationException) AppError.E_NETWORK.asFailure() else throw e
    } catch (e: Exception) {
        toError(e).asFailure()
    }

    /** 인증 예외를 사용자에게 보여 줄 오류로 바꾼다. 네트워크 오류와 그 밖의 예외는 모두 [AppError.E_NETWORK] */
    private fun toError(e: Exception): AppError = when (e) {
        is FirebaseAuthUserCollisionException -> AppError.AUTH_EMAIL_IN_USE
        is FirebaseAuthWeakPasswordException -> AppError.AUTH_WEAK_PASSWORD
        is FirebaseAuthInvalidCredentialsException, is FirebaseAuthInvalidUserException -> AppError.AUTH_WRONG_CREDENTIAL
        is FirebaseTooManyRequestsException -> AppError.AUTH_TOO_MANY
        else -> AppError.E_NETWORK
    }

    private companion object {
        const val USERS = "users"
        const val NICKNAMES = "nicknames"
        const val REVIEWS = "reviews"
        const val TIMEOUT_MS = 15_000L

        /** 로그인 직후 프로필 확인은 짧게: 늦으면 있는 것으로 보고 홈으로 보낸다 */
        const val PROFILE_TIMEOUT_MS = 5_000L
    }
}
