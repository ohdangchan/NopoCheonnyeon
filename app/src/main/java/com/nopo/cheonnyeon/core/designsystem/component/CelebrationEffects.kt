package com.nopo.cheonnyeon.core.designsystem.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import com.nopo.cheonnyeon.core.designsystem.theme.NopoTheme
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * 도장이 찍힌 직후 화면 전체에 터지는 축하 효과(폭죽 + 색종이). 눌러도 막지 않는다.
 * [fire]가 true가 되는 순간 한 번 재생하고, [tier](0~10)가 높을수록 화려하다. 애니메이션을 끈 기기에서는 그리지 않는다.
 */
@Composable
fun CelebrationOverlay(fire: Boolean, tier: Int, animate: Boolean, modifier: Modifier = Modifier) {
    val c = NopoTheme.colors
    val burst = remember { Animatable(0f) }
    val confetti = remember { Animatable(0f) }
    LaunchedEffect(fire, animate) {
        if (!fire || !animate) return@LaunchedEffect
        coroutineScope {
            launch { confetti.animateTo(1f, tween(3800, easing = LinearEasing)) }
            launch { burst.animateTo(1f, tween(1500, easing = LinearOutSlowInEasing)) }
        }
    }
    val colors = listOf(c.brass, c.stamp, c.primary, Color.White, c.tealSoft, c.brassSoft)
    Canvas(modifier.fillMaxSize()) {
        val center = Offset(size.width / 2, size.height * 0.28f)
        drawFireworks(center, burst.value, tier, colors)
        drawConfetti(confetti.value, 30 + tier * 6, colors)
    }
}

/** 폭죽: 배지 주변 세 곳에서 순서대로 터지며 중력으로 떨어진다. 칭호가 높을수록 더 많이·더 많은 곳에서 */
internal fun DrawScope.drawFireworks(center: Offset, t: Float, tier: Int, colors: List<Color>) {
    if (t <= 0f || t >= 1f) return
    val spots = when {
        tier >= 9 -> 5
        tier >= 6 -> 4
        tier >= 3 -> 3
        else -> 2
    }
    val anchors = listOf(Offset(0f, 0f), Offset(-130f, 90f), Offset(130f, 70f), Offset(-70f, -110f), Offset(80f, -120f))
    repeat(spots) { s ->
        val start = s * 0.13f
        val lt = ((t - start) / (1f - start)).coerceIn(0f, 1f)
        if (lt <= 0f) return@repeat
        val origin = Offset(center.x + anchors[s].x.dp.toPx(), center.y + anchors[s].y.dp.toPx())
        val sparks = 26 + tier * 2
        val rnd = Random(s * 31 + 7)
        for (i in 0 until sparks) {
            val ang = (i * 2 * PI / sparks).toFloat() + rnd.nextFloat() * 0.2f
            val speed = (110 + rnd.nextInt(90)).dp.toPx()
            val d = speed * lt
            val x = origin.x + cos(ang) * d
            val y = origin.y + sin(ang) * d + 90.dp.toPx() * lt * lt
            val a = (1f - lt).coerceIn(0f, 1f)
            val col = colors[(i + s) % colors.size].copy(alpha = a)
            drawCircle(col, (3.5f * (1f - lt * 0.5f)).dp.toPx(), Offset(x, y))
            // 꼬리
            drawLine(
                col.copy(alpha = a * 0.5f),
                Offset(x, y),
                Offset(x - cos(ang) * 10.dp.toPx() * (1f - lt), y - sin(ang) * 10.dp.toPx() * (1f - lt)),
                2.dp.toPx()
            )
        }
        drawCircle(
            colors[s % colors.size].copy(alpha = (1f - lt) * 0.5f),
            120.dp.toPx() * lt,
            origin,
            style = Stroke(4.dp.toPx() * (1f - lt))
        )
    }
}

/** 화면 위에서 흔들리며 떨어지는 색종이 */
internal fun DrawScope.drawConfetti(progress: Float, count: Int, colors: List<Color>) {
    if (progress <= 0f || progress >= 1f) return
    for (i in 0 until count) {
        val r = Random(i * 97 + 13)
        val delay = r.nextFloat() * 0.45f
        val t = ((progress - delay) / (1f - delay)).coerceIn(0f, 1f)
        if (t <= 0f) continue
        val x = r.nextFloat() * size.width + sin(t * 7f + i) * 26.dp.toPx()
        val y = -24.dp.toPx() + t * (size.height + 48.dp.toPx())
        val w = (5 + r.nextInt(7)).dp.toPx()
        val h = w * (0.5f + r.nextFloat() * 0.8f)
        val a = (1f - ((t - 0.85f) / 0.15f).coerceIn(0f, 1f))
        rotate(t * (360f + r.nextInt(720)) * (if (i % 2 == 0) 1 else -1), Offset(x, y)) {
            drawRect(colors[i % colors.size].copy(alpha = a), Offset(x - w / 2, y - h / 2), Size(w, h))
        }
    }
}

