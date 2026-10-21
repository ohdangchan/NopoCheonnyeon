package com.nopo.cheonnyeon.feature.store

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import com.nopo.cheonnyeon.core.common.message
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nopo.cheonnyeon.R
import com.nopo.cheonnyeon.core.designsystem.component.EmptyState
import com.nopo.cheonnyeon.core.designsystem.component.NopoButton
import com.nopo.cheonnyeon.core.designsystem.component.NopoButtonStyle
import com.nopo.cheonnyeon.core.designsystem.theme.NopoTheme
import com.nopo.cheonnyeon.domain.model.Store
import com.nopo.cheonnyeon.domain.rule.TitlePolicy

/** 노포 상세 (S-505, FR-STORE-01·02·05): 큰 업력, 보상 미리보기, 영업·메뉴, 길찾기. 비어 있는 필드는 줄을 숨긴다. */
@Composable
fun StoreDetailScreen(vm: StoreDetailViewModel, onBack: () -> Unit, onVerify: () -> Unit, translateBar: @Composable (Store) -> Unit = {}, reviews: @Composable (Store) -> Unit) {
    val state by vm.state.collectAsStateWithLifecycle()
    val c = NopoTheme.colors
    val store = state.store
    val translation by vm.translation.collectAsStateWithLifecycle()
    if (store == null) {
        EmptyState(stringResource(if (state.loading) R.string.list_loading else R.string.detail_not_found), actionLabel = stringResource(R.string.detail_back), onAction = onBack)
        return
    }
    val context = LocalContext.current
    Column(Modifier.fillMaxSize().background(c.paper)) {
        val scroll = rememberScrollState()
        val requested by vm.reviewRequested.collectAsStateWithLifecycle()
        LaunchedEffect(requested) {
            if (requested) { kotlinx.coroutines.delay(300); scroll.animateScrollTo(scroll.maxValue) }
        }
        Column(Modifier.weight(1f).verticalScroll(scroll)) {
            Photos(store)
            Column(Modifier.padding(NopoTheme.spacing.lg), verticalArrangement = Arrangement.spacedBy(NopoTheme.spacing.md)) {
                Row(horizontalArrangement = Arrangement.spacedBy(NopoTheme.spacing.sm), verticalAlignment = androidx.compose.ui.Alignment.Bottom) {
                    Text(stringResource(R.string.years_value, state.years), style = NopoTheme.typography.displayMedium, color = c.stamp)
                    if (store.foundedYear == null) Text(stringResource(R.string.detail_years_checking), style = NopoTheme.typography.bodySmall, color = c.ink2)
                }
                Text(store.name, style = NopoTheme.typography.headlineMedium, color = c.ink)
                ProgramBadges(store)
                translateBar(store)
                store.summary?.let { Text(translation?.pick(it) ?: it, style = NopoTheme.typography.bodyMedium, color = c.ink2) }
                Text(
                    listOfNotNull(
                        store.foundedYear?.let { stringResource(R.string.detail_founded, it) },
                        store.address.ifBlank { null },
                    ).joinToString(" · "),
                    style = NopoTheme.typography.bodySmall,
                    color = c.ink2,
                )
                RewardBox(state, store.isTest)
                InfoSection(store)
                store.story?.takeIf { it.isNotBlank() }?.let { StorySection(translation?.pick(it.trim()) ?: it, showSource = !store.isTest) }
                if (!store.isTest) InfoReport(vm)
                reviews(store)
                store.sourceUrl?.let { Text(stringResource(R.string.detail_source, it), style = NopoTheme.typography.bodySmall, color = c.ink2) }
            }
        }
        Row(
            Modifier.fillMaxWidth().background(c.surface).navigationBarsPadding().padding(NopoTheme.spacing.md),
            horizontalArrangement = Arrangement.spacedBy(NopoTheme.spacing.sm),
        ) {
            NopoButton(stringResource(R.string.detail_back), onBack, style = NopoButtonStyle.Outline, modifier = Modifier.weight(1f))
            if (store.lat != null && store.lng != null) {
                NopoButton(stringResource(R.string.detail_directions), { openDirections(context, store) }, style = NopoButtonStyle.Outline, modifier = Modifier.weight(1f))
            }
            NopoButton(stringResource(R.string.detail_verify), onVerify, modifier = Modifier.weight(1.4f))
        }
    }
}

