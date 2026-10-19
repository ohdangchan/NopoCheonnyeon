package com.nopo.cheonnyeon.core.util

/** 사업자등록번호 체크섬 (PRD 7.5). 테스트용 가상 번호: 123-45-67891 유효, 123-45-67890 무효. */
object BusinessNumber {
    private val weights = intArrayOf(1, 3, 7, 1, 3, 7, 1, 3, 5)

    fun digits(raw: String): String = raw.filter { it in '0'..'9' }

    fun isValid(raw: String): Boolean {
        val d = digits(raw)
        if (d.length != 10) return false
        val n = d.map { it - '0' }
        val sum = (0..8).sumOf { n[it] * weights[it] } + n[8] * 5 / 10
        return (10 - sum % 10) % 10 == n[9]
    }
}
