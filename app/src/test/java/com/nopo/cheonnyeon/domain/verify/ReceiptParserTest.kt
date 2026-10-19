package com.nopo.cheonnyeon.domain.verify

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDateTime

class ReceiptParserTest {
    private fun paidAt(text: String) = ReceiptParser.parse(text).paidAt

    @Test fun `U-207-1 날짜 네 자리 연도 하이픈`() =
        assertEquals(LocalDateTime.of(2026, 10, 17, 12, 31), paidAt("판매일시: 2026-10-17 12:31"))

    @Test fun `U-207-1 날짜 점 구분`() =
        assertEquals(LocalDateTime.of(2026, 10, 17, 12, 31), paidAt("2026.10.17 12:31"))

    @Test fun `U-207-1 날짜 두 자리 연도 슬래시`() =
        assertEquals(LocalDateTime.of(2026, 10, 17, 12, 31), paidAt("26/10/17 12:31"))

    @Test fun `U-207-1 시간이 한 자리`() =
        assertEquals(LocalDateTime.of(2026, 10, 17, 9, 5), paidAt("2026-10-17 9:05"))

    @Test fun `U-207-1 요일 괄호와 초가 있어도 인식하고 초는 버린다`() =
        assertEquals(LocalDateTime.of(2026, 10, 17, 12, 31), paidAt("2026-10-17(토) 12:31:05"))

    @Test fun `날짜가 없으면 null`() = assertNull(paidAt("합계 12,000"))

    @Test fun `존재하지 않는 날짜는 건너뛴다`() = assertNull(paidAt("2026-13-45 25:61"))

    @Test fun `U-207-2 OCR이 숫자를 문자로 읽어도 숫자 맥락에서만 보정`() =
        assertEquals("021234567", ReceiptParser.parse("TEL O2-l23-4567").phone)

    @Test fun `U-207-2 일반 단어 속 문자는 건드리지 않는다`() =
        assertNull(ReceiptParser.parse("SOLO SALE IOLI").phone)

    @Test fun `U-207-3 카드번호를 전화번호로 잡지 않는다`() =
        assertNull(ReceiptParser.parse("카드번호: 1234-56**-****-7890").phone)

    @Test fun `U-207-4 승인번호 8자리를 사업자번호로 잡지 않는다`() =
        assertNull(ReceiptParser.parse("승인번호: 38291048 일시불").businessNo)

    @Test fun `체크섬이 틀린 10자리는 사업자번호가 아니다`() =
        assertNull(ReceiptParser.parse("승인번호 1234567890").businessNo)

    @Test fun `체크섬이 맞는 사업자번호를 찾는다`() =
        assertEquals("1234567891", ReceiptParser.parse("사업자번호: 123-45-67891").businessNo)

    @Test fun `U-207-5 합계 금액`() = assertEquals(12000L, ReceiptParser.parse("합계 12,000").amount)

    @Test fun `U-207-5 결제금액 원 표기`() = assertEquals(12000L, ReceiptParser.parse("결제금액 12000원").amount)

    @Test fun `U-207-5 합계 금액 표기`() = assertEquals(53000L, ReceiptParser.parse("합계 금액 53,000").amount)

    @Test fun `U-207-5 금액이 없어도 나머지는 파싱된다`() {
        val r = ReceiptParser.parse("예시설렁탕\nTEL 02-123-4567\n2026-10-17 12:31")
        assertNull(r.amount)
        assertEquals("021234567", r.phone)
        assertNotNull(r.paidAt)
    }

    @Test fun `U-207-6 가맹점명 줄이 있으면 상호로 우선`() {
        val text = "서울특별시 종로구 우정국로\n예시냉면\n가맹점명: 예시설렁탕\n2026-10-17 12:31"
        assertEquals("예시설렁탕", ReceiptParser.parse(text).name)
    }

    @Test fun `상호 라벨이 없으면 위쪽 줄 중 한글이 가장 많은 줄이되 주소와 번호 줄은 제외`() {
        val text = "노포천년테스트식당\n사업자번호: 123-45-67891\n서울특별시 중구 세종대로 110\n판매일시: 2026-10-17 12:31"
        assertEquals("노포천년테스트식당", ReceiptParser.parse(text).name)
    }

    @Test fun `테스트 영수증 전체 파싱`() {
        val text = """
            노포천년테스트식당
            사업자번호: 123-45-67891
            대표: 홍길동 TEL: 02-000-0000
            서울특별시 중구 세종대로 110
            판매일시: 2026-10-17 12:31
            영수증번호: 1017-123105
            설렁탕 x2 24,000
            수육(소) x1 28,000
            공기밥 x1 1,000
            합계 53,000
            카드번호: 1234-56**-****-7890
            승인번호: 38291048 일시불
        """.trimIndent()
        val r = ReceiptParser.parse(text)
        assertEquals("노포천년테스트식당", r.name)
        assertEquals("1234567891", r.businessNo)
        assertEquals("020000000", r.phone)
        assertEquals(LocalDateTime.of(2026, 10, 17, 12, 31), r.paidAt)
        assertEquals(53000L, r.amount)
    }

    @Test fun `빈 글자는 모두 null`() {
        val r = ReceiptParser.parse("")
        assertNull(r.phone); assertNull(r.businessNo); assertNull(r.paidAt); assertNull(r.amount); assertNull(r.name)
    }
}
