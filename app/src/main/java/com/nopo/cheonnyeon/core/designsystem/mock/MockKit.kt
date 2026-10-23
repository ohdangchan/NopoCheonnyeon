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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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
const val MOCK_MAX_WIDTH_DP = 600

val LocalMockU = compositionLocalOf { 0.47f }

/** 기준 y 좌표: [MockStage] 안에서 Modifier.at(x, y) 의 y 에서 이 값을 뺀다 */
val LocalMockOriginY = compositionLocalOf { 0 }

@Composable
fun ProvideMockScale(content: @Composable () -> Unit) {
    // 태블릿·폴더블 펼침처럼 넓은 화면에서는 폭을 MAX_WIDTH_DP 로 묶는다 (MainActivity 가 같은 폭으로 가운데에 놓는다)
    val w = minOf(LocalConfiguration.current.screenWidthDp, MOCK_MAX_WIDTH_DP)
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
fun pxSp(n: Int): TextUnit = with(androidx.compose.ui.platform.LocalDensity.current) { (n * LocalMockU.current).dp.toSp() } // 시안 좌표 레이아웃이 깨지지 않게 시스템 글자 크기 배율과 무관한 고정 크기

/** 시안에서 잰 색 */
object MockColors {
    val Cream = Color(0xFFFCF7EB)
    val Card = Color(0xFFFAF7EC)
    val CardLine = Color(0xFFEFE9D8)
    val Navy = Color(0xFF061A31)
    val Ink2 = Color(0xFF4B5563)
    val Hint = Color(0xFF6B7280)
    val Lime = Color(0xFFBDF642)
    /** 2차 시안(로그인·인증·룰렛)의 연두: 조금 더 노랗다 */
    val Lime2 = Color(0xFFD6F44A)
    val LimeDeep = Color(0xFFA1E41E)
    val Chip = Color(0xFFEEECE1)
    val Line = Color(0xFFE6E0CC)
    val Star = Color(0xFFF5B800)
    val ProgressCard = Color(0xFFF0F1DB)
    val Stamp = Color(0xFFB4332A)
}

val MockSerif = FontFamily(Font(R.font.nanum_myeongjo_extrabold, FontWeight.ExtraBold), Font(R.font.nanum_myeongjo_extrabold, FontWeight.Bold))
val MockSans = FontFamily.Default

/** 로고 글자와 비슷한 굵은 디스플레이 글자체(제목·닉네임용) */
val MockBrush = FontFamily(Font(R.font.black_han_sans, FontWeight.ExtraBold), Font(R.font.black_han_sans, FontWeight.Bold), Font(R.font.black_han_sans, FontWeight.Normal))

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
    // 누를 수 있는 그림은 TalkBack 이 읽을 이름이 필요하다: 그림 파일 이름으로 정한다
    val label = description ?: if (onClick != null) {
        val name = androidx.compose.ui.platform.LocalContext.current.resources.getResourceEntryName(id)
        when {
            name.contains("bell") -> "알림"
            name.contains("ic_search") -> "검색"
            name.contains("menu_all") -> "전체 메뉴 보기"
            name.contains("my_edit") -> "프로필 편집"
            name.contains("_all") -> "전체 보기"
            name.contains("more") -> "더보기"
            name.contains("card_saved") -> "저장한 노포"
            name.contains("card_history") -> "방문 기록"
            name.contains("card_alarm") -> "알림 설정"
            name.contains("card_prefs") -> "앱 환경 설정"
            name.contains("card_account") -> "계정 관리"
            name.contains("card_support") -> "고객센터"
            else -> null
        }
    } else null
    androidx.compose.foundation.Image(
        painter = painterResource(id),
        contentDescription = label,
        contentScale = ContentScale.FillBounds,
        modifier = modifier.size(px(w), px(h)).then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier),
    )
}

/**
 * 글자 크기 단계표(시안 px). 화면마다 제각각이던 크기를 이 단계로 모은다:
 * 캡션 18 · 작은 글 20 · 본문 22 · 본문 큰 24 · 소제목 26 · 강조 28 · 제목 30 · 큰 제목 34 · 표제 40.
 * 40을 넘는 큰 숫자·제목은 그대로 둔다.
 */
fun snapTextPx(n: Int): Int = when {
    n <= 18 -> 18
    n <= 20 -> 20
    n <= 22 -> 22
    n <= 24 -> 24
    n <= 26 -> 26
    n <= 28 -> 28
    n <= 31 -> 30
    n <= 35 -> 34
    n <= 45 -> 40
    else -> n
}

