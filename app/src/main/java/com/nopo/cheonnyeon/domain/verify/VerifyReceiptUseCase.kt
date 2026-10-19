package com.nopo.cheonnyeon.domain.verify

import com.nopo.cheonnyeon.core.common.AppError
import com.nopo.cheonnyeon.core.common.AppResult
import com.nopo.cheonnyeon.core.common.asFailure
import com.nopo.cheonnyeon.core.common.asSuccess
import com.nopo.cheonnyeon.domain.model.Store
import com.nopo.cheonnyeon.domain.model.VisitRef
import com.nopo.cheonnyeon.domain.rule.DailyLimitPolicy
import java.time.Instant

/** 영수증 인증을 통과했을 때 기록에 넘길 값. 영수증 내용은 해시 외에는 남기지 않는다. */
data class ReceiptOutcome(val hash: String)

/**
 * 영수증 글자 → 인증 판정 (아키텍처 5.1, 위치 미사용). 순서: 하루 한도 → 글자 읽기 → 가게·날짜 확인 → 해시.
 * 기기 기록 저장은 부르는 쪽(VisitRepository)이 한다.
 */
object VerifyReceiptUseCase {
    fun verify(lines: List<String>, store: Store, visits: List<VisitRef>, now: Instant): AppResult<ReceiptOutcome> {
        DailyLimitPolicy.check(visits, store.id, store.isTest, now)?.let { return it.asFailure() }
        val text = lines.joinToString("\n").trim()
        if (text.isEmpty()) return AppError.E_RECEIPT_UNREADABLE.asFailure()
        val parsed = ReceiptParser.parse(text)
        ReceiptMatcher.match(parsed, store, now).errorOrNull?.let { return it.asFailure() }
        return ReceiptOutcome(ReceiptHasher.hash(parsed, store.id)).asSuccess()
    }
}
