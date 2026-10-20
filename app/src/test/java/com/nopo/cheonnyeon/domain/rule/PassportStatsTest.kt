package com.nopo.cheonnyeon.domain.rule

import org.junit.Assert.assertEquals
import org.junit.Test

class PassportStatsTest {
    @Test fun `도장 수와 시도 수와 가장 오래된 세월`() {
        val s = PassportStats.of(listOf("서울" to 45, "서울" to 106, "경북" to 66, "" to 30))
        assertEquals(PassportStats(4, 2, 106), s)
    }

    @Test fun `도장이 없으면 모두 0`() {
        assertEquals(PassportStats(0, 0, 0), PassportStats.of(emptyList()))
    }
}
