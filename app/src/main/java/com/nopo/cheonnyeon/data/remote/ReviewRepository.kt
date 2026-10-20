package com.nopo.cheonnyeon.data.remote

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.Timestamp
import com.google.firebase.firestore.AggregateField
import com.google.firebase.firestore.AggregateSource
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.nopo.cheonnyeon.core.common.AppError
import com.nopo.cheonnyeon.core.common.AppResult
import com.nopo.cheonnyeon.core.common.asFailure
import com.nopo.cheonnyeon.core.common.asSuccess
import com.nopo.cheonnyeon.domain.rule.ReviewCheck
import com.nopo.cheonnyeon.domain.rule.ReviewRules
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout

data class ReviewItem(
    val id: String,
    val storeId: String,
    val storeName: String,
    val uid: String,
    val nickname: String,
    val titleId: String,
    val rating: Int,
    val text: String,
    val visitCount: Int,
    val updatedAtMillis: Long,
    /** 시연용 예시 리뷰(서버에 없음) */
    val demo: Boolean = false,
    /** 리뷰 사진(최대 [ReviewRules.MAX_PHOTOS]장): 실제 리뷰는 줄인 base64 WebP, 시연용은 "asset:" 경로 */
    val photos: List<String> = emptyList(),
)

data class ReviewSummary(val count: Int, val average: Double?)

/** 한 번에 받은 리뷰와 다음 페이지를 이어 받을 위치 */
class ReviewPage(val items: List<ReviewItem>, internal val last: DocumentSnapshot?, val hasMore: Boolean)

/**
 * 평점·리뷰 (S-801, PRD 6.4). 온라인에서만 쓰고 지운다. 오프라인이면 [AppError.E_REVIEW_OFFLINE].
 * 서버 규칙이 방문 문서가 있을 때만 쓰기를 허용한다.
 */
