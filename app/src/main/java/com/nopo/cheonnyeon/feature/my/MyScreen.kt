package com.nopo.cheonnyeon.feature.my

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.nopo.cheonnyeon.R
import com.nopo.cheonnyeon.app.AppContainer
import com.nopo.cheonnyeon.core.common.SeoulZone
import com.nopo.cheonnyeon.core.designsystem.component.TitleBadge
import com.nopo.cheonnyeon.core.designsystem.theme.NopoTheme
import com.nopo.cheonnyeon.domain.model.Store
import com.nopo.cheonnyeon.domain.rule.TitlePolicy
import com.nopo.cheonnyeon.feature.explore.StoreCard
import com.nopo.cheonnyeon.feature.passport.PassportViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

class MyViewModel(private val container: AppContainer) : ViewModel() {
    private val catalog = kotlinx.coroutines.flow.MutableStateFlow<List<Store>>(emptyList())

    init {
        viewModelScope.launch { catalog.value = container.catalog.all().filter { !it.isTest } }
    }

    /** 저장한 노포 (저장한 순서는 기기에 남기지 않아 이름순) */
    val saved: StateFlow<List<Store>> = kotlinx.coroutines.flow.combine(container.settings.bookmarks, catalog) { ids, all ->
        all.filter { it.id in ids }.sortedBy { it.name }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
}

/**
 * 시안 "마이": 프로필 카드(닉네임, 칭호, 방문한 노포 수), 저장한 노포, 최근 방문, 메뉴 카드.
 * 프로필 사진, 알림, 고객센터는 데이터와 운영 정보가 없어 만들지 않았다.
 */
@Composable
fun MyScreen(
    vm: MyViewModel, passportVm: PassportViewModel,
    onOpenStore: (String) -> Unit, onOpenProfile: (String) -> Unit, onOpenCollection: () -> Unit, onOpenSettings: () -> Unit,
) {
    val c = NopoTheme.colors
    val sp = NopoTheme.spacing
    val state by passportVm.state.collectAsStateWithLifecycle()
    val saved by vm.saved.collectAsStateWithLifecycle()
    val nickname by produceState("") { value = passportVm.nickname() }
    val year = LocalDate.now(SeoulZone).year
    val real = state.stamps.filter { !it.isTest }
    val title = TitlePolicy.titleFor(state.totalYears)
    val recent = real.sortedByDescending { it.visit.lastVisitedAt }.mapNotNull { it.store }.take(8)
    Column(Modifier.fillMaxSize().background(c.paper).verticalScroll(rememberScrollState()).padding(bottom = sp.xl), verticalArrangement = Arrangement.spacedBy(sp.md)) {
        Text(stringResource(R.string.app_name), style = NopoTheme.typography.displaySmall, color = c.sign, modifier = Modifier.padding(horizontal = sp.lg, vertical = sp.md))
        Column(Modifier.padding(horizontal = sp.lg).fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(c.surface).padding(sp.lg), verticalArrangement = Arrangement.spacedBy(sp.sm)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(sp.md)) {
                Text(nickname.ifBlank { stringResource(R.string.my_default_nickname) }, style = NopoTheme.typography.headlineMedium, color = c.sign)
                TitleBadge(title)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(sp.xl)) {
                Column { Text(real.size.toString(), style = NopoTheme.typography.displaySmall, color = c.sign); Text(stringResource(R.string.my_visited), style = NopoTheme.typography.labelMedium, color = c.ink2) }
                Column { Text(stringResource(R.string.hall_years, state.totalYears), style = NopoTheme.typography.displaySmall, color = c.stamp); Text(stringResource(R.string.my_total_years), style = NopoTheme.typography.labelMedium, color = c.ink2) }
            }
        }
        Row(Modifier.padding(horizontal = sp.lg), horizontalArrangement = Arrangement.spacedBy(sp.md)) {
            MenuCard(stringResource(R.string.my_menu_collection), stringResource(R.string.my_menu_collection_body), Modifier.weight(1f), onOpenCollection)
            MenuCard(stringResource(R.string.my_menu_settings), stringResource(R.string.my_menu_settings_body), Modifier.weight(1f), onOpenSettings)
        }
        Text(stringResource(R.string.my_saved_title, saved.size), style = NopoTheme.typography.titleLarge, color = c.sign, modifier = Modifier.padding(horizontal = sp.lg))
        if (saved.isEmpty()) {
            Text(stringResource(R.string.my_saved_empty), style = NopoTheme.typography.bodyMedium, color = c.ink2, modifier = Modifier.padding(horizontal = sp.lg))
        } else {
            LazyRow(contentPadding = PaddingValues(horizontal = sp.lg), horizontalArrangement = Arrangement.spacedBy(sp.md)) {
                items(saved, key = { it.id }) { s -> StoreCard(s, year, Modifier.width(180.dp)) { onOpenStore(s.id) } }
            }
        }
        if (recent.isNotEmpty()) {
            Text(stringResource(R.string.my_recent_title), style = NopoTheme.typography.titleLarge, color = c.sign, modifier = Modifier.padding(horizontal = sp.lg))
            LazyRow(contentPadding = PaddingValues(horizontal = sp.lg), horizontalArrangement = Arrangement.spacedBy(sp.md)) {
                items(recent, key = { it.id }) { s -> StoreCard(s, year, Modifier.width(180.dp)) { onOpenStore(s.id) } }
            }
        }
    }
}

@Composable
private fun MenuCard(title: String, body: String, modifier: Modifier, onClick: () -> Unit) {
    val c = NopoTheme.colors
    Column(
        modifier.clip(RoundedCornerShape(16.dp)).background(c.surface).clickable(role = Role.Button, onClick = onClick).padding(NopoTheme.spacing.md),
        verticalArrangement = Arrangement.spacedBy(NopoTheme.spacing.xxs),
    ) {
        Text("$title ›", style = NopoTheme.typography.titleSmall, color = c.sign)
        Text(body, style = NopoTheme.typography.bodySmall, color = c.ink2)
    }
}
