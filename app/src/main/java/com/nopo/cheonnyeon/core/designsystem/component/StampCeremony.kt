package com.nopo.cheonnyeon.core.designsystem.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.nopo.cheonnyeon.core.designsystem.theme.NopoTheme
import com.nopo.cheonnyeon.domain.model.StampGrade
import kotlinx.coroutines.launch
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private const val BURST_COUNT = 22
private const val SPARKLE_COUNT = 12

/**
 * 도장 찍기 연출 (FR-STP-02): 위에서 크게 내려와 "쿵" 하고 찍히면 충격 고리가 퍼지고 인주가 사방으로 튄다.
 * 도장은 살짝 눌렸다 되돌아오고 화면이 한 번 흔들린다. 백년(금빛) 도장은 반짝이는 별이 둘러싼다.
 * 세월 숫자는 찍힌 직후부터 0에서 [years]까지 올라간다. [animate]가 false(시스템 애니메이션 끔)면 처음부터 찍힌 모습이다.
 * [onImpact]는 도장이 닿는 순간 한 번 불린다(진동용).
 */
@Composable
fun StampCeremony(
    years: Int,
    grade: StampGrade,
    animate: Boolean,
    modifier: Modifier = Modifier,
    onImpact: () -> Unit = {},
) {
    val c = NopoTheme.colors
    val ink = when (grade) {
        StampGrade.NORMAL -> c.primary
        StampGrade.RED -> c.stamp
        StampGrade.GOLD -> c.brass
    }
    val drop = remember { Animatable(if (animate) 0f else 1f) }      // 0 → 1: 내려오는 중
    val impact = remember { Animatable(if (animate) 0f else 1f) }    // 0 → 1: 찍힌 뒤 퍼지는 효과
    val squash = remember { Animatable(1f) }                         // 찍히며 눌림
    val shake = remember { Animatable(0f) }                          // 화면 흔들림(dp)
    var shownYears by remember { mutableIntStateOf(if (animate) 0 else years) }

    LaunchedEffect(animate) {
        if (!animate) return@LaunchedEffect
        drop.animateTo(1f, tween(340, easing = FastOutSlowInEasing))
        onImpact()
        coroutineScope {
            launch { impact.animateTo(1f, tween(900, easing = LinearOutSlowInEasing)) }
            launch {
                squash.animateTo(0.9f, tween(70, easing = LinearEasing))
                squash.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium))
            }
            launch {
                for (i in 0 until 6) {
                    shake.animateTo(if (i % 2 == 0) 5f - i else -(5f - i), tween(45))
                }
                shake.animateTo(0f, tween(45))
            }
            launch {
                val steps = 24
                for (i in 1..steps) {
                    shownYears = years * i / steps
                    delay(25)
                }
                shownYears = years
            }
        }
    }

    val twinkle = rememberInfiniteTransition(label = "twinkle")
    val phase by twinkle.animateFloat(0f, 1f, infiniteRepeatable(tween(1400, easing = LinearEasing), RepeatMode.Restart), label = "phase")

    val box = StampSize.Ceremony.dp * 1.9f
    Box(
        modifier = modifier
            .size(box)
            .graphicsLayer { translationY = shake.value * density }
            .drawBehind {
                val center = Offset(size.width / 2, size.height / 2)
                val t = impact.value
                if (t > 0f && t < 1f) {
                    drawShockwave(center, ink, t)
                    drawInkBurst(center, ink, t)
                }
                if (grade == StampGrade.GOLD && impact.value >= 1f) drawSparkles(center, c.brass, phase)
            },
        contentAlignment = Alignment.Center,
    ) {
        val d = drop.value
        val scale = (2.4f - 1.4f * d) * squash.value
        StampView(
            years = shownYears,
            grade = grade,
            size = StampSize.Ceremony,
            modifier = Modifier.graphicsLayer {
                scaleX = scale
                scaleY = scale
                rotationZ = -16f + 10f * d
                alpha = 0.15f + 0.85f * d
            },
        )
    }
}

/** 충격 고리: 도장 가장자리에서 바깥으로 퍼지며 사라진다 */
private fun DrawScope.drawShockwave(center: Offset, ink: Color, t: Float) {
    val base = StampSize.Ceremony.dp.toPx() / 2
    val r = base * (1f + 0.55f * t)
    drawCircle(ink.copy(alpha = (1f - t) * 0.55f), r, center, style = Stroke(width = (7.dp.toPx() * (1f - t)).coerceAtLeast(0.5f)))
    val r2 = base * (1f + 0.9f * t)
    drawCircle(ink.copy(alpha = (1f - t) * 0.25f), r2, center, style = Stroke(width = 2.dp.toPx()))
}

/** 인주 튀김: 정해진 각도로 작은 방울과 짧은 줄이 퍼진다 */
private fun DrawScope.drawInkBurst(center: Offset, ink: Color, t: Float) {
    val base = StampSize.Ceremony.dp.toPx() / 2
    for (i in 0 until BURST_COUNT) {
        val angle = (i * 2f * PI / BURST_COUNT).toFloat() + (i % 3) * 0.07f
        val far = 1.05f + (i % 4) * 0.12f
        val dist = base * (far + 0.55f * t * (0.8f + (i % 5) * 0.12f))
        val x = center.x + cos(angle) * dist
        val y = center.y + sin(angle) * dist
        val a = (1f - t).coerceIn(0f, 1f)
        if (i % 2 == 0) {
            drawCircle(ink.copy(alpha = a * 0.9f), (2.5f + (i % 3)).dp.toPx() * (1f - t * 0.6f), Offset(x, y))
        } else {
            val tail = 9.dp.toPx() * (1f - t)
            drawLine(ink.copy(alpha = a * 0.8f), Offset(x, y), Offset(x - cos(angle) * tail, y - sin(angle) * tail), strokeWidth = 2.dp.toPx())
        }
    }
}

/** 금빛 도장: 별이 깜박이며 둘러싼다 */
private fun DrawScope.drawSparkles(center: Offset, color: Color, phase: Float) {
    val base = StampSize.Ceremony.dp.toPx() / 2
    for (i in 0 until SPARKLE_COUNT) {
        val angle = (i * 2f * PI / SPARKLE_COUNT).toFloat() + 0.2f
        val dist = base * (1.18f + (i % 3) * 0.1f)
        val p = (phase + i / SPARKLE_COUNT.toFloat()) % 1f
        val s = (sin(p * PI).toFloat()).coerceIn(0f, 1f)
        if (s <= 0.02f) continue
        star(Offset(center.x + cos(angle) * dist, center.y + sin(angle) * dist), (3f + (i % 3) * 2f).dp.toPx() * s, color.copy(alpha = s))
    }
}

private fun DrawScope.star(c: Offset, r: Float, color: Color) {
    val inner = r * 0.28f
    val path = Path()
    for (k in 0 until 8) {
        val rad = if (k % 2 == 0) r else inner
        val a = (k * PI / 4).toFloat() - (PI / 2).toFloat()
        val x = c.x + cos(a) * rad
        val y = c.y + sin(a) * rad
        if (k == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    drawPath(path, color)
}

@NopoPreviews
@Composable
private fun StampCeremonyPreview() = PreviewSurface {
    StampCeremony(years = 46, grade = StampGrade.RED, animate = false)
    StampCeremony(years = 126, grade = StampGrade.GOLD, animate = false)
}
