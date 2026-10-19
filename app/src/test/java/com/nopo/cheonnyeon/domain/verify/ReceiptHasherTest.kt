package com.nopo.cheonnyeon.domain.verify

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import java.time.LocalDateTime

class ReceiptHasherTest {
    private fun parsed(
        biz: String? = "1234567891",
        phone: String? = "021234567",
        at: LocalDateTime? = LocalDateTime.of(2026, 10, 17, 12, 31, 5),
        amount: Long? = 12_000,
    ) = ParsedReceipt(phone = phone, businessNo = biz, paidAt = at, amount = amount, name = null)

    @Test fun `U-209-1 같은 입력은 같은 해시`() = assertEquals(ReceiptHasher.hash(parsed(), "BN-1"), ReceiptHasher.hash(parsed(), "BN-1"))

    @Test fun `U-209-1 금액이 1원 다르면 다른 해시`() =
        assertNotEquals(ReceiptHasher.hash(parsed(amount = 12_000), "BN-1"), ReceiptHasher.hash(parsed(amount = 12_001), "BN-1"))

    @Test fun `U-209-1 초 단위 차이는 같은 해시`() =
        assertEquals(
            ReceiptHasher.hash(parsed(at = LocalDateTime.of(2026, 10, 17, 12, 31, 5)), "BN-1"),
            ReceiptHasher.hash(parsed(at = LocalDateTime.of(2026, 10, 17, 12, 31, 55)), "BN-1"),
        )

    @Test fun `분이 다르면 다른 해시`() =
        assertNotEquals(
            ReceiptHasher.hash(parsed(at = LocalDateTime.of(2026, 10, 17, 12, 31)), "BN-1"),
            ReceiptHasher.hash(parsed(at = LocalDateTime.of(2026, 10, 17, 12, 32)), "BN-1"),
        )

    @Test fun `해시는 소문자 16진수 64자`() {
        val h = ReceiptHasher.hash(parsed(), "BN-1")
        assertEquals(64, h.length)
        assertEquals(h.lowercase(), h)
    }

    @Test fun `사업자번호가 없으면 전화번호로 만든다`() =
        assertNotEquals(ReceiptHasher.hash(parsed(biz = null, phone = "021234567"), "BN-1"), ReceiptHasher.hash(parsed(biz = null, phone = "021234568"), "BN-1"))

    @Test fun `둘 다 없으면 가게 id로 만들어 가게가 다르면 해시도 다르다`() =
        assertNotEquals(ReceiptHasher.hash(parsed(biz = null, phone = null), "BN-1"), ReceiptHasher.hash(parsed(biz = null, phone = null), "BN-2"))

    @Test fun `금액을 못 읽은 영수증도 해시가 만들어진다`() =
        assertEquals(64, ReceiptHasher.hash(parsed(amount = null), "BN-1").length)
}
