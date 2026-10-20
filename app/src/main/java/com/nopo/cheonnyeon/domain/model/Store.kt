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
    /** 공식 대표 사진 주소 (백년가게 상세 페이지, 없으면 빈 목록) */
    val photoUrls: List<String> = emptyList(),
    val isTest: Boolean = false,
    /** 가게 이야기: 소상공인마당 상세 페이지 '소개글' 원문. 없으면 null */
    val story: String? = null,
) {
    /** 이름과 별칭 전부 (영수증·간판 글자와 맞춰 볼 후보) */
    val nameCandidates: List<String> get() = listOf(name) + aliases

    companion object {
        /** 테스트 가게 id 접두어 (FR-TST-01) */
        const val TEST_PREFIX = "TEST-"
    }
}
