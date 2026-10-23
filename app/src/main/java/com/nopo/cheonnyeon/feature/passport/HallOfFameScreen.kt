package com.nopo.cheonnyeon.feature.passport

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.nopo.cheonnyeon.R
import com.nopo.cheonnyeon.app.AppContainer
import com.nopo.cheonnyeon.core.designsystem.component.StarRating
import com.nopo.cheonnyeon.core.designsystem.component.TitleBadge
import com.nopo.cheonnyeon.core.designsystem.theme.NopoTheme
import com.nopo.cheonnyeon.data.remote.DemoUser
import com.nopo.cheonnyeon.domain.model.Store
import com.nopo.cheonnyeon.domain.rule.HallOfFameRule
import com.nopo.cheonnyeon.domain.rule.TitlePolicy
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HallStore(val store: Store, val average: Double, val count: Int)

data class HallUiState(
    val loading: Boolean = true,
    /** 시연용 예시 데이터를 켜 둔 경우에만 순위가 있다 */
    val demo: Boolean = false,
    val nickname: String = "",
    val myYears: Int = 0,
    val myRank: Int = 1,
    val myStores: Int = 0,
    val topPercent: Int = 100,
    val byYears: List<DemoUser> = emptyList(),
    val byReviews: List<DemoUser> = emptyList(),
    val topStores: List<HallStore> = emptyList(),
)

/** 식객 명예의 전당 (시안 hall_of_fame). 서버 집계가 없어서 시연용 예시 식객과 견준 순위를 보여 준다. */
class HallOfFameViewModel(private val container: AppContainer) : ViewModel() {
    private val _state = MutableStateFlow(HallUiState())
    val state: StateFlow<HallUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val visits = container.database.myVisitDao().observeAll().first().filter { !it.storeId.startsWith(Store.TEST_PREFIX) }
            val years = visits.sumOf { it.yearsAwarded }
            val nick = container.auth.currentNickname().orEmpty()
            val demo = container.settings.demoData.first()
            val data = if (demo) container.demoReviews.load() else null
            val users = data?.users.orEmpty()
            val rank = HallOfFameRule.rank(years, users.map { it.years })
            val stores = container.catalog.all().associateBy { it.id }
            _state.update {
                it.copy(
                    loading = false, demo = demo && users.isNotEmpty(), nickname = nick, myYears = years, myStores = visits.size,
                    myRank = rank, topPercent = HallOfFameRule.topPercent(rank, users.size),
                    byYears = users.sortedByDescending { u -> u.years },
                    byReviews = users.sortedWith(compareByDescending<DemoUser> { u -> u.reviews }.thenByDescending { u -> u.years }),
                    topStores = data?.topStores(10).orEmpty().mapNotNull { t -> stores[t.storeId]?.let { s -> HallStore(s, t.average, t.count) } },
                )
            }
        }
    }
}

@Composable
fun HallOfFameScreen(vm: HallOfFameViewModel, onBack: () -> Unit, onOpenStore: (String) -> Unit) {
    val state by vm.state.collectAsStateWithLifecycle()
    val c = NopoTheme.colors
    var tab by rememberSaveable { mutableIntStateOf(0) }

    Column(Modifier.fillMaxSize().background(c.paper).statusBarsPadding().navigationBarsPadding().padding(horizontal = NopoTheme.spacing.lg), verticalArrangement = Arrangement.spacedBy(NopoTheme.spacing.md)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(NopoTheme.spacing.sm)) {
            TextButton(onBack) { Text(stringResource(R.string.detail_back)) }
            Text(stringResource(R.string.hall_title), style = NopoTheme.typography.headlineMedium, color = c.primary)
        }
        if (!state.demo) {
            Text(stringResource(if (state.loading) R.string.list_loading else R.string.hall_empty), color = c.ink2)
            return@Column
        }
        val list = (if (tab == 0) state.byYears else state.byReviews).take(20)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(NopoTheme.spacing.sm)) {
            item { Text(stringResource(R.string.hall_demo_note), style = NopoTheme.typography.bodySmall, color = c.brass) }
            item { MyCard(state) }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(NopoTheme.spacing.sm)) {
                    listOf(R.string.hall_tab_years, R.string.hall_tab_reviews, R.string.hall_tab_stores).forEachIndexed { i, res ->
                        FilterChip(tab == i, { tab = i }, label = { Text(stringResource(res)) })
                    }
                }
            }
            if (tab == 2) {
                items(state.topStores.size) { i -> StoreRow(i + 1, state.topStores[i], onOpenStore) }
            } else {
                item { Podium(list.take(3), byReviews = tab == 1) }
                items(maxOf(0, list.size - 3)) { i -> UserRow(i + 4, list[i + 3], tab == 1) }
            }
            item { Text(stringResource(R.string.hall_titles_note), style = NopoTheme.typography.bodySmall, color = c.ink2, modifier = Modifier.padding(vertical = NopoTheme.spacing.md)) }
        }
    }
}

