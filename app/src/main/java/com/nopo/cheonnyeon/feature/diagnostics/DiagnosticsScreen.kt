package com.nopo.cheonnyeon.feature.diagnostics

import android.os.Build
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.nopo.cheonnyeon.BuildConfig
import com.nopo.cheonnyeon.R
import com.nopo.cheonnyeon.core.designsystem.DesignCatalogScreen
import com.nopo.cheonnyeon.core.designsystem.component.NopoPreviews
import com.nopo.cheonnyeon.core.designsystem.theme.NopoTheme

private enum class DiagTab(@param:StringRes val label: Int) {
    Map(R.string.diag_tab_map),
    Camera(R.string.diag_tab_camera),
    Ocr(R.string.diag_tab_ocr),
    Location(R.string.diag_tab_location),
    Firebase(R.string.diag_tab_firebase),
    Design(R.string.diag_tab_design),
}

/**
 * 1일차 기능 검증용 진단 화면 (SPK-0). 최종 앱에도 설정 → 앱 정보 5회 탭으로 남는다.
 * 지도 탭은 SPK-1(네이버 지도 키 필요), 나머지는 SPK-2~5.
 */
@Composable
fun DiagnosticsScreen(modifier: Modifier = Modifier) {
    // 회전·프로세스 재생성 후에도 보던 탭 유지
    var selected by rememberSaveable { mutableIntStateOf(0) }
    val tabs = DiagTab.entries
    val c = NopoTheme.colors

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(c.paper),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(c.sign)
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
                .padding(horizontal = NopoTheme.spacing.lg, vertical = NopoTheme.spacing.md),
        ) {
            Text(stringResource(R.string.diag_title), style = NopoTheme.typography.titleLarge, color = c.onSign)
            DeviceInfo()
        }
        PrimaryScrollableTabRow(
            selectedTabIndex = selected,
            containerColor = c.surface,
            contentColor = c.primary,
            edgePadding = NopoTheme.spacing.sm,
            indicator = {
                TabRowDefaults.PrimaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(selected, matchContentSize = true),
                    color = c.primary,
                )
            },
        ) {
            tabs.forEachIndexed { i, tab ->
                Tab(
                    selected = selected == i,
                    onClick = { selected = i },
                    text = { Text(stringResource(tab.label), style = NopoTheme.typography.labelLarge) },
                    selectedContentColor = c.primary,
                    unselectedContentColor = c.ink2,
                )
            }
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal)),
        ) {
            when (tabs[selected]) {
                DiagTab.Design -> DesignCatalogScreen(contentPadding = PaddingValues(NopoTheme.spacing.lg))
                DiagTab.Firebase -> DiagnosticsFirebaseTab()
                DiagTab.Camera -> DiagnosticsCameraTab()
                DiagTab.Ocr -> DiagnosticsOcrTab()
                DiagTab.Location -> DiagnosticsLocationTab()
                else -> Preparing()
            }
        }
    }
}

@Composable
private fun DeviceInfo() {
    val style = NopoTheme.extraTypography.metric
    val color = NopoTheme.colors.onSign
    Text(stringResource(R.string.diag_device, Build.MANUFACTURER, Build.MODEL), style = style, color = color)
    Text(stringResource(R.string.diag_android, Build.VERSION.RELEASE, Build.VERSION.SDK_INT), style = style, color = color)
    Text(
        stringResource(R.string.diag_app_version, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE),
        style = style,
        color = color,
    )
}

@Composable
private fun Preparing() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = stringResource(R.string.diag_preparing),
            style = NopoTheme.typography.bodyLarge,
            color = NopoTheme.colors.ink2,
        )
    }
}

@NopoPreviews
@Composable
private fun DiagnosticsScreenPreview() {
    NopoTheme { DiagnosticsScreen() }
}
