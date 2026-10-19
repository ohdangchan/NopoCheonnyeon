package com.nopo.cheonnyeon.domain.rule

/**
 * 인증 기준값 (PRD 6.3). 이 값들은 코드 어디에서도 다시 쓰지 않고 여기서만 읽는다.
 * 조정할 때는 이 파일만 고친다 (PRD 6.5).
 */
object VerificationConfig {
    const val RECEIPT_MAX_AGE_HOURS = 24
    const val RECEIPT_FUTURE_TOLERANCE_MIN = 10
    const val RECEIPT_NAME_SIM_MIN = 0.70
    const val PHOTO_MAX_DISTANCE_M = 30.0
    const val PHOTO_MAX_ACCURACY_M = 30.0
    const val PHOTO_NAME_SIM_MIN = 0.60
    const val PHOTO_OCR_MAX_ATTEMPTS = 3
    const val ALLOW_LOCATION_ONLY_AFTER_OCR_FAIL = true
    const val LOCATION_TIMEOUT_MS = 15_000L
    const val DAILY_LIMIT = 5

    /** 위치 측정 중 이 정확도 이하면 15초를 기다리지 않고 끝낸다 (아키텍처 5.2) */
    const val LOCATION_EARLY_STOP_ACCURACY_M = 10.0
    const val LOCATION_INTERVAL_MS = 1_000L
}
