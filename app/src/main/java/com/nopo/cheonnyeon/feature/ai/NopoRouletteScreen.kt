package com.nopo.cheonnyeon.feature.ai

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.nopo.cheonnyeon.R
import com.nopo.cheonnyeon.app.AppContainer
import com.nopo.cheonnyeon.core.common.AppResult
import com.nopo.cheonnyeon.core.common.SeoulZone
import com.nopo.cheonnyeon.core.designsystem.mock.BookmarkIcon
import com.nopo.cheonnyeon.core.designsystem.mock.ChevronDown
import com.nopo.cheonnyeon.core.designsystem.mock.ChevronRight
import com.nopo.cheonnyeon.core.designsystem.mock.MockColors
import com.nopo.cheonnyeon.core.designsystem.mock.MockPic
import com.nopo.cheonnyeon.core.designsystem.mock.MockSerif
import com.nopo.cheonnyeon.core.designsystem.mock.MockStage
import com.nopo.cheonnyeon.core.designsystem.mock.MockText
import com.nopo.cheonnyeon.core.designsystem.mock.PinIcon
import com.nopo.cheonnyeon.core.designsystem.mock.RatingText
import com.nopo.cheonnyeon.core.designsystem.mock.at
import com.nopo.cheonnyeon.core.designsystem.mock.px
import com.nopo.cheonnyeon.data.ai.ChatContextInfo
import com.nopo.cheonnyeon.domain.model.Store
import com.nopo.cheonnyeon.domain.rule.ChatCandidate
import com.nopo.cheonnyeon.domain.rule.RouletteFilter
import com.nopo.cheonnyeon.domain.rule.RoulettePicker
import com.nopo.cheonnyeon.domain.rule.YearsCalculator
import com.nopo.cheonnyeon.feature.common.StoreCardData
import com.nopo.cheonnyeon.feature.common.StoreCards
import com.nopo.cheonnyeon.feature.main.MockNavBar
import com.nopo.cheonnyeon.feature.store.StorePhoto
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

enum class RoulettePhase { IDLE, SPINNING, RESULT }

data class NopoRouletteState(
    val filter: RouletteFilter = RouletteFilter(),
    val price: String = "상관없음",
    val regions: List<String> = emptyList(),
    val phase: RoulettePhase = RoulettePhase.IDLE,
    val pick: StoreCardData? = null,
    val reason: String = "",
    val segment: String = "",
    val notice: String? = null,
    val reel: List<Any> = emptyList(),
    val consent: Boolean = false,
    val last: StoreCardData? = null,
)

/** 시안 "AI 노포 룰렛": 조건(지역·가격대·음식 종류·도보 거리)을 고르고 돌리면 사진이 돌아가다가 오늘의 노포가 정해진다. */
class NopoRouletteViewModel(private val container: AppContainer) : ViewModel() {
    private val _state = MutableStateFlow(NopoRouletteState())
    val state: StateFlow<NopoRouletteState> = _state.asStateFlow()
    private val legacy = MenuRouletteViewModel(container)
    private var stores: List<Store> = emptyList()
    private val year = LocalDate.now(SeoulZone).year

    init {
        viewModelScope.launch {
            stores = container.catalog.all().filter { !it.isTest && it.isFood }
            _state.update { it.copy(regions = stores.map { s -> s.sido }.filter { s -> s.isNotBlank() }.distinct().sorted(), consent = container.settings.aiConsent.first()) }
        }
        viewModelScope.launch { legacy.state.collect { s -> _state.update { it.copy(reel = s.reel) } } }
        container.refreshLocation()
    }

    fun setSido(v: String?) = _state.update { it.copy(filter = it.filter.copy(sido = v)) }
    fun setCuisine(v: String?) = _state.update { it.copy(filter = it.filter.copy(cuisine = v)) }
    fun setWalk(v: Int?) = _state.update { it.copy(filter = it.filter.copy(walkMeters = v)) }
    fun setPrice(v: String) = _state.update { it.copy(price = v) }
    fun backToIdle() = _state.update { it.copy(phase = RoulettePhase.IDLE) }
    fun toggleBookmark(id: String) { viewModelScope.launch { container.settings.toggleBookmark(id) } }
    fun agree() { viewModelScope.launch { container.settings.setAiConsent(true); _state.update { it.copy(consent = true) } } }

