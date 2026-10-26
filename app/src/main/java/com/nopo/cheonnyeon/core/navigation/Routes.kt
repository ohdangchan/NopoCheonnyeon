package com.nopo.cheonnyeon.core.navigation

import android.net.Uri

/** 화면 경로 (아키텍처 3장). 값에 {id} 처럼 중괄호가 있으면 인자를 받는 경로이고, 아래 함수로 실제 경로를 만든다. */
object Routes {
    const val SPLASH = "splash"
    const val ONBOARDING = "onboarding"
    const val LOGIN = "login"
    const val SIGN_UP = "sign_up"
    const val RESET = "reset"
    const val NICKNAME = "nickname"
    const val HOME = "home"
    const val STORE = "store/{id}"
    fun store(id: String) = "store/$id"
    const val VERIFY = "verify/{id}"
    fun verify(id: String) = "verify/$id"
    const val REVIEW = "review/{id}"
    fun review(id: String) = "review/$id"
    const val PROFILE = "profile/{uid}"
    fun profile(uid: String) = "profile/$uid"
    const val DIAGNOSTICS = "diagnostics"
    const val HALL = "hall"
    const val NOTIFICATIONS = "notifications"
    const val ROULETTE = "roulette"
    const val CHAT = "chat?q={q}"
    fun chat(q: String = "") = if (q.isBlank()) "chat" else "chat?q=" + Uri.encode(q)

    /** 로그인 전에 열 수 있는 화면 (FR-AUTH-06) */
    val signedOutRoutes = setOf(LOGIN, SIGN_UP, RESET)
}
