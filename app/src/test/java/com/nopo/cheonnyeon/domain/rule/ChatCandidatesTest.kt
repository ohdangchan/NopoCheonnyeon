package com.nopo.cheonnyeon.domain.rule

import com.nopo.cheonnyeon.domain.model.Store
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatCandidatesTest {
    private fun s(id: String, gu: String, lat: Double, lng: Double, founded: Int = 1980, sido: String = "서울", food: Boolean = true, test: Boolean = false) =
        Store(id = id, name = "가게$id", sido = sido, sigungu = gu, lat = lat, lng = lng, foundedYear = founded, isFood = food, isTest = test)

    private val stores = listOf(
        s("A", "종로구", 37.570, 126.990), s("B", "송파구", 37.500, 127.100), s("C", "종로구", 37.575, 126.985, founded = 1900),
        s("D", "마포구", 37.550, 126.920, food = false), s("T", "종로구", 37.57, 126.99, test = true), s("N", "중구", 0.0, 0.0).copy(lat = null),
    )

    @Test fun `음식점이 아니거나 시험용이거나 좌표 없는 가게는 후보에서 빠진다`() {
        val ids = ChatCandidates.pick(stores, emptySet(), null, "", 2026).map { it.store.id }
        assertEquals(setOf("A", "B", "C"), ids.toSet())
    }

    @Test fun `말한 지역이 있으면 그 지역 가게만`() {
        val ids = ChatCandidates.pick(stores, emptySet(), null, "비 오는 날 종로에서 혼자 먹기 좋은 국밥집", 2026).map { it.store.id }
        assertEquals(setOf("A", "C"), ids.toSet())
    }

    @Test fun `위치가 있으면 가까운 순이고 거리와 방문 표시가 붙는다`() {
        val c = ChatCandidates.pick(stores, setOf("A"), 37.571 to 126.991, "", 2026)
        assertEquals("A", c.first().store.id)
        assertTrue(c.first().visited)
        assertTrue(c.first().distanceKm!! < 0.5)
    }

    @Test fun `위치가 없으면 업력이 높은 순`() {
        val c = ChatCandidates.pick(stores, emptySet(), null, "", 2026)
        assertEquals("C", c.first().store.id)
    }

    @Test fun `후보 수는 상한을 넘지 않는다`() {
        val many = (1..300).map { s("S$it", "중구", 37.5 + it * 0.001, 127.0, founded = 1900 + it % 80) }
        val c = ChatCandidates.pick(many, emptySet(), 37.5 to 127.0, "", 2026)
        assertTrue(c.size <= ChatCandidates.NEAR_LIMIT + ChatCandidates.RICH_LIMIT)
        assertEquals(c.size, c.map { it.store.id }.toSet().size)
    }
}
