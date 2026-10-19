package com.nopo.cheonnyeon.core.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GeoDistanceTest {
    private val earthRadius = 6_371_000.0

    /** 북쪽으로 정확히 [meters]만큼 떨어진 위도 */
    private fun northOf(lat: Double, meters: Double) = lat + Math.toDegrees(meters / earthRadius)

    @Test fun `U-204-1 같은 좌표는 0m`() = assertEquals(0.0, GeoDistance.meters(37.5663, 126.9779, 37.5663, 126.9779), 0.0)

    @Test fun `U-204-2 서울시청에서 광화문까지 약 1071m 오차 1퍼센트`() {
        val d = GeoDistance.meters(37.5663, 126.9779, 37.5759, 126.9769)
        assertEquals(1071.0, d, 1071.0 * 0.01)
    }

    @Test fun `U-204-3 29점9 30점0 30점1 미터를 구분한다`() {
        val lat = 37.5663
        val d299 = GeoDistance.meters(lat, 126.9779, northOf(lat, 29.9), 126.9779)
        val d300 = GeoDistance.meters(lat, 126.9779, northOf(lat, 30.0), 126.9779)
        val d301 = GeoDistance.meters(lat, 126.9779, northOf(lat, 30.1), 126.9779)
        assertEquals(29.9, d299, 0.001)
        assertEquals(30.0, d300, 0.001)
        assertEquals(30.1, d301, 0.001)
        assertTrue(d299 < 30.0 && d301 > 30.0)
    }

    @Test fun `방향이 반대여도 같은 거리`() {
        val a = GeoDistance.meters(37.5663, 126.9779, 37.5759, 126.9769)
        val b = GeoDistance.meters(37.5759, 126.9769, 37.5663, 126.9779)
        assertEquals(a, b, 1e-9)
    }
}
