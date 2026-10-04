package com.example.parentsync.ui.tracking

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.parentsync.data.model.AppUsage
import com.example.parentsync.data.model.CategoryBreakdown
import com.example.parentsync.data.model.ScreenTimeSummary
import com.example.parentsync.data.repository.AppUsageRepository
import com.example.parentsync.data.repository.AppUsageRepositoryImpl
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AppActivityTrackingViewModel(
    application: Application,
    private val repository: AppUsageRepository = AppUsageRepositoryImpl(application)
) : AndroidViewModel(application) {

    val appUsages: StateFlow<List<AppUsage>> = repository.getAppUsageStats()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val screenTimeSummary: StateFlow<ScreenTimeSummary?> = repository.getScreenTimeSummary()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val categoryBreakdowns: StateFlow<List<CategoryBreakdown>> = repository.getCategoryBreakdowns()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val hasPermission: Boolean
        get() = repository.hasUsageStatsPermission()

    fun refresh() {
        viewModelScope.launch {
            repository.refreshUsageStats()
        }
    }
}
