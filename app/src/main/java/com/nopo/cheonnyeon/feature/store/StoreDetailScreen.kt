package com.nopo.cheonnyeon.feature.store

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nopo.cheonnyeon.R
import com.nopo.cheonnyeon.core.common.message
import com.nopo.cheonnyeon.core.designsystem.component.EmptyState
import com.nopo.cheonnyeon.core.designsystem.mock.BookmarkIcon
import com.nopo.cheonnyeon.core.designsystem.mock.ChevronRight
import com.nopo.cheonnyeon.core.designsystem.mock.MockColors
import com.nopo.cheonnyeon.core.designsystem.mock.MockPic
import com.nopo.cheonnyeon.core.designsystem.mock.MockSerif
import com.nopo.cheonnyeon.core.designsystem.mock.MockStage
import com.nopo.cheonnyeon.core.designsystem.mock.MockText
import com.nopo.cheonnyeon.core.designsystem.mock.PinIcon
import com.nopo.cheonnyeon.core.designsystem.mock.StarIcon
import com.nopo.cheonnyeon.core.designsystem.mock.at
import com.nopo.cheonnyeon.core.designsystem.mock.px
import com.nopo.cheonnyeon.core.designsystem.theme.NopoTheme
import com.nopo.cheonnyeon.core.util.GeoDistance
import com.nopo.cheonnyeon.domain.model.Store
import com.nopo.cheonnyeon.feature.common.StoreCards
import kotlinx.coroutines.launch

private const val GOOGLE_PHOTO_PREFIX = "file:///android_asset/places/"
private const val VISITSEOUL_PHOTO_PREFIX = "file:///android_asset/visitseoul/"

