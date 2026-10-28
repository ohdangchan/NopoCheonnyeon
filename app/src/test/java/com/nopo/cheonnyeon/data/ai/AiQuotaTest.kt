package com.nopo.cheonnyeon.data.ai

import com.nopo.cheonnyeon.core.common.AppError
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** AI 하루 한도: 한도까지만 허용, 날짜가 바뀌면 초기화, 넘으면 서버로 요청을 보내지 않는다 */
class AiQuotaTest {
    private var time = 5 * 86_400_000L + 1000
    private var stored = -1L to 0
    private fun quota(limit: Int) = AiQuota(limit, { time }, { stored }, { d, u -> stored = d to u })

    @Test fun `한도까지만 허용하고 넘으면 거절한다`() {
        val q = quota(3)
        repeat(3) { assertTrue(q.tryAcquire()) }
        assertFalse(q.tryAcquire())
        assertFalse(q.tryAcquire())
    }

    @Test fun `다음 날이 되면 다시 쓸 수 있다`() {
        val q = quota(1)
        assertTrue(q.tryAcquire())
        assertFalse(q.tryAcquire())
        time += 86_400_000L
        assertTrue(q.tryAcquire())
    }

    @Test fun `한도를 넘으면 전송 없이 E_AI_LIMIT을 돌려준다`() = runBlocking {
        var calls = 0
        val client = GeminiClient(transport = { _, _, _ -> calls++; "{}" }, quota = quota(1))
        val schema = kotlinx.serialization.json.JsonPrimitive("x")
        assertTrue(client.generateJson(listOf(GeminiPart.Text("a")), schema).valueOrNull != null)
        assertEquals(AppError.E_AI_LIMIT, client.generateJson(listOf(GeminiPart.Text("a")), schema).errorOrNull)
        assertEquals(1, calls)
    }
}
