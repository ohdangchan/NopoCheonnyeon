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
}
