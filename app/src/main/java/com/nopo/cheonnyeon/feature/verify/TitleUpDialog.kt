package com.nopo.cheonnyeon.feature.verify

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.window.Dialog
import com.nopo.cheonnyeon.R
import com.nopo.cheonnyeon.core.designsystem.component.NopoButton
import com.nopo.cheonnyeon.core.designsystem.component.TitleBadge
import com.nopo.cheonnyeon.core.designsystem.component.YearsGauge
import com.nopo.cheonnyeon.core.designsystem.theme.NopoTheme
import com.nopo.cheonnyeon.domain.model.Title
import com.nopo.cheonnyeon.domain.rule.TitlePolicy

/** 칭호 승급 연출 (FR-STP-06, S-705): 배지가 커졌다 자리 잡는다. 애니메이션을 끈 기기에서는 바로 보여 준다. */
@Composable
fun TitleUpDialog(title: Title, totalYears: Int, onDismiss: () -> Unit) {
    val c = NopoTheme.colors
    val context = LocalContext.current
    val animate = remember {
        android.provider.Settings.Global.getFloat(context.contentResolver, android.provider.Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f
    }
    val scale = remember { Animatable(if (animate) 0.3f else 1f) }
    LaunchedEffect(Unit) {
        if (animate) scale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow))
    }
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth().background(c.surface, NopoTheme.shapes.card).padding(NopoTheme.spacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(NopoTheme.spacing.md),
        ) {
            Text(stringResource(R.string.title_up_headline), style = NopoTheme.typography.titleMedium, color = c.stamp, textAlign = TextAlign.Center)
            TitleBadge(title, Modifier.graphicsLayer { scaleX = scale.value * 1.6f; scaleY = scale.value * 1.6f }.padding(NopoTheme.spacing.md))
            Text(stringResource(titleName(title)), style = NopoTheme.typography.headlineMedium, color = c.ink)
            YearsGauge(totalYears = totalYears, yearsToNextTitle = TitlePolicy.yearsToNext(totalYears))
            NopoButton(stringResource(R.string.title_up_confirm), onDismiss, modifier = Modifier.fillMaxWidth())
        }
    }
}

private fun titleName(t: Title): Int = when (t) {
    Title.SPROUT -> R.string.title_sprout
    Title.REGULAR -> R.string.title_regular
    Title.ELDER -> R.string.title_elder
    Title.ARTISAN -> R.string.title_artisan
    Title.MASTER -> R.string.title_master
}
