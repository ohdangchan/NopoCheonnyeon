package com.nopo.cheonnyeon.feature.explore

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.nopo.cheonnyeon.R
import com.nopo.cheonnyeon.app.AppContainer
import com.nopo.cheonnyeon.core.common.SeoulZone
import com.nopo.cheonnyeon.core.designsystem.mock.AiSparkIcon
import com.nopo.cheonnyeon.core.designsystem.mock.BookmarkIcon
import com.nopo.cheonnyeon.core.designsystem.mock.ChevronDown
import com.nopo.cheonnyeon.core.designsystem.mock.LimePill
import com.nopo.cheonnyeon.core.designsystem.mock.MockColors
import com.nopo.cheonnyeon.core.designsystem.mock.MockPic
import com.nopo.cheonnyeon.core.designsystem.mock.MockSans
import com.nopo.cheonnyeon.core.designsystem.mock.MockSerif
import com.nopo.cheonnyeon.core.designsystem.mock.MockStage
import com.nopo.cheonnyeon.core.designsystem.mock.MockText
import com.nopo.cheonnyeon.core.designsystem.mock.PinIcon
import com.nopo.cheonnyeon.core.designsystem.mock.RatingText
import com.nopo.cheonnyeon.core.designsystem.mock.at
import com.nopo.cheonnyeon.core.designsystem.mock.mockCardBox
import com.nopo.cheonnyeon.core.designsystem.mock.pxSp
import com.nopo.cheonnyeon.core.designsystem.mock.px
import com.nopo.cheonnyeon.domain.catalog.StoreFilter
import com.nopo.cheonnyeon.domain.catalog.StoreFilters
import com.nopo.cheonnyeon.domain.model.Store
import com.nopo.cheonnyeon.feature.common.StoreCardData
import com.nopo.cheonnyeon.feature.common.StoreCards
import com.nopo.cheonnyeon.feature.store.StorePhoto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

/** 탐색 화면 칩: 이름 대신 검색어로 쓰는 음식 종류 (대표 메뉴·이름에서 찾는다). 맨 앞이 "전체" */
val ExploreChips = listOf("전체", "국밥", "면", "고기", "분식", "카페")

class ExploreViewModel(private val container: AppContainer) : ViewModel() {
    private val stores = MutableStateFlow<List<Store>>(emptyList())
    val query = MutableStateFlow("")
    val chip = MutableStateFlow(0)
    val region = MutableStateFlow<String?>(null)
    private val year = LocalDate.now(SeoulZone).year

