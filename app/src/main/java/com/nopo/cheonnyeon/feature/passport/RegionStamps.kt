package com.nopo.cheonnyeon.feature.passport

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.nopo.cheonnyeon.R
import com.nopo.cheonnyeon.core.designsystem.theme.NopoTheme
import com.nopo.cheonnyeon.domain.rule.RegionProgress
import com.nopo.cheonnyeon.domain.rule.RegionProgressRule

/** 시안 "컬렉션"의 지역 스탬프: 시·도마다 원형 도장 하나. 목표만큼 방문하면 붉은 도장이 찍힌다. */
@Composable
fun RegionStamps(regions: List<RegionProgress>) {
    val c = NopoTheme.colors
    val sp = NopoTheme.spacing
    val next = RegionProgressRule.next(regions)
    val earned = regions.count { it.stamped }
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        horizontalArrangement = Arrangement.spacedBy(sp.md),
        verticalArrangement = Arrangement.spacedBy(sp.md),
    ) {
        item(span = { GridItemSpan(3) }) {
            Column(Modifier.fillMaxWidth().background(c.tealSoft, RoundedCornerShape(16.dp)).padding(sp.md),
                verticalArrangement = Arrangement.spacedBy(sp.xs)) {
                Text(stringResource(R.string.region_stamps_count, earned), style = NopoTheme.typography.titleMedium, color = c.sign)
                if (next != null) {
                    Text(stringResource(R.string.region_stamps_next, next.sido, next.remaining), style = NopoTheme.typography.bodyMedium,
                        color = c.ink)
                    LinearProgressIndicator(
                        progress = { next.visited / next.target.toFloat() }, modifier = Modifier.fillMaxWidth(), color = c.accent, trackColor = c.surface,
                    )
                    Text(stringResource(R.string.region_stamps_progress, next.visited, next.target),
                        style = NopoTheme.typography.labelMedium, color = c.ink2)
                } else {
                    Text(stringResource(R.string.region_stamps_empty, RegionProgress.REGION_STAMP_TARGET),
                        style = NopoTheme.typography.bodyMedium, color = c.ink2)
                }
            }
        }
        items(regions, key = { it.sido }) { r -> RegionStamp(r) }
    }
}

@Composable
private fun RegionStamp(r: RegionProgress) {
    val c = NopoTheme.colors
    val ring = if (r.stamped) c.stamp else c.line
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(NopoTheme.spacing.xs)) {
        Box(
            Modifier.size(84.dp).rotate(if (r.stamped) -8f else 0f).border(BorderStroke(if (r.stamped) 3.dp else 2.dp, ring),
                CircleShape).padding(6.dp)
                .border(BorderStroke(1.dp, ring), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                if (r.stamped) r.sido else "?",
                style = NopoTheme.typography.titleSmall, color = if (r.stamped) c.stamp else c.ink2, textAlign = TextAlign.Center,
            )
        }
        Text(stringResource(R.string.regions_stamp_row, r.visited, r.target), style = NopoTheme.typography.labelMedium, color = c.ink2)
        if (!r.stamped) Text(r.sido, style = NopoTheme.typography.labelSmall, color = c.ink2)
    }
}
