package com.nopo.cheonnyeon.feature.verify

import android.Manifest
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate as rotateDraw
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nopo.cheonnyeon.R
import com.nopo.cheonnyeon.core.common.message
import com.nopo.cheonnyeon.core.designsystem.component.CelebrationOverlay
import com.nopo.cheonnyeon.core.designsystem.component.labelRes
import com.nopo.cheonnyeon.core.designsystem.mock.ChevronRight
import com.nopo.cheonnyeon.core.designsystem.mock.MockColors
import com.nopo.cheonnyeon.core.designsystem.mock.MockLimeBar
import com.nopo.cheonnyeon.core.designsystem.mock.MockPic
import com.nopo.cheonnyeon.core.designsystem.mock.MockSerif
import com.nopo.cheonnyeon.core.designsystem.mock.MockStage
import com.nopo.cheonnyeon.core.designsystem.mock.MockText
import com.nopo.cheonnyeon.core.designsystem.mock.PinIcon
import com.nopo.cheonnyeon.core.designsystem.mock.at
import com.nopo.cheonnyeon.core.designsystem.mock.px
import com.nopo.cheonnyeon.core.designsystem.mock.pxSp
import com.nopo.cheonnyeon.domain.model.StampGrade
import com.nopo.cheonnyeon.domain.rule.TitlePolicy
import com.nopo.cheonnyeon.feature.my.SUPPORT_EMAIL
import com.nopo.cheonnyeon.feature.store.StorePhoto
import com.nopo.cheonnyeon.platform.camera.rememberCameraController
import com.nopo.cheonnyeon.platform.permission.PermissionStatus
import com.nopo.cheonnyeon.platform.permission.rememberPermission
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private val Hairline = Color(0xFFE2DDCB)
private val Gray = Color(0xFF4B5563)
private val Hint = Color(0xFF6B7280)
private val CardWhite = Color(0xFFFCFAF4)
private val Red = Color(0xFFD23B2E)

private fun fmtDate(t: java.time.LocalDateTime): String =
    "${t.year}. %02d. %02d. (${t.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.KOREAN)}) %02d:%02d".format(t.monthValue, t.dayOfMonth, t.hour, t.minute)

private fun Modifier.desc(label: String): Modifier = this.semantics { contentDescription = label }

/** 공통 머리글: 로고·부제 + 오른쪽 닫기 */
@Composable
private fun androidx.compose.foundation.layout.BoxScope.VerifyHeader(onClose: (() -> Unit)?, shareIcon: (() -> Unit)? = null) {
    MockPic(R.drawable.mk2_v_hdr, 460, 117, Modifier.at(20, 88))
    if (onClose != null) Box(Modifier.at(652, 98).size(px(76)).clickable(role = Role.Button, onClick = onClose).desc("닫기"), contentAlignment = Alignment.Center) { CloseX(38) }
    if (shareIcon != null) Box(Modifier.at(572, 98).size(px(76)).clickable(role = Role.Button, onClick = shareIcon).desc("공유"), contentAlignment = Alignment.Center) { ShareGlyph(40) }
}

@Composable
private fun CloseX(sizePx: Int) {
    Canvas(Modifier.size(px(sizePx))) {
        val sw = size.width * 0.1f
        drawLine(MockColors.Navy, Offset(size.width * 0.1f, size.height * 0.1f), Offset(size.width * 0.9f, size.height * 0.9f), sw, StrokeCap.Round)
        drawLine(MockColors.Navy, Offset(size.width * 0.9f, size.height * 0.1f), Offset(size.width * 0.1f, size.height * 0.9f), sw, StrokeCap.Round)
    }
}

@Composable
private fun ShareGlyph(sizePx: Int) {
    Canvas(Modifier.size(px(sizePx))) {
        val w = size.width
        val st = Stroke(w * 0.08f, cap = StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round)
        val box = androidx.compose.ui.graphics.Path().apply { moveTo(w * 0.3f, w * 0.42f); lineTo(w * 0.15f, w * 0.42f); lineTo(w * 0.15f, w * 0.92f); lineTo(w * 0.85f, w * 0.92f); lineTo(w * 0.85f, w * 0.42f); lineTo(w * 0.7f, w * 0.42f) }
        drawPath(box, MockColors.Navy, style = st)
        drawLine(MockColors.Navy, Offset(w * 0.5f, w * 0.65f), Offset(w * 0.5f, w * 0.08f), w * 0.08f, StrokeCap.Round)
        val head = androidx.compose.ui.graphics.Path().apply { moveTo(w * 0.33f, w * 0.25f); lineTo(w * 0.5f, w * 0.08f); lineTo(w * 0.67f, w * 0.25f) }
        drawPath(head, MockColors.Navy, style = st)
    }
}

@Composable
private fun BackChevron(sizePx: Int = 40, color: Color = MockColors.Navy) {
    Canvas(Modifier.size(px(sizePx))) {
        val p = androidx.compose.ui.graphics.Path().apply { moveTo(size.width * 0.7f, size.height * 0.1f); lineTo(size.width * 0.25f, size.height * 0.5f); lineTo(size.width * 0.7f, size.height * 0.9f) }
        drawPath(p, color, style = Stroke(size.width * 0.09f, cap = StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round))
    }
}

@Composable
private fun CameraGlyph(sizePx: Int) {
    Canvas(Modifier.size(px(sizePx))) {
        val w = size.width
        val st = Stroke(w * 0.08f, join = androidx.compose.ui.graphics.StrokeJoin.Round)
        drawRoundRect(MockColors.Navy, Offset(w * 0.05f, w * 0.25f), androidx.compose.ui.geometry.Size(w * 0.9f, w * 0.65f), androidx.compose.ui.geometry.CornerRadius(w * 0.12f), style = st)
        drawCircle(MockColors.Navy, w * 0.2f, Offset(w * 0.5f, w * 0.57f), style = st)
        drawLine(MockColors.Navy, Offset(w * 0.33f, w * 0.22f), Offset(w * 0.42f, w * 0.1f), w * 0.08f, StrokeCap.Round)
        drawLine(MockColors.Navy, Offset(w * 0.42f, w * 0.1f), Offset(w * 0.6f, w * 0.1f), w * 0.08f, StrokeCap.Round)
    }
}

