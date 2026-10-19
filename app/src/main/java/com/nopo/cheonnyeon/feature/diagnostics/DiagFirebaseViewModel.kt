package com.nopo.cheonnyeon.feature.diagnostics

import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import kotlin.time.TimeSource

/** 진단 화면 Firebase 탭 결과 한 줄. 화면이 문구로 바꾼다. */
data class DiagLogLine(val step: DiagStep, val ok: Boolean, val millis: Long, val detail: String?)

enum class DiagStep { SIGN_UP, SIGN_IN, SIGN_OUT, WRITE, READ }

data class DiagFirebaseUiState(
    val signedInUid: String? = null,
    val running: DiagStep? = null,
    val log: List<DiagLogLine> = emptyList(),
)

/**
 * SPK-5: 가입·로그인·diagnostics/{uid} 쓰기·서버 읽기 왕복 시간을 잰다.
 * 서버 호출은 15초 제한(아키텍처 9장). 예외는 UI로 던지지 않고 결과 줄로 바꾼다.
 * 이메일·비밀번호는 기록하지 않는다.
 */
class DiagFirebaseViewModel(
    private val auth: FirebaseAuth,
    private val db: FirebaseFirestore,
) : ViewModel() {

    private val _state = MutableStateFlow(DiagFirebaseUiState(signedInUid = auth.currentUser?.uid))
    val state: StateFlow<DiagFirebaseUiState> = _state.asStateFlow()

    fun signUp(email: String, password: String) = run(DiagStep.SIGN_UP) {
        auth.createUserWithEmailAndPassword(email.trim(), password).await()
        null
    }

    fun signIn(email: String, password: String) = run(DiagStep.SIGN_IN) {
        auth.signInWithEmailAndPassword(email.trim(), password).await()
        null
    }

    fun signOut() = run(DiagStep.SIGN_OUT) {
        auth.signOut()
        null
    }

    /** 쓰기 → 서버에서 다시 읽기. 오프라인이면 쓰기는 기기에 남고 제한 시간 뒤 실패로 표시된다. */
    fun writeAndRead() {
        val uid = auth.currentUser?.uid ?: return
        val doc = db.collection("diagnostics").document(uid)
        run(DiagStep.WRITE) {
            doc.set(mapOf("model" to Build.MODEL, "sdk" to Build.VERSION.SDK_INT, "at" to FieldValue.serverTimestamp())).await()
            null
        }.invokeOnCompletion {
            run(DiagStep.READ) {
                val snap = doc.get(Source.SERVER).await()
                if (snap.exists()) snap.getString("model") else error("NOT_FOUND")
            }
        }
    }

    private fun run(step: DiagStep, block: suspend () -> String?) = viewModelScope.launch {
        _state.update { it.copy(running = step) }
        val mark = TimeSource.Monotonic.markNow()
        val line = try {
            val detail = withTimeout(TIMEOUT_MS) { block() }
            DiagLogLine(step, ok = true, millis = mark.elapsedNow().inWholeMilliseconds, detail = detail)
        } catch (e: TimeoutCancellationException) {
            DiagLogLine(step, ok = false, millis = mark.elapsedNow().inWholeMilliseconds, detail = "TIMEOUT")
        } catch (e: Exception) {
            // 진단용: 예외 종류만 표시한다 (메시지에 이메일이 섞일 수 있어 넣지 않음)
            DiagLogLine(step, ok = false, millis = mark.elapsedNow().inWholeMilliseconds, detail = e.javaClass.simpleName)
        }
        _state.update {
            it.copy(running = null, signedInUid = auth.currentUser?.uid, log = (listOf(line) + it.log).take(MAX_LOG))
        }
    }

    private companion object {
        const val TIMEOUT_MS = 15_000L
        const val MAX_LOG = 20
    }
}
