package com.nopo.cheonnyeon.domain.rule

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class YearsCalculatorTest {
    @Test fun `U-201-1 창업 1996 방문 2026 은 30`() = assertEquals(30, YearsCalculator.yearsAtVisit(1996, 2026))

    @Test fun `U-201-2 창업 2000 방문 2026 은 26이 아니라 바닥값 30`() = assertEquals(30, YearsCalculator.yearsAtVisit(2000, 2026))

    @Test fun `U-201-3 창업연도 null 은 30`() = assertEquals(30, YearsCalculator.yearsAtVisit(null, 2026))

    @Test fun `U-201-4 창업 1964 방문 2026 은 62`() = assertEquals(62, YearsCalculator.yearsAtVisit(1964, 2026))

    @Test fun `U-201-4 방문 연도가 바뀌면 계산값은 바뀐다 - 적립값 고정은 저장 쪽 책임`() =
        assertEquals(63, YearsCalculator.yearsAtVisit(1964, 2027))

    @Test fun `U-201-5 창업연도가 미래면 오류`() {
        assertThrows(IllegalArgumentException::class.java) { YearsCalculator.yearsAtVisit(2030, 2026) }
    }

    @Test fun `올해 창업도 바닥값 30`() = assertEquals(30, YearsCalculator.yearsAtVisit(2026, 2026))
}
