package com.nopo.cheonnyeon.feature.passport

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nopo.cheonnyeon.app.AppContainer
import com.nopo.cheonnyeon.data.remote.DemoUser
import com.nopo.cheonnyeon.domain.model.Store
import com.nopo.cheonnyeon.domain.rule.HallOfFameRule
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue

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
                    topStores = data?.topStores(10).orEmpty().mapNotNull { t -> stores[t.storeId]?.let { s -> HallStore(s, t.average,
                        t.count) } },
                )
            }
        }
    }
}
