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
    suspend fun post(url: String, apiKey: String, body: String): Pair<Int, String>
}

private object HttpTransport : GeminiTransport {
    override suspend fun post(url: String, apiKey: String, body: String): Pair<Int, String> = withContext(Dispatchers.IO) {
        val conn = URL(url).openConnection() as HttpURLConnection
        try {
            conn.requestMethod = "POST"
            conn.connectTimeout = 15_000
            conn.readTimeout = 60_000
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("x-goog-api-key", apiKey)
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
 * Google Gemini REST 호출(generateContent, JSON 응답 모드). 키는 빌드 때 환경변수 GEMINI_API_KEY에서 BuildConfig로 들어오며
 * 코드·로그·오류 문구에 남기지 않는다. 키가 비어 있으면 [available]이 false다.
 * 주의: 앱에 들어가는 키는 추출될 수 있으니 GCP 콘솔에서 앱(패키지·SHA-1)과 API로 제한하고 예산 알림을 걸 것.
 */
class GeminiClient(
    private val apiKey: String,
    private val model: String = DEFAULT_MODEL,
    private val transport: GeminiTransport = HttpTransport,
) {
    val available: Boolean get() = apiKey.isNotBlank()

    private val json = Json { ignoreUnknownKeys = true }

    /** [schema]는 Gemini responseSchema(JSON). 성공하면 모델이 만든 JSON 문자열을 돌려준다 */
    suspend fun generateJson(parts: List<GeminiPart>, schema: JsonElement, temperature: Double = 0.9): AppResult<String> {
        if (!available) return AppError.E_AI_FAILED.asFailure()
        return try {
            val (code, text) = transport.post(url(), apiKey, buildBody(parts, schema, temperature).toString())
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

    internal fun url() = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent"

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

    companion object {
        /** 빠르고 저렴한 모델(응답 약 3초). 바꾸려면 이 한 곳만 */
        const val DEFAULT_MODEL = "gemini-3.1-flash-lite"
    }
}
