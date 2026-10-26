package com.nopo.cheonnyeon.feature.permission

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.nopo.cheonnyeon.R
import com.nopo.cheonnyeon.core.designsystem.mock.MockColors
import com.nopo.cheonnyeon.core.designsystem.mock.MockPic
import com.nopo.cheonnyeon.core.designsystem.mock.MockStage
import com.nopo.cheonnyeon.core.designsystem.mock.MockText
import com.nopo.cheonnyeon.core.designsystem.mock.at
import com.nopo.cheonnyeon.core.designsystem.mock.px

/** 화면 위의 누르는 자리 (시안 좌표) */
class Spot(val x: Int, val y: Int, val w: Int, val h: Int, val label: String, val onClick: () -> Unit)

fun isGranted(context: Context, permission: String): Boolean =
    ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

fun locationGranted(context: Context) = isGranted(context, Manifest.permission.ACCESS_FINE_LOCATION) || isGranted(context, Manifest.permission.ACCESS_COARSE_LOCATION)

fun cameraGranted(context: Context) = isGranted(context, Manifest.permission.CAMERA)

fun notificationsGranted(context: Context): Boolean = NotificationManagerCompat.from(context).areNotificationsEnabled()

fun openAppSettings(context: Context) {
    runCatching {
        context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}

/** 앨범(사진) 권한: 안드로이드 13 이상은 사진 권한, 그 아래는 저장소 읽기 */
val AlbumPermission: String get() = if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_IMAGES else Manifest.permission.READ_EXTERNAL_STORAGE

fun albumGranted(context: Context) = isGranted(context, AlbumPermission) || (Build.VERSION.SDK_INT >= 34 && isGranted(context, "android.permission.READ_MEDIA_VISUAL_USER_SELECTED"))

/** 알림 권한: 안드로이드 13 이상에서만 따로 허용을 받는다 */
val NotificationPermission: String? get() = if (Build.VERSION.SDK_INT >= 33) "android.permission.POST_NOTIFICATIONS" else null

/** 화면이 다시 앞으로 올 때(설정에 다녀온 뒤)마다 올라가는 번호 */
@Composable
fun rememberResumeTick(): Int {
    var tick by remember { mutableIntStateOf(0) }
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val obs = LifecycleEventObserver { _, e -> if (e == Lifecycle.Event.ON_RESUME) tick++ }
        owner.lifecycle.addObserver(obs)
        onDispose { owner.lifecycle.removeObserver(obs) }
    }
    return tick
}

/** 시안 그림 한 장 위에 누르는 자리를 얹는다(처음 권한 안내용) */
@Composable
private fun StaticSpotsScreen(res: Int, heightPx: Int, originY: Int, spots: List<Spot>) {
    Column(Modifier.fillMaxSize().background(MockColors.Cream).statusBarsPadding().navigationBarsPadding().verticalScroll(rememberScrollState())) {
        MockStage(originY = originY, heightPx = heightPx) {
            MockPic(res, 760, heightPx, Modifier.at(0, originY))
            spots.forEach { s -> Box(Modifier.at(s.x, s.y).size(px(s.w), px(s.h)).clickable(role = Role.Button, onClick = s.onClick).semantics { contentDescription = s.label }) }
        }
    }
}

/** 시안 그림 한 장 + 아래에 고정된 두 버튼(왼쪽 테두리, 오른쪽 연두). 그림은 스크롤된다. */
@Composable
private fun StaticScreen(res: Int, heightPx: Int, originY: Int, later: String, allow: String, icon: @Composable () -> Unit, onLater: () -> Unit, onAllow: () -> Unit) {
    Column(Modifier.fillMaxSize().background(MockColors.Cream).statusBarsPadding().navigationBarsPadding()) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            MockStage(originY = originY, heightPx = heightPx) { MockPic(res, 760, heightPx, Modifier.at(0, originY)) }
        }
        // 그림 칸이 남는 높이를 모두 쓰고(넘치면 스크롤), 두 버튼은 아래에 고정한다. (여기에 빈 칸을 같이 늘리면 그림이 절반에서 잘린다)
        androidx.compose.foundation.layout.Row(Modifier.padding(horizontal = px(16), vertical = px(12)), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(px(14))) {
            Box(
                Modifier.weight(0.8f).size(px(10), px(90)).clip(RoundedCornerShape(px(26))).background(Color(0xFFFCFAF4)).border(1.dp, Color(0xFFD5CFBC), RoundedCornerShape(px(26)))
                    .clickable(role = Role.Button, onClick = onLater),
                contentAlignment = Alignment.Center,
            ) { MockText(later, 28, weight = FontWeight.Medium) }
            Box(
                Modifier.weight(1.15f).size(px(10), px(90)).clip(RoundedCornerShape(px(26))).background(MockColors.Lime2).clickable(role = Role.Button, onClick = onAllow),
                contentAlignment = Alignment.Center,
            ) { androidx.compose.foundation.layout.Row(verticalAlignment = Alignment.CenterVertically) { icon(); Box(Modifier.width(px(14))); MockText(allow, 28, weight = FontWeight.ExtraBold) } }
        }
    }
}

