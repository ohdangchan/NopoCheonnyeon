package com.nopo.cheonnyeon.feature.passport

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.nopo.cheonnyeon.R
import com.nopo.cheonnyeon.core.designsystem.mock.MockColors
import com.nopo.cheonnyeon.core.designsystem.mock.MockPic
import com.nopo.cheonnyeon.core.designsystem.mock.MockSerif
import com.nopo.cheonnyeon.core.designsystem.mock.MockText
import com.nopo.cheonnyeon.core.designsystem.mock.px
import com.nopo.cheonnyeon.domain.model.StampGrade
import com.nopo.cheonnyeon.domain.rule.RegionProgress
import com.nopo.cheonnyeon.domain.rule.StampGradePolicy
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.sin

internal sealed interface Slot {
    data class Region(val r: RegionProgress) : Slot
    data class Stamp(val s: PassportStamp) : Slot
    data object Empty : Slot
}

internal data class BookPage(val title: String, val slots: List<Slot>)

/** 첫 장 오른쪽 면의 숫자 */
internal data class BookStats(val visited: Int, val goal: Int, val titles: Int)

private val Parchment = Color(0xFFF3EBDB)
private val PageLine = Color(0xFFDCD1B8)
private val Faint = Color(0xFFC9BFA6)
private val RegionColors = listOf(Color(0xFFB4332A), Color(0xFF1F4E8C), Color(0xFF2E7D32), Color(0xFF8A5A2B), Color(0xFF6B3FA0))

private const val PAGE_W = 314
private const val PAGE_H = 426
private const val SPINE = 432
private const val PAGE_TOP = 78
private const val FIRST_PAGE_SLOTS = 6
private const val PAGE_SLOTS = 9

/** 여권 쪽 구성: 0쪽(첫 펼침의 오른쪽, 지역 스탬프 6칸) → 나머지 지역 스탬프 → 방문한 노포 도장 */
internal fun buildPages(regions: List<RegionProgress>, stamps: List<PassportStamp>): List<BookPage> {
    val region = regions.sortedWith(compareByDescending<RegionProgress> { it.stamped }.thenByDescending { it.visited }).map { Slot.Region(it) as Slot }
    val pages = ArrayList<BookPage>()
    pages += BookPage("", (region.take(FIRST_PAGE_SLOTS) + List(FIRST_PAGE_SLOTS) { Slot.Empty }).take(FIRST_PAGE_SLOTS))
    region.drop(FIRST_PAGE_SLOTS).chunked(PAGE_SLOTS).forEach { pages += BookPage("지역 스탬프", it) }
    val mine = stamps.filter { !it.isTest }.sortedBy { it.visit.firstVisitedAt }.map { Slot.Stamp(it) as Slot }
    if (mine.isEmpty()) pages += BookPage("방문 스탬프", emptyList())
    mine.chunked(PAGE_SLOTS).forEach { pages += BookPage("방문 스탬프", it) }
    return pages
}

internal fun spreadCount(pages: Int) = 1 + (maxOf(0, pages - 1) + 1) / 2

/**
 * 시안 "컬렉션"의 펼친 여권. 왼쪽으로 밀면 오른쪽 장이 책등을 축으로 넘어가며 다음 펼침이 나온다.
 * 첫 펼침은 시안 그림을 그대로 쓰고(왼쪽 면 그림 + 숫자/도장만 직접 그림), 이후 장은 양피지 위에 모은 도장을 찍는다.
 */
