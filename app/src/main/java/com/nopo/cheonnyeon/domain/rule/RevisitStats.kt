package com.nopo.cheonnyeon.domain.rule

import kotlin.math.roundToInt

/** 가게별 재방문율 표시 규칙 (FR-STORE-07). 재방문자 = 그 가게를 2회 이상 인증한 사용자. */
object RevisitStats {
    /** 방문자가 이보다 적으면 비율이 오해를 부르므로 "N명 중 K명"만 보여 준다 */
    const val MIN_SAMPLE = 5

    /** 표본이 충분할 때만 0~100 퍼센트, 아니면 null. 값이 말이 안 되면(재방문자 > 방문자) null */
    fun percent(visitors: Int, revisitors: Int): Int? =
        if (visitors < MIN_SAMPLE || revisitors < 0 || revisitors > visitors) null else (revisitors * 100.0 / visitors).roundToInt()
}
