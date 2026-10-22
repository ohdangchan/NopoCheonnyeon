package com.nopo.cheonnyeon.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.nopo.cheonnyeon.R
import com.nopo.cheonnyeon.app.AppContainer
import com.nopo.cheonnyeon.core.common.SeoulZone
import com.nopo.cheonnyeon.core.designsystem.theme.NopoTheme
import com.nopo.cheonnyeon.domain.model.Store
import com.nopo.cheonnyeon.domain.rule.YearsCalculator
import com.nopo.cheonnyeon.feature.explore.StoreCard
import com.nopo.cheonnyeon.feature.store.StorePhoto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

class HomeViewModel(private val container: AppContainer) : ViewModel() {
    private val _stores = MutableStateFlow<List<Store>>(emptyList())
    val stores: StateFlow<List<Store>> = _stores

    init {
        viewModelScope.launch { _stores.value = container.catalog.all().filter { !it.isTest && it.isFood && it.photoUrls.isNotEmpty() } }
    }
}

/**
 * 시안 "홈". 인기·평점 집계가 없어서 "핫한 노포" 대신 업력이 깊은 노포를 보여 주고,
 * "오늘의 추천"은 날짜로 고정해 하루 동안 같은 가게가 나온다.
 */
@Composable
fun HomeScreen(vm: HomeViewModel, onOpenStore: (String) -> Unit, onSearch: () -> Unit, onOpenChat: (String) -> Unit) {
    val stores by vm.stores.collectAsStateWithLifecycle()
    val c = NopoTheme.colors
    val sp = NopoTheme.spacing
    val today = LocalDate.now(SeoulZone)
    val year = today.year
    val pick = remember(stores) { if (stores.isEmpty()) null else stores[(today.toEpochDay() % stores.size).toInt()] }
    val oldest = remember(stores) { stores.sortedByDescending { YearsCalculator.yearsAtVisit(it.foundedYear, year) }.take(10) }
    val tourPrompt = stringResource(R.string.home_tour_prompt)
    Column(Modifier.fillMaxSize().background(c.paper).verticalScroll(rememberScrollState()).padding(bottom = sp.xl)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = sp.lg, vertical = sp.md), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.app_name), style = NopoTheme.typography.displaySmall, color = c.sign)
                Text(stringResource(R.string.home_tagline), style = NopoTheme.typography.bodySmall, color = c.ink2)
            }
            Text(
                stringResource(R.string.home_search),
                style = NopoTheme.typography.labelLarge, color = c.ink,
                modifier = Modifier.clip(RoundedCornerShape(50)).background(c.tealSoft).clickable(role = Role.Button, onClick = onSearch).padding(horizontal = 16.dp, vertical = 10.dp),
            )
        }
        if (pick != null) {
            Hero(pick, onOpen = { onOpenStore(pick.id) })
            Text(stringResource(R.string.home_pick_label), style = NopoTheme.typography.labelLarge, color = c.onAccent,
                modifier = Modifier.padding(start = sp.xl, top = sp.md).clip(RoundedCornerShape(50)).background(c.accent).padding(horizontal = 14.dp, vertical = 6.dp))
            StoreCard(pick, year, Modifier.padding(horizontal = sp.lg, vertical = sp.sm).fillMaxWidth()) { onOpenStore(pick.id) }
        }
        Text(stringResource(R.string.home_oldest_title), style = NopoTheme.typography.titleLarge, color = c.sign, modifier = Modifier.padding(horizontal = sp.lg, vertical = sp.sm))
        LazyRow(contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = sp.lg), horizontalArrangement = Arrangement.spacedBy(sp.md)) {
            items(oldest, key = { it.id }) { s -> StoreCard(s, year, Modifier.width(180.dp)) { onOpenStore(s.id) } }
        }
        Row(
            Modifier.padding(horizontal = sp.lg, vertical = sp.lg).fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(c.tealSoft)
                .clickable(role = Role.Button) { onOpenChat(tourPrompt) }.padding(sp.lg),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(sp.xxs)) {
                Text(stringResource(R.string.home_tour_title), style = NopoTheme.typography.titleMedium, color = c.sign)
                Text(stringResource(R.string.home_tour_body), style = NopoTheme.typography.bodySmall, color = c.ink2)
            }
            Text("›", style = NopoTheme.typography.displaySmall, color = c.sign)
        }
    }
}

@Composable
private fun Hero(store: Store, onOpen: () -> Unit) {
    val sp = NopoTheme.spacing
    Box(Modifier.padding(horizontal = NopoTheme.spacing.lg).fillMaxWidth().height(220.dp).clip(RoundedCornerShape(20.dp)).clickable(role = Role.Button, onClick = onOpen)) {
        StorePhoto(store, Modifier.fillMaxSize())
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(NopoTheme.colors.sign.copy(alpha = 0f), NopoTheme.colors.sign.copy(alpha = 0.8f)))))
        Column(Modifier.align(Alignment.BottomStart).padding(sp.lg), verticalArrangement = Arrangement.spacedBy(sp.xxs)) {
            Text(stringResource(R.string.home_hero_title), style = NopoTheme.typography.titleLarge, color = NopoTheme.colors.onSign)
            Text(stringResource(R.string.home_hero_body), style = NopoTheme.typography.bodySmall, color = NopoTheme.colors.onSign, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}
