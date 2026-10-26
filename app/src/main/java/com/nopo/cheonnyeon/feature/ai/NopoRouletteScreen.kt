package com.nopo.cheonnyeon.feature.ai

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.SystemClock
import androidx.activity.compose.BackHandler
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
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
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
import com.nopo.cheonnyeon.core.designsystem.mock.MockColors
import com.nopo.cheonnyeon.core.designsystem.mock.MockPic
import com.nopo.cheonnyeon.core.designsystem.mock.MockSerif
import com.nopo.cheonnyeon.core.designsystem.mock.MockStage
import com.nopo.cheonnyeon.core.designsystem.mock.MockText
import com.nopo.cheonnyeon.core.designsystem.mock.PinIcon
import com.nopo.cheonnyeon.core.designsystem.mock.RatingText
import com.nopo.cheonnyeon.core.designsystem.mock.at
import com.nopo.cheonnyeon.core.designsystem.mock.px
import com.nopo.cheonnyeon.core.util.GeoDistance
import com.nopo.cheonnyeon.data.ai.ChatContextInfo
import com.nopo.cheonnyeon.domain.model.Store
import com.nopo.cheonnyeon.domain.rule.ChatCandidate
import com.nopo.cheonnyeon.domain.rule.RouletteFilter
import com.nopo.cheonnyeon.domain.rule.RoulettePicker
import com.nopo.cheonnyeon.domain.rule.TasteFood
import com.nopo.cheonnyeon.domain.rule.TasteRecommender
import com.nopo.cheonnyeon.domain.rule.TasteRegion
import com.nopo.cheonnyeon.domain.rule.YearsCalculator
import com.nopo.cheonnyeon.feature.common.StoreCardData
import com.nopo.cheonnyeon.feature.common.StoreCards
import com.nopo.cheonnyeon.feature.main.MockNavBar
import com.nopo.cheonnyeon.feature.store.StorePhoto
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.time.LocalDate

enum class RoulettePhase { IDLE, SPINNING, RESULT }

data class NopoRouletteState(
    val filter: RouletteFilter = RouletteFilter(),
    val price: String = "상관없음",
    val regions: List<String> = emptyList(),
    val phase: RoulettePhase = RoulettePhase.IDLE,
    val pick: StoreCardData? = null,
    /** 이번에 나온 노포 3곳(첫 번째가 오늘의 추천). 지금까지 나온 곳은 최대 30곳까지 겹치지 않는다 */
    val picks: List<StoreCardData> = emptyList(),
    val shownIds: List<String> = emptyList(),
    val reason: String = "",
    val segment: String = "",
    val notice: String? = null,
    val reel: List<Any> = emptyList(),
    val consent: Boolean = false,
    val last: StoreCardData? = null,
)

/** 한 번 돌릴 때 보여 주는 노포 수 / 겹치지 않게 추천하는 최대 개수 */
private const val PER_SPIN = 3
private const val MAX_SHOWN = 30

/** 앞쪽 몇 자리를 처음에 고른 취향에 맞는 가게로 먼저 채운다 */
private const val LIKED_SLOTS = 2

/** 첫 응답을 5초 안에 주려고 AI 는 이 시간까지만 기다리고, 늦으면 앱이 정한 이유를 쓴다 */
private const val AI_WAIT_MS = 4_000L

/** 룰렛 화면을 열 때 AI 연결을 미리 데우는 짧은 요청의 제한 시간 */
private const val WARM_UP_TIMEOUT_MS = 8_000L

/** 사진이 돌아가는 연출의 최소 시간 */
private const val MIN_SPIN_MS = 3_500L

/** AI 후보: 사진이 있는 곳 위주로 RANK_POOL 곳을 추리고, 그중 취향에 맞는 최대 LIKED_CANDIDATES 곳을 먼저 넣어 모두 ASK_CANDIDATES 곳으로 만든다 */
private const val RANK_POOL = 60
private const val ASK_CANDIDATES = 10
private const val LIKED_CANDIDATES = 6

/** 거리·음식 조건을 풀 때 보여 주는 안내 문구 */
private const val NOTICE_NO_NEARBY = "가까운 곳에 없어서 거리 조건을 풀고 골랐어요."
private const val NOTICE_NO_CUISINE = "조건에 맞는 노포가 없어서 음식 종류를 풀고 골랐어요."
private const val NOTICE_NO_MATCH = "조건을 모두 풀고 골랐어요."
private const val NOTICE_SEEN_ALL = "지금까지 나온 곳을 모두 보셔서 처음부터 다시 골랐어요."
private const val NOTICE_NOTHING = "고를 노포가 없어요."
private const val NOTICE_FAILED = "룰렛을 돌리지 못했어요. 다시 시도해 주세요."

