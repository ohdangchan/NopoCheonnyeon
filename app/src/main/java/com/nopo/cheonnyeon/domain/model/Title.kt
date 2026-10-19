package com.nopo.cheonnyeon.domain.model

/** 칭호 (FR-STP-04). 필요 세월(이상) 판정은 rule/TitlePolicy. */
enum class Title(val requiredYears: Int) {
    SPROUT(0),
    REGULAR(100),
    ELDER(300),
    ARTISAN(600),
    MASTER(1_000),
}
