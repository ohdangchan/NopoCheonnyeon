package com.nopo.cheonnyeon.feature.passport

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.nopo.cheonnyeon.R
import com.nopo.cheonnyeon.app.AppContainer
import com.nopo.cheonnyeon.core.common.SeoulZone
import com.nopo.cheonnyeon.core.designsystem.component.StampSize
import com.nopo.cheonnyeon.core.designsystem.component.StampView
import com.nopo.cheonnyeon.core.designsystem.component.labelRes
import com.nopo.cheonnyeon.core.designsystem.mock.BookmarkIcon
import com.nopo.cheonnyeon.core.designsystem.mock.ChevronRight
import com.nopo.cheonnyeon.core.designsystem.mock.GrayPill
import com.nopo.cheonnyeon.core.designsystem.mock.MockColors
import com.nopo.cheonnyeon.core.designsystem.mock.MockPic
import com.nopo.cheonnyeon.core.designsystem.mock.MockSerif
import com.nopo.cheonnyeon.core.designsystem.mock.MockStage
import com.nopo.cheonnyeon.core.designsystem.mock.MockText
import com.nopo.cheonnyeon.core.designsystem.mock.PinIcon
import com.nopo.cheonnyeon.core.designsystem.mock.at
import com.nopo.cheonnyeon.core.designsystem.mock.px
import com.nopo.cheonnyeon.data.local.MyVisitEntity
import com.nopo.cheonnyeon.domain.model.Store
import com.nopo.cheonnyeon.domain.model.Title
import com.nopo.cheonnyeon.domain.rule.StampGradePolicy
import com.nopo.cheonnyeon.domain.rule.TitlePolicy
import com.nopo.cheonnyeon.feature.store.StorePhoto
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant

data class PassportStamp(val visit: MyVisitEntity, val store: Store?) {
    val isTest: Boolean get() = visit.storeId.startsWith(Store.TEST_PREFIX)
}

data class PassportUiState(
    val loading: Boolean = true,
    val stamps: List<PassportStamp> = emptyList(),
    val totalYears: Int = 0,
    val regions: List<com.nopo.cheonnyeon.domain.rule.RegionProgress> = emptyList(),
)

class PassportViewModel(private val container: AppContainer) : ViewModel() {
    private val _reviews = kotlinx.coroutines.flow.MutableStateFlow<List<com.nopo.cheonnyeon.data.remote.ReviewItem>?>(null)

    /** 내가 남긴 리뷰 (null = 아직 안 불러옴). 온라인에서만 불러온다 */
    val reviews: StateFlow<List<com.nopo.cheonnyeon.data.remote.ReviewItem>?> = _reviews

    val bookmarks = container.settings.bookmarks
    fun toggleBookmark(id: String) { viewModelScope.launch { container.settings.toggleBookmark(id) } }

    fun loadReviews() {
        val uid = container.auth.currentUid ?: return
        viewModelScope.launch {
            val server = container.reviews.byUser(uid).valueOrNull.orEmpty()
            // 시연용 패스포트를 채운 경우 그 도장에 달린 예시 리뷰도 함께 보여 준다
            val demo = if (container.settings.demoData.first() && container.database.myVisitDao().demoVisitCount() > 0) {
                container.demoReviews.myReviews(container.auth.currentNickname().orEmpty())
            } else emptyList()
            _reviews.value = demo + server
        }
    }

    val myUid: String? get() = container.auth.currentUid

    suspend fun nickname(): String = container.auth.currentNickname().orEmpty()

