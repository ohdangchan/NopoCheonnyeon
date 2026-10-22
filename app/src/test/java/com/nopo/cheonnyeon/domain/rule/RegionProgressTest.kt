package com.nopo.cheonnyeon.domain.rule

import com.nopo.cheonnyeon.domain.model.Store
import org.junit.Assert.assertEquals
import org.junit.Test

class RegionProgressTest {
    private fun s(id: String, sido: String, test: Boolean = false) = Store(id = id, name = id, sido = sido, isTest = test)

    @Test fun `시도별 방문 수와 전체 수를 센다`() {
        val r = RegionProgressRule.compute(listOf(s("a", "서울"), s("b", "서울"), s("c", "부산")), setOf("a", "c"))
        assertEquals(listOf(RegionProgress("서울", 1, 2), RegionProgress("부산", 1, 1)), r)
        assertEquals(50, r[0].percent)
    }

    @Test fun `테스트 가게와 지역이 비어 있는 가게는 세지 않는다`() {
        val r = RegionProgressRule.compute(listOf(s("a", "서울"), s("TEST-1", "서울"), s("t", "서울", test = true), s("x", "")), setOf("TEST-1"))
        assertEquals(listOf(RegionProgress("서울", 0, 1)), r)
    }

    @Test fun `가게가 없으면 0퍼센트`() {
        assertEquals(0, RegionProgress("서울", 0, 0).percent)
    }

    @Test fun `목표 5곳을 채우면 지역 스탬프가 찍힌다`() {
        assertEquals(false, RegionProgress("서울", 4, 270).stamped)
        assertEquals(true, RegionProgress("서울", 5, 270).stamped)
        assertEquals(1, RegionProgress("서울", 4, 270).remaining)
    }

    @Test fun `가게가 목표보다 적은 지역은 전부 방문하면 찍힌다`() {
        assertEquals(true, RegionProgress("세종", 3, 3).stamped)
        assertEquals(false, RegionProgress("세종", 2, 3).stamped)
        assertEquals(false, RegionProgress("없음", 0, 0).stamped)
    }

    @Test fun `다음 스탬프는 방문이 가장 많은 미완성 지역`() {
        val r = listOf(RegionProgress("서울", 5, 9), RegionProgress("부산", 3, 9), RegionProgress("대구", 1, 9), RegionProgress("광주", 0, 9))
        assertEquals("부산", RegionProgressRule.next(r)?.sido)
        assertEquals(null, RegionProgressRule.next(listOf(RegionProgress("광주", 0, 9))))
    }
}
