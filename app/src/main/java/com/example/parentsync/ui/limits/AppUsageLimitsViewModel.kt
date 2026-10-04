package com.example.parentsync.ui.limits

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.parentsync.data.local.ParentSyncDatabase
import com.example.parentsync.data.model.AppUsageLimit
import com.example.parentsync.data.repository.AppUsageLimitRepository
import com.example.parentsync.data.repository.AppUsageLimitRepositoryImpl
import com.example.parentsync.data.repository.AppUsageRepository
import com.example.parentsync.data.repository.AppUsageRepositoryImpl
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AppUsageLimitsViewModel(
    application: Application,
    private val usageRepository: AppUsageRepository = AppUsageRepositoryImpl(application),
    private val limitRepository: AppUsageLimitRepository = AppUsageLimitRepositoryImpl(
        ParentSyncDatabase.getDatabase(application).appUsageLimitDao()
    )
) : AndroidViewModel(application) {

    init {
        viewModelScope.launch {
            usageRepository.getAppUsageStats().collect { appList ->
                limitRepository.initLimitsForApps(appList)
            }
        }
    }

    val appUsageLimits: StateFlow<List<AppUsageLimit>> = usageRepository.getAppUsageStats()
        .flatMapLatest { apps ->
            limitRepository.getAppUsageLimits(apps)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setQuota(packageName: String, quotaMinutes: Int) {
        viewModelScope.launch {
            limitRepository.setQuota(packageName, quotaMinutes)
        }
    }

    fun toggleBlock(packageName: String, isBlocked: Boolean) {
        viewModelScope.launch {
            limitRepository.setBlocked(packageName, isBlocked)
        }
    }
}