    val state: StateFlow<PassportUiState> = combine(
        container.database.myVisitDao().observeAll(),
        container.catalog.changes,
    ) { visits, _ ->
        val stores = container.catalog.all().associateBy { it.id }
        PassportUiState(
            loading = false,
            stamps = visits.map { PassportStamp(it, stores[it.storeId]) },
            totalYears = visits.filter { !it.storeId.startsWith(Store.TEST_PREFIX) }.sumOf { it.yearsAwarded },
            regions = com.nopo.cheonnyeon.domain.rule.RegionProgressRule.compute(stores.values.toList(), visits.map { it.storeId }.toSet()),
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, PassportUiState())
}

/** 시안 "컬렉션"(6.webp) 복제: 넘겨 볼 수 있는 여권 + 다음 칭호 카드 + AI 룰렛 카드 + 내가 방문한 노포 */
@Composable
fun PassportScreen(
    vm: PassportViewModel, onOpenStore: (String) -> Unit, onOpenProfile: (String) -> Unit, onOpenHall: () -> Unit,
    onOpenRoulette: () -> Unit = {}, onBell: () -> Unit = {},
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val bookmarks by vm.bookmarks.collectAsStateWithLifecycle(initialValue = emptySet())
    var detail by rememberSaveable { mutableStateOf<String?>(null) }
    var all by rememberSaveable { mutableStateOf(false) }
    var dex by rememberSaveable { mutableStateOf(false) }

    val real = remember(state.stamps) { state.stamps.filter { !it.isTest && it.store != null } }
    val visited = remember(real) { real.sortedByDescending { it.visit.lastVisitedAt } }
    val title = TitlePolicy.titleFor(state.totalYears)
    val nextTitle = Title.entries.firstOrNull { it.requiredYears > state.totalYears }
    val pages = remember(state.regions, state.stamps) { buildPages(state.regions, state.stamps) }
    val goal = maxOf(50, ((real.size / 50) + 1) * 50)
    val stats = BookStats(visited = real.size, goal = goal, titles = title.ordinal + 1)

    if (all) {
        PassportAll(vm, state, visited, bookmarks, onBack = { all = false }, onOpenStore = onOpenStore, onOpenProfile = onOpenProfile, onOpenHall = onOpenHall, onOpenDex = { dex = true })
    } else {
        Column(Modifier.fillMaxSize().background(MockColors.Cream).statusBarsPadding().verticalScroll(rememberScrollState())) {
            MockStage(originY = 84, heightPx = 1490) {
                MockPic(R.drawable.mk_deco_collection, 330, 160, Modifier.at(430, 150))
                MockPic(R.drawable.mk_hdr_logo, 432, 122, Modifier.at(20, 84))
                MockPic(R.drawable.mk_ic_search, 54, 52, Modifier.at(604, 90), onClick = {})
                MockPic(R.drawable.mk_ic_bell, 54, 56, Modifier.at(682, 88), onClick = onBell)
                MockPic(R.drawable.mk_col_title, 470, 113, Modifier.at(0, 232))
                MockPic(R.drawable.mk_col_note, 270, 120, Modifier.at(490, 290))
                PassportBook(stats, pages, onStamp = { detail = it.visit.storeId }, modifier = Modifier.at(0, 380))

                // 다음 칭호까지
                Box(
                    Modifier.at(18, 953).size(px(490), px(142)).clip(RoundedCornerShape(px(22))).background(MockColors.ProgressCard)
                        .clickable(role = Role.Button) { dex = true },
                )
                MockPic(R.drawable.mk_col_ic_flag, 52, 54, Modifier.at(30, 966))
                MockText("다음 칭호까지", 22, Modifier.at(98, 975), color = MockColors.Navy, weight = FontWeight.Medium)
                MockText(
                    if (nextTitle != null) "'${stringResource(nextTitle.labelRes())}'까지 ${nextTitle.requiredYears - state.totalYears}년 남았어요!" else "최고 칭호에 올랐어요!",
                    25, Modifier.at(98, 1010).width(px(385)), weight = FontWeight.Bold,
                )
                val span = (nextTitle?.requiredYears ?: title.requiredYears + 1) - title.requiredYears
                val into = (state.totalYears - title.requiredYears).coerceAtLeast(0)
                val frac = if (nextTitle == null) 1f else (into.toFloat() / span).coerceIn(0f, 1f)
                Box(Modifier.at(42, 1062).size(px(380), px(18)).clip(RoundedCornerShape(50)).background(Color(0xFFD9DBCB))) {
                    Box(Modifier.fillMaxSize().padding(end = px(380) * (1f - frac)).clip(RoundedCornerShape(50)).background(MockColors.LimeDeep))
                }
                MockText(if (nextTitle != null) "$into/$span" else "MAX", 22, Modifier.at(436, 1058), color = MockColors.Ink2, weight = FontWeight.Medium)
                ChevronRight(26, Modifier.at(474, 1012))

                // AI 룰렛 (시안의 선물 자리)
                Box(
                    Modifier.at(516, 953).size(px(229), px(142)).clip(RoundedCornerShape(px(22))).background(Color(0xFFF3EFE3))
                        .clickable(role = Role.Button, onClick = onOpenRoulette),
                )
                Canvas(Modifier.at(522, 959).size(px(217), px(130))) {
                    drawRoundRect(
                        Color(0xFFC9C2AB), cornerRadius = androidx.compose.ui.geometry.CornerRadius(20.dp.toPx()),
                        style = Stroke(1.2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 7f))),
                    )
                }
                Box(Modifier.at(534, 1002).size(px(62), px(62)).clip(CircleShape).background(MockColors.Lime), contentAlignment = Alignment.Center) {
                    com.nopo.cheonnyeon.core.designsystem.mock.AiSparkIcon(40)
                }
                MockText("오늘 뭐 먹지?", 21, Modifier.at(608, 990), weight = FontWeight.Bold)
                MockText("AI 룰렛으로", 19, Modifier.at(608, 1022), color = MockColors.Ink2)
                MockText("골라보기 →", 19, Modifier.at(608, 1048), color = MockColors.Ink2)

                // 내가 방문한 노포
                MockPic(R.drawable.mk_col_title_visited, 296, 60, Modifier.at(14, 1110))
                MockPic(R.drawable.mk_col_all, 145, 48, Modifier.at(605, 1118), onClick = { all = true })
                if (visited.isEmpty()) {
                    Box(Modifier.at(18, 1190).size(px(724), px(230)).clip(RoundedCornerShape(px(20))).background(MockColors.Card).border(1.dp, MockColors.CardLine, RoundedCornerShape(px(20))), contentAlignment = Alignment.Center) {
                        MockText("아직 방문한 노포가 없어요.\n영수증 인증으로 첫 도장을 찍어 보세요!", 24, color = MockColors.Hint, align = TextAlign.Center, maxLines = 2, lineHeightPx = 36)
                    }
                } else {
                    val rowState = rememberLazyListState()
                    LazyRow(
                        Modifier.at(0, 1178).fillMaxWidth().height(px(290)), state = rowState,
                        contentPadding = PaddingValues(horizontal = px(18)), horizontalArrangement = Arrangement.spacedBy(px(8)),
                    ) {
                        items(visited.take(30), key = { it.visit.storeId }) { s ->
                            VisitedCard(s, s.visit.storeId in bookmarks, px(197), onOpen = { onOpenStore(s.visit.storeId) }, onBookmark = { vm.toggleBookmark(s.visit.storeId) })
                        }
                    }
                    val dots = minOf(4, (minOf(visited.size, 30) + 1) / 2).coerceAtLeast(1)
                    val selected = (rowState.firstVisibleItemIndex / 2).coerceIn(0, dots - 1)
                    Row(Modifier.at(0, 1480).fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                        repeat(dots) { i ->
                            val on = i == selected
                            Box(Modifier.padding(horizontal = px(8)).size(px(if (on) 18 else 13)).clip(CircleShape).background(if (on) MockColors.LimeDeep else Color(0xFFCFCCBE)))
                        }
                    }
                }
            }
        }
    }

    if (dex) com.nopo.cheonnyeon.feature.passport.TitleDexDialog(state.totalYears) { dex = false }
    state.stamps.firstOrNull { it.visit.storeId == detail }?.let { stamp ->
        StampDetail(stamp, onDismiss = { detail = null }, onOpenStore = { detail = null; onOpenStore(stamp.visit.storeId) })
    }
}

