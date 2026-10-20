package com.nopo.cheonnyeon.domain.rule

/** 패스포트 한눈에 보기: 도장 수, 방문한 시·도 수, 가장 오래된 가게의 세월 */
data class PassportStats(val stamps: Int, val regions: Int, val oldestYears: Int) {
    companion object {
        /** [entries]는 (시·도, 적립 세월). 테스트 가게는 미리 빼고 넘긴다. */
        fun of(entries: List<Pair<String, Int>>): PassportStats = PassportStats(
            stamps = entries.size,
            regions = entries.map { it.first }.filter { it.isNotBlank() }.toSet().size,
            oldestYears = entries.maxOfOrNull { it.second } ?: 0,
        )
    }
}