@Composable
private fun CameraGlyph2() {
    androidx.compose.foundation.Canvas(Modifier.size(px(44))) {
        val w = size.width
        val st = androidx.compose.ui.graphics.drawscope.Stroke(w * 0.08f, join = androidx.compose.ui.graphics.StrokeJoin.Round)
        drawRoundRect(MockColors.Navy, androidx.compose.ui.geometry.Offset(w * 0.05f, w * 0.25f), androidx.compose.ui.geometry.Size(w * 0.9f, w * 0.65f), androidx.compose.ui.geometry.CornerRadius(w * 0.12f), style = st)
        drawCircle(MockColors.Navy, w * 0.2f, androidx.compose.ui.geometry.Offset(w * 0.5f, w * 0.57f), style = st)
    }
}

@Composable
private fun BellGlyph() {
    androidx.compose.foundation.Canvas(Modifier.size(px(44))) {
        val w = size.width
        val st = androidx.compose.ui.graphics.drawscope.Stroke(w * 0.08f, cap = androidx.compose.ui.graphics.StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round)
        val p = androidx.compose.ui.graphics.Path().apply {
            moveTo(w * 0.2f, w * 0.75f); cubicTo(w * 0.28f, w * 0.65f, w * 0.28f, w * 0.5f, w * 0.28f, w * 0.4f)
            cubicTo(w * 0.28f, w * 0.2f, w * 0.72f, w * 0.2f, w * 0.72f, w * 0.4f); cubicTo(w * 0.72f, w * 0.5f, w * 0.72f, w * 0.65f, w * 0.8f, w * 0.75f); close()
        }
        drawPath(p, MockColors.Navy, style = st)
        drawLine(MockColors.Navy, androidx.compose.ui.geometry.Offset(w * 0.42f, w * 0.88f), androidx.compose.ui.geometry.Offset(w * 0.58f, w * 0.88f), w * 0.08f, androidx.compose.ui.graphics.StrokeCap.Round)
    }
}

@Composable
private fun PinGlyph() = com.nopo.cheonnyeon.core.designsystem.mock.PinIcon(44)

