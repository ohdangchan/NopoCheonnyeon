package com.nopo.cheonnyeon.core.common

import java.time.Instant
import java.time.ZoneId

/** 시각 판단은 이 인터페이스로만 한다 (테스트에서 고정). 기기 시간대와 무관하게 서울 기준. */
interface Clock {
    fun now(): Instant
}

object SystemClock : Clock {
    override fun now(): Instant = Instant.now()
}

/** 날짜 경계(하루 한도, 방문일 저장)는 항상 서울 기준 (PRD 6장, 아키텍처 9장) */
val SeoulZone: ZoneId = ZoneId.of("Asia/Seoul")