/** 방문 인증 화면 (시안 영수증 인증 흐름 복제): 안내 → 촬영 → 정보 확인 → 완료, 실패 안내, 위치·가게 사진 인증 */
@Composable
fun VerifyScreen(vm: VerifyViewModel, onClose: () -> Unit, onReview: () -> Unit = {}) {
    val state by vm.state.collectAsStateWithLifecycle()
    val camera = rememberCameraController()
    LaunchedEffect(state.error) { state.error?.let { com.nopo.cheonnyeon.core.common.ErrorLog.verifyFailed(it); com.nopo.cheonnyeon.core.common.Analytics.verifyResult(state.step.name, "fail", it.code) } }
    val cameraPermission = rememberPermission(Manifest.permission.CAMERA)
    val locationPermission = rememberPermission(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
    val resolver = androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.StartIntentSenderForResult()) { result ->
        vm.onLocationResolution(result.resultCode == android.app.Activity.RESULT_OK, locationPermission.approximateOnly)
    }
    LaunchedEffect(state.pendingResolution) {
        state.pendingResolution?.let { resolver.launch(androidx.activity.result.IntentSenderRequest.Builder(it.resolution).build()) }
    }
    var cameraOpen by rememberSaveable { mutableStateOf(false) }
    var failedShown by rememberSaveable { mutableStateOf(false) }
    var guide by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current

    Box(Modifier.fillMaxSize().background(MockColors.Cream)) {
        when {
            state.step == VerifyStep.DONE && state.award != null -> DoneScreen(state, onReview = onReview, onClose = onClose)
            state.blocked -> FailScreen(state, blocked = true, onRetry = onClose, onClose = onClose, onGuide = { guide = true }, onSupport = { mailSupport(context) })
            state.confirm != null -> ConfirmScreen(state, vm::retakeReceipt, vm::confirmReceipt)
            state.step == VerifyStep.RECEIPT && (state.error != null && !state.busy && state.scanFrame == null || failedShown && state.error != null) && !cameraOpen ->
                FailScreen(state, blocked = false, onRetry = { vm.clearError(); failedShown = false; cameraOpen = true }, onClose = onClose, onGuide = { guide = true }, onSupport = { mailSupport(context) })
            state.step == VerifyStep.RECEIPT && cameraOpen -> CameraScreen(vm, state, camera, cameraPermission, onBack = { cameraOpen = false; vm.clearError() }, onNoReceipt = { cameraOpen = false; vm.goToPhoto() }, onFailed = { cameraOpen = false; failedShown = true })
            state.step == VerifyStep.RECEIPT -> IntroScreen(state, onStart = { vm.clearError(); cameraOpen = true }, onNoReceipt = vm::goToPhoto, onClose = onClose)
            else -> StepScreen(vm, state, camera, cameraPermission, locationPermission, onClose)
        }
    }
    if (guide) {
        AlertDialog(
            onDismissRequest = { guide = false },
            title = { Text(androidx.compose.ui.res.stringResource(com.nopo.cheonnyeon.R.string.ver_01)) },
            text = { Text("• 영수증 전체가 한 장에 들어오게 찍어 주세요.\n• 식당명, 날짜·시간, 금액이 잘 보여야 해요.\n• 흔들림 없이 밝은 곳에서 찍으면 더 정확해요.\n• 영수증은 방문한 당일 결제한 것만 인정돼요.") },
            confirmButton = { TextButton({ guide = false }) { Text(androidx.compose.ui.res.stringResource(com.nopo.cheonnyeon.R.string.ver_02)) } },
        )
    }
    state.aiHint?.let { tip ->
        AlertDialog(
            onDismissRequest = vm::dismissHint,
            title = { Text(stringResource(R.string.verify_ai_hint_title)) },
            text = { Text(tip) },
            confirmButton = { TextButton({ vm.clearScan(); vm.clearError(); failedShown = false; cameraOpen = true }) { Text(stringResource(R.string.verify_ai_hint_retake)) } },
            dismissButton = { TextButton(vm::dismissHint) { Text(stringResource(R.string.verify_ai_hint_close)) } },
        )
    }
    if (state.askAiConsent) com.nopo.cheonnyeon.feature.ai.AiConsentDialog(onAgree = { vm.answerAiConsent(true) }, onDismiss = { vm.answerAiConsent(false) })
}

