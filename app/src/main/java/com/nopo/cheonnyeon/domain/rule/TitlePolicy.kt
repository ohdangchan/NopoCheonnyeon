package com.nopo.cheonnyeon.domain.rule

import com.nopo.cheonnyeon.domain.model.Title

/** 칭호 (FR-STP-04): 세월 100년마다 한 단계, 0 첫술 식객 ~ 1,000 천년 미식가(모두 11단계) */
object TitlePolicy {
    fun titleFor(totalYears: Int): Title = Title.entries.last { totalYears >= it.requiredYears }

    /** 세월이 늘어 칭호가 올랐으면 새 칭호(여러 단계를 건너뛰면 가장 높은 것), 아니면 null */
    fun titleUp(beforeYears: Int, afterYears: Int): Title? {
        val before = titleFor(beforeYears)
        val after = titleFor(afterYears)
        return if (after.ordinal > before.ordinal) after else null
    }

    /** 다음 칭호까지 남은 세월. 최고 칭호면 null */
    fun yearsToNext(totalYears: Int): Int? =
        Title.entries.firstOrNull { it.requiredYears > totalYears }?.let { it.requiredYears - totalYears }
}
