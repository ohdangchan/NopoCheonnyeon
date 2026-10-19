package com.nopo.cheonnyeon.core.designsystem.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import com.nopo.cheonnyeon.R
import com.nopo.cheonnyeon.core.designsystem.theme.NopoTheme
import com.nopo.cheonnyeon.domain.model.Title

private const val MILLENNIUM = 1_000

/**
 * 누적 세월 → 천 년 게이지 (FR-PAS-01). 칭호 경계(100/300/600/1,000)에 눈금을 찍는다.
 * [yearsToNextTitle]은 TitlePolicy가 계산해 넘긴다. null이면 최고 칭호.
 */
@Composable
fun YearsGauge(
    totalYears: Int,
    yearsToNextTitle: Int?,
    modifier: Modifier = Modifier,
) {
    val c = NopoTheme.colors
    val fraction = (totalYears.toFloat() / MILLENNIUM).coerceIn(0f, 1f)
    val percent = (fraction * 100).toInt()
    val description = stringResource(R.string.cd_years_gauge, totalYears, percent)

    Column(
        modifier = modifier.clearAndSetSemantics { contentDescription = description },
        verticalArrangement = Arrangement.spacedBy(NopoTheme.spacing.sm),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
            Text(
                text = stringResource(R.string.years_gauge_total, totalYears),
                style = NopoTheme.extraTypography.metric,
                color = c.ink,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = if (yearsToNextTitle == null) {
                    stringResource(R.string.years_gauge_max)
                } else {
                    stringResource(R.string.years_gauge_next, yearsToNextTitle)
                },
                style = NopoTheme.typography.labelMedium,
                color = c.stamp,
            )
        }
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(12.dp),
        ) {
            val radius = CornerRadius(size.height / 2)
            drawRoundRect(c.line, cornerRadius = radius)
            if (fraction > 0f) {
                drawRoundRect(
                    brush = Brush.horizontalGradient(listOf(c.primary, c.brass)),
                    size = Size(size.width * fraction, size.height),
                    cornerRadius = radius,
                )
            }
            Title.entries.drop(1).dropLast(1).forEach { t ->
                val x = size.width * t.requiredYears / MILLENNIUM
                drawLine(
                    color = c.surface,
                    start = Offset(x, 0f),
                    end = Offset(x, size.height),
                    strokeWidth = 1.5.dp.toPx(),
                )
            }
        }
    }
}

@NopoPreviews
@Composable
private fun YearsGaugePreview() = PreviewSurface {
    YearsGauge(totalYears = 0, yearsToNextTitle = 100)
    YearsGauge(totalYears = 442, yearsToNextTitle = 158)
    YearsGauge(totalYears = 1_120, yearsToNextTitle = null)
}
