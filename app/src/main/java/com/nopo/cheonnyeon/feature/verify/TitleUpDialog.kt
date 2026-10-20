package com.nopo.cheonnyeon.feature.verify

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.nopo.cheonnyeon.R
import com.nopo.cheonnyeon.core.designsystem.component.NopoButton
import com.nopo.cheonnyeon.core.designsystem.component.TitleBadge
import com.nopo.cheonnyeon.core.designsystem.component.YearsGauge
import com.nopo.cheonnyeon.core.designsystem.component.labelRes
import com.nopo.cheonnyeon.core.designsystem.component.drawConfetti
import com.nopo.cheonnyeon.core.designsystem.component.drawFireworks
import com.nopo.cheonnyeon.core.designsystem.component.star
import com.nopo.cheonnyeon.core.designsystem.theme.NopoTheme
import com.nopo.cheonnyeon.domain.model.Title
import com.nopo.cheonnyeon.domain.rule.TitlePolicy
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * 칭호 승급 연출 (FR-STP-06, S-705): 어두운 무대에 빛줄기가 돌고, 배지가 뒤집히며 튀어나오고,
 * 색종이가 쏟아지고 폭죽이 터지며, 칭호 이름이 한 글자씩 나타난다. 높은 칭호일수록 폭죽·색종이가 늘어난다.
 * 애니메이션을 끈 기기에서는 최종 모습만 바로 보여 준다.
 */
