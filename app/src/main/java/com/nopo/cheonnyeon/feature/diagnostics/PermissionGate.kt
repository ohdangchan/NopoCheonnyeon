package com.nopo.cheonnyeon.feature.diagnostics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.nopo.cheonnyeon.R
import com.nopo.cheonnyeon.core.designsystem.component.NopoButton
import com.nopo.cheonnyeon.core.designsystem.component.NopoButtonStyle
import com.nopo.cheonnyeon.core.designsystem.theme.NopoTheme
import com.nopo.cheonnyeon.platform.permission.PermissionHandle
import com.nopo.cheonnyeon.platform.permission.PermissionStatus

/** 권한이 없을 때의 안내. 처음엔 요청 버튼, '다시 묻지 않음'이면 설정 이동 버튼 (M-10). */
@Composable
fun PermissionGate(handle: PermissionHandle, message: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(NopoTheme.spacing.xxl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(NopoTheme.spacing.md),
    ) {
        Text(message, style = NopoTheme.typography.bodyLarge, color = NopoTheme.colors.ink, textAlign = TextAlign.Center)
        if (handle.status == PermissionStatus.PERMANENTLY_DENIED) {
            Text(
                stringResource(R.string.perm_denied_forever),
                style = NopoTheme.typography.bodyMedium,
                color = NopoTheme.colors.ink2,
                textAlign = TextAlign.Center,
            )
            NopoButton(stringResource(R.string.perm_open_settings), handle.openSettings, style = NopoButtonStyle.Outline)
        } else {
            NopoButton(stringResource(R.string.perm_request), handle.request)
        }
    }
}
