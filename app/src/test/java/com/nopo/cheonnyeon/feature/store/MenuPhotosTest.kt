package com.nopo.cheonnyeon.feature.store

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MenuPhotosTest {
    @Test fun `메뉴 이름에서 요리 종류를 찾는다`() {
        assertEquals("seolleongtang", MenuPhotos.keyFor("설렁탕"))
        assertEquals("naengmyeon", MenuPhotos.keyFor("평양냉면"))
        assertEquals("kalguksu", MenuPhotos.keyFor("바지락칼국수"))
        assertEquals("jajang", MenuPhotos.keyFor("짜장면"))
        assertEquals("gogi", MenuPhotos.keyFor("삼겹살"))
    }

    @Test fun `모르는 메뉴는 사진이 없다`() {
        assertNull(MenuPhotos.keyFor("크림빵"))
        assertNull(MenuPhotos.urlFor("커피"))
    }
}
