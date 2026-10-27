package com.nopo.cheonnyeon.feature.verify

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.naver.maps.geometry.LatLng
import com.naver.maps.geometry.LatLngBounds
import com.naver.maps.map.CameraAnimation
import com.naver.maps.map.CameraUpdate
import com.naver.maps.map.NaverMap
import com.naver.maps.map.overlay.Marker
import com.naver.maps.map.util.MarkerIcons
import com.nopo.cheonnyeon.R
import com.nopo.cheonnyeon.core.common.message
import com.nopo.cheonnyeon.core.designsystem.mock.MockColors
import com.nopo.cheonnyeon.core.designsystem.mock.MockPic
import com.nopo.cheonnyeon.core.designsystem.mock.MockSerif
import com.nopo.cheonnyeon.core.designsystem.mock.MockStage
import com.nopo.cheonnyeon.core.designsystem.mock.MockText
import com.nopo.cheonnyeon.core.designsystem.mock.PinIcon
import com.nopo.cheonnyeon.core.designsystem.mock.StarIcon
import com.nopo.cheonnyeon.core.designsystem.mock.at
import com.nopo.cheonnyeon.core.designsystem.mock.px
import com.nopo.cheonnyeon.feature.store.StorePhoto
import com.nopo.cheonnyeon.platform.map.NaverMapView
import com.nopo.cheonnyeon.platform.permission.PermissionHandle
import com.nopo.cheonnyeon.platform.permission.PermissionStatus

private val Hairline = Color(0xFFE2DDCB)
private val Gray = Color(0xFF4B5563)
private val Red = Color(0xFFD23B2E)
private val CardBackground = Color(0xFFFCFAF4)

