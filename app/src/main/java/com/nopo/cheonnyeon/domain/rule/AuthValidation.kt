package com.nopo.cheonnyeon.domain.rule

enum class AuthInvalid { EMAIL, PASSWORD_SHORT, PASSWORD_WEAK, PASSWORD_MISMATCH, NICK_LENGTH, NICK_CHARS }

/** 가입·로그인 입력 규칙 (FR-AUTH-01). 통과하면 null, 아니면 이유. */
object AuthValidation {
    private val emailRegex = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")
    private val nickRegex = Regex("^[가-힣A-Za-z0-9]+$")
    const val PASSWORD_MIN = 8
    const val NICK_MIN = 2
    const val NICK_MAX = 12

    fun email(raw: String): AuthInvalid? = if (emailRegex.matches(raw.trim())) null else AuthInvalid.EMAIL

    fun password(pw: String): AuthInvalid? = when {
        pw.length < PASSWORD_MIN -> AuthInvalid.PASSWORD_SHORT
        pw.none { it.isLetter() } || pw.none { it.isDigit() } -> AuthInvalid.PASSWORD_WEAK
        else -> null
    }

    fun passwordConfirm(pw: String, confirm: String): AuthInvalid? = if (pw == confirm) null else AuthInvalid.PASSWORD_MISMATCH

    fun nickname(raw: String): AuthInvalid? = when {
        raw.length !in NICK_MIN..NICK_MAX -> AuthInvalid.NICK_LENGTH
        !nickRegex.matches(raw) -> AuthInvalid.NICK_CHARS
        else -> null
    }

    /** nicknames 컬렉션의 문서 id (대소문자 구분 없이 중복 검사) */
    fun nicknameKey(nick: String): String = nick.lowercase()
}