/** 시안 "가게 상세"(1.webp) 복제. 사진은 가게의 실제 사진, 거리는 현재 위치 기준 km. 리뷰·번역·제보 등 기존 기능은 아래에 이어 붙인다. */
@Composable
fun StoreDetailScreen(vm: StoreDetailViewModel, onBack: () -> Unit, onVerify: () -> Unit, translateBar: @Composable (Store) -> Unit = {}, reviews: @Composable (Store) -> Unit) {
    val state by vm.state.collectAsStateWithLifecycle()
    val store = state.store
    val translation by vm.translation.collectAsStateWithLifecycle()
    if (store == null) {
        EmptyState(stringResource(if (state.loading) R.string.list_loading else R.string.detail_not_found), actionLabel = stringResource(R.string.detail_back), onAction = onBack)
        return
    }
    val context = LocalContext.current
    val saved by vm.bookmarked.collectAsStateWithLifecycle()
    val ratings by vm.ratings.collectAsStateWithLifecycle()
    val fix by vm.lastFix.collectAsStateWithLifecycle()
    val rating = ratings[store.id]
    val km = if (fix != null && store.lat != null && store.lng != null) GeoDistance.meters(fix!!.lat, fix!!.lng, store.lat, store.lng) else null
    var allMenu by rememberSaveable { mutableStateOf(false) }
    var introOpen by rememberSaveable { mutableStateOf(false) }
    val requested by vm.reviewRequested.collectAsStateWithLifecycle()
    val scroll = rememberScrollState()
    LaunchedEffect(requested) {
        if (requested) { kotlinx.coroutines.delay(300); scroll.animateScrollTo(scroll.maxValue) }
    }
    val tag = StoreCards.tag(store, state.years.let { java.time.LocalDate.now(com.nopo.cheonnyeon.core.common.SeoulZone).year })
    val menus = store.menuItems.filter { it.isNotBlank() }.take(8)
    val intro = (store.story?.takeIf { it.isNotBlank() } ?: store.summary).orEmpty().trim()
    val introShown = translation?.pick(intro) ?: intro
    val photoCount = store.photoUrls.size.coerceAtLeast(1)
    val pager = rememberPagerState { photoCount }

    val heroPx = with(androidx.compose.ui.platform.LocalDensity.current) { px(600).toPx() }
    Box(Modifier.fillMaxSize().background(MockColors.Cream)) {
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.weight(1f).verticalScroll(scroll)) {
            MockStage(originY = 0, heightPx = 1540) {
                // 사진
                HorizontalPager(pager, Modifier.at(0, 0).fillMaxWidth().height(px(668))) { page ->
                    Box(Modifier.fillMaxSize()) {
                        StorePhoto(store, Modifier.fillMaxSize(), index = page)
                        val photo = store.photoUrls.getOrNull(page)
                        val credit = when {
                            photo?.startsWith(GOOGLE_PHOTO_PREFIX) == true -> R.string.photo_credit_google
                            photo?.startsWith(VISITSEOUL_PHOTO_PREFIX) == true -> R.string.photo_credit_visitseoul
                            else -> null
                        }
                        if (credit != null) MockText(stringResource(credit), 16, Modifier.align(Alignment.TopEnd).padding(top = px(150), end = px(16)), color = Color(0xCCFFFFFF))
                    }
                }
                Box(Modifier.at(0, 0).fillMaxWidth().height(px(190)).background(Brush.verticalGradient(listOf(Color(0x99000000), Color.Transparent))))
                Box(Modifier.at(0, 250).fillMaxWidth().height(px(420)).background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xCC000000)))))
                // 상단 줄
                Box(Modifier.at(20, 72).size(px(64), px(64)).clickable(role = Role.Button, onClick = onBack), contentAlignment = Alignment.Center) { BackChevron() }
                MockText("노포 패스포트", 23, Modifier.at(88, 88), color = Color.White, weight = FontWeight.Medium)
                if (!store.isTest) {
                    Box(Modifier.at(600, 72).size(px(64), px(64)).clickable(role = Role.Button, onClick = vm::toggleBookmark), contentAlignment = Alignment.Center) {
                        BookmarkIcon(40, filled = saved)
                    }
                }
                Box(Modifier.at(676, 72).size(px(64), px(64)).clickable(role = Role.Button) { share(context, store) }, contentAlignment = Alignment.Center) { ShareIcon() }
                // 제목 영역
                Row(Modifier.at(40, 268), horizontalArrangement = Arrangement.spacedBy(px(10))) {
                    Pill(tag)
                    state.visit?.let { Pill("방문 완료 ${it.visitCount}회", dark = true) }
                }
                MockText(store.name, 54, Modifier.at(40, 318).width(px(680)), color = Color.White, font = MockSerif, weight = FontWeight.ExtraBold)
                Row(Modifier.at(40, 400), verticalAlignment = Alignment.CenterVertically) {
                    PinIcon(26, color = Color.White)
                    Box(Modifier.width(px(10)))
                    MockText(listOf(store.sido, store.sigungu).filter { it.isNotBlank() }.joinToString(" ").ifBlank { store.address }, 25, color = Color.White, weight = FontWeight.Medium)
                }
                MockText(
                    (translation?.pick(store.summary.orEmpty()) ?: store.summary).orEmpty().ifBlank { store.menuItems.take(3).joinToString(", ") },
                    24, Modifier.at(40, 448).width(px(600)), color = Color.White, maxLines = 2, lineHeightPx = 36,
                )
                // 평점 | 거리
                Row(Modifier.at(40, 548), verticalAlignment = Alignment.CenterVertically) {
                    if (rating != null) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                StarIcon(32)
                                Box(Modifier.width(px(10)))
                                MockText(String.format("%.1f", rating.average), 32, color = Color.White, weight = FontWeight.Bold)
                            }
                            MockText("(${"%,d".format(rating.count)}개)", 19, color = Color(0xCCFFFFFF))
                        }
                        Box(Modifier.padding(horizontal = px(30)).width(1.dp).height(px(64)).background(Color(0x66FFFFFF)))
                    }
                    Column {
                        MockText(km?.let { StoreCards.formatDistance(it) } ?: "-", 30, color = Color.White, weight = FontWeight.Bold)
                        MockText("현재 위치에서", 19, color = Color(0xCCFFFFFF))
                    }
                }
                if (photoCount > 1) {
                    Box(Modifier.at(670, 590).clip(RoundedCornerShape(50)).background(Color(0x99000000)).padding(horizontal = px(18), vertical = px(6))) {
                        MockText("${pager.currentPage + 1} / $photoCount", 21, color = Color.White, weight = FontWeight.Medium)
                    }
                }
                // 시트
                Box(Modifier.at(0, 640).fillMaxWidth().height(px(900)).clip(RoundedCornerShape(topStart = px(40), topEnd = px(40))).background(MockColors.Cream))
                MockPic(R.drawable.mk_det_card, 732, 232, Modifier.at(14, 640))
                MockText("시간이 만든 맛을 여행하는 곳", 22, Modifier.at(189, 672), color = MockColors.Ink2)
                MockText(
                    store.foundedYear?.let { "${it}년부터, ${state.years}년 전통" } ?: "${state.years}년 전통",
                    33, Modifier.at(189, 710).width(px(390)), font = MockSerif, weight = FontWeight.ExtraBold,
                )
                MockText("한 그릇에 담긴 세월,\n지금도 변함없는 맛으로\n사람들을 맞이합니다.", 22, Modifier.at(189, 758).width(px(380)), color = MockColors.Ink2, maxLines = 3, lineHeightPx = 31)

                // 대표 메뉴
                MockPic(R.drawable.mk_det_title_menu, 146, 44, Modifier.at(14, 892))
                if (menus.size > 3) MockPic(R.drawable.mk_det_menu_all, 155, 30, Modifier.at(590, 900), onClick = { allMenu = true })
                if (menus.isEmpty()) {
                    Box(Modifier.at(18, 950).size(px(724), px(120)).mockMenuCard(), contentAlignment = Alignment.Center) {
                        MockText("대표 메뉴 정보를 준비 중이에요.", 24, color = MockColors.Hint)
                    }
                } else {
                    LazyRow(
                        Modifier.at(0, 940).fillMaxWidth().height(px(285)),
                        contentPadding = PaddingValues(horizontal = px(17)), horizontalArrangement = Arrangement.spacedBy(px(14)),
                    ) {
                        items(menus.size.coerceAtMost(5)) { i -> MenuCard(store, menus[i], i, state.years) }
                    }
                }

                // 가게 소개
                MockPic(R.drawable.mk_det_title_intro, 126, 56, Modifier.at(14, 1240))
                MockPic(R.drawable.mk_det_deco_intro, 120, 111, Modifier.at(640, 1285))
                if (intro.isNotBlank() && intro.length > 90) MockPic(R.drawable.mk_det_more, 85, 35, Modifier.at(660, 1250), onClick = { introOpen = true })
                MockText(
                    introShown.ifBlank { "${store.name}의 소개가 아직 없어요." },
                    21, Modifier.at(22, 1300).width(px(610)), color = MockColors.Ink2, maxLines = 3, lineHeightPx = 29,
                )
                // 영업시간 · 주소 · 전화
                Box(Modifier.at(18, 1400).size(px(724), px(108)).clip(RoundedCornerShape(px(18))).background(MockColors.Card).border(1.dp, MockColors.CardLine, RoundedCornerShape(px(18))))
                Canvas(Modifier.at(0, 1400).size(px(760), px(108))) {
                    listOf(245f, 527f).forEach { x ->
                        val xx = x * size.width / 760f
                        drawLine(Color(0xFFD5CFBC), Offset(xx, size.height * 0.2f), Offset(xx, size.height * 0.8f), strokeWidth = 1.2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 5f)))
                    }
                }
                MockPic(R.drawable.mk_det_ic_clock, 34, 34, Modifier.at(36, 1414))
                MockText("영업시간", 17, Modifier.at(80, 1418), color = MockColors.Hint)
                MockText(store.hours?.takeIf { it.isNotBlank() } ?: "정보 없음", 21, Modifier.at(80, 1446).width(px(160)), weight = FontWeight.Bold, maxLines = 2, lineHeightPx = 26)
                Box(Modifier.at(250, 1404).size(px(280), px(100)).clickable(enabled = store.lat != null && store.lng != null, role = Role.Button) { openDirections(context, store) })
                MockPic(R.drawable.mk_det_ic_pin, 32, 34, Modifier.at(270, 1416))
                MockText("주소", 17, Modifier.at(316, 1418), color = MockColors.Hint)
                MockText(store.address.ifBlank { "정보 없음" }, 20, Modifier.at(316, 1446).width(px(180)), weight = FontWeight.Bold, maxLines = 2, lineHeightPx = 25)
                ChevronRight(26, Modifier.at(498, 1452))
                val phone = store.phone?.takeIf { it.isNotBlank() }
                Box(Modifier.at(532, 1404).size(px(206), px(100)).clickable(enabled = phone != null, role = Role.Button) { phone?.let { dial(context, it) } })
                MockPic(R.drawable.mk_det_ic_phone, 34, 32, Modifier.at(552, 1416))
                MockText("전화번호", 17, Modifier.at(596, 1418), color = MockColors.Hint)
                MockText(phone?.let { formatPhone(it) } ?: "정보 없음", 18, Modifier.at(594, 1450).width(px(120)), weight = FontWeight.Bold, maxLines = 1)
                if (phone != null) ChevronRight(24, Modifier.at(712, 1452))
            }
            // 시안 아래로 이어지는 기존 기능들
            Column(Modifier.padding(horizontal = px(22)).padding(bottom = px(30)), verticalArrangement = Arrangement.spacedBy(px(14))) {
                if (store.amenities.isNotEmpty()) MockText("이용 정보 · ${store.amenities.joinToString(", ")}", 21, color = MockColors.Ink2, maxLines = 3)
                translateBar(store)
                reviews(store)
                if (!store.isTest) InfoReport(vm)
                store.sourceUrl?.let { MockText(stringResource(R.string.detail_source, it), 17, color = MockColors.Hint, maxLines = 2) }
            }
        }
        // 하단 바
        Row(
            Modifier.fillMaxWidth().background(MockColors.Cream).border(1.dp, MockColors.CardLine).navigationBarsPadding().padding(horizontal = px(18), vertical = px(14)),
            horizontalArrangement = Arrangement.spacedBy(px(12)), verticalAlignment = Alignment.CenterVertically,
        ) {
            if (!store.isTest) {
                Row(
                    Modifier.weight(0.8f).height(px(76)).clip(RoundedCornerShape(px(18))).background(MockColors.Chip).clickable(role = Role.Button, onClick = vm::toggleBookmark),
                    horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
                ) {
                    BookmarkIcon(36, color = MockColors.Navy, filled = saved)
                    Box(Modifier.width(px(10)))
                    MockText(if (saved) "저장됨" else "저장하기", 23, weight = FontWeight.Medium)
                }
            }
            Row(
                Modifier.weight(1f).height(px(76)).clip(RoundedCornerShape(px(18))).border(2.dp, MockColors.Navy, RoundedCornerShape(px(18)))
                    .clickable(enabled = store.lat != null && store.lng != null, role = Role.Button) { openDirections(context, store) },
                horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
            ) {
                MockPic(R.drawable.mk_det_ic_go, 50, 40)
                Box(Modifier.width(px(10)))
                MockText("가보기", 25, weight = FontWeight.Bold)
            }
            Row(
                Modifier.weight(1.4f).height(px(76)).clip(RoundedCornerShape(px(18))).background(MockColors.Lime).clickable(role = Role.Button, onClick = onVerify),
                horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
            ) {
                MockPic(R.drawable.mk_det_ic_stamp, 46, 52)
                Box(Modifier.width(px(12)))
                MockText("방문 인증하기", 26, weight = FontWeight.ExtraBold)
            }
        }
    }
    // 사진을 지나 스크롤하면 상태 표시줄 뒤를 종이색으로 덮어 글자가 겹쳐 보이지 않게 한다
    if (scroll.value > heroPx) Box(Modifier.align(Alignment.TopStart).fillMaxWidth().windowInsetsTopHeight(WindowInsets.statusBars).background(MockColors.Cream))
    }

    if (allMenu) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { allMenu = false },
            title = { Text(stringResource(R.string.detail_menu)) },
            text = { Text(store.menuItems.filter { it.isNotBlank() }.joinToString("\n")) },
            confirmButton = { androidx.compose.material3.TextButton({ allMenu = false }) { Text(stringResource(R.string.passport_close)) } },
        )
    }
    if (introOpen) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { introOpen = false },
            title = { Text(stringResource(R.string.store_story_title)) },
            text = { Column(Modifier.verticalScroll(rememberScrollState())) { Text(introShown); if (!store.isTest && !store.story.isNullOrBlank()) Text(stringResource(R.string.store_story_source), style = NopoTheme.typography.bodySmall) } },
            confirmButton = { androidx.compose.material3.TextButton({ introOpen = false }) { Text(stringResource(R.string.passport_close)) } },
        )
    }
}

