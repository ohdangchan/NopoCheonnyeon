package com.nopo.cheonnyeon.data.remote

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.nopo.cheonnyeon.core.common.AppError
import com.nopo.cheonnyeon.core.common.AppResult
import com.nopo.cheonnyeon.core.common.asFailure
import com.nopo.cheonnyeon.core.common.asSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout

data class UserProfile(
    val uid: String,
    val nickname: String,
    val titleId: String,
    val totalYears: Int,
    val visitCount: Int,
    val reviewCount: Int,
    val createdAtMillis: Long? = null,
    /** 방문 기록 공개 범위: ALL(기본) · COUNT(방문 횟수만) · PRIVATE */
    val visibility: String = "ALL",
)

data class VisitedStore(
    val storeId: String,
    val storeName: String,
    val sigungu: String,
    val yearsAwarded: Int,
    val visitCount: Int,
    val firstVisitedMillis: Long? = null
)

/** 다른 사용자의 프로필 조회 (S-804): 칭호·세월·다녀온 노포·남긴 리뷰. 읽기 전용. */
class ProfileRepository(private val db: () -> FirebaseFirestore, private val reviews: ReviewRepository) {

    /** 없는 사용자면 성공(null) */
    suspend fun profile(uid: String): AppResult<UserProfile?> = call {
        val d = db().collection("users").document(uid).get().await()
        if (!d.exists()) null else UserProfile(
            uid = uid,
            nickname = d.getString("nickname").orEmpty(),
            titleId = d.getString("titleId").orEmpty(),
            totalYears = (d.getLong("totalYears") ?: 0L).toInt(),
            visitCount = (d.getLong("visitCount") ?: 0L).toInt(),
            reviewCount = (d.getLong("reviewCount") ?: 0L).toInt(),
            createdAtMillis = d.getTimestamp("createdAt")?.toDate()?.time,
            visibility = d.getString("visitsVisibility") ?: "ALL",
        )
    }

    suspend fun visits(uid: String, limit: Long = 60): AppResult<List<VisitedStore>> = call {
        db().collection("users")
            .document(uid)
            .collection("visits")
            .orderBy("lastVisitedAt", Query.Direction.DESCENDING)
            .limit(limit)
            .get()
            .await()
            .documents.map {
                VisitedStore(
                    storeId = it.id, storeName = it.getString("storeName").orEmpty(), sigungu = it.getString("sigungu").orEmpty(),
                    yearsAwarded = (it.getLong("yearsAwarded") ?: 0L).toInt(), visitCount = (it.getLong("visitCount") ?: 1L).toInt(),
                    firstVisitedMillis = it.getTimestamp("firstVisitedAt")?.toDate()?.time,
                )
            }
    }

    suspend fun reviewsOf(uid: String) = reviews.byUser(uid)

    /** 내 방문 기록 공개 범위 저장 (FR-PRF-07) */
    suspend fun setVisibility(uid: String, visibility: String): AppResult<Unit> = call {
        db().collection("users").document(uid)
            .update(mapOf(
                "visitsVisibility" to visibility,
                "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
            )).await()
        Unit
    }

    private suspend fun <T> call(block: suspend () -> T): AppResult<T> = try {
        withTimeout(12_000L) { block().asSuccess() }
    } catch (e: CancellationException) {
        if (e is TimeoutCancellationException) AppError.E_NETWORK.asFailure() else throw e
    } catch (e: FirebaseFirestoreException) {
        android.util.Log.e("Nopo", "프로필 서버 호출 실패: " + e.code)
        AppError.E_NETWORK.asFailure()
    } catch (_: Exception) {
        AppError.E_NETWORK.asFailure()
    }
}
