package com.nopo.cheonnyeon.domain.verify

import com.nopo.cheonnyeon.core.common.AppError
import com.nopo.cheonnyeon.core.common.AppResult
import com.nopo.cheonnyeon.core.common.SeoulZone
import com.nopo.cheonnyeon.domain.model.Store
import com.nopo.cheonnyeon.domain.model.VisitRef
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

class VerifyReceiptUseCaseTest {
    private val now = LocalDateTime.of(2026, 10, 17, 12, 31).atZone(SeoulZone).toInstant()
    private val store = Store(id = "BN-1", name = "노포천년테스트식당", phone = "02-000-0000")
    private val receipt = listOf("노포천년테스트식당", "TEL 02-000-0000", "판매일시: 2026-10-17 11:20", "합계 12,000")

    private fun err(lines: List<String>, visits: List<VisitRef> = emptyList(), s: Store = store) =
        VerifyReceiptUseCase.verify(lines, s, visits, now).errorOrNull

    @Test fun `정상 영수증은 성공하고 해시를 돌려준다`() {
        val r = VerifyReceiptUseCase.verify(receipt, store, emptyList(), now)
        assertTrue(r is AppResult.Success)
        assertEquals(64, (r as AppResult.Success).value.hash.length)
    }

    @Test fun `글자가 없으면 인식 실패`() = assertEquals(AppError.E_RECEIPT_UNREADABLE, err(emptyList()))

    @Test fun `다른 가게 영수증은 불일치`() = assertEquals(AppError.E_RECEIPT_MISMATCH, err(listOf("전혀다른집", "TEL 031-999-8888", "2026-10-17 11:20")))

    @Test fun `오래된 영수증은 기간 초과`() = assertEquals(AppError.E_RECEIPT_EXPIRED, err(listOf("노포천년테스트식당", "TEL 02-000-0000", "2026-10-10 11:20")))

    @Test fun `날짜가 없으면 날짜 없음`() = assertEquals(AppError.E_RECEIPT_NO_DATE, err(listOf("노포천년테스트식당", "TEL 02-000-0000")))

    @Test fun `오늘 이미 인증한 가게는 한도보다 먼저 막힌다`() =
        assertEquals(AppError.E_ALREADY_TODAY, err(receipt, listOf(VisitRef("BN-1", now, false))))

    @Test fun `하루 5곳이면 한도 초과`() {
        val five = (1..5).map { VisitRef("X$it", now, false) }
        assertEquals(AppError.E_DAILY_LIMIT, err(receipt, five))
    }

    @Test fun `테스트 가게는 한도에서 제외`() {
        val five = (1..5).map { VisitRef("X$it", now, false) }
        assertEquals(null, err(receipt, five, store.copy(isTest = true)))
    }
}
