package com.nopo.cheonnyeon.data.ai

import com.nopo.cheonnyeon.core.common.AppError
import com.nopo.cheonnyeon.core.common.AppResult
import com.nopo.cheonnyeon.core.common.asFailure
import com.nopo.cheonnyeon.core.common.asSuccess
import com.nopo.cheonnyeon.domain.rule.MenuPick
import com.nopo.cheonnyeon.domain.rule.MenuRoulette
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** 취향 분석에 쓰는 내 리뷰 한 개(가게 메뉴 포함) */
data class TasteReview(val storeName: String, val storeMenus: List<String>, val rating: Int, val text: String)

/** 취향 분석 입력: 내 리뷰들과 내가 올린 음식 사진(base64 WebP, 최근 것부터) */
data class TasteInput(val reviews: List<TasteReview>, val photos: List<String>)

data class MenuAnalysis(val summary: String, val picks: List<MenuPick>)

/** 가게 리뷰 분석에 쓰는 리뷰 한 개 */
data class InsightReview(val rating: Int, val text: String)

data class InsightMenu(val menu: String, val reason: String)

/** 다른 이용자 리뷰를 AI가 분석한 결과 */
data class StoreInsight(
    val oneLine: String,
    val strengths: List<String>,
    val menus: List<InsightMenu>,
    val weaknesses: List<String>,
    val bestFor: String,
)

@Serializable private data class PickDto(val menu: String = "", val reason: String = "", val ingredients: List<String> = emptyList())
@Serializable private data class RecommendDto(val analysis: String = "", val picks: List<PickDto> = emptyList())
@Serializable private data class TranslateDto(val items: List<String> = emptyList())
@Serializable private data class HintDto(val tip: String = "")
@Serializable private data class MenuDto(val menu: String = "", val reason: String = "")
@Serializable private data class InsightDto(
    val oneLine: String = "", val strengths: List<String> = emptyList(), val menus: List<MenuDto> = emptyList(),
    val weaknesses: List<String> = emptyList(), val bestFor: String = "",
)

/**
 * AI 기능 두 가지 (Gemini): ① 내 리뷰·음식 사진 기반 메뉴 추천 룰렛, ② 가게 리뷰 분석(특장점·추천 메뉴·단점).
 * 보내는 것은 리뷰 글·별점·음식 사진·가게 메뉴뿐이다. 위치·영수증·이메일은 보내지 않는다.
 */
class AiRepository(private val client: GeminiClient) {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val insightCache = HashMap<String, StoreInsight>()

    val available: Boolean get() = client.available

    /**
     * 메뉴 3개를 추천한다. [rounds]는 지금까지 나온 라운드(제외 메뉴로 쓰임).
     * 중복이 섞여 3개가 안 되면 한 번 더 요청해 채운다. 하나도 못 얻으면 실패.
     */
    suspend fun recommendMenus(input: TasteInput, rounds: List<List<MenuPick>>): AppResult<MenuAnalysis> {
        if (!MenuRoulette.canSpin(rounds)) return AppError.E_AI_FAILED.asFailure()
        var summary = ""
        val picked = ArrayList<MenuPick>()
        repeat(2) { attempt ->
            val exclude = MenuRoulette.exclusions(rounds) + picked.map { it.menu }
            val result = client.generateJson(tastePrompt(input, exclude, rounds.size + 1, 3 - picked.size, attempt), RECOMMEND_SCHEMA, temperature = 1.0)
            val text = when (result) {
                is AppResult.Success -> result.value
                is AppResult.Failure -> if (picked.isEmpty()) return result else return finish(summary, picked)
            }
            val dto = runCatching { json.decodeFromString<RecommendDto>(text) }.getOrNull() ?: return if (picked.isEmpty()) AppError.E_AI_FAILED.asFailure() else finish(summary, picked)
            if (summary.isBlank()) summary = dto.analysis.trim()
            val fresh = MenuRoulette.fresh(dto.picks.map { MenuPick(it.menu.trim(), it.reason.trim(), it.ingredients.map(String::trim).filter(String::isNotEmpty).take(6)) }, rounds, picked)
            picked += fresh.take(MenuRoulette.PER_ROUND - picked.size)
            if (picked.size >= MenuRoulette.PER_ROUND) return finish(summary, picked)
        }
        return finish(summary, picked)
    }

