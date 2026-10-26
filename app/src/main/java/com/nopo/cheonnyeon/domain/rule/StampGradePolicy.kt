package com.nopo.cheonnyeon.domain.rule

import com.nopo.cheonnyeon.domain.model.StampGrade

/** 도장 등급 (FR-STP-03): 30~49 일반(청록), 50~99 붉은, 100 이상 금빛 */
object StampGradePolicy {
    private const val GOLD_MIN_YEARS = 100
    private const val RED_MIN_YEARS = 50

    fun gradeFor(years: Int): StampGrade = when {
        years >= GOLD_MIN_YEARS -> StampGrade.GOLD
        years >= RED_MIN_YEARS -> StampGrade.RED
        else -> StampGrade.NORMAL
    }
}
