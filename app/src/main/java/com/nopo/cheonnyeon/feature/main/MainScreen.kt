package com.nopo.cheonnyeon.feature.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.nopo.cheonnyeon.feature.map.MapViewModel

/** 로그인 뒤 첫 화면: 아래 탭 3개 (지도 탐색 / 내 패스포트 / 설정). 패스포트·설정은 E7·E9에서 채운다. */
@Composable
fun MainScreen(container: AppContainer, onOpenStore: (String) -> Unit, onDiagnostics: () -> Unit) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val mapVm: MapViewModel = viewModel(factory = factoryOf { MapViewModel(container) })
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.weight(1f)) {
            when (tab) {
                0 -> MapScreen(mapVm, onOpenStore)
                1 -> TabPlaceholder(R.string.tab_passport_placeholder)
                else -> TabPlaceholder(R.string.tab_settings_placeholder) {
                    if (BuildConfig.DEBUG) {
                        NopoButton(stringResource(R.string.diag_title), onDiagnostics)
                        TestStoreButton(container)
                    }
                    NopoButton(stringResource(R.string.auth_sign_out), { container.auth.signOut() })
                }
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
