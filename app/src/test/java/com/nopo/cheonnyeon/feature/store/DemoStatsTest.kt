package com.nopo.cheonnyeon.feature.store

import com.nopo.cheonnyeon.data.remote.DemoReviews
import com.nopo.cheonnyeon.data.remote.ReviewItem
import com.nopo.cheonnyeon.data.remote.StoreStats
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DemoStatsTest {
    private fun demo(visits: Int, uid: String = DemoReviews.DEMO_UID) = ReviewItem("d", "S", "", uid, "예시", "SPROUT", 5, "", visits, 0L, demo = true)

    @Test fun `예시 리뷰어를 방문자로 단골 2회 이상을 재방문자로 더한다`() {
        val r = mergeDemoStats(StoreStats(2, 1), listOf(demo(1), demo(2), demo(3), demo(1)))
        assertEquals(StoreStats(6, 3), r)
    }

    @Test fun `예시가 없거나 내 예시 리뷰뿐이면 서버 값 그대로`() {
        assertNull(mergeDemoStats(null, emptyList()))
        assertEquals(StoreStats(2, 1), mergeDemoStats(StoreStats(2, 1), listOf(demo(1, uid = DemoReviews.DEMO_ME_UID))))
    }
}
