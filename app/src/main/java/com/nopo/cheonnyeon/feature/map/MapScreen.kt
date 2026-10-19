package com.nopo.cheonnyeon.feature.map

import android.Manifest
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.platform.LocalContext
import com.naver.maps.geometry.LatLng
import com.naver.maps.map.CameraAnimation
import com.naver.maps.map.CameraUpdate
import com.naver.maps.map.NaverMap
import com.naver.maps.map.NaverMapSdk
import com.naver.maps.map.overlay.Marker
import com.naver.maps.map.util.MarkerIcons
import com.nopo.cheonnyeon.R
import com.nopo.cheonnyeon.core.designsystem.component.NopoButton
import com.nopo.cheonnyeon.core.designsystem.component.NopoButtonSize
import com.nopo.cheonnyeon.core.designsystem.component.NopoButtonStyle
import com.nopo.cheonnyeon.core.designsystem.theme.NopoTheme
import com.nopo.cheonnyeon.core.util.GeoDistance
import com.nopo.cheonnyeon.domain.catalog.Program
import com.nopo.cheonnyeon.domain.model.StampGrade
import com.nopo.cheonnyeon.domain.rule.StampGradePolicy
import com.nopo.cheonnyeon.platform.map.NaverMapView
import com.nopo.cheonnyeon.platform.permission.PermissionStatus
import com.nopo.cheonnyeon.platform.permission.rememberPermission

private val CITY_HALL = LatLng(37.5665, 126.9780)
private const val MIN_ZOOM_FOR_MARKERS = 9.0
private const val CAPTION_ZOOM = 14.0
private const val MAX_MARKERS = 250

