package com.nopo.cheonnyeon.domain.rule

import com.nopo.cheonnyeon.core.util.GeoDistance
import com.nopo.cheonnyeon.domain.model.Store
import kotlin.random.Random

/** AI 노포 룰렛의 조건: 지역(시·도), 음식 종류, 도보 거리(미터). null 이면 제한 없음 */
data class RouletteFilter(val sido: String? = null, val cuisine: String? = null, val walkMeters: Int? = null)

/** 룰렛 음식 종류 분류와 후보 고르기 (순수 규칙) */
object RoulettePicker {
    val CUISINES = listOf("한식", "중식", "일식", "양식", "분식", "카페")

    /** 사진 있는 가게에 주는 가중치 배수 */
    private const val PHOTO_WEIGHT = 3.0

    /** 평점 가중치 = [RATING_BASE] + 평점 / [RATING_SCALE]. 평점이 없으면 [DEFAULT_RATING]으로 본다 */
    private const val RATING_BASE = 0.5
    private const val RATING_SCALE = 5.0
    private const val DEFAULT_RATING = 3.5

    private val rules = listOf(
        "카페" to listOf("카페", "커피", "베이커리", "제과", "빵", "디저트", "다방", "케이크", "떡집"),
        "중식" to listOf("중식", "중화", "짜장", "짬뽕", "탕수", "마라", "딤섬", "만두"),
        "일식" to listOf("일식", "초밥", "스시", "사시미", "우동", "돈까스", "돈가스", "라멘", "소바", "오마카세"),
        "양식" to listOf("양식", "파스타", "피자", "스테이크", "햄버거", "경양식", "샌드위치", "브런치"),
        "분식" to listOf("분식", "떡볶이", "김밥", "튀김", "어묵", "오뎅"),
    )

    /** 가게의 음식 종류. 위 단어가 없으면 한식 */
    fun cuisineOf(s: Store): String {
        val text = listOf(s.categoryGroup, s.name, s.menuItems.joinToString(" ")).joinToString(" ")
        return rules.firstOrNull { (_, words) -> words.any { text.contains(it) } }?.first ?: "한식"
    }

    /** 휠에 적힌 여섯 칸(시장맛집·국밥·냉면·고기·분식·카페) 중 가게가 가까운 칸 */
    fun wheelSegment(s: Store): String {
        val text = listOf(s.name, s.menuItems.joinToString(" ")).joinToString(" ")
        return when {
            cuisineOf(s) == "카페" -> "카페"
            cuisineOf(s) == "분식" -> "분식"
            listOf("국밥", "해장국", "설렁탕", "곰탕", "순대국", "탕").any { text.contains(it) } -> "국밥"
            listOf("냉면", "막국수", "국수", "칼국수", "면").any { text.contains(it) } -> "냉면"
            listOf("고기", "갈비", "삼겹", "구이", "불고기", "곱창", "족발", "보쌈").any { text.contains(it) } -> "고기"
            else -> "시장맛집"
        }
    }

    /** 조건에 맞는 후보. 거리 조건이 위치 없이 걸리면 무시하고, 거리 때문에 비면 거리 조건만 푼다. */
    fun candidates(stores: List<Store>, f: RouletteFilter, here: Pair<Double, Double>?): List<Store> {
        val base = stores.filter { it.isFood && !it.isTest && it.lat != null && it.lng != null }
            .filter { f.sido == null || it.sido == f.sido }
            .filter { f.cuisine == null || cuisineOf(it) == f.cuisine }
        if (f.walkMeters == null || here == null) return base
        return base.filter { GeoDistance.meters(here.first, here.second, it.lat!!, it.lng!!) <= f.walkMeters }
    }

    /** 사진이 있는 가게와 평점이 높은 가게가 더 잘 뽑히게 가중 무작위로 한 곳을 고른다. 후보가 없으면 null */
    fun pick(list: List<Store>, ratings: Map<String, Double>, random: Random = Random.Default): Store? {
        if (list.isEmpty()) return null
        val weights = list.map { s ->
            (if (s.photoUrls.isNotEmpty()) PHOTO_WEIGHT else 1.0) * (RATING_BASE + (ratings[s.id] ?: DEFAULT_RATING) / RATING_SCALE)
        }
        var x = random.nextDouble() * weights.sum()
        list.forEachIndexed { i, s -> x -= weights[i]; if (x <= 0) return s }
        return list.last()
    }
}
