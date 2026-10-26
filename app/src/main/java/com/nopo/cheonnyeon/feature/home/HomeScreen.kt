package com.nopo.cheonnyeon.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.nopo.cheonnyeon.R
import com.nopo.cheonnyeon.app.AppContainer
import com.nopo.cheonnyeon.core.common.AppResult
import com.nopo.cheonnyeon.core.common.SeoulZone
import com.nopo.cheonnyeon.core.designsystem.mock.GrayPill
import com.nopo.cheonnyeon.core.designsystem.mock.HeartIcon
import com.nopo.cheonnyeon.core.designsystem.mock.LimeArrowButton
import com.nopo.cheonnyeon.core.designsystem.mock.LimePill
import com.nopo.cheonnyeon.core.designsystem.mock.LocalMockU
import com.nopo.cheonnyeon.core.designsystem.mock.MockColors
import com.nopo.cheonnyeon.core.designsystem.mock.MockPic
import com.nopo.cheonnyeon.core.designsystem.mock.MockSerif
import com.nopo.cheonnyeon.core.designsystem.mock.MockStage
import com.nopo.cheonnyeon.core.designsystem.mock.MockText
import com.nopo.cheonnyeon.core.designsystem.mock.PhotoCircleButton
import com.nopo.cheonnyeon.core.designsystem.mock.PinIcon
import com.nopo.cheonnyeon.core.designsystem.mock.RatingText
import com.nopo.cheonnyeon.core.designsystem.mock.at
import com.nopo.cheonnyeon.core.designsystem.mock.mockCardBox
import com.nopo.cheonnyeon.core.designsystem.mock.px
import com.nopo.cheonnyeon.core.util.GeoDistance
import com.nopo.cheonnyeon.data.ai.ChatContextInfo
import com.nopo.cheonnyeon.data.remote.StoreRating
import com.nopo.cheonnyeon.domain.model.Store
import com.nopo.cheonnyeon.domain.rule.ChatCandidates
import com.nopo.cheonnyeon.domain.rule.TasteRecommender
import com.nopo.cheonnyeon.feature.common.StoreCardData
import com.nopo.cheonnyeon.feature.common.StoreCards
import com.nopo.cheonnyeon.feature.store.StorePhoto
import com.nopo.cheonnyeon.platform.location.Fix
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class HomeUiState(
    val pick: StoreCardData? = null,
    val pickReason: String? = null,
    val hot: List<StoreCardData> = emptyList(),
    val nearLabel: String = "",
)

class HomeViewModel(private val container: AppContainer) : ViewModel() {
    private val stores = MutableStateFlow<List<Store>>(emptyList())
    private val aiPick = MutableStateFlow<Pair<String, String>?>(null)
    private val year = LocalDate.now(SeoulZone).year

    private val taste = combine(container.settings.prefRegions, container.settings.prefFoods) { r, f -> r to f }