private fun mailSupport(context: android.content.Context) {
    val send = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$SUPPORT_EMAIL")).putExtra(Intent.EXTRA_SUBJECT, "[노포 패스포트] 방문 인증 문의")
    try { context.startActivity(send.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } catch (_: android.content.ActivityNotFoundException) { }
}

// ---------------------------------------------------------------- 안내 (시안 9)

@Composable
private fun IntroScreen(state: VerifyUiState, onStart: () -> Unit, onNoReceipt: () -> Unit, onClose: () -> Unit) {
    Column(Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState()).navigationBarsPadding()) {
        MockStage(originY = 84, heightPx = 1450) {
            VerifyHeader(onClose)
            MockPic(R.drawable.mk2_v9_hero, 760, 460, Modifier.at(0, 205))
            state.clockSkewMinutes?.let { MockText(stringResource(R.string.verify_clock_skew, it), 19, Modifier.at(30, 668).width(px(700)), color = Red, maxLines = 2) }
            MockPic(R.drawable.mk2_v9_steps, 723, 242, Modifier.at(22, 650))
            // 영수증 촬영하기
            Box(Modifier.at(28, 905).size(px(707), px(92)).clip(RoundedCornerShape(px(30))).background(MockColors.Lime2).clickable(role = Role.Button, onClick = onStart), contentAlignment = Alignment.Center) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CameraGlyph(52)
                    Box(Modifier.width(px(26)))
                    MockText(androidx.compose.ui.res.stringResource(com.nopo.cheonnyeon.R.string.ver_03), 30, weight = FontWeight.ExtraBold)
                }
                Box(Modifier.align(Alignment.CenterEnd).padding(end = px(30))) { ChevronRight(28) }
            }
            // 영수증이 없을 때: 가게 사진·위치로 인증 (앱 안 카메라만 쓰므로 앨범 대신)
            Box(Modifier.at(28, 1010).size(px(707), px(86)).clip(RoundedCornerShape(px(28))).background(Color(0xFFF1EDE0)).clickable(role = Role.Button, onClick = onNoReceipt), contentAlignment = Alignment.Center) {
                MockText(androidx.compose.ui.res.stringResource(com.nopo.cheonnyeon.R.string.ver_04), 26, weight = FontWeight.ExtraBold)
                Box(Modifier.align(Alignment.CenterEnd).padding(end = px(30))) { ChevronRight(26) }
            }
            MockPic(R.drawable.mk2_v9_tips, 725, 193, Modifier.at(20, 1105))
            MockText(androidx.compose.ui.res.stringResource(com.nopo.cheonnyeon.R.string.ver_05), 28, Modifier.at(30, 1316), weight = FontWeight.ExtraBold)
            Box(Modifier.at(580, 1312).size(px(160), px(50)).clickable(role = Role.Button, onClick = onClose), contentAlignment = Alignment.CenterEnd) {
                Row(verticalAlignment = Alignment.CenterVertically) { MockText(androidx.compose.ui.res.stringResource(com.nopo.cheonnyeon.R.string.ver_06), 21, color = Gray); Box(Modifier.width(px(8))); ChevronRight(24) }
            }
            val r = state.recent.firstOrNull()
            Box(Modifier.at(28, 1370).size(px(707), px(150)).clip(RoundedCornerShape(px(24))).background(CardWhite).border(1.dp, Hairline, RoundedCornerShape(px(24)))) {
                if (r == null) MockText(androidx.compose.ui.res.stringResource(com.nopo.cheonnyeon.R.string.ver_07), 23, Modifier.align(Alignment.Center), color = Hint)
                else {
                    Box(Modifier.padding(start = px(16), top = px(14)).size(px(165), px(122)).clip(RoundedCornerShape(px(16)))) { StorePhoto(r.store, Modifier.fillMaxSize()) }
                    MockText(r.store.name, 29, Modifier.padding(start = px(205), top = px(16)).width(px(330)), font = MockSerif, weight = FontWeight.ExtraBold)
                    val d = java.time.Instant.ofEpochMilli(r.at).atZone(com.nopo.cheonnyeon.core.common.SeoulZone)
                    MockText("%d.%02d.%02d  |  %02d:%02d".format(d.year, d.monthValue, d.dayOfMonth, d.hour, d.minute), 21, Modifier.padding(start = px(205), top = px(62)), color = Hint)
                    Row(Modifier.padding(start = px(205), top = px(96)), horizontalArrangement = Arrangement.spacedBy(px(10))) {
                        listOf(r.store.categoryGroup.takeIf { it.isNotBlank() }, r.store.sigungu.takeIf { it.isNotBlank() }).filterNotNull().forEach { t ->
                            Box(Modifier.clip(RoundedCornerShape(50)).background(Color(0xFFEFEBDD)).padding(horizontal = px(14), vertical = px(4))) { MockText(t, 18, color = Gray) }
                        }
                    }
                    Box(Modifier.align(Alignment.CenterEnd).padding(end = px(24)).clip(RoundedCornerShape(50)).background(Color(0xFFE7F5A8)).padding(horizontal = px(18), vertical = px(8))) {
                        MockText(androidx.compose.ui.res.stringResource(com.nopo.cheonnyeon.R.string.ver_08), 19, weight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------- 카메라 (시안 10)

@Composable
private fun CameraScreen(
    vm: VerifyViewModel, state: VerifyUiState, camera: com.nopo.cheonnyeon.platform.camera.CameraController,
    permission: com.nopo.cheonnyeon.platform.permission.PermissionHandle, onBack: () -> Unit, onNoReceipt: () -> Unit, onFailed: () -> Unit,
) {
    val granted = permission.status == PermissionStatus.GRANTED
    if (!granted) {
        if (permission.status == PermissionStatus.PERMANENTLY_DENIED) com.nopo.cheonnyeon.feature.permission.PermissionOffScreen(onOpenSettings = permission.openSettings, onLater = onBack)
        else com.nopo.cheonnyeon.feature.permission.CameraPermissionScreen(onAllow = permission.request, onLater = onBack)
        return
    }
    LaunchedEffect(state.error, state.busy, state.scanFrame) { if (state.error != null && !state.busy) onFailed() }
    Column(Modifier.fillMaxSize().background(MockColors.Cream).statusBarsPadding().navigationBarsPadding()) {
        Box(Modifier.fillMaxWidth().height(px(150))) {
            MockStage(originY = 84, heightPx = 150) {
                Box(Modifier.at(24, 108).size(px(76)).clip(CircleShape).background(Color(0xFFF1EDE0)).clickable(role = Role.Button, onClick = onBack).desc("뒤로"), contentAlignment = Alignment.Center) { BackChevron(34) }
                MockPic(R.drawable.mk2_v10_hdr, 490, 145, Modifier.at(110, 90))
                MockText("영수증 전체가 프레임 안에 들어오게 촬영해주세요.", 22, Modifier.at(40, 188).width(px(700)), color = Gray)
            }
        }
        Box(Modifier.weight(1f).fillMaxWidth().clip(androidx.compose.ui.graphics.RectangleShape)) {
            val frame = state.scanFrame
            if (frame != null) ReceiptScanView(frame, state.scanBoxes, state.scanPhase, Modifier.fillMaxSize())
            else if (granted) {
                com.nopo.cheonnyeon.platform.camera.CameraPreview(camera, Modifier.fillMaxSize())
                ScanFrame(Modifier.fillMaxSize())
            } else {
                Column(Modifier.fillMaxSize().background(Color(0xFF1B1B1B)).padding(px(40)), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                    MockText(stringResource(R.string.perm_camera_needed), 24, color = Color.White, align = TextAlign.Center, maxLines = 3)
                    Box(Modifier.height(px(24)))
                    MockLimeBar(stringResource(if (permission.status == PermissionStatus.PERMANENTLY_DENIED) R.string.perm_open_settings else R.string.perm_request), 80, Modifier.fillMaxWidth(), textPx = 26) {
                        if (permission.status == PermissionStatus.PERMANENTLY_DENIED) permission.openSettings() else permission.request()
                    }
                }
            }
            if (state.busy) {
                Box(Modifier.align(Alignment.TopCenter).padding(top = px(110)).clip(RoundedCornerShape(50)).background(Color(0xCC2B2B14)).border(1.dp, MockColors.Lime2, RoundedCornerShape(50)).padding(horizontal = px(26), vertical = px(12))) {
                    MockText(if (state.scanPhase == ScanPhase.NONE) "촬영 중…" else "자동 인식 중…", 22, color = MockColors.Lime2, weight = FontWeight.Medium)
                }
            }
        }
        // 아래 촬영 패널
        Box(Modifier.fillMaxWidth().height(px(215)).clip(RoundedCornerShape(topStart = px(40), topEnd = px(40))).background(MockColors.Cream)) {
            Box(Modifier.align(Alignment.Center).size(px(126)).clip(CircleShape).background(MockColors.Navy).clickable(enabled = !state.busy && granted, role = Role.Button) { vm.captureReceipt(camera) }.desc("영수증 촬영"), contentAlignment = Alignment.Center) {
                Box(Modifier.size(px(108)).clip(CircleShape).background(MockColors.Cream), contentAlignment = Alignment.Center) {
                    Box(Modifier.size(px(96)).clip(CircleShape).background(if (state.busy) Color(0xFFD5DDA0) else MockColors.Lime2))
                }
            }
            Box(Modifier.align(Alignment.CenterStart).padding(start = px(40)).clickable(enabled = !state.busy, role = Role.Button, onClick = onNoReceipt)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.size(px(88)).clip(RoundedCornerShape(px(22))).background(Color(0xFFF1EDE0)).border(2.dp, MockColors.Navy, RoundedCornerShape(px(22))), contentAlignment = Alignment.Center) { PinIcon(44) }
                    MockText(androidx.compose.ui.res.stringResource(com.nopo.cheonnyeon.R.string.ver_09), 21, color = Gray)
                }
            }
        }
    }
}

@Composable
private fun ScanFrame(modifier: Modifier) {
    Canvas(modifier) {
        val w = size.width * 0.82f
        val h = size.height * 0.78f
        val left = (size.width - w) / 2
        val top = (size.height - h) / 2 + size.height * 0.02f
        val len = 44.dp.toPx()
        val sw = 6.dp.toPx()
        val c = Color(0xFFD6F542)
        drawRoundRect(Color(0x33FFFFFF), Offset(left, top), androidx.compose.ui.geometry.Size(w, h), androidx.compose.ui.geometry.CornerRadius(28.dp.toPx()), style = Stroke(1.2.dp.toPx()))
        fun corner(x: Float, y: Float, dx: Float, dy: Float) {
            drawLine(c, Offset(x, y), Offset(x + dx * len, y), sw, StrokeCap.Round)
            drawLine(c, Offset(x, y), Offset(x, y + dy * len), sw, StrokeCap.Round)
        }
        corner(left, top, 1f, 1f); corner(left + w, top, -1f, 1f); corner(left, top + h, 1f, -1f); corner(left + w, top + h, -1f, -1f)
    }
}

// ---------------------------------------------------------------- 정보 확인 (시안 8)

@Composable
internal fun ConfirmScreen(state: VerifyUiState, onRetake: () -> Unit, onConfirm: () -> Unit) {
    val info = state.confirm ?: return
    val unknown = "인식하지 못했어요"
    Column(Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState()).navigationBarsPadding()) {
        MockStage(originY = 84, heightPx = 1530) {
            Box(Modifier.at(20, 100).size(px(70)).clickable(role = Role.Button, onClick = onRetake).desc("뒤로"), contentAlignment = Alignment.Center) { BackChevron(34) }
            MockText(androidx.compose.ui.res.stringResource(com.nopo.cheonnyeon.R.string.ver_10), 33, Modifier.at(0, 100).fillMaxWidth(), font = MockSerif, weight = FontWeight.ExtraBold, align = TextAlign.Center)
            MockText(androidx.compose.ui.res.stringResource(com.nopo.cheonnyeon.R.string.ver_11), 21, Modifier.at(0, 160).fillMaxWidth(), color = Gray, align = TextAlign.Center)
            Box(Modifier.at(20, 208).size(px(725), px(547)).clip(RoundedCornerShape(px(28))).background(Color(0xFF2B1B10))) {
                state.scanFrame?.let { f -> androidx.compose.foundation.Image(f.asImageBitmap(), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
                Box(Modifier.align(Alignment.BottomEnd).padding(end = px(18), bottom = px(18)).clip(RoundedCornerShape(px(26))).background(Color(0xCC1B1B1B)).clickable(role = Role.Button, onClick = onRetake).padding(horizontal = px(26), vertical = px(16))) {
                    MockText(androidx.compose.ui.res.stringResource(com.nopo.cheonnyeon.R.string.ver_12), 22, color = Color.White, weight = FontWeight.Medium)
                }
            }
            Box(Modifier.at(20, 775).size(px(725), px(530)).clip(RoundedCornerShape(px(28))).background(CardWhite))
            val rows = listOf(
                Triple(R.drawable.mk2_v8_row1, 797, 320), Triple(R.drawable.mk2_v8_row2, 893, 320), Triple(R.drawable.mk2_v8_row3, 995, 320),
                Triple(R.drawable.mk2_v8_row4, 1090, 290), Triple(R.drawable.mk2_v8_row5, 1200, 323),
            )
            val h = listOf(70, 65, 70, 105, 105)
            val w = listOf(272, 272, 272, 243, 278)
            val ys = listOf(795, 895, 990, 1085, 1195)
            val values = listOf(
                info.storeName,
                info.paidAt?.let { fmtDate(it) } ?: unknown,
                info.amount?.let { "%,d원".format(it) } ?: unknown,
                info.address.ifBlank { unknown },
                info.approval ?: unknown,
            )
            rows.forEachIndexed { i, (res, fy, fx) ->
                MockPic(res, w[i], h[i], Modifier.at(40, ys[i]))
                Box(Modifier.at(fx, fy).size(px(735 - fx - 80), px(74)).clip(RoundedCornerShape(px(18))).background(Color(0xFFFBF8EE)).border(1.dp, Hairline, RoundedCornerShape(px(18))), contentAlignment = Alignment.CenterStart) {
                    MockText(values[i], if (values[i].length > 16) 19 else 22, Modifier.padding(horizontal = px(18)), color = if (values[i] == unknown) Hint else MockColors.Navy, maxLines = 2, lineHeightPx = 26)
                }
            }
            MockPic(R.drawable.mk2_v8_warn, 705, 157, Modifier.at(30, 1318))
            state.error?.let { MockText(it.message(LocalContext.current), 20, Modifier.at(40, 1480).width(px(690)), color = Red) }
            Box(Modifier.at(33, 1493).size(px(284), px(92)).clip(RoundedCornerShape(px(28))).border(2.dp, MockColors.Navy, RoundedCornerShape(px(28))).background(CardWhite).clickable(role = Role.Button, onClick = onRetake), contentAlignment = Alignment.Center) {
                Row(verticalAlignment = Alignment.CenterVertically) { CameraGlyph(40); Box(Modifier.width(px(12))); MockText(androidx.compose.ui.res.stringResource(com.nopo.cheonnyeon.R.string.ver_13), 26, weight = FontWeight.ExtraBold) }
            }
            Box(Modifier.at(335, 1493).size(px(397), px(92)).clip(RoundedCornerShape(px(28))).background(if (state.busy) Color(0xFFE2E8C4) else MockColors.Lime2).clickable(enabled = !state.busy, role = Role.Button, onClick = onConfirm), contentAlignment = Alignment.Center) {
                MockText(if (state.busy) "기록 중…" else "인증 정보 확정하기  →", 26, weight = FontWeight.ExtraBold)
            }
        }
    }
}

// ---------------------------------------------------------------- 실패 (시안 4)

@Composable
private fun FailScreen(state: VerifyUiState, blocked: Boolean, onRetry: () -> Unit, onClose: () -> Unit, onGuide: () -> Unit, onSupport: () -> Unit) {
    val context = LocalContext.current
    val msg = state.error?.message(context, state.errorMeters).orEmpty()
    Column(Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState()).navigationBarsPadding()) {
        MockStage(originY = 84, heightPx = 1450) {
            VerifyHeader(onClose)
            MockPic(R.drawable.mk2_v4_hero, 760, 420, Modifier.at(0, 205))
            MockText(if (blocked) "오늘은 더 인증할 수 없어요" else "아래와 같은 이유로 방문 인증이 실패했어요.", 23, Modifier.at(20, 636).fillMaxWidth(), color = Gray, align = TextAlign.Center)
            MockText(msg.ifBlank { "다시 한 번 확인하고 인증을 시도해 주세요." }, 21, Modifier.at(40, 676).width(px(680)), color = Red, align = TextAlign.Center, maxLines = 2, lineHeightPx = 28)
            if (blocked && state.todayVisits.isNotEmpty()) {
                state.todayVisits.take(2).forEachIndexed { i, (name, years) -> MockText("${i + 1}. $name · 세월 +${years}년", 20, Modifier.at(40, 740 + i * 30).width(px(680)), color = Hint, align = TextAlign.Center) }
            } else {
                MockPic(R.drawable.mk2_v4_reasons, 715, 396, Modifier.at(25, 735))
            }
            val by = if (blocked && state.todayVisits.isNotEmpty()) 840 else 1148
            Box(Modifier.at(25, by).size(px(710), px(92)).clip(RoundedCornerShape(px(32))).background(MockColors.Lime2).clickable(role = Role.Button, onClick = onRetry), contentAlignment = Alignment.Center) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (!blocked) { Box(Modifier.size(px(52))) { RetryGlyph() }; Box(Modifier.width(px(18))) }
                    MockText(if (blocked) "돌아가기" else "다시 인증하기", 31, weight = FontWeight.ExtraBold)
                }
            }
            if (!blocked) {
                Row(Modifier.at(25, by + 112), horizontalArrangement = Arrangement.spacedBy(px(14))) {
                    Box(Modifier.size(px(348), px(78)).clip(RoundedCornerShape(px(24))).border(1.dp, Hairline, RoundedCornerShape(px(24))).background(CardWhite).clickable(role = Role.Button, onClick = onGuide), contentAlignment = Alignment.Center) {
                        Row(verticalAlignment = Alignment.CenterVertically) { CameraGlyph(38); Box(Modifier.width(px(12))); MockText(androidx.compose.ui.res.stringResource(com.nopo.cheonnyeon.R.string.ver_14), 24, weight = FontWeight.Medium) }
                    }
                    Box(Modifier.size(px(348), px(78)).clip(RoundedCornerShape(px(24))).border(1.dp, Hairline, RoundedCornerShape(px(24))).background(CardWhite).clickable(role = Role.Button, onClick = onSupport), contentAlignment = Alignment.Center) {
                        MockText(androidx.compose.ui.res.stringResource(com.nopo.cheonnyeon.R.string.ver_15), 24, weight = FontWeight.Medium)
                    }
                }
                MockPic(R.drawable.mk2_v4_tip, 715, 175, Modifier.at(25, by + 205))
            }
        }
    }
}

// ---------------------------------------------------------------- 위치·가게 사진 인증 (시안 5)

@Composable
private fun StepScreen(
    vm: VerifyViewModel, state: VerifyUiState, camera: com.nopo.cheonnyeon.platform.camera.CameraController,
    cameraPermission: com.nopo.cheonnyeon.platform.permission.PermissionHandle, locationPermission: com.nopo.cheonnyeon.platform.permission.PermissionHandle, onClose: () -> Unit,
) {
    val context = LocalContext.current
    val photo = state.step == VerifyStep.PHOTO
    if (!photo) { GpsVerifyScreen(vm, state, locationPermission); return }
    val camGranted = cameraPermission.status == PermissionStatus.GRANTED
    // 머리글(노포 패스포트)을 빼고 제목·단계 표시를 바짝 붙여, 촬영 화면이 크게 보이게 한다
    Column(Modifier.fillMaxSize().background(MockColors.Cream).statusBarsPadding().navigationBarsPadding()) {
        Box(Modifier.fillMaxWidth().height(px(232))) {
            MockStage(originY = 0, heightPx = 232) {
                Box(Modifier.at(14, 14).size(px(70)).clickable(role = Role.Button, onClick = vm::backToReceipt).desc("뒤로"), contentAlignment = Alignment.Center) { BackChevron(34) }
                MockText(androidx.compose.ui.res.stringResource(com.nopo.cheonnyeon.R.string.det_09), 40, Modifier.at(90, 14), font = MockSerif, weight = FontWeight.ExtraBold)
                Box(Modifier.at(652, 14).size(px(76)).clickable(role = Role.Button, onClick = onClose).desc("닫기"), contentAlignment = Alignment.Center) { CloseX(36) }
                MockPic(R.drawable.mk2_v5_steps, 725, 140, Modifier.at(20, 84))
            }
        }
        Box(Modifier.weight(1f).fillMaxWidth().clip(androidx.compose.ui.graphics.RectangleShape)) {
            val frame = state.scanFrame
            when {
                frame != null -> ReceiptScanView(frame, state.scanBoxes, state.scanPhase, Modifier.fillMaxSize())
                camGranted -> {
                    com.nopo.cheonnyeon.platform.camera.CameraPreview(camera, Modifier.fillMaxSize())
                    ScanFrame(Modifier.fillMaxSize())
                }
                else -> Column(Modifier.fillMaxSize().background(Color(0xFF1B1B1B)).padding(px(30)), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                    MockText(stringResource(R.string.perm_camera_needed), 22, color = Color.White, align = TextAlign.Center, maxLines = 3)
                    Box(Modifier.height(px(20)))
                    MockLimeBar(stringResource(if (cameraPermission.status == PermissionStatus.PERMANENTLY_DENIED) R.string.perm_open_settings else R.string.perm_request), 76, Modifier.fillMaxWidth(), textPx = 24) {
                        if (cameraPermission.status == PermissionStatus.PERMANENTLY_DENIED) cameraPermission.openSettings() else cameraPermission.request()
                    }
                }
            }
        }
        // 아래: 상태 한 줄 + 촬영 버튼(작게)
        Column(Modifier.fillMaxWidth().background(MockColors.Cream).padding(horizontal = px(22), vertical = px(10)), verticalArrangement = Arrangement.spacedBy(px(8))) {
            val statusTitle = when {
                state.busy -> "글자를 읽는 중…"
                state.error != null -> "다시 시도해 주세요"
                else -> "가게 사진 인증"
            }
            val sub = state.error?.message(context, state.errorMeters) ?: "앱 안 카메라로 가게 간판이나 메뉴판의 글자가 보이게 찍어 주세요."
            MockText(statusTitle, 24, weight = FontWeight.ExtraBold)
            MockText(sub, 19, color = if (state.error != null) Red else Gray, maxLines = 2, lineHeightPx = 25)
            Box(
                Modifier.fillMaxWidth().height(px(84)).clip(RoundedCornerShape(px(24))).background(MockColors.Navy).clickable(enabled = !state.busy, role = Role.Button) { vm.capturePhoto(camera) },
                contentAlignment = Alignment.Center,
            ) {
                MockText(if (state.busy) "분석 중…" else "가게 사진 촬영하기", 28, color = Color.White, weight = FontWeight.ExtraBold)
            }
            if (state.failedPhotoAttempts > 0) MockText(stringResource(R.string.verify_attempts, state.failedPhotoAttempts, com.nopo.cheonnyeon.domain.rule.VerificationConfig.PHOTO_OCR_MAX_ATTEMPTS), 19, color = Red)
            if (vm.canOfferLocationOnly()) {
                Box(Modifier.fillMaxWidth().height(px(64)).clip(RoundedCornerShape(px(20))).border(2.dp, Red, RoundedCornerShape(px(20))).clickable(enabled = !state.busy, role = Role.Button, onClick = vm::acceptLocationOnly), contentAlignment = Alignment.Center) {
                    MockText(stringResource(R.string.verify_location_only), 22, color = Red, weight = FontWeight.Bold)
                }
            }
            Box(Modifier.fillMaxWidth().height(px(60)).clip(RoundedCornerShape(px(18))).background(Color(0xFFF1EDE0)).clickable(role = Role.Button, onClick = vm::backToReceipt), contentAlignment = Alignment.Center) {
                MockText(stringResource(R.string.verify_back_to_receipt), 21, weight = FontWeight.Medium)
            }
        }
    }
}

// ---------------------------------------------------------------- 완료 (시안 11)

@Composable
internal fun DoneScreen(state: VerifyUiState, onReview: () -> Unit, onClose: () -> Unit) {
    val award = state.award ?: return
    val store = state.store ?: return
    val context = LocalContext.current
    val animate = remember { animationsEnabled(context) }
    var impacted by remember { mutableStateOf(!animate) }
    val title = TitlePolicy.titleFor(award.totalYearsAfter)
    val nextYears = TitlePolicy.yearsToNext(award.totalYearsAfter)
    var showTitleUp by remember { mutableStateOf(false) }
    val pop by animateFloatAsState(if (impacted) 1f else 1.7f, tween(if (animate) 420 else 0), finishedListener = { }, label = "stamp")
    LaunchedEffect(Unit) { if (animate) { kotlinx.coroutines.delay(350); vibrateOnce(context); impacted = true } }
    LaunchedEffect(impacted) {
        if (impacted && award.titleUp != null) { kotlinx.coroutines.delay(if (animate) 2800 else 0); showTitleUp = true }
    }
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState()).navigationBarsPadding()) {
            MockStage(originY = 84, heightPx = 1580) {
                VerifyHeader(onClose, shareIcon = {
                    val text = "${store.name}에서 방문 인증을 마쳤어요! 노포 패스포트"
                    runCatching { context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text), null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                })
                MockPic(R.drawable.mk2_v11_title, 640, 205, Modifier.at(60, 205))
                // 폴라로이드
                Box(Modifier.at(30, 410).size(px(335), px(405)).rotate(-5f).shadow(8.dp, RoundedCornerShape(px(6))).background(Color.White)) {
                    Box(Modifier.padding(px(14)).fillMaxWidth().height(px(300)).clip(RoundedCornerShape(px(4)))) { StorePhoto(store, Modifier.fillMaxSize()) }
                    MockText(store.name, 28, Modifier.align(Alignment.BottomStart).padding(start = px(22), bottom = px(52)).width(px(290)), font = MockSerif, weight = FontWeight.ExtraBold)
                    MockText(androidx.compose.ui.res.stringResource(com.nopo.cheonnyeon.R.string.ver_16), 19, Modifier.align(Alignment.BottomStart).padding(start = px(22), bottom = px(20)), color = Hint)
                }
                // 도장
                Box(Modifier.at(385, 425).size(px(330)).graphicsLayer { scaleX = pop; scaleY = pop; alpha = if (pop > 1.5f) 0.0f else 1f }) {
                    VisitStamp(store.name, store.foundedYear, award.grade)
                }
                MockText("좋은 식당은\n시간이 지나도\n변하지 않는다.", 20, Modifier.at(595, 730).width(px(150)), color = Gray, maxLines = 3, lineHeightPx = 28)

                // 가게 카드
                Box(Modifier.at(15, 855).size(px(727), px(235)).clip(RoundedCornerShape(px(26))).background(CardWhite).border(1.dp, Hairline, RoundedCornerShape(px(26)))) {
                    Box(Modifier.padding(start = px(18), top = px(18)).size(px(250), px(198)).clip(RoundedCornerShape(px(18)))) {
                        StorePhoto(store, Modifier.fillMaxSize())
                        Row(Modifier.align(Alignment.BottomStart).padding(start = px(12), bottom = px(10)).clip(RoundedCornerShape(50)).background(Color(0x99000000)).padding(horizontal = px(14), vertical = px(4)), verticalAlignment = Alignment.CenterVertically) {
                            PinIcon(20, color = Color.White); Box(Modifier.width(px(6))); MockText(store.sigungu.ifBlank { store.sido }, 18, color = Color.White)
                        }
                    }
                    MockText(store.name, 31, Modifier.padding(start = px(295), top = px(18)).width(px(370)), font = MockSerif, weight = FontWeight.ExtraBold)
                    Box(Modifier.align(Alignment.TopEnd).padding(top = px(22), end = px(22))) { ChevronRight(30) }
                    Row(Modifier.padding(start = px(295), top = px(78)), horizontalArrangement = Arrangement.spacedBy(px(10))) {
                        listOf(store.categoryGroup.takeIf { it.isNotBlank() }, store.foundedYear?.let { "${it / 10 * 10}년대" }, store.sigungu.takeIf { it.isNotBlank() }).filterNotNull().forEach { t ->
                            Box(Modifier.clip(RoundedCornerShape(50)).background(Color(0xFFEFEBDD)).padding(horizontal = px(16), vertical = px(5))) { MockText(t, 19, color = Gray) }
                        }
                    }
                    val now = java.time.LocalDateTime.now(com.nopo.cheonnyeon.core.common.SeoulZone)
                    MockText("▣ " + fmtDate(now), 21, Modifier.padding(start = px(295), top = px(133)), color = Gray)
                    MockText("⌖ " + store.address, 21, Modifier.padding(start = px(295), top = px(172)).width(px(420)), color = Gray)
                }

                // 획득 카드
                Box(Modifier.at(15, 1105).size(px(727), px(228)).clip(RoundedCornerShape(px(26))).background(CardWhite).border(1.dp, Hairline, RoundedCornerShape(px(26)))) {
                    MockText(androidx.compose.ui.res.stringResource(com.nopo.cheonnyeon.R.string.ver_17), 25, Modifier.padding(start = px(22), top = px(12)), weight = FontWeight.ExtraBold)
                    // 1: 스탬프
                    Box(Modifier.padding(start = px(25), top = px(60)).size(px(100)).clip(CircleShape).border(2.dp, Red, CircleShape), contentAlignment = Alignment.Center) { MockText(store.name.take(2), 22, color = Red, font = MockSerif, weight = FontWeight.ExtraBold) }
                    MockText("+1", 27, Modifier.padding(start = px(138), top = px(66)), weight = FontWeight.ExtraBold)
                    MockText(androidx.compose.ui.res.stringResource(com.nopo.cheonnyeon.R.string.ver_18), 21, Modifier.padding(start = px(138), top = px(104)), weight = FontWeight.ExtraBold)
                    MockText("노포 여권에\n스탬프가 추가되었어요.", 18, Modifier.padding(start = px(25), top = px(170)).width(px(220)), color = Gray, maxLines = 2, lineHeightPx = 24)
                    Box(Modifier.padding(start = px(255), top = px(60)).size(1.dp, px(155)).background(Hairline))
                    // 2: 칭호
                    MockPic(R.drawable.mk2_v11_ic_star, 65, 75, Modifier.padding(start = px(268), top = px(66)))
                    MockText(stringResource(title.labelRes()), if (stringResource(title.labelRes()).length > 6) 21 else 25, Modifier.padding(start = px(338), top = px(66)).width(px(150)), weight = FontWeight.ExtraBold, maxLines = 2, lineHeightPx = 28)
                    MockText(androidx.compose.ui.res.stringResource(com.nopo.cheonnyeon.R.string.ver_19), 21, Modifier.padding(start = px(338), top = px(110)), weight = FontWeight.ExtraBold)
                    MockText(
                        if (award.titleUp != null) "새 칭호를 얻었어요!" else if (nextYears != null) "다음 칭호까지\n${nextYears}년 남았어요." else "최고 칭호예요!",
                        18, Modifier.padding(start = px(268), top = px(170)).width(px(200)), color = Gray, maxLines = 2, lineHeightPx = 24,
                    )
                    Box(Modifier.padding(start = px(478), top = px(60)).size(1.dp, px(155)).background(Hairline))
                    // 3: 지역
                    val region = state.region
                    MockPic(R.drawable.mk2_v11_ic_badge, 83, 90, Modifier.padding(start = px(492), top = px(58)))
                    MockText("${store.sido} 지역 스탬프\n진행도 +1", 18, Modifier.padding(start = px(580), top = px(60)).width(px(150)), weight = FontWeight.ExtraBold, maxLines = 2, lineHeightPx = 24)
                    if (region != null) {
                        MockText("${region.visited} / ${region.target}", 19, Modifier.padding(start = px(580), top = px(112)), color = Gray)
                        Box(Modifier.padding(start = px(580), top = px(140)).size(px(135), px(10)).clip(RoundedCornerShape(50)).background(Color(0xFFDDDACB))) {
                            Box(Modifier.fillMaxHeight0(region.visited, region.target).clip(RoundedCornerShape(50)).background(MockColors.LimeDeep))
                        }
                    }
                    MockText("${store.sido} 지역 노포 탐방\n진행 중이에요.", 18, Modifier.padding(start = px(492), top = px(170)).width(px(230)), color = Gray, maxLines = 2, lineHeightPx = 24)
                }

                // 내 노포 패스포트
                MockText(androidx.compose.ui.res.stringResource(com.nopo.cheonnyeon.R.string.ver_20), 29, Modifier.at(25, 1355), font = MockSerif, weight = FontWeight.ExtraBold)
                Box(Modifier.at(580, 1352).size(px(160), px(50)).clickable(role = Role.Button, onClick = onClose), contentAlignment = Alignment.CenterEnd) {
                    Row(verticalAlignment = Alignment.CenterVertically) { MockText(androidx.compose.ui.res.stringResource(com.nopo.cheonnyeon.R.string.ver_21), 21); Box(Modifier.width(px(8))); ChevronRight(24) }
                }
                Box(Modifier.at(15, 1402).size(px(727), px(150)).clip(RoundedCornerShape(px(20))).background(Color(0xFFF5EEDB)))
                val recent = state.recent.take(4)
                (0 until 5).forEach { i ->
                    val x = 40 + i * 140
                    val r = recent.getOrNull(i)
                    if (r != null) {
                        Box(Modifier.at(x, 1412).size(px(100)).clip(CircleShape).border(2.dp, if (i == 0) Red else Color(0xFF3C5A8A), CircleShape), contentAlignment = Alignment.Center) {
                            MockText(r.store.name.take(2), 22, color = if (i == 0) Red else Color(0xFF3C5A8A), font = MockSerif, weight = FontWeight.ExtraBold)
                        }
                        MockText(r.store.name, 17, Modifier.at(x - 15, 1518).width(px(130)), align = TextAlign.Center, weight = FontWeight.Bold)
                    } else {
                        Box(Modifier.at(x, 1412).size(px(100)).clip(CircleShape).border(1.5.dp, Color(0xFFC4BAA2), CircleShape), contentAlignment = Alignment.Center) { MockText("?", 34, color = Color(0xFFB8AE96), font = MockSerif) }
                        if (i == (recent.size).coerceAtLeast(0)) MockText("다음 노포를\n찾아보세요!", 16, Modifier.at(x - 15, 1518).width(px(130)), color = Hint, align = TextAlign.Center, maxLines = 2, lineHeightPx = 20)
                    }
                }
                // 버튼
                Box(Modifier.at(25, 1572).size(px(340), px(78)).clip(RoundedCornerShape(px(30))).border(1.dp, Hairline, RoundedCornerShape(px(30))).background(CardWhite).clickable(role = Role.Button, onClick = onClose), contentAlignment = Alignment.Center) {
                    Row(verticalAlignment = Alignment.CenterVertically) { MockPic(R.drawable.mk2_v11_ic_map, 40, 40); Box(Modifier.width(px(14))); MockText(androidx.compose.ui.res.stringResource(com.nopo.cheonnyeon.R.string.ver_22), 25, weight = FontWeight.ExtraBold) }
                }
                Box(Modifier.at(388, 1572).size(px(350), px(78)).clip(RoundedCornerShape(px(30))).background(MockColors.Lime2).clickable(role = Role.Button, onClick = onClose), contentAlignment = Alignment.Center) {
                    Row(verticalAlignment = Alignment.CenterVertically) { MockPic(R.drawable.mk2_v11_ic_next, 40, 40); Box(Modifier.width(px(12))); MockText(androidx.compose.ui.res.stringResource(com.nopo.cheonnyeon.R.string.ver_23), 25, weight = FontWeight.ExtraBold) }
                    Box(Modifier.align(Alignment.CenterEnd).padding(end = px(22))) { ChevronRight(26) }
                }
                Box(Modifier.at(25, 1660).size(px(700), px(40)), contentAlignment = Alignment.Center) {
                    TextButton(onClick = { onReview() }) { Text(stringResource(R.string.verify_done_review)) }
                }
            }
        }
        CelebrationOverlay(fire = impacted && animate, animate = animate, tier = title.ordinal)
    }
    award.titleUp?.let { t -> if (showTitleUp) TitleUpDialog(t, award.totalYearsAfter) { showTitleUp = false } }
}

