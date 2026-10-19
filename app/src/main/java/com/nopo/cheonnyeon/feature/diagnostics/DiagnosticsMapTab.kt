package com.nopo.cheonnyeon.feature.diagnostics

import android.Manifest
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.naver.maps.geometry.LatLng
import com.naver.maps.map.CameraAnimation
import com.naver.maps.map.CameraUpdate
import com.naver.maps.map.NaverMap
import com.naver.maps.map.NaverMapSdk
import com.naver.maps.map.overlay.Marker
import com.nopo.cheonnyeon.BuildConfig
import com.nopo.cheonnyeon.R
import com.nopo.cheonnyeon.core.designsystem.component.NopoButton
import com.nopo.cheonnyeon.core.designsystem.component.NopoButtonSize
import com.nopo.cheonnyeon.core.designsystem.component.NopoButtonStyle
import com.nopo.cheonnyeon.core.designsystem.theme.NopoTheme
import com.nopo.cheonnyeon.platform.location.Fix
import com.nopo.cheonnyeon.platform.location.FusedLocationAdapter
import com.nopo.cheonnyeon.platform.location.LocationSettingsResult
import com.nopo.cheonnyeon.platform.map.AddressResult
import com.nopo.cheonnyeon.platform.map.NaverMapView
import com.nopo.cheonnyeon.platform.map.ReverseGeocodeError
import com.nopo.cheonnyeon.platform.map.ReverseGeocodeException
import com.nopo.cheonnyeon.platform.map.ReverseGeocoder
import com.nopo.cheonnyeon.platform.permission.PermissionStatus
import com.nopo.cheonnyeon.platform.permission.rememberPermission
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.random.Random
import kotlin.time.TimeSource

data class DiagMapState(
    val locating: Boolean = false,
    val progressAccuracy: Double? = null,
    val fix: Fix? = null,
    val addressLoading: Boolean = false,
    val address: AddressResult? = null,
    val addressError: String? = null,
    val settingsOff: Boolean = false,
    val timedOut: Boolean = false,
)

/** 내 위치: 최선 좌표 → 도로명 주소. 좌표는 화면에만 보이고 저장·로그하지 않는다. */
class DiagMapViewModel(
    private val location: FusedLocationAdapter,
    private val geocoder: ReverseGeocoder,
) : ViewModel() {
    private val _state = MutableStateFlow(DiagMapState())
    val state = _state.asStateFlow()

    fun findMyLocation() {
        if (_state.value.locating || _state.value.addressLoading) return
        viewModelScope.launch {
            if (location.checkSettings() != LocationSettingsResult.Ok) {
                _state.update { it.copy(settingsOff = true) }
                return@launch
            }
            _state.value = DiagMapState(locating = true)
            val fix = location.bestFix { best -> _state.update { it.copy(progressAccuracy = best.accuracyM) } }
            if (fix == null) {
                _state.update { it.copy(locating = false, timedOut = true) }
                return@launch
            }
            _state.update { it.copy(locating = false, fix = fix, addressLoading = true) }
            val result = geocoder.lookup(fix.lat, fix.lng)
            _state.update {
                it.copy(
                    addressLoading = false,
                    address = result.getOrNull(),
                    addressError = (result.exceptionOrNull() as? ReverseGeocodeException)?.error?.let { e ->
                        when (e) {
                            is ReverseGeocodeError.Http -> "HTTP ${e.code}"
                            ReverseGeocodeError.Network -> "NETWORK"
                            ReverseGeocodeError.Timeout -> "TIMEOUT"
                        }
                    },
                )
            }
        }
    }
}

