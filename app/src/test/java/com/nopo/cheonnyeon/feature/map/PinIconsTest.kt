package com.nopo.cheonnyeon.feature.map

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** FR-MAP-02: 100년 이상 가게 핀에는 금빛 테두리가 그려진다 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
@org.robolectric.annotation.GraphicsMode(org.robolectric.annotation.GraphicsMode.Mode.NATIVE)
class PinIconsTest {
    private val fill = 0xFF1F6B63.toInt()
    private val brass = 0xFFD9A22C.toInt()

    /** 핀 몸통 왼쪽 가장자리(원의 가장 왼쪽 점) 근처에서 테두리 색을 읽는다 */
    private fun edgePixels(rim: Int?) = PinIcons.draw(fill, rim).let { bmp -> (6..14).map { x -> bmp.getPixel(x, 42) } }

    @Test fun `금빛 테두리가 있으면 가장자리에 금빛 픽셀이 있다`() {
        assertEquals(true, edgePixels(brass).any { it == brass })
    }

    @Test fun `테두리가 없으면 금빛 픽셀이 없다`() {
        assertEquals(false, edgePixels(null).any { it == brass })
    }

    @Test fun `핀 크기는 일정하고 가운데는 색이 채워진다`() {
        val bmp = PinIcons.draw(fill, brass)
        assertEquals(88, bmp.width)
        assertEquals(112, bmp.height)
        assertNotEquals(0, bmp.getPixel(44, 70))
    }
}
