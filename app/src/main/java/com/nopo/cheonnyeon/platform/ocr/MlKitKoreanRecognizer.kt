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

/** 인식된 줄과 그 위치(이미지 크기에 대한 0~1 비율) */
data class OcrBox(val text: String, val left: Float, val top: Float, val right: Float, val bottom: Float)

data class OcrScan(val lines: List<String>, val boxes: List<OcrBox>)

/** ML Kit 한국어 텍스트 인식(앱 포함 모델, 오프라인 동작). 쓰는 쪽에서 [close]를 반드시 호출한다. */
class MlKitKoreanRecognizer : Closeable {
    private val recognizer = TextRecognition.getClient(KoreanTextRecognizerOptions.Builder().build())

    /** [bitmap]은 이미 똑바로 세워진 상태여야 한다(회전 0으로 넘김). */
    suspend fun read(bitmap: Bitmap): OcrResult {
        val text = recognizer.process(InputImage.fromBitmap(bitmap, 0)).await()
        return OcrResult(text.textBlocks.flatMap { b -> b.lines.map { it.text } })
    }

    /** 글자 줄과 위치(형광펜 연출용). 위치는 비율이라 화면에 어떤 크기로 그려도 맞는다 */
    suspend fun readScan(bitmap: Bitmap): OcrScan {
        val text = recognizer.process(InputImage.fromBitmap(bitmap, 0)).await()
        val w = bitmap.width.toFloat().coerceAtLeast(1f)
        val h = bitmap.height.toFloat().coerceAtLeast(1f)
        val lines = text.textBlocks.flatMap { it.lines }
        val boxes = lines.mapNotNull { l ->
            l.boundingBox?.let { b -> OcrBox(l.text, b.left / w, b.top / h, b.right / w, b.bottom / h) }
        }
        return OcrScan(lines.map { it.text }, boxes)
    }

    override fun close() = recognizer.close()
}
