package com.nopo.cheonnyeon.core.designsystem.mock

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nopo.cheonnyeon.R
import kotlin.math.cos
import kotlin.math.sin

/**
 * 디자인 시안(design/redesign) 복제용 도구 모음.
 * 시안의 폰 화면은 가로 760px 이다. 모든 크기·위치는 시안 픽셀(px)로 적고, 실제 화면 폭에 맞춰 dp 로 바꾼다.
 */
val LocalMockU = compositionLocalOf { 0.47f }

/** 기준 y 좌표: [MockStage] 안에서 Modifier.at(x, y) 의 y 에서 이 값을 뺀다 */
val LocalMockOriginY = compositionLocalOf { 0 }

@Composable
fun ProvideMockScale(content: @Composable () -> Unit) {
    val w = LocalConfiguration.current.screenWidthDp
    CompositionLocalProvider(LocalMockU provides (w / 760f), content = content)
}

@Composable
@ReadOnlyComposable
fun px(n: Int): Dp = (n * LocalMockU.current).dp

@Composable
@ReadOnlyComposable
fun px(n: Float): Dp = (n * LocalMockU.current).dp

@Composable
@ReadOnlyComposable
fun pxSp(n: Int): TextUnit = (n * LocalMockU.current).sp

/** 시안에서 잰 색 */
object MockColors {
    val Cream = Color(0xFFFCF7EB)
    val Card = Color(0xFFFAF7EC)
    val CardLine = Color(0xFFEFE9D8)
    val Navy = Color(0xFF061A31)
    val Ink2 = Color(0xFF4B5563)
    val Hint = Color(0xFF6B7280)
    val Lime = Color(0xFFBDF642)
    val LimeDeep = Color(0xFFA1E41E)
    val Chip = Color(0xFFEEECE1)
    val Line = Color(0xFFE6E0CC)
    val Star = Color(0xFFF5B800)
    val ProgressCard = Color(0xFFF0F1DB)
    val Stamp = Color(0xFFB4332A)
}

val MockSerif = FontFamily(Font(R.font.nanum_myeongjo_extrabold, FontWeight.ExtraBold), Font(R.font.nanum_myeongjo_extrabold, FontWeight.Bold))
val MockSans = FontFamily.Default

/** [MockStage] 안에서 시안 좌표(x, y)에 놓는다. */
@Composable
fun Modifier.at(x: Int, y: Int): Modifier = this.offset(px(x), px(y - LocalMockOriginY.current))

/** 시안 좌표계 무대. 높이는 시안 px 로 준다. */
@Composable
fun MockStage(originY: Int, heightPx: Int, modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    CompositionLocalProvider(LocalMockOriginY provides originY) {
        Box(modifier.fillMaxWidth().height(px(heightPx)), content = content)
    }
}

/** 시안에서 잘라 낸 그림. 원래 크기(w x h px)로 그린다. */
@Composable
fun MockPic(@DrawableRes id: Int, w: Int, h: Int, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, description: String? = null) {
    androidx.compose.foundation.Image(
        painter = painterResource(id),
        contentDescription = description,
        contentScale = ContentScale.FillBounds,
        modifier = modifier.size(px(w), px(h)).then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier),
    )
}

/** 시안 글자. 크기는 시안 px(글자 높이 기준에 가까운 값). */
@Composable
fun MockText(
    text: String,
    sizePx: Int,
    modifier: Modifier = Modifier,
    color: Color = MockColors.Navy,
    weight: FontWeight = FontWeight.Normal,
    font: FontFamily = MockSans,
    maxLines: Int = 1,
    align: TextAlign = TextAlign.Start,
    lineHeightPx: Int? = null,
) {
    Text(
        text,
        modifier = modifier,
        color = color,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        textAlign = align,
        style = TextStyle(
            fontFamily = font, fontWeight = weight, fontSize = pxSp(sizePx),
            lineHeight = if (lineHeightPx != null) pxSp(lineHeightPx) else TextUnit.Unspecified,
        ),
    )
}

/** 연두 알약 (태그·선택된 칩) */
@Composable
fun LimePill(text: String, sizePx: Int, modifier: Modifier = Modifier, padX: Int = 24, padY: Int = 8, bold: Boolean = true, onClick: (() -> Unit)? = null) {
    Box(
        modifier.clip(RoundedCornerShape(50)).background(MockColors.Lime)
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
            .padding(horizontal = px(padX), vertical = px(padY)),
        contentAlignment = Alignment.Center,
    ) {
        MockText(text, sizePx, weight = if (bold) FontWeight.Bold else FontWeight.Medium)
    }
}

