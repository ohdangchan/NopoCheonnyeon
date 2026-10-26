package com.nopo.cheonnyeon.domain.rule

import com.nopo.cheonnyeon.core.util.GeoDistance
import com.nopo.cheonnyeon.domain.model.Store

/** 챗봇에게 보여 줄 후보 가게 한 곳 */
data class ChatCandidate(val store: Store, val years: Int, val distanceKm: Double?, val visited: Boolean)

/**
 * AI 챗봇이 고를 후보 가게를 미리 좁힌다(전체 1,300곳을 다 보내지 않는다).
 * 사용자가 말한 지역(종로, 송파 등)이 있으면 그 지역을 우선하고, 없으면 내 위치에서 가까운 곳(위치가 없으면 업력 순)을 고른다.
 * 1,000년 경로를 짤 때를 위해 안 가 본 곳 중 업력이 높은 곳도 따로 더한다.
 */
object ChatCandidates {
    /** 가까운(또는 지역에 맞는) 후보 수 */
    const val NEAR_LIMIT = 24

    /** 1,000년 경로용으로 더하는 "안 가 본 곳 중 업력 높은" 후보 수 */
    const val RICH_LIMIT = 10

    /** 업력 높은 후보를 고르기 전에 살펴보는 범위 배수 (RICH_LIMIT × 이 값) */
    private const val RICH_SCAN_FACTOR = 10

    fun pick(
        stores: List<Store>,
        visitedIds: Set<String>,
        here: Pair<Double, Double>?,
        userText: String,
        visitYear: Int,
    ): List<ChatCandidate> {
        val foods = stores.filter { it.isFood && !it.isTest && it.lat != null && it.lng != null }
        val all = foods.map { s ->
            ChatCandidate(
                s, YearsCalculator.yearsAtVisit(s.foundedYear, visitYear),
                here?.let { GeoDistance.meters(it.first, it.second, s.lat!!, s.lng!!) / 1000.0 }, s.id in visitedIds,
            )
        }
        val regionHits = all.filter { regionMatches(it.store, userText) }
        val base = if (regionHits.isNotEmpty()) regionHits else all
        val ordered = if (here != null) base.sortedBy { it.distanceKm } else base.sortedByDescending { it.years }
        val near = ordered.take(NEAR_LIMIT)
        val pool = ordered.take(RICH_LIMIT * RICH_SCAN_FACTOR).filter { !it.visited && it !in near }
        val rich = pool.sortedByDescending { it.years }.take(RICH_LIMIT)
        return near + rich
    }

    /** "종로"가 "종로구"와, "서울"이 시·도와 맞는지(앞 두 글자 이상) */
    fun regionMatches(s: Store, text: String): Boolean {
        val gu = s.sigungu.removeSuffix("구").removeSuffix("시").removeSuffix("군")
        if (gu.length >= 2 && text.contains(gu)) return true
        val sido = s.sido.take(2)
        // 서울은 너무 넓어 지역 한정으로 쓰지 않는다
        return sido.length == 2 && s.sido.isNotBlank() && text.contains(sido) && s.sido != "서울"
    }
}