private fun Modifier.fillMaxHeight0(visited: Int, target: Int): Modifier = this.fillMaxHeight().fillMaxWidthFraction(if (target <= 0) 0f else (visited.toFloat() / target).coerceIn(0f, 1f))

private fun Modifier.fillMaxHeight(): Modifier = this.then(Modifier.height(10.dp))

private fun Modifier.fillMaxWidthFraction(f: Float): Modifier = this.then(Modifier.fillMaxWidth(f))

/** 시안의 붉은 원형 도장: 윗부분 "노포 패스포트", 아랫부분 "방문 인증 완료", 가운데에 개업 연도와 가게 이름 */
@Composable
private fun VisitStamp(name: String, founded: Int?, grade: StampGrade) {
    val ink = when (grade) { StampGrade.GOLD -> Color(0xFFB8860B); else -> Red }
    Box(Modifier.fillMaxSize()) {
        Canvas(Modifier.fillMaxSize()) {
            val c = Offset(size.width / 2, size.height / 2)
            val r = size.minDimension / 2 - 4.dp.toPx()
            rotateDraw(-8f, c) {
                drawCircle(ink, r, c, style = Stroke(5.dp.toPx()))
                drawCircle(ink, r * 0.82f, c, style = Stroke(1.5.dp.toPx()))
                drawCircle(ink, r * 0.58f, c, style = Stroke(1.5.dp.toPx()))
                val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                    color = ink.toArgb(); textSize = r * 0.2f; typeface = android.graphics.Typeface.DEFAULT_BOLD; textAlign = android.graphics.Paint.Align.CENTER
                }
                val ringR = r * 0.70f
                val top = android.graphics.Path().apply { addArc(c.x - ringR, c.y - ringR, c.x + ringR, c.y + ringR, 200f, 140f) }
                val bottom = android.graphics.Path().apply { addArc(c.x - ringR - paint.textSize * 0.6f, c.y - ringR - paint.textSize * 0.6f, c.x + ringR + paint.textSize * 0.6f, c.y + ringR + paint.textSize * 0.6f, 160f, -140f) }
                drawContext.canvas.nativeCanvas.drawTextOnPath("노포 패스포트", top, 0f, 0f, paint)
                drawContext.canvas.nativeCanvas.drawTextOnPath("방문 인증 완료", bottom, 0f, 0f, paint)
                paint.textSize = r * 0.13f
                drawContext.canvas.nativeCanvas.drawText("★", c.x - r * 0.9f, c.y + r * 0.05f, paint)
                drawContext.canvas.nativeCanvas.drawText("★", c.x + r * 0.9f, c.y + r * 0.05f, paint)
            }
        }
        Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
            if (founded != null) MockText("EST. $founded", 22, color = ink, font = MockSerif, weight = FontWeight.Bold)
            MockText(name, if (name.length > 6) 25 else 32, Modifier.width(px(190)), color = ink, font = MockSerif, weight = FontWeight.ExtraBold, align = TextAlign.Center, maxLines = 2, lineHeightPx = 34)
        }
    }
}

@Composable
private fun RetryGlyph() {
    Canvas(Modifier.fillMaxSize()) {
        val w = size.width
        val st = Stroke(w * 0.09f, cap = StrokeCap.Round)
        drawArc(MockColors.Navy, 215f, 130f, false, Offset(w * 0.12f, w * 0.12f), androidx.compose.ui.geometry.Size(w * 0.76f, w * 0.76f), style = st)
        drawArc(MockColors.Navy, 35f, 130f, false, Offset(w * 0.12f, w * 0.12f), androidx.compose.ui.geometry.Size(w * 0.76f, w * 0.76f), style = st)
    }
}
