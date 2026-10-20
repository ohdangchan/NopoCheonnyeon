package com.nopo.cheonnyeon.domain.rule

import com.nopo.cheonnyeon.domain.model.Store

/** 시·도별 방문 진행도 (지역 컬렉션) */
data class RegionProgress(val sido: String, val visited: Int, val total: Int) {
    val percent: Int get() = if (total == 0) 0 else visited * 100 / total
}

object RegionProgressRule {
    /** 테스트 가게는 세지 않는다. 전체 가게가 많은 지역 순, 같으면 이름순. */
    fun compute(stores: List<Store>, visitedIds: Set<String>): List<RegionProgress> =
        stores.filter { !it.isTest && !it.id.startsWith(Store.TEST_PREFIX) && it.sido.isNotBlank() }
            .groupBy { it.sido }
            .map { (sido, list) -> RegionProgress(sido, list.count { it.id in visitedIds }, list.size) }
            .sortedWith(compareByDescending<RegionProgress> { it.total }.thenBy { it.sido })
}
