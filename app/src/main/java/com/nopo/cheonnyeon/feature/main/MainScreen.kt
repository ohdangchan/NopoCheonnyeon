package com.nopo.cheonnyeon.feature.main

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nopo.cheonnyeon.BuildConfig
import com.nopo.cheonnyeon.R
import com.nopo.cheonnyeon.app.AppContainer
import com.nopo.cheonnyeon.core.common.factoryOf
import com.nopo.cheonnyeon.core.designsystem.component.NopoButton
import com.nopo.cheonnyeon.core.designsystem.theme.NopoTheme
import com.nopo.cheonnyeon.feature.map.MapScreen
import com.nopo.cheonnyeon.feature.settings.SettingsScreen
import com.nopo.cheonnyeon.feature.passport.PassportScreen
import com.nopo.cheonnyeon.feature.passport.PassportViewModel
import com.nopo.cheonnyeon.feature.map.MapViewModel
import com.nopo.cheonnyeon.feature.explore.ExploreScreen
import com.nopo.cheonnyeon.feature.explore.ExploreViewModel
import com.nopo.cheonnyeon.feature.home.HomeScreen
import com.nopo.cheonnyeon.feature.home.HomeViewModel
import com.nopo.cheonnyeon.feature.my.MyScreen
import com.nopo.cheonnyeon.feature.my.MyViewModel

/** 로그인 뒤 첫 화면: 아래 탭 5개 (홈 / 탐색 / 지도 / 컬렉션 / 마이). 컬렉션은 패스포트, 마이는 설정 화면이다. */
@Composable
fun MainScreen(container: AppContainer, onOpenStore: (String) -> Unit, onOpenProfile: (String) -> Unit, onDiagnostics: () -> Unit, onOpenHall: () -> Unit, onOpenRoulette: () -> Unit, onOpenChat: (String) -> Unit = {}, onOpenNotifications: () -> Unit = {}) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val locationPermission = com.nopo.cheonnyeon.platform.permission.rememberPermission(android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION)
    androidx.compose.runtime.LaunchedEffect(locationPermission.status) {
        if (locationPermission.status == com.nopo.cheonnyeon.platform.permission.PermissionStatus.GRANTED) container.refreshLocation()
    }
    androidx.compose.runtime.LaunchedEffect(Unit) {
        if (locationPermission.status != com.nopo.cheonnyeon.platform.permission.PermissionStatus.GRANTED) locationPermission.request()
    }
    val mapVm: MapViewModel = viewModel(factory = factoryOf { MapViewModel(container) })
    val passportVm: PassportViewModel = viewModel(factory = factoryOf { PassportViewModel(container) })
    val homeVm: HomeViewModel = viewModel(factory = factoryOf { HomeViewModel(container) })
    val exploreVm: ExploreViewModel = viewModel(factory = factoryOf { ExploreViewModel(container) })
    val myVm: MyViewModel = viewModel(factory = factoryOf { MyViewModel(container) })
    var settingsOpen by rememberSaveable { mutableStateOf(false) }
    androidx.activity.compose.BackHandler(enabled = tab == 4 && settingsOpen) { settingsOpen = false }
    var showQueue by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(false) }
    val online by container.connectivity.online.collectAsStateWithLifecycle(initialValue = true)
    val pending by container.visits.pendingCount.collectAsStateWithLifecycle(initialValue = 0)
    androidx.compose.runtime.LaunchedEffect(online) { if (online) container.syncScheduler.enqueue() }
    val duplicateNotices by container.visits.duplicateNotices.collectAsStateWithLifecycle()
    if (duplicateNotices.isNotEmpty()) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = container.visits::dismissDuplicateNotice,
            title = { Text(stringResource(com.nopo.cheonnyeon.R.string.dup_notice_title)) },
            text = { Text(stringResource(com.nopo.cheonnyeon.R.string.dup_notice_body, duplicateNotices.joinToString(", "))) },
            confirmButton = { androidx.compose.material3.TextButton(container.visits::dismissDuplicateNotice) { Text(stringResource(com.nopo.cheonnyeon.R.string.verify_done_close)) } },
        )
    }
    com.nopo.cheonnyeon.core.designsystem.mock.ProvideMockScale {
    Column(Modifier.fillMaxSize().background(com.nopo.cheonnyeon.core.designsystem.mock.MockColors.Cream)) {
        com.nopo.cheonnyeon.core.designsystem.component.OfflineBanner(
            visible = !online, pendingCount = pending,
            modifier = Modifier.fillMaxWidth().clickable(role = androidx.compose.ui.semantics.Role.Button) { showQueue = true },
        )
        if (showQueue) PendingQueueDialog(container, onDismiss = { showQueue = false })
        val restoring by container.restoring.collectAsStateWithLifecycle()
        if (restoring) {
            Text(
                stringResource(R.string.restoring_banner), style = NopoTheme.typography.labelMedium, color = NopoTheme.colors.ink,
                modifier = Modifier.fillMaxWidth().background(NopoTheme.colors.tealSoft).padding(horizontal = NopoTheme.spacing.lg, vertical = NopoTheme.spacing.sm),
            )
        }
        Column(Modifier.weight(1f)) {
            when (tab) {
                0 -> HomeScreen(homeVm, onOpenStore, onSearch = { tab = 1 }, onOpenMy = { tab = 4 }, onOpenChat = onOpenChat)
                1 -> ExploreScreen(exploreVm, onOpenStore = onOpenStore, onOpenChat = onOpenChat, onBell = onOpenNotifications)
                2 -> MapScreen(mapVm, onOpenStore, onOpenChat, onBell = onOpenNotifications)
                3 -> PassportScreen(passportVm, onOpenStore, onOpenProfile, onOpenHall, onOpenRoulette, onBell = onOpenNotifications)
                else -> if (!settingsOpen) MyScreen(myVm, passportVm, onOpenStore, onOpenProfile, onOpenCollection = { tab = 3 }, onOpenSettings = { settingsOpen = true }, onBell = onOpenNotifications) else SettingsScreen(
                    container,
                    signOutButton = { SignOutButton(container) },
                    devTools = {
                        TestStoreButton(container)
                        TestDataDeleteButton(container)
                        if (BuildConfig.DEBUG) DebugVisitButton(container)
                        StampPreviewButtons()
                    },
                    onDiagnostics = onDiagnostics,
                )
            }
        }
        MockNavBar(tab) { tab = it }
    }
    }
}