/** 시안 "AI 노포 룰렛": 조건(지역·가격대·음식 종류·도보 거리)을 고르고 돌리면 사진이 돌아가다가 오늘의 노포가 정해진다. */
class NopoRouletteViewModel(private val container: AppContainer) : ViewModel() {
    private val _state = MutableStateFlow(NopoRouletteState())
    val state: StateFlow<NopoRouletteState> = _state.asStateFlow()
    private val legacy = MenuRouletteViewModel(container)
    private var stores: List<Store> = emptyList()
    private val year = LocalDate.now(SeoulZone).year

    /** 처음 설치 때 고른 취향(지역·음식). 룰렛과 AI 추천에서 우선 반영한다 */
    private var prefRegions: List<TasteRegion> = emptyList()
    private var prefFoods: List<TasteFood> = emptyList()

    private fun prefMatch(s: Store) =
        prefRegions.any { TasteRecommender.inRegion(s, it) } || prefFoods.any { TasteRecommender.matchesFood(s, it) }

    private fun yearsOf(s: Store) = YearsCalculator.yearsAtVisit(s.foundedYear?.takeIf { it <= year }, year)

    init {
        viewModelScope.launch {
            stores = container.catalog.all().filter { !it.isTest && it.isFood }
            val regionKeys = container.settings.prefRegions.first()
            val foodKeys = container.settings.prefFoods.first()
            prefRegions = TasteRecommender.REGIONS.filter { it.key in regionKeys }
            prefFoods = TasteRecommender.FOODS.filter { it.key in foodKeys }
            val sidos = stores.map { it.sido }.filter { it.isNotBlank() }.distinct().sorted()
            _state.update { it.copy(regions = sidos, consent = container.settings.aiConsent.first()) }
        }
        viewModelScope.launch {
            if (container.ai.available && container.settings.aiConsent.first()) {
                runCatching { withTimeoutOrNull(WARM_UP_TIMEOUT_MS) { container.ai.warmUp() } }
            }
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
            try {
                runSpin()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // 어떤 이유로든 실패하면 돌리는 중 상태에 갇히지 않게 처음 화면으로 돌아간다
                _state.update { it.copy(phase = RoulettePhase.IDLE, notice = NOTICE_FAILED) }
            }
        }
    }

    /** 후보를 모으고(조건을 차례로 풀며), 3곳과 이유를 고른 뒤, 연출 시간을 채우고 결과를 낸다. */
    private suspend fun runSpin() {
        val started = SystemClock.elapsedRealtime()
        val filter = _state.value.filter
        val fix = container.lastFix.value
        val (list, relaxNotice) = candidatesRelaxing(filter, fix?.let { it.lat to it.lng })

        // 예전처럼 30곳까지는 겹치지 않게 추천한다. 남은 곳이 3곳보다 적으면 처음부터 다시
        var shown = _state.value.shownIds
        var fresh = list.filter { it.id !in shown }
        var notice = relaxNotice
        if (fresh.size < PER_SPIN || shown.size >= MAX_SHOWN) {
            if (shown.isNotEmpty()) notice = (notice?.plus(" ") ?: "") + NOTICE_SEEN_ALL
            shown = emptyList()
            fresh = list
        }

        val (chosen, aiReason) = choosePicks(fresh, filter)
        if (chosen.isEmpty()) {
            _state.update { it.copy(phase = RoulettePhase.IDLE, notice = NOTICE_NOTHING) }
            return
        }
        val picked = chosen.take(PER_SPIN)
        val reason = aiReason.ifBlank { fallbackReason(picked[0], filter) }
        val wait = MIN_SPIN_MS - (SystemClock.elapsedRealtime() - started)
        if (wait > 0) delay(wait)
        val bookmarks = container.settings.bookmarks.first()
        val cards = picked.map { StoreCards.of(it, year, container.ratings.value, fix, bookmarks) }
        _state.update {
            it.copy(
                phase = RoulettePhase.RESULT, pick = cards[0], picks = cards, reason = reason, segment = RoulettePicker.wheelSegment(picked[0]),
                notice = notice, last = cards[0], shownIds = shown + picked.map { c -> c.id },
            )
        }
    }

