package com.nopo.cheonnyeon.feature.permission

import android.Manifest
import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nopo.cheonnyeon.app.AppContainer
import com.nopo.cheonnyeon.platform.permission.PermissionStatus
import com.nopo.cheonnyeon.platform.permission.rememberPermission
import kotlinx.coroutines.launch

/**
 * 처음 권한 안내: 여섯 가지(위치·카메라·사진 앨범·알림·방문 기록 공유·앱 사용 데이터 수집)를 눌러 고르고 한꺼번에 요청한다.
 * 앞의 네 가지는 시스템 허용 창이, 뒤의 두 가지는 앱의 허용 창이 차례로 뜬다. 안내를 마치면 다시 보이지 않게 기록한다.
 */
@Composable
fun PermissionIntroHost(container: AppContainer) {
    val scope = rememberCoroutineScope()
    var selected by rememberSaveable { mutableStateOf(IntroPermRows.indices.toSet()) }
    // 앱 안 허용 창 단계: 0 없음, 1 방문 기록 공유, 2 앱 사용 데이터 수집 (고른 것만 차례로 묻는다)
    var asking by remember { mutableIntStateOf(0) }
    var chosen by remember { mutableStateOf(emptySet<Int>()) }
    val finish = { scope.launch { container.settings.setPermIntroDone() }; Unit }

    fun nextAsk(from: Int) {
        val n = (from..2).firstOrNull { (it + 3) in chosen } ?: 0
        if (n == 0) finish() else asking = n
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { nextAsk(1) }

    fun run(pick: Set<Int>) {
        chosen = pick
        // 고르지 않은 앱 안 항목은 꺼 둔다(고른 것만 허용)
        if (4 !in pick) container.auth.currentUid?.let { uid -> scope.launch { container.profiles.setVisibility(uid, "PRIVATE") } }
        if (5 !in pick) scope.launch { container.settings.setAnalyticsOn(false) }
        val list = mutableListOf<String>()
        if (0 in pick) { list += Manifest.permission.ACCESS_FINE_LOCATION; list += Manifest.permission.ACCESS_COARSE_LOCATION }
        if (1 in pick) list += Manifest.permission.CAMERA
        if (2 in pick) list += AlbumPermission
        if (3 in pick) NotificationPermission?.let { list += it }
        if (list.isEmpty()) nextAsk(1) else launcher.launch(list.toTypedArray())
    }

    PermissionIntroScreen(
        selected = selected,
        onToggle = { i -> selected = if (i in selected) selected - i else selected + i },
        onAllowAll = { selected = IntroPermRows.indices.toSet(); run(IntroPermRows.indices.toSet()) },
        onAllowSelected = { run(selected) },
        onLater = { finish() },
    )

    if (asking == 1) AlertDialog(
        onDismissRequest = {},
        title = { Text("방문 기록 공유") },
        text = { Text("다른 사용자와 내 방문 기록을 공유하고 나만의 노포 여정을 보여줄까요? 설정에서 언제든 바꿀 수 있어요.") },
        confirmButton = { TextButton({ container.auth.currentUid?.let { uid -> scope.launch { container.profiles.setVisibility(uid, "ALL") } }; nextAsk(2) }) { Text("허용") } },
        dismissButton = { TextButton({ container.auth.currentUid?.let { uid -> scope.launch { container.profiles.setVisibility(uid, "PRIVATE") } }; nextAsk(2) }) { Text("허용 안 함") } },
    )
    if (asking == 2) AlertDialog(
        onDismissRequest = {},
        title = { Text("앱 사용 데이터 수집") },
        text = { Text("서비스를 더 좋게 만들기 위해 앱 사용 데이터(화면 이용 등, 위치·영수증 내용은 제외)를 수집해도 될까요? 설정에서 언제든 끌 수 있어요.") },
        confirmButton = { TextButton({ scope.launch { container.settings.setAnalyticsOn(true) }; asking = 0; finish() }) { Text("허용") } },
        dismissButton = { TextButton({ scope.launch { container.settings.setAnalyticsOn(false) }; asking = 0; finish() }) { Text("허용 안 함") } },
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
    var albumTick by remember { mutableIntStateOf(0) }
    val album = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { albumTick++ }
    LaunchedEffect(Unit) { container.auth.currentUid?.let { uid -> container.profiles.profile(uid).valueOrNull?.let { share = it.visibility != "PRIVATE" } } }
    val states = remember(tick, albumTick, loc.status, cam.status, marketing, analytics, share) {
        listOf(locationGranted(context), cameraGranted(context), notificationsGranted(context), marketing, share, analytics, albumGranted(context))
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
                5 -> scope.launch { container.settings.setAnalyticsOn(!analytics) }
                else -> if (states[6]) openAppSettings(context) else album.launch(AlbumPermission)
            }
        },
        onInfo = { openAppSettings(context) },
    )
}
