package com.nopo.cheonnyeon.domain.verify

import com.nopo.cheonnyeon.core.common.AppError
import com.nopo.cheonnyeon.core.common.AppResult
import com.nopo.cheonnyeon.core.common.SeoulZone
import com.nopo.cheonnyeon.domain.model.Store
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

class ReceiptMatcherTest {
    private val now = LocalDateTime.of(2026, 10, 17, 12, 31).atZone(SeoulZone).toInstant()
    private val store = Store(id = "BN-1", name = "가나다라", phone = "021234567")

    private fun receipt(
        phone: String? = null,
        name: String? = null,
        paidAt: LocalDateTime? = LocalDateTime.of(2026, 10, 17, 9, 31),
    ) = ParsedReceipt(phone = phone, businessNo = null, paidAt = paidAt, amount = null, name = name)

    private fun error(r: ParsedReceipt, s: Store = store) = ReceiptMatcher.match(r, s, now).errorOrNull

    @Test fun `U-208-1 전화 일치 그리고 3시간 전이면 성공`() {
        val result = ReceiptMatcher.match(receipt(phone = "02-123-4567"), store, now)
        assertTrue(result is AppResult.Success)
        assertTrue((result as AppResult.Success).value.byPhone)
    }

    @Test fun `U-208-2 전화가 없고 상호 유사도 0점75면 성공`() {
        val result = ReceiptMatcher.match(receipt(name = "가나다마"), store, now)
        assertTrue(result is AppResult.Success)
        assertEquals(0.75, (result as AppResult.Success).value.nameSimilarity, 1e-9)
    }

    @Test fun `U-208-3 전화 불일치 그리고 상호 0점70 미만이면 불일치`() =
        assertEquals(AppError.E_RECEIPT_MISMATCH, error(receipt(phone = "02-999-9999", name = "마바사아")))

    @Test fun `U-208-3 전화가 달라도 상호가 맞으면 성공 - 번호 변경과 여러 번호 대비`() =
        assertTrue(ReceiptMatcher.match(receipt(phone = "02-999-9999", name = "가나다라"), store, now) is AppResult.Success)

    @Test fun `아무 정보도 못 읽으면 불일치`() = assertEquals(AppError.E_RECEIPT_MISMATCH, error(receipt()))

    @Test fun `별칭과도 비교한다`() {
        val s = store.copy(aliases = listOf("예시식당"))
        assertTrue(ReceiptMatcher.match(receipt(name = "예시식당"), s, now) is AppResult.Success)
    }

    @Test fun `U-208-4 25시간 전이면 기간 지남`() =
        assertEquals(AppError.E_RECEIPT_EXPIRED, error(receipt(phone = "021234567", paidAt = LocalDateTime.of(2026, 10, 16, 11, 31))))

    @Test fun `24시간 정각 전은 허용`() =
        assertTrue(ReceiptMatcher.match(receipt(phone = "021234567", paidAt = LocalDateTime.of(2026, 10, 16, 12, 31)), store, now) is AppResult.Success)

    @Test fun `U-208-5 미래 15분은 실패 - 허용 오차 10분 초과`() =
        assertEquals(AppError.E_RECEIPT_EXPIRED, error(receipt(phone = "021234567", paidAt = LocalDateTime.of(2026, 10, 17, 12, 46))))

    @Test fun `U-208-5 미래 5분은 기기 시계 오차로 허용`() =
        assertTrue(ReceiptMatcher.match(receipt(phone = "021234567", paidAt = LocalDateTime.of(2026, 10, 17, 12, 36)), store, now) is AppResult.Success)

    @Test fun `U-208-6 날짜를 못 읽으면 날짜 없음`() =
        assertEquals(AppError.E_RECEIPT_NO_DATE, error(receipt(phone = "021234567", paidAt = null)))

    @Test fun `가게 확인이 날짜 확인보다 먼저`() =
        assertEquals(AppError.E_RECEIPT_MISMATCH, error(receipt(phone = "02-999-9999", name = "마바사아", paidAt = null)))

    @Test fun `U-208-7 위치 값이 전혀 없어도 끝까지 판정된다 - 판정 입력에 위치가 없다`() =
        assertTrue(ReceiptMatcher.match(receipt(phone = "021234567"), store, now) is AppResult.Success)

    @Test fun `U-208-8 상호 유사도 0점69는 불일치 0점70은 성공`() {
        val longStore = store.copy(name = "가".repeat(100), phone = null)
        val at69 = receipt(name = "가".repeat(69) + "나".repeat(31))
        val at70 = receipt(name = "가".repeat(70) + "나".repeat(30))
        assertEquals(AppError.E_RECEIPT_MISMATCH, error(at69, longStore))
        assertFalse(error(at70, longStore) != null)
    }
}