/** SPK-1 지도 탭 + 내 위치(좌표·도로명 주소). */
@Composable
fun DiagnosticsMapTab(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val appContext = context.applicationContext
    val viewModel: DiagMapViewModel = viewModel {
        DiagMapViewModel(
            FusedLocationAdapter(appContext),
            ReverseGeocoder(BuildConfig.NAVER_MAP_KEY_ID, BuildConfig.NAVER_MAP_CLIENT_SECRET),
        )
    }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val locationPermission = rememberPermission(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)

    var naverMap by remember { mutableStateOf<NaverMap?>(null) }
    var authError by rememberSaveable { mutableStateOf<String?>(null) }
    val shown = remember { TimeSource.Monotonic.markNow() }
    var readyMs by rememberSaveable { mutableStateOf<Long?>(null) }
    var markerInfo by rememberSaveable { mutableStateOf<String?>(null) }
    val bulkMarkers = remember { mutableListOf<Marker>() }
    val markerFmt = stringResource(R.string.diag_map_markers_done)
    val cityHallCaption = stringResource(R.string.diag_map_city_hall)

    // 인증 실패(401/429/800 등)를 화면에 표시 (아키텍처 5.6)
    DisposableEffect(Unit) {
        val sdk = NaverMapSdk.getInstance(context)
        sdk.onAuthFailedListener = NaverMapSdk.OnAuthFailedListener { e -> authError = e.errorCode }
        onDispose { sdk.onAuthFailedListener = null }
    }

    // 위치가 잡히면 지도 이동 + 내 위치 표시
    DisposableEffect(naverMap, state.fix) {
        val map = naverMap
        val fix = state.fix
        if (map != null && fix != null) {
            val here = LatLng(fix.lat, fix.lng)
            map.locationOverlay.apply {
                isVisible = true
                position = here
                circleRadius = fix.accuracyM.toInt()
            }
            map.moveCamera(CameraUpdate.scrollAndZoomTo(here, 17.0).animate(CameraAnimation.Easing))
        }
        onDispose { }
    }

    Column(modifier = modifier.fillMaxSize()) {
        Box(Modifier.weight(1f).fillMaxWidth()) {
            NaverMapView(Modifier.fillMaxSize()) { map ->
                naverMap = map
                // 처음 열 때만 서울시청으로 이동. 회전 뒤에는 MapView가 복원한 카메라 위치를 유지
                val firstOpen = readyMs == null
                if (firstOpen) readyMs = shown.elapsedNow().inWholeMilliseconds
                map.uiSettings.isZoomControlEnabled = true
                Marker().apply {
                    position = CITY_HALL
                    captionText = cityHallCaption
                    this.map = map
                }
                if (firstOpen && state.fix == null) map.moveCamera(CameraUpdate.scrollTo(CITY_HALL))
            }
            authError?.let {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .background(NopoTheme.colors.stampSoft)
                        .padding(NopoTheme.spacing.md),
                ) {
                    Text(stringResource(R.string.diag_map_auth_failed, it), style = NopoTheme.typography.titleSmall, color = NopoTheme.colors.ink)
                    Text(stringResource(R.string.diag_map_auth_hint), style = NopoTheme.typography.bodySmall, color = NopoTheme.colors.ink2)
                }
            }
        }

        Column(
            Modifier
                .fillMaxWidth()
                .background(NopoTheme.colors.surface)
                .padding(NopoTheme.spacing.lg),
            verticalArrangement = Arrangement.spacedBy(NopoTheme.spacing.sm),
        ) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(NopoTheme.spacing.sm),
                verticalArrangement = Arrangement.spacedBy(NopoTheme.spacing.sm),
            ) {
                NopoButton(
                    text = if (state.locating) {
                        stringResource(R.string.diag_map_locating, state.progressAccuracy?.let { "%.0f".format(it) } ?: "-")
                    } else {
                        stringResource(R.string.diag_map_my_location)
                    },
                    onClick = {
                        if (locationPermission.status == PermissionStatus.GRANTED) viewModel.findMyLocation()
                        else if (locationPermission.status == PermissionStatus.PERMANENTLY_DENIED) locationPermission.openSettings()
                        else locationPermission.request()
                    },
                    size = NopoButtonSize.Medium,
                    style = NopoButtonStyle.Stamp,
                    enabled = !state.locating && !state.addressLoading,
                )
                NopoButton(
                    text = stringResource(if (bulkMarkers.isEmpty()) R.string.diag_map_markers_1000 else R.string.diag_map_markers_clear),
                    onClick = {
                        val map = naverMap ?: return@NopoButton
                        if (bulkMarkers.isNotEmpty()) {
                            bulkMarkers.forEach { it.map = null }
                            bulkMarkers.clear()
                            markerInfo = null
                        } else {
                            val mark = TimeSource.Monotonic.markNow()
                            val rnd = Random(42)
                            repeat(1_000) {
                                bulkMarkers += Marker().apply {
                                    position = LatLng(37.45 + rnd.nextDouble() * 0.25, 126.80 + rnd.nextDouble() * 0.35)
                                    this.map = map
                                }
                            }
                            markerInfo = markerFmt.format(bulkMarkers.size, mark.elapsedNow().inWholeMilliseconds.toInt())
                        }
                    },
                    size = NopoButtonSize.Medium,
                    style = NopoButtonStyle.Outline,
                    enabled = naverMap != null,
                )
            }
            readyMs?.let { Text(stringResource(R.string.diag_map_ready, it.toInt()), style = NopoTheme.extraTypography.metric, color = NopoTheme.colors.ink2) }
            markerInfo?.let { Text(it, style = NopoTheme.extraTypography.metric, color = NopoTheme.colors.ink2) }
            LocationResult(state)
        }
    }
}

@Composable
private fun LocationResult(state: DiagMapState) {
    val c = NopoTheme.colors
    if (state.settingsOff) Text(stringResource(R.string.diag_loc_settings_off), color = c.stamp, style = NopoTheme.typography.bodyMedium)
    if (state.timedOut) Text(stringResource(R.string.diag_loc_timeout), color = c.stamp, style = NopoTheme.typography.bodyMedium)
    val fix = state.fix ?: return
    Text(stringResource(R.string.diag_map_latlng, fix.lat, fix.lng, fix.accuracyM), style = NopoTheme.extraTypography.metric, color = c.ink)
    if (fix.isMock) Text(stringResource(R.string.diag_loc_mock), style = NopoTheme.typography.labelLarge, color = c.stamp)
    when {
        state.addressLoading -> Text(stringResource(R.string.diag_map_addr_loading), style = NopoTheme.typography.bodyMedium, color = c.ink2)
        state.addressError != null -> Text(stringResource(R.string.diag_map_addr_error, state.addressError), style = NopoTheme.typography.bodyMedium, color = c.stamp)
        state.address != null -> {
            val a = state.address
            Text(
                a.road?.let { stringResource(R.string.diag_map_road, it) } ?: stringResource(R.string.diag_map_no_road),
                style = NopoTheme.typography.titleSmall,
                color = c.ink,
            )
            a.building?.let { Text(stringResource(R.string.diag_map_building, it), style = NopoTheme.typography.bodyMedium, color = c.ink2) }
            a.jibun?.let { Text(stringResource(R.string.diag_map_jibun, it), style = NopoTheme.typography.bodyMedium, color = c.ink2) }
        }
    }
}

private val CITY_HALL = LatLng(37.5663, 126.9779)