@Composable
fun TitleUpDialog(title: Title, totalYears: Int, preview: Boolean = false, onDismiss: () -> Unit) {
    val c = NopoTheme.colors
    val context = LocalContext.current
    val animate = remember {
        android.provider.Settings.Global.getFloat(context.contentResolver, android.provider.Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f
    }
    val name = stringResource(title.labelRes())
    val previous = Title.entries.getOrNull(title.ordinal - 1)
    val previousName = previous?.let { stringResource(it.labelRes()) }

    val enter = remember { Animatable(if (animate) 0f else 1f) }       // 배지 등장(튀어나옴)
    val flip = remember { Animatable(if (animate) 90f else 0f) }       // 배지 뒤집힘
    val burst = remember { Animatable(if (animate) 0f else 1f) }       // 폭죽·충격 고리
    val confetti = remember { Animatable(if (animate) 0f else 1f) }    // 색종이 낙하
    var typed by remember { mutableIntStateOf(if (animate) 0 else name.length) }
    val tier = title.ordinal                                           // 0..10

    LaunchedEffect(animate) {
        if (!animate) return@LaunchedEffect
        coroutineScope {
            launch { confetti.animateTo(1f, tween(4200, easing = LinearEasing)) }
            launch {
                delay(250)
                launch { flip.animateTo(0f, spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessLow)) }
                enter.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow))
            }
            launch {
                delay(450)
                vibrateCelebrate(context, tier)
                burst.animateTo(1f, tween(1400, easing = LinearOutSlowInEasing))
            }
            launch {
                delay(900)
                for (i in 1..name.length) { typed = i; delay(110) }
            }
        }
    }

    val infinite = rememberInfiniteTransition(label = "stage")
    val spin by infinite.animateFloat(0f, 360f, infiniteRepeatable(tween(16000, easing = LinearEasing), RepeatMode.Restart), label = "rays")
    val glow by infinite.animateFloat(0.55f, 1f, infiniteRepeatable(tween(1100, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "glow")
    val twinkle by infinite.animateFloat(0f, 1f, infiniteRepeatable(tween(1500, easing = LinearEasing), RepeatMode.Restart), label = "twinkle")

    val confettiColors = listOf(c.brass, c.stamp, c.primary, Color.White, c.tealSoft, c.brassSoft)

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(
            Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(c.sign, Color(0xFF050B0A)))),
            contentAlignment = Alignment.TopCenter,
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2, size.height * 0.34f)
                drawRays(center, spin, c.brass, enter.value)
                drawGlow(center, c.brass, glow * enter.value)
                drawFireworks(center, burst.value, tier, confettiColors)
                drawConfetti(confetti.value, 36 + tier * 7, confettiColors)
                if (enter.value >= 1f) drawTwinkles(center, twinkle, c.brassSoft)
            }
            Column(
                Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(NopoTheme.spacing.xl),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(Modifier.fillMaxWidth().weight(0.68f), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(NopoTheme.spacing.lg)) {
                        Text(
                            stringResource(R.string.title_up_headline),
                            style = NopoTheme.typography.titleLarge, color = Color.White, textAlign = TextAlign.Center,
                            modifier = Modifier.graphicsLayer { alpha = enter.value.coerceIn(0f, 1f) },
                        )
                        TitleBadge(
                            title,
                            Modifier.graphicsLayer {
                                val s = enter.value * 2.4f
                                scaleX = s; scaleY = s
                                rotationY = flip.value
                                cameraDistance = 14f * density
                            }.padding(NopoTheme.spacing.lg),
                        )
                        Text(
                            name.take(typed),
                            style = NopoTheme.typography.displaySmall, color = c.brass, textAlign = TextAlign.Center,
                            modifier = Modifier.graphicsLayer { alpha = if (typed > 0) 1f else 0f },
                        )
                        if (previousName != null) {
                            Text(
                                stringResource(R.string.title_up_from_to, previousName, name),
                                style = NopoTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.8f), textAlign = TextAlign.Center,
                                modifier = Modifier.graphicsLayer { alpha = if (typed >= name.length) 1f else 0f },
                            )
                        }
                        if (preview) Text(stringResource(R.string.title_up_preview), style = NopoTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.7f))
                    }
                }
                Column(
                    Modifier.fillMaxWidth().background(c.surface, NopoTheme.shapes.card).padding(NopoTheme.spacing.lg),
                    verticalArrangement = Arrangement.spacedBy(NopoTheme.spacing.md),
                ) {
                    YearsGauge(totalYears = totalYears, yearsToNextTitle = TitlePolicy.yearsToNext(totalYears))
                    NopoButton(stringResource(R.string.title_up_confirm), onDismiss, modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
}

/** 배지 뒤에서 천천히 도는 빛줄기 */
private fun DrawScope.drawRays(center: Offset, spin: Float, color: Color, strength: Float) {
    if (strength <= 0f) return
    val len = size.maxDimension
    val rays = 18
    rotate(spin, center) {
        for (i in 0 until rays) {
            val a = (i * 2 * PI / rays).toFloat()
            val half = (PI / rays * 0.55f).toFloat()
            val path = androidx.compose.ui.graphics.Path().apply {
                moveTo(center.x, center.y)
                lineTo(center.x + cos(a - half) * len, center.y + sin(a - half) * len)
                lineTo(center.x + cos(a + half) * len, center.y + sin(a + half) * len)
                close()
            }
            drawPath(path, Brush.radialGradient(listOf(color.copy(alpha = 0.2f * strength), Color.Transparent), center, len * 0.7f))
        }
    }
}

/** 배지 뒤의 숨 쉬는 빛무리 */
private fun DrawScope.drawGlow(center: Offset, color: Color, strength: Float) {
    if (strength <= 0f) return
    val r = 150.dp.toPx()
    drawCircle(Brush.radialGradient(listOf(color.copy(alpha = 0.55f * strength), Color.Transparent), center, r * 1.6f), r * 1.6f, center)
}

/** 배지 주변에서 반짝이는 별 */
private fun DrawScope.drawTwinkles(center: Offset, phase: Float, color: Color) {
    val n = 14
    for (i in 0 until n) {
        val ang = (i * 2 * PI / n).toFloat() + 0.3f
        val dist = (118 + (i % 3) * 26).dp.toPx()
        val p = (phase + i / n.toFloat()) % 1f
        val s = sin(p * PI).toFloat().coerceIn(0f, 1f)
        if (s <= 0.03f) continue
        star(Offset(center.x + cos(ang) * dist, center.y + sin(ang) * dist), (4f + (i % 3) * 2f).dp.toPx() * s, color.copy(alpha = s))
    }
}

/** 축하 진동: 높은 칭호일수록 길고 여러 번 */
@Suppress("DEPRECATION")
private fun vibrateCelebrate(context: android.content.Context, tier: Int) {
    runCatching {
        val vibrator = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            (context.getSystemService(android.content.Context.VIBRATOR_MANAGER_SERVICE) as android.os.VibratorManager).defaultVibrator
        } else {
            context.getSystemService(android.content.Context.VIBRATOR_SERVICE) as android.os.Vibrator
        }
        val pulses = 2 + tier / 3
        val pattern = LongArray(pulses * 2) { if (it % 2 == 0) 70L else 90L }
        pattern[0] = 0L
        vibrator.vibrate(android.os.VibrationEffect.createWaveform(pattern, -1))
    }
}

@com.nopo.cheonnyeon.core.designsystem.component.NopoPreviews
@Composable
private fun TitleUpDialogPreview() {
    com.nopo.cheonnyeon.core.designsystem.component.PreviewSurface { TitleUpDialog(Title.ELDER, totalYears = 312) {} }
}