/** 회색 알약 칩 (카테고리 칩) */
@Composable
fun GrayPill(text: String, sizePx: Int, modifier: Modifier = Modifier, padX: Int = 22, padY: Int = 8, color: Color = MockColors.Chip, textColor: Color = MockColors.Ink2, onClick: (() -> Unit)? = null) {
    Box(
        modifier.clip(RoundedCornerShape(50)).background(color)
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
            .padding(horizontal = px(padX), vertical = px(padY)),
        contentAlignment = Alignment.Center,
    ) {
        MockText(text, sizePx, color = textColor, weight = FontWeight.Medium)
    }
}

// ---------------------------------------------------------------------------------------------
// 직접 그리는 아이콘 (사진 위에 얹히거나 색이 바뀌는 것)

@Composable
fun StarIcon(sizePx: Int, modifier: Modifier = Modifier, color: Color = MockColors.Star) {
    Canvas(modifier.size(px(sizePx))) {
        val r = size.minDimension / 2f
        val c = Offset(size.width / 2f, size.height / 2f + r * 0.06f)
        val path = Path()
        for (i in 0 until 10) {
            val rad = if (i % 2 == 0) r else r * 0.45f
            val a = -Math.PI / 2 + i * Math.PI / 5
            val p = Offset(c.x + (rad * cos(a)).toFloat(), c.y + (rad * sin(a)).toFloat())
            if (i == 0) path.moveTo(p.x, p.y) else path.lineTo(p.x, p.y)
        }
        path.close()
        drawPath(path, color)
        drawPath(path, Color(0xFF8A6A00), style = Stroke(width = size.minDimension * 0.07f, join = StrokeJoin.Round))
    }
}

@Composable
fun PinIcon(sizePx: Int, modifier: Modifier = Modifier, color: Color = MockColors.Navy) {
    Canvas(modifier.size(px(sizePx))) {
        val w = size.width
        val h = size.height
        val path = Path().apply {
            moveTo(w * 0.5f, h * 0.97f)
            cubicTo(w * 0.12f, h * 0.55f, w * 0.14f, h * 0.30f, w * 0.14f, h * 0.30f)
            cubicTo(w * 0.14f, h * 0.10f, w * 0.30f, h * 0.0f, w * 0.5f, h * 0.0f)
            cubicTo(w * 0.70f, h * 0.0f, w * 0.86f, h * 0.10f, w * 0.86f, h * 0.30f)
            cubicTo(w * 0.86f, h * 0.30f, w * 0.88f, h * 0.55f, w * 0.5f, h * 0.97f)
            close()
        }
        drawPath(path, color)
        drawCircle(Color(0xFFFCF7EB), radius = w * 0.17f, center = Offset(w * 0.5f, h * 0.32f))
    }
}

/** 북마크. [filled]가 true 면 속이 채워진다. 사진 위에서는 흰색으로 쓴다. */
@Composable
fun BookmarkIcon(sizePx: Int, modifier: Modifier = Modifier, color: Color = Color.White, filled: Boolean = false) {
    Canvas(modifier.size(px(sizePx))) {
        val w = size.width
        val h = size.height
        val path = Path().apply {
            moveTo(w * 0.22f, h * 0.08f)
            lineTo(w * 0.78f, h * 0.08f)
            lineTo(w * 0.78f, h * 0.92f)
            lineTo(w * 0.5f, h * 0.68f)
            lineTo(w * 0.22f, h * 0.92f)
            close()
        }
        if (filled) drawPath(path, color) else drawPath(path, color, style = Stroke(width = w * 0.09f, join = StrokeJoin.Round, cap = StrokeCap.Round))
    }
}

@Composable
fun HeartIcon(sizePx: Int, modifier: Modifier = Modifier, color: Color = Color.White, filled: Boolean = false) {
    Canvas(modifier.size(px(sizePx))) {
        val w = size.width
        val h = size.height
        val path = Path().apply {
            moveTo(w * 0.5f, h * 0.88f)
            cubicTo(w * 0.05f, h * 0.55f, w * 0.08f, h * 0.12f, w * 0.33f, h * 0.12f)
            cubicTo(w * 0.43f, h * 0.12f, w * 0.5f, h * 0.2f, w * 0.5f, h * 0.27f)
            cubicTo(w * 0.5f, h * 0.2f, w * 0.57f, h * 0.12f, w * 0.67f, h * 0.12f)
            cubicTo(w * 0.92f, h * 0.12f, w * 0.95f, h * 0.55f, w * 0.5f, h * 0.88f)
            close()
        }
        if (filled) drawPath(path, color) else drawPath(path, color, style = Stroke(width = w * 0.09f, join = StrokeJoin.Round))
    }
}

