package com.nopo.cheonnyeon.domain.rule

import com.nopo.cheonnyeon.domain.model.StampGrade
import org.junit.Assert.assertEquals
import org.junit.Test

class StampGradePolicyTest {
    @Test fun `U-202-1 30과 49는 일반`() {
        assertEquals(StampGrade.NORMAL, StampGradePolicy.gradeFor(30))
        assertEquals(StampGrade.NORMAL, StampGradePolicy.gradeFor(49))
    }

    @Test fun `U-202-2 50과 99는 붉은`() {
        assertEquals(StampGrade.RED, StampGradePolicy.gradeFor(50))
        assertEquals(StampGrade.RED, StampGradePolicy.gradeFor(99))
    }

    @Test fun `U-202-3 100과 150은 금빛`() {
        assertEquals(StampGrade.GOLD, StampGradePolicy.gradeFor(100))
        assertEquals(StampGrade.GOLD, StampGradePolicy.gradeFor(150))
    }
}
