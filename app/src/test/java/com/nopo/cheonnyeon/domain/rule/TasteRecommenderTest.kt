package com.nopo.cheonnyeon.domain.rule

import com.nopo.cheonnyeon.domain.model.Store
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TasteRecommenderTest {
    private fun s(id: String, name: String, sigungu: String, menu: List<String> = emptyList(), sido: String = "서울", photo: Boolean = true, year: Int? = 1980) =
        Store(id = id, name = name, sigungu = sigungu, sido = sido, menuItems = menu, lat = 37.5, lng = 127.0, foundedYear = year, photoUrls = if (photo) listOf("x") else emptyList())

    @Test fun `지역과 음식에 모두 맞는 곳을 먼저 고른다`() {
        val list = listOf(s("1", "가", "종로구", listOf("냉면")), s("2", "나", "종로구", listOf("국밥")), s("3", "다", "마포구", listOf("냉면")))
        assertEquals("1", TasteRecommender.recommend(list, setOf("jongno"), setOf("naengmyeon"))?.id)
    }

    @Test fun `음식이 안 맞으면 지역만 맞는 곳을 고른다`() {
        val list = listOf(s("1", "가", "종로구", listOf("국밥")), s("3", "다", "마포구", listOf("냉면")))
        assertEquals("1", TasteRecommender.recommend(list, setOf("jongno"), setOf("naengmyeon"))?.id)
    }

    @Test fun `부산은 시도 이름으로 맞춘다`() {
        val list = listOf(s("1", "가", "중구", sido = "부산광역시", menu = listOf("돼지국밥")), s("2", "나", "중구"))
        assertEquals("1", TasteRecommender.recommend(list, setOf("busan"), emptySet())?.id)
    }

    @Test fun `고른 것이 없으면 사진 있는 곳 중에서 고르고 후보가 없으면 null`() {
        val list = listOf(s("1", "가", "종로구", photo = false), s("2", "나", "마포구"))
        assertEquals("2", TasteRecommender.recommend(list, emptySet(), emptySet())?.id)
        assertNull(TasteRecommender.recommend(emptyList(), emptySet(), emptySet()))
    }

    @Test fun `평점이 높은 곳이 먼저`() {
        val list = listOf(s("1", "가", "종로구"), s("2", "나", "종로구"))
        assertEquals("2", TasteRecommender.recommend(list, setOf("jongno"), emptySet(), mapOf("1" to 3.5, "2" to 4.7))?.id)
    }
}
