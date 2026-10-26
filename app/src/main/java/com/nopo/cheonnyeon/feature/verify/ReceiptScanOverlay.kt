package com.nopo.cheonnyeon.feature.verify

import android.graphics.Bitmap
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.nopo.cheonnyeon.R
import com.nopo.cheonnyeon.core.designsystem.mock.MockColors
import com.nopo.cheonnyeon.core.designsystem.theme.NopoTheme
import com.nopo.cheonnyeon.platform.ocr.OcrBox

/** 스캔선·모서리 색: 앱의 연두색(방문 인증 버튼과 같은 색) */
private val LimeScan = MockColors.Lime2

/**
 * 영수증 스캔 연출 (Stitch 시안 receipt_scan 재현): 찍은 영수증 위를 빨간 레이저 선이 위아래로 훑고,
 * 글자 인식이 끝나면 인식한 글자 줄마다 형광펜이 차례로 칠해진다. 장면은 메모리에서만 쓰고 저장하지 않는다.
 */
@Composable
fun ReceiptScanView(frame: Bitmap, boxes: List<OcrBox>, phase: ScanPhase, modifier: Modifier = Modifier) {
    val bitmap = remember(frame) { frame.asImageBitmap() }
    Box(modifier.background(Color.Black), contentAlignment = Alignment.Center) {
        Image(bitmap, stringResource(R.string.cd_receipt_scan), Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
        Canvas(Modifier.fillMaxSize()) {
            // 이미지가 Fit으로 놓인 영역
            val scale = minOf(size.width / frame.width, size.height / frame.height)
            val w = frame.width * scale
            val h = frame.height * scale
            val left = (size.width - w) / 2
            val top = (size.height - h) / 2
            // 어둡게 깔아 스캔 느낌을 낸다
            drawRect(Color.Black.copy(alpha = 0.25f), Offset(left, top), Size(w, h))
            // 영수증 가장자리 모서리 표시
            val len = 28.dp.toPx()
            val stroke = 3.dp.toPx()
            val corners = listOf(
                Offset(left, top) to (1f to 1f),
                Offset(left + w, top) to (-1f to 1f),
                Offset(left, top + h) to (1f to -1f),
                Offset(left + w, top + h) to (-1f to -1f),
            )
            corners.forEach { (p, d) ->
                drawLine(LimeScan, p, Offset(p.x + d.first * len, p.y), stroke)
                drawLine(LimeScan, p, Offset(p.x, p.y + d.second * len), stroke)
            }
        }
        if (phase == ScanPhase.LASER) LaserSweep(frame.width.toFloat() / frame.height)
        if (phase == ScanPhase.HIGHLIGHT || boxes.isNotEmpty()) Highlights(frame, boxes, animate = phase == ScanPhase.HIGHLIGHT)
    }
}

/** 영수증 위아래를 오가는 빨간 레이저 선과 번지는 빛 */
@Composable
private fun LaserSweep(aspect: Float) {
    val t by rememberInfiniteTransition(label = "laser").animateFloat(
        0f, 1f, infiniteRepeatable(tween(950, easing = LinearEasing), RepeatMode.Reverse), label = "y",
    )
    Canvas(Modifier.fillMaxSize()) {
        // aspect = 가로/세로 → Fit 영역 계산
        val h = minOf(size.height, size.width / aspect)
        val w = h * aspect
        val left = (size.width - w) / 2
        val top = (size.height - h) / 2
        val y = top + h * t
        val glow = 64.dp.toPx()
        // 선 위쪽으로 빛이 번진다
        val brush = Brush.verticalGradient(
            listOf(Color.Transparent, LimeScan.copy(alpha = 0.38f)),
            startY = y - glow, endY = y,
        )
        drawRect(brush, Offset(left, (y - glow).coerceAtLeast(top)), Size(w, (glow.coerceAtMost(y - top))))
        drawLine(LimeScan, Offset(left, y), Offset(left + w, y), strokeWidth = 3.dp.toPx())
        drawLine(Color.White.copy(alpha = 0.8f), Offset(left, y), Offset(left + w, y), strokeWidth = 1.dp.toPx())
        // 양 끝의 작은 불빛
        drawCircle(LimeScan, 5.dp.toPx(), Offset(left, y))
        drawCircle(LimeScan, 5.dp.toPx(), Offset(left + w, y))
    }
}

/** 인식된 글자 줄마다 형광펜이 왼쪽에서 오른쪽으로 칠해진다(위 줄부터 차례로) */
@Composable
private fun Highlights(frame: Bitmap, boxes: List<OcrBox>, animate: Boolean) {
    val c = NopoTheme.colors
    val progress = remember(boxes) { Animatable(if (animate) 0f else 1f) }
    LaunchedEffect(boxes, animate) { if (animate) progress.animateTo(1f, tween(1400, easing = LinearEasing)) }
    val sorted = remember(boxes) { boxes.sortedBy { it.top } }
    Canvas(Modifier.fillMaxSize()) {
        val scale = minOf(size.width / frame.width, size.height / frame.height)
        val w = frame.width * scale
        val h = frame.height * scale
        val left = (size.width - w) / 2
        val top = (size.height - h) / 2
        val n = sorted.size.coerceAtLeast(1)
        sorted.forEachIndexed { i, b ->
            val start = i.toFloat() / n * 0.8f
            val f = ((progress.value - start) / 0.2f).coerceIn(0f, 1f)
            if (f <= 0f) return@forEachIndexed
            val x1 = left + b.left * w
            val x2 = left + b.right * w
            val y1 = top + b.top * h
            val y2 = top + b.bottom * h
            val pad = 2.dp.toPx()
            drawRoundRect(
                c.highlighter.copy(alpha = 0.5f),
                Offset(x1 - pad, y1 - pad), Size((x2 - x1 + pad * 2) * f, (y2 - y1) + pad * 2),
                CornerRadius(3.dp.toPx()),
            )
        }
    }
}
