package com.nopo.cheonnyeon.feature.map

import android.Manifest
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.naver.maps.geometry.LatLng
import com.naver.maps.map.CameraAnimation
import com.naver.maps.map.CameraUpdate
import com.naver.maps.map.NaverMap
import com.naver.maps.map.NaverMapSdk
import com.naver.maps.map.overlay.Marker
import com.naver.maps.map.util.MarkerIcons
import com.nopo.cheonnyeon.R
import com.nopo.cheonnyeon.core.designsystem.mock.BookmarkIcon
import com.nopo.cheonnyeon.core.designsystem.mock.ChevronDown
import com.nopo.cheonnyeon.core.designsystem.mock.LocalMockU
import com.nopo.cheonnyeon.core.designsystem.mock.MockColors
import com.nopo.cheonnyeon.core.designsystem.mock.MockPic
import com.nopo.cheonnyeon.core.designsystem.mock.MockSans
import com.nopo.cheonnyeon.core.designsystem.mock.MockSerif
import com.nopo.cheonnyeon.core.designsystem.mock.MockStage
import com.nopo.cheonnyeon.core.designsystem.mock.MockText
import com.nopo.cheonnyeon.core.designsystem.mock.PinIcon
import com.nopo.cheonnyeon.core.designsystem.mock.RatingText
import com.nopo.cheonnyeon.core.designsystem.mock.at
import com.nopo.cheonnyeon.core.designsystem.mock.px
import com.nopo.cheonnyeon.core.designsystem.mock.pxSp
import com.nopo.cheonnyeon.core.designsystem.mock.saveDescription
import com.nopo.cheonnyeon.core.designsystem.theme.NopoTheme
import com.nopo.cheonnyeon.core.util.GeoDistance
import com.nopo.cheonnyeon.data.remote.StoreRating
import com.nopo.cheonnyeon.domain.catalog.StoreFilters
import com.nopo.cheonnyeon.domain.model.StampGrade
import com.nopo.cheonnyeon.domain.rule.StampGradePolicy
import com.nopo.cheonnyeon.feature.common.StoreCardData
import com.nopo.cheonnyeon.feature.common.StoreCards
import com.nopo.cheonnyeon.feature.diagnostics.DiagLast
import com.nopo.cheonnyeon.feature.store.StorePhoto
import com.nopo.cheonnyeon.platform.map.NaverMapView
import com.nopo.cheonnyeon.platform.permission.PermissionStatus
import com.nopo.cheonnyeon.platform.permission.rememberPermission
import kotlinx.coroutines.delay

private val CITY_HALL = LatLng(37.5665, 126.9780)
private const val MIN_ZOOM_FOR_MARKERS = 9.0
private const val CAPTION_ZOOM = 14.0
private const val MAX_MARKERS = 250
private const val BUBBLES = 6

