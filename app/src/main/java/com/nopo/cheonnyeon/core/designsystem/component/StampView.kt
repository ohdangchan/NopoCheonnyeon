package com.nopo.cheonnyeon.core.designsystem.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nopo.cheonnyeon.R
import com.nopo.cheonnyeon.core.designsystem.theme.BlackHanSans
import com.nopo.cheonnyeon.core.designsystem.theme.NopoTheme
import com.nopo.cheonnyeon.domain.model.StampGrade

/** DESIGN.md 3.1 크기 배리언트 */
enum class StampSize(val dp: Dp) {
    /** 지도 핀·리스트 축약형: 숫자만 */
    Mini(48.dp),

    /** 패스포트 3열 격자 */
    Grid(80.dp),

    /** 도장 연출 */
    Ceremony(160.dp),
}

/**
 * 인주 원형 도장. 선 굵기는 Grid(80dp) 기준 값을 크기에 비례해 키운다.
 * - NORMAL: 단일 외곽선 + 보조선, "세월"
 * - RED: 파선 외곽선 + 보조선, "각인"
 * - GOLD: 3중 원(내부 점선), "百年" + "백년 노포"
 * 회전·낙하 연출은 호출하는 쪽(StampCeremonyScreen)에서 graphicsLayer로 건다.
 */
@Composable
fun StampView(
    years: Int,
    grade: StampGrade,
    modifier: Modifier = Modifier,
    size: StampSize = StampSize.Grid,
) {
    val c = NopoTheme.colors
    val ink = when (grade) {
        StampGrade.NORMAL -> c.primary
        StampGrade.RED -> c.stamp
        StampGrade.GOLD -> c.brass
    }
    val gradeName = stringResource(
        when (grade) {
            StampGrade.NORMAL -> R.string.cd_stamp_grade_normal
            StampGrade.RED -> R.string.cd_stamp_grade_red
            StampGrade.GOLD -> R.string.cd_stamp_grade_gold
        },
    )
    val description = stringResource(R.string.cd_stamp, gradeName, years)
    val scale = size.dp.value / StampSize.Grid.dp.value

    Box(
        modifier = modifier
            .size(size.dp)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) { drawStampRings(grade, ink, scale) }
        if (size == StampSize.Mini) {
            StampText(years.toString(), ink, 16f, BlackHanSans)
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                val label = when (grade) {
                    StampGrade.NORMAL -> R.string.stamp_label_normal
                    StampGrade.RED -> R.string.stamp_label_red
                    StampGrade.GOLD -> R.string.stamp_label_gold
                }
                StampText(stringResource(label), ink, 10f * scale)
                StampText(stringResource(R.string.years_value, years), ink, 17f * scale, BlackHanSans)
                if (grade == StampGrade.GOLD) {
                    StampText(stringResource(R.string.stamp_gold_caption), ink, 9f * scale)
                }
            }
        }
    }
}

/**
 * 도장 안 글자는 원 크기에 묶여 있어 시스템 글꼴 배율을 적용하지 않는다(dp 고정).
 * 큰 글씨 사용자는 contentDescription과 주변 텍스트로 정보를 얻는다.
 */
@Composable
private fun StampText(text: String, color: Color, sizeDp: Float, fontFamily: FontFamily? = null) {
    val fontSize = with(LocalDensity.current) { sizeDp.dp.toSp() }
    Text(
        text = text,
        color = color,
        maxLines = 1,
        style = TextStyle(
            fontFamily = fontFamily,
            fontWeight = if (fontFamily == null) FontWeight.Bold else null,
            fontSize = fontSize,
            lineHeight = fontSize * 1.1f,
        ),
    )
}

private fun DrawScope.drawStampRings(grade: StampGrade, ink: Color, scale: Float) {
    val outerWidth = when (grade) {
        StampGrade.NORMAL -> 5f
        StampGrade.RED -> 5.5f
        StampGrade.GOLD -> 6f
    } * scale * density
    val thin = 1.8f * scale * density
    val outerR = size.minDimension / 2 - outerWidth / 2
    val innerR = outerR - outerWidth / 2 - 4f * scale * density

    when (grade) {
        StampGrade.NORMAL -> {
            drawCircle(ink, outerR, style = Stroke(outerWidth))
            drawCircle(ink, innerR, style = Stroke(thin))
        }
        StampGrade.RED -> {
            val dash = 7f * scale * density
            drawCircle(ink, outerR, style = Stroke(outerWidth, pathEffect = PathEffect.dashPathEffect(floatArrayOf(dash, dash / 2.5f))))
            drawCircle(ink, innerR, style = Stroke(thin))
        }
        StampGrade.GOLD -> {
            val dot = 1f * scale * density
            drawCircle(ink, outerR, style = Stroke(outerWidth))
            drawCircle(ink, innerR, style = Stroke(thin))
            drawCircle(
                ink,
                innerR - 4f * scale * density,
                style = Stroke(dot, pathEffect = PathEffect.dashPathEffect(floatArrayOf(dot * 2, dot * 3))),
            )
        }
    }
}

@NopoPreviews
@Composable
private fun StampViewPreview() = PreviewSurface {
    Row(horizontalArrangement = Arrangement.spacedBy(NopoTheme.spacing.md), verticalAlignment = Alignment.CenterVertically) {
        StampView(38, StampGrade.NORMAL)
        StampView(85, StampGrade.RED)
        StampView(120, StampGrade.GOLD)
    }
    Row(horizontalArrangement = Arrangement.spacedBy(NopoTheme.spacing.md)) {
        StampView(38, StampGrade.NORMAL, size = StampSize.Mini)
        StampView(85, StampGrade.RED, size = StampSize.Mini)
        StampView(120, StampGrade.GOLD, size = StampSize.Mini)
    }
    StampView(120, StampGrade.GOLD, size = StampSize.Ceremony)
}
