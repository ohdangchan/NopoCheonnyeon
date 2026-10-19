package com.nopo.cheonnyeon.domain.rule

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AuthValidationTest {
    @Test fun `이메일 형식`() {
        assertNull(AuthValidation.email("a@b.co"))
        assertEquals(AuthInvalid.EMAIL, AuthValidation.email("a@b"))
        assertEquals(AuthInvalid.EMAIL, AuthValidation.email(""))
        assertEquals(AuthInvalid.EMAIL, AuthValidation.email("a b@c.com"))
    }

    @Test fun `이메일 앞뒤 공백은 허용`() = assertNull(AuthValidation.email("  a@b.co "))

    @Test fun `비밀번호 8자 이상 영문과 숫자 포함`() {
        assertNull(AuthValidation.password("abcd1234"))
        assertEquals(AuthInvalid.PASSWORD_SHORT, AuthValidation.password("abc123"))
        assertEquals(AuthInvalid.PASSWORD_WEAK, AuthValidation.password("abcdefgh"))
        assertEquals(AuthInvalid.PASSWORD_WEAK, AuthValidation.password("12345678"))
    }

    @Test fun `비밀번호 확인`() {
        assertNull(AuthValidation.passwordConfirm("abcd1234", "abcd1234"))
        assertEquals(AuthInvalid.PASSWORD_MISMATCH, AuthValidation.passwordConfirm("abcd1234", "abcd1235"))
    }

    @Test fun `닉네임 2~12자 한글 영문 숫자`() {
        assertNull(AuthValidation.nickname("국밥장인"))
        assertNull(AuthValidation.nickname("ab"))
        assertNull(AuthValidation.nickname("가나다라마바사아자차카타".take(12)))
        assertEquals(AuthInvalid.NICK_LENGTH, AuthValidation.nickname("가"))
        assertEquals(AuthInvalid.NICK_LENGTH, AuthValidation.nickname("가나다라마바사아자차카타파"))
    }

    @Test fun `닉네임에 공백 특수문자 이모지 불가`() {
        assertEquals(AuthInvalid.NICK_CHARS, AuthValidation.nickname("국 밥"))
        assertEquals(AuthInvalid.NICK_CHARS, AuthValidation.nickname("국밥!"))
        assertEquals(AuthInvalid.NICK_CHARS, AuthValidation.nickname("국밥😀"))
    }

    @Test fun `닉네임 키는 소문자`() = assertEquals("abc", AuthValidation.nicknameKey("AbC"))
}
