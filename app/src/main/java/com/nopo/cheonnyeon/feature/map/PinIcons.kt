package com.nopo.cheonnyeon.feature.map

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import com.naver.maps.map.overlay.OverlayImage

/** 마커 모양: 방울 핀. 100년 이상(금빛 등급)은 금빛 테두리를 두른다 (FR-MAP-02). 색·테두리 조합별로 한 번만 그려 둔다. */
object PinIcons {
    private val cache = HashMap<Long, OverlayImage>()

    fun pin(fill: Int, goldRim: Int?): OverlayImage {
        val key = fill.toLong() * 31 + (goldRim ?: 0).toLong()
        return cache.getOrPut(key) { OverlayImage.fromBitmap(draw(fill, goldRim)) }
    }

    private fun draw(fill: Int, goldRim: Int?): Bitmap {
        val w = 88
        val h = 112
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val cv = Canvas(bmp)
        val body = Path().apply {
            addCircle(w / 2f, 42f, 36f, Path.Direction.CW)
            moveTo(w / 2f - 24f, 62f)
            lineTo(w / 2f, h - 4f)
            lineTo(w / 2f + 24f, 62f)
            close()
        }
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.style = Paint.Style.FILL
        p.color = fill
        cv.drawPath(body, p)
        p.style = Paint.Style.STROKE
        p.strokeJoin = Paint.Join.ROUND
        if (goldRim != null) {
            p.strokeWidth = 10f
            p.color = goldRim
        } else {
            p.strokeWidth = 4f
            p.color = 0xFFFFFFFF.toInt()
        }
        cv.drawPath(body, p)
        p.style = Paint.Style.FILL
        p.color = 0xFFFFFFFF.toInt()
        cv.drawCircle(w / 2f, 42f, 11f, p)
        return bmp
    }
}
