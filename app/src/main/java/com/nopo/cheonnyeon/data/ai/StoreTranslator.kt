package com.nopo.cheonnyeon.data.ai

import com.nopo.cheonnyeon.core.common.AppError
import com.nopo.cheonnyeon.core.common.AppResult
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** 지금 보고 있는 가게의 번역 상태. [map]은 원문 → 번역문 */
data class StoreTranslation(
    val storeId: String,
    val language: String,
    val map: Map<String, String> = emptyMap(),
    val loading: Boolean = false,
    val error: AppError? = null,
) {
    fun pick(text: String): String = map[text] ?: text
    val active: Boolean get() = map.isNotEmpty()
}

/**
 * 가게 소개글·리뷰 번역(Gemini). 가게 상세 화면이 번역할 글(소개글, 한 줄 소개, 리뷰)을 [addSources]로 모아 두고,
 * 사용자가 언어를 고르면 한 번에 번역한다. 번역문은 화면에서 원문 대신 보이며 저장하지 않는다.
 */
class StoreTranslator(private val ai: AiRepository) {
    private val sources = LinkedHashMap<String, LinkedHashSet<String>>()
    private val cache = HashMap<String, Map<String, String>>()
    private val _state = MutableStateFlow<StoreTranslation?>(null)
    val state: StateFlow<StoreTranslation?> = _state.asStateFlow()

    val available: Boolean get() = ai.available

    /** 번역 대상 글을 등록한다(이미 번역 중인 언어가 있으면 새 글은 다음 번역 때 포함) */
    fun addSources(storeId: String, texts: Collection<String>) {
        val set = sources.getOrPut(storeId) { LinkedHashSet() }
        texts.filter { it.isNotBlank() }.forEach { set.add(it.trim()) }
    }

    suspend fun translate(storeId: String, language: String) {
        val lang = language.trim().take(40)
        if (lang.isEmpty()) return
        val texts = sources[storeId].orEmpty().toList().take(MAX_TEXTS)
        if (texts.isEmpty()) return
        val key = storeId + "|" + lang + "|" + texts.hashCode()
        cache[key]?.let { _state.value = StoreTranslation(storeId, lang, it); return }
        _state.value = StoreTranslation(storeId, lang, loading = true)
        // 소개글(앞의 두 개)을 먼저 따로 번역해 바로 보여 주고, 나머지(리뷰)는 위에서부터 작게 나눠 동시에 번역한다.
        // 한 묶음이 끝날 때마다 화면에 반영한다.
        val chunks = listOf(texts.take(INTRO_COUNT)) + texts.drop(INTRO_COUNT).chunked(CHUNK)
        val done = LinkedHashMap<String, String>()
        var failure: AppError? = null
        val lock = Any()
        kotlinx.coroutines.coroutineScope {
            chunks.filter { it.isNotEmpty() }.map { chunk ->
                async {
                    when (val r = ai.translateChunk(chunk, lang)) {
                        is AppResult.Success -> synchronized(lock) {
                            done.putAll(chunk.zip(r.value))
                            _state.value = StoreTranslation(storeId, lang, LinkedHashMap(done), loading = true)
                        }
                        is AppResult.Failure -> synchronized(lock) { failure = r.error }
                    }
                }
            }.awaitAll()
        }
        if (done.isNotEmpty()) cache[key] = LinkedHashMap(done)
        _state.value = if (done.isEmpty()) StoreTranslation(storeId, lang, error = failure ?: AppError.E_AI_FAILED) else StoreTranslation(storeId, lang, LinkedHashMap(done))
    }

    /** 원문으로 되돌린다 */
    fun clear() = _state.update { null }

    private companion object {
        const val MAX_TEXTS = 40
        const val INTRO_COUNT = 2
        const val CHUNK = 4
    }
}
