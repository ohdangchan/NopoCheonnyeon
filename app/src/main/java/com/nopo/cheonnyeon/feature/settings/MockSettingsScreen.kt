package com.nopo.cheonnyeon.feature.settings

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nopo.cheonnyeon.R
import com.nopo.cheonnyeon.core.designsystem.mock.MockColors
import com.nopo.cheonnyeon.core.designsystem.mock.MockPic
import com.nopo.cheonnyeon.core.designsystem.mock.MockSerif
import com.nopo.cheonnyeon.core.designsystem.mock.MockStage
import com.nopo.cheonnyeon.core.designsystem.mock.MockText
import com.nopo.cheonnyeon.core.designsystem.mock.at
import com.nopo.cheonnyeon.core.designsystem.mock.px
import com.nopo.cheonnyeon.feature.my.MyViewModel
import com.nopo.cheonnyeon.feature.my.SupportDialog
import com.nopo.cheonnyeon.feature.passport.PassportViewModel

/**
 * 시안 "설정"(12.webp) 복제. 프로필 카드와 일곱 줄 메뉴, 로그아웃, 회원 탈퇴.
 * 개인정보·계정 관련 자세한 기능(닉네임 변경, 공개 범위, 탈퇴, 데이터 출처, 라이선스)은 [onLegacy] 의 기존 설정 화면에 그대로 있다.
 */
@Composable
fun MockSettingsScreen(
    myVm: MyViewModel, passportVm: PassportViewModel,
    onNotify: () -> Unit, onLegacy: () -> Unit, onPermissions: () -> Unit, onLogout: () -> Unit, onBell: () -> Unit, onBack: () -> Unit,
) {
    val context = LocalContext.current
    val state by passportVm.state.collectAsStateWithLifecycle()
    val saved by myVm.saved.collectAsStateWithLifecycle()
    val photoVersion by myVm.photoVersion.collectAsStateWithLifecycle()
    val reviews by passportVm.reviews.collectAsStateWithLifecycle()
    androidx.compose.runtime.LaunchedEffect(Unit) { passportVm.loadReviews() }
    val nickname by produceState("") { value = passportVm.nickname() }
    val real = remember(state.stamps) { state.stamps.filter { !it.isTest } }
    val titles = com.nopo.cheonnyeon.domain.rule.TitlePolicy.titleFor(state.totalYears).ordinal + 1
    var dialog by rememberSaveable { mutableIntStateOf(0) }
    var support by rememberSaveable { mutableStateOf(false) }
    androidx.activity.compose.BackHandler(onBack = onBack)

    Column(Modifier.fillMaxSize().background(MockColors.Cream).statusBarsPadding().verticalScroll(rememberScrollState())) {
        MockStage(originY = 84, heightPx = 1480) {
            MockPic(R.drawable.mk2_v_hdr, 460, 117, Modifier.at(20, 88))
            Box(Modifier.at(652, 98).size(px(76)).clickable(role = Role.Button, onClick = onBack), contentAlignment = Alignment.Center) { /* 시안의 메뉴 줄 아이콘 자리: 뒤로 */ }
            MockPic(R.drawable.mk2_a_title, 410, 105, Modifier.at(20, 225))
            // 프로필 카드
            MockPic(R.drawable.mk2_a_profile, 736, 262, Modifier.at(12, 338))
            Box(Modifier.at(12, 338).size(px(736), px(170)).clickable(role = Role.Button, onClick = onLegacy))
            Box(Modifier.at(38, 365).size(px(128)).clip(CircleShape).background(Color(0xFFECE7D4)), contentAlignment = Alignment.Center) {
                if (photoVersion != 0L) coil3.compose.AsyncImage(
                    model = coil3.request.ImageRequest.Builder(context).data(myVm.photoFile).memoryCacheKey("profile-$photoVersion").build(),
                    contentDescription = "프로필 사진", contentScale = androidx.compose.ui.layout.ContentScale.Crop, modifier = Modifier.fillMaxSize(),
                ) else androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
                    val w = size.width
                    drawCircle(MockColors.Navy, w * 0.17f, Offset(w * 0.5f, w * 0.38f))
                    drawCircle(MockColors.Navy, w * 0.34f, Offset(w * 0.5f, w * 1.0f))
                }
            }
            MockText(nickname.ifBlank { "노포여행자" }, 34, Modifier.at(198, 375).width(px(480)), font = MockSerif, weight = FontWeight.ExtraBold)
            MockText(myVm.email ?: "", 23, Modifier.at(198, 424).width(px(480)), color = Color(0xFF4B5563))
            MockText("좋은 맛집이, 좋은 하루를 만듭니다.", 20, Modifier.at(198, 462).width(px(480)), color = Color(0xFF4B5563))
            val stats = listOf(real.size, saved.size, reviews?.size ?: 0, titles)
            val xs = listOf(118, 293, 468, 643)
            val labels = listOf("방문한 노포", "저장한 맛집", "작성한 리뷰", "획득한 칭호")
            stats.forEachIndexed { i, n ->
                MockText("$n", 30, Modifier.at(xs[i] - 60, 506).width(px(120)), font = MockSerif, weight = FontWeight.ExtraBold, align = TextAlign.Center)
                MockText(labels[i], 20, Modifier.at(xs[i] - 75, 548).width(px(150)), color = Color(0xFF4B5563), align = TextAlign.Center)
            }
            // 메뉴 일곱 줄
            MockPic(R.drawable.mk2_a_list, 736, 697, Modifier.at(12, 608))
            MockText("한국어", 22, Modifier.at(586, 1047).width(px(100)), weight = FontWeight.Medium, align = TextAlign.End)
            val granted = androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED ||
                androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_COARSE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED
            MockText(if (granted) "허용됨" else "허용 안 됨", 22, Modifier.at(586, 1147).width(px(100)), weight = FontWeight.Medium, align = TextAlign.End)
            val actions: List<() -> Unit> = listOf(
                onLegacy, { dialog = 1 }, onNotify, onLegacy, { dialog = 2 },
                onPermissions,
                { support = true },
            )
            actions.forEachIndexed { i, a -> Box(Modifier.at(12, 608 + i * 99).size(px(736), px(99)).clickable(role = Role.Button, onClick = a)) }
            // 로그아웃 · 회원 탈퇴
            MockPic(R.drawable.mk2_a_logout, 736, 90, Modifier.at(12, 1325))
            Box(Modifier.at(12, 1325).size(px(736), px(90)).clickable(role = Role.Button, onClick = onLogout))
            MockPic(R.drawable.mk2_a_delete, 736, 90, Modifier.at(12, 1432))
            Box(Modifier.at(12, 1432).size(px(736), px(90)).clickable(role = Role.Button, onClick = onLegacy))
        }
    }
    if (dialog == 1) AlertDialog(
        onDismissRequest = { dialog = 0 }, title = { Text("연결된 계정") },
        text = { Text("현재 이메일 계정으로 로그인되어 있어요.\n${myVm.email ?: ""}\n\n구글·카카오·네이버 계정 연동은 준비 중이에요.") },
        confirmButton = { TextButton({ dialog = 0 }) { Text("확인") } },
    )
    if (dialog == 2) AlertDialog(
        onDismissRequest = { dialog = 0 }, title = { Text("언어 설정") },
        text = { Text("지금은 한국어만 지원해요. 다른 언어는 준비 중이에요.") },
        confirmButton = { TextButton({ dialog = 0 }) { Text("확인") } },
    )
    if (support) SupportDialog(myVm.email) { support = false }
}