    fun spin() {
        if (_state.value.phase == RoulettePhase.SPINNING) return
        _state.update { it.copy(phase = RoulettePhase.SPINNING, notice = null) }
        viewModelScope.launch {
            val started = android.os.SystemClock.elapsedRealtime()
            val filter = _state.value.filter
            val fix = container.lastFix.value
            val here = fix?.let { it.lat to it.lng }
            var list = RoulettePicker.candidates(stores, filter, here)
            var notice: String? = null
            if (list.isEmpty() && filter.walkMeters != null) {
                list = RoulettePicker.candidates(stores, filter.copy(walkMeters = null), here)
                notice = "가까운 곳에 없어서 거리 조건을 풀고 골랐어요."
            }
            if (list.isEmpty() && filter.cuisine != null) {
                list = RoulettePicker.candidates(stores, filter.copy(cuisine = null, walkMeters = null), here)
                notice = "조건에 맞는 노포가 없어서 음식 종류를 풀고 골랐어요."
            }
            if (list.isEmpty()) {
                list = RoulettePicker.candidates(stores, RouletteFilter(), here)
                notice = "조건을 모두 풀고 골랐어요."
            }
            val ratings = container.ratings.value.mapValues { it.value.average }
            var pick = RoulettePicker.pick(list, ratings)
            var reason = ""
            if (pick != null && container.ai.available && _state.value.consent) {
                val ai = askAi(list, filter)
                if (ai != null) {
                    pick = list.firstOrNull { it.id == ai.first } ?: pick
                    if (pick.id == ai.first) reason = ai.second
                }
            }
            val chosen = pick
            if (chosen == null) {
                _state.update { it.copy(phase = RoulettePhase.IDLE, notice = "고를 노포가 없어요.") }
                return@launch
            }
            if (reason.isBlank()) reason = fallbackReason(chosen, filter)
            // 사진이 돌아가는 연출은 최소 3.5초
            val wait = 3500L - (android.os.SystemClock.elapsedRealtime() - started)
            if (wait > 0) delay(wait)
            val card = StoreCards.of(chosen, year, container.ratings.value, fix, container.settings.bookmarks.first())
            _state.update { it.copy(phase = RoulettePhase.RESULT, pick = card, reason = reason, segment = RoulettePicker.wheelSegment(chosen), notice = notice, last = card) }
        }
    }

    private suspend fun askAi(list: List<Store>, f: RouletteFilter): Pair<String, String>? {
        val fix = container.lastFix.value
        val candidates = list.sortedByDescending { it.photoUrls.isNotEmpty() }.take(80).shuffled().take(25).map { s ->
            ChatCandidate(
                s, YearsCalculator.yearsAtVisit(s.foundedYear?.takeIf { it <= year }, year),
                fix?.let { com.nopo.cheonnyeon.core.util.GeoDistance.meters(it.lat, it.lng, s.lat!!, s.lng!!) / 1000.0 }, false,
            )
        }
        val info = ChatContextInfo(
            nowText = LocalDate.now(SeoulZone).toString(), locationText = if (fix != null) "사용자 위치 기준" else "위치 모름",
            totalYears = 0, candidates = candidates,
        )
        val q = "노포 룰렛 조건: 지역 ${f.sido ?: "전국"}, 음식 ${f.cuisine ?: "아무거나"}, 가격대 ${_state.value.price}, 도보 ${f.walkMeters?.let { "${it}m 이내" } ?: "제한 없음"}. " +
            "후보 중 오늘 가볼 노포를 딱 한 곳만 골라 줘. 이유는 두세 문장."
        val reply = container.ai.chat(emptyList(), q, info)
        return (reply as? AppResult.Success)?.value?.picks?.firstOrNull()
    }

    private fun fallbackReason(s: Store, f: RouletteFilter): String {
        val years = YearsCalculator.yearsAtVisit(s.foundedYear?.takeIf { it <= year }, year)
        val menu = s.menuItems.firstOrNull { it.isNotBlank() }
        val cond = listOfNotNull(f.sido, f.cuisine).joinToString(" · ").ifBlank { "전국 · 아무거나" }
        return "${years}년 동안 자리를 지킨 노포예요." + (if (menu != null) " 대표 메뉴 '$menu'가 잘 알려져 있어요." else "") + " 고른 조건($cond)에 맞는 곳 중에서 룰렛이 정했어요."
    }
}