    /** 조건에 맞는 후보. 없으면 거리 → 음식 종류 → 모든 조건 순으로 풀고, 푼 것을 안내 문구로 돌려준다. */
    private fun candidatesRelaxing(filter: RouletteFilter, here: Pair<Double, Double>?): Pair<List<Store>, String?> {
        var list = RoulettePicker.candidates(stores, filter, here)
        var notice: String? = null
        if (list.isEmpty() && filter.walkMeters != null) {
            list = RoulettePicker.candidates(stores, filter.copy(walkMeters = null), here)
            notice = NOTICE_NO_NEARBY
        }
        if (list.isEmpty() && filter.cuisine != null) {
            list = RoulettePicker.candidates(stores, filter.copy(cuisine = null, walkMeters = null), here)
            notice = NOTICE_NO_CUISINE
        }
        if (list.isEmpty()) {
            list = RoulettePicker.candidates(stores, RouletteFilter(), here)
            notice = NOTICE_NO_MATCH
        }
        return list to notice
    }

    /**
     * [fresh] 에서 PER_SPIN 곳을 고른다. 먼저 AI 의 추천(첫 번째 이유가 오늘의 추천 이유)을 받고,
     * 모자라면 평점·사진 가중 무작위로 채운다. 앞쪽 LIKED_SLOTS 자리는 처음에 고른 취향에 맞는 가게를 먼저 쓴다.
     * 돌려주는 이유는 AI 가 준 것이며, AI 를 못 썼으면 빈 문자열이다.
     */
    private suspend fun choosePicks(fresh: List<Store>, filter: RouletteFilter): Pair<List<Store>, String> {
        val chosen = ArrayList<Store>()
        var reason = ""
        if (container.ai.available && _state.value.consent && fresh.isNotEmpty()) {
            val ai = withTimeoutOrNull(AI_WAIT_MS) { askAi(fresh, filter) }
            ai?.forEach { (id, why) ->
                val s = fresh.firstOrNull { it.id == id }
                if (s != null && chosen.none { c -> c.id == s.id }) {
                    chosen += s
                    if (chosen.size == 1) reason = why
                }
            }
        }
        val ratings = container.ratings.value.mapValues { it.value.average }
        var pool = fresh.filter { f -> chosen.none { it.id == f.id } }
        while (chosen.size < PER_SPIN && pool.isNotEmpty()) {
            val liked = if (chosen.size < LIKED_SLOTS) pool.filter { prefMatch(it) } else emptyList()
            val one = RoulettePicker.pick(liked.ifEmpty { pool }, ratings) ?: break
            chosen += one
            pool = pool.filter { it.id != one.id }
        }
        return chosen to reason
    }

    /** AI에게 후보(ASK_CANDIDATES 곳)에서 서로 다른 세 곳을 골라 달라고 한다. (id, 이유) 목록, 첫 번째가 오늘의 추천 */
    private suspend fun askAi(list: List<Store>, f: RouletteFilter): List<Pair<String, String>>? {
        val fix = container.lastFix.value
        val ranked = list.sortedByDescending { it.photoUrls.isNotEmpty() }.take(RANK_POOL)
        val likedPick = ranked.filter { prefMatch(it) }.shuffled().take(LIKED_CANDIDATES)
        val candidates = (likedPick + ranked.shuffled().take(ASK_CANDIDATES - likedPick.size)).distinct().map { s ->
            val km = if (fix != null && s.lat != null && s.lng != null) GeoDistance.meters(fix.lat, fix.lng, s.lat,
                s.lng) / 1000.0 else null
            ChatCandidate(s, yearsOf(s), km, false)
        }
        val info = ChatContextInfo(
            nowText = LocalDate.now(SeoulZone).toString(), locationText = if (fix != null) "사용자 위치 기준" else "위치 모름",
            totalYears = 0, candidates = candidates,
        )
        val reply = container.ai.chat(emptyList(), question(f), info)
        return (reply as? AppResult.Success)?.value?.picks
    }