/** 시안의 "내가 방문한 노포" 카드: 사진 + 왼쪽 아래 붉은 VISITED 도장 + 북마크, 아래에 상호·지역 */
@Composable
private fun VisitedCard(s: PassportStamp, bookmarked: Boolean, width: androidx.compose.ui.unit.Dp, onOpen: () -> Unit, onBookmark: () -> Unit) {
    val store = s.store ?: return
    val date = Instant.ofEpochMilli(s.visit.lastVisitedAt).atZone(SeoulZone).toLocalDate()
    Column(
        Modifier.width(width).height(px(280)).clip(RoundedCornerShape(px(16))).background(MockColors.Card)
            .border(1.dp, MockColors.CardLine, RoundedCornerShape(px(16))).clickable(role = Role.Button, onClick = onOpen),
    ) {
        Box(Modifier.fillMaxWidth().height(px(198))) {
            StorePhoto(store, Modifier.fillMaxSize())
            Box(
                Modifier.align(Alignment.TopEnd).padding(top = px(14), end = px(12)).size(px(40)).clip(CircleShape).background(Color(0xE6FFFFFF)).clickable(role = Role.Button, onClick = onBookmark),
                contentAlignment = Alignment.Center,
            ) { BookmarkIcon(26, color = MockColors.Navy, filled = bookmarked) }
            Box(Modifier.align(Alignment.BottomStart).padding(start = px(10), bottom = px(8)).size(px(84)).rotate(-9f).clip(CircleShape).background(Color(0xCCFFFFFF)).border(2.dp, MockColors.Stamp, CircleShape), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    MockText("${date.year}", 15, color = MockColors.Stamp, font = MockSerif, weight = FontWeight.ExtraBold)
                    MockText("%d.%02d".format(date.monthValue, date.dayOfMonth), 22, color = MockColors.Stamp, font = MockSerif, weight = FontWeight.ExtraBold)
                    MockText("VISITED", 12, color = MockColors.Stamp, weight = FontWeight.Bold)
                }
            }
        }
        Column(Modifier.padding(horizontal = px(14)).padding(top = px(8)), verticalArrangement = Arrangement.spacedBy(px(4))) {
            MockText(store.name, 25, font = MockSerif, weight = FontWeight.ExtraBold)
            Row(verticalAlignment = Alignment.CenterVertically) {
                PinIcon(19); Box(Modifier.width(px(6)))
                MockText(listOf(store.sido, store.sigungu).filter { it.isNotBlank() }.joinToString(" "), 19, color = MockColors.Ink2)
            }
        }
    }
}

