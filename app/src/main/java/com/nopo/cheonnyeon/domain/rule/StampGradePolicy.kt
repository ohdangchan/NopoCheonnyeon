package com.nopo.cheonnyeon.domain.rule

import com.nopo.cheonnyeon.domain.model.StampGrade

/** 도장 등급 (FR-STP-03): 30~49 일반(청록), 50~99 붉은, 100 이상 금빛 */
object StampGradePolicy {
    fun gradeFor(years: Int): StampGrade = when {
        years >= 100 -> StampGrade.GOLD
        years >= 50 -> StampGrade.RED
        else -> StampGrade.NORMAL
    }
}