    private fun finish(summary: String, picked: List<MenuPick>): AppResult<MenuAnalysis> =
        if (picked.isEmpty()) AppError.E_AI_FAILED.asFailure() else MenuAnalysis(summary, picked.take(MenuRoulette.PER_ROUND)).asSuccess()

    /** [texts]를 [language]로 번역한다(순서·개수 유지). 가게 이름·메뉴 이름 같은 고유명사는 원어를 괄호로 함께 둔다 */
    suspend fun translate(texts: List<String>, language: String): AppResult<List<String>> {
        if (texts.isEmpty()) return emptyList<String>().asSuccess()
        val prompt = buildString {
            append("다음 JSON 배열의 각 글을 '$language'(으)로 자연스럽게 번역해 줘. 한국 음식·가게 소개글과 이용자 리뷰야.\n")
            append("규칙: 배열 길이와 순서를 그대로 유지하고, 줄바꿈은 살려. 가게 이름·메뉴 이름(예: 설렁탕)은 번역문 뒤에 원어를 괄호로 한 번 붙여 줘(예: seolleongtang (설렁탕)). 번역 결과만 JSON으로 답해.\n\n")
            append(kotlinx.serialization.json.JsonArray(texts.map { kotlinx.serialization.json.JsonPrimitive(it.take(2000)) }).toString())
        }
        val result = client.generateJson(listOf(GeminiPart.Text(prompt)), TRANSLATE_SCHEMA, temperature = 0.2)
        val text = when (result) {
            is AppResult.Success -> result.value
            is AppResult.Failure -> return result
        }
        val items = runCatching { json.decodeFromString<TranslateDto>(text).items }.getOrNull()
        return if (items == null || items.size != texts.size) AppError.E_AI_FAILED.asFailure() else items.asSuccess()
    }

    /** 가게의 다른 이용자 리뷰를 분석한다. 같은 가게·같은 리뷰 묶음은 이번 실행 동안 다시 묻지 않는다 */
    suspend fun analyzeStore(storeId: String, storeName: String, menus: List<String>, reviews: List<InsightReview>): AppResult<StoreInsight> {
        val usable = reviews.filter { it.text.isNotBlank() }.take(MAX_INSIGHT_REVIEWS)
        val cacheKey = storeId + "|" + reviews.size + "|" + usable.joinToString("") { it.text }.hashCode()
        insightCache[cacheKey]?.let { return it.asSuccess() }
        val result = client.generateJson(listOf(GeminiPart.Text(insightPrompt(storeName, menus, reviews, usable))), INSIGHT_SCHEMA, temperature = 0.4)
        val text = when (result) {
            is AppResult.Success -> result.value
            is AppResult.Failure -> return result
        }
        val dto = runCatching { json.decodeFromString<InsightDto>(text) }.getOrNull() ?: return AppError.E_AI_FAILED.asFailure()
        val insight = StoreInsight(
            oneLine = dto.oneLine.trim(),
            strengths = dto.strengths.map(String::trim).filter(String::isNotEmpty).take(4),
            menus = dto.menus.filter { it.menu.isNotBlank() }.map { InsightMenu(it.menu.trim(), it.reason.trim()) }.take(3),
            weaknesses = dto.weaknesses.map(String::trim).filter(String::isNotEmpty).take(3),
            bestFor = dto.bestFor.trim(),
        )
        if (insight.oneLine.isBlank() && insight.strengths.isEmpty()) return AppError.E_AI_FAILED.asFailure()
        insightCache[cacheKey] = insight
        return insight.asSuccess()
    }

