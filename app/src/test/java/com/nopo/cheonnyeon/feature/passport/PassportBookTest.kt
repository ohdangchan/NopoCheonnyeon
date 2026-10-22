package com.nopo.cheonnyeon.feature.passport

import com.nopo.cheonnyeon.data.local.MyVisitEntity
import com.nopo.cheonnyeon.domain.rule.RegionProgress
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PassportBookTest {
    private fun stamp(i: Int) = PassportStamp(MyVisitEntity("S-$i", 30, "RECEIPT", i.toLong(), i.toLong()), null)

    @Test fun `도장이 없어도 첫 장과 방문 도장 빈 장이 만들어진다`() {
        val pages = buildPages(emptyList(), emptyList())
        assertEquals(2, pages.size)
        assertTrue(pages[1].slots.isEmpty())
        assertEquals(6, pages[0].slots.size)
    }

    @Test fun `지역 스탬프는 찍힌 곳이 먼저 온다`() {
        val regions = listOf(RegionProgress("부산", 1, 10), RegionProgress("서울", 5, 10))
        val first = buildPages(regions, emptyList())[0].slots.first() as Slot.Region
        assertEquals("서울", first.r.sido)
    }

    @Test fun `방문 도장은 한 장에 아홉 개씩 나뉜다`() {
        val pages = buildPages(emptyList(), (1..20).map { stamp(it) })
        // 첫 장 + 방문 도장 3장(9+9+2)
        assertEquals(4, pages.size)
        assertEquals(2, pages[3].slots.size)
    }

    @Test fun `펼침 수는 쪽 수에 맞춰 올림한다`() {
        assertEquals(1, spreadCount(1))
        assertEquals(2, spreadCount(2))
        assertEquals(2, spreadCount(3))
        assertEquals(3, spreadCount(4))
    }
}
