package com.nopo.cheonnyeon.core.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SimilarityTest {
    @Test fun `U-205-1 띄어쓰기만 다르면 1점0`() = assertEquals(1.0, Similarity.names("예시 설렁탕", "예시설렁탕"), 0.0)

    @Test fun `U-205-2 주 표기와 본점을 빼면 1점0`() =
        assertEquals(1.0, Similarity.names("(주)예시식당 본점", "예시식당"), 0.0)

    @Test fun `U-205-3 다른 가게는 0점6 미만`() = assertTrue(Similarity.names("예시설렁탕", "예시냉면") < 0.6)

    @Test fun `U-205-4 빈 값은 일치로 보지 않는다`() {
        assertEquals(0.0, Similarity.names("", ""), 0.0)
        assertEquals(0.0, Similarity.names("   ", "가"), 0.0)
        assertEquals(0.0, Similarity.names("가나다", ""), 0.0)
        assertEquals(0.0, Similarity.names("🍜", "🍜"), 0.0)
    }

    @Test fun `한 글자 바꾸면 비율만큼 낮아진다`() = assertEquals(0.75, Similarity.names("가나다라", "가나다마"), 1e-9)

    @Test fun `인접한 두 글자가 바뀐 것은 한 번의 수정`() = assertEquals(0.75, Similarity.names("가나다라", "나가다라"), 1e-9)
}