    /**
     * 영수증 인식에 실패했을 때 원인과 다시 찍는 방법을 한두 문장으로 알려 준다(예: "가게 이름이 잘렸어요. 위쪽까지 찍어 주세요").
     * [imageBase64]는 방금 찍은 영수증 사진(JPEG, 줄인 것)이며 이 호출에서만 쓰고 저장하지 않는다.
     */
    suspend fun receiptHint(storeName: String, ocrLines: List<String>, reason: String, imageBase64: String?): AppResult<String> {
        val prompt = buildString {
            append("너는 영수증 방문 인증 앱의 촬영 도우미야. 사용자가 '$storeName'에서 받은 영수증을 찍었는데 인식에 실패했어.\n")
            append("실패 이유: $reason\n")
            append("글자 인식(OCR)이 읽은 줄(최대 25줄):\n")
            if (ocrLines.isEmpty()) append("(읽힌 글자 없음)\n") else ocrLines.take(25).forEachIndexed { i, l -> append("${i + 1}. ${l.take(60)}\n") }
            if (imageBase64 != null) append("첨부 사진은 방금 찍은 영수증이야. 사진에서 가게 이름·날짜·시간이 보이는지, 잘렸는지, 흐리거나 어둡거나 빛이 반사되거나 기울었는지, 다른 가게 영수증은 아닌지 살펴봐.\n")
            append("가장 큰 원인 하나를 골라, 사용자가 다시 찍을 때 할 행동을 존댓말 한두 문장으로 알려 줘. 예: \"가게 이름이 잘렸어요. 영수증 위쪽까지 화면에 담아 주세요.\"\n")
            append("추측이 어려우면 일반적인 촬영 요령(밝은 곳, 평평하게, 전체가 틀 안에)을 알려 줘. 한국어 JSON으로만 답해.")
        }
        val parts = ArrayList<GeminiPart>()
        parts += GeminiPart.Text(prompt)
        imageBase64?.let { parts += GeminiPart.Image("image/jpeg", it) }
        val result = client.generateJson(parts, HINT_SCHEMA, temperature = 0.3)
        val text = when (result) {
            is AppResult.Success -> result.value
            is AppResult.Failure -> return result
        }
        val tip = runCatching { json.decodeFromString<HintDto>(text).tip.trim() }.getOrNull().orEmpty()
        return if (tip.isBlank()) AppError.E_AI_FAILED.asFailure() else tip.asSuccess()
    }

    internal fun tastePrompt(input: TasteInput, exclude: List<String>, round: Int, need: Int, attempt: Int): List<GeminiPart> {
        val sb = StringBuilder()
        sb.append("너는 한국 노포(오래된 맛집) 메뉴 추천 도우미야. 아래는 사용자가 리뷰를 남긴 가게의 메뉴와 사용자의 별점·한마디, 그리고 사용자가 리뷰에 올린 음식 사진이야.\n")
        sb.append("1) 사진 속 음식이 무엇인지, 주재료·구성 성분(예: 밀가루, 소고기, 돼지고기, 닭고기, 해산물, 회, 채소, 쌀, 유제품 등)과 맛 성향(국물/매움/담백/기름짐 등)을 분석해.\n")
        sb.append("2) 가게 메뉴·별점(높을수록 좋아함)·한마디와 합쳐 사용자의 취향을 파악해.\n")
        sb.append("3) 그 취향에 맞는 다른 메뉴를 정확히 ${need}개 추천해. 메뉴는 '곰탕', '물회'처럼 일반적인 한식 메뉴 이름으로, 특정 가게 이름은 쓰지 마.\n")
        sb.append("   각 메뉴마다 추천 이유(한두 문장, 사용자의 리뷰·사진과 연결)와 주재료·구성 성분 목록(2~5개)을 적어.\n")
        sb.append("4) 이번이 ${round}번째 추천이야. 앞에서 추천한 것과 겹치지 않게 다양하게(국물·면·구이·해산물·밥·분식·디저트 등 폭넓게) 골라.\n")
        if (exclude.isNotEmpty()) sb.append("반드시 제외할 메뉴(이미 추천함): ${exclude.joinToString(", ")}\n")
        if (attempt > 0) sb.append("앞선 답에 중복이 있었으니 제외 목록과 전혀 다른 메뉴로만 답해.\n")
        sb.append("\n[사용자 리뷰]\n")
        if (input.reviews.isEmpty()) sb.append("(아직 리뷰가 없음: 일반적으로 사랑받는 노포 메뉴 중에서 골라줘)\n")
        input.reviews.take(MAX_TASTE_REVIEWS).forEachIndexed { i, r ->
            sb.append("${i + 1}. ${r.storeName} (메뉴: ${r.storeMenus.take(6).joinToString(", ").ifBlank { "정보 없음" }}) 별점 ${r.rating}/5")
            if (r.text.isNotBlank()) sb.append(" — \"${r.text.take(120)}\"")
            sb.append('\n')
        }
        if (input.photos.isNotEmpty()) sb.append("\n[첨부 사진 ${input.photos.size}장: 사용자가 올린 음식 사진]\n")
        sb.append("\n한국어로 JSON만 답해.")
        val parts = ArrayList<GeminiPart>()
        parts += GeminiPart.Text(sb.toString())
        input.photos.take(MAX_TASTE_PHOTOS).forEach { parts += GeminiPart.Image("image/webp", it) }
        return parts
    }