@Composable
private fun GearGlyph() {
    androidx.compose.foundation.Canvas(Modifier.size(px(44))) {
        val w = size.width
        val st = androidx.compose.ui.graphics.drawscope.Stroke(w * 0.09f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
        drawCircle(MockColors.Navy, w * 0.2f, androidx.compose.ui.geometry.Offset(w / 2, w / 2), style = st)
        for (i in 0 until 8) {
            val a = Math.toRadians(i * 45.0)
            val c = androidx.compose.ui.geometry.Offset(w / 2, w / 2)
            drawLine(MockColors.Navy, c + androidx.compose.ui.geometry.Offset((Math.cos(a) * w * 0.34).toFloat(), (Math.sin(a) * w * 0.34).toFloat()), c + androidx.compose.ui.geometry.Offset((Math.cos(a) * w * 0.44).toFloat(), (Math.sin(a) * w * 0.44).toFloat()), w * 0.12f, androidx.compose.ui.graphics.StrokeCap.Round)
        }
    }
}

/** 시안 15: 카메라 권한이 필요해요 */
@Composable
fun CameraPermissionScreen(onAllow: () -> Unit, onLater: () -> Unit) =
    StaticScreen(R.drawable.mk2_p_cam, 1384, 88, "나중에 하기", "카메라 허용하기", { CameraGlyph2() }, onLater, onAllow)

/** 시안 16: 알림을 받아볼까요? */
@Composable
fun NotificationPermissionScreen(onAllow: () -> Unit, onLater: () -> Unit) =
    StaticScreen(R.drawable.mk2_p_notif, 1394, 88, "나중에", "알림 허용하기", { BellGlyph() }, onLater, onAllow)

/** 시안 20: 위치 권한이 필요해요 */
@Composable
fun LocationPermissionScreen(onAllow: () -> Unit, onLater: () -> Unit) =
    StaticScreen(R.drawable.mk2_p_loc, 1422, 88, "지금 안 함", "허용하기", { PinGlyph() }, onLater, onAllow)

/** 시안 17: 권한이 꺼져 있어요 (설정에서 켜야 할 때) */
@Composable
fun PermissionOffScreen(onOpenSettings: () -> Unit, onLater: () -> Unit) =
    StaticScreen(R.drawable.mk2_p_off, 1420, 88, "나중에", "설정 열기", { GearGlyph() }, onLater, onOpenSettings)

/** 처음 권한 안내에서 고를 수 있는 여섯 가지. 번호는 [PermissionIntroHost] 와 맞춘다. */
class IntroPermRow(val title: String, val required: Boolean, val desc: String)

val IntroPermRows = listOf(
    IntroPermRow("위치 접근 권한", true, "현재 위치를 기반으로\n주변 노포를 찾고, 방문 인증을 할 수 있어요."),
    IntroPermRow("카메라 접근 권한", true, "영수증 인증, 음식 사진 촬영 등\n다양한 인증에 필요해요."),
    IntroPermRow("사진 앨범 접근 권한", true, "앨범에 저장된 사진으로\n리뷰에 사진을 붙일 수 있어요."),
    IntroPermRow("알림 권한", false, "새로운 노포, 이벤트, 스탬프 달성 소식을\n바로 알려드려요."),
    IntroPermRow("방문 기록 공유", false, "다른 사용자와 방문 기록을 공유하고\n나만의 노포 여정을 보여줄 수 있어요."),
    IntroPermRow("앱 사용 데이터 수집", false, "더 나은 서비스 제공을 위해\n앱 사용 데이터를 수집해요."),
)

/**
 * 시안 19: 처음 권한 안내. 머리글 그림은 시안 그대로, 여섯 가지 권한은 눌러서 고르거나 뺄 수 있는 줄로 다시 그렸다.
 * 아래 "모두 허용하고 시작하기"는 전부 고른 것으로 보고 요청하고, "선택한 권한만 허용하기"는 고른 것만 요청한다.
 */
@Composable
fun PermissionIntroScreen(selected: Set<Int>, onToggle: (Int) -> Unit, onAllowAll: () -> Unit, onAllowSelected: () -> Unit, onLater: () -> Unit) {
    Column(Modifier.fillMaxSize().background(MockColors.Cream).statusBarsPadding().navigationBarsPadding()) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            MockStage(originY = 62, heightPx = 470) {
                MockPic(R.drawable.mk2_p_intro_head, 760, 470, Modifier.at(0, 62))
                Box(Modifier.at(20, 70).size(px(70), px(60)).clickable(role = Role.Button, onClick = onLater).semantics { contentDescription = "뒤로" })
                Box(Modifier.at(590, 85).size(px(150), px(45)).clickable(role = Role.Button, onClick = onLater).semantics { contentDescription = "나중에 하기" })
            }
            IntroPermRows.forEachIndexed { i, r ->
                val on = i in selected
                androidx.compose.foundation.layout.Row(
                    Modifier.padding(horizontal = px(30), vertical = px(7)).fillMaxWidth().clip(RoundedCornerShape(px(26)))
                        .background(Color(0xFFFCFAF4)).border(if (on) 2.dp else 1.dp, if (on) MockColors.Lime2 else Color(0xFFE2DDCB), RoundedCornerShape(px(26)))
                        .clickable(role = Role.Checkbox) { onToggle(i) }.semantics { contentDescription = r.title + if (on) " 선택됨" else " 선택 안 됨" }.padding(px(18)),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.size(px(84)).clip(RoundedCornerShape(px(24))).background(Color(0xFFF1EDE0)), contentAlignment = Alignment.Center) {
                        when (i) {
                            0 -> PinGlyph()
                            1 -> CameraGlyph2()
                            2 -> AlbumGlyph()
                            3 -> BellGlyph()
                            4 -> com.nopo.cheonnyeon.core.designsystem.mock.PinIcon(34)
                            else -> GearGlyph()
                        }
                    }
                    Column(Modifier.weight(1f).padding(start = px(20)), verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(px(6))) {
                        androidx.compose.foundation.layout.Row(verticalAlignment = Alignment.CenterVertically) {
                            MockText(r.title, 27, weight = FontWeight.ExtraBold)
                            Box(Modifier.width(px(12)))
                            Box(Modifier.clip(RoundedCornerShape(50)).background(if (r.required) Color(0xFFE7F5A8) else Color(0xFFEAE6D8)).padding(horizontal = px(14), vertical = px(3))) {
                                MockText(if (r.required) "필수" else "선택", 19, weight = FontWeight.Bold)
                            }
                        }
                        MockText(r.desc, 20, color = Color(0xFF4B5563), maxLines = 2, lineHeightPx = 28)
                    }
                    Box(
                        Modifier.size(px(52)).clip(CircleShape).background(if (on) MockColors.Lime2 else Color.White).border(2.dp, if (on) MockColors.Lime2 else Color(0xFFCFCABB), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (on) androidx.compose.foundation.Canvas(Modifier.size(px(26))) {
                            val q = androidx.compose.ui.graphics.Path().apply { moveTo(size.width * 0.12f, size.height * 0.55f); lineTo(size.width * 0.4f, size.height * 0.82f); lineTo(size.width * 0.9f, size.height * 0.2f) }
                            drawPath(q, MockColors.Navy, style = androidx.compose.ui.graphics.drawscope.Stroke(size.width * 0.16f, cap = androidx.compose.ui.graphics.StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round))
                        }
                    }
                }
            }
            Box(Modifier.size(px(20)))
        }
        Column(Modifier.fillMaxWidth().background(MockColors.Cream).padding(horizontal = px(30), vertical = px(10)), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier.fillMaxWidth().size(px(10), px(92)).clip(RoundedCornerShape(50)).background(MockColors.Navy).clickable(role = Role.Button, onClick = onAllowAll),
                contentAlignment = Alignment.Center,
            ) { MockText("모두 허용하고 시작하기  ›", 30, color = MockColors.Lime2, weight = FontWeight.ExtraBold) }
            Box(Modifier.fillMaxWidth().padding(top = px(8)).clickable(role = Role.Button, onClick = onAllowSelected).padding(vertical = px(12)), contentAlignment = Alignment.Center) {
                MockText("선택한 권한만 허용", 24, weight = FontWeight.Bold, color = Color(0xFF374151))
            }
        }
    }
}