/** "전체 보기": 방문한 노포 전체 + 칭호 도감·명예의 전당·내 리뷰·공유 카드 (옛 패스포트 기능) */
@Composable
private fun PassportAll(
    vm: PassportViewModel, state: PassportUiState, visited: List<PassportStamp>, bookmarks: Set<String>,
    onBack: () -> Unit, onOpenStore: (String) -> Unit, onOpenProfile: (String) -> Unit, onOpenHall: () -> Unit, onOpenDex: () -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    var reviewsMode by rememberSaveable { mutableStateOf(false) }
    var shareFailed by rememberSaveable { mutableStateOf(false) }
    val myReviews by vm.reviews.collectAsStateWithLifecycle()
    val c = com.nopo.cheonnyeon.core.designsystem.theme.NopoTheme.colors
    val title = TitlePolicy.titleFor(state.totalYears)
    val titleName = stringResource(title.labelRes())
    androidx.activity.compose.BackHandler(onBack = onBack)

    Column(Modifier.fillMaxSize().background(MockColors.Cream).statusBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = px(20), vertical = px(18)), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.clickable(role = Role.Button, onClick = onBack).padding(px(10))) { MockText("‹ 컬렉션", 30, font = MockSerif, weight = FontWeight.ExtraBold) }
            Box(Modifier.weight(1f))
            MockText("내가 방문한 노포 ${visited.size}곳", 22, color = MockColors.Ink2)
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = px(20)).horizontalScrollCompat(), horizontalArrangement = Arrangement.spacedBy(px(12))) {
            GrayPill("방문한 노포", 22, color = if (!reviewsMode) MockColors.Lime else MockColors.Chip, textColor = MockColors.Navy) { reviewsMode = false }
            GrayPill("내가 쓴 리뷰", 22, color = if (reviewsMode) MockColors.Lime else MockColors.Chip, textColor = MockColors.Navy) { reviewsMode = true; vm.loadReviews() }
            GrayPill("칭호 도감", 22, textColor = MockColors.Navy, onClick = onOpenDex)
            GrayPill("명예의 전당", 22, textColor = MockColors.Navy, onClick = onOpenHall)
            vm.myUid?.let { uid -> GrayPill("내 프로필", 22, textColor = MockColors.Navy) { onOpenProfile(uid) } }
            if (visited.isNotEmpty()) GrayPill("공유 카드", 22, textColor = MockColors.Navy) {
                scope.launch {
                    val data = com.nopo.cheonnyeon.platform.share.ShareCardData(
                        nickname = vm.nickname().ifBlank { "-" },
                        titleName = titleName,
                        totalYears = state.totalYears,
                        stampCount = visited.size,
                        recent = visited.take(3).map { (it.store?.name ?: it.visit.storeId) to it.visit.yearsAwarded },
                        colors = com.nopo.cheonnyeon.platform.share.ShareCardColors(
                            c.paper.toArgb(), c.surface.toArgb(), c.primary.toArgb(), c.stamp.toArgb(), c.brass.toArgb(), c.ink.toArgb(), c.ink2.toArgb(),
                        ),
                    )
                    shareFailed = !com.nopo.cheonnyeon.platform.share.PassportShareCard.share(context, data)
                }
            }
        }
        if (shareFailed) Box(Modifier.padding(px(20))) { MockText(stringResource(R.string.share_card_failed), 22, color = MockColors.Stamp) }
        Box(Modifier.height(px(14)))
        if (reviewsMode) {
            val list = myReviews
            when {
                list == null -> Box(Modifier.padding(px(24))) { MockText(stringResource(R.string.list_loading), 24, color = MockColors.Hint) }
                list.isEmpty() -> Box(Modifier.padding(px(24))) { MockText(stringResource(R.string.profile_no_reviews), 24, color = MockColors.Hint) }
                else -> LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(px(20)), verticalArrangement = Arrangement.spacedBy(px(12))) {
                    items(list.size) { i ->
                        val r = list[i]
                        Column(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(px(16))).background(MockColors.Card).border(1.dp, MockColors.CardLine, RoundedCornerShape(px(16)))
                                .clickable(role = Role.Button) { onOpenStore(r.storeId) }.padding(px(18)),
                            verticalArrangement = Arrangement.spacedBy(px(8)),
                        ) {
                            MockText(r.storeName, 26, font = MockSerif, weight = FontWeight.ExtraBold)
                            com.nopo.cheonnyeon.core.designsystem.component.StarRating(r.rating.toFloat())
                            if (r.text.isNotBlank()) MockText(r.text, 22, maxLines = 6)
                            if (r.photos.isNotEmpty()) com.nopo.cheonnyeon.feature.store.ReviewPhotoRow(r.photos, Modifier.fillMaxWidth())
                        }
                    }
                }
            }
        } else if (visited.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(px(30)), contentAlignment = Alignment.Center) {
                MockText("아직 방문한 노포가 없어요.\n영수증 인증으로 첫 도장을 찍어 보세요!", 24, color = MockColors.Hint, align = TextAlign.Center, maxLines = 2, lineHeightPx = 36)
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2), modifier = Modifier.weight(1f), contentPadding = PaddingValues(px(20)),
                horizontalArrangement = Arrangement.spacedBy(px(14)), verticalArrangement = Arrangement.spacedBy(px(14)),
            ) {
                gridItems(visited, key = { it.visit.storeId }) { s ->
                    VisitedCard(s, s.visit.storeId in bookmarks, px(350), onOpen = { onOpenStore(s.visit.storeId) }, onBookmark = { vm.toggleBookmark(s.visit.storeId) })
                }
            }
        }
    }
}

