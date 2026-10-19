package com.nopo.cheonnyeon.domain.rule

enum class ReviewInvalid { RATING_RANGE, TEXT_TOO_LONG, TOO_MANY_LINES }

sealed interface ReviewCheck {
    /** [text]는 앞뒤 공백을 지운 값. 공백뿐이면 빈 문자열(별점만 저장, FR-REV-01) */
    data class Valid(val rating: Int, val text: String) : ReviewCheck
    data class Invalid(val reason: ReviewInvalid) : ReviewCheck
}

/** 평점·리뷰 입력 규칙 (PRD 6.4, FR-REV-01·07) */
object ReviewRules {
    const val RATING_MIN = 1
    const val RATING_MAX = 5
    const val TEXT_MAX = 500
    const val MAX_LINES = 10
    const val PAGE_SIZE = 20

    fun validate(rating: Int, text: String): ReviewCheck {
        if (rating !in RATING_MIN..RATING_MAX) return ReviewCheck.Invalid(ReviewInvalid.RATING_RANGE)
        val trimmed = text.trim()
        if (trimmed.length > TEXT_MAX) return ReviewCheck.Invalid(ReviewInvalid.TEXT_TOO_LONG)
        if (trimmed.lines().size > MAX_LINES) return ReviewCheck.Invalid(ReviewInvalid.TOO_MANY_LINES)
        return ReviewCheck.Valid(rating, trimmed)
    }
}
