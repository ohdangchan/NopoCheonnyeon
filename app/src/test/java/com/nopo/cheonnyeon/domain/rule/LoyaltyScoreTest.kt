package com.nopo.cheonnyeon.domain.rule

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LoyaltyScoreTest {
    private fun peer(rate: Double, visits: Double = 1.5) = LoyaltyInput(20, (20 * rate).toInt(), visits, 20)
    private val peers = (1..10).map { peer(it / 20.0, 1.0 + it / 10.0) }   // 재방문율 5~50%

    @Test fun `백분위는 같은 값을 절반으로 센다`() {
        assertEquals(0.5, LoyaltyScore.percentile(3.0, listOf(1.0, 3.0, 3.0, 5.0)), 1e-9)
        assertEquals(0.5, LoyaltyScore.percentile(1.0, emptyList()), 1e-9)
    }

    @Test fun `재방문율이 높을수록 기준 점수가 높다`() {
        val high = LoyaltyScore.baseline(LoyaltyInput(30, 21, 2.5, 30), peers)
        val low = LoyaltyScore.baseline(LoyaltyInput(30, 2, 1.0, 30), peers)
        assertTrue(high > 80 && low < 20)
    }

    @Test fun `표본이 적으면 50점 쪽으로 보정한다`() {
        val few = LoyaltyScore.baseline(LoyaltyInput(4, 4, 3.0, 4), peers)       // 방문자 4명, 확신 0.2
        val many = LoyaltyScore.baseline(LoyaltyInput(40, 40, 3.0, 40), peers)
        assertTrue(few in 50..65)
        assertTrue(many > 90)
    }

    @Test fun `AI 점수는 기준 점수 12점 안으로 고정된다`() {
        assertEquals(72, LoyaltyScore.clamp(99, 60))
        assertEquals(48, LoyaltyScore.clamp(0, 60))
        assertEquals(100, LoyaltyScore.clamp(150, 95).coerceAtMost(100))
        assertEquals(61, LoyaltyScore.clamp(61, 60))
    }

    @Test fun `상위 퍼센트와 요약`() {
        val top = LoyaltyScore.topPercent(LoyaltyInput(20, 20, 2.0, 20), peers)
        assertTrue(top <= 5)
        val s = LoyaltyScore.summary(peers)
        assertEquals(10, s.count)
        assertTrue(s.meanRate in 0.2..0.35)
    }

    @Test fun `이름표 단계`() {
        assertEquals(listOf(0, 1, 2, 3, 4), listOf(10, 30, 50, 70, 90).map(LoyaltyScore::gradeKey))
    }
}
