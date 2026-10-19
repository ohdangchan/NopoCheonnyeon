package com.nopo.cheonnyeon.domain.rule

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReviewRulesTest {
    private fun invalid(rating: Int, text: String) = (ReviewRules.validate(rating, text) as? ReviewCheck.Invalid)?.reason

    @Test fun `U-212-1 별점 0과 6은 거부`() {
        assertEquals(ReviewInvalid.RATING_RANGE, invalid(0, ""))
        assertEquals(ReviewInvalid.RATING_RANGE, invalid(6, ""))
    }

    @Test fun `U-212-1 별점 1에서 5는 허용`() {
        (1..5).forEach { assertTrue(ReviewRules.validate(it, "") is ReviewCheck.Valid) }
    }

    @Test fun `U-212-1 글 500자는 허용 501자는 거부`() {
        assertTrue(ReviewRules.validate(5, "가".repeat(500)) is ReviewCheck.Valid)
        assertEquals(ReviewInvalid.TEXT_TOO_LONG, invalid(5, "가".repeat(501)))
    }

    @Test fun `U-212-1 공백만 있는 글은 빈 글로 처리해 별점만 저장`() {
        val r = ReviewRules.validate(4, "  \n  \t ") as ReviewCheck.Valid
        assertEquals("", r.text)
        assertEquals(4, r.rating)
    }

    @Test fun `U-212-1 줄바꿈 10줄은 허용 11줄은 거부`() {
        assertTrue(ReviewRules.validate(5, (1..10).joinToString("\n") { "줄$it" }) is ReviewCheck.Valid)
        assertEquals(ReviewInvalid.TOO_MANY_LINES, invalid(5, (1..11).joinToString("\n") { "줄$it" }))
    }

    @Test fun `앞뒤 공백은 지우고 저장`() = assertEquals("맛있다", (ReviewRules.validate(5, "  맛있다 \n") as ReviewCheck.Valid).text)

    @Test fun `별점 오류가 글 오류보다 먼저`() = assertEquals(ReviewInvalid.RATING_RANGE, invalid(0, "가".repeat(501)))
}