private val PriceOptions = listOf("상관없음", "1만원 이하", "1~2만원대", "2~3만원대", "3만원 이상")
private val WalkOptions = listOf<Int?>(500, 1000, 2000, 3000, null)

private fun walkLabel(m: Int?): String = if (m == null) "제한 없음" else if (m < 1000) "${m}m 이내" else "${m / 1000}km 이내"

@Composable
fun NopoRouletteScreen(vm: NopoRouletteViewModel, onBack: () -> Unit, onOpenStore: (String) -> Unit, onTab: (Int) -> Unit) {
    val state by vm.state.collectAsStateWithLifecycle()
    var askConsent by rememberSaveable { mutableStateOf(false) }
    androidx.activity.compose.BackHandler(enabled = state.phase == RoulettePhase.RESULT) { vm.backToIdle() }
    Column(Modifier.fillMaxSize().background(MockColors.Cream)) {
        Box(Modifier.weight(1f)) {
            if (state.phase == RoulettePhase.RESULT && state.pick != null) ResultView(vm, state, onClose = vm::backToIdle, onOpenStore = onOpenStore)
            else IdleView(vm, state, onBack = onBack, onSpin = { if (!state.consent) askConsent = true else vm.spin() })
        }
        MockNavBar(0, onTab)
    }
    if (askConsent) AiConsentDialog(onAgree = { askConsent = false; vm.agree(); vm.spin() }, onDismiss = { askConsent = false; vm.spin() })
}

@Composable
private fun CondChip(text: String, modifier: Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
        MockText(text, 22, weight = FontWeight.ExtraBold, maxLines = 1)
        Box(Modifier.width(px(8)))
        ChevronDown(20)
    }
}

