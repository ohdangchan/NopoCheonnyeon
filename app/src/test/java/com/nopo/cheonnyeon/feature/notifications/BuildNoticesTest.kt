package com.nopo.cheonnyeon.feature.notifications

import com.nopo.cheonnyeon.data.local.MyVisitEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BuildNoticesTest {
    private fun visit(id: String, years: Int, at: Long) = MyVisitEntity(id, years, "RECEIPT", at, at)

    @Test fun `방문이 없으면 환영 알림만 나온다`() {
        val l = buildNotices(emptyList(), emptyMap(), news = true, reco = false, nowMs = 1_000)
        assertEquals(listOf("welcome"), l.map { it.key })
    }

    @Test fun `새 소식을 끄면 도장 알림이 빠진다`() {
        val l = buildNotices(listOf(visit("S-1", 30, 10)), mapOf("S-1" to "가게"), news = false, reco = true, nowMs = 1_000)
        assertTrue(l.none { it.key.startsWith("stamp-") })
        assertTrue(l.any { it.key == "reco" })
    }

    @Test fun `시험 가게 방문은 알림에 넣지 않는다`() {
        val l = buildNotices(listOf(visit("TEST-1", 30, 10)), emptyMap(), news = true, reco = false, nowMs = 1_000)
        assertEquals(listOf("welcome"), l.map { it.key })
    }

    @Test fun `칭호가 오른 방문에만 칭호 알림이 붙고 최신순이다`() {
        val l = buildNotices(listOf(visit("S-1", 60, 10), visit("S-2", 60, 20)), mapOf("S-1" to "가", "S-2" to "나"), news = true, reco = false, nowMs = 1_000)
        val titles = l.filter { it.key.startsWith("title-") }
        assertEquals(1, titles.size) // 60→120년에서 100년 칭호를 처음 넘는다
        assertEquals(20L + 1, titles.first().at)
        assertEquals(l.sortedByDescending { it.at }, l)
    }

    @Test fun `카탈로그에 없는 가게는 id 로 보여 준다`() {
        val l = buildNotices(listOf(visit("S-9", 30, 10)), emptyMap(), news = true, reco = false, nowMs = 1_000)
        assertTrue(l.first { it.key == "stamp-S-9" }.body.startsWith("S-9"))
        assertFalse(l.isEmpty())
    }
}
