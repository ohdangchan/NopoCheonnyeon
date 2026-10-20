package com.nopo.cheonnyeon.data.remote

import android.content.Context
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
private data class DemoFile(val reviews: List<DemoRow> = emptyList())

@Serializable
private data class DemoRow(val s: String, val n: String, val t: String, val r: Int, val x: String = "", val v: Int = 1, val d: Int = 1)

/**
 * 시연용 예시 리뷰(assets/demo_reviews.json, tools/gen_demo_data.py 로 생성). 실제 사용자 리뷰가 아니며 서버에 올라가지 않는다.
 * 화면에는 "시연용 예시" 표시가 붙고, 설정 > 개발자 도구에서 끌 수 있다.
 */
class DemoReviews(private val context: Context, private val io: CoroutineDispatcher, private val nowMillis: () -> Long) {
    private var byStore: Map<String, List<ReviewItem>>? = null

    suspend fun forStore(storeId: String): List<ReviewItem> {
        val map = byStore ?: withContext(io) { load() }.also { byStore = it }
        return map[storeId].orEmpty()
    }

    private fun load(): Map<String, List<ReviewItem>> = runCatching {
        val text = context.assets.open(ASSET).bufferedReader(Charsets.UTF_8).use { it.readText() }
        val now = nowMillis()
        Json { ignoreUnknownKeys = true }.decodeFromString<DemoFile>(text).reviews.mapIndexed { i, r ->
            ReviewItem(
                id = "demo_$i", storeId = r.s, storeName = "", uid = DEMO_UID, nickname = r.n, titleId = r.t, rating = r.r.coerceIn(1, 5),
                text = r.x, visitCount = r.v, updatedAtMillis = now - r.d * DAY_MS, demo = true,
            )
        }.groupBy { it.storeId }
    }.getOrDefault(emptyMap())

    companion object {
        const val ASSET = "demo_reviews.json"
        const val DEMO_UID = "demo"
        private const val DAY_MS = 86_400_000L

        /** 서버 평점 요약과 예시 리뷰를 합친 요약 */
        fun merge(server: ReviewSummary?, demo: List<ReviewItem>): ReviewSummary? {
            if (demo.isEmpty()) return server
            val sc = server?.count ?: 0
            val sAvg = server?.average ?: 0.0
            val count = sc + demo.size
            return ReviewSummary(count, (sAvg * sc + demo.sumOf { it.rating }) / count)
        }
    }
}
