package com.nopo.cheonnyeon.feature.store

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import coil3.compose.SubcomposeAsyncImage
import com.nopo.cheonnyeon.R
import com.nopo.cheonnyeon.core.designsystem.theme.NopoTheme
import com.nopo.cheonnyeon.domain.model.Store

/** 공식 지정 배지: 백년가게(금) · 오래가게(청록) */
@Composable
fun ProgramBadges(store: Store, modifier: Modifier = Modifier) {
    val c = NopoTheme.colors
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(NopoTheme.spacing.xs)) {
        if ("BAEKNYEON" in store.programs) Badge(stringResource(R.string.badge_baeknyeon), c.brassSoft, if (c.isDark) c.brass else c.onBrass)
        if ("ORAE" in store.programs) Badge(stringResource(R.string.badge_orae), c.tealSoft, c.sign)
    }
}

@Composable
private fun Badge(text: String, bg: androidx.compose.ui.graphics.Color, fg: androidx.compose.ui.graphics.Color) {
    Text(
        text,
        style = NopoTheme.typography.labelMedium,
        color = fg,
        modifier = Modifier.background(bg, NopoTheme.shapes.sm).padding(horizontal = NopoTheme.spacing.sm, vertical = NopoTheme.spacing.xxs),
    )
}

/** 가게 대표 사진. 사진이 없거나 못 불러오면 종류 글자가 있는 기본 카드로 대신한다 (앱이 사진 없이도 깨지지 않게). */
@Composable
fun StorePhoto(store: Store, modifier: Modifier = Modifier, index: Int = 0) {
    val url = store.photoUrls.getOrNull(index)
    if (url == null) {
        PhotoPlaceholder(store, modifier)
        return
    }
    SubcomposeAsyncImage(
        model = url,
        contentDescription = stringResource(R.string.cd_store_photo, store.name),
        contentScale = ContentScale.Crop,
        modifier = modifier,
        loading = { PhotoPlaceholder(store, Modifier) },
        error = { PhotoPlaceholder(store, Modifier) },
    )
}

@Composable
private fun PhotoPlaceholder(store: Store, modifier: Modifier) {
    val c = NopoTheme.colors
    Box(modifier.background(c.tealSoft), contentAlignment = Alignment.Center) {
        Text(store.name.take(1), style = NopoTheme.typography.headlineMedium, color = c.primary)
    }
}
