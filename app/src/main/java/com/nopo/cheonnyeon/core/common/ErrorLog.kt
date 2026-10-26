package com.nopo.cheonnyeon.core.common

import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.nopo.cheonnyeon.BuildConfig

/** 인증 실패를 Crashlytics에 비치명으로 남기기 위한 표지. 메시지에는 오류 코드만 들어간다. */
class VerifyFailure(code: String) : Exception("verify_fail:$code")

/**
 * PRD 11장: 인증 실패 비치명 로그(오류 코드만, 개인정보·좌표·영수증 내용 제외).
 * 디버그 빌드와 Firebase가 없는 환경에서는 아무것도 하지 않는다.
 */
object ErrorLog {
    fun verifyFailed(error: AppError) {
        if (BuildConfig.DEBUG) return
        runCatching { FirebaseCrashlytics.getInstance().recordException(VerifyFailure(error.code)) }
    }
}
