package com.nopo.cheonnyeon.core.navigation

/** 화면 경로 (아키텍처 3장). 가게·인증 화면은 E5~E6에서 추가 */
object Routes {
    const val SPLASH = "splash"
    const val LOGIN = "login"
    const val SIGN_UP = "sign_up"
    const val RESET = "reset"
    const val NICKNAME = "nickname"
    const val HOME = "home"
    const val DIAGNOSTICS = "diagnostics"

    /** 로그인 전에 열 수 있는 화면 (FR-AUTH-06) */
    val signedOutRoutes = setOf(LOGIN, SIGN_UP, RESET)
}