/** 시안의 하단 탭 줄: 화면마다 선택된 탭 모양이 다른 그림을 그대로 쓰고, 다섯 칸을 눌러 이동한다 */
@Composable
private fun MockNavBar(selected: Int, onSelect: (Int) -> Unit) {
    val res = listOf(R.drawable.mk_nav_home, R.drawable.mk_nav_explore, R.drawable.mk_nav_map, R.drawable.mk_nav_collection, R.drawable.mk_nav_my)
    val labels = listOf(R.string.tab_home, R.string.tab_explore, R.string.tab_map, R.string.tab_passport, R.string.tab_settings)
    Column(Modifier.fillMaxWidth().background(com.nopo.cheonnyeon.core.designsystem.mock.MockColors.Cream).navigationBarsPadding()) {
        androidx.compose.foundation.layout.Box {
            com.nopo.cheonnyeon.core.designsystem.mock.MockPic(res[selected], 760, listOf(84, 98, 98, 87, 98)[selected])
            androidx.compose.foundation.layout.Row(Modifier.matchParentSize()) {
                for (i in 0..4) {
                    val label = stringResource(labels[i])
                    androidx.compose.foundation.layout.Box(
                        Modifier.weight(1f).fillMaxSize().clickable(role = androidx.compose.ui.semantics.Role.Tab) { onSelect(i) }
                            .semantics { contentDescription = label },
                    )
                }
            }
        }
    }
}

@Composable
private fun TabPlaceholder(textRes: Int, extra: @Composable () -> Unit = {}) {
    Column(
        Modifier.fillMaxSize().padding(NopoTheme.spacing.lg),
        verticalArrangement = Arrangement.spacedBy(NopoTheme.spacing.lg, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(stringResource(textRes), color = NopoTheme.colors.ink, style = NopoTheme.typography.titleMedium)
        extra()
    }
}

/** 현재 위치에 시험용 가게를 만든다 (디버그 전용). 위치 권한이 있어야 한다. */
@Composable
private fun TestStoreButton(container: AppContainer) {
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val permission = com.nopo.cheonnyeon.platform.permission.rememberPermission(
        android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION,
    )
    var message by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<Int?>(null) }
    NopoButton(stringResource(R.string.test_store_create), {
        if (permission.status != com.nopo.cheonnyeon.platform.permission.PermissionStatus.GRANTED) {
            permission.request()
        } else {
            message = R.string.test_store_working
            scope.launch {
                val fix = try {
                    com.nopo.cheonnyeon.platform.location.FusedLocationAdapter(container.appContext).bestFix(timeoutMs = 10_000L)
                } catch (_: Exception) {
                    null
                }
                if (fix == null) {
                    message = R.string.test_store_no_fix
                } else {
                    container.catalog.createTestStore(fix.lat, fix.lng)
                    message = R.string.test_store_done
                }
            }
        }
    })
    message?.let { Text(stringResource(it), color = NopoTheme.colors.ink2) }
}

/** 올라가지 않은 기록 목록과 "지금 올리기" (시안 offline_queue) */
@Composable
private fun PendingQueueDialog(container: AppContainer, onDismiss: () -> Unit) {
    var names by androidx.compose.runtime.remember { mutableStateOf<List<String>>(emptyList()) }
    androidx.compose.runtime.LaunchedEffect(Unit) {
        names = container.database.myVisitDao().observeAll().first()
            .filter { !it.synced && !it.storeId.startsWith(com.nopo.cheonnyeon.domain.model.Store.TEST_PREFIX) }
            .map { container.catalog.byId(it.storeId)?.name ?: it.storeId }
    }
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.queue_title)) },
        text = { Text(if (names.isEmpty()) stringResource(R.string.queue_empty) else stringResource(R.string.queue_body, names.joinToString(", "))) },
        confirmButton = { androidx.compose.material3.TextButton({ container.syncScheduler.enqueue(); onDismiss() }) { Text(stringResource(R.string.queue_sync_now)) } },
        dismissButton = { androidx.compose.material3.TextButton(onDismiss) { Text(stringResource(R.string.detail_back)) } },
    )
}

