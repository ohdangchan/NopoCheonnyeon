package com.nopo.cheonnyeon.domain.rule

import com.nopo.cheonnyeon.domain.model.Title
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CollectionRulesTest {
    @Test fun `목표는 50곳 단위로 오른다`() {
        assertEquals(50, CollectionRules.goal(0))
        assertEquals(50, CollectionRules.goal(49))
        assertEquals(100, CollectionRules.goal(50))
        assertEquals(100, CollectionRules.goal(99))
        assertEquals(150, CollectionRules.goal(100))
    }

    @Test fun `칭호 경계에서 진행은 0에서 시작한다`() {
        val p = CollectionRules.titleProgress(Title.entries[1].requiredYears)
        assertEquals(0, p.into)
        assertEquals(0f, p.fraction, 0f)
        assertEquals(p.span, p.yearsLeft)
    }

    @Test fun `0년이면 첫 칭호이고 다음 칭호까지 남은 세월이 맞다`() {
        val p = CollectionRules.titleProgress(0)
        assertEquals(Title.entries[0], p.current)
        assertEquals(Title.entries[1].requiredYears, p.yearsLeft)
    }

    @Test fun `최고 칭호는 진행이 가득 찬다`() {
        val p = CollectionRules.titleProgress(Title.entries.last().requiredYears + 50)
        assertNull(p.next)
        assertEquals(1f, p.fraction, 0f)
        assertEquals(0, p.yearsLeft)
    }
}
