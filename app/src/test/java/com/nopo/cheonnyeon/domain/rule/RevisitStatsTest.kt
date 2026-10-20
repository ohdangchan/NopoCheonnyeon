package com.nopo.cheonnyeon.domain.rule

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RevisitStatsTest {
    @Test fun `표본이 5명 미만이면 퍼센트를 내지 않는다`() = assertNull(RevisitStats.percent(4, 2))

    @Test fun `5명이면 퍼센트를 낸다`() = assertEquals(40, RevisitStats.percent(5, 2))

    @Test fun `반올림한다`() = assertEquals(33, RevisitStats.percent(6, 2))

    @Test fun `재방문자가 없으면 0퍼센트`() = assertEquals(0, RevisitStats.percent(10, 0))

    @Test fun `전원 재방문이면 100퍼센트`() = assertEquals(100, RevisitStats.percent(8, 8))

    @Test fun `재방문자가 방문자보다 많으면 데이터 오류로 보고 null`() = assertNull(RevisitStats.percent(5, 6))

    @Test fun `방문자 0명은 null`() = assertNull(RevisitStats.percent(0, 0))
}
