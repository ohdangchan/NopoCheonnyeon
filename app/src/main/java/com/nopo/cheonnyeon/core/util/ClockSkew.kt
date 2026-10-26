package com.nopo.cheonnyeon.core.util

import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.abs

/**
 * 기기 시계가 서버 시각과 얼마나 어긋났는지 (아키텍처 9장).
 * 영수증 24시간 판정이 기기 시계를 쓰기 때문에 10분 넘게 어긋나면 경고한다.
 */
object ClockSkew {
    /** 이 값(분)을 넘게 어긋나면 경고한다. 영수증의 미래 허용 오차(RECEIPT_FUTURE_TOLERANCE_MIN)와 같게 둔다 */
    const val WARN_MINUTES = 10L
    private const val MILLIS_PER_MINUTE = 60_000L

    /** HTTP Date 헤더(RFC 1123)와 기기 현재 시각(epoch ms)의 차이(ms, 기기가 빠르면 +). 읽을 수 없으면 null */
    fun skewMillis(serverDateHeader: String?, deviceNowMillis: Long): Long? {
        if (serverDateHeader.isNullOrBlank()) return null
        val server = try {
            ZonedDateTime.parse(serverDateHeader.trim(), DateTimeFormatter.RFC_1123_DATE_TIME).toInstant().toEpochMilli()
        } catch (_: Exception) {
            return null
        }
        return deviceNowMillis - server
    }

    fun exceedsWarning(skewMillis: Long): Boolean = abs(skewMillis) > WARN_MINUTES * MILLIS_PER_MINUTE
}
