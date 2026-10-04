package com.example.parentsync.data.repository

import com.example.parentsync.data.model.AppUsageLimit
import com.example.parentsync.data.model.AppUsage
import kotlinx.coroutines.flow.Flow

interface AppUsageLimitRepository {
    fun getAppUsageLimits(appUsages: List<AppUsage>): Flow<List<AppUsageLimit>>
    suspend fun setQuota(packageName: String, quotaMinutes: Int)
    suspend fun setBlocked(packageName: String, isBlocked: Boolean)
    suspend fun initLimitsForApps(apps: List<AppUsage>)
}
