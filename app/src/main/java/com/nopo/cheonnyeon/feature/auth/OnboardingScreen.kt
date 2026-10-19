package com.nopo.cheonnyeon.feature.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.nopo.cheonnyeon.R
import com.nopo.cheonnyeon.core.designsystem.component.NopoButton
import com.nopo.cheonnyeon.core.designsystem.component.NopoButtonStyle
import com.nopo.cheonnyeon.core.designsystem.theme.NopoTheme
import kotlinx.coroutines.launch

private val pages = listOf(
    R.string.onboarding_1_title to R.string.onboarding_1_body,
    R.string.onboarding_2_title to R.string.onboarding_2_body,
    R.string.onboarding_3_title to R.string.onboarding_3_body,
)

/** 처음 한 번만 보는 3장짜리 소개 (S-402). 건너뛰기 가능, 끝나면 다시 나오지 않는다. */
@Composable
fun OnboardingScreen(onDone: () -> Unit) {
    val c = NopoTheme.colors
    val pager = rememberPagerState { pages.size }
    val scope = rememberCoroutineScope()
    Column(Modifier.fillMaxSize().background(c.paper).statusBarsPadding().navigationBarsPadding().padding(NopoTheme.spacing.lg)) {
        NopoButton(stringResource(R.string.onboarding_skip), onDone, style = NopoButtonStyle.Outline, modifier = Modifier.align(Alignment.End))
        HorizontalPager(pager, Modifier.weight(1f).fillMaxWidth()) { page ->
            Column(
                Modifier.fillMaxSize().padding(NopoTheme.spacing.lg),
                verticalArrangement = Arrangement.spacedBy(NopoTheme.spacing.md, Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(stringResource(pages[page].first), style = NopoTheme.typography.headlineMedium, color = c.primary, textAlign = TextAlign.Center)
                Text(stringResource(pages[page].second), style = NopoTheme.typography.bodyLarge, color = c.ink, textAlign = TextAlign.Center)
            }
        }
        Row(Modifier.align(Alignment.CenterHorizontally).padding(NopoTheme.spacing.md), horizontalArrangement = Arrangement.spacedBy(NopoTheme.spacing.sm)) {
            repeat(pages.size) { i ->
                Column(Modifier.size(10.dp).background(if (i == pager.currentPage) c.primary else c.line, CircleShape)) {}
            }
        }
        val last = pager.currentPage == pages.lastIndex
        NopoButton(
            stringResource(if (last) R.string.onboarding_start else R.string.onboarding_next),
            { if (last) onDone() else scope.launch { pager.animateScrollToPage(pager.currentPage + 1) } },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
