package com.nopo.cheonnyeon.feature.passport

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.nopo.cheonnyeon.R
import com.nopo.cheonnyeon.core.designsystem.component.labelRes
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.stateIn
import java.time.Instant

data class PassportStamp(val visit: MyVisitEntity, val store: Store?) {
    val isTest: Boolean get() = visit.storeId.startsWith(Store.TEST_PREFIX)
}

data class PassportUiState(
    val loading: Boolean = true,
    val stamps: List<PassportStamp> = emptyList(),
    val totalYears: Int = 0,
    val regions: List<com.nopo.cheonnyeon.domain.rule.RegionProgress> = emptyList(),
)

class PassportViewModel(private val container: AppContainer) : ViewModel() {
    private val _reviews = kotlinx.coroutines.flow.MutableStateFlow<List<com.nopo.cheonnyeon.data.remote.ReviewItem>?>(null)

    /** 내가 남긴 리뷰 (null = 아직 안 불러옴). 온라인에서만 불러온다 */
    val reviews: StateFlow<List<com.nopo.cheonnyeon.data.remote.ReviewItem>?> = _reviews

    fun loadReviews() {
        val uid = container.auth.currentUid ?: return
        viewModelScope.launch {
            val server = container.reviews.byUser(uid).valueOrNull.orEmpty()
            // 시연용 패스포트를 채운 경우 그 도장에 달린 예시 리뷰도 함께 보여 준다
            val demo = if (container.settings.demoData.first() && container.database.myVisitDao().demoVisitCount() > 0) {
                container.demoReviews.myReviews(container.auth.currentNickname().orEmpty())
            } else emptyList()
            _reviews.value = demo + server
        }
    }

    val myUid: String? get() = container.auth.currentUid

    suspend fun nickname(): String = container.auth.currentNickname().orEmpty()