@Composable
internal fun PassportBook(stats: BookStats, pages: List<BookPage>, onStamp: (PassportStamp) -> Unit, modifier: Modifier = Modifier) {
    val spreads = spreadCount(pages.size)
    val pos = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val pageWpx = with(LocalDensity.current) { px(PAGE_W).toPx() }
    val density = LocalDensity.current.density
    val maxPos = (spreads - 1).toFloat()

    fun snap(lastDx: Float) {
        val v = pos.value
        val f = v - floor(v)
        val target = if (lastDx < 0f) { if (f > 0.2f) ceil(v) else floor(v) } else { if (f < 0.8f) floor(v) else ceil(v) }
        scope.launch { pos.animateTo(target.coerceIn(0f, maxPos), tween(260)) }
    }

    val v = pos.value
    val k = floor(v).toInt().coerceIn(0, spreads - 1)
    val t = if (k >= spreads - 1) 0f else (v - k).coerceIn(0f, 1f)
    fun page(i: Int): BookPage? = pages.getOrNull(i)

    Box(
        modifier.fillMaxWidth().height(px(560)).pointerInput(spreads) {
            var lastDx = 0f
            detectHorizontalDragGestures(
                onDragStart = { lastDx = 0f; scope.launch { pos.stop() } },
                onDragEnd = { snap(lastDx) },
                onDragCancel = { snap(lastDx) },
                onHorizontalDrag = { change, dx ->
                    change.consume()
                    lastDx = dx
                    scope.launch { pos.snapTo((pos.value - dx / pageWpx).coerceIn(0f, maxPos)) }
                },
            )
        },
    ) {
        MockPic(R.drawable.mk_col_book, 760, 560)
        // 왼쪽 정지면
        if (k >= 1) PageSurface(left = true, Modifier.offset(px(SPINE - PAGE_W), px(PAGE_TOP))) { PageBody(2 * k - 1, page(2 * k - 1), stats, onStamp) }
        // 오른쪽에 드러날 다음 장
        if (t > 0f && k + 1 < spreads) PageSurface(left = false, Modifier.offset(px(SPINE), px(PAGE_TOP))) { PageBody(2 * (k + 1), page(2 * (k + 1)), stats, onStamp) }
        // 넘어가는 장
        if (t < 0.5f) {
            PageSurface(
                left = false,
                Modifier.offset(px(SPINE), px(PAGE_TOP)).graphicsLayer {
                    transformOrigin = TransformOrigin(0f, 0.5f)
                    rotationY = -180f * t
                    cameraDistance = 24f * density
                },
                shade = 0.32f * sin(PI.toFloat() * t),
            ) { PageBody(2 * k, page(2 * k), stats, onStamp) }
        } else {
            PageSurface(
                left = true,
                Modifier.offset(px(SPINE - PAGE_W), px(PAGE_TOP)).graphicsLayer {
                    transformOrigin = TransformOrigin(1f, 0.5f)
                    rotationY = 180f * (1f - t)
                    cameraDistance = 24f * density
                },
                shade = 0.32f * sin(PI.toFloat() * t),
            ) { PageBody(2 * (k + 1) - 1, page(2 * (k + 1) - 1), stats, onStamp) }
        }
        // 쪽 표시
        Box(Modifier.align(Alignment.BottomCenter).offset(y = -px(6))) {
            MockText("${(v + 0.5f).toInt().coerceIn(0, spreads - 1) + 1} / $spreads  ·  밀어서 넘겨 보세요", 19, color = Color(0xFF8A7F66))
        }
    }
}

@Composable
private fun PageSurface(left: Boolean, modifier: Modifier, shade: Float = 0f, content: @Composable () -> Unit) {
    val gutter = if (left) Brush.horizontalGradient(listOf(Color.Transparent, Color(0x26000000))) else Brush.horizontalGradient(listOf(Color(0x26000000), Color.Transparent))
    Box(modifier.size(px(PAGE_W), px(PAGE_H)).background(Parchment).border(1.dp, PageLine)) {
        Box(Modifier.align(if (left) Alignment.CenterEnd else Alignment.CenterStart).size(px(34), px(PAGE_H)).background(gutter))
        content()
        if (shade > 0.01f) Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = shade)))
    }
}

private val ColX = listOf(52, 157, 262)

@Composable
private fun PageBody(index: Int, page: BookPage?, stats: BookStats, onStamp: (PassportStamp) -> Unit) {
    if (index == 0) {
        Page0(stats, page, onStamp)
        return
    }
    if (page == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { MockText("—", 28, color = Faint) }
        return
    }
    Box(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxWidth().offset(y = px(14)), contentAlignment = Alignment.TopCenter) { MockText(page.title, 23, color = Color(0xFF8A7F66), font = MockSerif, weight = FontWeight.ExtraBold) }
        page.slots.forEachIndexed { i, s -> SlotAt(s, ColX[i % 3], 100 + (i / 3) * 122, 96, onStamp) }
        if (page.slots.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                MockText("영수증 인증으로\n첫 도장을 찍어 보세요", 22, color = Faint, align = TextAlign.Center, maxLines = 2, lineHeightPx = 32)
            }
        }
        Box(Modifier.fillMaxWidth().offset(y = px(PAGE_H - 36)), contentAlignment = Alignment.TopCenter) { MockText("– ${index + 1} –", 20, color = Faint) }
    }
}

