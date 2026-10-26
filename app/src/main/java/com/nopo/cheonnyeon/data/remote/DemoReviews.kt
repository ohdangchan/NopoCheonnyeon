package com.nopo.cheonnyeon.data.remote

import android.content.Context
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
private data class DemoFile(
    val users: List<DemoUserRow> = emptyList(),
    val reviews: List<DemoRow> = emptyList(),
    val me: DemoMeRow = DemoMeRow(),
)

@Serializable
private data class DemoRow(
    val s: String,
    val n: String,
    val t: String,
    val r: Int,
    val x: String = "",
    val v: Int = 1,
    val d: Int = 1,
    val p: String = "",
    val p2: String = "",
    val p3: String = ""
)

@Serializable
private data class DemoUserRow(val n: String, val y: Int, val c: Int = 1, val rc: Int = 0)

@Serializable
private data class DemoMeRow(
    val totalYears: Int = 0,
    val visits: List<DemoVisitRow> = emptyList(),
    val reviews: List<DemoMyReviewRow> = emptyList()
)

@Serializable
private data class DemoVisitRow(val s: String, val y: Int, val d: Int, val c: Int = 1)

@Serializable
private data class DemoMyReviewRow(
    val s: String,
    val n: String = "",
    val r: Int,
    val x: String = "",
    val d: Int = 1,
    val p: String = "",
    val p2: String = "",
    val p3: String = ""
)

/** 명예의 전당에 쓰는 시연용 식객 한 명 */
data class DemoUser(val nickname: String, val years: Int, val stores: Int, val reviews: Int)

/** 시연용 패스포트에 넣는 도장 한 줄 */
data class DemoVisit(val storeId: String, val years: Int, val daysAgo: Int, val visitCount: Int)

/** 시연용 인기 노포 한 곳 */
data class DemoTopStore(val storeId: String, val average: Double, val count: Int)

class DemoData internal constructor(
    val users: List<DemoUser>,
    val byStore: Map<String, List<ReviewItem>>,
    val myVisits: List<DemoVisit>,
    internal val myReviews: List<Pair<String, ReviewItem>>,
) {
    /** 리뷰가 [minCount]개 이상인 가게를 평균 평점·리뷰 수 순으로 */
    fun topStores(limit: Int, minCount: Int = 5): List<DemoTopStore> =
        byStore.map { (id, list) -> DemoTopStore(id, list.sumOf { it.rating }.toDouble() / list.size, list.size) }
            .filter { it.count >= minCount }
            .sortedWith(compareByDescending<DemoTopStore> { it.average }.thenByDescending { it.count }.thenBy { it.storeId })
            .take(limit)
}

/**
 * 시연용 예시 데이터(assets/demo_reviews.json, tools/gen_demo_data.py 로 생성). 실제 사용자 리뷰가 아니며 서버에 올라가지 않는다.
 * 화면에는 "시연용 예시" 표시가 붙고, 설정 > 개발자 도구에서 끌 수 있다.
 */
class DemoReviews(private val context: Context, private val io: CoroutineDispatcher, private val nowMillis: () -> Long) {
    private var data: DemoData? = null

    suspend fun load(): DemoData = data ?: withContext(io) { read() }.also { data = it }

    suspend fun forStore(storeId: String): List<ReviewItem> = load().byStore[storeId].orEmpty()

    /** 내가 시연용 패스포트로 남긴 리뷰(닉네임은 지금 계정 것으로 바꿔 보여 준다) */
    suspend fun myReviews(nickname: String, storeId: String? = null): List<ReviewItem> =
        load().myReviews.filter { storeId == null || it.first == storeId }.map { it.second.copy(nickname = nickname) }

    private fun read(): DemoData = runCatching {
        val text = context.assets.open(ASSET).bufferedReader(Charsets.UTF_8).use { it.readText() }
        val now = nowMillis()
        val file = Json { ignoreUnknownKeys = true }.decodeFromString<DemoFile>(text)
        val byStore = file.reviews.mapIndexed { i, r ->
            ReviewItem(
                id = "demo_$i", storeId = r.s, storeName = "", uid = DEMO_UID, nickname = r.n, titleId = r.t, rating = r.r.coerceIn(1, 5),
                text = r.x, visitCount = r
                    .v, updatedAtMillis = now - r
                    .d * DAY_MS, demo = true, photos = listOf(r.p, r.p2, r.p3)
                    .filter { it.isNotBlank() }.map { "asset:$it" },
            )
        }.groupBy { it.storeId }
        val mine = file.me.reviews.mapIndexed { i, r ->
            r.s to ReviewItem(
                id = "demo_me_$i", storeId = r.s, storeName = r
                    .n, uid = DEMO_ME_UID, nickname = "", titleId = "ARTISAN", rating = r
                    .r
                    .coerceIn(1, 5),
                text = r.x, visitCount = 1, updatedAtMillis = now - r.d * DAY_MS, demo = true,
                photos = listOf(r.p, r.p2, r.p3).filter { it.isNotBlank() }.map { "asset:$it" },
            )
        }
        DemoData(
            users = file.users.map { DemoUser(it.n, it.y, maxOf(it.c, it.rc), it.rc) }.sortedByDescending { it.years },
            byStore = byStore,
            myVisits = file.me.visits.map { DemoVisit(it.s, it.y, it.d, it.c) },
            myReviews = mine,
        )
    }.getOrDefault(DemoData(emptyList(), emptyMap(), emptyList(), emptyList()))

    companion object {
        const val ASSET = "demo_reviews.json"
        const val DEMO_UID = "demo"
        const val DEMO_ME_UID = "demo-me"
        /** 시연용 패스포트 도장의 인증 방법 표지 */
        const val METHOD_DEMO = "DEMO"
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