    /** AI 에게 보내는 질문: 고른 조건, (있으면) 처음에 고른 취향, 요청 내용 */
    private fun question(f: RouletteFilter): String {
        val walk = f.walkMeters?.let { "${it}m 이내" } ?: "제한 없음"
        val conditions = "노포 룰렛 조건: 지역 ${f.sido ?: "전국"}, 음식 ${f.cuisine ?: "아무거나"}, 가격대 ${_state.value.price}, 도보 $walk. "
        return conditions + tasteSentence() +
            "후보 중 오늘 가볼 노포를 서로 다른 세 곳 골라 줘. 첫 번째가 가장 추천하는 곳이고, 첫 번째 이유는 두세 문장으로."
    }

    private fun tasteSentence(): String {
        if (prefRegions.isEmpty() && prefFoods.isEmpty()) return ""
        val regions = prefRegions.joinToString("·") { it.label }.ifBlank { "상관없음" }
        val foods = prefFoods.joinToString("·") { it.label }.ifBlank { "상관없음" }
        return "사용자가 처음에 고른 취향: 지역 $regions, 음식 $foods. 이 취향에 맞는 후보를 우선해 줘. "
    }

    private fun fallbackReason(s: Store, f: RouletteFilter): String {
        val menu = s.menuItems.firstOrNull { it.isNotBlank() }
        val cond = listOfNotNull(f.sido, f.cuisine).joinToString(" · ").ifBlank { "전국 · 아무거나" }
        val menuSentence = if (menu != null) " 대표 메뉴 '$menu'가 잘 알려져 있어요." else ""
        return "${yearsOf(s)}년 동안 자리를 지킨 노포예요.$menuSentence 고른 조건($cond)에 맞는 곳 중에서 룰렛이 정했어요."
    }
}

// ------------------------------------------------------------------ 화면에서 쓰는 값

private val PriceOptions = listOf("상관없음", "1만원 이하", "1~2만원대", "2~3만원대", "3만원 이상")
private val WalkOptions = listOf<Int?>(500, 1000, 2000, 3000, null)

private fun walkLabel(m: Int?): String = if (m == null) "제한 없음" else if (m < 1000) "${m}m 이내" else "${m / 1000}km 이내"

/** 평점이 있을 때만 거리 앞에 가운뎃점 구분을 붙인다 */
private fun distanceSuffix(hasRating: Boolean, label: String) = (if (hasRating) "  ·  " else "") + label

/** 조건 네 칸의 왼쪽 위치·너비(시안 px) */
private val CONDITION_X = listOf(28, 212, 404, 584)
private val CONDITION_W = listOf(180, 186, 170, 156)

/** "함께 가 볼 만한 노포" 카드의 첫 줄 위치와 줄 간격(시안 px) */
private const val OTHER_PICK_TOP = 1322
private const val OTHER_PICK_STEP = 190

private val RouletteCardBg = Color(0xFFFCFAF4)
private val RouletteCardLine = Color(0xFFE2DDCB)
private val RouletteInkGray = Color(0xFF4B5563)
private val RouletteChipBg = Color(0xFFEFEBDD)
private val RouletteDivider = Color(0xFFDDD8C6)
private val RouletteReasonBg = Color(0xFFF7F1E1)
private val RouletteHintBrown = Color(0xFF8A7F66)
private val RouletteBadgeShade = Color(0x99000000)
private val SpinningButtonBg = Color(0xFFDCE9A4)

// ------------------------------------------------------------------ 화면

/** 시안 "AI 노포 룰렛": 안내 카드·조건 고르기·룰렛 판 위 사진 릴. 결과가 나오면 [ResultView] 로 바뀐다. */
@Composable
fun NopoRouletteScreen(vm: NopoRouletteViewModel, onBack: () -> Unit, onOpenStore: (String) -> Unit, onTab: (Int) -> Unit) {
    val state by vm.state.collectAsStateWithLifecycle()
    var askConsent by rememberSaveable { mutableStateOf(false) }
    BackHandler(enabled = state.phase == RoulettePhase.RESULT) { vm.backToIdle() }
    Column(Modifier.fillMaxSize().background(MockColors.Cream)) {
        Box(Modifier.weight(1f)) {
            if (state.phase == RoulettePhase.RESULT && state.pick != null) {
                ResultView(vm, state, onClose = vm::backToIdle, onOpenStore = onOpenStore)
            } else {
                IdleView(vm, state, onBack = onBack, onSpin = { if (!state.consent) askConsent = true else vm.spin() })
            }
        }
        MockNavBar(0, onTab)
    }
    if (askConsent) {
        AiConsentDialog(onAgree = { askConsent = false; vm.agree(); vm.spin() }, onDismiss = { askConsent = false; vm.spin() })
    }
}

