package com.nopo.cheonnyeon.domain.model

/** 칭호 (FR-STP-04): 세월 100년마다 한 단계. 필요 세월(이상) 판정은 rule/TitlePolicy. 이름(SPROUT 등)은 서버에 저장되므로 바꾸지 않는다. */
enum class Title(val requiredYears: Int) {
    SPROUT(0),
    REGULAR(100),
    WANDERER(200),
    ELDER(300),
    CONNOISSEUR(400),
    PIONEER(500),
    ARTISAN(600),
    KEEPER(700),
    SAGE(800),
    LEGEND(900),
    MASTER(1_000),
}
