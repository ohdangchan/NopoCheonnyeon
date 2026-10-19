package com.nopo.cheonnyeon.core.designsystem.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nopo.cheonnyeon.R
import com.nopo.cheonnyeon.core.designsystem.theme.NopoTheme
import kotlin.math.cos
import kotlin.math.sin

private const val MAX_STARS = 5

/** 평점 표시용 (소수점은 별 일부만 채운다). */
@Composable
fun StarRating(
    rating: Float,
    modifier: Modifier = Modifier,
    starSize: Dp = 16.dp,
) {
    val c = NopoTheme.colors
    val description = stringResource(R.string.cd_star_rating, "%.1f".format(rating))
    Row(modifier.clearAndSetSemantics { contentDescription = description }) {
        repeat(MAX_STARS) { i ->
            val fill = (rating - i).coerceIn(0f, 1f)
            Canvas(Modifier.size(starSize)) { drawStar(fill, c.brass, c.line) }
        }
    }
}

/** 평점 입력용 (1~5 필수, FR-REV). 별 하나가 48dp 터치 영역. */
@Composable
fun StarRatingInput(
    rating: Int,
    onRatingChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val c = NopoTheme.colors
    Row(modifier.selectableGroup()) {
        for (value in 1..MAX_STARS) {
            val description = stringResource(R.string.cd_star_input, value)
            Box(
                modifier = Modifier
                    .size(NopoTheme.spacing.minTouch)
                    .selectable(
                        selected = rating == value,
                        enabled = enabled,
                        role = Role.RadioButton,
                        onClick = { onRatingChange(value) },
                    )
                    .semantics { contentDescription = description },
                contentAlignment = Alignment.Center,
            ) {
                Canvas(Modifier.size(32.dp)) {
                    drawStar(if (value <= rating) 1f else 0f, c.brass, c.ink2)
                }
            }
        }
    }
}

private fun DrawScope.drawStar(fill: Float, fillColor: Color, emptyColor: Color) {
    val path = starPath(size.minDimension / 2, center)
    if (fill < 1f) drawPath(path, emptyColor, style = Stroke(width = size.minDimension / 14))
    if (fill > 0f) {
        clipRect(right = size.width * fill) { drawPath(path, fillColor) }
    }
}

private fun starPath(outer: Float, center: Offset): Path {
    val inner = outer * 0.45f
    return Path().apply {
        for (i in 0 until 10) {
            val r = if (i % 2 == 0) outer * 0.95f else inner
            val angle = Math.toRadians(-90.0 + i * 36.0)
            val x = center.x + r * cos(angle).toFloat()
            val y = center.y + r * sin(angle).toFloat()
            if (i == 0) moveTo(x, y) else lineTo(x, y)
        }
        close()
    }
}

@NopoPreviews
@Composable
private fun StarRatingPreview() = PreviewSurface {
    StarRating(4.7f)
    StarRating(2.5f, starSize = 24.dp)
    StarRatingInput(rating = 3, onRatingChange = {})
}
