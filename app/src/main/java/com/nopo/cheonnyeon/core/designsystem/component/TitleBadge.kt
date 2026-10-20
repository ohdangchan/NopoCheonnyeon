package com.nopo.cheonnyeon.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nopo.cheonnyeon.R
import com.nopo.cheonnyeon.core.designsystem.theme.NopoTheme
import com.nopo.cheonnyeon.domain.model.Title

fun Title.labelRes(): Int = when (this) {
    Title.SPROUT -> R.string.title_sprout
    Title.REGULAR -> R.string.title_regular
    Title.WANDERER -> R.string.title_wanderer
    Title.ELDER -> R.string.title_elder
    Title.CONNOISSEUR -> R.string.title_connoisseur
    Title.PIONEER -> R.string.title_pioneer
    Title.ARTISAN -> R.string.title_artisan
    Title.KEEPER -> R.string.title_keeper
    Title.SAGE -> R.string.title_sage
    Title.LEGEND -> R.string.title_legend
    Title.MASTER -> R.string.title_master
}

@Composable
fun Title.label(): String = stringResource(labelRes())

private data class BadgeStyle(
    val container: Color,
    val content: Color,
    val border: BorderStroke?,
    val minHeight: Dp,
    val shadow: Dp = 0.dp,
)

/** DESIGN.md 3.2. 칭호가 높을수록 배지가 커진다(22 → 26 → 30dp). */
@Composable
fun TitleBadge(title: Title, modifier: Modifier = Modifier) {
    val c = NopoTheme.colors
    val style = when (title) {
        Title.SPROUT -> BadgeStyle(c.tealSoft, if (c.isDark) c.ink else c.sign, null, 22.dp)
        Title.REGULAR, Title.WANDERER -> BadgeStyle(c.primary, c.onPrimary, null, 22.dp)
        Title.ELDER, Title.CONNOISSEUR, Title.PIONEER -> BadgeStyle(c.brassSoft, if (c.isDark) c.brass else c.onBrass, BorderStroke(1.2.dp, c.brass), 26.dp)
        Title.ARTISAN, Title.KEEPER -> BadgeStyle(c.stamp, c.onStamp, null, 26.dp, shadow = 2.dp)
        Title.SAGE, Title.LEGEND -> BadgeStyle(c.stamp, c.onStamp, BorderStroke(1.5.dp, c.brass), 28.dp, shadow = 3.dp)
        Title.MASTER -> BadgeStyle(c.brass, c.onBrass, BorderStroke(2.dp, c.stamp), 30.dp)
    }
    val label = title.label()
    val description = stringResource(R.string.cd_title_badge, label)
    Surface(
        modifier = modifier.clearAndSetSemantics { contentDescription = description },
        shape = NopoTheme.shapes.pill,
        color = style.container,
        contentColor = style.content,
        border = style.border,
        shadowElevation = style.shadow,
    ) {
        Box(
            modifier = Modifier
                .heightIn(min = style.minHeight)
                .padding(horizontal = if (title >= Title.ELDER) 12.dp else 10.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = label,
                style = NopoTheme.typography.labelMedium.let {
                    if (title == Title.MASTER) it.copy(fontWeight = FontWeight.ExtraBold) else it
                },
            )
        }
    }
}

@NopoPreviews
@Composable
private fun TitleBadgePreview() = PreviewSurface {
    Title.entries.forEach { TitleBadge(it) }
}
