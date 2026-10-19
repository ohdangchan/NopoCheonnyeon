package com.nopo.cheonnyeon.core.util

import org.junit.Assert.assertEquals
import org.junit.Test

class TextNormalizerTest {
    @Test fun `공백을 없앤다`() = assertEquals("예시설렁탕", TextNormalizer.normalize("예시 설렁탕"))

    @Test fun `주 표기와 본점 원조를 없앤다`() {
        assertEquals("예시식당", TextNormalizer.normalize("(주)예시식당 본점"))
        assertEquals("예시식당", TextNormalizer.normalize("㈜예시식당"))
        assertEquals("예시식당", TextNormalizer.normalize("주식회사 예시식당"))
        assertEquals("예시식당", TextNormalizer.normalize("원조 예시식당"))
    }

    @Test fun `영문은 소문자로 특수문자는 없앤다`() = assertEquals("abccafe", TextNormalizer.normalize("ABC-Cafe!"))

    @Test fun `U-205-4 빈 문자열 공백 이모지 한자에서 예외가 나지 않는다`() {
        assertEquals("", TextNormalizer.normalize(""))
        assertEquals("", TextNormalizer.normalize("   \n\t"))
        assertEquals("老鋪食堂", TextNormalizer.normalize("🍜 老鋪食堂 🍜"))
    }
}
