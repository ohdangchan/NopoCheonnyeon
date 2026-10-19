package com.nopo.cheonnyeon.core.util

/** 전화번호는 숫자만 남겨 비교한다 (하이픈·괄호·공백 표기 차이 흡수). */
object PhoneNumber {
    fun normalize(raw: String): String = raw.filter { it in '0'..'9' }

    /** 둘 다 비어 있지 않고 숫자가 같을 때만 같은 번호 */
    fun same(a: String?, b: String?): Boolean {
        val x = a?.let(::normalize).orEmpty()
        val y = b?.let(::normalize).orEmpty()
        return x.isNotEmpty() && x == y
    }
}
