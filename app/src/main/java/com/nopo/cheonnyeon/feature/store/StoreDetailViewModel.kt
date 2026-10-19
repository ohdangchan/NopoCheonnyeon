package com.nopo.cheonnyeon.feature.store

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nopo.cheonnyeon.app.AppContainer
import com.nopo.cheonnyeon.core.common.SeoulZone
import com.nopo.cheonnyeon.data.local.MyVisitEntity
import com.nopo.cheonnyeon.domain.model.Store
import com.nopo.cheonnyeon.domain.rule.YearsCalculator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.time.LocalDate

data class StoreDetailUiState(
    val loading: Boolean = true,
    val store: Store? = null,
    val years: Int = 0,
    val visit: MyVisitEntity? = null,
    val myTotalYears: Int = 0,
)

class StoreDetailViewModel(private val container: AppContainer, private val storeId: String) : ViewModel() {
    private val _state = MutableStateFlow(StoreDetailUiState())
    val state: StateFlow<StoreDetailUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val store = container.catalog.byId(storeId)
            val year = LocalDate.now(SeoulZone).year
            val dao = container.database.myVisitDao()
            combine(dao.observeAll(), dao.observeTotalYears()) { visits, total -> visits.firstOrNull { it.storeId == storeId } to total }
                .collect { (visit, total) ->
                    _state.value = StoreDetailUiState(
                        loading = false,
                        store = store,
                        years = store?.let { YearsCalculator.yearsAtVisit(it.foundedYear, year) } ?: 0,
                        visit = visit,
                        myTotalYears = total,
                    )
                }
        }
    }
}
