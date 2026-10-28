package com.nopo.cheonnyeon.data.ai

import android.content.Context

/**
 * AI 호출 하루 한도. 이 기기에서 하루(UTC 기준 날짜가 바뀔 때까지)에 [limit]번까지만 Gemini를 부른다.
 * 앱이 아닌 도구가 직접 부르는 것은 막지 못한다(그 방어는 서버의 할당량·예산 한도). 정상 이용자의 과다 호출·버그 폭주를 막는다.
 */
class AiQuota(
    private val limit: Int,
    private val now: () -> Long,
    private val load: () -> Pair<Long, Int>,
    private val save: (day: Long, used: Int) -> Unit,
) {
    private val lock = Any()

    /** 한도 안이면 1회분을 쓰고 true, 한도를 넘었으면 false */
    fun tryAcquire(): Boolean = synchronized(lock) {
        val today = now() / DAY_MS
        val (day, used) = load()
        val count = if (day == today) used else 0
        if (count >= limit) return false
        save(today, count + 1)
        true
    }

    companion object {
        const val DEFAULT_LIMIT = 60
        private const val DAY_MS = 86_400_000L

        /** 기기 저장소(SharedPreferences)에 날짜와 사용 횟수를 보관한다 */
        fun persistent(context: Context, limit: Int = DEFAULT_LIMIT): AiQuota {
            val prefs = context.getSharedPreferences("ai_quota", Context.MODE_PRIVATE)
            return AiQuota(
                limit,
                { System.currentTimeMillis() },
                { prefs.getLong("day", -1L) to prefs.getInt("used", 0) },
                { day, used -> prefs.edit().putLong("day", day).putInt("used", used).apply() },
            )
        }
    }
}