/** 지도 탐색 탭 (S-502): 검색창 + 필터 칩 + 지도/목록 + 하단 시트 */
@Composable
fun MapScreen(vm: MapViewModel, onOpenStore: (String) -> Unit) {
    val state by vm.state.collectAsStateWithLifecycle()
    val c = NopoTheme.colors
    val permission = rememberPermission(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)

    // 권한이 허용되면 내 위치를 한 번 찾는다
    LaunchedEffect(permission.status) {
        if (permission.status == PermissionStatus.GRANTED && state.fix == null) vm.findMe()
    }

    Column(Modifier.fillMaxSize().background(c.paper)) {
        Column(
            Modifier.fillMaxWidth().background(c.sign).statusBarsPadding().padding(NopoTheme.spacing.md),
            verticalArrangement = Arrangement.spacedBy(NopoTheme.spacing.sm),
        ) {
            OutlinedTextField(
                value = state.filter.query,
                onValueChange = vm::setQuery,
                placeholder = { Text(stringResource(R.string.map_search_hint)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().background(c.surface, NopoTheme.shapes.md),
            )
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(NopoTheme.spacing.sm),
            ) {
                Chip(R.string.map_chip_food, state.filter.foodOnly, vm::toggleFood)
                Chip(R.string.map_chip_baeknyeon, state.filter.program == Program.BAEKNYEON) { vm.toggleProgram(Program.BAEKNYEON) }
                Chip(R.string.map_chip_orae, state.filter.program == Program.ORAE) { vm.toggleProgram(Program.ORAE) }
                Chip(R.string.map_chip_unvisited, state.filter.unvisitedOnly, vm::toggleUnvisited)
            }
        }

        Box(Modifier.weight(1f).fillMaxWidth()) {
            if (state.showList) {
                StoreListScreen(state, vm::setSort, onOpenStore)
            } else {
                MapContent(state, vm, onOpenStore, onLocate = {
                    when (permission.status) {
                        PermissionStatus.GRANTED -> vm.findMe()
                        PermissionStatus.PERMANENTLY_DENIED -> permission.openSettings()
                        else -> permission.request()
                    }
                })
            }
            // 지도/목록 전환 (검색어가 있으면 목록 고정)
            if (state.filter.query.isBlank()) {
                NopoButton(
                    text = stringResource(if (state.listMode) R.string.map_show_map else R.string.map_show_list),
                    onClick = vm::toggleListMode,
                    size = NopoButtonSize.Medium,
                    style = NopoButtonStyle.Outline,
                    modifier = Modifier.align(Alignment.TopEnd).padding(NopoTheme.spacing.md),
                )
            }
        }
    }
}

@Composable
private fun Chip(label: Int, selected: Boolean, onClick: () -> Unit) {
    FilterChip(selected = selected, onClick = onClick, label = { Text(stringResource(label)) })
}

@Composable
private fun MapContent(state: MapUiState, vm: MapViewModel, onOpenStore: (String) -> Unit, onLocate: () -> Unit) {
    val context = LocalContext.current
    val c = NopoTheme.colors
    var naverMap by remember { mutableStateOf<NaverMap?>(null) }
    var cameraTick by remember { mutableIntStateOf(0) }
    var authError by rememberSaveable { mutableStateOf<String?>(null) }
    var zoomedOut by remember { mutableStateOf(false) }
    val markers = remember { HashMap<String, Marker>() }
    val captionYears = stringResource(R.string.map_marker_caption)

    DisposableEffect(Unit) {
        val sdk = NaverMapSdk.getInstance(context)
        sdk.onAuthFailedListener = NaverMapSdk.OnAuthFailedListener { e -> authError = e.errorCode }
        onDispose { sdk.onAuthFailedListener = null }
    }

    // 내 위치가 잡히면 표시하고 처음 한 번 이동
    var movedToFix by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(naverMap, state.fix) {
        val map = naverMap ?: return@LaunchedEffect
        val fix = state.fix ?: return@LaunchedEffect
        val here = LatLng(fix.lat, fix.lng)
        map.locationOverlay.apply { isVisible = true; position = here; circleRadius = fix.accuracyM.toInt() }
        if (!movedToFix) {
            movedToFix = true
            map.moveCamera(CameraUpdate.scrollAndZoomTo(here, 14.0).animate(CameraAnimation.Easing))
        }
    }

    // 화면 영역 안의 가게만 마커로 그린다 (FR-MAP-04). 카메라가 멈추거나 필터가 바뀌면 다시 계산
    LaunchedEffect(naverMap, state.filtered, state.visitedIds, cameraTick, state.selectedId) {
        val map = naverMap ?: return@LaunchedEffect
        val zoom = map.cameraPosition.zoom
        zoomedOut = zoom < MIN_ZOOM_FOR_MARKERS
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
        visible.forEach { s ->
            val years = state.yearsOf(s)
            val color = when {
                s.id in state.visitedIds -> c.stamp
                StampGradePolicy.gradeFor(years) == StampGrade.GOLD -> c.brass
                else -> c.primary
            }
            val marker = markers.getOrPut(s.id) {
                Marker().apply {
                    icon = MarkerIcons.BLACK
                    position = LatLng(s.lat!!, s.lng!!)
                    setOnClickListener { vm.select(s.id); true }
                    this.map = map
                }
            }
            marker.iconTintColor = color.toArgb()
            marker.width = if (s.id == state.selectedId) 64 else 44
            marker.height = if (s.id == state.selectedId) 80 else 56
            marker.captionText = if (zoom >= CAPTION_ZOOM) captionYears.format(years, s.name) else ""
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
        Column(Modifier.align(Alignment.TopStart).padding(NopoTheme.spacing.md)) {
            YearsPill(state.totalYears)
        }
        if (zoomedOut) {
            Text(
                stringResource(R.string.map_zoom_in_hint),
                color = c.onPrimary,
                modifier = Modifier.align(Alignment.Center).background(c.primary.copy(alpha = 0.85f), NopoTheme.shapes.md)
                    .padding(horizontal = NopoTheme.spacing.lg, vertical = NopoTheme.spacing.sm),
            )
        }
        authError?.let {
            Text(
                stringResource(R.string.diag_map_auth_failed, it),
                color = c.onStamp,
                modifier = Modifier.align(Alignment.TopCenter).background(c.stamp).padding(NopoTheme.spacing.md),
            )
        }
        NopoButton(
            text = stringResource(if (state.locating) R.string.map_locating else R.string.map_my_location),
            onClick = onLocate,
            size = NopoButtonSize.Medium,
            style = NopoButtonStyle.Outline,
            enabled = !state.locating,
            modifier = Modifier.align(Alignment.BottomEnd).padding(end = NopoTheme.spacing.md, bottom = if (state.selected != null) 220.dp else NopoTheme.spacing.md),
        )
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

@Composable
private fun YearsPill(totalYears: Int) {
    val c = NopoTheme.colors
    Text(
        stringResource(R.string.map_my_years, totalYears),
        style = NopoTheme.extraTypography.metric,
        color = c.primary,
        modifier = Modifier.background(c.surface, NopoTheme.shapes.md).padding(horizontal = NopoTheme.spacing.md, vertical = NopoTheme.spacing.xs),
    )
}


