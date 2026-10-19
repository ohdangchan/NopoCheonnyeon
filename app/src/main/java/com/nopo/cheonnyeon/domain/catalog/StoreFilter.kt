package com.nopo.cheonnyeon.domain.catalog

import com.nopo.cheonnyeon.core.util.GeoDistance
import com.nopo.cheonnyeon.core.util.TextNormalizer
import com.nopo.cheonnyeon.domain.model.Store
import com.nopo.cheonnyeon.domain.rule.YearsCalculator

/** 공식 지정 종류 (stores.json의 programs 값) */
enum class Program(val code: String) {
    BAEKNYEON("BAEKNYEON"),
    ORAE("ORAE"),
}

/** 지도·목록이 함께 쓰는 필터 (FR-MAP-03·06). 조건은 모두 만족해야 한다. */
data class StoreFilter(
    val query: String = "",
    val foodOnly: Boolean = false,
    val program: Program? = null,
    val unvisitedOnly: Boolean = false,
)

object StoreFilters {
    fun apply(stores: List<Store>, filter: StoreFilter, visitedIds: Set<String>): List<Store> {
        val words = filter.query.split(Regex("\\s+")).map(TextNormalizer::normalize).filter { it.isNotEmpty() }
        return stores.filter { s ->
            (!filter.foodOnly || s.isFood) &&
                (filter.program == null || filter.program.code in s.programs) &&
                (!filter.unvisitedOnly || s.id !in visitedIds) &&
                (words.isEmpty() || matches(s, words))
        }
    }

    /** 검색어의 모든 단어가 이름·별칭·지역·메뉴 중 어딘가에 들어 있어야 한다 */
    private fun matches(s: Store, words: List<String>): Boolean {
        val haystacks = (s.nameCandidates + s.sido + s.sigungu + s.menuItems).map(TextNormalizer::normalize)
        return words.all { w -> haystacks.any { it.contains(w) } }
    }

    /** 업력 큰 순. 창업연도를 모르면 30년으로 본다 */
    fun sortByYears(stores: List<Store>, currentYear: Int): List<Store> =
        stores.sortedByDescending { YearsCalculator.yearsAtVisit(it.foundedYear, currentYear) }

    /** 가까운 순. 좌표가 없는 가게는 맨 뒤 */
    fun sortByDistance(stores: List<Store>, lat: Double, lng: Double): List<Store> =
        stores.sortedBy { s ->
            if (s.lat == null || s.lng == null) Double.MAX_VALUE else GeoDistance.meters(lat, lng, s.lat, s.lng)
        }

    fun inBounds(stores: List<Store>, south: Double, north: Double, west: Double, east: Double): List<Store> =
        stores.filter { s -> s.lat != null && s.lng != null && s.lat in south..north && s.lng in west..east }
}
