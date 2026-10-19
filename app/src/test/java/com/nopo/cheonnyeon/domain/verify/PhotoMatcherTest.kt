package com.nopo.cheonnyeon.domain.verify

import com.nopo.cheonnyeon.core.common.AppError
import com.nopo.cheonnyeon.domain.model.Store
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PhotoMatcherTest {
    private val store = Store(id = "BN-1", name = "예시설렁탕", phone = "021234567", aliases = listOf("예시식당"))

    private fun ok(lines: List<String>, s: Store = store) = PhotoMatcher.matchText(lines, s).errorOrNull == null

    @Test fun `U-210-1 간판 줄에 가게명이 있으면 통과`() = assertTrue(ok(listOf("예시설렁탕 1964")))

    @Test fun `U-210-1 두 줄로 나뉜 이름은 이어 붙여 통과`() = assertTrue(ok(listOf("예시", "설렁탕")))

    @Test fun `U-210-1 별칭도 통과`() = assertTrue(ok(listOf("예시식당")))

    @Test fun `U-210-2 전화번호만 보여도 통과`() = assertTrue(ok(listOf("TEL 02-123-4567")))

    @Test fun `U-210-3 관계없는 글자는 사진에 가게 정보 없음`() =
        assertEquals(AppError.E_PHOTO_NO_MATCH, PhotoMatcher.matchText(listOf("주차금지", "영업중"), store).errorOrNull)

    @Test fun `U-210-3 글자가 0줄이어도 같은 오류`() =
        assertEquals(AppError.E_PHOTO_NO_MATCH, PhotoMatcher.matchText(emptyList(), store).errorOrNull)

    @Test fun `U-210-4 거리 30m 정확도 30m 는 통과`() = assertNull(PhotoMatcher.judgeLocation(30.0, 30.0))

    @Test fun `U-210-4 거리 31m 는 너무 멂`() = assertEquals(AppError.E_FAR, PhotoMatcher.judgeLocation(31.0, 10.0))

    @Test fun `U-210-4 정확도 31m 는 위치가 정확하지 않음`() = assertEquals(AppError.E_ACCURACY, PhotoMatcher.judgeLocation(10.0, 31.0))

    @Test fun `정확도가 나쁘면 거리 판단은 의미가 없어 정확도 오류가 먼저`() =
        assertEquals(AppError.E_ACCURACY, PhotoMatcher.judgeLocation(80.0, 80.0))

    @Test fun `U-210-5 세 번 실패하고 30m 안이면 위치 확인 도장 선택지`() =
        assertTrue(PhotoMatcher.canOfferLocationOnly(failedAttempts = 3, withinRange = true, allow = true))

    @Test fun `U-210-5 설정값이 false면 선택지 없음`() =
        assertFalse(PhotoMatcher.canOfferLocationOnly(failedAttempts = 3, withinRange = true, allow = false))

    @Test fun `두 번 실패나 30m 밖이면 선택지 없음`() {
        assertFalse(PhotoMatcher.canOfferLocationOnly(2, withinRange = true, allow = true))
        assertFalse(PhotoMatcher.canOfferLocationOnly(3, withinRange = false, allow = true))
    }

    @Test fun `U-210-6 글자 유사도 0점59는 실패 0점60은 통과`() {
        val s = Store(id = "BN-2", name = "가".repeat(100))
        assertFalse(ok(listOf("가".repeat(59) + "나".repeat(41)), s))
        assertTrue(ok(listOf("가".repeat(60) + "나".repeat(40)), s))
    }
}
