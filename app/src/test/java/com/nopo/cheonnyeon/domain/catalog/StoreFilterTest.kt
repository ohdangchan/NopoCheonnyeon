package com.nopo.cheonnyeon.domain.catalog

import com.nopo.cheonnyeon.domain.model.Store
import org.junit.Assert.assertEquals
import org.junit.Test

class StoreFilterTest {
    private val a = Store(id = "A", name = "이문설농탕", sido = "서울", sigungu = "종로구", programs = listOf("BAEKNYEON"), foundedYear = 1904, menuItems = listOf("설렁탕", "도가니탕"), lat = 37.57, lng = 126.99)
    private val b = Store(id = "B", name = "을지면옥", sido = "서울", sigungu = "중구", programs = listOf("ORAE"), foundedYear = 1971, menuItems = listOf("평양냉면"), lat = 37.56, lng = 126.99)
    private val c = Store(id = "C", name = "고령금산한우", sido = "경북", sigungu = "고령군", programs = listOf("BAEKNYEON"), foundedYear = 1995, isFood = true)
    private val d = Store(id = "D", name = "성수사진관", sido = "서울", sigungu = "성동구", programs = listOf("BAEKNYEON"), isFood = false, foundedYear = 1960, lat = 37.54, lng = 127.05)
    private val all = listOf(a, b, c, d)

    private fun ids(f: StoreFilter, visited: Set<String> = emptySet()) = StoreFilters.apply(all, f, visited).map { it.id }

    @Test fun `필터가 없으면 전부`() = assertEquals(listOf("A", "B", "C", "D"), ids(StoreFilter()))

    @Test fun `음식점만`() = assertEquals(listOf("A", "B", "C"), ids(StoreFilter(foodOnly = true)))

    @Test fun `백년가게만`() = assertEquals(listOf("A", "C", "D"), ids(StoreFilter(program = Program.BAEKNYEON)))

    @Test fun `오래가게만`() = assertEquals(listOf("B"), ids(StoreFilter(program = Program.ORAE)))

    @Test fun `안 가 본 곳만`() = assertEquals(listOf("B", "C", "D"), ids(StoreFilter(unvisitedOnly = true), visited = setOf("A")))

    @Test fun `필터 조합은 모두 만족해야 한다`() =
        assertEquals(listOf("C"), ids(StoreFilter(foodOnly = true, program = Program.BAEKNYEON, unvisitedOnly = true), visited = setOf("A")))

    @Test fun `가게 이름으로 검색`() = assertEquals(listOf("B"), ids(StoreFilter(query = "을지")))

    @Test fun `지역으로 검색 - 시군구`() = assertEquals(listOf("A"), ids(StoreFilter(query = "종로구")))

    @Test fun `지역으로 검색 - 시도`() = assertEquals(listOf("A", "B", "D"), ids(StoreFilter(query = "서울")))

    @Test fun `대표 메뉴로 검색`() = assertEquals(listOf("B"), ids(StoreFilter(query = "냉면")))

    @Test fun `띄어쓰기와 대소문자 무시`() = assertEquals(listOf("A"), ids(StoreFilter(query = " 이문 설농탕 ")))

    @Test fun `검색어가 여러 단어면 모두 맞아야 한다`() = assertEquals(listOf("A"), ids(StoreFilter(query = "서울 설렁탕")))

    @Test fun `좌표가 없는 가게도 검색된다`() = assertEquals(listOf("C"), ids(StoreFilter(query = "금산")))

    @Test fun `결과가 없으면 빈 목록`() = assertEquals(emptyList<String>(), ids(StoreFilter(query = "없는가게")))

    @Test fun `업력순 정렬은 오래된 가게가 먼저이고 창업연도를 모르면 30년으로 본다`() {
        val unknown = Store(id = "U", name = "모름", foundedYear = null)
        val sorted = StoreFilters.sortByYears(listOf(c, unknown, a, b), currentYear = 2026).map { it.id }
        assertEquals(listOf("A", "B", "C", "U"), sorted)
    }

    @Test fun `가까운 순 정렬은 좌표 없는 가게를 맨 뒤로 보낸다`() {
        val sorted = StoreFilters.sortByDistance(all, lat = 37.565, lng = 126.99).map { it.id }
        assertEquals(listOf("B", "A", "D", "C"), sorted)
    }

    @Test fun `영역 안 가게만 고른다`() {
        val inBox = StoreFilters.inBounds(all, south = 37.55, north = 37.58, west = 126.9, east = 127.0).map { it.id }
        assertEquals(listOf("A", "B"), inBox)
    }
}
