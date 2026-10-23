package com.nopo.cheonnyeon.feature.permission

import android.Manifest
import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nopo.cheonnyeon.app.AppContainer
import com.nopo.cheonnyeon.platform.permission.PermissionStatus
import com.nopo.cheonnyeon.platform.permission.rememberPermission
import kotlinx.coroutines.launch

/** 처음 권한 안내: 고른 권한을 한꺼번에 요청하고 안내를 마친 것으로 기록한다 */
@Composable
fun PermissionIntroHost(container: AppContainer) {
    val scope = rememberCoroutineScope()
    val finish = { scope.launch { container.settings.setPermIntroDone() }; Unit }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { finish() }
    PermissionIntroScreen(
        onAllowAll = {
            val list = mutableListOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.CAMERA)
            NotificationPermission?.let { list += it }
            launcher.launch(list.toTypedArray())
        },
        onAllowRequired = { launcher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.CAMERA)) },
        onLater = { finish() },
    )
}

/** 알림 권한 안내(시안 16): 허용하면 시스템 요청을 띄운다 */
@Composable
fun NotificationPermissionHost(onDone: () -> Unit) {
    val perm = NotificationPermission
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { onDone() }
    val context = LocalContext.current
    NotificationPermissionScreen(
        onAllow = {
            if (perm == null || notificationsGranted(context)) { onDone() }
            else launcher.launch(perm)
        },
        onLater = onDone,
    )
}

/** 권한 관리(시안 18): 권한 세 칸은 실제 상태, 나머지 세 칸은 앱 안의 선택 */
@Composable
fun PermissionManageHost(container: AppContainer, onBack: () -> Unit, onNotificationScreen: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val tick = rememberResumeTick()
    val loc = rememberPermission(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
    val cam = rememberPermission(Manifest.permission.CAMERA)
    val marketing by container.settings.marketing.collectAsStateWithLifecycle(initialValue = false)
    val analytics by container.settings.analyticsOn.collectAsStateWithLifecycle(initialValue = true)
    var share by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) { container.auth.currentUid?.let { uid -> container.profiles.profile(uid).valueOrNull?.let { share = it.visibility != "PRIVATE" } } }
    val states = remember(tick, loc.status, cam.status, marketing, analytics, share) {
        listOf(locationGranted(context), cameraGranted(context), notificationsGranted(context), marketing, share, analytics)
    }
    PermissionManageScreen(
        states = states, onBack = onBack,
        onRow = { i ->
            when (i) {
                0 -> if (states[0] || loc.status == PermissionStatus.PERMANENTLY_DENIED) openAppSettings(context) else loc.request()
                1 -> if (states[1] || cam.status == PermissionStatus.PERMANENTLY_DENIED) openAppSettings(context) else cam.request()
                2 -> if (states[2]) {
                    runCatching { context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                } else onNotificationScreen()
                3 -> scope.launch { container.settings.setMarketing(!marketing) }
                4 -> {
                    val next = !share
                    share = next
                    scope.launch { container.auth.currentUid?.let { container.profiles.setVisibility(it, if (next) "ALL" else "PRIVATE") } }
                }
                else -> scope.launch { container.settings.setAnalyticsOn(!analytics) }
            }
        },
        onInfo = { openAppSettings(context) },
    )
}
