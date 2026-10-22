package com.nopo.cheonnyeon.feature.store

import android.content.Context
import org.json.JSONObject

/** 대표 메뉴 사진: 메뉴 이름에서 요리 종류를 찾아 위키미디어 공용의 자유 이용 사진(assets/menu)을 돌려 쓴다. 못 찾으면 null. */
object MenuPhotos {
    private val rules = listOf(
        "jajang" to listOf("짜장", "짬뽕", "탕수육"),
        "seolleongtang" to listOf("설렁탕", "곰탕", "갈비탕", "도가니", "순대국", "설농탕"),
        "gukbap" to listOf("국밥", "해장국", "내장", "뼈", "육개장", "소머리"),
        "naengmyeon" to listOf("냉면", "막국수", "메밀"),
        "kalguksu" to listOf("칼국수", "수제비", "국수", "우동", "라면", "만두", "면"),
        "tteokbokki" to listOf("떡볶이", "분식", "튀김", "순대", "오뎅", "어묵", "떡"),
        "gimbap" to listOf("김밥"),
        "bulgogi" to listOf("불고기", "전골", "샤브"),
        "gogi" to listOf("갈비", "삼겹", "구이", "고기", "수육", "족발", "보쌈", "돼지", "소고기", "한우", "곱창", "제육", "육회", "생선"),
        "jjigae" to listOf("찌개", "탕", "매운탕", "추어", "해물"),
        "bibimbap" to listOf("비빔밥", "덮밥", "정식", "백반", "한정식", "밥", "회"),
    )

    fun keyFor(menu: String): String? = rules.firstOrNull { (_, words) -> words.any { menu.contains(it) } }?.first

    fun urlFor(menu: String): String? = keyFor(menu)?.let { "file:///android_asset/menu/$it.webp" }

    /** "작성자 · 라이선스" (CC BY 표기용). 파일에서 못 읽으면 null */
    fun credit(context: Context, menu: String): String? {
        val key = keyFor(menu) ?: return null
        return runCatching {
            val json = context.assets.open("menu/credits.json").bufferedReader().use { JSONObject(it.readText()) }
            val c = json.getJSONObject(key)
            val artist = c.optString("artist").take(28)
            listOf(artist, c.optString("license")).filter { it.isNotBlank() }.joinToString(" · ")
        }.getOrNull()
    }
}
