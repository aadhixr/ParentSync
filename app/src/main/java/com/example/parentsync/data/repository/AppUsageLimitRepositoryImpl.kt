package com.example.parentsync.data.repository

import com.example.parentsync.data.local.AppUsageLimitDao
import com.example.parentsync.data.local.AppUsageLimitEntity
import com.example.parentsync.data.model.AppCategory
import com.example.parentsync.data.model.AppUsage
import com.example.parentsync.data.model.AppUsageLimit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AppUsageLimitRepositoryImpl(
    private val dao: AppUsageLimitDao
) : AppUsageLimitRepository {

    override fun getAppUsageLimits(appUsages: List<AppUsage>): Flow<List<AppUsageLimit>> {
        return dao.getAllLimits().map { entities ->
            val entityMap = entities.associateBy { it.packageName }
            appUsages.map { usage ->
                val entity = entityMap[usage.packageName]
                AppUsageLimit(
                    packageName = usage.packageName,
                    appName = usage.appName,
                    category = usage.category,
                    dailyQuotaMinutes = entity?.dailyQuotaMinutes ?: 0, // default 0 (unlimited)
                    isBlocked = entity?.isBlocked ?: false,
                    usageTimeMillis = usage.usageTimeMillis
                )
            }
        }
    }

    override suspend fun setQuota(packageName: String, quotaMinutes: Int) {
        val existing = dao.getLimit(packageName)
        if (existing != null) {
            dao.updateQuota(packageName, quotaMinutes)
        } else {
            dao.insertOrUpdateLimit(
                AppUsageLimitEntity(
                    packageName = packageName,
                    appName = packageName.substringAfterLast('.'),
                    category = AppCategory.OTHER.name,
                    dailyQuotaMinutes = quotaMinutes,
                    isBlocked = false
                )
            )
        }
    }

    override suspend fun setBlocked(packageName: String, isBlocked: Boolean) {
        val existing = dao.getLimit(packageName)
        if (existing != null) {
            dao.updateBlockStatus(packageName, isBlocked)
        } else {
            dao.insertOrUpdateLimit(
                AppUsageLimitEntity(
                    packageName = packageName,
                    appName = packageName.substringAfterLast('.'),
                    category = AppCategory.OTHER.name,
                    dailyQuotaMinutes = 0,
                    isBlocked = isBlocked
                )
            )
        }
    }

    override suspend fun initLimitsForApps(apps: List<AppUsage>) {
        for (app in apps) {
            dao.insertLimitIfNotExists(
                AppUsageLimitEntity(
                    packageName = app.packageName,
                    appName = app.appName,
                    category = app.category.name,
                    dailyQuotaMinutes = 0,
                    isBlocked = false
                )
            )
        }
    }
}
