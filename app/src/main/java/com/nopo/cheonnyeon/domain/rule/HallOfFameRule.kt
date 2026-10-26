package com.nopo.cheonnyeon.domain.rule

import com.nopo.cheonnyeon.domain.model.Title

/** 명예의 전당 순위 계산 (같은 세월이면 같은 순위, 나보다 세월이 많은 사람 수 + 1) */
object HallOfFameRule {
    fun rank(myYears: Int, otherYears: List<Int>): Int = 1 + otherYears.count { it > myYears }

    /** 상위 몇 %인지(올림, 1~100). 나를 포함한 전체 인원 기준 */
    fun topPercent(rank: Int, others: Int): Int {
        val total = others + 1
        return ((rank * 100 + total - 1) / total).coerceIn(1, 100)
    }

    /** 천년 미식가(1,000년)까지 얼마나 왔는지 0~100 */
    fun progressToMaster(years: Int): Int = (years * 100 / Title.MASTER.requiredYears).coerceIn(0, 100)
}