/** 가게 이야기 (FR-STORE-01): 소상공인마당 '소개글' 원문. 길면 접어서 보여 주고 눌러서 펼친다 */
@Composable
private fun StorySection(story: String, showSource: Boolean) {
    val c = NopoTheme.colors
    var expanded by androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableStateOf(false) }
    val long = story.length > STORY_PREVIEW_CHARS || story.count { it == '\n' } >= 3
    Column(
        Modifier.fillMaxWidth().background(c.surface, NopoTheme.shapes.md).padding(NopoTheme.spacing.md),
        verticalArrangement = Arrangement.spacedBy(NopoTheme.spacing.sm),
    ) {
        Text(stringResource(R.string.store_story_title), style = NopoTheme.typography.titleMedium, color = c.primary)
        Text(story, style = NopoTheme.typography.bodyMedium, color = c.ink, maxLines = if (expanded || !long) Int.MAX_VALUE else STORY_PREVIEW_LINES)
        if (long) {
            androidx.compose.material3.TextButton(onClick = { expanded = !expanded }) {
                Text(stringResource(if (expanded) R.string.store_story_less else R.string.store_story_more))
            }
        }
        if (showSource) Text(stringResource(R.string.store_story_source), style = NopoTheme.typography.bodySmall, color = c.ink2)
    }
}

private const val GOOGLE_PHOTO_PREFIX = "file:///android_asset/places/"
private const val STORY_PREVIEW_CHARS = 180
private const val STORY_PREVIEW_LINES = 5