/** 오른쪽 위 닫기(X) 버튼. 머리글과 같은 자리에 둔다. */
@Composable
private fun CloseButton(onClick: () -> Unit) {
    Box(
        Modifier.at(652, 98).size(px(76)).clickable(role = Role.Button, onClick = onClick).semantics { contentDescription = "닫기" },
        contentAlignment = Alignment.Center,
    ) { CloseGlyph() }
}

@Composable
private fun CondChip(text: String, modifier: Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
        MockText(text, 22, weight = FontWeight.ExtraBold, maxLines = 1)
        Box(Modifier.width(px(8)))
        ChevronDown(20)
    }
}

/** 조건 네 칸(지역·가격대·음식 종류·도보 거리)과 눌렀을 때 뜨는 목록 */
@Composable
private fun ConditionRow(vm: NopoRouletteViewModel, state: NopoRouletteState, enabled: Boolean) {
    var menu by remember { mutableStateOf(0) }
    val labels = listOf(
        state.filter.sido?.let { "$it 전체" } ?: "전국",
        state.price,
        state.filter.cuisine ?: "아무거나",
        walkLabel(state.filter.walkMeters),
    )
    labels.forEachIndexed { i, label ->
        Box(Modifier.at(CONDITION_X[i], 450).size(px(CONDITION_W[i]), px(58))) {
            CondChip(label, Modifier.fillMaxSize().clickable(enabled = enabled, role = Role.Button) { menu = i + 1 })
            DropdownMenu(expanded = menu == i + 1, onDismissRequest = { menu = 0 }) {
                val close = { menu = 0 }
                when (i) {
                    0 -> {
                        DropdownMenuItem(text = { Text(stringResource(R.string.rou_02)) }, onClick = { vm.setSido(null); close() })
                        state.regions.forEach { r -> DropdownMenuItem(text = { Text(r) }, onClick = { vm.setSido(r); close() }) }
                    }
                    1 -> PriceOptions.forEach { p -> DropdownMenuItem(text = { Text(p) }, onClick = { vm.setPrice(p); close() }) }
                    2 -> {
                        DropdownMenuItem(text = { Text(stringResource(R.string.rou_03)) }, onClick = { vm.setCuisine(null); close() })
                        RoulettePicker.CUISINES.forEach { c -> DropdownMenuItem(text = { Text(c) },
                            onClick = { vm.setCuisine(c); close() }) }
                    }
                    else -> WalkOptions.forEach { w -> DropdownMenuItem(text = { Text(walkLabel(w)) },
                        onClick = { vm.setWalk(w); close() }) }
                }
            }
        }
    }
}

