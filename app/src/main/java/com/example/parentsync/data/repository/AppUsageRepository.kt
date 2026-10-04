package com.example.parentsync.data.repository

import com.example.parentsync.data.model.AppUsage
import com.example.parentsync.data.model.CategoryBreakdown
import com.example.parentsync.data.model.ScreenTimeSummary
import kotlinx.coroutines.flow.Flow

interface AppUsageRepository {
    fun getAppUsageStats(): Flow<List<AppUsage>>
    fun getScreenTimeSummary(): Flow<ScreenTimeSummary>
    fun getCategoryBreakdowns(): Flow<List<CategoryBreakdown>>
    suspend fun refreshUsageStats()
    fun hasUsageStatsPermission(): Boolean
}
