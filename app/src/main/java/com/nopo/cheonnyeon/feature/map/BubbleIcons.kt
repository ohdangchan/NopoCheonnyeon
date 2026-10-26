package com.nopo.cheonnyeon.feature.map

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import androidx.core.content.res.ResourcesCompat
import coil3.SingletonImageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.toBitmap
import com.naver.maps.map.overlay.OverlayImage
import com.nopo.cheonnyeon.R
import com.nopo.cheonnyeon.domain.model.Store

/** 시안 지도의 사진 말풍선 마커: 동그란 가게 사진 + 상호 + 별점, 아래쪽에 남색 꼬리 */
object BubbleIcons {
    const val W = 330
    const val H = 150
    private val cache = HashMap<String, OverlayImage>()

    fun cached(key: String): OverlayImage? = cache[key]

    suspend fun load(context: Context, store: Store, rating: Double?): OverlayImage? {
        val key = "${store.id}:${rating ?: 0.0}"
        cache[key]?.let { return it }
        val url = store.photoUrls.firstOrNull() ?: return null
        val req = ImageRequest.Builder(context).data(url).size(240, 240).allowHardware(false).build()
        val photo = (SingletonImageLoader.get(context).execute(req) as? SuccessResult)?.image?.toBitmap() ?: return null
        val icon = OverlayImage.fromBitmap(draw(context, store.name, rating, photo))
        cache[key] = icon
        return icon
    }

    private fun draw(context: Context, name: String, rating: Double?, photo: Bitmap): Bitmap {
        val bmp = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888)
        val cv = Canvas(bmp)
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        val r = 56f
        val cx = 64f
        val cy = 62f
        // 글자 상자
        p.color = 0x33000000
        cv.drawRoundRect(RectF(cx + 8f, 24f, W - 6f, 104f), 36f, 36f, p)
        p.color = 0xFFFFFFFF.toInt()
        cv.drawRoundRect(RectF(cx + 4f, 20f, W - 10f, 100f), 36f, 36f, p)
        // 꼬리
        p.color = 0xFF061A31.toInt()
        val tail = Path().apply { moveTo(cx - 20f, H - 40f); lineTo(cx + 20f, H - 40f); lineTo(cx, H - 10f); close() }
        cv.drawPath(tail, p)
        // 사진 동그라미
        p.color = 0xFFFFFFFF.toInt()
        cv.drawCircle(cx, cy, r + 5f, p)
        val clip = Path().apply { addCircle(cx, cy, r, Path.Direction.CW) }
        cv.save()
        cv.clipPath(clip)
        val side = minOf(photo.width, photo.height)
        val src = android.graphics.Rect((photo.width - side) / 2, (photo.height - side) / 2, (photo.width + side) / 2,
            (photo.height + side) / 2)
        cv.drawBitmap(photo, src, RectF(cx - r, cy - r, cx + r, cy + r), p)
        cv.restore()
        // 글자
        val face = ResourcesCompat.getFont(context, R.font.nanum_myeongjo_extrabold) ?: Typeface.DEFAULT_BOLD
        val tp = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF061A31.toInt(); typeface = face; textSize = 31f }
        val label = if (name.length > 7) name.take(7) + "…" else name
        cv.drawText(label, cx + 24f + 8f, 58f, tp)
        if (rating != null) {
            tp.typeface = Typeface.DEFAULT_BOLD
            tp.textSize = 28f
            tp.color = 0xFFF5B800.toInt()
            cv.drawText("★", cx + 32f, 92f, tp)
            tp.color = 0xFF061A31.toInt()
            cv.drawText(String.format("%.1f", rating), cx + 66f, 92f, tp)
        }
        return bmp
    }
}
