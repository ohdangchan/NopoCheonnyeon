package com.nopo.cheonnyeon.feature.permission

import com.nopo.cheonnyeon.R
import androidx.compose.ui.res.stringResource
import android.Manifest
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nopo.cheonnyeon.app.AppContainer
import com.nopo.cheonnyeon.platform.permission.PermissionStatus
import com.nopo.cheonnyeon.platform.permission.rememberPermission
import kotlinx.coroutines.launch

// 처음 권한 안내 줄 번호 ([IntroPermRows] 순서와 같다)
private const val INTRO_LOCATION = 0
private const val INTRO_CAMERA = 1
private const val INTRO_ALBUM = 2
private const val INTRO_NOTIFICATION = 3
private const val INTRO_SHARE = 4
private const val INTRO_ANALYTICS = 5

// 앱 안 허용 창 단계
private const val ASK_NONE = 0
private const val ASK_SHARE = 1
private const val ASK_ANALYTICS = 2

// 방문 기록 공개 범위 (프로필 문서의 값)
private const val VISIBILITY_ALL = "ALL"
private const val VISIBILITY_PRIVATE = "PRIVATE"

// 권한 관리 화면 줄 번호
private const val MANAGE_LOCATION = 0
private const val MANAGE_CAMERA = 1
private const val MANAGE_NOTIFICATION = 2
private const val MANAGE_MARKETING = 3
private const val MANAGE_SHARE = 4
private const val MANAGE_ANALYTICS = 5

/**
 * 처음 권한 안내: 여섯 가지(위치·카메라·사진 앨범·알림·방문 기록 공유·앱 사용 데이터 수집)를 눌러 고르고 한꺼번에 요청한다.
 * 앞의 네 가지는 시스템 허용 창이, 뒤의 두 가지는 앱의 허용 창이 차례로 뜬다. 안내를 마치면 다시 보이지 않게 기록한다.
 */
@Composable
fun PermissionIntroHost(container: AppContainer) {
    val scope = rememberCoroutineScope()
    var selected by rememberSaveable { mutableStateOf(IntroPermRows.indices.toSet()) }
    // 앱 안 허용 창 단계 (고른 것만 차례로 묻는다)
    var asking by remember { mutableIntStateOf(ASK_NONE) }
    var chosen by remember { mutableStateOf(emptySet<Int>()) }
    val finish = { scope.launch { container.settings.setPermIntroDone() }; Unit }

    fun saveVisibility(visibility: String) {
        container.auth.currentUid?.let { uid -> scope.launch { container.profiles.setVisibility(uid, visibility) } }
    }

    /** [from] 단계부터 고른 앱 안 항목이 있으면 그 허용 창을 띄우고, 없으면 안내를 마친다 */
    fun nextAsk(from: Int) {
        val next = (from..ASK_ANALYTICS).firstOrNull { (it + INTRO_SHARE - ASK_SHARE) in chosen } ?: ASK_NONE
        if (next == ASK_NONE) finish() else asking = next
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { nextAsk(ASK_SHARE) }

    fun run(pick: Set<Int>) {
        chosen = pick
        // 고르지 않은 앱 안 항목은 꺼 둔다(고른 것만 허용)
        if (INTRO_SHARE !in pick) saveVisibility(VISIBILITY_PRIVATE)
        if (INTRO_ANALYTICS !in pick) scope.launch { container.settings.setAnalyticsOn(false) }
        val list = mutableListOf<String>()
        if (INTRO_LOCATION in pick) {
            list += Manifest.permission.ACCESS_FINE_LOCATION
            list += Manifest.permission.ACCESS_COARSE_LOCATION
        }
        if (INTRO_CAMERA in pick) list += Manifest.permission.CAMERA
        if (INTRO_ALBUM in pick) list += AlbumPermission
        if (INTRO_NOTIFICATION in pick) NotificationPermission?.let { list += it }
        if (list.isEmpty()) nextAsk(ASK_SHARE) else launcher.launch(list.toTypedArray())
    }

    PermissionIntroScreen(
        selected = selected,
        onToggle = { i -> selected = if (i in selected) selected - i else selected + i },
        onAllowAll = {
            selected = IntroPermRows.indices.toSet()
            run(IntroPermRows.indices.toSet())
        },
        onAllowSelected = { run(selected) },
        onLater = { finish() },
    )

    if (asking == ASK_SHARE) AlertDialog(
        onDismissRequest = {},
        title = { Text(stringResource(R.string.ui_permission_01)) },
        text = { Text(stringResource(R.string.ui_permission_02)) },
        confirmButton = { TextButton({ saveVisibility(VISIBILITY_ALL); nextAsk(ASK_ANALYTICS) }) { Text(stringResource(R.string.ui_permission_03)) } },
        dismissButton = { TextButton({ saveVisibility(VISIBILITY_PRIVATE); nextAsk(ASK_ANALYTICS) }) { Text(stringResource(R.string.ui_permission_04)) } },
    )
    if (asking == ASK_ANALYTICS) AlertDialog(
        onDismissRequest = {},
        title = { Text(stringResource(R.string.ui_permission_05)) },
        text = {
            Text(stringResource(R.string.ui_permission_06))
        },
        confirmButton = { TextButton({ scope.launch { container.settings.setAnalyticsOn(true) };
             asking = ASK_NONE;
            finish() }) { Text(stringResource(R.string.ui_permission_03)) } },
        dismissButton = { TextButton({ scope.launch { container.settings.setAnalyticsOn(false) };
             asking = ASK_NONE;
            finish() }) { Text(stringResource(R.string.ui_permission_04)) } },
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
            if (perm == null || notificationsGranted(context)) onDone() else launcher.launch(perm)
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
    LaunchedEffect(Unit) {
        container.auth.currentUid?.let { uid ->
            container.profiles.profile(uid).valueOrNull?.let { share = it.visibility != VISIBILITY_PRIVATE }
        }
    }
    val states = remember(tick, albumTick, loc.status, cam.status, marketing, analytics, share) {
        listOf(
            locationGranted(context), cameraGranted(context), notificationsGranted(context),
            marketing, share, analytics, albumGranted(context),
        )
    }
    PermissionManageScreen(
        states = states, onBack = onBack,
        onRow = { i ->
            when (i) {
                MANAGE_LOCATION ->
                    if (states[0] || loc.status == PermissionStatus.PERMANENTLY_DENIED) openAppSettings(context) else loc.request()
                MANAGE_CAMERA ->
                    if (states[1] || cam.status == PermissionStatus.PERMANENTLY_DENIED) openAppSettings(context) else cam.request()
                MANAGE_NOTIFICATION -> if (states[2]) openNotificationSettings(context) else onNotificationScreen()
                MANAGE_MARKETING -> scope.launch { container.settings.setMarketing(!marketing) }
                MANAGE_SHARE -> {
                    val next = !share
                    share = next
                    scope.launch {
                        container.auth.currentUid?.let { container.profiles.setVisibility(
                            it,
                            if (next) VISIBILITY_ALL else VISIBILITY_PRIVATE
                        ) }
                    }
                }
                MANAGE_ANALYTICS -> scope.launch { container.settings.setAnalyticsOn(!analytics) }
                else -> if (states[6]) openAppSettings(context) else album.launch(AlbumPermission)
            }
        },
        onInfo = { openAppSettings(context) },
    )
}

/** 이 앱의 알림 설정 화면을 연다 */
private fun openNotificationSettings(context: Context) {
    runCatching {
        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}