@Composable
private fun IdleView(vm: NopoRouletteViewModel, state: NopoRouletteState, onBack: () -> Unit, onSpin: () -> Unit) {
    var menu by remember { mutableStateOf(0) }
    val spinning = state.phase == RoulettePhase.SPINNING
    Column(Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState())) {
        MockStage(originY = 84, heightPx = 1520) {
            MockPic(R.drawable.mk2_v_hdr, 460, 117, Modifier.at(20, 88))
            Box(Modifier.at(652, 98).size(px(76)).clickable(role = Role.Button, onClick = onBack).semantics { contentDescription = "닫기" }, contentAlignment = Alignment.Center) { CloseGlyph() }
            MockPic(R.drawable.mk2_r_title, 480, 155, Modifier.at(20, 225))
            MockPic(R.drawable.mk2_r_note, 245, 150, Modifier.at(500, 250))
            MockPic(R.drawable.mk2_r_filter, 720, 117, Modifier.at(20, 395))
            MockText("음식 종류", 21, Modifier.at(448, 418), color = MockColors.Navy, weight = FontWeight.Medium)
            val labels = listOf(
                state.filter.sido?.let { "$it 전체" } ?: "전국",
                state.price,
                state.filter.cuisine ?: "아무거나",
                walkLabel(state.filter.walkMeters),
            )
            val xs = listOf(28, 212, 404, 584)
            val ws = listOf(180, 186, 170, 156)
            labels.forEachIndexed { i, label ->
                Box(Modifier.at(xs[i], 450).size(px(ws[i]), px(58))) {
                    CondChip(label, Modifier.fillMaxSize().clickable(enabled = !spinning, role = Role.Button) { menu = i + 1 })
                    DropdownMenu(expanded = menu == i + 1, onDismissRequest = { menu = 0 }) {
                        when (i) {
                            0 -> {
                                DropdownMenuItem(text = { Text("전국") }, onClick = { vm.setSido(null); menu = 0 })
                                state.regions.forEach { r -> DropdownMenuItem(text = { Text(r) }, onClick = { vm.setSido(r); menu = 0 }) }
                            }
                            1 -> PriceOptions.forEach { p -> DropdownMenuItem(text = { Text(p) }, onClick = { vm.setPrice(p); menu = 0 }) }
                            2 -> {
                                DropdownMenuItem(text = { Text("아무거나") }, onClick = { vm.setCuisine(null); menu = 0 })
                                RoulettePicker.CUISINES.forEach { c -> DropdownMenuItem(text = { Text(c) }, onClick = { vm.setCuisine(c); menu = 0 }) }
                            }
                            else -> WalkOptions.forEach { w -> DropdownMenuItem(text = { Text(walkLabel(w)) }, onClick = { vm.setWalk(w); menu = 0 }) }
                        }
                    }
                }
            }
            MockPic(R.drawable.mk2_r_note2, 140, 140, Modifier.at(10, 960))
            MockPic(R.drawable.mk2_r_note3, 155, 120, Modifier.at(590, 940))
            // 룰렛판. 돌리는 동안은 사진 릴이 돌아간다
            MockPic(R.drawable.mk2_r_wheel, 610, 610, Modifier.at(95, 490))
            if (spinning) {
                Box(Modifier.at(118, 600).size(px(524), px(390)).clip(RoundedCornerShape(px(36))).background(Color(0xE6FCF7EB)).padding(px(10))) {
                    if (state.reel.isNotEmpty()) PhotoReel(state.reel, true, Modifier.fillMaxSize())
                    else Box(Modifier.fillMaxSize().background(MockColors.Navy, RoundedCornerShape(px(26))), contentAlignment = Alignment.Center) {
                        MockText("AI가 노포를 고르는 중…", 26, color = Color.White)
                    }
                }
            }
            Box(
                Modifier.at(145, 1093).size(px(475), px(92)).clip(RoundedCornerShape(50)).background(if (spinning) Color(0xFFDCE9A4) else MockColors.Lime2)
                    .clickable(enabled = !spinning, role = Role.Button, onClick = onSpin),
                contentAlignment = Alignment.Center,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ChevronLeftGlyph()
                    Box(Modifier.width(px(18)))
                    MockPic(R.drawable.mk2_r_dice, 60, 58)
                    Box(Modifier.width(px(14)))
                    MockText(if (spinning) "돌리는 중…" else "룰렛 돌리기", 33, weight = FontWeight.ExtraBold)
                }
            }
            state.notice?.let { MockText(it, 20, Modifier.at(40, 1192).width(px(680)), color = MockColors.Stamp, align = TextAlign.Center) }
            val last = state.last
            if (last != null) {
                MockPic(R.drawable.mk2_r_chip_tab, 327, 47, Modifier.at(45, 1215))
                Box(Modifier.at(20, 1268).size(px(720), px(230)).clip(RoundedCornerShape(px(26))).background(Color(0xFFFCFAF4)).border(1.dp, Color(0xFFE2DDCB), RoundedCornerShape(px(26)))) {
                    Box(Modifier.padding(px(16)).size(px(250), px(198)).clip(RoundedCornerShape(px(18)))) { StorePhoto(last.store, Modifier.fillMaxSize()) }
                    MockText(last.store.name, 31, Modifier.padding(start = px(290), top = px(22)).width(px(360)), font = MockSerif, weight = FontWeight.ExtraBold)
                    MockText(last.oneLiner, 21, Modifier.padding(start = px(290), top = px(74)).width(px(420)), color = Color(0xFF4B5563), maxLines = 2, lineHeightPx = 28)
                    Row(Modifier.padding(start = px(290), top = px(150))) {
                        RatingText(last.rating?.average, last.rating?.count, 21)
                        last.distanceLabel?.let { MockText("  ·  $it", 21, color = Color(0xFF4B5563)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun CloseGlyph() {
    Canvas(Modifier.size(px(38))) {
        val sw = size.width * 0.1f
        drawLine(MockColors.Navy, Offset(size.width * 0.1f, size.height * 0.1f), Offset(size.width * 0.9f, size.height * 0.9f), sw, StrokeCap.Round)
        drawLine(MockColors.Navy, Offset(size.width * 0.9f, size.height * 0.1f), Offset(size.width * 0.1f, size.height * 0.9f), sw, StrokeCap.Round)
    }
}

@Composable
private fun ChevronLeftGlyph() {
    Canvas(Modifier.size(px(24), px(34))) {
        val p = androidx.compose.ui.graphics.Path().apply {
            moveTo(size.width * 0.9f, size.height * 0.1f); lineTo(size.width * 0.15f, size.height * 0.5f); lineTo(size.width * 0.9f, size.height * 0.9f)
        }
        drawPath(p, MockColors.Navy, style = Stroke(size.width * 0.14f, cap = StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round))
    }
}

private val SegmentPhoto = mapOf("국밥" to "gukbap", "냉면" to "naengmyeon", "고기" to "gogi", "분식" to "tteokbokki", "시장맛집" to "kalguksu")

@Composable
private fun ResultView(vm: NopoRouletteViewModel, state: NopoRouletteState, onClose: () -> Unit, onOpenStore: (String) -> Unit) {
    val card = state.pick ?: return
    val store = card.store
    val context = LocalContext.current
    val photoCount = store.photoUrls.size.coerceAtLeast(1)
    val pager = rememberPagerState { photoCount }
    Column(Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState())) {
        MockStage(originY = 84, heightPx = 1560) {
            MockPic(R.drawable.mk2_v_hdr, 460, 117, Modifier.at(20, 88))
            Box(Modifier.at(652, 98).size(px(76)).clickable(role = Role.Button, onClick = onClose).semantics { contentDescription = "닫기" }, contentAlignment = Alignment.Center) { CloseGlyph() }
            MockPic(R.drawable.mk2_r6_title, 760, 285, Modifier.at(0, 205))
            // 휠 윗부분: 이번에 걸린 칸이 가운데에서 빛난다
            Canvas(Modifier.at(150, 470).size(px(460), px(160))) {
                val w = size.width
                drawArc(Color(0xFFD9B44A), 188f, 164f, false, Offset(w * 0.02f, w * 0.1f), androidx.compose.ui.geometry.Size(w * 0.96f, w * 0.96f), style = Stroke(w * 0.035f, cap = StrokeCap.Round))
            }
            val order = listOf("냉면", "국밥", state.segment, "고기", "분식")
            order.forEachIndexed { i, name ->
                val center = i == 2
                val w = if (center) 120 else 92
                val h = if (center) 96 else 74
                val x = 380 - w / 2 + (i - 2) * 112
                val y = if (center) 530 else 548 + kotlin.math.abs(i - 2) * 6
                Box(
                    Modifier.at(x, y).size(px(w), px(h)).clip(RoundedCornerShape(px(14))).background(Color(0xFFF1E3B8))
                        .then(if (center) Modifier.border(3.dp, Color(0xFFD9B44A), RoundedCornerShape(px(14))) else Modifier),
                    contentAlignment = Alignment.Center,
                ) {
                    val asset = SegmentPhoto[name]
                    if (asset != null) coil3.compose.AsyncImage("file:///android_asset/menu/$asset.webp", name, Modifier.fillMaxSize(), contentScale = androidx.compose.ui.layout.ContentScale.Crop)
                    else MockText(name, 20, weight = FontWeight.ExtraBold)
                }
            }
            Box(Modifier.at(358, 470)) { PinIcon(46, color = Color(0xFFD9A323)) }

            // 사진 카드
            Box(Modifier.at(22, 628).size(px(716), px(296)).clip(RoundedCornerShape(topStart = px(36), topEnd = px(36)))) {
                HorizontalPager(pager, Modifier.fillMaxSize()) { page -> StorePhoto(store, Modifier.fillMaxSize(), index = page) }
                Row(
                    Modifier.padding(start = px(16), top = px(16)).clip(RoundedCornerShape(50)).background(MockColors.Lime2).padding(horizontal = px(22), vertical = px(8)),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    MockText("♛", 24); Box(Modifier.width(px(10))); MockText("오늘의 추천 맛집", 23, weight = FontWeight.ExtraBold)
                }
                Box(
                    Modifier.align(Alignment.TopEnd).padding(top = px(14), end = px(14)).size(px(56)).clickable(role = Role.Button) { vm.toggleBookmark(store.id) }.semantics { contentDescription = "저장하기" },
                    contentAlignment = Alignment.Center,
                ) { BookmarkIcon(40, filled = card.bookmarked) }
                if (photoCount > 1) Box(Modifier.align(Alignment.BottomEnd).padding(end = px(20), bottom = px(16)).clip(RoundedCornerShape(50)).background(Color(0x99000000)).padding(horizontal = px(16), vertical = px(4))) {
                    MockText("${pager.currentPage + 1}/$photoCount", 19, color = Color.White)
                }
            }
            // 가게 정보 카드
            Box(Modifier.at(22, 915).size(px(716), px(250)).clip(RoundedCornerShape(bottomStart = px(30), bottomEnd = px(30))).background(Color(0xFFFCFAF4)))
            MockText(store.name, 40, Modifier.at(50, 935).width(px(520)), font = MockSerif, weight = FontWeight.ExtraBold)
            MockText(card.oneLiner, 22, Modifier.at(50, 996).width(px(560)), color = Color(0xFF4B5563))
            Row(Modifier.at(50, 1043), horizontalArrangement = Arrangement.spacedBy(px(12))) {
                listOfNotNull(RoulettePicker.cuisineOf(store), store.foundedYear?.let { "${it / 10 * 10}년대" }, store.sigungu.takeIf { it.isNotBlank() }, "노포").forEach { t ->
                    Box(Modifier.clip(RoundedCornerShape(50)).background(Color(0xFFEFEBDD)).padding(horizontal = px(20), vertical = px(7))) { MockText(t, 21, color = Color(0xFF4B5563)) }
                }
            }
            Row(Modifier.at(50, 1105), verticalAlignment = Alignment.CenterVertically) {
                RatingText(card.rating?.average, card.rating?.count, 27, showCount = true)
                card.distanceLabel?.let {
                    if (card.rating != null) Box(Modifier.padding(horizontal = px(22)).width(1.dp).height(px(34)).background(Color(0xFFDDD8C6)))
                    PinIcon(26); Box(Modifier.width(px(8))); MockText(it, 26, weight = FontWeight.Medium)
                }
            }
            Box(
                Modifier.at(655, 935).size(px(62)).clip(CircleShape).background(Color.White).border(1.dp, Color(0xFFE2DDCB), CircleShape)
                    .clickable(role = Role.Button) { vm.toggleBookmark(store.id) }.semantics { contentDescription = "저장하기" },
                contentAlignment = Alignment.Center,
            ) { BookmarkIcon(32, color = MockColors.Navy, filled = card.bookmarked) }
            // 추천 이유
            Box(Modifier.at(22, 1178).size(px(716), px(172)).clip(RoundedCornerShape(px(26))).background(Color(0xFFF7F1E1)))
            MockPic(R.drawable.mk2_r6_char, 152, 183, Modifier.at(590, 1167))
            MockText("✦ AI가 추천한 이유", 27, Modifier.at(48, 1190), weight = FontWeight.ExtraBold)
            MockText(state.reason, 20, Modifier.at(48, 1236).width(px(520)), color = MockColors.Navy, maxLines = 4, lineHeightPx = 28)
            state.notice?.let { MockText(it, 18, Modifier.at(48, 1352).width(px(660)), color = MockColors.Stamp, maxLines = 1) }
            // 버튼
            Box(Modifier.at(30, 1372).size(px(702), px(82)).clip(RoundedCornerShape(50)).background(MockColors.Lime2).clickable(role = Role.Button) { onOpenStore(store.id) }, contentAlignment = Alignment.Center) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PinIcon(30, color = MockColors.Navy); Box(Modifier.width(px(14))); MockText("가보러 가기", 30, weight = FontWeight.ExtraBold); Box(Modifier.width(px(12))); ChevronRight(26)
                }
            }
            Box(
                Modifier.at(30, 1470).size(px(346), px(76)).clip(RoundedCornerShape(50)).border(1.dp, Color(0xFFE2DDCB), RoundedCornerShape(50)).background(Color(0xFFFCFAF4)).clickable(role = Role.Button) { vm.spin() },
                contentAlignment = Alignment.Center,
            ) { MockText("↻  다시 돌리기", 26, weight = FontWeight.ExtraBold) }
            Box(
                Modifier.at(386, 1470).size(px(346), px(76)).clip(RoundedCornerShape(50)).border(1.dp, Color(0xFFE2DDCB), RoundedCornerShape(50)).background(Color(0xFFFCFAF4)).clickable(role = Role.Button) {
                    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://map.naver.com/p/search/" + Uri.encode(store.name))).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                },
                contentAlignment = Alignment.Center,
            ) { MockText("지도에서 보기", 26, weight = FontWeight.ExtraBold) }
        }
    }
}
