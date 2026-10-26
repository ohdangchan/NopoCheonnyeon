package com.nopo.cheonnyeon.data.ai

import android.content.Context
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/** 챗봇이 추천한 가게 한 곳(눌러서 상세로 이동) */
@Serializable
data class ChatStoreRef(val id: String, val name: String, val years: Int, val reason: String)

@Serializable
data class ChatMessage(
    val id: Long,
    /** "user" 또는 "bot" */
    val role: String,
    val text: String,
    val at: Long,
    val stores: List<ChatStoreRef> = emptyList(),
) {
    val isUser: Boolean get() = role == ROLE_USER

    companion object {
        const val ROLE_USER = "user"
        const val ROLE_BOT = "bot"
    }
}

/**
 * 챗봇 대화 기록을 이 기기의 저장 공간(앱 전용 폴더의 한 줄에 메시지 하나인 파일)에 쌓는다. 서버에는 올리지 않는다.
 * 앱을 껐다 켜도 예전 기록을 모두 볼 수 있고, "기록 지우기"로 지울 수 있다.
 */
class ChatStore(context: Context, private val io: CoroutineDispatcher) {
    private val dir = File(context.filesDir, "chat")
    /** 계정마다 따로 저장해 다른 계정으로 로그인해도 기록이 섞이지 않는다 */
    private fun file(owner: String) = File(
        dir,
        owner.filter { it.isLetterOrDigit() || it == '_' || it == '-' }.ifBlank { "guest" } + ".jsonl"
    )
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun load(owner: String): List<ChatMessage> = withContext(io) {
        val file = file(owner)
        if (!file.exists()) return@withContext emptyList()
        file.readLines(Charsets.UTF_8).mapNotNull { line -> runCatching { json.decodeFromString<ChatMessage>(line) }.getOrNull() }
    }

    suspend fun append(owner: String, vararg messages: ChatMessage) = withContext(io) {
        val file = file(owner)
        file.parentFile?.mkdirs()
        file.appendText(messages.joinToString("") { json.encodeToString(it) + "\n" }, Charsets.UTF_8)
    }

    suspend fun clear(owner: String) = withContext(io) { file(owner).delete(); Unit }
}
