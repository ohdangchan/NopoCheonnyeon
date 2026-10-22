package com.nopo.cheonnyeon.domain.rule

import com.nopo.cheonnyeon.domain.model.Store

/** 시·도별 방문 진행도 (지역 컬렉션) */
data class RegionProgress(val sido: String, val visited: Int, val total: Int) {
    val percent: Int get() = if (total == 0) 0 else visited * 100 / total

    /** 지역 스탬프: 그 시·도의 노포를 [REGION_STAMP_TARGET]곳 방문하면 찍힌다 (가게 수가 목표보다 적은 지역은 전부 방문하면 찍힌다) */
    val target: Int get() = minOf(REGION_STAMP_TARGET, total)
    val stamped: Boolean get() = target > 0 && visited >= target
    val remaining: Int get() = (target - visited).coerceAtLeast(0)

    companion object {
        const val REGION_STAMP_TARGET = 5
    }
}

object RegionProgressRule {
    /** 테스트 가게는 세지 않는다. 전체 가게가 많은 지역 순, 같으면 이름순. */
    /** 다음에 찍을 스탬프: 아직 안 찍힌 지역 중 가장 많이 방문한 곳(같으면 먼저 나온 곳). 방문이 하나도 없으면 null */
    fun next(regions: List<RegionProgress>): RegionProgress? =
        regions.filter { !it.stamped && it.visited > 0 }.maxByOrNull { it.visited }

    fun compute(stores: List<Store>, visitedIds: Set<String>): List<RegionProgress> =
        stores.filter { !it.isTest && !it.id.startsWith(Store.TEST_PREFIX) && it.sido.isNotBlank() }
            .groupBy { it.sido }
            .map { (sido, list) -> RegionProgress(sido, list.count { it.id in visitedIds }, list.size) }
            .sortedWith(compareByDescending<RegionProgress> { it.total }.thenBy { it.sido })
}
