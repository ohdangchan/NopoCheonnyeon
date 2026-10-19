package com.nopo.cheonnyeon.domain.verify

import com.nopo.cheonnyeon.core.common.AppError
import com.nopo.cheonnyeon.core.common.AppResult
import com.nopo.cheonnyeon.core.common.SeoulZone
import com.nopo.cheonnyeon.core.common.asFailure
import com.nopo.cheonnyeon.core.common.asSuccess
import com.nopo.cheonnyeon.core.util.PhoneNumber
import com.nopo.cheonnyeon.core.util.Similarity
import com.nopo.cheonnyeon.domain.model.Store
import com.nopo.cheonnyeon.domain.rule.VerificationConfig
import java.time.Duration
import java.time.Instant

data class ReceiptMatch(val byPhone: Boolean, val nameSimilarity: Double)

/** 영수증이 이 가게 것이고 24시간 안인지 판정 (FR-VER-05~07). 가게 확인 → 날짜 확인 순서. 위치는 쓰지 않는다. */
object ReceiptMatcher {
    fun match(receipt: ParsedReceipt, store: Store, now: Instant): AppResult<ReceiptMatch> {
        val byPhone = PhoneNumber.same(receipt.phone, store.phone)
        val similarity = receipt.name?.let { n -> store.nameCandidates.maxOf { Similarity.names(n, it) } } ?: 0.0
        if (!byPhone && similarity < VerificationConfig.RECEIPT_NAME_SIM_MIN) return AppError.E_RECEIPT_MISMATCH.asFailure()

        val paidAt = receipt.paidAt ?: return AppError.E_RECEIPT_NO_DATE.asFailure()
        val age = Duration.between(paidAt.atZone(SeoulZone).toInstant(), now)
        val tooOld = age > Duration.ofHours(VerificationConfig.RECEIPT_MAX_AGE_HOURS.toLong())
        val tooFuture = age.isNegative && age.negated() > Duration.ofMinutes(VerificationConfig.RECEIPT_FUTURE_TOLERANCE_MIN.toLong())
        if (tooOld || tooFuture) return AppError.E_RECEIPT_EXPIRED.asFailure()
        return ReceiptMatch(byPhone, similarity).asSuccess()
    }
}
