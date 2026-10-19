package com.nopo.cheonnyeon.feature.passport

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.nopo.cheonnyeon.R
import com.nopo.cheonnyeon.app.AppContainer
import com.nopo.cheonnyeon.core.common.SeoulZone
import com.nopo.cheonnyeon.core.designsystem.component.EmptyState
import com.nopo.cheonnyeon.core.designsystem.component.StampSize
import com.nopo.cheonnyeon.core.designsystem.component.StampView
import com.nopo.cheonnyeon.core.designsystem.component.TitleBadge
import com.nopo.cheonnyeon.core.designsystem.component.YearsGauge
import com.nopo.cheonnyeon.core.designsystem.theme.NopoTheme
import com.nopo.cheonnyeon.data.local.MyVisitEntity
import com.nopo.cheonnyeon.domain.model.Store
import com.nopo.cheonnyeon.domain.rule.StampGradePolicy
import com.nopo.cheonnyeon.domain.rule.TitlePolicy
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.stateIn
import java.time.Instant

data class PassportStamp(val visit: MyVisitEntity, val store: Store?) {
    val isTest: Boolean get() = visit.storeId.startsWith(Store.TEST_PREFIX)
}

data class PassportUiState(val loading: Boolean = true, val stamps: List<PassportStamp> = emptyList(), val totalYears: Int = 0)

class PassportViewModel(private val container: AppContainer) : ViewModel() {
    private val _reviews = kotlinx.coroutines.flow.MutableStateFlow<List<com.nopo.cheonnyeon.data.remote.ReviewItem>?>(null)

    /** 내가 남긴 리뷰 (null = 아직 안 불러옴). 온라인에서만 불러온다 */
    val reviews: StateFlow<List<com.nopo.cheonnyeon.data.remote.ReviewItem>?> = _reviews

    fun loadReviews() {
        val uid = container.auth.currentUid ?: return
        viewModelScope.launch { _reviews.value = container.reviews.byUser(uid).valueOrNull.orEmpty() }
    }

    val myUid: String? get() = container.auth.currentUid