@Composable
private fun ToggleSwitch(on: Boolean) {
    val track = if (on) Color(0xFF8EDB1F) else Color(0xFFCFCFCF)
    Box(Modifier.size(px(80), px(48)).clip(RoundedCornerShape(50)).background(track)) {
        Box(Modifier.align(if (on) Alignment.CenterEnd else Alignment.CenterStart).padding(px(4)).size(px(40)).clip(CircleShape).background(Color.White))
    }
}


private val RowTitles = listOf("위치 정보", "카메라", "알림", "마케팅 정보 수신", "방문 기록 공유", "앱 사용 데이터 수집", "사진 앨범")

/**
 * 시안 18: 권한 관리. 위치·카메라·알림은 실제 권한 상태이고, 마케팅·방문 기록 공유·사용 데이터 수집은 이 앱 안의 선택이다.
 * [states] 여섯 칸의 켜짐 여부, [onRow] 누르면 호출(권한 칸은 요청이나 설정 열기, 선택 칸은 켜고 끄기).
 */
@Composable
fun PermissionManageScreen(states: List<Boolean>, onBack: () -> Unit, onRow: (Int) -> Unit, onInfo: () -> Unit) {
    Column(Modifier.fillMaxSize().background(MockColors.Cream).statusBarsPadding().verticalScroll(rememberScrollState())) {
        MockStage(originY = 88, heightPx = 1580) {
            MockPic(R.drawable.mk2_pm_head, 760, 468, Modifier.at(0, 88))
            Box(Modifier.at(18, 110).size(px(80), px(80)).clickable(role = Role.Button, onClick = onBack).semantics { contentDescription = "뒤로" })
            val rows = listOf(R.drawable.mk2_pm_row1, R.drawable.mk2_pm_row2, R.drawable.mk2_pm_row3, R.drawable.mk2_pm_row4, R.drawable.mk2_pm_row5, R.drawable.mk2_pm_row6)
            rows.forEachIndexed { i, res ->
                val y0 = (558 + i * 139.6).toInt()
                val on = states.getOrElse(i) { false }
                MockPic(res, 736, 130, Modifier.at(12, y0))
                val perm = i < 3
                MockText(
                    if (on) (if (perm) "허용됨" else if (i == 3 || i == 5) "동의함" else "허용됨") else (if (perm) "허용 안 됨" else "선택 안 함"),
                    22, Modifier.at(486, y0 + 38).size(px(118), px(40)), color = if (on) Color(0xFF3F8F0A) else Color(0xFF4B5563), weight = FontWeight.Medium, align = TextAlign.End,
                )
                Box(Modifier.at(612, y0 + 36)) { ToggleSwitch(on) }
                Box(Modifier.at(12, y0).size(px(736), px(130)).clickable(role = Role.Switch) { onRow(i) }.semantics { contentDescription = RowTitles[i] })
            }
            // 일곱째 줄: 사진 앨범 (시안에는 없어서 같은 모양으로 직접 그린다)
            val ay = 1398
            val aOn = states.getOrElse(6) { false }
            Box(Modifier.at(12, ay).size(px(736), px(130)).clip(RoundedCornerShape(px(22))).background(Color(0xFFFCFAF4)).border(1.dp, Color(0xFFEFE9D8), RoundedCornerShape(px(22))))
            Box(Modifier.at(40, ay + 30).size(px(70)).clip(CircleShape).background(Color(0xFFE2F77A)), contentAlignment = Alignment.Center) { AlbumGlyph() }
            MockText(androidx.compose.ui.res.stringResource(com.nopo.cheonnyeon.R.string.perm_01), 28, Modifier.at(142, ay + 20), weight = FontWeight.ExtraBold, font = com.nopo.cheonnyeon.core.designsystem.mock.MockSans)
            MockText("리뷰에 음식 사진을 올릴 때\n앨범에서 고를 수 있어요.", 20, Modifier.at(142, ay + 62).size(px(360), px(60)), color = Color(0xFF4B5563), maxLines = 2, lineHeightPx = 28)
            MockText(if (aOn) "허용됨" else "허용 안 됨", 22, Modifier.at(486, ay + 38).size(px(118), px(40)), color = if (aOn) Color(0xFF3F8F0A) else Color(0xFF4B5563), weight = FontWeight.Medium, align = TextAlign.End)
            Box(Modifier.at(612, ay + 36)) { ToggleSwitch(aOn) }
            Box(Modifier.at(12, ay).size(px(736), px(130)).clickable(role = Role.Switch) { onRow(6) }.semantics { contentDescription = RowTitles[6] })
            MockPic(R.drawable.mk2_pm_info, 736, 98, Modifier.at(12, 1550))
            Box(Modifier.at(12, 1550).size(px(736), px(98)).clickable(role = Role.Button, onClick = onInfo))
        }
    }
}

@Composable
private fun AlbumGlyph() {
    androidx.compose.foundation.Canvas(Modifier.size(px(40))) {
        val w = size.width
        val st = androidx.compose.ui.graphics.drawscope.Stroke(w * 0.08f, join = androidx.compose.ui.graphics.StrokeJoin.Round, cap = androidx.compose.ui.graphics.StrokeCap.Round)
        drawRoundRect(MockColors.Navy, androidx.compose.ui.geometry.Offset(w * 0.08f, w * 0.12f), androidx.compose.ui.geometry.Size(w * 0.84f, w * 0.76f), androidx.compose.ui.geometry.CornerRadius(w * 0.1f), style = st)
        drawCircle(MockColors.Navy, w * 0.07f, androidx.compose.ui.geometry.Offset(w * 0.34f, w * 0.34f))
        val p = androidx.compose.ui.graphics.Path().apply { moveTo(w * 0.12f, w * 0.78f); lineTo(w * 0.4f, w * 0.52f); lineTo(w * 0.58f, w * 0.68f); lineTo(w * 0.72f, w * 0.5f); lineTo(w * 0.9f, w * 0.72f) }
        drawPath(p, MockColors.Navy, style = st)
    }
}
