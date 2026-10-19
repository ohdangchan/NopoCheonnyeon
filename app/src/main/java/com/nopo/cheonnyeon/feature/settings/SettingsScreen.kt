package com.nopo.cheonnyeon.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.nopo.cheonnyeon.R
import com.nopo.cheonnyeon.app.AppContainer
import com.nopo.cheonnyeon.core.designsystem.component.NopoButton
import com.nopo.cheonnyeon.core.designsystem.component.NopoButtonStyle
import com.nopo.cheonnyeon.core.designsystem.theme.NopoTheme

private const val DEV_TAPS = 5

/** 설정 (S-903): 앱 정보·데이터 출처·오픈소스 라이선스·로그아웃. 앱 정보를 5번 누르면 진단 도구가 열린다 (SC-8). */
@Composable
fun SettingsScreen(container: AppContainer, signOutButton: @Composable () -> Unit, devTools: @Composable () -> Unit, onDiagnostics: () -> Unit) {
    val c = NopoTheme.colors
    val context = LocalContext.current
    var taps by rememberSaveable { mutableIntStateOf(0) }
    var catalogInfo by remember { mutableStateOf<Pair<Long, Int>?>(null) }
    var showLicenses by rememberSaveable { mutableStateOf(false) }
    val version = remember { runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull().orEmpty() }

    LaunchedEffect(Unit) {
        catalogInfo = container.settings.catalogVersion() to container.catalog.all().size
    }

    Column(
        Modifier.fillMaxSize().background(c.paper).statusBarsPadding().verticalScroll(rememberScrollState()).padding(NopoTheme.spacing.lg),
        verticalArrangement = Arrangement.spacedBy(NopoTheme.spacing.md),
    ) {
        Text(stringResource(R.string.tab_settings), style = NopoTheme.typography.headlineMedium, color = c.primary)

        Section(stringResource(R.string.settings_app_info)) {
            Text(
                stringResource(R.string.settings_version, version),
                color = c.ink,
                modifier = Modifier.fillMaxWidth().clickable { taps++ },
            )
            catalogInfo?.let { (v, n) -> Text(stringResource(R.string.settings_catalog, n, v), color = c.ink2, style = NopoTheme.typography.bodySmall) }
        }
        Section(stringResource(R.string.settings_data_source)) {
            Text(stringResource(R.string.settings_data_source_body), color = c.ink2, style = NopoTheme.typography.bodySmall)
        }
        Section(stringResource(R.string.settings_privacy)) {
            Text(stringResource(R.string.settings_privacy_body), color = c.ink2, style = NopoTheme.typography.bodySmall)
        }
        NopoButton(
            stringResource(if (showLicenses) R.string.settings_licenses_hide else R.string.settings_licenses),
            { showLicenses = !showLicenses },
            style = NopoButtonStyle.Outline,
            modifier = Modifier.fillMaxWidth(),
        )
        if (showLicenses) Text(stringResource(R.string.settings_licenses_body), color = c.ink2, style = NopoTheme.typography.bodySmall)

        if (taps >= DEV_TAPS) {
            Section(stringResource(R.string.settings_dev)) {
                NopoButton(stringResource(R.string.diag_title), onDiagnostics, style = NopoButtonStyle.Outline, modifier = Modifier.fillMaxWidth())
                devTools()
            }
        }
        signOutButton()
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    val c = NopoTheme.colors
    Column(
        Modifier.fillMaxWidth().background(c.surface, NopoTheme.shapes.md).padding(NopoTheme.spacing.md),
        verticalArrangement = Arrangement.spacedBy(NopoTheme.spacing.xs),
    ) {
        Text(title, style = NopoTheme.typography.titleSmall, color = c.primary)
        content()
    }
}
