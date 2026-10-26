package com.nopo.cheonnyeon.feature.map

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.nopo.cheonnyeon.R
import com.nopo.cheonnyeon.core.designsystem.component.EmptyState
import com.nopo.cheonnyeon.core.designsystem.component.NopoButton
import com.nopo.cheonnyeon.core.designsystem.component.NopoPreviews
import com.nopo.cheonnyeon.core.designsystem.component.PreviewSurface
import com.nopo.cheonnyeon.core.designsystem.theme.NopoTheme
import com.nopo.cheonnyeon.domain.model.Store
import com.nopo.cheonnyeon.feature.store.ProgramBadges
import com.nopo.cheonnyeon.feature.store.StorePhoto

fun formatDistance(meters: Double): String =
    if (meters < 1000) "${(meters / 10).toInt() * 10}m" else "%.1fkm".format(meters / 1000)

/** 마커를 누르면 나오는 하단 시트 (FR-MAP-05, S-503) */
@Composable
fun StoreSheet(
    store: Store,
    years: Int,
    distanceM: Double?,
    visited: Boolean,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = NopoTheme.colors
    Column(
        modifier.fillMaxWidth().background(c.surface, RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)).padding(NopoTheme.spacing.lg),
        verticalArrangement = Arrangement.spacedBy(NopoTheme.spacing.sm),
    ) {
        Row(
            Modifier.fillMaxWidth().clip(NopoTheme.shapes.md).clickable(role = Role.Button, onClick = onOpen),
            horizontalArrangement = Arrangement.spacedBy(NopoTheme.spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StorePhoto(store, Modifier.size(72.dp).clip(NopoTheme.shapes.md))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(NopoTheme.spacing.xs)) {
                ProgramBadges(store)
                Text(store.name, style = NopoTheme.typography.titleLarge, color = c.ink)
                Text(
                    listOfNotNull(
                        stringResource(R.string.years_value, years),
                        distanceM?.let { formatDistance(it) },
                        store.sigungu.ifBlank { null },
                    ).joinToString(" · "),
                    style = NopoTheme.typography.bodyMedium,
                    color = c.ink2,
                )
            }
        }
        Text(
            if (visited) stringResource(R.string.sheet_visited) else stringResource(R.string.sheet_reward, years),
            style = NopoTheme.typography.bodyMedium,
            color = if (visited) c.stamp else c.primary,
        )
        NopoButton(stringResource(R.string.sheet_open), onOpen, modifier = Modifier.fillMaxWidth())
    }
}

/** 목록 보기: 가까운 순 / 업력 순 (FR-MAP-07, S-504). 좌표 없는 가게도 나온다. */
@Composable
fun StoreListScreen(state: MapUiState, onSort: (ListSort) -> Unit, onOpenStore: (String) -> Unit) {
    val c = NopoTheme.colors
    Column(Modifier.fillMaxWidth().background(c.paper)) {
        Row(
            Modifier.padding(horizontal = NopoTheme.spacing.md, vertical = NopoTheme.spacing.sm),
            horizontalArrangement = Arrangement.spacedBy(NopoTheme.spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FilterChip(state.sort == ListSort.NEAREST, { onSort(ListSort.NEAREST) },
                label = { Text(stringResource(R.string.list_sort_nearest)) })
            FilterChip(state.sort == ListSort.YEARS, { onSort(ListSort.YEARS) }, label = { Text(stringResource(R.string.list_sort_years)) })
        }
        Text(
            stringResource(R.string.list_count, state.filtered.size),
            style = NopoTheme.typography.bodySmall,
            color = c.ink2,
            modifier = Modifier.padding(horizontal = NopoTheme.spacing.lg),
        )
        if (state.filtered.isEmpty()) {
            EmptyState(
                title = stringResource(if (state.loading) R.string.list_loading else R.string.list_empty_title),
                body = if (state.loading) null else stringResource(R.string.list_empty_body),
            )
            return@Column
        }
        LazyColumn(contentPadding = PaddingValues(NopoTheme.spacing.md),
            verticalArrangement = Arrangement.spacedBy(NopoTheme.spacing.sm)) {
            items(state.sorted, key = { it.id }) { s -> StoreRow(s, state, onOpenStore) }
        }
    }
}

@Composable
private fun StoreRow(s: Store, state: MapUiState, onOpen: (String) -> Unit) {
    val c = NopoTheme.colors
    Row(
        Modifier.fillMaxWidth().clip(NopoTheme.shapes.md).background(c.surface)
            .clickable(role = Role.Button) { onOpen(s.id) }.padding(NopoTheme.spacing.md),
        horizontalArrangement = Arrangement.spacedBy(NopoTheme.spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StorePhoto(s, Modifier.size(64.dp).clip(NopoTheme.shapes.sm))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(NopoTheme.spacing.xxs)) {
            ProgramBadges(s)
            Text(s.name, style = NopoTheme.typography.titleMedium, color = c.ink)
            Text(
                listOfNotNull(
                    stringResource(R.string.years_value, state.yearsOf(s)),
                    state.distanceTo(s)?.let { formatDistance(it) },
                    (s.sido + " " + s.sigungu).trim().ifBlank { null },
                    if (s.id in state.visitedIds) stringResource(R.string.list_visited) else null,
                ).joinToString(" · "),
                style = NopoTheme.typography.bodySmall,
                color = c.ink2,
            )
        }
    }
}

private val previewStore = Store(id = "P-1", name = "노포천년식당", programs = listOf("백년가게"), sido = "서울", sigungu = "송파구", foundedYear = 1980)

@NopoPreviews
@Composable
private fun StoreSheetPreview() {
    PreviewSurface {
        StoreSheet(previewStore, years = 46, distanceM = 320.0, visited = false, onOpen = {})
        StoreSheet(previewStore, years = 46, distanceM = null, visited = true, onOpen = {})
    }
}

@NopoPreviews
@Composable
private fun StoreListPreview() {
    val stores = listOf(previewStore, previewStore.copy(id = "P-2", name = "오래된국수집", foundedYear = 1950))
    PreviewSurface { StoreListScreen(MapUiState(loading = false,
        filtered = stores), onSort = {}, onOpenStore = {}) }
}
