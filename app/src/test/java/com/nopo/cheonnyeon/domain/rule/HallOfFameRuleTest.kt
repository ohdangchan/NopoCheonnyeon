package com.nopo.cheonnyeon.domain.rule

import org.junit.Assert.assertEquals
import org.junit.Test

class HallOfFameRuleTest {
    @Test fun `나보다 세월이 많은 사람 수 더하기 1이 순위`() {
        assertEquals(3, HallOfFameRule.rank(637, listOf(1474, 900, 637, 100)))
        assertEquals(1, HallOfFameRule.rank(2000, listOf(1474)))
    }

    @Test fun `상위 퍼센트는 올림하고 1에서 100 사이`() {
        assertEquals(1, HallOfFameRule.topPercent(1, 999))
        assertEquals(14, HallOfFameRule.topPercent(16, 120))
        assertEquals(100, HallOfFameRule.topPercent(10, 9))
    }

    @Test fun `천년 도달률은 1000년에서 100퍼센트`() {
        assertEquals(63, HallOfFameRule.progressToMaster(637))
        assertEquals(100, HallOfFameRule.progressToMaster(1500))
        assertEquals(0, HallOfFameRule.progressToMaster(0))
    }
}
