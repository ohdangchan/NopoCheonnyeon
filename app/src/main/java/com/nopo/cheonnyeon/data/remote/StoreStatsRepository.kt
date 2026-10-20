package com.nopo.cheonnyeon.data.remote

import com.google.firebase.firestore.FirebaseFirestore
import com.nopo.cheonnyeon.core.common.AppError
import com.nopo.cheonnyeon.core.common.AppResult
import com.nopo.cheonnyeon.core.common.asFailure
import com.nopo.cheonnyeon.core.common.asSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout

data class StoreStats(val visitors: Int, val revisitors: Int)

/** 가게별 방문자·재방문자 수 읽기 (FR-STORE-07). 쓰기는 방문 기록 전송(VisitRepository)에서 한다. */
class StoreStatsRepository(private val db: () -> FirebaseFirestore) {
    /** 문서가 없으면 0명 */
    suspend fun get(storeId: String): AppResult<StoreStats> = try {
        withTimeout(TIMEOUT_MS) {
            val d = db().collection("storeStats").document(storeId).get().await()
            StoreStats((d.getLong("visitors") ?: 0L).toInt(), (d.getLong("revisitors") ?: 0L).toInt()).asSuccess()
        }
    } catch (e: CancellationException) {
        if (e is TimeoutCancellationException) AppError.E_NETWORK.asFailure() else throw e
    } catch (_: Exception) {
        AppError.E_NETWORK.asFailure()
    }

    private companion object {
        const val TIMEOUT_MS = 12_000L
    }
}