/** 돌리는 동안 위에 뜨는 안내 팝업 */
@Composable
private fun AnalysisNotice() {
    Row(
        Modifier.at(24, 96).size(px(712),
            px(120)).clip(RoundedCornerShape(px(24))).background(MockColors.Lime2).padding(horizontal = px(24)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircularProgressIndicator(Modifier.size(px(44)), color = MockColors.Navy, strokeWidth = 3.dp)
        Box(Modifier.width(px(18)))
        MockText(
            "작성한 리뷰나 다녀온 가게를 분석해 AI가 취향 저격 맛집을 찾고 있어요! 조금만 기다려 주세요.",
            22, color = MockColors.Navy, weight = FontWeight.Bold, maxLines = 3, lineHeightPx = 30,
        )
    }
}

/** "룰렛 돌리기" 큰 버튼 */
@Composable
private fun SpinButton(spinning: Boolean, onSpin: () -> Unit) {
    Box(
        Modifier.at(145, 1140).size(px(475), px(92)).clip(RoundedCornerShape(50))
            .background(if (spinning) SpinningButtonBg else MockColors.Lime2)
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
}

/** 지난번에 나온 노포 카드 */
@Composable
private fun LastPickCard(last: StoreCardData) {
    MockPic(R.drawable.mk2_r_chip_tab, 327, 47, Modifier.at(45, 1262))
    Box(Modifier.at(20, 1315).size(px(720), px(230)).clip(RoundedCornerShape(px(26))).background(RouletteCardBg).border(1.dp,
        RouletteCardLine, RoundedCornerShape(px(26)))) {
        Box(Modifier.padding(px(16)).size(px(250), px(198)).clip(RoundedCornerShape(px(18)))) { StorePhoto(last.store,
            Modifier.fillMaxSize()) }
        MockText(last.store.name, 31, Modifier.padding(start = px(290), top = px(22)).width(px(360)), font = MockSerif,
            weight = FontWeight.ExtraBold)
        MockText(last.oneLiner, 21, Modifier.padding(start = px(290), top = px(74)).width(px(420)), color = RouletteInkGray, maxLines = 2,
            lineHeightPx = 28)
        Row(Modifier.padding(start = px(290), top = px(150))) {
            RatingText(last.rating?.average, last.rating?.count, 21)
            last.distanceLabel?.let { MockText(distanceSuffix(last.rating != null, it), 21, color = RouletteInkGray) }
        }
    }
}

@Composable
private fun IdleView(vm: NopoRouletteViewModel, state: NopoRouletteState, onBack: () -> Unit, onSpin: () -> Unit) {
    val spinning = state.phase == RoulettePhase.SPINNING
    Column(Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState())) {
        MockStage(originY = 84, heightPx = 1580) {
            MockPic(R.drawable.mk2_v_hdr, 460, 117, Modifier.at(20, 88))
            CloseButton(onBack)
            if (spinning) AnalysisNotice()
            MockPic(R.drawable.mk2_r_title, 480, 155, Modifier.at(20, 225))
            MockPic(R.drawable.mk2_r_note, 245, 150, Modifier.at(500, 250))
            MockPic(R.drawable.mk2_r_filter, 720, 117, Modifier.at(20, 395))
            MockText(stringResource(R.string.rou_01), 21, Modifier.at(448, 418), color = MockColors.Navy, weight = FontWeight.Medium)
            ConditionRow(vm, state, enabled = !spinning)
            // 룰렛판(코드로 그린 완전한 원). 돌리는 동안은 판 전체를 덮는 사진 릴이 돌아가서 판이 보이지 않는다
            RouletteWheel(Modifier.at(95, 514).size(px(610), px(600)))
            if (spinning) {
                Box(Modifier.at(60, 516).size(px(640), px(610)).clip(RoundedCornerShape(px(36))).background(MockColors.Navy)) {
                    if (state.reel.isNotEmpty()) {
                        PhotoReel(state.reel, true, Modifier.fillMaxSize(), rows = 3, frame = false)
                    } else {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            MockText(stringResource(R.string.rou_04), 26, color = Color.White)
                        }
                    }
                }
            }
            SpinButton(spinning, onSpin)
            state.notice?.let { MockText(it, 20, Modifier.at(40, 1240).width(px(680)), color = MockColors.Stamp, align = TextAlign.Center) }
            state.last?.let { LastPickCard(it) }
        }
    }
}

@Composable
private fun CloseGlyph() {
    Canvas(Modifier.size(px(38))) {
        val sw = size.width * 0.1f
        drawLine(MockColors.Navy, Offset(size.width * 0.1f, size.height * 0.1f), Offset(size.width * 0.9f, size.height * 0.9f), sw,
            StrokeCap.Round)
        drawLine(MockColors.Navy, Offset(size.width * 0.9f, size.height * 0.1f), Offset(size.width * 0.1f, size.height * 0.9f), sw,
            StrokeCap.Round)
    }
}