    val state: StateFlow<HomeUiState> = combine(combine(stores, aiPick, container.ratings, container.lastFix,
        container.settings.bookmarks) { a, b, c, d, e -> listOf(a, b, c, d, e) }, taste) { arr, tp ->
        @Suppress("UNCHECKED_CAST")
        val all = arr[0] as List<Store>
        @Suppress("UNCHECKED_CAST")
        val ai = arr[1] as Pair<String, String>?
        @Suppress("UNCHECKED_CAST")
        val ratings = arr[2] as Map<String, StoreRating>
        val fix = arr[3] as Fix?
        @Suppress("UNCHECKED_CAST")
        val saved = arr[4] as Set<String>
        if (all.isEmpty()) return@combine HomeUiState()
        val today = LocalDate.now(SeoulZone).toEpochDay()
        val withPhoto = all.filter { it.photoUrls.isNotEmpty() }
        val rated = withPhoto.filter { ratings[it.id] != null }
        val fallback = (if (fix != null && rated.isNotEmpty()) rated.filter { it.lat != null && it.lng != null }
            .minByOrNull { GeoDistance.meters(fix.lat, fix.lng, it.lat!!, it.lng!!) } else null)
            ?: (rated.ifEmpty { withPhoto }).let { list -> list.getOrNull((today % list.size.coerceAtLeast(1)).toInt()) }
        // 처음 설치 때 고른 취향이 있으면 AI 결과가 없을 때 그 취향에 맞는 노포를 먼저 보여 준다
        val tasteStore = if (tp.first.isNotEmpty() || tp.second.isNotEmpty()) TasteRecommender.recommend(all,
            tp.first, tp.second, ratings.mapValues { it.value.average }) else null
        val pickStore = ai?.let { (id, _) -> all.firstOrNull { it.id == id } } ?: tasteStore ?: fallback
        val hotIds = ratings.entries.filter { it.value.count >= 8 }
            .sortedWith(compareByDescending<Map.Entry<String,
            StoreRating>> { it.value.average }.thenByDescending { it.value.count }).map { it.key }
        val byId = all.associateBy { it.id }
        val hot = (hotIds.mapNotNull { byId[it] }.filter { it.photoUrls.isNotEmpty() && it.id != pickStore?.id }.take(10)
            .ifEmpty { withPhoto.sortedByDescending { it.foundedYear?.let { y -> year - y } ?: 0 }.take(10) })
            .map { StoreCards.of(it, year, ratings, fix, saved) }
        val near = fix?.let { f ->
            all.filter { it.lat != null && it.lng != null }
                .minByOrNull { GeoDistance.meters(f.lat, f.lng, it.lat!!, it.lng!!) }
        }
        HomeUiState(
            pick = pickStore?.let { StoreCards.of(it, year, ratings, fix, saved) },
            pickReason = ai?.second,
            hot = hot,
            nearLabel = near?.let { listOf(it.sido, it.sigungu).filter { s -> s.isNotBlank() }.joinToString(" ") + " 주변" } ?: "",
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, HomeUiState())

    init {
        viewModelScope.launch { stores.value = container.catalog.all().filter { !it.isTest && it.isFood } }
        viewModelScope.launch { askAi() }
    }

    fun toggleBookmark(id: String) { viewModelScope.launch { container.settings.toggleBookmark(id) } }

    /** 오늘의 AI 추천: 하루에 한 번 AI에게 후보 중 한 곳을 골라 달라고 한다. 실패하면 날짜로 고른 가게를 쓴다. */
    private suspend fun askAi() {
        val today = LocalDate.now(SeoulZone)
        cache?.takeIf { it.first == today }?.let { aiPick.value = it.second; return }
        val all = container.catalog.all().filter { !it.isTest && it.isFood }
        if (all.isEmpty()) return
        val visits = container.database.myVisitDao().observeAll().first()
        val fix = container.lastFix.value
        val candidates = ChatCandidates.pick(all, visits.map { it.storeId }.toSet(), fix?.let { it.lat to it.lng }, "", today.year)
            .filter { it.store.photoUrls.isNotEmpty() }.take(30)
        if (candidates.isEmpty()) return
        val info = ChatContextInfo(
            nowText = today.toString(), locationText = fix?.let { "사용자 위치 기준 가까운 순" } ?: "위치 모름", totalYears = visits
                .sumOf { it.yearsAwarded }, candidates = candidates,
        )
        val reply = container.ai.chat(emptyList(), "오늘 가볼 만한 노포를 딱 한 곳만 골라 줘. 이유는 한 문장으로.", info)
        if (reply is AppResult.Success) {
            reply.value.picks.firstOrNull()?.let { pair ->
                cache = today to pair
                aiPick.value = pair
            }
        }
    }

    private companion object {
        var cache: Pair<LocalDate, Pair<String, String>>? = null
    }
}

/**
 * 시안 "홈"(3.webp) 복제. 위쪽 제목·히어로 그림, 아래쪽 칩 줄·배너는 시안에서 잘라 쓰고
 * "오늘의 AI 추천" 카드와 가게 카드는 실제 가게로 채운다.
 */
@Composable
fun HomeScreen(vm: HomeViewModel, onOpenStore: (String) -> Unit, onSearch: () -> Unit, onOpenMy: () -> Unit,
    onOpenChat: (String) -> Unit, onPickChip: (Int) -> Unit = {}, onBell: () -> Unit = {}) {
    val ui by vm.state.collectAsStateWithLifecycle()
    val tourPrompt = stringResource(R.string.home_tour_prompt)
    Column(Modifier.fillMaxSize().background(MockColors.Cream).statusBarsPadding().verticalScroll(rememberScrollState())) {
        // 시안 3번 폰은 화면 폭이 738px(왼쪽 12px은 테두리)라서 가로로 3% 늘려 화면에 꼭 맞춘다
        val u = LocalMockU.current
        val density = LocalDensity.current.density
        MockStage(originY = 92, heightPx = 1420, modifier = Modifier.graphicsLayer {
            transformOrigin = TransformOrigin(0f, 0f)
            scaleX = 760f / 734f
            translationX = -14f * (760f / 734f) * u * density
        }) {
            MockPic(R.drawable.mk_home_header, 760, 116, Modifier.at(0, 90))
            Box(Modifier.at(575, 92).size(px(80), px(70)).clickable(role = Role.Button, onClick = onSearch))
            MockPic(R.drawable.mk_ic_bell, 54, 56, Modifier.at(672, 100), onClick = onBell)
            MockPic(R.drawable.mk_home_hero, 760, 373, Modifier.at(0, 205))

            // 오늘의 AI 추천 카드
            val pick = ui.pick
            Box(Modifier.at(10, 582).size(px(728), px(220)).mockCardBox(26))
            if (pick != null) {
                Box(Modifier.at(42, 598).size(px(315), px(194)).clip(RoundedCornerShape(px(18)))
                    .clickable(role = Role.Button) { onOpenStore(pick.store.id) }) {
                    StorePhoto(pick.store, Modifier.fillMaxSize())
                }
                Box(Modifier.at(380, 604).width(px(270)).clickable(role = Role.Button) { onOpenStore(pick.store.id) }) {
                    MockText(pick.store.name, 31, font = MockSerif, weight = FontWeight.ExtraBold)
                }
                Box(Modifier.at(662, 607).size(px(48))) { LimeArrowButton(48) { onOpenStore(pick.store.id) } }
                val line = ui.pickReason ?: pick.oneLiner
                Box(Modifier.at(381, 654).width(px(330))) { MockText(line, 21, color = MockColors.Hint, maxLines = 1) }
                Row(Modifier.at(380, 700), horizontalArrangement = Arrangement.spacedBy(px(14))) {
                    StoreCards.mainMenu(pick.store)?.let { GrayPill(it.take(6), 20, padX = 22) }
                    pick.store.foundedYear?.let { GrayPill("${it / 10 * 10}년대", 20, padX = 22) }
                    pick.distanceLabel?.let { GrayPill("${pick.store.sigungu} $it", 20, padX = 22) }
                }
                Row(Modifier.at(382, 760), verticalAlignment = Alignment.CenterVertically) {
                    RatingText(pick.rating?.average, pick.rating?.count, 24, showCount = true)
                    if (pick.rating != null && pick.distanceLabel != null) {
                        Box(Modifier.padding(horizontal = px(16)).size(px(2), px(26)).background(MockColors.Line))
                    }
                    pick.distanceLabel?.let {
                        PinIcon(26)
                        Box(Modifier.width(px(8)))
                        MockText(it, 24, weight = FontWeight.Medium)
                    }
                }
            }
            Box(Modifier.at(38, 566)) { LimePill(stringResource(R.string.home_01),
                21, padX = 22, padY = 9) }

            // 지금 가장 핫한 노포
            MockPic(R.drawable.mk_home_title_hot, 276, 50, Modifier.at(24, 850))
            MockPic(R.drawable.mk_home_more, 110, 45, Modifier.at(630, 855), onClick = onSearch)
            LazyRow(
                Modifier.at(0, 906).fillMaxSize().height(px(310)),
                contentPadding = PaddingValues(horizontal = px(33)),
                horizontalArrangement = Arrangement.spacedBy(px(18)),
            ) {
                items(ui.hot, key = { it.store.id }) { c -> HotCard(c, onOpen = { onOpenStore(c.store.id) },
                    onHeart = { vm.toggleBookmark(c.store.id) }) }
            }

            // 내 주변 노포 찾기
            MockPic(R.drawable.mk_home_title_near, 246, 52, Modifier.at(24, 1240))
            if (ui.nearLabel.isNotBlank()) {
                Row(Modifier.at(466, 1252), verticalAlignment = Alignment.CenterVertically) {
                    PinIcon(30)
                    Box(Modifier.width(px(8)))
                    MockText(ui.nearLabel, 21, color = MockColors.Ink2, weight = FontWeight.Medium)
                }
            }
            MockPic(R.drawable.mk_home_ic_target, 45, 42, Modifier.at(690, 1252))
            MockPic(R.drawable.mk_home_chips, 725, 76, Modifier.at(20, 1298))
            // 칩 일곱 개: 전체·국밥·냉면·분식·고기·카페·기타 → 탐색 탭 칩(전체·국밥·면·고기·분식·카페)
            listOf(8 to 94, 111 to 96, 217 to 96, 324 to 88, 420 to 85, 513 to 90, 613 to 108).forEachIndexed { i, (x, w) ->
                val target = intArrayOf(0, 1, 2, 4, 3, 5, 0)[i]
                Box(Modifier.at(20 + x, 1298).size(px(w), px(76)).clickable(role = Role.Button) { onPickChip(target) }
                    .semantics { contentDescription = "내 주변 노포 칩 " + i })
            }

            // 노포 AI 투어 코스 배너
            MockPic(R.drawable.mk_home_banner, 706, 112, Modifier.at(28, 1388), onClick = { onOpenChat(tourPrompt) })
            Box(Modifier.at(113, 1405)
                .width(px(270))) { MockText(stringResource(R.string.home_02),
                23, weight = FontWeight.Bold, font = MockSerif) }
            Box(Modifier.at(113, 1443).width(px(300))) {
                MockText(stringResource(R.string.home_03), 17,
                    color = MockColors.Ink2, maxLines = 2, lineHeightPx = 25)
            }
        }
        Box(Modifier.height(px(40)))
    }
}

@Composable
private fun HotCard(c: StoreCardData, onOpen: () -> Unit, onHeart: () -> Unit) {
    Column(Modifier.width(px(233)).height(px(293)).mockCardBox(18).clickable(role = Role.Button, onClick = onOpen)) {
        Box(Modifier.width(px(233)).height(px(158))) {
            StorePhoto(c.store, Modifier.fillMaxSize())
            PhotoCircleButton(36, Modifier.align(Alignment.TopEnd).padding(px(12)), onHeart) { HeartIcon(22, filled = c.bookmarked) }
        }
        Column(Modifier.padding(horizontal = px(19), vertical = px(14)), verticalArrangement = Arrangement.spacedBy(px(10))) {
            MockText(c.store.name, 25, font = MockSerif, weight = FontWeight.ExtraBold)
            Row(horizontalArrangement = Arrangement.spacedBy(px(10))) {
                StoreCards.mainMenu(c.store)?.let { GrayPill(it.take(5), 17, padX = 14, padY = 5) }
                GrayPill("${c.years}년", 17, padX = 14, padY = 5)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                RatingText(c.rating?.average, c.rating?.count, 19, showCount = true)
                if (c.rating != null && c.distanceLabel != null) Box(Modifier.padding(horizontal = px(10))
                    .size(px(2), px(18)).background(MockColors.Line))
                c.distanceLabel?.let { PinIcon(19); Box(Modifier.width(px(5))); MockText(it, 19, weight = FontWeight.Medium) }
            }
        }
    }
}
