package com.nopo.cheonnyeon.core.common

import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.nopo.cheonnyeon.BuildConfig

/**
 * PRD 11장 사용 기록(선택). 기능 사용 횟수와 오류 코드만 보내고 닉네임·이메일·위치·영수증·가게 이름은 보내지 않는다.
 * 디버그 빌드에서는 수집하지 않고, Firebase가 없는 환경에서는 아무것도 하지 않는다.
 */
object Analytics {
    private var client: FirebaseAnalytics? = null

    /** 권한 관리의 "앱 사용 데이터 수집" 선택을 따른다 (디버그 빌드는 항상 끔) */
    fun setCollection(on: Boolean) { runCatching { client?.setAnalyticsCollectionEnabled(on && !BuildConfig.DEBUG) } }

    fun init(context: Context) {
        client = runCatching { FirebaseAnalytics.getInstance(context).also { it.setAnalyticsCollectionEnabled(false) } }.getOrNull()
    }

    private fun log(name: String, vararg params: Pair<String, Any>) {
        if (BuildConfig.DEBUG) return
        val c = client ?: return
        runCatching {
            c.logEvent(name, Bundle().apply {
                params.forEach { (k, v) -> if (v is Int) putInt(k, v) else putString(k, v.toString()) }
            })
        }
    }

    fun signUp() = log("sign_up")
    fun login() = log("login")
    fun verifyStart() = log("verify_start")
    fun verifyResult(method: String, result: String, code: String = "") = log("verify_result", "method" to method, "result" to result, "code" to code)
    fun stampAwarded(years: Int, grade: String) = log("stamp_awarded", "years" to years, "grade" to grade)
    fun reviewSaved(hasText: Boolean) = log("review_saved", "has_text" to hasText)
    fun profileOpen() = log("profile_open")
}
