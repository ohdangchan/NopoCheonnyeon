package com.nopo.cheonnyeon.feature.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.launch
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

/** 로그인 뒤 첫 화면: 아래 탭 3개 (지도 탐색 / 내 패스포트 / 설정). 패스포트·설정은 E7·E9에서 채운다. */
@Composable
fun MainScreen(container: AppContainer, onOpenStore: (String) -> Unit, onOpenProfile: (String) -> Unit, onDiagnostics: () -> Unit) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val mapVm: MapViewModel = viewModel(factory = factoryOf { MapViewModel(container) })
    val passportVm: PassportViewModel = viewModel(factory = factoryOf { PassportViewModel(container) })
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
    Column(Modifier.fillMaxSize()) {
        com.nopo.cheonnyeon.core.designsystem.component.OfflineBanner(visible = !online, pendingCount = pending, modifier = Modifier.fillMaxWidth())
        Column(Modifier.weight(1f)) {
            when (tab) {
                0 -> MapScreen(mapVm, onOpenStore)
                1 -> PassportScreen(passportVm, onOpenStore, onOpenProfile)
                else -> SettingsScreen(
                    container,
                    signOutButton = { SignOutButton(container) },
                    devTools = {
                        TestStoreButton(container)
                        if (BuildConfig.DEBUG) DebugVisitButton(container)
                    },
                    onDiagnostics = onDiagnostics,
                )
            }
        }
        NavigationBar {
            listOf(R.string.tab_map, R.string.tab_passport, R.string.tab_settings).forEachIndexed { i, label ->
                NavigationBarItem(selected = tab == i, onClick = { tab = i }, icon = {}, label = { Text(stringResource(label)) }, alwaysShowLabel = true)
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

/** 올라가지 않은 기록이 있으면 경고한 뒤 로그아웃 (FR-AUTH-05) */
@Composable
private fun SignOutButton(container: AppContainer) {
    val pending by container.visits.pendingCount.collectAsStateWithLifecycle(initialValue = 0)
    var confirm by androidx.compose.runtime.remember { mutableStateOf(false) }
    NopoButton(stringResource(R.string.auth_sign_out), { if (pending > 0) confirm = true else container.signOut() })
    if (confirm) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { confirm = false },
            title = { Text(stringResource(R.string.signout_warn_title)) },
            text = { Text(stringResource(R.string.signout_warn_body, pending)) },
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
