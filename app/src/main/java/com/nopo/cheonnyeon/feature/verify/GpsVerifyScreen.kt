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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import com.nopo.cheonnyeon.platform.permission.PermissionHandle
import com.nopo.cheonnyeon.platform.permission.PermissionStatus

private val Hairline = Color(0xFFE2DDCB)
private val Gray = Color(0xFF4B5563)
private val Red = Color(0xFFD23B2E)

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
    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
      Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
        MockStage(originY = 84, heightPx = 1430) {
            MockPic(R.drawable.mk2_v_hdr, 460, 117, Modifier.at(20, 88))
            MockPic(R.drawable.mk_ic_search, 54, 52, Modifier.at(604, 90))
            MockPic(R.drawable.mk_ic_bell, 54, 56, Modifier.at(682, 88))
            MockPic(R.drawable.mk2_g_head, 760, 160, Modifier.at(0, 205))
            Box(Modifier.at(15, 220).size(px(70), px(70)).clickable(role = Role.Button, onClick = vm::backToReceipt).semantics { contentDescription = "뒤로" })
            MockPic(R.drawable.mk2_g_map, 733, 444, Modifier.at(15, 378))
            if (store != null) {
                // 지도 위 말풍선(시안의 예시 말풍선을 덮는다)
                Box(Modifier.at(98, 430).size(px(322), px(126)).clip(RoundedCornerShape(px(22))).background(Color(0xFFFCFAF4))) {
                    Box(Modifier.padding(start = px(10), top = px(12)).size(px(100)).clip(CircleShape)) { StorePhoto(store, Modifier.fillMaxSize()) }
                    MockText(store.name, 22, Modifier.padding(start = px(122), top = px(14)).width(px(190)), font = MockSerif, weight = FontWeight.ExtraBold)
                    Row(Modifier.padding(start = px(122), top = px(50)), verticalAlignment = Alignment.CenterVertically) {
                        if (rating != null) {
                            StarIcon(24); Box(Modifier.width(px(6))); MockText("%.1f (%,d)".format(rating.average, rating.count), 20)
                        }
                    }
                    Row(Modifier.padding(start = px(122), top = px(86)), verticalAlignment = Alignment.CenterVertically) {
                        PinIcon(22); Box(Modifier.width(px(8))); MockText(dText, 20, weight = FontWeight.Medium)
                    }
                }
                Box(Modifier.at(452, 602).size(px(122), px(72)).clip(RoundedCornerShape(px(14))).background(MockColors.Navy), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        MockText("현재 위치", 19, color = Color.White); MockText(dText, 21, color = Color.White, weight = FontWeight.Bold)
                    }
                }
                // 상태 카드
                Box(Modifier.at(15, 840).size(px(733), px(555)).clip(RoundedCornerShape(px(28))).background(Color(0xFFFCFAF4)).border(1.dp, Hairline, RoundedCornerShape(px(28))))
                Box(Modifier.at(40, 864).size(px(226), px(262)).clip(RoundedCornerShape(px(18)))) { StorePhoto(store, Modifier.fillMaxSize()) }
                MockPic(R.drawable.mk2_g_ic_pin, 40, 48, Modifier.at(292, 870))
                MockText(title, 24, Modifier.at(350, 872).width(px(380)), weight = FontWeight.ExtraBold, maxLines = 1)
                MockText(sub, 19, Modifier.at(350, 912).width(px(380)), color = if (err != null) Red else Gray, maxLines = 2, lineHeightPx = 25)
                MockText(store.name, 32, Modifier.at(292, 950).width(px(430)), font = MockSerif, weight = FontWeight.ExtraBold)
                MockText(store.summary?.takeIf { it.isNotBlank() } ?: store.menuItems.take(3).joinToString(", "), 20, Modifier.at(292, 1008).width(px(440)), color = Gray)
                Row(Modifier.at(292, 1048), horizontalArrangement = Arrangement.spacedBy(px(10))) {
                    listOfNotNull(store.categoryGroup.takeIf { it.isNotBlank() }, store.foundedYear?.let { "${it / 10 * 10}년대" }, store.sigungu.takeIf { it.isNotBlank() }).forEach { t ->
                        Box(Modifier.clip(RoundedCornerShape(50)).background(Color(0xFFEFEBDD)).padding(horizontal = px(16), vertical = px(6))) { MockText(t, 19, color = Gray) }
                    }
                }
                Row(Modifier.at(292, 1100), verticalAlignment = Alignment.CenterVertically) {
                    if (rating != null) {
                        StarIcon(28); Box(Modifier.width(px(8))); MockText("%.1f (%,d)".format(rating.average, rating.count), 24, weight = FontWeight.Medium)
                        Box(Modifier.padding(horizontal = px(18)).width(1.dp).height(px(30)).background(Color(0xFFDDD8C6)))
                    }
                    PinIcon(26); Box(Modifier.width(px(8))); MockText(dText, 24, weight = FontWeight.Medium)
                }
                MockPic(R.drawable.mk2_g_steps, 705, 182, Modifier.at(30, 1170))
            }
            MockPic(R.drawable.mk2_g_info, 733, 103, Modifier.at(15, 1405))
        }
      }
      GpsButtons(vm, state, granted, ok, locationPermission)
    }
}

@Composable
private fun GpsButtons(vm: VerifyViewModel, state: VerifyUiState, granted: Boolean, ok: Boolean, locationPermission: PermissionHandle) {
    Row(Modifier.padding(horizontal = px(15), vertical = px(10)), horizontalArrangement = Arrangement.spacedBy(px(14))) {
            val label = when {
                state.busy -> "확인 중…"
                !granted -> "위치 권한 허용"
                state.distanceM == null -> "위치 확인"
                else -> "다시 확인"
            }
            Box(
                Modifier.weight(1f).height(px(84)).clip(RoundedCornerShape(px(26))).background(Color(0xFFFCFAF4)).border(1.dp, Hairline, RoundedCornerShape(px(26)))
                    .clickable(enabled = !state.busy, role = Role.Button) {
                        if (!granted) {
                            if (locationPermission.status == PermissionStatus.PERMANENTLY_DENIED) locationPermission.openSettings() else locationPermission.request()
                        } else vm.measureLocation(permissionGranted = true, approximateOnly = locationPermission.approximateOnly)
                    },
                contentAlignment = Alignment.Center,
            ) { MockText(label, 28, weight = FontWeight.ExtraBold) }
            Box(
                Modifier.weight(1f).height(px(84)).clip(RoundedCornerShape(px(26))).background(if (ok) MockColors.Lime2 else Color(0xFFE2E8C4))
                    .clickable(enabled = ok, role = Role.Button, onClick = vm::startPhotoStep),
                contentAlignment = Alignment.Center,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) { PinIcon(34); Box(Modifier.width(px(12))); MockText("방문 인증 시작", 28, weight = FontWeight.ExtraBold) }
            }
    }
}
