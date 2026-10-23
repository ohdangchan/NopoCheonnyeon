package com.nopo.cheonnyeon.domain.rule

import com.nopo.cheonnyeon.domain.model.Store
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class RoulettePickerTest {
    private fun s(id: String, name: String, menu: List<String> = emptyList(), sido: String = "서울", lat: Double = 37.5, lng: Double = 127.0, photo: Boolean = true) =
        Store(id = id, name = name, menuItems = menu, sido = sido, lat = lat, lng = lng, photoUrls = if (photo) listOf("x") else emptyList())

    @Test fun `음식 종류를 단어로 나눈다`() {
        assertEquals("중식", RoulettePicker.cuisineOf(s("1", "홍콩반점", listOf("짜장면"))))
        assertEquals("일식", RoulettePicker.cuisineOf(s("2", "스시집", listOf("초밥"))))
        assertEquals("분식", RoulettePicker.cuisineOf(s("3", "엄마분식", listOf("떡볶이"))))
        assertEquals("카페", RoulettePicker.cuisineOf(s("4", "나폴레옹과자점", listOf("빵"))))
        assertEquals("한식", RoulettePicker.cuisineOf(s("5", "진미옥설농탕", listOf("설렁탕"))))
    }

    @Test fun `지역과 음식 종류로 거른다`() {
        val list = listOf(s("1", "가", listOf("짜장면")), s("2", "나", listOf("짜장면"), sido = "부산"), s("3", "다", listOf("설렁탕")))
        assertEquals(listOf("1"), RoulettePicker.candidates(list, RouletteFilter("서울", "중식", null), null).map { it.id })
    }

    @Test fun `위치가 없으면 거리 조건을 무시한다`() {
        val list = listOf(s("1", "가"), s("2", "나", lat = 35.0, lng = 129.0))
        assertEquals(2, RoulettePicker.candidates(list, RouletteFilter(walkMeters = 500), null).size)
        assertEquals(1, RoulettePicker.candidates(list, RouletteFilter(walkMeters = 500), 37.5 to 127.0).size)
    }

    @Test fun `후보가 없으면 null 이고 있으면 후보 중 하나`() {
        assertNull(RoulettePicker.pick(emptyList(), emptyMap()))
        val list = listOf(s("1", "가"), s("2", "나", photo = false))
        repeat(20) { assertTrue(RoulettePicker.pick(list, emptyMap(), Random(it))!!.id in setOf("1", "2")) }
        assertNotNull(RoulettePicker.pick(list, mapOf("1" to 5.0), Random(1)))
    }

    @Test fun `휠 칸을 고른다`() {
        assertEquals("국밥", RoulettePicker.wheelSegment(s("1", "가", listOf("순대국"))))
        assertEquals("냉면", RoulettePicker.wheelSegment(s("2", "나", listOf("평양냉면"))))
        assertEquals("고기", RoulettePicker.wheelSegment(s("3", "다", listOf("삼겹살"))))
        assertEquals("시장맛집", RoulettePicker.wheelSegment(s("4", "라", listOf("비빔밥"))))
    }
}
