package com.nopo.cheonnyeon.core.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PhoneNumberTest {
    @Test fun `U-206-2 여러 표기를 숫자만으로 정규화`() {
        assertEquals("021234567", PhoneNumber.normalize("02-123-4567"))
        assertEquals("021234567", PhoneNumber.normalize("021234567"))
        assertEquals("021234567", PhoneNumber.normalize("(02) 123 4567"))
        assertEquals("01012345678", PhoneNumber.normalize("010-1234-5678"))
        assertEquals("050712345678", PhoneNumber.normalize("0507-1234-5678"))
    }

    @Test fun `표기가 달라도 같은 번호`() = assertTrue(PhoneNumber.same("02-123-4567", "(02) 123 4567"))

    @Test fun `다른 번호와 빈 값은 같지 않다`() {
        assertFalse(PhoneNumber.same("02-123-4567", "02-123-4568"))
        assertFalse(PhoneNumber.same(null, "02-123-4567"))
        assertFalse(PhoneNumber.same("", ""))
    }
}