/** 탐색 검색창 오른쪽의 AI 아이콘: 반짝이는 별 두 개 */
@Composable
fun AiSparkIcon(sizePx: Int, modifier: Modifier = Modifier, color: Color = MockColors.Navy) {
    Canvas(modifier.size(px(sizePx))) {
        fun spark(cx: Float, cy: Float, r: Float) {
            val p = Path().apply {
                moveTo(cx, cy - r)
                quadraticBezierTo(cx, cy, cx + r, cy)
                quadraticBezierTo(cx, cy, cx, cy + r)
                quadraticBezierTo(cx, cy, cx - r, cy)
                quadraticBezierTo(cx, cy, cx, cy - r)
                close()
            }
            drawPath(p, color, style = Stroke(width = size.width * 0.075f, join = StrokeJoin.Round))
        }
        spark(size.width * 0.42f, size.height * 0.58f, size.width * 0.36f)
        spark(size.width * 0.78f, size.height * 0.22f, size.width * 0.17f)
    }
}

@Composable
fun ChevronRight(sizePx: Int, modifier: Modifier = Modifier, color: Color = MockColors.Navy) {
    Canvas(modifier.size(px(sizePx))) {
        val w = size.width
        val h = size.height
        val p = Path().apply { moveTo(w * 0.35f, h * 0.2f); lineTo(w * 0.68f, h * 0.5f); lineTo(w * 0.35f, h * 0.8f) }
        drawPath(p, color, style = Stroke(width = w * 0.12f, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

@Composable
fun ChevronDown(sizePx: Int, modifier: Modifier = Modifier, color: Color = MockColors.Navy) {
    Canvas(modifier.size(px(sizePx))) {
        val w = size.width
        val h = size.height
        val p = Path().apply { moveTo(w * 0.2f, h * 0.35f); lineTo(w * 0.5f, h * 0.68f); lineTo(w * 0.8f, h * 0.35f) }
        drawPath(p, color, style = Stroke(width = w * 0.12f, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

/** 둥근 연두 버튼 안의 > 표시 */
@Composable
fun LimeArrowButton(sizePx: Int, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
    Box(
        modifier.size(px(sizePx)).clip(CircleShape).background(MockColors.Lime)
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center,
    ) { ChevronRight((sizePx * 0.5f).toInt()) }
}

/** 사진 위에 얹는 반투명 어두운 동그라미 버튼(하트·북마크) */
@Composable
fun PhotoCircleButton(sizePx: Int, modifier: Modifier = Modifier, onClick: () -> Unit, content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier.size(px(sizePx)).clip(CircleShape).background(Color(0x59000000)).clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
        content = content,
    )
}

/** 별 + 평점 한 줄. 평점이 없으면 아무것도 그리지 않는다. */
@Composable
fun RatingText(rating: Double?, count: Int?, sizePx: Int, modifier: Modifier = Modifier, showCount: Boolean = false) {
    if (rating == null) return
    androidx.compose.foundation.layout.Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        StarIcon(sizePx + 4)
        androidx.compose.foundation.layout.Spacer(Modifier.width(px(8)))
        MockText(String.format("%.1f", rating), sizePx, weight = FontWeight.Medium)
        if (showCount && count != null) {
            androidx.compose.foundation.layout.Spacer(Modifier.width(px(6)))
            MockText("(${"%,d".format(count)})", sizePx - 2, color = MockColors.Hint)
        }
    }
}

/** 둥근 모서리 + 연한 테두리 카드 */
@Composable
fun Modifier.mockCardBox(radiusPx: Int = 18, fill: Color = MockColors.Card): Modifier {
    val shape = RoundedCornerShape(px(radiusPx))
    return this.clip(shape).background(fill).border(1.dp, MockColors.CardLine, shape)
}

/** 한 줄 가로 구분 여백 */
@Composable
fun VSpacePx(h: Int) = androidx.compose.foundation.layout.Spacer(Modifier.height(px(h)))

@Composable
fun HSpacePx(w: Int) = androidx.compose.foundation.layout.Spacer(Modifier.width(px(w)))

@Composable
fun Modifier.mockPadX(x: Int): Modifier = this.padding(horizontal = px(x))

@Composable
fun Modifier.fullWidth(): Modifier = this.fillMaxWidth()

