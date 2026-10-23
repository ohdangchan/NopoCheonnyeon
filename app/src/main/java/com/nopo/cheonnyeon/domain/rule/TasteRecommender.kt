package com.nopo.cheonnyeon.domain.rule

import com.nopo.cheonnyeon.domain.model.Store

data class TasteRegion(val key: String, val label: String, val desc: String, val sigungu: List<String> = emptyList(), val sidoPrefix: String? = null)

data class TasteFood(val key: String, val label: String, val desc: String, val words: List<String>)

/** 처음 설치 때 고르는 좋아하는 지역·음식, 그리고 그 취향에 맞는 노포 하나 고르기 (순수 규칙) */
object TasteRecommender {
    val REGIONS = listOf(
        TasteRegion("jongno", "종로", "역사와 시간이\n머무는 거리", sigungu = listOf("종로구")),
        TasteRegion("euljiro", "을지로", "레트로 감성의\n노포 골목", sigungu = listOf("중구")),
        TasteRegion("mapo", "마포", "젊음과 전통이\n어우러진 맛집", sigungu = listOf("마포구")),
        TasteRegion("seongsu", "성수", "트렌디한 노포의\n새로운 발견", sigungu = listOf("성동구")),
        TasteRegion("busan", "부산", "바다와 함께하는\n노포 미식 여행", sidoPrefix = "부산"),
    )

    val FOODS = listOf(
        TasteFood("naengmyeon", "냉면", "시원한 깊은 맛", listOf("냉면", "막국수", "메밀")),
        TasteFood("gukbap", "국밥", "뜨끈한 한 그릇", listOf("국밥", "해장국", "설렁탕", "설농탕", "곰탕", "순대국", "탕")),
        TasteFood("bunsik", "분식", "추억의 맛", listOf("분식", "떡볶이", "김밥", "튀김", "어묵", "오뎅")),
        TasteFood("gogi", "고기", "불향 가득한 맛", listOf("고기", "갈비", "삼겹", "구이", "불고기", "곱창", "족발", "보쌈", "수육")),
        TasteFood("kalguksu", "칼국수", "손맛이 살아있는 맛", listOf("칼국수", "수제비", "국수", "우동")),
        TasteFood("jeontongju", "전통주", "음식과 어울리는 술", listOf("막걸리", "전통주", "주점", "소주", "술")),
        TasteFood("haemul", "해산물", "바다의 신선한 맛", listOf("해물", "생선", "회", "횟집", "조개", "낙지", "게장")),
        TasteFood("jeon", "전·한식", "정겨운 우리 맛", listOf("전", "부침", "한식", "백반", "정식", "비빔밥", "보리밥")),
    )

    fun inRegion(s: Store, r: TasteRegion): Boolean =
        (r.sigungu.isNotEmpty() && r.sigungu.any { s.sigungu == it || s.sigungu.startsWith(it) }) || (r.sidoPrefix != null && s.sido.startsWith(r.sidoPrefix))

    fun matchesFood(s: Store, f: TasteFood): Boolean {
        val text = listOf(s.name, s.categoryGroup, s.menuItems.joinToString(" ")).joinToString(" ")
        return f.words.any { text.contains(it) }
    }

    /** 취향에 맞는 노포 하나(평점·사진·업력 순). 조건이 너무 좁으면 음식, 그다음 지역 조건을 풀고, 그래도 없으면 null */
    fun recommend(stores: List<Store>, regionKeys: Set<String>, foodKeys: Set<String>, ratings: Map<String, Double> = emptyMap()): Store? {
        val base = stores.filter { it.isFood && !it.isTest && it.lat != null && it.lng != null }
        val regions = REGIONS.filter { it.key in regionKeys }
        val foods = FOODS.filter { it.key in foodKeys }
        fun pick(list: List<Store>) = list.sortedWith(
            compareByDescending<Store> { it.photoUrls.isNotEmpty() }.thenByDescending { ratings[it.id] ?: 0.0 }.thenBy { it.foundedYear ?: Int.MAX_VALUE },
        ).firstOrNull()
        val byRegion = if (regions.isEmpty()) base else base.filter { s -> regions.any { inRegion(s, it) } }
        val both = if (foods.isEmpty()) byRegion else byRegion.filter { s -> foods.any { matchesFood(s, it) } }
        return pick(both) ?: pick(byRegion) ?: pick(base)
    }
}
