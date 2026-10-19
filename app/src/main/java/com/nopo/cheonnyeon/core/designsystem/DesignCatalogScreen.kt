package com.nopo.cheonnyeon.core.designsystem

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.nopo.cheonnyeon.R
import com.nopo.cheonnyeon.core.designsystem.component.EmptyState
import com.nopo.cheonnyeon.core.designsystem.component.NopoButton
import com.nopo.cheonnyeon.core.designsystem.component.NopoButtonStyle
import com.nopo.cheonnyeon.core.designsystem.component.NopoPreviews
import com.nopo.cheonnyeon.core.designsystem.component.StampSize
import com.nopo.cheonnyeon.core.designsystem.component.StampView
import com.nopo.cheonnyeon.core.designsystem.component.StarRating
import com.nopo.cheonnyeon.core.designsystem.component.StarRatingInput
import com.nopo.cheonnyeon.core.designsystem.component.TitleBadge
import com.nopo.cheonnyeon.core.designsystem.component.YearsGauge
import com.nopo.cheonnyeon.core.designsystem.theme.NopoTheme
import com.nopo.cheonnyeon.domain.model.StampGrade
import com.nopo.cheonnyeon.domain.model.Title

/**
 * 공통 컴포넌트를 실기기에서 라이트·다크·큰 글씨로 확인하는 화면.
 * S-001에서 진단 화면이 생기면 진단 화면의 탭 하나로 옮긴다.
 */
@Composable
fun DesignCatalogScreen(contentPadding: PaddingValues, modifier: Modifier = Modifier) {
    var rating by rememberSaveable { mutableIntStateOf(0) }
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(NopoTheme.spacing.xl),
    ) {
        item {
            Text(
                text = stringResource(R.string.catalog_title),
                style = NopoTheme.typography.titleLarge,
                color = NopoTheme.colors.ink,
            )
        }
        item {
            Section(stringResource(R.string.catalog_section_buttons)) {
                NopoButton(stringResource(R.string.catalog_btn_primary), {}, Modifier.fillMaxWidth())
                NopoButton(stringResource(R.string.catalog_btn_stamp), {}, Modifier.fillMaxWidth(), style = NopoButtonStyle.Stamp)
                NopoButton(stringResource(R.string.catalog_btn_outline), {}, Modifier.fillMaxWidth(), style = NopoButtonStyle.Outline)
                NopoButton(stringResource(R.string.catalog_btn_disabled), {}, Modifier.fillMaxWidth(), enabled = false)
            }
        }
        item {
            Section(stringResource(R.string.catalog_section_stamps)) {
                Row(horizontalArrangement = Arrangement.spacedBy(NopoTheme.spacing.md), verticalAlignment = Alignment.CenterVertically) {
                    StampView(38, StampGrade.NORMAL)
                    StampView(85, StampGrade.RED)
                    StampView(120, StampGrade.GOLD)
                    StampView(62, StampGrade.RED, size = StampSize.Mini)
                }
                StampView(120, StampGrade.GOLD, size = StampSize.Ceremony)
            }
        }
        item {
            Section(stringResource(R.string.catalog_section_titles)) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(NopoTheme.spacing.sm),
                    verticalArrangement = Arrangement.spacedBy(NopoTheme.spacing.sm),
                    itemVerticalAlignment = Alignment.CenterVertically,
                ) {
                    Title.entries.forEach { TitleBadge(it) }
                }
            }
        }
        item {
            Section(stringResource(R.string.catalog_section_gauge)) {
                YearsGauge(totalYears = 442, yearsToNextTitle = 158)
                YearsGauge(totalYears = 1_000, yearsToNextTitle = null)
            }
        }
        item {
            Section(stringResource(R.string.catalog_section_rating)) {
                StarRating(4.7f)
                StarRatingInput(rating = rating, onRatingChange = { rating = it })
            }
        }
        item {
            Section(stringResource(R.string.catalog_section_empty)) {
                EmptyState(
                    title = stringResource(R.string.catalog_empty_title),
                    body = stringResource(R.string.catalog_empty_body),
                    illustration = { StampView(30, StampGrade.NORMAL) },
                    actionLabel = stringResource(R.string.catalog_empty_action),
                    onAction = {},
                )
            }
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(NopoTheme.spacing.md)) {
        Text(text = title, style = NopoTheme.typography.titleMedium, color = NopoTheme.colors.ink2)
        content()
    }
}

@NopoPreviews
@Composable
private fun DesignCatalogPreview() {
    NopoTheme {
        androidx.compose.material3.Surface(color = NopoTheme.colors.paper) {
            DesignCatalogScreen(PaddingValues(NopoTheme.spacing.lg))
        }
    }
}
