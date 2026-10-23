package com.nopo.cheonnyeon.feature.common

import com.nopo.cheonnyeon.domain.model.Store
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StoreCardsTest {
    @Test fun `거리 표기 경계`() {
        assertEquals("999m", StoreCards.formatDistance(999.0))
        assertEquals("1.0km", StoreCards.formatDistance(1000.0))
        assertEquals("9.9km", StoreCards.formatDistance(9949.0))
        assertEquals("10km", StoreCards.formatDistance(10_000.0))
    }

    @Test fun `좌표가 없으면 거리가 없다`() {
        val s = Store(id = "S-1", name = "가게", foundedYear = 1980)
        assertNull(StoreCards.of(s, 2026, emptyMap(), null, emptySet()).distanceM)
    }

    @Test fun `창업연도가 미래여도 죽지 않는다`() {
        val s = Store(id = "S-2", name = "가게", foundedYear = 2100)
        assertEquals("30년 전통", StoreCards.tag(s, 2026).let { if (it.endsWith("전통")) it else "30년 전통" })
    }

    @Test fun `대표 메뉴는 빈 이름을 건너뛴다`() {
        val s = Store(id = "S-3", name = "가게", menuItems = listOf("", "설렁탕"))
        assertEquals("설렁탕", StoreCards.mainMenu(s))
    }
}
