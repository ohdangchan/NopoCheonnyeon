package com.nopo.cheonnyeon.domain.rule

import com.nopo.cheonnyeon.domain.model.Title

/** 컬렉션·마이 화면의 숫자 규칙 */
object CollectionRules {
    /** "방문한 노포 N / 목표"의 목표: 50곳 단위로 올라간다 */
    fun goal(visited: Int): Int = maxOf(50, (visited / 50 + 1) * 50)

    /** 현재 칭호 안에서의 진행: [into]/[span], 다음 칭호가 없으면 [next]=null·[fraction]=1 */
    data class TitleProgress(val current: Title, val next: Title?, val into: Int, val span: Int) {
        val fraction: Float get() = if (next == null) 1f else (into.toFloat() / span).coerceIn(0f, 1f)
        val yearsLeft: Int get() = if (next == null) 0 else (next.requiredYears - (current.requiredYears + into)).coerceAtLeast(0)
    }

    fun titleProgress(totalYears: Int): TitleProgress {
        val cur = TitlePolicy.titleFor(totalYears)
        val next = Title.entries.firstOrNull { it.requiredYears > totalYears }
        val span = if (next == null) 1 else next.requiredYears - cur.requiredYears
        return TitleProgress(cur, next, (totalYears - cur.requiredYears).coerceAtLeast(0), span)
    }
}
