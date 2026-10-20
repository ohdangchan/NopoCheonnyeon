package com.nopo.cheonnyeon.data.catalog

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** U-301-1: stores.json 읽기 (알 수 없는 키 무시, 없는 값은 기본값) */
class StoreJsonTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test fun `가게 이야기가 있으면 도메인 모델까지 전달된다`() {
        val s = json.decodeFromString<StoreJson>("""{"id":"BN-1","name":"백년식당","story":"첫 줄\n둘째 줄"}""").toDomain()
        assertEquals("첫 줄\n둘째 줄", s.story)
    }

    @Test fun `가게 이야기가 없으면 null`() {
        assertNull(json.decodeFromString<StoreJson>("""{"id":"OR-1","name":"오래식당"}""").toDomain().story)
    }

    @Test fun `알 수 없는 키는 무시하고 좌표 null을 허용한다`() {
        val s = json.decodeFromString<StoreJson>("""{"id":"X","name":"가","unknown":123,"lat":null}""").toDomain()
        assertNull(s.lat)
        assertEquals("가", s.name)
    }

    @Test fun `전체 파일 형식을 읽는다`() {
        val c = json.decodeFromString<CatalogJson>("""{"version":26102013,"count":1,"stores":[{"id":"BN-1","name":"가게"}]}""")
        assertEquals(26102013L, c.version)
        assertEquals(1, c.stores.size)
    }
}
