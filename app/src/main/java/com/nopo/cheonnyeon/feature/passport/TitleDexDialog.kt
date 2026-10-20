package com.nopo.cheonnyeon.feature.passport

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.nopo.cheonnyeon.R
import com.nopo.cheonnyeon.core.designsystem.component.TitleBadge
import com.nopo.cheonnyeon.core.designsystem.theme.NopoTheme
import com.nopo.cheonnyeon.domain.model.Title
import com.nopo.cheonnyeon.domain.rule.TitlePolicy
import com.nopo.cheonnyeon.feature.verify.TitleUpDialog

/**
 * 칭호 도감: 11개 칭호와 필요 세월, 내가 얻은 칭호를 한눈에 본다. 칭호를 누르면 승급 연출을 미리 볼 수 있다
 * (배지 색 변화와 연출을 실제 인증 없이 확인하는 용도).
 */
@Composable
fun TitleDexDialog(totalYears: Int, onDismiss: () -> Unit) {
    val c = NopoTheme.colors
    val mine = TitlePolicy.titleFor(totalYears)
    var preview by remember { mutableStateOf<Title?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.title_dex_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(NopoTheme.spacing.sm)) {
                Text(stringResource(R.string.title_dex_hint), style = NopoTheme.typography.bodySmall, color = c.ink2)
                LazyColumn(Modifier.heightIn(max = 420.dp), verticalArrangement = Arrangement.spacedBy(NopoTheme.spacing.xs)) {
                    items(Title.entries.toList()) { t ->
                        val reached = totalYears >= t.requiredYears
                        Row(
                            Modifier.fillMaxWidth().clickable(role = Role.Button) { preview = t }.padding(vertical = NopoTheme.spacing.xs),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(NopoTheme.spacing.md),
                        ) {
                            Box(Modifier.width(120.dp).graphicsLayer { alpha = if (reached) 1f else 0.45f }) { TitleBadge(t) }
                            Column(Modifier.weight(1f)) {
                                Text(stringResource(R.string.title_dex_years, t.requiredYears), style = NopoTheme.typography.bodyMedium, color = c.ink)
                                Text(
                                    when {
                                        t == mine -> stringResource(R.string.title_dex_current)
                                        reached -> stringResource(R.string.title_dex_done)
                                        else -> stringResource(R.string.title_dex_locked, t.requiredYears - totalYears)
                                    },
                                    style = NopoTheme.typography.bodySmall,
                                    color = if (t == mine) c.stamp else c.ink2,
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.detail_back)) } },
    )
    preview?.let { TitleUpDialog(it, it.requiredYears, onDismiss = { preview = null }, preview = true) }
}