    internal fun insightPrompt(storeName: String, menus: List<String>, all: List<InsightReview>, usable: List<InsightReview>): String {
        val sb = StringBuilder()
        sb.append("너는 맛집 리뷰 분석가야. 아래는 '$storeName'(메뉴: ${menus.take(8).joinToString(", ").ifBlank { "정보 없음" }})에 다른 이용자들이 남긴 리뷰야.\n")
        sb.append("리뷰에 실제로 나온 내용만 근거로 특장점, 추천 메뉴(최대 3개), 아쉬운 점, 어떤 사람에게 잘 맞는지를 정리해. 근거가 없으면 지어내지 말고 비워 둬.\n")
        sb.append("별점 분포: ")
        sb.append((1..5).joinToString(", ") { s -> "${s}점 ${all.count { it.rating == s }}개" })
        sb.append("\n\n[리뷰]\n")
        usable.forEachIndexed { i, r -> sb.append("${i + 1}. (${r.rating}점) ${r.text.take(200)}\n") }
        sb.append("\n한국어로 JSON만 답해. oneLine은 한 문장 요약, strengths는 짧은 구절 2~4개, weaknesses는 짧은 구절 0~3개, bestFor는 한 문장.")
        return sb.toString()
    }

    private companion object {
        const val MAX_TASTE_REVIEWS = 12
        const val MAX_TASTE_PHOTOS = 6
        const val MAX_INSIGHT_REVIEWS = 60

        private fun str(desc: String? = null) = buildJsonObject { put("type", "STRING"); if (desc != null) put("description", desc) }
        private fun arr(item: JsonElement) = buildJsonObject { put("type", "ARRAY"); put("items", item) }
        private fun obj(props: Map<String, JsonElement>) = buildJsonObject {
            put("type", "OBJECT")
            put("properties", buildJsonObject { props.forEach { (k, v) -> put(k, v) } })
            put("required", buildJsonArray { props.keys.forEach { add(kotlinx.serialization.json.JsonPrimitive(it)) } })
        }

        val RECOMMEND_SCHEMA: JsonElement = obj(
            mapOf(
                "analysis" to str("사진·리뷰에서 파악한 취향 한두 문장"),
                "picks" to arr(obj(mapOf("menu" to str(), "reason" to str(), "ingredients" to arr(str())))),
            ),
        )
        val TRANSLATE_SCHEMA: JsonElement = obj(mapOf("items" to arr(str())))
        val HINT_SCHEMA: JsonElement = obj(mapOf("tip" to str("사용자에게 보여 줄 한두 문장")))
        val INSIGHT_SCHEMA: JsonElement = obj(
            mapOf(
                "oneLine" to str(),
                "strengths" to arr(str()),
                "menus" to arr(obj(mapOf("menu" to str(), "reason" to str()))),
                "weaknesses" to arr(str()),
                "bestFor" to str(),
            ),
        )
    }
}
