package com.nopo.cheonnyeon.data.ai

import com.nopo.cheonnyeon.core.common.AppError
import com.nopo.cheonnyeon.core.common.AppResult
import com.nopo.cheonnyeon.core.common.asFailure
import com.nopo.cheonnyeon.core.common.asSuccess
import com.nopo.cheonnyeon.domain.rule.MenuPick
import com.nopo.cheonnyeon.domain.rule.MenuRoulette
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** 챗봇 대화 한 줄(이전 대화를 AI에게 다시 알려 줄 때) */
data class ChatTurn(val isUser: Boolean, val text: String)

/** 챗봇 답변에 쓰는 사용자 상황: 지금 시각·위치, 세월, 후보 가게 */
data class ChatContextInfo(
    val nowText: String,
    val locationText: String,
    val totalYears: Int,
    val candidates: List<com.nopo.cheonnyeon.domain.rule.ChatCandidate>,
)

/** 챗봇 답변: 글과, 후보 가게 중에서 고른 가게(ID, 이유; 코스면 순서대로) */
data class ChatReply(val text: String, val picks: List<Pair<String, String>>)

/** 단골 충성도 분석 결과 */
data class LoyaltyAnalysis(
    /** 최종 점수(0~100, 기준 점수 ±12 안) */
    val score: Int,
    val baseline: Int,
    val label: String,
    val summary: String,
    val reasons: List<String>,
    val peerCount: Int,
    val topPercent: Int,
    val input: com.nopo.cheonnyeon.domain.rule.LoyaltyInput,
)

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
@Serializable private data class ChatPickDto(val id: String = "", val reason: String = "")
@Serializable private data class ChatDto(val reply: String = "", val stores: List<ChatPickDto> = emptyList())
@Serializable private data class LoyaltyDto(val score: Int = 0, val label: String = "", val summary: String = "", val reasons: List<String> = emptyList())
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

    /**
     * 노포 도우미 챗봇: 자연어 찾기("비 오는 날 종로에서 혼자 먹기 좋은 국밥집"), 천년 경로 설계("1,000년까지 남은 312년을 가장 빨리 채우는 코스"),
     * 오늘의 노포 코스(지금 위치·영업시간 고려) 등. 가게는 [ChatContextInfo.candidates] 안에서만 고르며, 후보에 없는 ID는 버린다.
     */
    suspend fun chat(history: List<ChatTurn>, userText: String, info: ChatContextInfo): AppResult<ChatReply> {
        val remaining = (com.nopo.cheonnyeon.domain.model.Title.MASTER.requiredYears - info.totalYears).coerceAtLeast(0)
        val prompt = buildString {
            append("너는 '노포 천년 미식가' 앱의 AI 노포 도우미야. 오래된 노포(30년 이상) 맛집을 찾고 코스를 짜 줘. 친근하지만 존댓말로, 한국어로 답해.\n")
            append("앱 규칙: 노포를 방문 인증하면 그 가게의 업력(년)만큼 '세월'이 쌓이고, 세월 1,000년이 되면 '천년 미식가'야. 이미 가 본 곳(visited=Y)은 세월이 더 쌓이지 않아.\n")
            append("지금 시각: ${info.nowText}\n내 위치: ${info.locationText}\n내 세월: ${info.totalYears}년 (1,000년까지 ${remaining}년 남음)\n\n")
            append("[요청 유형 가이드]\n")
            append("- 말로 찾기: 분위기·날씨·인원·메뉴 조건에 맞는 가게를 후보에서 골라 이유를 말해 줘(비 오는 날=국물·따뜻한 메뉴 등 상식으로 해석).\n")
            append("- 천년 경로 설계: 안 가 본 곳(visited=N) 중 years가 큰 곳을 서로 가까운 순서로 묶어, 남은 ${remaining}년을 가장 빨리(가장 적은 곳 수로) 채우는 코스를 짜. 코스의 years 합계가 남은 세월에 닿는지 말해 줘(합계는 후보의 years 숫자만 더해서).\n")
            append("- 오늘의 노포 코스: 지금 시각과 영업시간(hours), 거리(km)를 고려해 반나절 3~4곳을 방문 순서대로. 영업시간이 불확실하면 확인하라고 덧붙여.\n\n")
            append("[후보 가게] (id | 이름 | 지역 | years | 메뉴 | 영업시간 | 거리 | visited)\n")
            info.candidates.forEach { c ->
                val s = c.store
                append("${s.id} | ${s.name} | ${s.sigungu} | ${c.years} | ${s.menuItems.take(3).joinToString("/")} | ${(s.hours ?: "정보 없음").take(40)} | ${c.distanceKm?.let { "%.1fkm".format(it) } ?: "-"} | ${if (c.visited) "Y" else "N"}\n")
            }
            append("\n[이전 대화]\n")
            history.takeLast(MAX_CHAT_TURNS).forEach { append(if (it.isUser) "사용자: " else "도우미: "); append(it.text.take(300)); append('\n') }
            append("\n[지금 사용자 말]\n$userText\n\n")
            append("답변 규칙: reply는 5문장 이내로 핵심만(마크다운 기호 없이 일반 글). stores에는 추천 가게를 후보의 id 그대로 최대 6개(코스면 방문 순서대로)와 한 줄 이유로 넣어. 후보에 없는 가게는 절대 넣지 마. 조건에 맞는 가게가 없으면 stores는 비우고 솔직히 말해. JSON으로만 답해.")
        }
        val result = client.generateJson(listOf(GeminiPart.Text(prompt)), CHAT_SCHEMA, temperature = 0.7)
        val text = when (result) {
            is AppResult.Success -> result.value
            is AppResult.Failure -> return result
        }
        val dto = runCatching { json.decodeFromString<ChatDto>(text) }.getOrNull() ?: return AppError.E_AI_FAILED.asFailure()
        val valid = info.candidates.map { it.store.id }.toSet()
        val picks = dto.stores.filter { it.id in valid }.distinctBy { it.id }.take(6).map { it.id to it.reason.trim() }
        if (dto.reply.isBlank() && picks.isEmpty()) return AppError.E_AI_FAILED.asFailure()
        return ChatReply(dto.reply.trim(), picks).asSuccess()
    }

    /**
     * 단골 충성도 점수(100점 만점): 이 가게의 재방문율·단골 횟수를 다른 가게들(비교 대상)과 견줘 AI가 점수와 이유를 낸다.
     * 기준 점수(백분위 계산)를 근거로 주고, AI 점수는 기준 ±12 안으로 고정해 터무니없는 값이 나오지 않게 한다.
     */
    suspend fun loyalty(
        storeName: String,
        input: com.nopo.cheonnyeon.domain.rule.LoyaltyInput,
        peers: List<com.nopo.cheonnyeon.domain.rule.LoyaltyInput>,
        avgRating: Double?,
    ): AppResult<LoyaltyAnalysis> {
        val rule = com.nopo.cheonnyeon.domain.rule.LoyaltyScore
        val summary = rule.summary(peers)
        val baseline = rule.baseline(input, peers)
        val top = rule.topPercent(input, peers)
        fun pct(x: Double) = "%.0f%%".format(x * 100)
        val prompt = buildString {
            append("너는 노포 맛집의 '단골 충성도'를 분석하는 데이터 분석가야. '$storeName'의 재방문 지표를 다른 가게들과 비교해서 100점 만점으로 점수를 매겨 줘.\n\n")
            append("[이 가게] 방문자 ${input.visitors}명, 다시 온 사람 ${input.revisitors}명(재방문율 ${pct(input.rate)}), 리뷰에 적힌 평균 단골 횟수 ${"%.2f".format(input.avgVisitCount)}회, 리뷰 ${input.reviewCount}개")
            if (avgRating != null) append(", 평균 별점 ${"%.1f".format(avgRating)}")
            append("\n[비교 대상 ${summary.count}곳] 재방문율 평균 ${pct(summary.meanRate)}, 중앙값 ${pct(summary.medianRate)}, 상위 25% 경계 ${pct(summary.p75Rate)}, 평균 단골 횟수 ${"%.2f".format(summary.meanVisits)}회\n")
            append("[계산한 기준] 재방문율은 비교 대상 중 상위 ${top}%, 표본 보정까지 한 기준 점수는 ${baseline}점\n\n")
            append("규칙: score는 기준 점수 ${baseline}에서 ±${rule.AI_ADJUST} 안으로만 조정해(표본이 적으면 기준에 가깝게, 지표가 뚜렷하면 더 벌려). ")
            append("label은 '동네 단골 맛집'처럼 6~12자 이름표. summary는 두 문장 이내로 단골들의 충성도를 평가. reasons는 근거 2~3개(숫자를 인용한 짧은 문장). 한국어 JSON으로만 답해.")
        }
        val result = client.generateJson(listOf(GeminiPart.Text(prompt)), LOYALTY_SCHEMA, temperature = 0.3)
        val text = when (result) {
            is AppResult.Success -> result.value
            is AppResult.Failure -> return result
        }
        val dto = runCatching { json.decodeFromString<LoyaltyDto>(text) }.getOrNull() ?: return AppError.E_AI_FAILED.asFailure()
        if (dto.summary.isBlank()) return AppError.E_AI_FAILED.asFailure()
        return LoyaltyAnalysis(
            score = rule.clamp(dto.score, baseline), baseline = baseline, label = dto.label.trim(), summary = dto.summary.trim(),
            reasons = dto.reasons.map(String::trim).filter(String::isNotEmpty).take(3), peerCount = summary.count, topPercent = top, input = input,
        ).asSuccess()
    }

    /** [texts]를 [language]로 번역한다(순서·개수 유지). 가게 이름·메뉴 이름 같은 고유명사는 원어를 괄호로 함께 둔다 */
    suspend fun translate(texts: List<String>, language: String): AppResult<List<String>> {
        if (texts.isEmpty()) return emptyList<String>().asSuccess()
        // 글이 많으면 한 번에 보내지 않고 묶음으로 나눠 동시에 번역한다(한 번에 40개를 보내면 30초 넘게 걸림). 순서는 유지한다
        val chunks = texts.chunked(TRANSLATE_CHUNK)
        val results = kotlinx.coroutines.coroutineScope {
            chunks.map { chunk -> async { translateChunk(chunk, language) } }.awaitAll()
        }
        val out = ArrayList<String>(texts.size)
        for (r in results) {
            when (r) {
                is AppResult.Success -> out += r.value
                is AppResult.Failure -> return r
            }
        }
        return out.asSuccess()
    }

    private suspend fun translateChunk(texts: List<String>, language: String): AppResult<List<String>> {
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
    suspend fun receiptHint(storeName: String, ocrLines: List<String>, reason: String, imageBase64: String?, subject: String = "영수증"): AppResult<String> {
        val prompt = buildString {
            append("너는 방문 인증 앱의 촬영 도우미야. 사용자가 '$storeName'의 $subject 사진을 찍었는데 인식에 실패했어.\n")
            append("실패 이유: $reason\n")
            append("글자 인식(OCR)이 읽은 줄(최대 25줄):\n")
            if (ocrLines.isEmpty()) append("(읽힌 글자 없음)\n") else ocrLines.take(25).forEachIndexed { i, l -> append("${i + 1}. ${l.take(60)}\n") }
            if (imageBase64 != null) append(if (subject == "영수증") "첨부 사진은 방금 찍은 영수증이야. 사진에서 가게 이름·날짜·시간이 보이는지, 잘렸는지, 흐리거나 어둡거나 빛이 반사되거나 기울었는지, 다른 가게 영수증은 아닌지 살펴봐.\n" else "첨부 사진은 방금 찍은 $subject 사진이야. 가게 이름·전화번호·메뉴 글자가 크고 선명하게 보이는지, 잘렸는지, 너무 멀거나 흐리거나 어둡거나 반사되는지, 다른 가게는 아닌지 살펴봐.\n")
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
        /** 번역을 한 번에 보내는 글 수(묶음별로 동시에 요청) */
        const val TRANSLATE_CHUNK = 8
        const val MAX_CHAT_TURNS = 8

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
        val CHAT_SCHEMA: JsonElement = obj(mapOf("reply" to str(), "stores" to arr(obj(mapOf("id" to str(), "reason" to str())))))
        val LOYALTY_SCHEMA: JsonElement = obj(mapOf("score" to buildJsonObject { put("type", "INTEGER") }, "label" to str(), "summary" to str(), "reasons" to arr(str())))
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
