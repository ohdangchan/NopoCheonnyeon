package com.nopo.cheonnyeon.data.ai

import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import com.nopo.cheonnyeon.core.common.AppError
import com.nopo.cheonnyeon.core.common.AppResult
import com.nopo.cheonnyeon.core.common.asFailure
import com.nopo.cheonnyeon.core.common.asSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.JsonElement

/** Gemini에 보내는 내용 한 조각 */
sealed interface GeminiPart {
    data class Text(val text: String) : GeminiPart

    /** [base64]는 NO_WRAP base64 문자열 */
    data class Image(val mimeType: String, val base64: String) : GeminiPart
}

/** 모델 호출 한 번. 성공하면 모델이 만든 JSON 문자열, 실패하면 예외. 시험에서는 가짜로 바꾼다 */
fun interface GeminiTransport {
    suspend fun generate(parts: List<GeminiPart>, temperature: Double, model: String): String?
}

/** Firebase AI Logic(Gemini Developer API). 키는 앱에 없고 Firebase 프로젝트가 보관한다 */
private object FirebaseTransport : GeminiTransport {
    override suspend fun generate(parts: List<GeminiPart>, temperature: Double, model: String): String? {
        val m = Firebase.ai(backend = GenerativeBackend.googleAI()).generativeModel(
            modelName = model,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                this.temperature = temperature.toFloat()
            },
        )
        val request = content {
            parts.forEach { p ->
                when (p) {
                    is GeminiPart.Text -> text(p.text)
                    is GeminiPart.Image -> inlineData(android.util.Base64.decode(p.base64, android.util.Base64.DEFAULT), p.mimeType)
                }
            }
        }
        return try {
            m.generateContent(request).text
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // 원인 확인용: 종류와 짧은 설명만 남긴다(요청 내용·키 없음)
            android.util.Log.w("Nopo", "AI 호출 실패 " + e.javaClass.simpleName + ": " + e.message?.take(300))
            throw e
        }
    }
}

/**
 * Gemini 호출(Firebase AI Logic). 앱에는 Gemini 키가 없다. 응답 형식(스키마)은 프롬프트 끝에 붙여 JSON으로만 답하게 한다.
 * [model]이 비어 있으면 AI 기능은 꺼진다.
 */
class GeminiClient(
    private val model: String = DEFAULT_MODEL,
    private val transport: GeminiTransport = FirebaseTransport,
) {
    val available: Boolean get() = model.isNotBlank()

    /** [schema]는 응답 JSON 모양(Gemini responseSchema 형식). 성공하면 모델이 만든 JSON 문자열을 돌려준다 */
    suspend fun generateJson(parts: List<GeminiPart>, schema: JsonElement, temperature: Double = 0.9): AppResult<String> {
        if (!available) return AppError.E_AI_FAILED.asFailure()
        val withSchema = parts + GeminiPart.Text("반드시 아래 JSON 스키마(OpenAPI 형식)를 그대로 지키는 JSON 하나만 답해. 설명·코드블록 금지.\n$schema")
        return try {
            val text = transport.generate(withSchema, temperature, model)?.trim().orEmpty()
            clean(text)?.asSuccess() ?: AppError.E_AI_FAILED.asFailure()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (generateSequence<Throwable>(e) { it.cause }.any { it is java.io.IOException }) AppError.E_NETWORK.asFailure() else AppError.E_AI_FAILED.asFailure()
        }
    }

    /** 혹시 붙은 ```json 코드블록 표시를 벗기고, 비었으면 null */
    internal fun clean(text: String): String? =
        text.removePrefix("```json").removePrefix("```").removeSuffix("```").trim().takeIf { it.isNotBlank() }

    companion object {
        /** 빠르고 저렴한 모델. 바꾸려면 이 한 곳만 */
        const val DEFAULT_MODEL = "gemini-3.1-flash-lite"
    }
}