/** 시안 21 "GPS 방문 인증": 현재 위치와 가게 사이의 거리를 확인하고 "방문 인증 시작"으로 다음 단계(가게 사진)로 간다. */
@Composable
internal fun GpsVerifyScreen(vm: VerifyViewModel, state: VerifyUiState, locationPermission: PermissionHandle) {
    val context = LocalContext.current
    val store = state.store
    val ratings by vm.ratings.collectAsStateWithLifecycle()
    val rating = store?.let { ratings[it.id] }
    val granted = locationPermission.status == PermissionStatus.GRANTED
    val dist = state.distanceM
    val dText = dist?.let { if (it >= 1000) "%.1fkm".format(it / 1000.0) else "${it}m" } ?: "-"
    val ok = state.withinRange && state.error == null
    val err = state.error
    val title = when {
        state.busy -> "현재 위치 확인 중…"
        err != null -> "위치를 확인하지 못했어요"
        ok -> "현재 위치를 확인했어요!"
        else -> "위치를 확인해 주세요"
    }
    val sub = when {
        state.busy -> stringResource(R.string.verify_locating, state.locatingBest?.let { "%.0f".format(it) } ?: "-")
        err != null -> err.message(context, state.errorMeters)
        ok -> "선택한 노포 근처에 있습니다."
        else -> "아래 '위치 확인'을 눌러 주세요."
    }
    // 가게와 멀거나 위치를 못 찾았을 때는 빨간 큰 글씨 팝업으로 알린다
    var dismissedKey by rememberSaveable { mutableStateOf<String?>(null) }
    val errKey = err?.let { it.code + "|" + state.distanceM }
    if (err != null && !state.busy && errKey != dismissedKey) {
        FarAwayDialog(err.message(context, state.errorMeters)) { dismissedKey = errKey }
    }
    var needLocation by rememberSaveable { mutableStateOf(false) }
    if (needLocation) NeedLocationDialog { needLocation = false }
    // 위치를 재는 동안(최대 10초) 안내 창을 띄운다. 끝나면 저절로 닫힌다
    if (state.busy) LocatingDialog()
    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            MockStage(originY = 84, heightPx = 1530) {
                MockPic(R.drawable.mk2_v_hdr, 460, 117, Modifier.at(20, 88))
                MockPic(R.drawable.mk_ic_search, 54, 52, Modifier.at(604, 90))
                MockPic(R.drawable.mk_ic_bell, 54, 56, Modifier.at(682, 88))
                MockPic(R.drawable.mk2_g_head, 760, 160, Modifier.at(0, 205))
                Box(
                    Modifier.at(15, 220).size(px(70), px(70)).clickable(role = Role.Button, onClick = vm::backToReceipt)
                        .semantics { contentDescription = "뒤로" },
                )
                // 실제 지도: 가게(연두 마커)와 내 위치(파란 마커). 아래 카드를 스크롤할 수 있게 지도는 손으로 움직이지 않는다
                if (store?.lat != null && store.lng != null) {
                    Box(Modifier.at(15, 378).size(px(733), px(444)).clip(RoundedCornerShape(px(28)))) {
                        VerifyMap(store.lat, store.lng, state.myLat, state.myLng)
                    }
                } else {
                    MockPic(R.drawable.mk2_g_map, 733, 444, Modifier.at(15, 378))
                }
                if (store != null) {
                    // 지도 위 말풍선(시안의 예시 말풍선을 덮는다)
                    if (state.myLat == null) {
                        Box(
                            Modifier.at(34, 396).size(px(322), px(126)).clip(RoundedCornerShape(px(22))).background(CardBackground),
                        ) {
                            Box(Modifier.padding(start = px(10), top = px(12)).size(px(100)).clip(CircleShape)) {
                                StorePhoto(store, Modifier.fillMaxSize())
                            }
                            MockText(
                                store.name, 22, Modifier.padding(start = px(122), top = px(14)).width(px(190)),
                                font = MockSerif, weight = FontWeight.ExtraBold,
                            )
                            Row(Modifier.padding(start = px(122), top = px(50)), verticalAlignment = Alignment.CenterVertically) {
                                if (rating != null) {
                                    StarIcon(24)
                                    Box(Modifier.width(px(6)))
                                    MockText("%.1f (%,d)".format(rating.average, rating.count), 20)
                                }
                            }
                            Row(Modifier.padding(start = px(122), top = px(86)), verticalAlignment = Alignment.CenterVertically) {
                                PinIcon(22)
                                Box(Modifier.width(px(8)))
                                MockText(dText, 20, weight = FontWeight.Medium)
                            }
                        }
                    }
                    Box(
                        Modifier.at(600, 735).size(px(122), px(72)).clip(RoundedCornerShape(px(14))).background(MockColors.Navy),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            MockText(stringResource(R.string.gps_01), 19, color = Color.White)
                            MockText(dText, 21, color = Color.White, weight = FontWeight.Bold)
                        }
                    }
                    // 상태 카드
                    Box(
                        Modifier.at(15, 840).size(px(733), px(555)).clip(RoundedCornerShape(px(28))).background(CardBackground)
                            .border(1.dp, Hairline, RoundedCornerShape(px(28))),
                    )
                    Box(Modifier.at(40, 864).size(px(226), px(262)).clip(RoundedCornerShape(px(18)))) {
                        StorePhoto(store, Modifier.fillMaxSize())
                    }
                    MockPic(R.drawable.mk2_g_ic_pin, 40, 48, Modifier.at(292, 870))
                    MockText(title, 24, Modifier.at(350, 872).width(px(380)), weight = FontWeight.ExtraBold, maxLines = 1)
                    MockText(
                        sub, 19, Modifier.at(350, 912).width(px(380)),
                        color = if (err != null) Red else Gray, maxLines = 2, lineHeightPx = 25,
                    )
                    MockText(store.name, 32, Modifier.at(292, 968).width(px(430)), font = MockSerif, weight = FontWeight.ExtraBold)
                    MockText(
                        store.summary?.takeIf { it.isNotBlank() } ?: store.menuItems.take(3).joinToString(", "),
                        20, Modifier.at(292, 1020).width(px(440)), color = Gray,
                    )
                    Row(Modifier.at(292, 1060), horizontalArrangement = Arrangement.spacedBy(px(10))) {
                        val tags = listOfNotNull(
                            store.categoryGroup.takeIf { it.isNotBlank() },
                            store.foundedYear?.let { "${it / 10 * 10}년대" },
                            store.sigungu.takeIf { it.isNotBlank() },
                        )
                        tags.forEach { t ->
                            Box(
                                Modifier.clip(RoundedCornerShape(50)).background(Color(0xFFEFEBDD))
                                    .padding(horizontal = px(16), vertical = px(6)),
                            ) { MockText(t, 19, color = Gray) }
                        }
                    }
                    Row(Modifier.at(292, 1110), verticalAlignment = Alignment.CenterVertically) {
                        if (rating != null) {
                            StarIcon(28)
                            Box(Modifier.width(px(8)))
                            MockText("%.1f (%,d)".format(rating.average, rating.count), 24, weight = FontWeight.Medium)
                            Box(Modifier.padding(horizontal = px(18)).width(1.dp).height(px(30)).background(Color(0xFFDDD8C6)))
                        }
                        PinIcon(26)
                        Box(Modifier.width(px(8)))
                        MockText(dText, 24, weight = FontWeight.Medium)
                    }
                    MockPic(R.drawable.mk2_g_steps, 705, 204, Modifier.at(30, 1170))
                }
                MockPic(R.drawable.mk2_g_info, 733, 103, Modifier.at(15, 1405))
            }
        }
        GpsButtons(vm, state, granted, ok, locationPermission, onBlocked = { needLocation = true })
    }
}

/** 가게와 멀거나 위치를 못 찾았을 때 뜨는 빨간 큰 글씨 안내 */
@Composable
private fun FarAwayDialog(message: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        text = { Text(message, color = Red, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 32.sp) },
        confirmButton = { TextButton(onDismiss) { Text(stringResource(R.string.ui_gpsverify_01), fontSize = 18.sp) } },
    )
}