/** 정보 오류 제보 (FR-STORE-06): 사유를 고르면 운영자에게 접수된다 */
@Composable
private fun InfoReport(vm: StoreDetailViewModel) {
    val c = NopoTheme.colors
    val context = LocalContext.current
    val sentText = stringResource(R.string.store_report_sent)
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    var open by androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableStateOf(false) }
    var message by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<String?>(null) }
    androidx.compose.material3.TextButton(onClick = { message = null; open = true }) { Text(stringResource(R.string.store_report_link), color = c.ink2) }
    message?.let { Text(it, style = NopoTheme.typography.bodySmall, color = c.primary) }
    if (open) {
        val reasons = listOf(R.string.store_report_closed, R.string.store_report_hours, R.string.store_report_phone, R.string.store_report_menu, R.string.store_report_other)
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { open = false },
            title = { Text(stringResource(R.string.store_report_title)) },
            text = {
                Column {
                    reasons.forEach { res ->
                        val label = stringResource(res)
                        androidx.compose.material3.TextButton(onClick = {
                            open = false
                            scope.launch {
                                val r = vm.reportInfo(label)
                                message = if (r is com.nopo.cheonnyeon.core.common.AppResult.Success) sentText
                                else r.errorOrNull?.message(context)
                            }
                        }) { Text(label) }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { androidx.compose.material3.TextButton(onClick = { open = false }) { Text(stringResource(R.string.detail_back)) } },
        )
    }
}

@Composable
private fun Photos(store: Store) {
    val count = store.photoUrls.size.coerceAtLeast(1)
    val pager = rememberPagerState { count }
    Column(Modifier.statusBarsPadding()) {
        HorizontalPager(pager, Modifier.fillMaxWidth().aspectRatio(4f / 3f)) { page ->
            androidx.compose.foundation.layout.Box(Modifier.fillMaxSize()) {
                StorePhoto(store, Modifier.fillMaxSize(), index = page)
                // Google 지도 사진은 출처를 표시한다
                if (store.photoUrls.getOrNull(page)?.startsWith(GOOGLE_PHOTO_PREFIX) == true) {
                    Text(
                        stringResource(R.string.photo_credit_google),
                        style = NopoTheme.typography.labelMedium,
                        color = NopoTheme.colors.onSign,
                        modifier = Modifier.align(androidx.compose.ui.Alignment.BottomEnd).padding(NopoTheme.spacing.sm)
                            .background(NopoTheme.colors.sign.copy(alpha = 0.7f), NopoTheme.shapes.sm)
                            .padding(horizontal = NopoTheme.spacing.sm, vertical = NopoTheme.spacing.xxs),
                    )
                }
            }
        }
        if (count > 1) {
            Text(
                "${pager.currentPage + 1} / $count",
                style = NopoTheme.extraTypography.metricSmall,
                color = NopoTheme.colors.ink2,
                modifier = Modifier.padding(start = NopoTheme.spacing.lg, top = NopoTheme.spacing.xs),
            )
        }
    }
}

/** 첫 방문이면 얻을 세월과 다음 칭호까지, 방문했으면 방문 완료 · 단골 횟수 (FR-STORE-02) */
@Composable
private fun RewardBox(state: StoreDetailUiState, isTest: Boolean) {
    val c = NopoTheme.colors
    val fg = if (c.isDark) c.brass else c.onBrass
    Column(
        Modifier.fillMaxWidth().background(c.brassSoft, NopoTheme.shapes.md).padding(NopoTheme.spacing.lg),
        verticalArrangement = Arrangement.spacedBy(NopoTheme.spacing.xs),
    ) {
        val visit = state.visit
        if (visit != null) {
            Text(stringResource(R.string.detail_visited, visit.visitCount), style = NopoTheme.typography.titleMedium, color = fg)
        } else if (isTest) {
            Text(stringResource(R.string.detail_reward_test), style = NopoTheme.typography.titleMedium, color = fg)
        } else {
            Text(stringResource(R.string.detail_reward_title, state.years), style = NopoTheme.typography.titleMedium, color = fg)
            val after = state.myTotalYears + state.years
            Text(stringResource(R.string.detail_reward_total, state.myTotalYears, after), style = NopoTheme.extraTypography.metric, color = fg)
            val next = TitlePolicy.yearsToNext(after)
            Text(
                if (next == null) stringResource(R.string.years_gauge_max) else stringResource(R.string.years_gauge_next, next),
                style = NopoTheme.typography.bodySmall,
                color = fg,
            )
        }
    }
}

@Composable
private fun InfoSection(store: Store) {
    val c = NopoTheme.colors
    val context = LocalContext.current
    Column(verticalArrangement = Arrangement.spacedBy(NopoTheme.spacing.sm)) {
        store.hours?.let { InfoLine(R.string.detail_hours, it) }
        store.phone?.takeIf { it.isNotBlank() }?.let { phone ->
            InfoLine(R.string.detail_phone, phone)
            NopoButton(stringResource(R.string.detail_call), { dial(context, phone) }, style = NopoButtonStyle.Outline)
        }
        if (store.amenities.isNotEmpty()) InfoLine(R.string.detail_amenities, store.amenities.joinToString(", "))
        if (store.menuItems.isNotEmpty()) InfoLine(R.string.detail_menu, store.menuItems.joinToString(", "))
        store.categoryGroup.takeIf { it.isNotBlank() }?.let { InfoLine(R.string.detail_category, it) }
    }
}

@Composable
private fun InfoLine(label: Int, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(NopoTheme.spacing.xxs)) {
        Text(stringResource(label), style = NopoTheme.typography.labelMedium, color = NopoTheme.colors.primary)
        Text(value, style = NopoTheme.typography.bodyMedium, color = NopoTheme.colors.ink)
    }
}

/** 네이버 지도 앱으로 길찾기, 앱이 없으면 웹 지도 (FR-STORE-05) */
private fun openDirections(context: Context, store: Store) {
    val name = Uri.encode(store.name)
    val app = Uri.parse("nmap://route/walk?dlat=${store.lat}&dlng=${store.lng}&dname=$name&appname=${context.packageName}")
    val web = Uri.parse("https://map.naver.com/p/search/$name")
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, app).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (_: ActivityNotFoundException) {
        context.startActivity(Intent(Intent.ACTION_VIEW, web).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}

private fun dial(context: Context, phone: String) {
    try {
        context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (_: ActivityNotFoundException) {
    }
}

@com.nopo.cheonnyeon.core.designsystem.component.NopoPreviews
@Composable
private fun StoreDetailPartsPreview() {
    val store = com.nopo.cheonnyeon.domain.model.Store(
        id = "P-1", name = "노포천년식당", programs = listOf("백년가게"), sido = "서울", sigungu = "송파구", address = "서울 송파구 동남로 309",
        foundedYear = 1980, hours = "매일 11:00 - 22:00", menuItems = listOf("설렁탕", "수육"), amenities = listOf("주차장", "예약"),
    )
    com.nopo.cheonnyeon.core.designsystem.component.PreviewSurface {
        RewardBox(StoreDetailUiState(loading = false, store = store, years = 46, myTotalYears = 668), isTest = false)
        InfoSection(store)
    }
}
