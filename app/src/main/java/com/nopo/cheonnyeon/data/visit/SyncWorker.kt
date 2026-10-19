package com.nopo.cheonnyeon.data.visit

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.CoroutineWorker
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.nopo.cheonnyeon.NopoApp
import java.util.concurrent.TimeUnit

/** 연결되면 대기열을 서버로 보낸다. 남은 게 있으면 WorkManager가 나중에 다시 부른다 (ADR-09). */
class SyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val container = (applicationContext as NopoApp).container
        return if (container.visits.sync() == 0) Result.success() else Result.retry()
    }
}

/** 동기화는 한 길로만: 같은 이름의 작업을 이어 붙여 동시에 두 번 돌지 않게 한다 (아키텍처 6장). */
class SyncScheduler(private val context: Context) {
    fun enqueue() {
        val request = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork("sync", ExistingWorkPolicy.APPEND_OR_REPLACE, request)
    }
}