/** 시안 "지도"(5.webp) 복제: 머리글 + 검색창 + 지역 칩 + 실제 네이버 지도 + 하단 "지금 이 근처 노포" 카드. 비행기 버튼은 뺐다. */
@Composable
fun MapScreen(vm: MapViewModel, onOpenStore: (String) -> Unit, onOpenChat: (String) -> Unit = {}, onBell: () -> Unit = {},
    onSearch: () -> Unit = {}) {
    val state by vm.state.collectAsStateWithLifecycle()
    val ratings by vm.ratings.collectAsStateWithLifecycle()
    val bookmarks by vm.bookmarks.collectAsStateWithLifecycle(initialValue = emptySet())
    val permission = rememberPermission(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
    var sigungu by rememberSaveable { mutableStateOf<String?>(null) }
    var regionMenu by remember { mutableStateOf(false) }
    var focusTick by rememberSaveable { mutableIntStateOf(0) }
    var focusTarget by remember { mutableStateOf<LatLng?>(null) }
    val focus = LocalFocusManager.current

    LaunchedEffect(permission.status) {
        if (permission.status == PermissionStatus.GRANTED && state.fix == null) vm.findMe()
    }

    val shown = remember(state,
        sigungu) { if (sigungu == null) state else state.copy(filtered = state.filtered.filter { it.sigungu == sigungu }) }
    val chipNames = remember(state.filtered) {
        state.filtered.filter { it.sigungu.isNotBlank() }.groupingBy { it.sigungu }.eachCount()
            .entries.sortedByDescending { it.value }.map { it.key }
    }
    val year = state.currentYear
    val nearCards = remember(shown, state.fix, ratings, bookmarks) {
        val base = shown.filtered.filter { it.lat != null && it.lng != null && !it.isTest }
        val from = state.fix
        val sorted = if (from != null) StoreFilters.sortByDistance(base, from.lat,
            from.lng) else StoreFilters.sortByDistance(base, CITY_HALL.latitude, CITY_HALL.longitude)
        val withPhoto = sorted.filter { it.photoUrls.isNotEmpty() }
        (withPhoto.ifEmpty { sorted }).take(12).map { StoreCards.of(it, year, ratings, from, bookmarks) }
    }
    fun focusOn(n: String) {
        sigungu = n
        val list = state.filtered.filter { it.sigungu == n && it.lat != null && it.lng != null }
        if (list.isNotEmpty()) { focusTarget = LatLng(list.map { it.lat!! }.average(), list.map { it.lng!! }.average()); focusTick++ }
    }

    Column(Modifier.fillMaxSize().background(MockColors.Cream).statusBarsPadding()) {
        MockStage(originY = 84, heightPx = 336) {
            MockPic(R.drawable.mk_deco_map, 330, 140, Modifier.at(430, 140))
            MockPic(R.drawable.mk_hdr_logo, 432, 122, Modifier.at(20, 84))
            MockPic(R.drawable.mk_ic_bell, 54, 56, Modifier.at(682, 88), onClick = onBell)
            // 검색창
            Box(Modifier.at(22, 263).size(px(623), px(70)).clip(RoundedCornerShape(px(35))).background(MockColors.Card)
                .border(1.dp, MockColors.CardLine, RoundedCornerShape(px(35))))
            MockPic(R.drawable.mk_map_ic_search, 36, 36, Modifier.at(44, 280))
            Box(Modifier.at(100, 270).size(px(535), px(56)), contentAlignment = Alignment.CenterStart) {
                if (state.filter.query.isEmpty()) MockText(stringResource(R.string.map_01),
                    24, color = MockColors.Hint)
                BasicTextField(
                    value = state.filter.query, onValueChange = vm::setQuery, singleLine = true,
                    textStyle = TextStyle(fontFamily = MockSans, fontSize = pxSp(24), color = MockColors.Navy),
                    cursorBrush = SolidColor(MockColors.Navy),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { focus.clearFocus(); if (looksLikeQuestion(state.filter.query)) onOpenChat(state.filter.query) }),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            // 오른쪽 동그라미: 시안의 필터 아이콘 자리에 AI 아이콘
            Box(
                Modifier.at(662, 261).size(px(76), px(76)).clip(CircleShape).background(MockColors.Card)
                    .border(1.dp, MockColors.CardLine, CircleShape)
                    .semantics { contentDescription = "AI 도우미" }.clickable(role = Role.Button) { onOpenChat(state.filter.query) },
                contentAlignment = Alignment.Center,
            ) {
                Box(Modifier.size(px(52)).clip(CircleShape).background(MockColors.Lime2),
                    contentAlignment = Alignment.Center) { MockText("AI", 24,
                    weight = FontWeight.ExtraBold, color = MockColors.Navy) }
            }
            // 지역 칩
            Row(Modifier.at(20, 349).fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(px(13))) {
                MapChip("전체", sigungu == null, 76) { sigungu = null }
                chipNames.take(5).forEach { n -> MapChip(n, sigungu == n, 94) { focusOn(n) } }
                Box {
                    Row(
                        Modifier.height(px(54)).clip(RoundedCornerShape(50)).background(MockColors.Chip)
                            .clickable(role = Role.Button) { regionMenu = true }.padding(horizontal = px(24)),
                        verticalAlignment = Alignment.CenterVertically,
                    ) { MockText(stringResource(R.string.map_02), 22,
                        weight = FontWeight.Medium); Box(Modifier.width(px(10))); ChevronDown(20) }
                    DropdownMenu(expanded = regionMenu, onDismissRequest = { regionMenu = false }) {
                        chipNames.forEach { n -> DropdownMenuItem(text = { Text(n) }, onClick = { regionMenu = false; focusOn(n) }) }
                    }
                }
            }
        }

        Box(Modifier.weight(1f).fillMaxWidth()) {
            if (state.showList) {
                Column(Modifier.fillMaxSize()) {
                    if (state.mapFailed) {
                        Text(
                            stringResource(R.string.map_failed_fallback), style = NopoTheme.typography.labelMedium, color = NopoTheme.colors.ink,
                            modifier = Modifier.fillMaxWidth().background(NopoTheme.colors.stampSoft)
                                .padding(horizontal = NopoTheme.spacing.lg, vertical = NopoTheme.spacing.sm),
                        )
                    }
                    StoreListScreen(shown, vm::setSort, onOpenStore)
                }
            } else {
                MapContent(shown, vm, ratings, onOpenStore, focusTarget, focusTick, onLocate = {
                    when (permission.status) {
                        PermissionStatus.GRANTED -> vm.findMe()
                        PermissionStatus.PERMANENTLY_DENIED -> permission.openSettings()
                        else -> permission.request()
                    }
                })
            }
        }

        // 하단 "지금 이 근처 노포"
        Box(Modifier.fillMaxWidth().height(px(435)).clip(RoundedCornerShape(topStart = px(34), topEnd = px(34)))
            .background(MockColors.Card)) {
            Box(Modifier.align(Alignment.TopCenter).padding(top = px(16)).size(px(60), px(7)).clip(RoundedCornerShape(50))
                .background(Color(0xFFCFCBBB)))
            MockPic(R.drawable.mk_map_title_near, 252, 52, Modifier.padding(start = px(18), top = px(23)))
            MockPic(R.drawable.mk_map_more, 95, 48, Modifier.align(Alignment.TopEnd).padding(end = px(15), top = px(27)),
                onClick = vm::toggleListMode)
            LazyRow(
                Modifier.padding(top = px(87)).fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = px(22)),
                horizontalArrangement = Arrangement.spacedBy(px(16)),
            ) {
                items(nearCards, key = { it.store.id }) { c -> NearCard(c, onOpen = { onOpenStore(c.store.id) },
                    onBookmark = { vm.toggleBookmark(c.store.id) }) }
            }
        }
    }
}

@Composable
private fun MapChip(label: String, selected: Boolean, widthPx: Int, onClick: () -> Unit) {
    Box(
        Modifier.height(px(54)).widthIn(min = px(widthPx)).clip(RoundedCornerShape(50))
            .background(if (selected) MockColors.Lime else MockColors.Chip)
            .clickable(role = Role.Button, onClick = onClick).padding(horizontal = px(16)),
        contentAlignment = Alignment.Center,
    ) { MockText(label, 22, weight = if (selected) FontWeight.Bold else FontWeight.Medium) }
}

@Composable
private fun NearCard(c: StoreCardData, onOpen: () -> Unit, onBookmark: () -> Unit) {
    Column(Modifier.width(px(281)).height(px(343)).clip(RoundedCornerShape(px(16))).background(MockColors.Cream)
        .border(1.dp, MockColors.CardLine, RoundedCornerShape(px(16))).clickable(role = Role.Button, onClick = onOpen)) {
        Box(Modifier.fillMaxWidth().height(px(156))) {
            StorePhoto(c.store, Modifier.fillMaxSize())
            Box(Modifier.align(Alignment.TopEnd).padding(top = px(10), end = px(10)).size(px(40)).saveDescription()
                .clickable(role = Role.Button, onClick = onBookmark), contentAlignment = Alignment.Center) {
                BookmarkIcon(32, filled = c.bookmarked)
            }
            Box(
                Modifier.align(Alignment.BottomStart).padding(start = px(16), bottom = px(14)).clip(RoundedCornerShape(50))
                    .background(Color(0x66000000))
                    .border(1.dp, Color.White, RoundedCornerShape(50)).padding(horizontal = px(14), vertical = px(4)),
            ) { MockText(c.tag, 19, color = Color.White, weight = FontWeight.Medium) }
        }
        Column(Modifier.padding(horizontal = px(15)).padding(top = px(10)), verticalArrangement = Arrangement.spacedBy(px(5))) {
            MockText(c.store.name, 25, font = MockSerif, weight = FontWeight.ExtraBold)
            Row(verticalAlignment = Alignment.CenterVertically) {
                RatingText(c.rating?.average, c.rating?.count, 20)
                c.distanceLabel?.let { MockText(" · $it", 20, color = MockColors.Ink2) }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                PinIcon(19); Box(Modifier.width(px(6)))
                MockText(c.regionLabel, 19, color = MockColors.Ink2)
            }
            MockText(c.oneLiner, 19, color = MockColors.Hint, maxLines = 2, lineHeightPx = 28)
        }
    }
}

@Composable
private fun MapContent(
    state: MapUiState, vm: MapViewModel, ratings: Map<String, StoreRating>, onOpenStore: (String) -> Unit,
    focusTarget: LatLng?, focusTick: Int, onLocate: () -> Unit,
) {
    val context = LocalContext.current
    val c = NopoTheme.colors
    val u = LocalMockU.current
    val density = LocalDensity.current.density
    val bubbleW = (u * 190 * density).toInt()
    val bubbleH = bubbleW * BubbleIcons.H / BubbleIcons.W
    var naverMap by remember { mutableStateOf<NaverMap?>(null) }
    // 지도가 8초 안에 준비되지 않으면(그림이 안 뜨는 기기) 목록으로 바꿔 빈 화면을 막는다
    LaunchedEffect(Unit) { delay(8_000); if (naverMap == null) vm.onMapFailed() }
    var cameraTick by remember { mutableIntStateOf(0) }
    var authError by rememberSaveable { mutableStateOf<String?>(null) }
    var zoomedOut by remember { mutableStateOf(false) }
    val markers = remember { HashMap<String, Marker>() }
    val captionYears = stringResource(R.string.map_marker_caption)

    DisposableEffect(Unit) {
        val sdk = NaverMapSdk.getInstance(context)
        sdk.onAuthFailedListener = NaverMapSdk.OnAuthFailedListener { e -> vm.onMapFailed(); authError = e.errorCode; DiagLast.mapAuthError = e.errorCode }
        onDispose { sdk.onAuthFailedListener = null }
    }

    // 내 위치가 잡히면 표시하고, 내 위치 버튼을 누르거나 처음 잡힐 때 그곳으로 이동
    LaunchedEffect(naverMap, state.fix) {
        val map = naverMap ?: return@LaunchedEffect
        val fix = state.fix ?: return@LaunchedEffect
        map.locationOverlay.apply { isVisible = true; position = LatLng(fix.lat, fix.lng); circleRadius = fix.accuracyM.toInt() }
    }
    var movedTick by rememberSaveable { mutableStateOf(0) }
    LaunchedEffect(naverMap, state.recenter) {
        val map = naverMap ?: return@LaunchedEffect
        val fix = state.fix ?: return@LaunchedEffect
        if (state.recenter == movedTick) return@LaunchedEffect
        movedTick = state.recenter
        map.moveCamera(CameraUpdate.scrollAndZoomTo(LatLng(fix.lat, fix.lng), 14.0).animate(CameraAnimation.Easing))
    }

    LaunchedEffect(naverMap, focusTick) {
        val map = naverMap ?: return@LaunchedEffect
        val t = focusTarget ?: return@LaunchedEffect
        map.moveCamera(CameraUpdate.scrollAndZoomTo(t, 13.0).animate(CameraAnimation.Easing))
    }

    // 화면 영역 안의 가게만 마커로 그린다 (FR-MAP-04). 카메라가 멈추거나 필터가 바뀌면 다시 계산
    LaunchedEffect(naverMap, state.filtered, state.visitedIds, cameraTick, state.selectedId) {
        val map = naverMap ?: return@LaunchedEffect
        val zoom = map.cameraPosition.zoom
        zoomedOut = zoom < MIN_ZOOM_FOR_MARKERS
        if (zoomedOut) {
            // 축소하면 시·도별 개수 원으로 묶어 보여 준다 (FR-MAP-04). 누르면 그 지역으로 확대
            val groups = state.filtered.filter { it.lat != null && it.lng != null }.groupBy { it.sido.ifBlank { "-" } }
            val keepClusters = groups.keys.map { "cluster:$it" }.toSet()
            markers.keys.filter { it !in keepClusters }.forEach { markers.remove(it)?.map = null }
            groups.forEach { (sido, list) ->
                val center = LatLng(list.map { it.lat!! }.average(), list.map { it.lng!! }.average())
                val marker = markers.getOrPut("cluster:$sido") {
                    Marker().apply {
                        icon = MarkerIcons.BLACK
                        this.map = map
                    }
                }
                marker.position = center
                marker.iconTintColor = c.sign.toArgb()
                marker.width = 72
                marker.height = 90
                marker.captionText = "$sido ${list.size}"
                marker.setOnClickListener {
                    map.moveCamera(CameraUpdate.scrollAndZoomTo(center, 10.5).animate(CameraAnimation.Easing))
                    true
                }
            }
            return@LaunchedEffect
        }
        val visible = if (zoomedOut) emptyList() else {
            val b = map.contentBounds
            val center = map.cameraPosition.target
            state.filtered
                .filter { it.lat != null && it.lng != null && b.contains(LatLng(it.lat, it.lng)) }
                .sortedBy { GeoDistance.meters(center.latitude, center.longitude, it.lat!!, it.lng!!) }
                .take(MAX_MARKERS)
        }
        val keep = visible.map { it.id }.toSet()
        markers.keys.filter { it !in keep }.forEach { markers.remove(it)?.map = null }
        // 확대하면 묶음 원은 지우고 가게 마커로 바꾼다
        visible.forEach { s ->
            val years = state.yearsOf(s)
            val gold = StampGradePolicy.gradeFor(years) == StampGrade.GOLD
            val color = if (s.id in state.visitedIds) c.stamp else c.primary
            val icon = PinIcons.pin(color.toArgb(), if (gold) c.brass.toArgb() else null)
            val marker = markers.getOrPut(s.id) {
                Marker().apply {
                    position = LatLng(s.lat!!, s.lng!!)
                    setOnClickListener { vm.select(s.id); true }
                    this.map = map
                }
            }
            marker.icon = icon
            marker.width = if (s.id == state.selectedId) 64 else 44
            marker.height = if (s.id == state.selectedId) 80 else 56
            marker.captionText = if (zoom >= CAPTION_ZOOM) captionYears.format(years, s.name) else ""
        }
        // 가까운 가게 몇 곳은 시안처럼 사진 말풍선으로 바꾼다
        if (zoom >= 12.0) {
            visible.filter { it.photoUrls.isNotEmpty() }.take(BUBBLES).forEach { s ->
                val rating = ratings[s.id]?.average
                val icon = BubbleIcons.load(context, s, rating) ?: return@forEach
                markers[s.id]?.apply {
                    this.icon = icon
                    width = bubbleW
                    height = bubbleH
                    anchor = android.graphics.PointF(64f / BubbleIcons.W, (BubbleIcons.H - 10f) / BubbleIcons.H)
                    captionText = ""
                    zIndex = if (s.id == state.selectedId) 10 else 0
                }
            }
        }
    }
    DisposableEffect(Unit) { onDispose { markers.values.forEach { it.map = null }; markers.clear() } }

    Box(Modifier.fillMaxSize()) {
        NaverMapView(Modifier.fillMaxSize()) { map ->
            naverMap = map
            map.uiSettings.isZoomControlEnabled = false
            map.uiSettings.isLocationButtonEnabled = false
            if (map.cameraPosition.zoom < 6) map.moveCamera(CameraUpdate.scrollAndZoomTo(CITY_HALL, 13.0))
            map.addOnCameraIdleListener { cameraTick++ }
            map.setOnMapClickListener { _, _ -> vm.select(null) }
        }
        authError?.let {
            Text(
                stringResource(R.string.diag_map_auth_failed, it),
                color = c.onStamp,
                modifier = Modifier.align(Alignment.TopCenter).background(c.stamp).padding(NopoTheme.spacing.md),
            )
        }
        Box(
            Modifier.align(Alignment.BottomEnd).padding(end = px(16), bottom = if (state.selected != null) 220.dp else px(40)).size(px(80))
                .shadow(6.dp, CircleShape).clip(CircleShape).background(Color.White)
                .clickable(enabled = !state.locating, role = Role.Button, onClick = onLocate),
            contentAlignment = Alignment.Center,
        ) { MockPic(R.drawable.mk_map_ic_locate, 80, 80) }
        state.selected?.let { s ->
            StoreSheet(
                store = s,
                years = state.yearsOf(s),
                distanceM = state.distanceTo(s),
                visited = s.id in state.visitedIds,
                onOpen = { onOpenStore(s.id) },
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}

/** 단어가 셋 이상이거나 물음표로 끝나면 "AI에게 물어보는 말"로 본다 */
internal fun looksLikeQuestion(q: String): Boolean {
    val t = q.trim()
    return t.length >= 6 && (t.split(Regex("\\s+")).size >= 3 || t.endsWith("?") || t.endsWith("줘") || t.endsWith("요"))
}
