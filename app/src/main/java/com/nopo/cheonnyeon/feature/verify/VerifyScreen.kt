package com.nopo.cheonnyeon.feature.verify

import android.Manifest
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nopo.cheonnyeon.R
import com.nopo.cheonnyeon.core.designsystem.component.labelRes
import com.nopo.cheonnyeon.core.common.AppError
import com.nopo.cheonnyeon.core.common.message
import com.nopo.cheonnyeon.core.designsystem.component.NopoButton
import com.nopo.cheonnyeon.core.designsystem.component.NopoButtonStyle
import com.nopo.cheonnyeon.core.designsystem.component.StampView
import com.nopo.cheonnyeon.core.designsystem.theme.NopoTheme
import com.nopo.cheonnyeon.platform.camera.CameraPreview
import com.nopo.cheonnyeon.platform.camera.rememberCameraController
import com.nopo.cheonnyeon.platform.permission.PermissionHandle
import com.nopo.cheonnyeon.platform.permission.PermissionStatus
import com.nopo.cheonnyeon.platform.permission.rememberPermission

/** 방문 인증 화면 (S-604·S-607·S-608): ① 영수증 → ② 가게 사진·위치 → 결과 */
@Composable
fun VerifyScreen(vm: VerifyViewModel, onClose: () -> Unit) {
    val state by vm.state.collectAsStateWithLifecycle()
    val c = NopoTheme.colors
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

    Column(
        Modifier.fillMaxSize().background(c.paper).statusBarsPadding().navigationBarsPadding().padding(NopoTheme.spacing.lg),
        verticalArrangement = Arrangement.spacedBy(NopoTheme.spacing.md),
    ) {
        Text(state.store?.name.orEmpty(), style = NopoTheme.typography.headlineMedium, color = c.primary)
        state.clockSkewMinutes?.let { Text(stringResource(R.string.verify_clock_skew, it), style = NopoTheme.typography.bodySmall, color = c.stamp) }

        if (state.step == VerifyStep.DONE && state.award != null) {
            ResultSection(state, onClose, onWriteReview = { vm.requestReview(); onClose() })
            return@Column
        }
        if (state.blocked) {
            ErrorText(state.error, state.errorMeters)
            if (state.todayVisits.isNotEmpty()) {
                Text(stringResource(R.string.verify_today_title, state.todayVisits.size), style = NopoTheme.typography.titleSmall, color = c.ink)
                state.todayVisits.forEachIndexed { i, (name, years) ->
                    Text(stringResource(R.string.verify_today_item, i + 1, name, years), style = NopoTheme.typography.bodyMedium, color = c.ink2)
                }
            }
            NopoButton(stringResource(R.string.detail_back), onClose, modifier = Modifier.fillMaxWidth())
            return@Column
        }

        when (state.step) {
            VerifyStep.RECEIPT -> {
                Text(stringResource(R.string.verify_receipt_title), style = NopoTheme.typography.titleMedium, color = c.ink)
                Text(stringResource(R.string.verify_receipt_guide), style = NopoTheme.typography.bodyMedium, color = c.ink2)
                androidx.compose.foundation.layout.Box(Modifier.weight(1f).fillMaxWidth()) {
                    CameraArea(camera, cameraPermission, Modifier.fillMaxSize())
                    if (cameraPermission.status == PermissionStatus.GRANTED) ReceiptGuideOverlay(Modifier.fillMaxSize())
                }
                ErrorText(state.error, state.errorMeters)
                NopoButton(
                    text = stringResource(if (state.busy) R.string.verify_analyzing else R.string.verify_receipt_capture),
                    onClick = { vm.captureReceipt(camera) },
                    enabled = !state.busy && cameraPermission.status == PermissionStatus.GRANTED,
                    modifier = Modifier.fillMaxWidth(),
                )
                NopoButton(stringResource(R.string.verify_no_receipt), vm::goToPhoto, style = NopoButtonStyle.Outline, enabled = !state.busy, modifier = Modifier.fillMaxWidth())
            }

            VerifyStep.LOCATE -> {
                Text(stringResource(R.string.verify_locate_title), style = NopoTheme.typography.titleMedium, color = c.ink)
                Text(stringResource(R.string.verify_locate_guide), style = NopoTheme.typography.bodyMedium, color = c.ink2)
                if (state.busy) {
                    Text(
                        stringResource(R.string.verify_locating, state.locatingBest?.let { "%.0f".format(it) } ?: "-"),
                        style = NopoTheme.extraTypography.metric,
                        color = c.ink,
                    )
                }
                ErrorText(state.error, state.errorMeters)
                LocationActions(state, locationPermission, vm)
                NopoButton(stringResource(R.string.verify_back_to_receipt), vm::backToReceipt, style = NopoButtonStyle.Outline, enabled = !state.busy, modifier = Modifier.fillMaxWidth())
            }

            VerifyStep.PHOTO -> {
                Text(stringResource(R.string.verify_photo_title), style = NopoTheme.typography.titleMedium, color = c.ink)
                Text(stringResource(R.string.verify_photo_guide), style = NopoTheme.typography.bodyMedium, color = c.ink2)
                CameraArea(camera, cameraPermission, Modifier.weight(1f))
                ErrorText(state.error, state.errorMeters)
                if (state.failedPhotoAttempts > 0) {
                    Text(
                        stringResource(R.string.verify_attempts, state.failedPhotoAttempts, com.nopo.cheonnyeon.domain.rule.VerificationConfig.PHOTO_OCR_MAX_ATTEMPTS),
                        style = NopoTheme.extraTypography.metric,
                        color = c.ink2,
                    )
                }
                NopoButton(
                    text = stringResource(if (state.busy) R.string.verify_analyzing else R.string.verify_photo_capture),
                    onClick = { vm.capturePhoto(camera) },
                    enabled = !state.busy && cameraPermission.status == PermissionStatus.GRANTED,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (vm.canOfferLocationOnly()) {
                    Text(stringResource(R.string.verify_location_only_note), style = NopoTheme.typography.bodySmall, color = c.ink2)
                    NopoButton(stringResource(R.string.verify_location_only), vm::acceptLocationOnly, style = NopoButtonStyle.Stamp, enabled = !state.busy, modifier = Modifier.fillMaxWidth())
                }
            }

            VerifyStep.DONE -> Unit
        }
        if (!state.busy) NopoButton(stringResource(R.string.verify_cancel), onClose, style = NopoButtonStyle.Outline, modifier = Modifier.fillMaxWidth())
    }
}

/** 영수증을 맞출 네 모서리 틀 (FR-VER-03). 가운데 세로로 긴 영수증 모양 */
@Composable
private fun ReceiptGuideOverlay(modifier: Modifier) {
    val color = NopoTheme.colors.brass
    Canvas(modifier) {
        val w = size.width * 0.7f
        val h = size.height * 0.85f
        val left = (size.width - w) / 2
        val top = (size.height - h) / 2
        val len = 36.dp.toPx()
        val stroke = 4.dp.toPx()
        fun corner(x: Float, y: Float, dx: Float, dy: Float) {
            drawLine(color, Offset(x, y), Offset(x + dx * len, y), stroke)
            drawLine(color, Offset(x, y), Offset(x, y + dy * len), stroke)
        }
        corner(left, top, 1f, 1f)
        corner(left + w, top, -1f, 1f)
        corner(left, top + h, 1f, -1f)
        corner(left + w, top + h, -1f, -1f)
    }
}

@Composable
private fun CameraArea(camera: com.nopo.cheonnyeon.platform.camera.CameraController, permission: PermissionHandle, modifier: Modifier) {
    if (permission.status == PermissionStatus.GRANTED) {
        CameraPreview(camera, modifier.fillMaxWidth())
    } else {
        Column(modifier, verticalArrangement = Arrangement.spacedBy(NopoTheme.spacing.sm)) {
            Text(stringResource(R.string.perm_camera_needed), color = NopoTheme.colors.ink)
            NopoButton(
                stringResource(if (permission.status == PermissionStatus.PERMANENTLY_DENIED) R.string.perm_open_settings else R.string.perm_request),
                { if (permission.status == PermissionStatus.PERMANENTLY_DENIED) permission.openSettings() else permission.request() },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun LocationActions(state: VerifyUiState, permission: PermissionHandle, vm: VerifyViewModel) {
    val granted = permission.status == PermissionStatus.GRANTED
    NopoButton(
        text = stringResource(
            when {
                state.busy -> R.string.verify_measuring
                !granted -> R.string.perm_request
                else -> R.string.verify_measure
            },
        ),
        onClick = {
            if (!granted) {
                if (permission.status == PermissionStatus.PERMANENTLY_DENIED) permission.openSettings() else permission.request()
            } else {
                vm.measureLocation(permissionGranted = true, approximateOnly = permission.approximateOnly)
            }
        },
        enabled = !state.busy,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun ErrorText(error: AppError?, meters: Int) {
    if (error == null) return
    Text(error.message(LocalContext.current, meters), color = NopoTheme.colors.stamp, style = NopoTheme.typography.bodyMedium)
}

@Composable
private fun ResultSection(state: VerifyUiState, onClose: () -> Unit, onWriteReview: () -> Unit) {
    val c = NopoTheme.colors
    val award = state.award ?: return
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(NopoTheme.spacing.md)) {
        // 도장 연출 (FR-STP-02): 내려와 쿵 찍히고, 충격 고리·인주 튀김·화면 흔들림·진동이 이어진다. 애니메이션을 끄면 바로 정적으로 보인다.
        val context = LocalContext.current
        val animate = remember { animationsEnabled(context) }
        com.nopo.cheonnyeon.core.designsystem.component.StampCeremony(
            years = award.years, grade = award.grade, animate = animate, onImpact = { vibrateOnce(context) },
        )
        Text(stringResource(R.string.verify_done_title), style = NopoTheme.typography.headlineMedium, color = c.ink)
        Text(
            stringResource(if (award.firstVisit) R.string.verify_done_years else R.string.verify_done_revisit, award.years),
            style = NopoTheme.typography.titleMedium,
            color = c.primary,
        )
        Text(stringResource(R.string.verify_done_total, award.totalYearsAfter), style = NopoTheme.extraTypography.metric, color = c.ink)
        // 인증 전 → 후 세월, 천 년 게이지와 현재 칭호 (시안 stamp_ceremony)
        val before = award.totalYearsAfter - (if (award.firstVisit) award.years else 0)
        if (award.firstVisit) Text(stringResource(R.string.detail_reward_total, before, award.totalYearsAfter), style = NopoTheme.extraTypography.metric, color = c.ink2)
        com.nopo.cheonnyeon.core.designsystem.component.YearsGauge(
            totalYears = award.totalYearsAfter,
            yearsToNextTitle = com.nopo.cheonnyeon.domain.rule.TitlePolicy.yearsToNext(award.totalYearsAfter),
            modifier = Modifier.fillMaxWidth(),
        )
        com.nopo.cheonnyeon.core.designsystem.component.TitleBadge(com.nopo.cheonnyeon.domain.rule.TitlePolicy.titleFor(award.totalYearsAfter))
        award.titleUp?.let { Text(stringResource(R.string.verify_title_up, stringResource(titleLabel(it))), style = NopoTheme.typography.titleMedium, color = c.stamp) }
        var showTitleUp by remember { mutableStateOf(award.titleUp != null) }
        award.titleUp?.let { t -> if (showTitleUp) TitleUpDialog(t, award.totalYearsAfter) { showTitleUp = false } }
        if (award.method == com.nopo.cheonnyeon.domain.model.VerifyMethod.LOCATION_ONLY) {
            Text(stringResource(R.string.verify_done_location_only), style = NopoTheme.typography.bodySmall, color = c.ink2)
        }
        Text(stringResource(R.string.verify_done_pending), style = NopoTheme.typography.bodySmall, color = c.ink2)
        NopoButton(stringResource(R.string.verify_done_review), onWriteReview, modifier = Modifier.fillMaxWidth())
        NopoButton(stringResource(R.string.verify_done_close), onClose, style = NopoButtonStyle.Outline, modifier = Modifier.fillMaxWidth())
    }
}

private fun titleLabel(t: com.nopo.cheonnyeon.domain.model.Title): Int = t.labelRes()

private fun animationsEnabled(context: android.content.Context): Boolean =
    android.provider.Settings.Global.getFloat(context.contentResolver, android.provider.Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f

@Suppress("DEPRECATION")
private fun vibrateOnce(context: android.content.Context) {
    val vibrator = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
        (context.getSystemService(android.content.Context.VIBRATOR_MANAGER_SERVICE) as android.os.VibratorManager).defaultVibrator
    } else {
        context.getSystemService(android.content.Context.VIBRATOR_SERVICE) as android.os.Vibrator
    }
    vibrator.vibrate(android.os.VibrationEffect.createOneShot(40, android.os.VibrationEffect.DEFAULT_AMPLITUDE))
}