@Composable
private fun Page0(stats: BookStats, page: BookPage?, onStamp: (PassportStamp) -> Unit) {
    Box(Modifier.fillMaxSize()) {
        // 숫자 상자 (점선 테두리)
        Canvas(Modifier.offset(px(11), px(0)).size(px(297), px(172))) {
            drawRoundRect(
                color = Color(0xFFCFC5AE), cornerRadius = androidx.compose.ui.geometry.CornerRadius(14.dp.toPx()),
                style = Stroke(width = 1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f))),
            )
            drawLine(Color(0xFFD8CEB7), Offset(size.width * 0.5f, 18.dp.toPx()), Offset(size.width * 0.5f, size.height - 18.dp.toPx()), strokeWidth = 1.dp.toPx())
        }
        MockPic(R.drawable.mk_col_ic_plane, 60, 45, Modifier.offset(px(54), px(22)))
        MockPic(R.drawable.mk_col_ic_seal, 47, 42, Modifier.offset(px(210), px(24)))
        Box(Modifier.offset(px(11), px(70)).size(px(148), px(32)), contentAlignment = Alignment.Center) { MockText("방문한 노포", 21, color = MockColors.Navy, weight = FontWeight.Medium) }
        Box(Modifier.offset(px(159), px(70)).size(px(148), px(32)), contentAlignment = Alignment.Center) { MockText("수집한 칭호", 21, color = MockColors.Navy, weight = FontWeight.Medium) }
        Box(Modifier.offset(px(11), px(100)).size(px(148), px(64)), contentAlignment = Alignment.Center) {
            androidx.compose.foundation.layout.Row(verticalAlignment = Alignment.Bottom) {
                MockText("${stats.visited}", 56, font = MockSerif, weight = FontWeight.ExtraBold)
                MockText(" / ${stats.goal}", 30, color = MockColors.Ink2, font = MockSerif)
            }
        }
        Box(Modifier.offset(px(159), px(100)).size(px(148), px(64)), contentAlignment = Alignment.Center) {
            androidx.compose.foundation.layout.Row(verticalAlignment = Alignment.Bottom) {
                MockText("${stats.titles}", 56, font = MockSerif, weight = FontWeight.ExtraBold)
                MockText(" 개", 28, color = MockColors.Ink2, font = MockSerif)
            }
        }
        page?.slots?.forEachIndexed { i, s -> SlotAt(s, ColX[i % 3], 245 + (i / 3) * 118, 96, onStamp) }
    }
}

@Composable
private fun SlotAt(slot: Slot, cx: Int, cy: Int, d: Int, onStamp: (PassportStamp) -> Unit) {
    Box(Modifier.offset(px(cx - d / 2), px(cy - d / 2)).size(px(d))) {
        when (slot) {
            is Slot.Region -> RegionStampView(slot.r)
            is Slot.Stamp -> StoreStampView(slot.s, onClick = { onStamp(slot.s) })
            Slot.Empty -> Canvas(Modifier.fillMaxSize()) {
                drawCircle(Color(0xFFE3DAC4), style = Stroke(1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 8f))))
            }
        }
    }
}

@Composable
private fun RegionStampView(r: RegionProgress) {
    val color = RegionColors[abs(r.sido.hashCode()) % RegionColors.size]
    Box(Modifier.fillMaxSize().rotate(if (r.stamped) -6f else 0f), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.minDimension
            if (r.stamped) {
                drawCircle(color, radius = w / 2 - 2.dp.toPx(), style = Stroke(2.6.dp.toPx()))
                drawCircle(color, radius = w / 2 - 7.dp.toPx(), style = Stroke(1.dp.toPx()))
            } else {
                drawCircle(Color(0xFFC4BAA2), radius = w / 2 - 2.dp.toPx(), style = Stroke(1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(7f, 7f))))
            }
        }
        androidx.compose.foundation.layout.Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (r.stamped) {
                MockText(r.sido, if (r.sido.length > 3) 19 else 24, color = color, font = MockSerif, weight = FontWeight.ExtraBold)
                MockText("${r.visited}곳", 17, color = color, weight = FontWeight.Medium)
            } else {
                MockText("?", 38, color = Color(0xFFB8AE96), font = MockSerif)
                MockText(r.sido, 17, color = Color(0xFFA89E86))
                MockText("${r.visited}/${r.target}", 16, color = Color(0xFFB8AE96))
            }
        }
    }
}

@Composable
private fun StoreStampView(s: PassportStamp, onClick: () -> Unit) {
    val grade = StampGradePolicy.gradeFor(s.visit.yearsAwarded)
    val color = when (grade) {
        StampGrade.NORMAL -> Color(0xFF1F4E8C)
        StampGrade.RED -> Color(0xFFB4332A)
        StampGrade.GOLD -> Color(0xFF9A7414)
    }
    val tilt = (abs(s.visit.storeId.hashCode()) % 9 - 4).toFloat()
    Box(Modifier.fillMaxSize().rotate(tilt).clickable(role = Role.Button, onClick = onClick), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.minDimension
            drawCircle(color, radius = w / 2 - 2.dp.toPx(), style = Stroke(2.6.dp.toPx()))
            drawCircle(color, radius = w / 2 - 7.dp.toPx(), style = Stroke(1.dp.toPx()))
        }
        androidx.compose.foundation.layout.Column(Modifier.size(px(62)), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center) {
            MockText(s.store?.name ?: s.visit.storeId, 15, color = color, font = MockSerif, weight = FontWeight.ExtraBold, maxLines = 3, align = TextAlign.Center, lineHeightPx = 17)
            MockText("${s.visit.yearsAwarded}년", 15, color = color, weight = FontWeight.Bold)
        }
    }
}