    val state: StateFlow<PassportUiState> = combine(
        container.database.myVisitDao().observeAll(),
        container.catalog.changes,
    ) { visits, _ ->
        val stores = container.catalog.all().associateBy { it.id }
        PassportUiState(
            loading = false,
            stamps = visits.map { PassportStamp(it, stores[it.storeId]) },
            totalYears = visits.filter { !it.storeId.startsWith(Store.TEST_PREFIX) }.sumOf { it.yearsAwarded },
            regions = com.nopo.cheonnyeon.domain.rule.RegionProgressRule.compute(stores.values.toList(), visits.map { it.storeId }.toSet()),
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, PassportUiState())
}

/** 내 패스포트 (S-702·S-703, FR-PAS-01~03): 세월 게이지, 칭호, 3열 도장 격자, 도장 상세 */
@Composable
fun PassportScreen(vm: PassportViewModel, onOpenStore: (String) -> Unit, onOpenProfile: (String) -> Unit, onOpenHall: () -> Unit) {
    val state by vm.state.collectAsStateWithLifecycle()
    val c = NopoTheme.colors
    var detail by rememberSaveable { mutableStateOf<String?>(null) }
    var reviewsTab by rememberSaveable { mutableStateOf(false) }
    var regionsTab by rememberSaveable { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    var shareFailed by rememberSaveable { mutableStateOf(false) }
    val myReviews by vm.reviews.collectAsStateWithLifecycle()
    val title = TitlePolicy.titleFor(state.totalYears)
    val titleName = stringResource(title.labelRes())

    Column(Modifier.fillMaxSize().background(c.paper).statusBarsPadding().padding(NopoTheme.spacing.lg), verticalArrangement = Arrangement.spacedBy(NopoTheme.spacing.md)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(NopoTheme.spacing.md)) {
            Text(stringResource(R.string.passport_title), style = NopoTheme.typography.headlineMedium, color = c.primary)
            var dex by rememberSaveable { mutableStateOf(false) }
            val dexLabel = stringResource(R.string.cd_open_title_dex)
            Box(Modifier.clickable(role = androidx.compose.ui.semantics.Role.Button) { dex = true }.semantics { contentDescription = dexLabel }) { TitleBadge(title) }
            if (dex) TitleDexDialog(state.totalYears) { dex = false }
        }
        YearsGauge(totalYears = state.totalYears, yearsToNextTitle = TitlePolicy.yearsToNext(state.totalYears))
        val stats = com.nopo.cheonnyeon.domain.rule.PassportStats.of(state.stamps.filter { !it.isTest }.map { (it.store?.sido ?: "") to it.visit.yearsAwarded })
        if (stats.stamps > 0) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(NopoTheme.spacing.sm)) {
                listOf(
                    R.string.passport_stat_stamps to stats.stamps.toString(),
                    R.string.passport_stat_regions to stats.regions.toString(),
                    R.string.passport_stat_oldest to stringResource(R.string.hall_years, stats.oldestYears),
                ).forEach { (label, value) ->
                    Column(
                        Modifier.weight(1f).background(c.surface, NopoTheme.shapes.md).padding(NopoTheme.spacing.sm),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(value, style = NopoTheme.typography.titleLarge, color = c.stamp)
                        Text(stringResource(label), style = NopoTheme.typography.labelMedium, color = c.ink2)
                    }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(NopoTheme.spacing.sm), verticalAlignment = Alignment.CenterVertically) {
            androidx.compose.material3.FilterChip(!reviewsTab && !regionsTab, { reviewsTab = false; regionsTab = false }, label = { Text(stringResource(R.string.passport_tab_stamps)) })
            androidx.compose.material3.FilterChip(regionsTab, { regionsTab = true; reviewsTab = false }, label = { Text(stringResource(R.string.passport_tab_regions)) })
            androidx.compose.material3.FilterChip(reviewsTab, { reviewsTab = true; regionsTab = false; vm.loadReviews() }, label = { Text(stringResource(R.string.passport_tab_reviews)) })
            vm.myUid?.let { uid -> TextButton({ onOpenProfile(uid) }) { Text(stringResource(R.string.passport_my_profile)) } }
        }
        TextButton({ onOpenHall() }) { Text(stringResource(R.string.hall_button)) }
        if (regionsTab) {
            androidx.compose.foundation.lazy.LazyColumn(verticalArrangement = Arrangement.spacedBy(NopoTheme.spacing.sm)) {
                item { Text(stringResource(R.string.regions_title), style = NopoTheme.typography.titleLarge, color = c.ink) }
                items(state.regions.size) { i ->
                    val r = state.regions[i]
                    Column(Modifier.fillMaxWidth().background(c.surface, NopoTheme.shapes.md).padding(NopoTheme.spacing.md), verticalArrangement = Arrangement.spacedBy(NopoTheme.spacing.xs)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(r.sido, color = c.ink, style = NopoTheme.typography.titleSmall)
                            Text(stringResource(R.string.regions_row, r.visited, r.total, r.percent), color = c.ink2, style = NopoTheme.typography.bodySmall)
                        }
                        androidx.compose.material3.LinearProgressIndicator(
                            progress = { r.percent / 100f }, modifier = Modifier.fillMaxWidth(), color = c.primary, trackColor = c.tealSoft,
                        )
                    }
                }
            }
        } else if (reviewsTab) {
            val list = myReviews
            when {
                list == null -> Text(stringResource(R.string.list_loading), color = c.ink2)
                list.isEmpty() -> Text(stringResource(R.string.profile_no_reviews), color = c.ink2)
                else -> androidx.compose.foundation.lazy.LazyColumn(verticalArrangement = Arrangement.spacedBy(NopoTheme.spacing.sm)) {
                    items(list.size) { i ->
                        val r = list[i]
                        Column(
                            Modifier.fillMaxWidth().background(c.surface, NopoTheme.shapes.md).clickable(role = androidx.compose.ui.semantics.Role.Button) { onOpenStore(r.storeId) }.padding(NopoTheme.spacing.md),
                            verticalArrangement = Arrangement.spacedBy(NopoTheme.spacing.xs),
                        ) {
                            Text(r.storeName, color = c.ink, style = NopoTheme.typography.titleSmall)
                            com.nopo.cheonnyeon.core.designsystem.component.StarRating(r.rating.toFloat())
                            if (r.text.isNotBlank()) Text(r.text, color = c.ink, style = NopoTheme.typography.bodyMedium)
                            if (r.photos.isNotEmpty()) com.nopo.cheonnyeon.feature.store.ReviewPhotoRow(r.photos, Modifier.fillMaxWidth())
                            if (!r.demo) TextButton({ onOpenStore(r.storeId) }) { Text(stringResource(R.string.my_review_edit)) }
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
            Column(verticalArrangement = Arrangement.spacedBy(NopoTheme.spacing.md)) {
            com.nopo.cheonnyeon.core.designsystem.component.NopoButton(
                stringResource(R.string.share_card_button),
                {
                    scope.launch {
                        val real = state.stamps.filter { !it.isTest }
                        val data = com.nopo.cheonnyeon.platform.share.ShareCardData(
                            nickname = vm.nickname().ifBlank { "-" },
                            titleName = titleName,
                            totalYears = state.totalYears,
                            stampCount = real.size,
                            recent = real.sortedByDescending { it.visit.firstVisitedAt }.take(3).map { (it.store?.name ?: it.visit.storeId) to it.visit.yearsAwarded },
                            colors = com.nopo.cheonnyeon.platform.share.ShareCardColors(
                                c.paper.toArgb(), c.surface.toArgb(), c.primary.toArgb(), c.stamp.toArgb(), c.brass.toArgb(), c.ink.toArgb(), c.ink2.toArgb(),
                            ),
                        )
                        shareFailed = !com.nopo.cheonnyeon.platform.share.PassportShareCard.share(context, data)
                    }
                },
                style = com.nopo.cheonnyeon.core.designsystem.component.NopoButtonStyle.Outline,
                modifier = Modifier.fillMaxWidth(),
            )
            if (shareFailed) Text(stringResource(R.string.share_card_failed), color = c.stamp, style = NopoTheme.typography.bodySmall)
            LazyVerticalGrid(
                modifier = Modifier.weight(1f),
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(NopoTheme.spacing.sm),
                verticalArrangement = Arrangement.spacedBy(NopoTheme.spacing.md),
            ) {
                items(state.stamps, key = { it.visit.storeId }) { s ->
                    Column(
                        Modifier.clickable(role = androidx.compose.ui.semantics.Role.Button) { detail = s.visit.storeId },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(NopoTheme.spacing.xs),
                    ) {
                        StampView(s.visit.yearsAwarded, StampGradePolicy.gradeFor(s.visit.yearsAwarded), size = StampSize.Grid)
                        Text(s.store?.name ?: s.visit.storeId, style = NopoTheme.typography.labelMedium, color = c.ink, maxLines = 1)
                        if (!s.visit.synced && !s.isTest) Text(stringResource(R.string.stamp_pending), style = NopoTheme.typography.labelMedium, color = c.stamp)
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
        "DEMO" -> R.string.method_demo
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
                val proof = java.io.File(androidx.compose.ui.platform.LocalContext.current.filesDir, "proofs/" + v.storeId + ".jpg")
                if (proof.exists()) {
                    coil3.compose.AsyncImage(
                        model = proof, contentDescription = stringResource(R.string.stamp_proof_photo),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                        modifier = Modifier.fillMaxWidth().height(160.dp),
                    )
                }
                Text(stringResource(R.string.stamp_first_visit, first.toString()))
                Text(stringResource(R.string.stamp_years, v.yearsAwarded))
                Text(stringResource(R.string.stamp_method, stringResource(method)))
                Text(stringResource(R.string.stamp_visit_count, v.visitCount))
                if (stamp.isTest) Text(stringResource(R.string.stamp_is_test), color = NopoTheme.colors.ink2)
            }
        },
    )
}