    val state: StateFlow<PassportUiState> = combine(
        container.database.myVisitDao().observeAll(),
        container.catalog.changes,
    ) { visits, _ ->
        val stores = container.catalog.all().associateBy { it.id }
        PassportUiState(
            loading = false,
            stamps = visits.map { PassportStamp(it, stores[it.storeId]) },
            totalYears = visits.filter { !it.storeId.startsWith(Store.TEST_PREFIX) }.sumOf { it.yearsAwarded },
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, PassportUiState())
}

/** 내 패스포트 (S-702·S-703, FR-PAS-01~03): 세월 게이지, 칭호, 3열 도장 격자, 도장 상세 */
@Composable
fun PassportScreen(vm: PassportViewModel, onOpenStore: (String) -> Unit, onOpenProfile: (String) -> Unit) {
    val state by vm.state.collectAsStateWithLifecycle()
    val c = NopoTheme.colors
    var detail by rememberSaveable { mutableStateOf<String?>(null) }
    var reviewsTab by rememberSaveable { mutableStateOf(false) }
    val myReviews by vm.reviews.collectAsStateWithLifecycle()
    val title = TitlePolicy.titleFor(state.totalYears)

    Column(Modifier.fillMaxSize().background(c.paper).statusBarsPadding().padding(NopoTheme.spacing.lg), verticalArrangement = Arrangement.spacedBy(NopoTheme.spacing.md)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(NopoTheme.spacing.md)) {
            Text(stringResource(R.string.passport_title), style = NopoTheme.typography.headlineMedium, color = c.primary)
            TitleBadge(title)
        }
        YearsGauge(totalYears = state.totalYears, yearsToNextTitle = TitlePolicy.yearsToNext(state.totalYears))
        Row(horizontalArrangement = Arrangement.spacedBy(NopoTheme.spacing.sm), verticalAlignment = Alignment.CenterVertically) {
            androidx.compose.material3.FilterChip(!reviewsTab, { reviewsTab = false }, label = { Text(stringResource(R.string.passport_tab_stamps)) })
            androidx.compose.material3.FilterChip(reviewsTab, { reviewsTab = true; vm.loadReviews() }, label = { Text(stringResource(R.string.passport_tab_reviews)) })
            vm.myUid?.let { uid -> TextButton({ onOpenProfile(uid) }) { Text(stringResource(R.string.passport_my_profile)) } }
        }
        if (reviewsTab) {
            val list = myReviews
            when {
                list == null -> Text(stringResource(R.string.list_loading), color = c.ink2)
                list.isEmpty() -> Text(stringResource(R.string.profile_no_reviews), color = c.ink2)
                else -> androidx.compose.foundation.lazy.LazyColumn(verticalArrangement = Arrangement.spacedBy(NopoTheme.spacing.sm)) {
                    items(list.size) { i ->
                        val r = list[i]
                        Column(
                            Modifier.fillMaxWidth().background(c.surface, NopoTheme.shapes.md).clickable { onOpenStore(r.storeId) }.padding(NopoTheme.spacing.md),
                            verticalArrangement = Arrangement.spacedBy(NopoTheme.spacing.xs),
                        ) {
                            Text(r.storeName, color = c.ink, style = NopoTheme.typography.titleSmall)
                            com.nopo.cheonnyeon.core.designsystem.component.StarRating(r.rating.toFloat())
                            if (r.text.isNotBlank()) Text(r.text, color = c.ink, style = NopoTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        } else if (state.stamps.isEmpty()) {
            EmptyState(
                title = stringResource(R.string.passport_empty_title),
                body = stringResource(R.string.passport_empty_body),
                illustration = { StampView(years = 30, grade = com.nopo.cheonnyeon.domain.model.StampGrade.NORMAL, modifier = Modifier.alpha(0.25f)) },
            )
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(NopoTheme.spacing.sm),
                verticalArrangement = Arrangement.spacedBy(NopoTheme.spacing.md),
            ) {
                items(state.stamps, key = { it.visit.storeId }) { s ->
                    Column(
                        Modifier.clickable { detail = s.visit.storeId },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(NopoTheme.spacing.xs),
                    ) {
                        StampView(s.visit.yearsAwarded, StampGradePolicy.gradeFor(s.visit.yearsAwarded), size = StampSize.Grid)
                        Text(s.store?.name ?: s.visit.storeId, style = NopoTheme.typography.labelMedium, color = c.ink, maxLines = 1)
                    }
                }
                // 빈 칸: 다음에 갈 노포 자리
                items(if (state.stamps.size % 3 == 0) 3 else 3 - state.stamps.size % 3) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Spacer(
                            Modifier.height(80.dp).fillMaxWidth().background(c.tealSoft.copy(alpha = 0.4f), RoundedCornerShape(40.dp)),
                        )
                        Text(stringResource(R.string.passport_next_slot), style = NopoTheme.typography.labelMedium, color = c.ink2)
                    }
                }
            }
        }
    }

    state.stamps.firstOrNull { it.visit.storeId == detail }?.let { stamp ->
        StampDetail(stamp, onDismiss = { detail = null }, onOpenStore = { detail = null; onOpenStore(stamp.visit.storeId) })
    }
}

@Composable
private fun StampDetail(stamp: PassportStamp, onDismiss: () -> Unit, onOpenStore: () -> Unit) {
    val v = stamp.visit
    val first = Instant.ofEpochMilli(v.firstVisitedAt).atZone(SeoulZone).toLocalDate()
    val method = when (v.method) {
        "RECEIPT" -> R.string.method_receipt
        "PHOTO" -> R.string.method_photo
        else -> R.string.method_location_only
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onOpenStore) { Text(stringResource(R.string.passport_open_store)) } },
        dismissButton = { TextButton(onDismiss) { Text(stringResource(R.string.passport_close)) } },
        title = { Text(stamp.store?.name ?: v.storeId) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(NopoTheme.spacing.sm)) {
                StampView(v.yearsAwarded, StampGradePolicy.gradeFor(v.yearsAwarded), size = StampSize.Grid)
                Text(stringResource(R.string.stamp_first_visit, first.toString()))
                Text(stringResource(R.string.stamp_years, v.yearsAwarded))
                Text(stringResource(R.string.stamp_method, stringResource(method)))
                Text(stringResource(R.string.stamp_visit_count, v.visitCount))
                if (stamp.isTest) Text(stringResource(R.string.stamp_is_test), color = NopoTheme.colors.ink2)
            }
        },
    )
}