@Composable
private fun Modifier.horizontalScrollCompat(): Modifier = this.then(Modifier.horizontalScroll(rememberScrollState()))

@Composable
private fun StampDetail(stamp: PassportStamp, onDismiss: () -> Unit, onOpenStore: () -> Unit) {
    val v = stamp.visit
    val first = Instant.ofEpochMilli(v.firstVisitedAt).atZone(SeoulZone).toLocalDate()
    val method = when (v.method) {
        "RECEIPT" -> R.string.method_receipt
        "PHOTO" -> R.string.method_photo
        "DEMO" -> R.string.method_demo
        else -> R.string.method_location_only
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onOpenStore) { Text(stringResource(R.string.passport_open_store)) } },
        dismissButton = { TextButton(onDismiss) { Text(stringResource(R.string.passport_close)) } },
        title = { Text(stamp.store?.name ?: v.storeId) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(com.nopo.cheonnyeon.core.designsystem.theme.NopoTheme.spacing.sm)) {
                StampView(v.yearsAwarded, StampGradePolicy.gradeFor(v.yearsAwarded), size = StampSize.Grid)
                val proof = java.io.File(androidx.compose.ui.platform.LocalContext.current.filesDir, "proofs/" + v.storeId + ".jpg")
                if (proof.exists()) {
                    coil3.compose.AsyncImage(
                        model = proof, contentDescription = stringResource(R.string.stamp_proof_photo),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                        modifier = Modifier.fillMaxWidth().height(160.dp),
                    )
                }
                Text(stringResource(R.string.stamp_first_visit, first.toString()))
                Text(stringResource(R.string.stamp_years, v.yearsAwarded))
                Text(stringResource(R.string.stamp_method, stringResource(method)))
                Text(stringResource(R.string.stamp_visit_count, v.visitCount))
                if (stamp.isTest) Text(stringResource(R.string.stamp_is_test), color = com.nopo.cheonnyeon.core.designsystem.theme.NopoTheme.colors.ink2)
            }
        },
    )
}
