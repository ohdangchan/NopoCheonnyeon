package com.nopo.cheonnyeon.platform.share

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QrCodeTest {
    @Test fun `내려받기 주소로 정사각 행렬이 만들어지고 왼쪽 위 탐지 무늬가 있다`() {
        val m = QrCode.matrix(ShareConfig.DOWNLOAD_URL)
        assertEquals(m.width, m.height)
        assertTrue(m.width >= 21)
        // 탐지 무늬: 모서리의 7x7 테두리는 모두 어둡다
        for (i in 0 until 7) {
            assertTrue(m.get(i, 0)); assertTrue(m.get(0, i)); assertTrue(m.get(i, 6)); assertTrue(m.get(6, i))
        }
    }

    @Test fun `내려받기 주소는 https`() = assertTrue(ShareConfig.DOWNLOAD_URL.startsWith("https://"))
}
