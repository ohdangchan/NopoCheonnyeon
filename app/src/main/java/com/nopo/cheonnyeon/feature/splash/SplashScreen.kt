package com.nopo.cheonnyeon.feature.splash

import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.nopo.cheonnyeon.core.designsystem.mock.MockColors
import com.nopo.cheonnyeon.core.designsystem.mock.MockText
import com.nopo.cheonnyeon.R

/** 시안 27 "로딩 중": 앱을 켜자마자 가장 먼저 뜨는 화면. 그림은 시안 그대로, 돌아가는 고리와 문구는 앱이 그린다. */
@Composable
fun SplashScreen() {
    val t = rememberInfiniteTransition(label = "spin")
    val angle by t.animateFloat(0f, 360f, infiniteRepeatable(tween(1100, easing = LinearEasing), RepeatMode.Restart), label = "angle")
    BoxWithConstraints(Modifier.fillMaxSize().background(Color(0xFFFBF6EB))) {
        Image(painterResource(R.drawable.mk2_n_splash), null, Modifier.fillMaxSize(), contentScale = ContentScale.FillBounds)
        // 고리와 문구는 시안에서 그 자리(그림 높이의 90%·94%)에 둔다
        Box(Modifier.align(Alignment.TopCenter).offset(y = maxHeight * 0.901f - 26.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(52.dp).rotate(angle)) {
                val w = 5.dp.toPx()
                drawArc(
                    Color(0xFFE3ECC0),
                    0f,
                    360f,
                    false,
                    topLeft = Offset(w / 2, w / 2),
                    size = Size(size.width - w, size.height - w),
                    style = Stroke(w)
                )
                drawArc(
                    MockColors.LimeDeep,
                    -90f,
                    110f,
                    false,
                    topLeft = Offset(w / 2, w / 2),
                    size = Size(size.width - w, size.height - w),
                    style = Stroke(w, cap = StrokeCap.Round)
                )
            }
        }
        Box(Modifier.align(Alignment.TopCenter).offset(y = maxHeight * 0.940f - 12.dp)) {
            MockText("맛있는 여정을 준비하고 있어요…", 24, color = Color(0xFF4B5563))
        }
    }
}