@Composable
private fun ChevronLeftGlyph() {
    Canvas(Modifier.size(px(24), px(34))) {
        val p = Path().apply {
            moveTo(size.width * 0.9f, size.height * 0.1f); lineTo(size.width * 0.15f, size.height * 0.5f); lineTo(size.width * 0.9f,
                size.height * 0.9f)
        }
        drawPath(p, MockColors.Navy, style = Stroke(size.width * 0.14f, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

// ------------------------------------------------------------------ 결과 화면

@Composable
private fun ResultView(vm: NopoRouletteViewModel, state: NopoRouletteState, onClose: () -> Unit, onOpenStore: (String) -> Unit) {
    val card = state.pick ?: return
    val store = card.store
    val context = LocalContext.current
    Column(Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState())) {
        MockStage(originY = 84, heightPx = 2300) {
            MockPic(R.drawable.mk2_v_hdr, 460, 117, Modifier.at(20, 88))
            CloseButton(onClose)
            ReasonCard(state.reason)
            // 원래 있던 "오늘의 추천이 정해졌어요!" 부분은 그대로 두고 이유 카드 밑에 놓는다
            Box(Modifier.at(0, 455).size(px(760), px(250)).clip(RectangleShape)) { MockPic(R.drawable.mk2_r6_title, 760, 285) }
            PickPhotoCard(card) { vm.toggleBookmark(store.id) }
            PickInfoCard(card) { vm.toggleBookmark(store.id) }
            OtherPicks(state.picks.drop(1), onOpenStore)
            state.notice?.let { MockText(it, 18, Modifier.at(30, 1709).width(px(700)), color = MockColors.Stamp, maxLines = 2) }
            MockText("지금까지 ${state.shownIds.size}곳 / 최대 ${MAX_SHOWN}곳을 겹치지 않게 추천해요", 19, Modifier.at(30, 1744), color = RouletteHintBrown)
            Box(
                Modifier.at(30, 1794).size(px(702), px(82)).clip(RoundedCornerShape(50)).background(MockColors.Lime2)
                    .clickable(role = Role.Button) { onOpenStore(store.id) },
                contentAlignment = Alignment.Center,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PinIcon(30, color = MockColors.Navy); Box(Modifier.width(px(14))); MockText(stringResource(R.string.rou_07), 30,
                        weight = FontWeight.ExtraBold)
                }
            }
            SecondaryPill(30, stringResource(R.string.rou_08)) { vm.spin() }
            SecondaryPill(386, stringResource(R.string.rou_09)) { openNaverSearch(context, store.name) }
        }
    }
}

/** 가장 위: AI가 추천한 이유 */
@Composable
private fun ReasonCard(reason: String) {
    Box(Modifier.at(22, 215).size(px(716), px(236)).clip(RoundedCornerShape(px(26))).background(RouletteReasonBg))
    MockPic(R.drawable.mk2_r6_char, 114, 137, Modifier.at(610, 225))
    MockText(stringResource(R.string.rou_06), 29, Modifier.at(48, 233), weight = FontWeight.ExtraBold)
    MockText(reason, 22, Modifier.at(48, 283).width(px(540)), color = MockColors.Navy, maxLines = 5, lineHeightPx = 31)
}

/** 오늘의 추천 사진 카드: 사진을 넘겨 볼 수 있고, 왼쪽 위에 "오늘의 추천 맛집" 표시와 저장 버튼이 붙는다. */
@Composable
private fun PickPhotoCard(card: StoreCardData, onBookmark: () -> Unit) {
    val photoCount = card.store.photoUrls.size.coerceAtLeast(1)
    val pager = rememberPagerState { photoCount }
    Box(Modifier.at(22, 710).size(px(716), px(296)).clip(RoundedCornerShape(topStart = px(36), topEnd = px(36)))) {
        HorizontalPager(pager, Modifier.fillMaxSize()) { page -> StorePhoto(card.store, Modifier.fillMaxSize(), index = page) }
        Row(
            Modifier.padding(start = px(16),
                top = px(16)).clip(RoundedCornerShape(50)).background(MockColors.Lime2).padding(horizontal = px(22), vertical = px(8)),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MockText("♛", 24); Box(Modifier.width(px(10))); MockText(stringResource(R.string.rou_05), 23, weight = FontWeight.ExtraBold)
        }
        Box(
            Modifier.align(Alignment.TopEnd).padding(top = px(14), end = px(14)).size(px(56))
                .clickable(role = Role.Button, onClick = onBookmark).semantics { contentDescription = "저장하기" },
            contentAlignment = Alignment.Center,
        ) { BookmarkIcon(40, filled = card.bookmarked) }
        if (photoCount > 1) {
            Box(
                Modifier.align(Alignment.BottomEnd).padding(end = px(20), bottom = px(16)).clip(RoundedCornerShape(50))
                    .background(RouletteBadgeShade).padding(horizontal = px(16), vertical = px(4)),
            ) { MockText("${pager.currentPage + 1}/$photoCount", 19, color = Color.White) }
        }
    }
}

/** 가게 정보 카드: 이름·한 줄 소개·태그·평점과 거리, 오른쪽 저장 버튼 */
@Composable
private fun PickInfoCard(card: StoreCardData, onBookmark: () -> Unit) {
    val store = card.store
    Box(Modifier.at(22, 997).size(px(716), px(250)).clip(RoundedCornerShape(bottomStart = px(30),
        bottomEnd = px(30))).background(RouletteCardBg))
    MockText(store.name, 40, Modifier.at(50, 1017).width(px(520)), font = MockSerif, weight = FontWeight.ExtraBold)
    MockText(card.oneLiner, 22, Modifier.at(50, 1078).width(px(560)), color = RouletteInkGray)
    Row(Modifier.at(50, 1125), horizontalArrangement = Arrangement.spacedBy(px(12))) {
        val tags = listOfNotNull(RoulettePicker.cuisineOf(store), store.foundedYear?.let { "${it / 10 * 10}년대" },
            store.sigungu.takeIf { it.isNotBlank() }, "노포")
        tags.forEach { t ->
            Box(Modifier.clip(RoundedCornerShape(50)).background(RouletteChipBg).padding(horizontal = px(20), vertical = px(7))) {
                MockText(t, 21, color = RouletteInkGray)
            }
        }
    }
    Row(Modifier.at(50, 1187), verticalAlignment = Alignment.CenterVertically) {
        RatingText(card.rating?.average, card.rating?.count, 27, showCount = true)
        card.distanceLabel?.let {
            if (card.rating != null) Box(Modifier.padding(horizontal = px(22)).width(1.dp).height(px(34)).background(RouletteDivider))
            PinIcon(26); Box(Modifier.width(px(8))); MockText(it, 26, weight = FontWeight.Medium)
        }
    }
    Box(
        Modifier.at(655, 1017).size(px(62)).clip(CircleShape).background(Color.White).border(1.dp, RouletteCardLine, CircleShape)
            .clickable(role = Role.Button, onClick = onBookmark).semantics { contentDescription = "저장하기" },
        contentAlignment = Alignment.Center,
    ) { BookmarkIcon(32, color = MockColors.Navy, filled = card.bookmarked) }
}

/** 추천 밑으로 함께 가 볼 만한 노포(추천 포함 모두 3곳) */
@Composable
private fun OtherPicks(others: List<StoreCardData>, onOpenStore: (String) -> Unit) {
    MockText("함께 가 볼 만한 노포 ${others.size}곳", 26, Modifier.at(30, 1274), weight = FontWeight.ExtraBold)
    others.forEachIndexed { i, o ->
        Box(
            Modifier.at(22, OTHER_PICK_TOP + i * OTHER_PICK_STEP).size(px(716), px(172)).clip(RoundedCornerShape(px(24)))
                .background(RouletteCardBg).border(1.dp, RouletteCardLine, RoundedCornerShape(px(24)))
                .clickable(role = Role.Button) { onOpenStore(o.store.id) },
        ) {
            Box(Modifier.padding(px(12)).size(px(200), px(148)).clip(RoundedCornerShape(px(16)))) { StorePhoto(o.store,
                Modifier.fillMaxSize()) }
            MockText(o.store.name, 30, Modifier.padding(start = px(236), top = px(16)).width(px(440)), font = MockSerif,
                weight = FontWeight.ExtraBold, maxLines = 1)
            MockText(o.oneLiner, 20, Modifier.padding(start = px(236), top = px(62)).width(px(450)), color = RouletteInkGray, maxLines = 2,
                lineHeightPx = 27)
            Row(Modifier.padding(start = px(236), top = px(122)), verticalAlignment = Alignment.CenterVertically) {
                RatingText(o.rating?.average, o.rating?.count, 21)
                o.distanceLabel?.let { MockText(distanceSuffix(o.rating != null, it), 21, color = RouletteInkGray) }
            }
        }
    }
}

/** 아래쪽 흰 알약 버튼("다시 돌리기", "지도에서 보기") */
@Composable
private fun SecondaryPill(x: Int, label: String, onClick: () -> Unit) {
    Box(
        Modifier.at(x, 1894).size(px(346), px(76)).clip(RoundedCornerShape(50)).border(1.dp, RouletteCardLine, RoundedCornerShape(50))
            .background(RouletteCardBg).clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { MockText(label, 26, weight = FontWeight.ExtraBold) }
}

/** 네이버 지도에서 가게 이름으로 검색한다. 열 앱이 없어도 오류 없이 넘어간다. */
private fun openNaverSearch(context: Context, name: String) {
    val uri = Uri.parse("https://map.naver.com/p/search/" + Uri.encode(name))
    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
}
