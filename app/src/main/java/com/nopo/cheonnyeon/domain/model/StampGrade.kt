package com.nopo.cheonnyeon.domain.model

/** 도장 등급 (FR-STP-03). 업력 → 등급 판정은 rule/StampGradePolicy. */
enum class StampGrade {
    /** 30~49년, 청록 */
    NORMAL,

    /** 50~99년, 인주 빨강 */
    RED,

    /** 100년 이상, 금빛 */
    GOLD,
}
