package com.nopo.cheonnyeon.feature.common

import com.nopo.cheonnyeon.core.util.GeoDistance
import com.nopo.cheonnyeon.data.remote.StoreRating
import com.nopo.cheonnyeon.domain.model.Store
import com.nopo.cheonnyeon.domain.rule.YearsCalculator
import com.nopo.cheonnyeon.platform.location.Fix

/** 시안의 가게 카드 한 장에 필요한 값 */
data class StoreCardData(
    val store: Store,
    val years: Int,
    /** "60년 전통" 또는 "1964년 개업" */
    val tag: String,
    val rating: StoreRating?,
    val distanceM: Double?,
    val bookmarked: Boolean,
) {
    val regionLabel: String get() = listOf(store.sido, store.sigungu).filter { it.isNotBlank() }.joinToString(" ")
    val oneLiner: String get() = store.summary?.takeIf { it.isNotBlank() } ?: store.menuItems.take(3).joinToString(", ")
    val distanceLabel: String? get() = distanceM?.let { StoreCards.formatDistance(it) }
}

object StoreCards {
    fun formatDistance(m: Double): String = when {
        m < 1000 -> "${m.toInt()}m"
        m < 10_000 -> String.format("%.1fkm", m / 1000.0)
        else -> "${(m / 1000.0).toInt()}km"
    }

    /** 시안은 "60년 전통"과 "1964년 개업" 두 가지 표기를 섞어 쓴다. 가게마다 같은 표기가 나오도록 id 로 고른다. */
    fun tag(store: Store, currentYear: Int): String {
        val years = YearsCalculator.yearsAtVisit(store.foundedYear?.takeIf { it <= currentYear }, currentYear)
        val founded = store.foundedYear
        return "${years}년 전통"
    }

    fun of(store: Store, currentYear: Int, ratings: Map<String, StoreRating>, fix: Fix?, bookmarks: Set<String>): StoreCardData {
        val d = if (fix != null && store.lat != null && store.lng != null) GeoDistance.meters(
            fix.lat,
            fix.lng,
            store.lat,
            store.lng
        ) else null
        return StoreCardData(
            store,
            YearsCalculator.yearsAtVisit(store.foundedYear?.takeIf { it <= currentYear }, currentYear),
            tag(store, currentYear),
            ratings[store.id],
            d,
            store.id in bookmarks
        )
    }

    /** 대표 메뉴로 보이는 말: 첫 번째 메뉴 */
    fun mainMenu(store: Store): String? = store.menuItems.firstOrNull { it.isNotBlank() }
}
