package com.nopo.cheonnyeon.domain.rule

import com.nopo.cheonnyeon.domain.model.Title
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TitlePolicyTest {
    @Test fun `U-203-1 100년마다 칭호가 바뀐다`() {
        assertEquals(Title.SPROUT, TitlePolicy.titleFor(0))
        assertEquals(Title.SPROUT, TitlePolicy.titleFor(99))
        assertEquals(Title.REGULAR, TitlePolicy.titleFor(100))
        assertEquals(Title.WANDERER, TitlePolicy.titleFor(250))
        assertEquals(Title.ELDER, TitlePolicy.titleFor(300))
        assertEquals(Title.CONNOISSEUR, TitlePolicy.titleFor(499))
        assertEquals(Title.PIONEER, TitlePolicy.titleFor(500))
        assertEquals(Title.ARTISAN, TitlePolicy.titleFor(600))
        assertEquals(Title.KEEPER, TitlePolicy.titleFor(799))
        assertEquals(Title.SAGE, TitlePolicy.titleFor(800))
        assertEquals(Title.LEGEND, TitlePolicy.titleFor(999))
        assertEquals(Title.MASTER, TitlePolicy.titleFor(1000))
        assertEquals(Title.MASTER, TitlePolicy.titleFor(1500))
    }

    @Test fun `칭호는 11단계이고 모두 100년 간격`() {
        assertEquals(11, Title.entries.size)
        Title.entries.forEachIndexed { i, t -> assertEquals(i * 100, t.requiredYears) }
    }

    @Test fun `U-203-2 280에서 620이 되면 중간 단계를 건너뛰고 맛의 장인`() =
        assertEquals(Title.ARTISAN, TitlePolicy.titleUp(280, 620))

    @Test fun `U-203-3 칭호 변화가 없으면 null`() {
        assertNull(TitlePolicy.titleUp(100, 199))
        assertNull(TitlePolicy.titleUp(1000, 1100))
    }

    @Test fun `경계를 넘는 순간 승급`() {
        assertEquals(Title.REGULAR, TitlePolicy.titleUp(70, 100))
        assertEquals(Title.WANDERER, TitlePolicy.titleUp(190, 200))
        assertEquals(Title.MASTER, TitlePolicy.titleUp(990, 1000))
    }

    @Test fun `다음 칭호까지 남은 세월`() {
        assertEquals(58, TitlePolicy.yearsToNext(442))
        assertEquals(100, TitlePolicy.yearsToNext(0))
        assertEquals(1, TitlePolicy.yearsToNext(599))
        assertNull(TitlePolicy.yearsToNext(1000))
        assertNull(TitlePolicy.yearsToNext(1500))
    }
}