@Composable
private fun Pill(text: String, dark: Boolean = false) {
    Box(Modifier.clip(RoundedCornerShape(50)).background(if (dark) Color(0x99000000) else MockColors.Lime).padding(horizontal = px(16), vertical = px(5))) {
        MockText(text, 21, color = if (dark) Color.White else MockColors.Navy, weight = FontWeight.Bold)
    }
}

@Composable
private fun BackChevron() {
    Canvas(Modifier.size(px(30), px(40))) {
        val p = androidx.compose.ui.graphics.Path().apply { moveTo(size.width * 0.8f, size.height * 0.1f); lineTo(size.width * 0.2f, size.height * 0.5f); lineTo(size.width * 0.8f, size.height * 0.9f) }
        drawPath(p, Color.White, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3.2.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round))
    }
}

@Composable
private fun ShareIcon() {
    Canvas(Modifier.size(px(40))) {
        val w = size.width
        val stroke = androidx.compose.ui.graphics.drawscope.Stroke(width = w * 0.08f, cap = androidx.compose.ui.graphics.StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round)
        val box = androidx.compose.ui.graphics.Path().apply {
            moveTo(w * 0.3f, w * 0.42f); lineTo(w * 0.18f, w * 0.42f); lineTo(w * 0.18f, w * 0.9f); lineTo(w * 0.82f, w * 0.9f); lineTo(w * 0.82f, w * 0.42f); lineTo(w * 0.7f, w * 0.42f)
        }
        drawPath(box, Color.White, style = stroke)
        drawLine(Color.White, Offset(w * 0.5f, w * 0.65f), Offset(w * 0.5f, w * 0.08f), strokeWidth = w * 0.08f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
        val head = androidx.compose.ui.graphics.Path().apply { moveTo(w * 0.34f, w * 0.24f); lineTo(w * 0.5f, w * 0.08f); lineTo(w * 0.66f, w * 0.24f) }
        drawPath(head, Color.White, style = stroke)
    }
}

@Composable
private fun Modifier.mockMenuCard(): Modifier = this.clip(RoundedCornerShape(px(16))).background(MockColors.Card).border(1.dp, MockColors.CardLine, RoundedCornerShape(px(16)))

/** 대표 메뉴 카드: 요리 종류가 같은 자유 이용 사진(Wikimedia Commons)을 쓰고, 맞는 사진이 없으면 가게 사진, 첫 카드에만 "대표 메뉴" 태그 */
@Composable
private fun MenuCard(store: Store, menu: String, index: Int, years: Int) {
    Column(Modifier.width(px(343)).height(px(280)).mockMenuCard()) {
        Box(Modifier.fillMaxWidth().height(px(170))) {
            val menuUrl = MenuPhotos.urlFor(menu)
            if (menuUrl != null) {
                coil3.compose.AsyncImage(menuUrl, contentDescription = menu, contentScale = androidx.compose.ui.layout.ContentScale.Crop, modifier = Modifier.fillMaxSize())
                val credit = MenuPhotos.credit(LocalContext.current, menu)
                if (credit != null) MockText(credit, 14, Modifier.align(Alignment.BottomEnd).padding(end = px(8), bottom = px(6)), color = Color(0xCCFFFFFF))
            } else {
                val photoIndex = if (store.photoUrls.isEmpty()) 0 else index % store.photoUrls.size
                StorePhoto(store, Modifier.fillMaxSize(), index = photoIndex)
            }
            if (index == 0) Box(Modifier.padding(start = px(15), top = px(15))) { Pill("대표 메뉴") }
        }
        Column(Modifier.padding(horizontal = px(16)).padding(top = px(8)), verticalArrangement = Arrangement.spacedBy(px(4))) {
            MockText(menu, 27, font = MockSerif, weight = FontWeight.ExtraBold)
            MockText(listOf(store.categoryGroup.takeIf { it.isNotBlank() }, "${years}년 전통").filterNotNull().joinToString(" · "), 20, color = MockColors.Hint)
        }
    }
}

private fun formatPhone(raw: String): String {
    val d = raw.filter { it.isDigit() }
    return when {
        d.startsWith("02") && d.length >= 9 -> "02-${d.substring(2, d.length - 4)}-${d.takeLast(4)}"
        d.length == 11 -> "${d.substring(0, 3)}-${d.substring(3, 7)}-${d.substring(7)}"
        d.length == 10 -> "${d.substring(0, 3)}-${d.substring(3, 6)}-${d.substring(6)}"
        d.length == 8 -> "${d.substring(0, 4)}-${d.substring(4)}"
        else -> raw
    }
}

private fun share(context: Context, store: Store) {
    val text = "${store.name} · ${store.address}\n노포 패스포트에서 만난 노포"
    val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text)
    try { context.startActivity(Intent.createChooser(send, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } catch (_: ActivityNotFoundException) { }
}

/** 정보 오류 제보 (FR-STORE-06): 사유를 고르면 운영자에게 접수된다 */
@Composable
private fun InfoReport(vm: StoreDetailViewModel) {
    val c = NopoTheme.colors
    val context = LocalContext.current
    val sentText = stringResource(R.string.store_report_sent)
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    var open by rememberSaveable { mutableStateOf(false) }
    var message by androidx.compose.runtime.remember { mutableStateOf<String?>(null) }
    androidx.compose.material3.TextButton(onClick = { message = null; open = true }) { Text(stringResource(R.string.store_report_link), color = c.ink2) }
    message?.let { Text(it, style = NopoTheme.typography.bodySmall, color = c.primary) }
    if (open) {
        val reasons = listOf(R.string.store_report_closed, R.string.store_report_hours, R.string.store_report_phone, R.string.store_report_menu, R.string.store_report_other)
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { open = false },
            title = { Text(stringResource(R.string.store_report_title)) },
            text = {
                Column {
                    reasons.forEach { res ->
                        val label = stringResource(res)
                        androidx.compose.material3.TextButton(onClick = {
                            open = false
                            scope.launch {
                                val r = vm.reportInfo(label)
                                message = if (r is com.nopo.cheonnyeon.core.common.AppResult.Success) sentText
                                else r.errorOrNull?.message(context)
                            }
                        }) { Text(label) }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { androidx.compose.material3.TextButton(onClick = { open = false }) { Text(stringResource(R.string.detail_back)) } },
        )
    }
}

/** 네이버 지도 앱으로 길찾기, 앱이 없으면 웹 지도 (FR-STORE-05) */
private fun openDirections(context: Context, store: Store) {
    val name = Uri.encode(store.name)
    val app = Uri.parse("nmap://route/walk?dlat=${store.lat}&dlng=${store.lng}&dname=$name&appname=${context.packageName}")
    val web = Uri.parse("https://map.naver.com/p/search/$name")
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, app).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (_: ActivityNotFoundException) {
        context.startActivity(Intent(Intent.ACTION_VIEW, web).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}

private fun dial(context: Context, phone: String) {
    try {
        context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (_: ActivityNotFoundException) {
    }
}
