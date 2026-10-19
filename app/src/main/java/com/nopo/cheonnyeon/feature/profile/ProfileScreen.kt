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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.nopo.cheonnyeon.R
import com.nopo.cheonnyeon.app.AppContainer
import com.nopo.cheonnyeon.core.common.AppError
import com.nopo.cheonnyeon.core.common.message
import com.nopo.cheonnyeon.core.designsystem.component.EmptyState
import com.nopo.cheonnyeon.core.designsystem.component.NopoButton
import com.nopo.cheonnyeon.core.designsystem.component.NopoButtonStyle
import com.nopo.cheonnyeon.core.designsystem.component.StarRating
import com.nopo.cheonnyeon.core.designsystem.component.TitleBadge
import com.nopo.cheonnyeon.core.designsystem.theme.NopoTheme
import com.nopo.cheonnyeon.data.remote.ReviewItem
import com.nopo.cheonnyeon.data.remote.UserProfile
import com.nopo.cheonnyeon.data.remote.VisitedStore
import com.nopo.cheonnyeon.domain.model.Title
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProfileUiState(
    val loading: Boolean = true,
    val profile: UserProfile? = null,
    val visits: List<VisitedStore> = emptyList(),
    val reviews: List<ReviewItem> = emptyList(),
    val error: AppError? = null,
)

class ProfileViewModel(private val container: AppContainer, private val uid: String) : ViewModel() {
    private val _state = MutableStateFlow(ProfileUiState())
    val state: StateFlow<ProfileUiState> = _state.asStateFlow()

    init { load() }

    fun load() {
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            val profile = container.profiles.profile(uid)
            val visits = container.profiles.visits(uid)
            val reviews = container.profiles.reviewsOf(uid)
            _state.update {
                ProfileUiState(
                    loading = false,
                    profile = profile.valueOrNull,
                    visits = visits.valueOrNull.orEmpty(),
                    reviews = reviews.valueOrNull.orEmpty(),
                    error = profile.errorOrNull,
                )
            }
        }
    }
}

fun titleOf(id: String): Title = Title.entries.firstOrNull { it.name == id } ?: Title.SPROUT

/** 유저 프로필 (S-804): 칭호·세월·다녀온 노포·매긴 평점. 없는 유저·오류도 안내한다. */
@Composable
fun ProfileScreen(vm: ProfileViewModel, onBack: () -> Unit, onOpenStore: (String) -> Unit) {
    val state by vm.state.collectAsStateWithLifecycle()
    val c = NopoTheme.colors
    val profile = state.profile
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
                    Row(horizontalArrangement = Arrangement.spacedBy(NopoTheme.spacing.md)) {
                        Text(profile.nickname, style = NopoTheme.typography.headlineMedium, color = c.ink)
                        TitleBadge(titleOf(profile.titleId))
                    }
                    Text(stringResource(R.string.profile_stats, profile.totalYears, profile.visitCount, profile.reviewCount), style = NopoTheme.extraTypography.metric, color = c.primary)

                    Text(stringResource(R.string.profile_visits), style = NopoTheme.typography.titleLarge, color = c.ink)
                    if (state.visits.isEmpty()) Text(stringResource(R.string.profile_no_visits), color = c.ink2)
                    state.visits.forEach { v ->
                        Row(
                            Modifier.fillMaxWidth().background(c.surface, NopoTheme.shapes.md).clickable { onOpenStore(v.storeId) }.padding(NopoTheme.spacing.md),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(v.storeName, color = c.ink, style = NopoTheme.typography.titleSmall)
                            Text(stringResource(R.string.years_value, v.yearsAwarded), color = c.stamp, style = NopoTheme.extraTypography.metric)
                        }
                    }

                    Text(stringResource(R.string.profile_reviews), style = NopoTheme.typography.titleLarge, color = c.ink)
                    if (state.reviews.isEmpty()) Text(stringResource(R.string.profile_no_reviews), color = c.ink2)
                    state.reviews.forEach { r ->
                        Column(
                            Modifier.fillMaxWidth().background(c.surface, NopoTheme.shapes.md).clickable { onOpenStore(r.storeId) }.padding(NopoTheme.spacing.md),
                            verticalArrangement = Arrangement.spacedBy(NopoTheme.spacing.xs),
                        ) {
                            Text(r.storeName, color = c.ink, style = NopoTheme.typography.titleSmall)
                            StarRating(r.rating.toFloat())
                            if (r.text.isNotBlank()) Text(r.text, color = c.ink, style = NopoTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
        NopoButton(stringResource(R.string.detail_back), onBack, style = NopoButtonStyle.Outline, modifier = Modifier.fillMaxWidth().padding(NopoTheme.spacing.md))
    }
}
