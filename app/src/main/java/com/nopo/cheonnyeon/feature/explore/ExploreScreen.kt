package com.nopo.cheonnyeon.feature.explore

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
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
import com.nopo.cheonnyeon.domain.catalog.StoreFilter
import com.nopo.cheonnyeon.domain.catalog.StoreFilters
import com.nopo.cheonnyeon.domain.model.Store
import com.nopo.cheonnyeon.domain.rule.YearsCalculator
import com.nopo.cheonnyeon.feature.store.StorePhoto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

/** 탐색 화면 칩: 이름 대신 검색어로 쓰는 음식 종류 (stores.json의 대표 메뉴·이름에서 찾는다) */
val ExploreCategoryQueries = listOf("", "국밥", "면", "고기", "분식", "카페")

class ExploreViewModel(private val container: AppContainer) : ViewModel() {
    private val _stores = MutableStateFlow<List<Store>>(emptyList())
    val stores: StateFlow<List<Store>> = _stores

    init {
        viewModelScope.launch { _stores.value = container.catalog.all().filter { !it.isTest } }
    }
}

/** 시안 "탐색": 검색창 + 종류 칩 + 사진 카드 2열 격자. 사진이 있는 가게를 먼저, 업력 깊은 순으로 보여 준다. */
@Composable
fun ExploreScreen(vm: ExploreViewModel, initialQuery: String = "", onOpenStore: (String) -> Unit) {
    val all by vm.stores.collectAsStateWithLifecycle()
    var query by rememberSaveable(initialQuery) { mutableStateOf(initialQuery) }
    var chip by rememberSaveable { mutableStateOf(0) }
    val year = LocalDate.now(SeoulZone).year
    val shown = remember(all, query, chip) {
        val q = listOf(query.trim(), ExploreCategoryQueries[chip]).filter { it.isNotEmpty() }.joinToString(" ")
        val found = StoreFilters.apply(all, StoreFilter(query = q, foodOnly = true), emptySet())
        found.sortedWith(compareByDescending<Store> { it.photoUrls.isNotEmpty() }.thenByDescending { YearsCalculator.yearsAtVisit(it.foundedYear, year) }).take(200)
    }
    val c = NopoTheme.colors
    val sp = NopoTheme.spacing
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize().background(c.paper).statusBarsPadding(),
        contentPadding = PaddingValues(horizontal = sp.lg, vertical = sp.lg),
        horizontalArrangement = Arrangement.spacedBy(sp.md),
        verticalArrangement = Arrangement.spacedBy(sp.md),
    ) {
        item(span = { GridItemSpan(2) }) {
            Column(verticalArrangement = Arrangement.spacedBy(sp.sm)) {
                Text(stringResource(R.string.explore_title), style = NopoTheme.typography.displaySmall, color = c.sign)
                Text(stringResource(R.string.explore_subtitle), style = NopoTheme.typography.bodyMedium, color = c.ink2)
                Box(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(c.surface).padding(horizontal = sp.lg, vertical = sp.md),
                ) {
                    if (query.isEmpty()) Text(stringResource(R.string.explore_search_hint), style = NopoTheme.typography.bodyMedium, color = c.ink2)
                    BasicTextField(
                        value = query,
                        onValueChange = { query = it },
                        singleLine = true,
                        textStyle = NopoTheme.typography.bodyMedium.copy(color = c.ink),
                        cursorBrush = SolidColor(c.ink),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(sp.sm)) {
                    ExploreCategoryQueries.forEachIndexed { i, q ->
                        CategoryChip(if (q.isEmpty()) stringResource(R.string.explore_chip_all) else q, selected = chip == i) { chip = i }
                    }
                }
            }
        }
        if (shown.isEmpty()) {
            item(span = { GridItemSpan(2) }) {
                Text(stringResource(R.string.explore_empty), style = NopoTheme.typography.bodyMedium, color = c.ink2, modifier = Modifier.padding(vertical = sp.xl))
            }
        }
        items(shown, key = { it.id }) { store -> StoreCard(store, year) { onOpenStore(store.id) } }
    }
}

@Composable
private fun CategoryChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val c = NopoTheme.colors
    Text(
        label,
        style = NopoTheme.typography.labelLarge,
        color = if (selected) c.onAccent else c.ink,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) c.accent else c.tealSoft)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 10.dp),
    )
}

/** 사진 카드: 위에 "N년 전통" 칩, 아래에 상호·지역·한 줄 소개 */
@Composable
fun StoreCard(store: Store, currentYear: Int, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val c = NopoTheme.colors
    val sp = NopoTheme.spacing
    val years = YearsCalculator.yearsAtVisit(store.foundedYear, currentYear)
    Column(
        modifier.clip(RoundedCornerShape(16.dp)).background(c.surface).clickable(role = Role.Button, onClick = onClick),
    ) {
        Box(Modifier.fillMaxWidth().aspectRatio(1.25f)) {
            StorePhoto(store, Modifier.fillMaxSize())
            Text(
                stringResource(R.string.explore_years_tag, years),
                style = NopoTheme.typography.labelMedium,
                color = c.onAccent,
                modifier = Modifier.padding(sp.sm).clip(RoundedCornerShape(50)).background(c.accent).padding(horizontal = 10.dp, vertical = 4.dp),
            )
        }
        Column(Modifier.padding(sp.md), verticalArrangement = Arrangement.spacedBy(sp.xxs)) {
            Text(store.name, style = NopoTheme.typography.titleSmall, color = c.sign, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                listOf(store.sido, store.sigungu).filter { it.isNotBlank() }.joinToString(" "),
                style = NopoTheme.typography.bodySmall, color = c.ink2, maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            val line = store.summary ?: store.menuItems.take(3).joinToString(", ")
            if (line.isNotBlank()) Text(line, style = NopoTheme.typography.bodySmall, color = c.ink2, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}
