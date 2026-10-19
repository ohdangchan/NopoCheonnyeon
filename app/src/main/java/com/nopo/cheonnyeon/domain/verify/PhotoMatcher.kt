package com.nopo.cheonnyeon.domain.verify

import com.nopo.cheonnyeon.core.common.AppError
import com.nopo.cheonnyeon.core.common.AppResult
import com.nopo.cheonnyeon.core.common.asFailure
import com.nopo.cheonnyeon.core.common.asSuccess
import com.nopo.cheonnyeon.core.util.PhoneNumber
import com.nopo.cheonnyeon.core.util.Similarity
import com.nopo.cheonnyeon.domain.model.Store
import com.nopo.cheonnyeon.domain.rule.VerificationConfig

/** 간판 사진 인증 판정 (FR-VER-09~12): 글자 일치와 위치 판정은 따로, 위치 오류는 정확도가 먼저. */
object PhotoMatcher {
    /** 성공 값은 간판 글자 유사도 (전화로 통과하면 1.0) */
    fun matchText(lines: List<String>, store: Store): AppResult<Double> {
        val joined = lines.joinToString("") // 두 줄로 나뉜 이름을 이어 붙인다
        val phone = ReceiptParser.parse(lines.joinToString("\n")).phone
        if (PhoneNumber.same(phone, store.phone)) return 1.0.asSuccess()
        val best = store.nameCandidates.maxOf { Similarity.partial(it, joined) }
        return if (best >= VerificationConfig.PHOTO_NAME_SIM_MIN) best.asSuccess() else AppError.E_PHOTO_NO_MATCH.asFailure()
    }

    fun judgeLocation(distanceM: Double, accuracyM: Double): AppError? = when {
        accuracyM > VerificationConfig.PHOTO_MAX_ACCURACY_M -> AppError.E_ACCURACY
        distanceM > VerificationConfig.PHOTO_MAX_DISTANCE_M -> AppError.E_FAR
        else -> null
    }

    fun canOfferLocationOnly(failedAttempts: Int, withinRange: Boolean, allow: Boolean): Boolean =
        allow && withinRange && failedAttempts >= VerificationConfig.PHOTO_OCR_MAX_ATTEMPTS
}
