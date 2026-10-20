package com.nopo.cheonnyeon.data.ai

import com.nopo.cheonnyeon.core.common.AppError
import com.nopo.cheonnyeon.core.common.AppResult
import com.nopo.cheonnyeon.domain.rule.MenuPick
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** 가짜 전송으로 Gemini 응답 처리·중복 제거·재요청·오류 변환을 시험한다 (실제 호출 없음) */
class AiRepositoryTest {
    private fun wrap(inner: String): String {
        val esc = inner.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\n")
        return """{"candidates":[{"content":{"parts":[{"text":"$esc"}]}}]}"""
    }

    private fun picks(vararg names: String) =
        """{"analysis":"소고기 국물 취향","picks":[${names.joinToString(",") { """{"menu":"$it","reason":"이유","ingredients":["소고기","대파"]}""" }}]}"""

    private fun repo(vararg responses: Pair<Int, String>, calls: MutableList<String> = mutableListOf()): AiRepository {
        val queue = ArrayDeque(responses.toList())
        return AiRepository(GeminiClient("test-key", transport = { _, _, body -> calls += body; queue.removeFirst() }))
    }

    private val input = TasteInput(listOf(TasteReview("진미옥", listOf("설렁탕"), 5, "맛있어요")), photos = listOf("QUJD"))

    @Test fun `3개를 받아 돌려준다`() = runBlocking {
        val r = repo(200 to wrap(picks("곰탕", "도가니탕", "냉면"))).recommendMenus(input, emptyList())
        assertTrue(r is AppResult.Success)
        assertEquals(listOf("곰탕", "도가니탕", "냉면"), r.valueOrNull!!.picks.map { it.menu })
        assertEquals("소고기 국물 취향", r.valueOrNull!!.summary)
    }

    @Test fun `이전에 나온 메뉴가 섞이면 한 번 더 요청해 채운다`() = runBlocking {
        val calls = mutableListOf<String>()
        val before = listOf(listOf(MenuPick("곰탕", "x"), MenuPick("냉면", "x"), MenuPick("물회", "x")))
        val r = repo(200 to wrap(picks("곰탕", "육회", "냉면")), 200 to wrap(picks("삼계탕", "칼국수")), calls = calls).recommendMenus(input, before)
        assertEquals(listOf("육회", "삼계탕", "칼국수"), r.valueOrNull!!.picks.map { it.menu })
        assertEquals(2, calls.size)
        assertTrue("제외 메뉴가 프롬프트에 들어간다", calls[0].contains("곰탕") && calls[0].contains("물회"))
    }

    @Test fun `서버 오류면 실패`() = runBlocking {
        assertEquals(AppError.E_AI_FAILED, repo(500 to "{}").recommendMenus(input, emptyList()).errorOrNull)
    }

    @Test fun `전송 중 입출력 오류는 네트워크 오류`() = runBlocking {
        val r = AiRepository(GeminiClient("k", transport = { _, _, _ -> throw java.io.IOException("x") })).recommendMenus(input, emptyList())
        assertEquals(AppError.E_NETWORK, r.errorOrNull)
    }

    @Test fun `키가 없으면 호출 없이 실패`() = runBlocking {
        val repo = AiRepository(GeminiClient("", transport = { _, _, _ -> error("호출하면 안 된다") }))
        assertFalse(repo.available)
        assertEquals(AppError.E_AI_FAILED, repo.recommendMenus(input, emptyList()).errorOrNull)
    }

    @Test fun `10번을 다 돌리면 더는 부르지 않는다`() = runBlocking {
        val full = List(10) { i -> listOf(MenuPick("a$i", "x"), MenuPick("b$i", "x"), MenuPick("c$i", "x")) }
        val calls = mutableListOf<String>()
        assertEquals(AppError.E_AI_FAILED, repo(calls = calls).recommendMenus(input, full).errorOrNull)
        assertEquals(0, calls.size)
    }

    @Test fun `요청 본문에 사진과 키 없이 모델 주소만 쓴다`() = runBlocking {
        val calls = mutableListOf<String>()
        repo(200 to wrap(picks("곰탕", "냉면", "물회")), calls = calls).recommendMenus(input, emptyList())
        assertTrue(calls[0].contains("inline_data") && calls[0].contains("image/webp"))
        assertFalse(calls[0].contains("test-key"))
    }

    private val insightJson = """{"oneLine":"국물이 진한 집","strengths":["국물","양"],"menus":[{"menu":"설렁탕","reason":"가장 많이 언급"}],"weaknesses":["웨이팅"],"bestFor":"혼밥"}"""

    @Test fun `가게 리뷰 분석 결과를 파싱하고 같은 입력은 캐시한다`() = runBlocking {
        val calls = mutableListOf<String>()
        val repo = repo(200 to wrap(insightJson), calls = calls)
        val reviews = listOf(InsightReview(5, "국물이 진해요"), InsightReview(3, "웨이팅이 길어요"))
        val a = repo.analyzeStore("S1", "진미옥", listOf("설렁탕"), reviews)
        val b = repo.analyzeStore("S1", "진미옥", listOf("설렁탕"), reviews)
        assertEquals("국물이 진한 집", (a as AppResult.Success).value.oneLine)
        assertEquals(listOf("웨이팅"), a.value.weaknesses)
        assertEquals(1, calls.size)
        assertEquals(a.value, (b as AppResult.Success).value)
    }

    @Test fun `분석 응답이 비어 있으면 실패`() = runBlocking {
        val r = repo(200 to wrap("""{"oneLine":"","strengths":[],"menus":[],"weaknesses":[],"bestFor":""}""")).analyzeStore("S2", "가게", emptyList(), listOf(InsightReview(4, "좋아요")))
        assertEquals(AppError.E_AI_FAILED, r.errorOrNull)
    }
}
