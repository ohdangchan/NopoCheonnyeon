package com.nopo.cheonnyeon.platform.ocr

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions
import kotlinx.coroutines.tasks.await
import java.io.Closeable

/** 인식된 줄 목록. 이미지 자체는 들고 있지 않는다. */
data class OcrResult(val lines: List<String>) {
    val fullText: String get() = lines.joinToString("\n")
}

/** ML Kit 한국어 텍스트 인식(앱 포함 모델, 오프라인 동작). 쓰는 쪽에서 [close]를 반드시 호출한다. */
class MlKitKoreanRecognizer : Closeable {
    private val recognizer = TextRecognition.getClient(KoreanTextRecognizerOptions.Builder().build())

    /** [bitmap]은 이미 똑바로 세워진 상태여야 한다(회전 0으로 넘김). */
    suspend fun read(bitmap: Bitmap): OcrResult {
        val text = recognizer.process(InputImage.fromBitmap(bitmap, 0)).await()
        return OcrResult(text.textBlocks.flatMap { b -> b.lines.map { it.text } })
    }

    override fun close() = recognizer.close()
}
