package com.nopo.cheonnyeon.domain.rule

import com.nopo.cheonnyeon.core.common.AppError
import com.nopo.cheonnyeon.core.common.SeoulZone
import com.nopo.cheonnyeon.domain.model.VisitRef
import java.time.Instant

/** 하루 5곳, 같은 가게 하루 1회 (FR-VER-14). 날짜는 기기 시간대와 상관없이 서울 자정 기준. */
object DailyLimitPolicy {
    /**
     * 테스트 가게는 한도와 같은 가게 제한에서 제외하고,
     * 테스트 방문은 실제 집계에도 넣지 않는다 (FR-TST-02).
     * @return 인증할 수 없으면 이유, 가능하면 null
     */
    fun check(visits: List<VisitRef>, storeId: String, isTestStore: Boolean, now: Instant): AppError? {
        if (isTestStore) return null
        val today = now.atZone(SeoulZone).toLocalDate()
        val visitedToday = visits
            .filter { !it.isTest && it.at.atZone(SeoulZone).toLocalDate() == today }
            .map { it.storeId }
            .toSet()
        return when {
            storeId in visitedToday -> AppError.E_ALREADY_TODAY
            visitedToday.size >= VerificationConfig.DAILY_LIMIT -> AppError.E_DAILY_LIMIT
            else -> null
        }
    }
}
