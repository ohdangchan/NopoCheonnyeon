package com.nopo.cheonnyeon.data.ai

import com.nopo.cheonnyeon.core.common.AppError
import com.nopo.cheonnyeon.core.common.AppResult
import com.nopo.cheonnyeon.core.common.asFailure
import com.nopo.cheonnyeon.core.common.asSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.net.HttpURLConnection
import java.net.URL

/** Gemini에 보내는 내용 한 조각 */
sealed interface GeminiPart {
    data class Text(val text: String) : GeminiPart

    /** [base64]는 NO_WRAP base64 문자열 */
    data class Image(val mimeType: String, val base64: String) : GeminiPart
}

/** HTTP 한 번의 결과. 시험에서는 가짜로 바꾼다 */
fun interface GeminiTransport {
    suspend fun post(url: String, idToken: String, body: String): Pair<Int, String>
}

private object HttpTransport : GeminiTransport {
    override suspend fun post(url: String, idToken: String, body: String): Pair<Int, String> = withContext(Dispatchers.IO) {
        val conn = URL(url).openConnection() as HttpURLConnection
        try {
            conn.requestMethod = "POST"
            conn.connectTimeout = 15_000
            conn.readTimeout = 60_000
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("Authorization", "Bearer $idToken")
            conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            code to (stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty())
        } finally {
            conn.disconnect()
        }
    }
}

/**
 * Gemini 호출. 앱에는 Gemini 키가 없다: 우리 서버(Cloud Functions 프록시, firebase/functions)에 로그인 ID 토큰을 보내면
 * 서버가 보관한 키로 Gemini를 부른다. 모델·응답 형식 제한과 하루 호출 수 제한은 서버가 맡는다.
 * [endpoint]가 비어 있거나 로그인하지 않았으면 AI 기능은 꺼진다.
 */
class GeminiClient(
    private val endpoint: String,
    private val tokenProvider: suspend () -> String? = { "" },
    private val transport: GeminiTransport = HttpTransport,
) {
    val available: Boolean get() = endpoint.isNotBlank()

    private val json = Json { ignoreUnknownKeys = true }

    /** [schema]는 Gemini responseSchema(JSON). 성공하면 모델이 만든 JSON 문자열을 돌려준다 */
    suspend fun generateJson(parts: List<GeminiPart>, schema: JsonElement, temperature: Double = 0.9): AppResult<String> {
        if (!available) return AppError.E_AI_FAILED.asFailure()
        return try {
            val token = tokenProvider() ?: return AppError.E_AUTH_REQUIRED.asFailure()
            val (code, text) = transport.post(endpoint, token, buildBody(parts, schema, temperature).toString())
            if (code !in 200..299) return AppError.E_AI_FAILED.asFailure()
            extractText(text)?.asSuccess() ?: AppError.E_AI_FAILED.asFailure()
        } catch (e: CancellationException) {
            throw e
        } catch (_: java.io.IOException) {
            AppError.E_NETWORK.asFailure()
        } catch (_: Exception) {
            AppError.E_AI_FAILED.asFailure()
        }
    }

    internal fun buildBody(parts: List<GeminiPart>, schema: JsonElement, temperature: Double): JsonObject = buildJsonObject {
        put("contents", buildJsonArray {
            add(buildJsonObject {
                put("parts", buildJsonArray {
                    parts.forEach { p ->
                        add(buildJsonObject {
                            when (p) {
                                is GeminiPart.Text -> put("text", p.text)
                                is GeminiPart.Image -> put("inline_data", buildJsonObject { put("mime_type", p.mimeType); put("data", p.base64) })
                            }
                        })
                    }
                })
            })
        })
        put("generationConfig", buildJsonObject {
            put("responseMimeType", "application/json")
            put("responseSchema", schema)
            put("temperature", temperature)
        })
    }

    /** candidates[0].content.parts[0].text. 차단·빈 응답이면 null */
    internal fun extractText(response: String): String? = runCatching {
        val root = json.parseToJsonElement(response).jsonObject
        root["candidates"]?.jsonArray?.firstOrNull()?.jsonObject
            ?.get("content")?.jsonObject?.get("parts")?.jsonArray?.firstOrNull()?.jsonObject
            ?.get("text")?.jsonPrimitive?.contentOrNull
    }.getOrNull()?.takeIf { it.isNotBlank() }
}
