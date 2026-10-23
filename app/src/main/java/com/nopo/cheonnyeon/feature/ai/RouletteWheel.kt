package com.nopo.cheonnyeon.feature.ai

import android.graphics.BitmapFactory
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.nopo.cheonnyeon.R
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** 룰렛 한 칸: 이름과 사진 */
private class Slice(val name: String, val photo: ImageBitmap)

/**
 * 시안의 "AI 노포 룰렛" 판을 코드로 그린 완전한 동그라미. (시안 그림은 아래쪽이 버튼에 가려져 있어 그림을 그대로 쓰면 잘려 보인다.)
 * 위에서부터 시계 방향: 국밥·냉면·고기·분식·카페·시장맛집. 가운데 AI 메달과 금빛 테두리, 둘레의 불빛은 시안과 같게 그린다.
 */
@Composable
fun RouletteWheel(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    fun asset(n: String): ImageBitmap = BitmapFactory.decodeStream(context.assets.open("menu/$n.webp")).asImageBitmap()
    val slices = remember {
        listOf(
            Slice("국밥", asset("gukbap")), Slice("냉면", asset("naengmyeon")), Slice("고기", asset("gogi")),
            Slice("분식", asset("tteokbokki")),
        )
    }
    val cafe = ImageBitmap.imageResource(R.drawable.mk2_wh_cafe)
    val market = ImageBitmap.imageResource(R.drawable.mk2_wh_market)
    val center = ImageBitmap.imageResource(R.drawable.mk2_wh_center)
    val all = remember(cafe, market) { slices + Slice("카페", cafe) + Slice("시장맛집", market) }
    Box(modifier) {
        Canvas(Modifier.fillMaxSize()) {
            val d = minOf(size.width, size.height)
            val c = Offset(size.width / 2, size.height / 2 + d * 0.02f)
            val r = d / 2 * 0.96f
            // 바깥 테두리(진한 남색 + 금빛 선)
            drawCircle(Color(0xFF1B2433), r, c)
            drawCircle(Color(0xFFC9A24A), r, c, style = Stroke(d * 0.012f))
            val inner = r * 0.84f
            drawCircle(Color(0xFF3A2E1E), inner, c)
            // 여섯 칸
            all.forEachIndexed { i, s ->
                val start = -120f + i * 60f           // 첫 칸(국밥)이 위쪽 가운데
                val path = Path().apply {
                    moveTo(c.x, c.y)
                    arcTo(androidx.compose.ui.geometry.Rect(c.x - inner, c.y - inner, c.x + inner, c.y + inner), start, 60f, false)
                    close()
                }
                clipPath(path) {
                    val mid = Math.toRadians((start + 30f).toDouble())
                    val cx = c.x + (inner * 0.58f * cos(mid)).toFloat()
                    val cy = c.y + (inner * 0.58f * sin(mid)).toFloat()
                    val side = (inner * 1.25f).toInt()
                    drawImage(s.photo, dstOffset = IntOffset((cx - side / 2).toInt(), (cy - side / 2).toInt()), dstSize = IntSize(side, side))
                    drawRect(Color(0x33000000), Offset(c.x - inner, c.y - inner), Size(inner * 2, inner * 2))
                }
                // 칸 이름
                drawContext.canvas.nativeCanvas.apply {
                    val p = android.graphics.Paint().apply {
                        color = android.graphics.Color.WHITE; textSize = d * 0.058f; isAntiAlias = true; isFakeBoldText = true
                        textAlign = android.graphics.Paint.Align.CENTER; setShadowLayer(d * 0.01f, 0f, 0f, android.graphics.Color.BLACK)
                    }
                    drawText(s.name, (c.x + inner * 0.78f * cos(mid(start))).toFloat(), (c.y + inner * 0.78f * sin(mid(start))).toFloat() + d * 0.02f, p)
                }
            }
            // 칸 사이 금빛 선
            repeat(6) { i ->
                val a = Math.toRadians((-120f + i * 60f).toDouble())
                drawLine(Color(0xFFC9A24A), c, Offset(c.x + (inner * cos(a)).toFloat(), c.y + (inner * sin(a)).toFloat()), d * 0.008f)
            }
            drawCircle(Color(0xFFC9A24A), inner, c, style = Stroke(d * 0.012f))
            // 둘레 불빛 12개
            repeat(12) { i ->
                val a = Math.toRadians((-90 + i * 30).toDouble())
                val p = Offset(c.x + (r * 0.92f * cos(a)).toFloat(), c.y + (r * 0.92f * sin(a)).toFloat())
                drawCircle(Color(0xFFFFE6A0), d * 0.016f, p)
                drawCircle(Color(0xFFFFB534), d * 0.011f, p)
            }
            // 가운데 AI 메달
            val cs = (inner * 0.4f).toInt() * 2
            drawImage(center, dstOffset = IntOffset((c.x - cs / 2).toInt(), (c.y - cs / 2).toInt()), dstSize = IntSize(cs, cs))
            // 위쪽 바늘
            val tip = Path().apply {
                moveTo(c.x, c.y - r - d * 0.015f); lineTo(c.x + d * 0.04f, c.y - r + d * 0.05f); lineTo(c.x, c.y - r + d * 0.1f); lineTo(c.x - d * 0.04f, c.y - r + d * 0.05f); close()
            }
            drawPath(tip, Color(0xFFD9A323)); drawPath(tip, Color(0xFF6B4A0E), style = Stroke(d * 0.006f))
        }
    }
}

private fun mid(start: Float): Double = Math.toRadians((start + 30f).toDouble())