/** 시안 글자. 크기는 시안 px 로 적으면 [snapTextPx] 단계로 맞춰 그린다. */
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
    val size = snapTextPx(sizePx)
    val line = if (lineHeightPx != null) maxOf(lineHeightPx, (size * 1.3f).toInt()) else null
    Text(
        text,
        modifier = modifier,
        color = color,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        textAlign = align,
        style = TextStyle(
            fontFamily = font, fontWeight = weight, fontSize = pxSp(size),
            lineHeight = if (line != null) pxSp(line) else TextUnit.Unspecified,
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


/** 북마크(저장하기) 버튼에 TalkBack 이 읽을 이름을 붙인다 */
@Composable
fun Modifier.saveDescription(): Modifier {
    val label = androidx.compose.ui.res.stringResource(R.string.det_save)
    return this.semantics { contentDescription = label }
}

/** 시안 입력칸 위에 얹는 글자 입력(안내 글자 포함). 배경은 시안 그림이 그린다. */
@Composable
fun MockInput(
    value: String,
    onChange: (String) -> Unit,
    hint: String,
    modifier: Modifier = Modifier,
    sizePx: Int = 24,
    password: Boolean = false,
    visible: Boolean = false,
    keyboard: androidx.compose.ui.text.input.KeyboardType = androidx.compose.ui.text.input.KeyboardType.Text,
    ime: androidx.compose.ui.text.input.ImeAction = androidx.compose.ui.text.input.ImeAction.Next,
    onDone: (() -> Unit)? = null,
) {
    Box(modifier, contentAlignment = Alignment.CenterStart) {
        if (value.isEmpty()) MockText(hint, sizePx, color = Color(0xFF9AA0AB))
        androidx.compose.foundation.text.BasicTextField(
            value = value, onValueChange = onChange, singleLine = true,
            textStyle = TextStyle(fontFamily = MockSans, fontSize = pxSp(sizePx), color = MockColors.Navy),
            cursorBrush = androidx.compose.ui.graphics.SolidColor(MockColors.Navy),
            visualTransformation = if (password && !visible) androidx.compose.ui.text.input.PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = if (password) androidx.compose.ui.text.input.KeyboardType.Password else keyboard, imeAction = ime),
            keyboardActions = androidx.compose.foundation.text.KeyboardActions(onDone = { onDone?.invoke() }),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** 시안의 둥근 네모 체크칸: 켜지면 연두색에 검은 체크 */
@Composable
fun MockCheck(checked: Boolean, sizePx: Int, modifier: Modifier = Modifier, onChange: (Boolean) -> Unit) {
    val shape = RoundedCornerShape(px((sizePx * 0.25f).toInt()))
    Box(
        modifier.size(px(sizePx)).clip(shape)
            .then(if (checked) Modifier.background(MockColors.Lime2) else Modifier.border(1.5.dp, Color(0xFFB8B3A3), shape))
            .clickable(role = Role.Checkbox) { onChange(!checked) },
        contentAlignment = Alignment.Center,
    ) {
        if (checked) Canvas(Modifier.size(px((sizePx * 0.6f).toInt()))) {
            val p = Path().apply { moveTo(size.width * 0.1f, size.height * 0.55f); lineTo(size.width * 0.4f, size.height * 0.85f); lineTo(size.width * 0.92f, size.height * 0.18f) }
            drawPath(p, MockColors.Navy, style = Stroke(width = size.width * 0.16f, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
    }
}

/** 연두색 긴 버튼(글자 가운데, 오른쪽에 > 표시) */
@Composable
fun MockLimeBar(text: String, heightPx: Int, modifier: Modifier = Modifier, textPx: Int = 28, enabled: Boolean = true, arrowCircle: Boolean = false, onClick: () -> Unit) {
    Box(
        modifier.height(px(heightPx)).clip(RoundedCornerShape(50)).background(if (enabled) MockColors.Lime2 else Color(0xFFE2E8C4))
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        MockText(text, textPx, weight = FontWeight.ExtraBold, color = if (enabled) MockColors.Navy else Color(0xFF8A9078))
        Box(Modifier.align(Alignment.CenterEnd).padding(end = px(if (arrowCircle) 18 else 34))) {
            if (arrowCircle) Box(Modifier.size(px(52)).clip(CircleShape).background(Color(0xCCFFFFFF)), contentAlignment = Alignment.Center) { ChevronRight(28) }
            else ChevronRight(30)
        }
    }
}