/** 테스트 가게와 테스트 도장을 모두 지운다 (FR-TST-03). 서버 기록은 건드리지 않는다. */
@Composable
private fun TestDataDeleteButton(container: AppContainer) {
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    var done by androidx.compose.runtime.remember { mutableStateOf(false) }
    NopoButton(stringResource(R.string.test_store_delete), {
        scope.launch {
            container.visits.clearTestData()
            container.settings.clearTestReviews()
            container.catalog.deleteTestStores()
            done = true
        }
    }, style = com.nopo.cheonnyeon.core.designsystem.component.NopoButtonStyle.Outline)
    if (done) Text(stringResource(R.string.test_store_deleted), color = NopoTheme.colors.ink2)
}

/** 올라가지 않은 기록이 있으면 경고한 뒤 로그아웃 (FR-AUTH-05) */
@Composable
private fun SignOutButton(container: AppContainer) {
    val pending by container.visits.pendingCount.collectAsStateWithLifecycle(initialValue = 0)
    var confirm by androidx.compose.runtime.remember { mutableStateOf(false) }
    var unsynced by androidx.compose.runtime.remember { mutableStateOf<List<String>>(emptyList()) }
    androidx.compose.runtime.LaunchedEffect(confirm) {
        if (confirm) {
            unsynced = container.database.myVisitDao().observeAll().first()
                .filter { !it.synced && !it.storeId.startsWith(com.nopo.cheonnyeon.domain.model.Store.TEST_PREFIX) }
                .map { container.catalog.byId(it.storeId)?.name ?: it.storeId }
        }
    }
    NopoButton(stringResource(R.string.auth_sign_out), { if (pending > 0) confirm = true else container.signOut() })
    if (confirm) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { confirm = false },
            title = { Text(stringResource(R.string.signout_warn_title)) },
            text = {
                Text(stringResource(R.string.signout_warn_body, pending) + if (unsynced.isEmpty()) "" else "\n\n" + stringResource(R.string.signout_warn_list, unsynced.joinToString(", ")))
            },
            confirmButton = { androidx.compose.material3.TextButton({ confirm = false; container.signOut() }) { Text(stringResource(R.string.auth_sign_out)) } },
            dismissButton = { androidx.compose.material3.TextButton({ confirm = false }) { Text(stringResource(R.string.detail_back)) } },
        )
    }
}

/** 디버그 전용: 영수증·위치 없이 가게 하나에 방문 기록을 만든다. 동기화·리뷰·프로필·복원 같은 서버 흐름을 시험할 때 쓴다. */
@Composable
private fun DebugVisitButton(container: AppContainer) {
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    var message by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<String?>(null) }
    NopoButton("가상 방문 기록 (디버그, 서버 동기화 시험)", {
        scope.launch {
            val visited = container.visits.visitRefs().map { it.storeId }.toSet()
            val store = container.catalog.all().firstOrNull { !it.isTest && it.id !in visited && it.foundedYear != null }
            if (store == null) {
                message = "기록할 가게가 없어요"
            } else {
                container.visits.record(store, com.nopo.cheonnyeon.domain.model.VerifyMethod.RECEIPT, true, null)
                container.syncScheduler.enqueue()
                message = "가상 방문 기록: " + store.name
            }
        }
    })
    message?.let { Text(it, color = NopoTheme.colors.ink2) }
}

/** 개발자 도구: 인증 없이 도장 찍기·폭죽·칭호 승급 연출을 미리 본다 */
@Composable
private fun StampPreviewButtons() {
    var preview by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<Int?>(null) }
    NopoButton(stringResource(R.string.dev_stamp_normal), { preview = 0 })
    NopoButton(stringResource(R.string.dev_stamp_red), { preview = 1 })
    NopoButton(stringResource(R.string.dev_stamp_gold), { preview = 2 })
    when (preview) {
        0 -> com.nopo.cheonnyeon.feature.verify.StampPreviewDialog(com.nopo.cheonnyeon.domain.model.StampGrade.NORMAL, 38, null) { preview = null }
        1 -> com.nopo.cheonnyeon.feature.verify.StampPreviewDialog(com.nopo.cheonnyeon.domain.model.StampGrade.RED, 72, null) { preview = null }
        2 -> com.nopo.cheonnyeon.feature.verify.StampPreviewDialog(com.nopo.cheonnyeon.domain.model.StampGrade.GOLD, 126, com.nopo.cheonnyeon.domain.model.Title.ELDER) { preview = null }
    }
}