    val regions: StateFlow<List<String>> = stores.combine(region) { all, _ -> all.map { it.sido }.filter { it.isNotBlank() }.distinct().sorted() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val cards: StateFlow<List<StoreCardData>> = combine(stores, query, chip, region, container.ratings) { all, q, c, r, ratings ->
        listOf(all, q, c, r, ratings)
    }.combine(container.lastFix) { a, fix -> a to fix }.combine(container.settings.bookmarks) { (a, fix), saved ->
        @Suppress("UNCHECKED_CAST")
        val all = a[0] as List<Store>
        val q = a[1] as String
        val c = a[2] as Int
        val r = a[3] as String?
        @Suppress("UNCHECKED_CAST")
        val ratings = a[4] as Map<String, com.nopo.cheonnyeon.data.remote.StoreRating>
        val words = listOf(q.trim(), if (c == 0) "" else ExploreChips[c]).filter { it.isNotEmpty() }.joinToString(" ")
        val found = StoreFilters.apply(all, StoreFilter(query = words, foodOnly = true), emptySet()).filter { r == null || it.sido == r }
        found.sortedWith(
            compareByDescending<Store> { it.photoUrls.isNotEmpty() }
                .thenByDescending { ratings[it.id]?.average ?: 0.0 }
                .thenByDescending { it.foundedYear?.let { y -> year - y } ?: 0 },
        ).take(300).map { StoreCards.of(it, year, ratings, fix, saved) }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    init {
        viewModelScope.launch { stores.value = container.catalog.all().filter { !it.isTest } }
    }

    fun toggleBookmark(id: String) { viewModelScope.launch { container.settings.toggleBookmark(id) } }
}

/** 시안 "탐색"(4.webp) 복제. 검색창 오른쪽 아이콘만 AI 아이콘으로 바꿨다. */
@Composable
fun ExploreScreen(vm: ExploreViewModel, onOpenStore: (String) -> Unit, onOpenChat: (String) -> Unit, onBell: () -> Unit) {
    val cards by vm.cards.collectAsStateWithLifecycle()
    val query by vm.query.collectAsStateWithLifecycle()
    val chip by vm.chip.collectAsStateWithLifecycle()
    val region by vm.region.collectAsStateWithLifecycle()
    val regions by vm.regions.collectAsStateWithLifecycle()
    var regionMenu by remember { mutableStateOf(false) }
    val focus = androidx.compose.ui.platform.LocalFocusManager.current
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize().background(MockColors.Cream).statusBarsPadding(),
        contentPadding = PaddingValues(bottom = px(30)),
        verticalArrangement = Arrangement.spacedBy(px(13)),
    ) {
        item(span = { GridItemSpan(2) }) {
            Box {
                MockStage(originY = 84, heightPx = 426) {
                    MockPic(R.drawable.mk_deco_explore, 330, 160, Modifier.at(430, 150))
                    MockPic(R.drawable.mk_hdr_logo, 432, 122, Modifier.at(20, 84))
                    MockPic(R.drawable.mk_ic_search, 54, 52, Modifier.at(604, 90))
                    MockPic(R.drawable.mk_ic_bell, 54, 56, Modifier.at(682, 88), onClick = onBell)
                    MockPic(R.drawable.mk_explore_title, 380, 111, Modifier.at(20, 225))
                    // 검색창
                    Box(Modifier.at(25, 343).size(px(713), px(75)).clip(RoundedCornerShape(px(38))).background(MockColors.Card).border(1.dp, MockColors.CardLine, RoundedCornerShape(px(38))))
                    MockPic(R.drawable.mk_explore_ic_search, 46, 38, Modifier.at(46, 362))
                    Box(Modifier.at(105, 352).size(px(540), px(56)), contentAlignment = Alignment.CenterStart) {
                        if (query.isEmpty()) MockText("지역, 식당명, 메뉴로 검색해보세요.", 24, color = MockColors.Hint)
                        BasicTextField(
                            value = query, onValueChange = { vm.query.value = it }, singleLine = true,
                            textStyle = TextStyle(fontFamily = MockSans, fontSize = pxSp(24), color = MockColors.Navy),
                            cursorBrush = SolidColor(MockColors.Navy),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = { focus.clearFocus() }),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    Box(
                        Modifier.at(672, 352).size(px(60), px(56)).clickable(role = Role.Button) { onOpenChat(query) },
                        contentAlignment = Alignment.Center,
                    ) { AiSparkIcon(44) }
                    // 칩 줄
                    Row(Modifier.at(24, 438).fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(px(13))) {
                        ExploreChips.forEachIndexed { i, label -> Chip(label, selected = chip == i && region == null || (i == 0 && chip == 0 && region == null)) { vm.chip.value = i; vm.region.value = null } }
                        Box {
                            Row(
                                Modifier.height(px(54)).clip(RoundedCornerShape(50)).background(if (region != null) MockColors.Lime else MockColors.Chip)
                                    .clickable(role = Role.Button) { regionMenu = true }.padding(horizontal = px(24)),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                MockText(region ?: "지역", 22, weight = FontWeight.Medium, color = MockColors.Navy)
                                Box(Modifier.width(px(10)))
                                ChevronDown(20)
                            }
                            DropdownMenu(expanded = regionMenu, onDismissRequest = { regionMenu = false }) {
                                DropdownMenuItem(text = { Text("전체 지역") }, onClick = { vm.region.value = null; regionMenu = false })
                                regions.forEach { r -> DropdownMenuItem(text = { Text(r) }, onClick = { vm.region.value = r; vm.chip.value = 0; regionMenu = false }) }
                            }
                        }
                    }
                }
            }
        }
        if (cards.isEmpty()) {
            item(span = { GridItemSpan(2) }) { Box(Modifier.padding(px(40))) { MockText("조건에 맞는 노포가 없어요.", 24, color = MockColors.Hint) } }
        }
        itemsIndexed(cards, key = { _, c -> c.store.id }) { i, c ->
            val left = i % 2 == 0
            ExploreCard(
                c, onOpen = { onOpenStore(c.store.id) }, onBookmark = { vm.toggleBookmark(c.store.id) },
                modifier = Modifier.padding(start = px(if (left) 19 else 7), end = px(if (left) 7 else 19)),
            )
        }
    }
}

@Composable
private fun Chip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.height(px(54)).width(px(if (label.length <= 1) 76 else if (label.length == 2) 94 else 98)).clip(RoundedCornerShape(50))
            .background(if (selected) MockColors.Lime else MockColors.Chip).clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { MockText(label, 22, weight = if (selected) FontWeight.Bold else FontWeight.Medium) }
}

/** 시안의 가게 카드: 사진 위에 연두 태그와 북마크, 아래에 상호·평점·지역·한 줄 소개 */
@Composable
fun ExploreCard(c: StoreCardData, onOpen: () -> Unit, onBookmark: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().height(px(372)).mockCardBox(18).clickable(role = Role.Button, onClick = onOpen)) {
        Box(Modifier.fillMaxWidth().height(px(243))) {
            StorePhoto(c.store, Modifier.fillMaxSize())
            Box(Modifier.padding(start = px(15), top = px(21))) { LimePill(c.tag, 20, padX = 16, padY = 5) }
            Box(Modifier.align(Alignment.TopEnd).padding(top = px(18), end = px(14)).size(px(40)).clickable(role = Role.Button, onClick = onBookmark), contentAlignment = Alignment.Center) {
                BookmarkIcon(34, filled = c.bookmarked)
            }
        }
        Column(Modifier.padding(horizontal = px(19)).padding(top = px(10)), verticalArrangement = Arrangement.spacedBy(px(6))) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f)) { MockText(c.store.name, 26, font = MockSerif, weight = FontWeight.ExtraBold) }
                RatingText(c.rating?.average, c.rating?.count, 21)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                PinIcon(21)
                Box(Modifier.width(px(8)))
                MockText(c.regionLabel, 21, color = MockColors.Ink2)
                c.distanceLabel?.let { Box(Modifier.width(px(10))); MockText("· $it", 21, color = MockColors.Ink2) }
            }
            MockText(c.oneLiner, 20, color = MockColors.Hint, maxLines = 1)
        }
    }
}

/** (임시) 예전 화면이 쓰던 간단한 가게 카드 */
@Composable
fun StoreCard(store: Store, currentYear: Int, modifier: Modifier = Modifier, onClick: () -> Unit) =
    ExploreCard(StoreCards.of(store, currentYear, emptyMap(), null, emptySet()), onClick, {}, modifier)
