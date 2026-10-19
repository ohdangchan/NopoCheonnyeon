package com.nopo.cheonnyeon.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.nopo.cheonnyeon.core.designsystem.theme.NopoTheme
import com.nopo.cheonnyeon.domain.model.StampGrade

/** 0건·도장 0개·리뷰 0개 등 빈 화면. [illustration]에는 보통 흐린 StampView를 넣는다. */
@Composable
fun EmptyState(
    title: String,
    modifier: Modifier = Modifier,
    body: String? = null,
    illustration: (@Composable () -> Unit)? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = NopoTheme.spacing.xxl, vertical = NopoTheme.spacing.xxxl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(NopoTheme.spacing.md),
    ) {
        if (illustration != null) Box(Modifier.padding(bottom = NopoTheme.spacing.sm)) { illustration() }
        Text(
            text = title,
            style = NopoTheme.typography.titleMedium,
            color = NopoTheme.colors.ink,
            textAlign = TextAlign.Center,
        )
        if (body != null) {
            Text(
                text = body,
                style = NopoTheme.typography.bodyMedium,
                color = NopoTheme.colors.ink2,
                textAlign = TextAlign.Center,
            )
        }
        if (actionLabel != null && onAction != null) {
            NopoButton(
                text = actionLabel,
                onClick = onAction,
                style = NopoButtonStyle.Outline,
                size = NopoButtonSize.Medium,
                modifier = Modifier.padding(top = NopoTheme.spacing.sm),
            )
        }
    }
}

@NopoPreviews
@Composable
private fun EmptyStatePreview() = PreviewSurface {
    EmptyState(
        title = "아직 찍은 도장이 없어요",
        body = "가까운 노포에서 첫 세월을 모아 보세요.",
        illustration = { StampView(30, StampGrade.NORMAL) },
        actionLabel = "노포 지도 보기",
        onAction = {},
    )
}
