package com.nopo.cheonnyeon.domain.rule

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MenuRouletteTest {
    private fun p(name: String) = MenuPick(name, "이유")
    private fun round(vararg n: String) = n.map(::p)

    @Test fun `한 번에 3개 최대 10번이면 서로 다른 메뉴 30개`() {
        assertEquals(30, MenuRoulette.MAX_MENUS)
        assertTrue(MenuRoulette.canSpin(List(9) { round("a$it", "b$it", "c$it") }))
        assertFalse(MenuRoulette.canSpin(List(10) { round("a$it", "b$it", "c$it") }))
    }

    @Test fun `제외 목록은 최근 9라운드까지만 기억한다`() {
        val rounds = List(10) { round("a$it", "b$it", "c$it") }
        val ex = MenuRoulette.exclusions(rounds)
        assertEquals(27, ex.size)
        assertFalse("a0" in ex)       // 가장 오래된 라운드는 잊는다
        assertTrue("a1" in ex && "c9" in ex)
    }

    @Test fun `이미 나온 메뉴와 라운드 안 중복은 걸러진다`() {
        val rounds = listOf(round("곰탕", "냉면", "칼국수"))
        val fresh = MenuRoulette.fresh(listOf(p("곰 탕"), p("물회"), p("물회"), p("(여름) 냉면"), p("육회"), p("삼계탕"), p("도가니탕")), rounds)
        assertEquals(listOf("물회", "육회", "삼계탕"), fresh.map { it.menu })
    }

    @Test fun `이미 이번 라운드에 담은 메뉴도 제외하고 빈 이름은 버린다`() {
        val fresh = MenuRoulette.fresh(listOf(p(""), p("물회"), p("육회")), emptyList(), alsoTaken = listOf(p("물회")))
        assertEquals(listOf("육회"), fresh.map { it.menu })
    }

    @Test fun `열쇠는 공백 괄호 대소문자를 무시한다`() {
        assertEquals(MenuRoulette.key("Gom Tang"), MenuRoulette.key("gomtang(대)"))
    }
}
