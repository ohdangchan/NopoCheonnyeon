package com.nopo.cheonnyeon.domain.model

/** 카탈로그의 가게 한 곳 (stores.json 항목, PRD 8.1). 좌표·창업연도·전화는 없을 수 있다. */
data class Store(
    val id: String,
    val name: String,
    val aliases: List<String> = emptyList(),
    val programs: List<String> = emptyList(),
    val categoryGroup: String = "",
    val isFood: Boolean = true,
    val sido: String = "",
    val sigungu: String = "",
    val address: String = "",
    /** 숫자만 */
    val phone: String? = null,
    val foundedYear: Int? = null,
    val foundedDate: String? = null,
    val lat: Double? = null,
    val lng: Double? = null,
    val menuItems: List<String> = emptyList(),
    val hours: String? = null,
    val amenities: List<String> = emptyList(),
    val summary: String? = null,
    val generation: Int? = null,
    val sourceUrl: String? = null,
    val isTest: Boolean = false,
) {
    /** 이름과 별칭 전부 (영수증·간판 글자와 맞춰 볼 후보) */
    val nameCandidates: List<String> get() = listOf(name) + aliases
}
