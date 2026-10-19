package com.nopo.cheonnyeon.domain.rule

import com.nopo.cheonnyeon.core.common.AppError
import com.nopo.cheonnyeon.domain.model.VisitRef
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant

class DailyLimitPolicyTest {
    /** 서울 2026-10-17 12:00 */
    private val now = Instant.parse("2026-10-17T03:00:00Z")

    private fun visit(id: String, at: String = "2026-10-17T01:00:00Z", test: Boolean = false) = VisitRef(id, Instant.parse(at), test)

    private fun five() = (1..5).map { visit("S$it") }

    @Test fun `U-211-1 오늘 5곳을 채우면 6번째 새 가게는 하루 한도 초과`() =
        assertEquals(AppError.E_DAILY_LIMIT, DailyLimitPolicy.check(five(), "S6", false, now))

    @Test fun `4곳이면 5번째는 가능`() = assertNull(DailyLimitPolicy.check(five().take(4), "S5", false, now))

    @Test fun `U-211-1 같은 가게 두 번째는 오늘 이미 다녀온 가게 - 한도보다 먼저`() =
        assertEquals(AppError.E_ALREADY_TODAY, DailyLimitPolicy.check(five(), "S3", false, now))

    @Test fun `U-211-1 서울 자정 전날 23시30분 기록은 오늘로 세지 않는다 - 기기 시간대와 무관`() {
        val yesterday = (1..5).map { visit("S$it", "2026-10-16T14:30:00Z") } // 서울 10-16 23:30
        assertNull(DailyLimitPolicy.check(yesterday, "S6", false, now))
    }

    @Test fun `U-211-1 서울 자정 직후 00시30분 기록은 오늘로 센다 - UTC 날짜로는 전날`() {
        val justAfter = (1..5).map { visit("S$it", "2026-10-16T15:30:00Z") } // 서울 10-17 00:30
        assertEquals(AppError.E_DAILY_LIMIT, DailyLimitPolicy.check(justAfter, "S6", false, now))
    }

    @Test fun `U-211-2 테스트 가게는 한도와 같은 가게 제한에서 제외`() {
        assertNull(DailyLimitPolicy.check(five(), "TEST-0001", true, now))
        assertNull(DailyLimitPolicy.check(five() + visit("TEST-0001", test = true), "TEST-0001", true, now))
    }

    @Test fun `U-211-2 테스트 방문은 실제 가게 집계에 들어가지 않는다`() {
        val tests = (1..5).map { visit("TEST-000$it", test = true) }
        assertNull(DailyLimitPolicy.check(tests, "S1", false, now))
    }

    @Test fun `같은 날 같은 가게를 여러 번 찍어도 한 곳으로 센다`() {
        val dup = listOf(visit("S1"), visit("S1"), visit("S2"), visit("S3"), visit("S4"))
        assertNull(DailyLimitPolicy.check(dup, "S5", false, now))
    }
}
