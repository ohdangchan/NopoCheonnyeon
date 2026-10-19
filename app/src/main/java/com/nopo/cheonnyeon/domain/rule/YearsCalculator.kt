package com.nopo.cheonnyeon.domain.rule

/** 업력 = 방문한 해 − 창업연도. 공식 지정 가게는 모두 30년 이상이라 30을 바닥값으로 둔다 (PRD 6.1, D-07). */
object YearsCalculator {
    const val MIN_YEARS = 30

    /**
     * 인증 당시 업력. 창업연도를 모르면 30.
     * 적립값은 인증한 날 한 번 정해 저장하고 이후 다시 계산하지 않는다 (FR-STP-01).
     * @throws IllegalArgumentException 창업연도가 방문 연도보다 미래일 때 (데이터 오류)
     */
    fun yearsAtVisit(foundedYear: Int?, visitYear: Int): Int {
        if (foundedYear == null) return MIN_YEARS
        require(foundedYear <= visitYear) { "창업연도($foundedYear)가 방문 연도($visitYear)보다 미래" }
        return maxOf(MIN_YEARS, visitYear - foundedYear)
    }
}