@Composable
private fun MyCard(s: HallUiState) {
    val c = NopoTheme.colors
    Column(Modifier.fillMaxWidth().background(c.surface, NopoTheme.shapes.md).padding(NopoTheme.spacing.md), verticalArrangement = Arrangement.spacedBy(NopoTheme.spacing.xs)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(NopoTheme.spacing.sm)) {
            Text(s.nickname.ifBlank { "-" }, style = NopoTheme.typography.titleLarge, color = c.ink)
            TitleBadge(TitlePolicy.titleFor(s.myYears))
        }
        Text(stringResource(R.string.hall_my_line, s.myYears, s.myRank, s.topPercent), color = c.ink)
        Text(stringResource(R.string.hall_my_progress, HallOfFameRule.progressToMaster(s.myYears)), style = NopoTheme.typography.bodySmall, color = c.ink2)
        LinearProgressIndicator(progress = { HallOfFameRule.progressToMaster(s.myYears) / 100f }, modifier = Modifier.fillMaxWidth(), color = c.brass, trackColor = c.tealSoft)
    }
}

@Composable
private fun Podium(top: List<DemoUser>, byReviews: Boolean) {
    val c = NopoTheme.colors
    if (top.size < 3) return
    // 2위 1위 3위 순서, 1위가 가장 높다
    val order = listOf(1 to 80.dp, 0 to 112.dp, 2 to 64.dp)
    Column(Modifier.fillMaxWidth().background(c.surface, NopoTheme.shapes.md).padding(NopoTheme.spacing.md), verticalArrangement = Arrangement.spacedBy(NopoTheme.spacing.sm)) {
        Text(stringResource(R.string.hall_top3), style = NopoTheme.typography.titleMedium, color = c.primary)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(NopoTheme.spacing.sm), verticalAlignment = Alignment.Bottom) {
            order.forEach { (idx, h) ->
                val u = top[idx]
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(NopoTheme.spacing.xxs)) {
                    TitleBadge(TitlePolicy.titleFor(u.years))
                    Text(u.nickname, style = NopoTheme.typography.titleSmall, color = c.ink, maxLines = 1)
                    Text(
                        if (byReviews) stringResource(R.string.hall_reviews_count, u.reviews) else stringResource(R.string.hall_years, u.years),
                        style = NopoTheme.typography.titleMedium, color = c.stamp,
                    )
                    Text(stringResource(R.string.hall_stores_count, u.stores), style = NopoTheme.typography.bodySmall, color = c.ink2)
                    Column(
                        Modifier.fillMaxWidth().height(h).background(if (idx == 0) c.brassSoft else c.tealSoft, NopoTheme.shapes.sm),
                        verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(stringResource(R.string.hall_rank, idx + 1), style = NopoTheme.typography.titleMedium, color = c.ink)
                    }
                }
            }
        }
    }
}

@Composable
private fun UserRow(rank: Int, u: DemoUser, byReviews: Boolean) {
    val c = NopoTheme.colors
    Row(
        Modifier.fillMaxWidth().background(c.surface, NopoTheme.shapes.md).padding(NopoTheme.spacing.md),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(NopoTheme.spacing.md),
    ) {
        Text(rank.toString(), style = NopoTheme.typography.titleLarge, color = c.ink2)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(NopoTheme.spacing.xxs)) {
            Row(horizontalArrangement = Arrangement.spacedBy(NopoTheme.spacing.sm), verticalAlignment = Alignment.CenterVertically) {
                Text(u.nickname, style = NopoTheme.typography.titleSmall, color = c.ink)
                TitleBadge(TitlePolicy.titleFor(u.years))
            }
            Text(stringResource(R.string.hall_stores_count, u.stores), style = NopoTheme.typography.bodySmall, color = c.ink2)
        }
        Text(
            if (byReviews) stringResource(R.string.hall_reviews_count, u.reviews) else stringResource(R.string.hall_years, u.years),
            style = NopoTheme.typography.titleMedium, color = c.stamp,
        )
    }
}

@Composable
private fun StoreRow(rank: Int, s: HallStore, onOpenStore: (String) -> Unit) {
    val c = NopoTheme.colors
    Row(
        Modifier.fillMaxWidth().background(c.surface, NopoTheme.shapes.md).clickable(role = androidx.compose.ui.semantics.Role.Button) { onOpenStore(s.store.id) }.padding(NopoTheme.spacing.md),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(NopoTheme.spacing.md),
    ) {
        Text(rank.toString(), style = NopoTheme.typography.titleLarge, color = c.ink2)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(NopoTheme.spacing.xxs)) {
            Text(s.store.name.replace("주식회사 ", ""), style = NopoTheme.typography.titleSmall, color = c.ink)
            Text((s.store.sido + " " + s.store.sigungu).trim(), style = NopoTheme.typography.bodySmall, color = c.ink2)
        }
        Column(horizontalAlignment = Alignment.End) {
            StarRating(s.average.toFloat())
            Text(stringResource(R.string.reviews_summary, s.average, s.count), style = NopoTheme.typography.bodySmall, color = c.ink2)
        }
    }
}

private val previewUsers = listOf(DemoUser("노포탐험가", 1474, 40, 22), DemoUser("국밥순례자", 1200, 33, 18), DemoUser("골목식객", 980, 28, 9))

@com.nopo.cheonnyeon.core.designsystem.component.NopoPreviews
@Composable
private fun HallOfFamePreview() {
    com.nopo.cheonnyeon.core.designsystem.component.PreviewSurface {
        MyCard(HallUiState(loading = false, demo = true, nickname = "nptNew1", myYears = 668, myRank = 19, topPercent = 16))
        Podium(previewUsers, byReviews = false)
        UserRow(4, previewUsers[2], byReviews = true)
    }
}
