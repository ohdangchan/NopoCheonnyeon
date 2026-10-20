package com.nopo.cheonnyeon.core.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class ClockSkewTest {
    private val header = "Tue, 20 Oct 2026 03:00:00 GMT"
    private val serverMillis = ZonedDateTime.parse(header, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant().toEpochMilli()

    @Test fun `같은 시각이면 차이 0`() = assertEquals(0L, ClockSkew.skewMillis(header, serverMillis))

    @Test fun `기기가 5분 빠르면 플러스 5분`() = assertEquals(300_000L, ClockSkew.skewMillis(header, serverMillis + 300_000L))

    @Test fun `기기가 늦으면 마이너스`() = assertEquals(-60_000L, ClockSkew.skewMillis(header, serverMillis - 60_000L))

    @Test fun `읽을 수 없는 헤더는 null`() {
        assertNull(ClockSkew.skewMillis(null, 0))
        assertNull(ClockSkew.skewMillis("", 0))
        assertNull(ClockSkew.skewMillis("어제 오후", 0))
    }

    @Test fun `10분까지는 허용하고 넘으면 경고`() {
        assertFalse(ClockSkew.exceedsWarning(10 * 60_000L))
        assertTrue(ClockSkew.exceedsWarning(10 * 60_000L + 1))
        assertTrue(ClockSkew.exceedsWarning(-11 * 60_000L))
    }
}
