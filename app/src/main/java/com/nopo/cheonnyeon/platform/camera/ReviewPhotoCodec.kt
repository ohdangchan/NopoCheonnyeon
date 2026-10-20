package com.nopo.cheonnyeon.platform.camera

import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.Build
import android.util.Base64
import androidx.camera.core.ImageProxy
import java.io.ByteArrayOutputStream
import kotlin.math.max

/**
 * 리뷰 사진: 앱 카메라로 찍은 장면을 파일 없이 줄여 WebP 문자열(base64)로 만든다.
 * 갤러리에서 고르는 경로는 없다. 긴 변은 [MAX_SIDE]px, 용량은 [MAX_BYTES] 이하(서버 규칙은 문자열 길이 170,000 이하).
 */
object ReviewPhotoCodec {
    const val MAX_SIDE = 720
    const val MAX_BYTES = 110_000

    /** 프레임을 닫는 일은 호출한 쪽([CameraController.captureInMemory])이 한다. 실패하면 null */
    fun encode(frame: ImageProxy): String? = runCatching {
        encode(frame.toBitmap(), frame.imageInfo.rotationDegrees)
    }.getOrNull()

    fun encode(source: Bitmap, rotationDegrees: Int): String? {
        val scale = MAX_SIDE.toFloat() / max(source.width, source.height)
        val m = Matrix().apply {
            if (scale < 1f) postScale(scale, scale)
            if (rotationDegrees != 0) postRotate(rotationDegrees.toFloat())
        }
        val bmp = Bitmap.createBitmap(source, 0, 0, source.width, source.height, m, true)
        var quality = 72
        while (quality >= 30) {
            val out = ByteArrayOutputStream()
            bmp.compress(webp(), quality, out)
            if (out.size() <= MAX_BYTES) return Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
            quality -= 12
        }
        return null
    }

    fun decode(photo: String): ByteArray? = runCatching { Base64.decode(photo, Base64.DEFAULT) }.getOrNull()

    @Suppress("DEPRECATION")
    private fun webp() = if (Build.VERSION.SDK_INT >= 30) Bitmap.CompressFormat.WEBP_LOSSY else Bitmap.CompressFormat.WEBP
}
