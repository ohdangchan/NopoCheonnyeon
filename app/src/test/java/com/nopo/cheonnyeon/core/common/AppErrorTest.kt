package com.nopo.cheonnyeon.core.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** S-106 AC: PRD 7.4 표의 모든 코드에 문구가 있다. */
class AppErrorTest {
    private val strings: String = File("src/main/res/values/strings.xml").readText()

    private fun stringName(e: AppError) = "err_" + e.code.lowercase()

    @Test fun `PRD 7-4의 오류 코드 27개가 모두 정의돼 있다`() = assertEquals(27, AppError.entries.size)

    @Test fun `모든 오류 코드에 strings xml 문구가 있다`() {
        AppError.entries.forEach { e ->
            val m = Regex("""<string name="${stringName(e)}">([^<]+)</string>""").find(strings)
            assertTrue("${e.code}의 문구(${stringName(e)})가 없다", m != null && m.groupValues[1].isNotBlank())
        }
    }

    @Test fun `코드 문자열이 이름과 같다`() = AppError.entries.forEach { assertEquals(it.name, it.code) }

    @Test fun `거리와 정확도 문구는 숫자 자리표시자를 가진다`() {
        assertTrue(strings.contains("""name="err_e_far">""") && Regex("""err_e_far">[^<]*%1\${'$'}d""").containsMatchIn(strings))
        assertTrue(Regex("""err_e_accuracy">[^<]*%1\${'$'}d""").containsMatchIn(strings))
    }
}
