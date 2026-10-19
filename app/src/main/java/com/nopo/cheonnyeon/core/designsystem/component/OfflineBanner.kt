package com.nopo.cheonnyeon.core.designsystem.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import com.nopo.cheonnyeon.R
import com.nopo.cheonnyeon.core.designsystem.theme.NopoTheme

/** 화면 상단 오프라인 안내. 상태가 바뀌면 TalkBack이 읽는다. */
@Composable
fun OfflineBanner(
    visible: Boolean,
    modifier: Modifier = Modifier,
    pendingCount: Int = 0,
) {
    AnimatedVisibility(visible = visible, enter = expandVertically(), exit = shrinkVertically()) {
        Text(
            text = if (pendingCount > 0) {
                stringResource(R.string.offline_banner_pending, pendingCount)
            } else {
                stringResource(R.string.offline_banner)
            },
            style = NopoTheme.typography.labelMedium,
            color = NopoTheme.colors.ink,
            modifier = modifier
                .fillMaxWidth()
                .background(NopoTheme.colors.stampSoft)
                .padding(horizontal = NopoTheme.spacing.lg, vertical = NopoTheme.spacing.sm)
                .semantics { liveRegion = LiveRegionMode.Polite },
        )
    }
}

@NopoPreviews
@Composable
private fun OfflineBannerPreview() = PreviewSurface {
    OfflineBanner(visible = true)
    OfflineBanner(visible = true, pendingCount = 3)
}