/** 위치를 확인하기 전에 "방문 인증 시작"을 눌렀을 때의 안내 */
@Composable
private fun NeedLocationDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        text = { Text(stringResource(R.string.ui_gpsverify_02)) },
        confirmButton = { TextButton(onDismiss) { Text(stringResource(R.string.ui_gpsverify_01)) } },
    )
}

/** 위치를 재는 동안 보이는 진행 안내(닫을 수 없다) */
@Composable
private fun LocatingDialog() {
    AlertDialog(
        onDismissRequest = {},
        text = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(Modifier.size(28.dp), color = MockColors.LimeDeep, strokeWidth = 3.dp)
                Box(Modifier.width(16.dp))
                Text(stringResource(R.string.ui_gpsverify_03))
            }
        },
        confirmButton = {},
    )
}

@Composable
private fun GpsButtons(
    vm: VerifyViewModel,
    state: VerifyUiState,
    granted: Boolean,
    ok: Boolean,
    locationPermission: PermissionHandle,
    onBlocked: () -> Unit,
) {
    Row(Modifier.padding(horizontal = px(15), vertical = px(10)), horizontalArrangement = Arrangement.spacedBy(px(14))) {
        val label = when {
            state.busy -> "확인 중…"
            !granted -> "위치 권한 허용"
            state.distanceM == null -> "위치 확인"
            else -> "다시 확인"
        }
        Box(
            Modifier.weight(1f).height(px(84)).clip(RoundedCornerShape(px(26)))
                .background(if (ok) CardBackground else MockColors.Lime2)
                .border(1.dp, if (ok) Hairline else MockColors.Lime2, RoundedCornerShape(px(26)))
                .clickable(enabled = !state.busy, role = Role.Button) {
                    if (!granted) {
                        if (locationPermission.status == PermissionStatus.PERMANENTLY_DENIED) {
                            locationPermission.openSettings()
                        } else {
                            locationPermission.request()
                        }
                    } else {
                        vm.measureLocation(permissionGranted = true, approximateOnly = locationPermission.approximateOnly)
                    }
                },
            contentAlignment = Alignment.Center,
        ) { MockText(label, 28, weight = FontWeight.ExtraBold) }
        Box(
            Modifier.weight(1f).height(px(84)).clip(RoundedCornerShape(px(26)))
                .background(if (ok) MockColors.Lime2 else Color(0xFFE2E8C4))
                .clickable(enabled = !state.busy, role = Role.Button) { if (ok) vm.startPhotoStep() else onBlocked() },
            contentAlignment = Alignment.Center,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PinIcon(34)
                Box(Modifier.width(px(12)))
                MockText(stringResource(R.string.gps_02), 28, weight = FontWeight.ExtraBold)
            }
        }
    }
}

/** 인증 화면의 작은 실제 지도(손으로는 움직이지 않음). 가게 위치와, 잰 뒤에는 내 위치도 보여 둘이 한눈에 들어오게 한다 */
@Composable
private fun VerifyMap(storeLat: Double, storeLng: Double, myLat: Double?, myLng: Double?) {
    var map by remember { mutableStateOf<NaverMap?>(null) }
    val storeMarker = remember { Marker() }
    val myMarker = remember { Marker() }
    DisposableEffect(Unit) { onDispose { storeMarker.map = null; myMarker.map = null } }
    LaunchedEffect(map, myLat, myLng) {
        val m = map ?: return@LaunchedEffect
        val store = LatLng(storeLat, storeLng)
        storeMarker.position = store
        storeMarker.icon = MarkerIcons.BLACK
        storeMarker.iconTintColor = 0xFFBDF642.toInt()
        storeMarker.map = m
        if (myLat != null && myLng != null) {
            val me = LatLng(myLat, myLng)
            myMarker.position = me
            myMarker.icon = MarkerIcons.BLACK
            myMarker.iconTintColor = 0xFF2F6BFF.toInt()
            myMarker.map = m
            val bounds = LatLngBounds.Builder().include(store).include(me).build()
            m.moveCamera(CameraUpdate.fitBounds(bounds, 120).animate(CameraAnimation.None))
            if (m.cameraPosition.zoom > 17.5) m.moveCamera(CameraUpdate.zoomTo(17.5))
        } else {
            myMarker.map = null
            m.moveCamera(CameraUpdate.scrollAndZoomTo(store, 16.0))
        }
    }
    NaverMapView(Modifier.fillMaxSize()) { m ->
        m.uiSettings.isScrollGesturesEnabled = false
        m.uiSettings.isZoomGesturesEnabled = false
        m.uiSettings.isTiltGesturesEnabled = false
        m.uiSettings.isRotateGesturesEnabled = false
        m.uiSettings.isZoomControlEnabled = false
        m.uiSettings.isLocationButtonEnabled = false
        map = m
    }
}
