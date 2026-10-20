package com.nopo.cheonnyeon.feature.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.nopo.cheonnyeon.R
import com.nopo.cheonnyeon.app.AppContainer
import com.nopo.cheonnyeon.core.common.AppError
import com.nopo.cheonnyeon.core.common.SeoulZone
import com.nopo.cheonnyeon.core.common.message
import com.nopo.cheonnyeon.core.designsystem.component.EmptyState
import com.nopo.cheonnyeon.core.designsystem.component.NopoButton
import com.nopo.cheonnyeon.core.designsystem.component.NopoButtonStyle
import com.nopo.cheonnyeon.core.designsystem.component.StampSize
import com.nopo.cheonnyeon.core.designsystem.component.StampView
import com.nopo.cheonnyeon.core.designsystem.component.StarRating
import com.nopo.cheonnyeon.core.designsystem.component.TitleBadge
import com.nopo.cheonnyeon.core.designsystem.component.YearsGauge
import com.nopo.cheonnyeon.core.designsystem.theme.NopoTheme
import com.nopo.cheonnyeon.data.remote.ReviewItem
import com.nopo.cheonnyeon.data.remote.UserProfile
import com.nopo.cheonnyeon.data.remote.VisitedStore
import com.nopo.cheonnyeon.domain.model.Title
import com.nopo.cheonnyeon.domain.rule.StampGradePolicy
import com.nopo.cheonnyeon.domain.rule.TitlePolicy
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant

data class ProfileUiState(
    val loading: Boolean = true,
    val profile: UserProfile? = null,
    val visits: List<VisitedStore> = emptyList(),
    val reviews: List<ReviewItem> = emptyList(),
    val error: AppError? = null,
    /** 다른 사용자가 방문 기록을 공개하지 않아 목록을 가져오지 않았다 (FR-PRF-07) */
    val visitsHidden: Boolean = false,
)

class ProfileViewModel(private val container: AppContainer, private val uid: String) : ViewModel() {
    private val _state = MutableStateFlow(ProfileUiState())
    val state: StateFlow<ProfileUiState> = _state.asStateFlow()

    init { load() }

    fun load() {
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            val profile = container.profiles.profile(uid)
            val p = profile.valueOrNull
            // 공개하지 않는 사용자의 방문 목록은 서버 규칙도 막으므로 요청하지 않는다
            val hidden = p != null && p.visibility != "ALL" && uid != container.auth.currentUid
            val visits = if (hidden) null else container.profiles.visits(uid)
            val reviews = container.profiles.reviewsOf(uid)
            _state.update {
                ProfileUiState(
                    loading = false,
                    profile = p,
                    visits = visits?.valueOrNull.orEmpty(),
                    reviews = reviews.valueOrNull.orEmpty(),
                    error = profile.errorOrNull,
                    visitsHidden = hidden,
                )
            }
        }
    }
}

fun titleOf(id: String): Title = Title.entries.firstOrNull { it.name == id } ?: Title.SPROUT

private const val PAGE = 20

private fun dateOf(millis: Long?): String =
    millis?.let { Instant.ofEpochMilli(it).atZone(SeoulZone).toLocalDate().toString() } ?: "-"