class ReviewRepository(
    private val db: () -> FirebaseFirestore,
    private val uid: () -> String?,
) {
    private fun reviews() = db().collection("reviews")

    suspend fun summary(storeId: String): AppResult<ReviewSummary> = call {
        val snap = reviews().whereEqualTo("storeId", storeId)
            .aggregate(AggregateField.count(), AggregateField.average("rating")).get(AggregateSource.SERVER).await()
        val count = snap.get(AggregateField.count()).toInt()
        ReviewSummary(count, if (count == 0) null else snap.get(AggregateField.average("rating")))
    }

    suspend fun page(storeId: String, after: ReviewPage? = null): AppResult<ReviewPage> = call {
        var q: Query = reviews().whereEqualTo("storeId", storeId).orderBy("updatedAt", Query.Direction.DESCENDING).limit(ReviewRules.PAGE_SIZE.toLong())
        after?.last?.let { q = q.startAfter(it) }
        val snap = q.get().await()
        ReviewPage(snap.documents.map(::toItem), snap.documents.lastOrNull(), snap.size() == ReviewRules.PAGE_SIZE)
    }

    /** 한 사용자가 남긴 리뷰 최신순 (프로필·내 리뷰 탭) */
    suspend fun byUser(uid: String, limit: Long = 60): AppResult<List<ReviewItem>> = call {
        reviews().whereEqualTo("uid", uid).orderBy("updatedAt", Query.Direction.DESCENDING).limit(limit).get().await().documents.map(::toItem)
    }

    /** 이 가게에 내가 남긴 리뷰 전부(최신순). 한 가게에 여러 번 쓸 수 있다 */
    suspend fun mine(storeId: String): AppResult<List<ReviewItem>> = call {
        val me = uid() ?: throw NotSignedIn()
        reviews().whereEqualTo("storeId", storeId).whereEqualTo("uid", me).get().await().documents.map(::toItem).sortedByDescending { it.updatedAtMillis }
    }

    /**
     * [reviewId]가 null이면 새 리뷰를 쌓고(문서 ID `가게_내uid_시각`), 있으면 그 리뷰를 고친다.
     * 예전 형식(`가게_내uid`) 리뷰도 그 ID로 고칠 수 있다.
     */
    suspend fun save(storeId: String, storeName: String, rating: Int, text: String, visitCount: Int, titleId: String, reviewId: String? = null, photos: List<String> = emptyList()): AppResult<Unit> {
        val check = ReviewRules.validate(rating, text)
        if (check !is ReviewCheck.Valid) return AppError.E_NETWORK.asFailure() // 화면이 먼저 걸러서 여기엔 오지 않는다
        return call {
            val me = uid() ?: throw NotSignedIn()
            val firestore = db()
            val userRef = firestore.collection("users").document(me)
            val ref = reviews().document(reviewId ?: "${storeId}_${me}_${System.currentTimeMillis()}")
            // 트랜잭션은 오프라인에서 바로 실패한다. 배치는 나중에 서버에서 거부돼 리뷰가 사라질 수 있어 쓰지 않는다 (D-12)
            firestore.runTransaction { tx ->
                val nickname = tx.get(userRef).getString("nickname") ?: throw NotSignedIn()
                if (reviewId != null) {
                    tx.update(ref, mapOf("rating" to check.rating, "text" to check.text, "nickname" to nickname, "titleId" to titleId, "visitCount" to visitCount, "photos" to (photos.takeIf { it.isNotEmpty() } ?: FieldValue.delete()), "photo" to FieldValue.delete(), "updatedAt" to FieldValue.serverTimestamp()))
                } else {
                    tx.set(
                        ref,
                        mapOf(
                            "storeId" to storeId, "storeName" to storeName, "uid" to me, "nickname" to nickname, "titleId" to titleId,
                            "rating" to check.rating, "text" to check.text, "visitCount" to visitCount,
                            "createdAt" to FieldValue.serverTimestamp(), "updatedAt" to FieldValue.serverTimestamp(),
                        ) + (if (photos.isNotEmpty()) mapOf("photos" to photos) else emptyMap()),
                    )
                    tx.update(userRef, mapOf("reviewCount" to FieldValue.increment(1), "updatedAt" to FieldValue.serverTimestamp()))
                }
                null
            }.await()
            Unit
        }
    }

    /** 리뷰 신고 (S-805, P1). 신고만 쌓고 처리는 운영자가 한다 */
    suspend fun report(reviewId: String, reason: String): AppResult<Unit> = call {
        val me = uid() ?: throw NotSignedIn()
        db().collection("reports").add(mapOf("reviewId" to reviewId, "reason" to reason, "reporterUid" to me, "createdAt" to FieldValue.serverTimestamp())).await()
        Unit
    }

    /** 가게 정보 오류 제보 (FR-STORE-06, P1). 사유만 보내고 운영자가 확인한다 */
    suspend fun reportStoreInfo(storeId: String, reason: String): AppResult<Unit> = call {
        val me = uid() ?: throw NotSignedIn()
        db().collection("reports").add(
            mapOf("kind" to "STORE_INFO", "storeId" to storeId, "reason" to reason, "reporterUid" to me, "createdAt" to FieldValue.serverTimestamp()),
        ).await()
        Unit
    }

    /** 내 리뷰 하나 지우기. 내 리뷰가 있을 때만 지우고 개수를 줄인다 */
    suspend fun delete(reviewId: String): AppResult<Unit> = call {
        val me = uid() ?: throw NotSignedIn()
        val firestore = db()
        val ref = reviews().document(reviewId)
        val userRef = firestore.collection("users").document(me)
        firestore.runTransaction { tx ->
            // 내 리뷰가 있을 때만 지우고 개수를 줄인다 (개수가 음수로 내려가지 않게)
            if (tx.get(ref).exists()) {
                tx.delete(ref)
                tx.update(userRef, mapOf("reviewCount" to FieldValue.increment(-1), "updatedAt" to FieldValue.serverTimestamp()))
            }
            null
        }.await()
        Unit
    }

    private fun toItem(d: DocumentSnapshot) = ReviewItem(
        id = d.id,
        storeId = d.getString("storeId").orEmpty(),
        storeName = d.getString("storeName").orEmpty(),
        uid = d.getString("uid").orEmpty(),
        nickname = d.getString("nickname").orEmpty(),
        titleId = d.getString("titleId").orEmpty(),
        rating = (d.getLong("rating") ?: 0L).toInt(),
        text = d.getString("text").orEmpty(),
        visitCount = (d.getLong("visitCount") ?: 1L).toInt(),
        updatedAtMillis = (d.getTimestamp("updatedAt") ?: Timestamp.now()).toDate().time,
        photos = ((d.get("photos") as? List<*>)?.filterIsInstance<String>() ?: listOfNotNull(d.getString("photo"))).filter { it.isNotBlank() }.take(ReviewRules.MAX_PHOTOS),
    )

    private class NotSignedIn : Exception()

    private suspend fun <T> call(block: suspend () -> T): AppResult<T> = try {
        withTimeout(TIMEOUT_MS) { block().asSuccess() }
    } catch (e: CancellationException) {
        if (e is TimeoutCancellationException) AppError.E_REVIEW_OFFLINE.asFailure() else throw e
    } catch (_: NotSignedIn) {
        AppError.E_AUTH_REQUIRED.asFailure()
    } catch (e: FirebaseFirestoreException) {
        android.util.Log.e("Nopo", "리뷰 서버 호출 실패: " + e.code + " " + e.message)
        when (e.code) {
            FirebaseFirestoreException.Code.UNAVAILABLE, FirebaseFirestoreException.Code.DEADLINE_EXCEEDED -> AppError.E_REVIEW_OFFLINE
            FirebaseFirestoreException.Code.PERMISSION_DENIED -> AppError.E_VISIT_SYNCING
            else -> AppError.E_NETWORK
        }.asFailure()
    } catch (_: FirebaseNetworkException) {
        AppError.E_REVIEW_OFFLINE.asFailure()
    } catch (e: Exception) {
        android.util.Log.e("Nopo", "리뷰 호출 예외: " + e)
        AppError.E_NETWORK.asFailure()
    }

    private companion object {
        const val TIMEOUT_MS = 12_000L
    }
}
