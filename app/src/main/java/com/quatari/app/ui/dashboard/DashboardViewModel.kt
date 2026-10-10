package com.quatari.app.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.quatari.app.data.repository.DoubleEntryLedgerRepository
import com.quatari.app.domain.model.DashboardSummary
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

enum class TimeFilter {
    ALL,
    MONTHLY,
    WEEKLY
}

class DashboardViewModel(
    private val repository: DoubleEntryLedgerRepository
) : ViewModel() {

    val dashboardSummary: StateFlow<DashboardSummary> =
        repository.getDashboardSummary()
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                DashboardSummary(
                    liquidNetWorthSantim = 0L,
                    cbeBalanceSantim = 0L,
                    telebirrBalanceSantim = 0L,
                    abyssiniaBalanceSantim = 0L,
                    totalRevenueSantim = 0L,
                    totalExpenseSantim = 0L,
                    channelRevenues = emptyList(),
                    cashFlowPoints = emptyList(),
                    pendingTriageCount = 0
                )
            )

    class Factory(private val repository: DoubleEntryLedgerRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return DashboardViewModel(repository) as T
        }
    }
}