/** 유저 프로필 (S-804, FR-PRF-02~04): 칭호·세월 게이지·가입일, 탭(다녀온 노포 / 매긴 평점), 20개씩 더보기. 시각(시:분)·좌표는 보여 주지 않는다. */
@Composable
fun ProfileScreen(vm: ProfileViewModel, onBack: () -> Unit, onOpenStore: (String) -> Unit) {
    val state by vm.state.collectAsStateWithLifecycle()
    val c = NopoTheme.colors
    val profile = state.profile
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var shownVisits by rememberSaveable { mutableIntStateOf(PAGE) }
    var shownReviews by rememberSaveable { mutableIntStateOf(PAGE) }
    Column(Modifier.fillMaxSize().background(c.paper).statusBarsPadding()) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(NopoTheme.spacing.lg), verticalArrangement = Arrangement.spacedBy(NopoTheme.spacing.md)) {
            when {
                state.loading -> Text(stringResource(R.string.list_loading), color = c.ink)
                state.error != null -> {
                    Text(state.error!!.message(LocalContext.current), color = c.stamp)
                    NopoButton(stringResource(R.string.profile_retry), vm::load, modifier = Modifier.fillMaxWidth())
                }
                profile == null -> EmptyState(stringResource(R.string.profile_not_found))
                else -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(NopoTheme.spacing.md), verticalAlignment = Alignment.CenterVertically) {
                        Text(profile.nickname, style = NopoTheme.typography.headlineMedium, color = c.ink)
                        TitleBadge(titleOf(profile.titleId))
                    }
                    Text(stringResource(R.string.profile_joined, dateOf(profile.createdAtMillis)), style = NopoTheme.typography.bodySmall, color = c.ink2)
                    Text(stringResource(R.string.profile_stats, profile.totalYears, profile.visitCount, profile.reviewCount), style = NopoTheme.extraTypography.metric, color = c.primary)
                    YearsGauge(totalYears = profile.totalYears, yearsToNextTitle = TitlePolicy.yearsToNext(profile.totalYears))

                    Row(horizontalArrangement = Arrangement.spacedBy(NopoTheme.spacing.sm)) {
                        FilterChip(tab == 0, { tab = 0 }, label = { Text(stringResource(R.string.profile_tab_visits, state.visits.size)) })
                        FilterChip(tab == 1, { tab = 1 }, label = { Text(stringResource(R.string.profile_tab_reviews, state.reviews.size)) })
                    }

                    if (tab == 0) {
                        when {
                            state.visitsHidden && profile.visibility == "COUNT" ->
                                Text(stringResource(R.string.profile_visits_count_only, profile.visitCount), color = c.ink2)
                            state.visitsHidden -> Text(stringResource(R.string.profile_visits_private), color = c.ink2)
                            state.visits.isEmpty() -> Text(stringResource(R.string.profile_no_visits), color = c.ink2)
                            else -> {
                                state.visits.take(shownVisits).forEach { v -> VisitCard(v, onOpenStore) }
                                if (state.visits.size > shownVisits) {
                                    NopoButton(stringResource(R.string.reviews_more), { shownVisits += PAGE }, style = NopoButtonStyle.Outline, modifier = Modifier.fillMaxWidth())
                                }
                            }
                        }
                    } else {
                        if (state.reviews.isEmpty()) Text(stringResource(R.string.profile_no_reviews), color = c.ink2)
                        state.reviews.take(shownReviews).forEach { r -> ReviewCard(r, onOpenStore) }
                        if (state.reviews.size > shownReviews) {
                            NopoButton(stringResource(R.string.reviews_more), { shownReviews += PAGE }, style = NopoButtonStyle.Outline, modifier = Modifier.fillMaxWidth())
                        }
                    }
                    Text(stringResource(R.string.profile_privacy_note), style = NopoTheme.typography.bodySmall, color = c.ink2)
                }
            }
        }
        NopoButton(stringResource(R.string.detail_back), onBack, style = NopoButtonStyle.Outline, modifier = Modifier.fillMaxWidth().padding(NopoTheme.spacing.md))
    }
}

@Composable
private fun VisitCard(v: VisitedStore, onOpenStore: (String) -> Unit) {
    val c = NopoTheme.colors
    Row(
        Modifier.fillMaxWidth().background(c.surface, NopoTheme.shapes.md).clickable { onOpenStore(v.storeId) }.padding(NopoTheme.spacing.md),
        horizontalArrangement = Arrangement.spacedBy(NopoTheme.spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StampView(v.yearsAwarded, StampGradePolicy.gradeFor(v.yearsAwarded), size = StampSize.Mini)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(NopoTheme.spacing.xxs)) {
            Text(v.storeName, color = c.ink, style = NopoTheme.typography.titleSmall)
            Text(
                listOfNotNull(v.sigungu.ifBlank { null }, stringResource(R.string.years_value, v.yearsAwarded)).joinToString(" · "),
                color = c.ink2, style = NopoTheme.typography.bodySmall,
            )
            Text(stringResource(R.string.profile_visit_meta, dateOf(v.firstVisitedMillis), v.visitCount), color = c.ink2, style = NopoTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun ReviewCard(r: ReviewItem, onOpenStore: (String) -> Unit) {
    val c = NopoTheme.colors
    Column(
        Modifier.fillMaxWidth().background(c.surface, NopoTheme.shapes.md).clickable { onOpenStore(r.storeId) }.padding(NopoTheme.spacing.md),
        verticalArrangement = Arrangement.spacedBy(NopoTheme.spacing.xs),
    ) {
        Text(r.storeName, color = c.ink, style = NopoTheme.typography.titleSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(NopoTheme.spacing.sm), verticalAlignment = Alignment.CenterVertically) {
            StarRating(r.rating.toFloat())
            Text(dateOf(r.updatedAtMillis), color = c.ink2, style = NopoTheme.typography.bodySmall)
        }
        if (r.text.isNotBlank()) Text(r.text, color = c.ink, style = NopoTheme.typography.bodyMedium)
    }
}
