package com.nopo.cheonnyeon.core.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BusinessNumberTest {
    @Test fun `U-206-1 체크섬이 맞는 번호는 유효`() {
        assertTrue(BusinessNumber.isValid("123-45-67891"))
        assertTrue(BusinessNumber.isValid("1234567891"))
    }

    @Test fun `U-206-1 체크섬이 틀리면 무효`() = assertFalse(BusinessNumber.isValid("123-45-67890"))

    @Test fun `U-206-1 9자리와 11자리는 무효`() {
        assertFalse(BusinessNumber.isValid("123-45-6789"))
        assertFalse(BusinessNumber.isValid("123-45-678912"))
    }

    @Test fun `빈 값과 숫자가 아닌 값은 무효`() {
        assertFalse(BusinessNumber.isValid(""))
        assertFalse(BusinessNumber.isValid("abc-de-fghij"))
    }

    @Test fun `숫자만 남긴다`() = assertEquals("1234567891", BusinessNumber.digits("123-45-67891"))
}
