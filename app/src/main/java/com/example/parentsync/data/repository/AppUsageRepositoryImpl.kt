package com.example.parentsync.data.repository

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Process
import com.example.parentsync.data.model.AppCategory
import com.example.parentsync.data.model.AppUsage
import com.example.parentsync.data.model.CategoryBreakdown
import com.example.parentsync.data.model.ScreenTimeSummary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Calendar

class AppUsageRepositoryImpl(
    private val context: Context
) : AppUsageRepository {

    private val _appUsages = MutableStateFlow<List<AppUsage>>(emptyList())
    private val _screenTimeSummary = MutableStateFlow<ScreenTimeSummary>(
        ScreenTimeSummary(0L, 0L, 0)
    )
    private val _categoryBreakdowns = MutableStateFlow<List<CategoryBreakdown>>(emptyList())

    init {
        loadUsageData()
    }

    override fun getAppUsageStats(): Flow<List<AppUsage>> = _appUsages.asStateFlow()

    override fun getScreenTimeSummary(): Flow<ScreenTimeSummary> = _screenTimeSummary.asStateFlow()

    override fun getCategoryBreakdowns(): Flow<List<CategoryBreakdown>> = _categoryBreakdowns.asStateFlow()

    override suspend fun refreshUsageStats() {
        loadUsageData()
    }

    override fun hasUsageStatsPermission(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager ?: return false
        val mode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    private fun loadUsageData() {
        val usages = if (hasUsageStatsPermission()) {
            queryRealUsageStats()
        } else {
            emptyList()
        }

        _appUsages.value = usages.sortedByDescending { it.usageTimeMillis }
        calculateSummaryAndBreakdowns(usages)
    }

    private fun queryRealUsageStats(): List<AppUsage> {
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
            ?: return emptyList()

        val calendar = Calendar.getInstance()
        val endTime = calendar.timeInMillis
        calendar.add(Calendar.DAY_OF_YEAR, -1)
        val startTime = calendar.timeInMillis

        val stats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            startTime,
            endTime
        )

        if (stats.isNullOrEmpty()) {
            return emptyList()
        }

        val packageManager = context.packageManager
        val result = mutableListOf<AppUsage>()

        for (usageStat in stats) {
            val totalTime = usageStat.totalTimeInForeground
            if (totalTime > 0) {
                val packageName = usageStat.packageName
                val appName = try {
                    val appInfo = packageManager.getApplicationInfo(packageName, 0)
                    packageManager.getApplicationLabel(appInfo).toString()
                } catch (e: PackageManager.NameNotFoundException) {
                    packageName.substringAfterLast('.')
                }

                val category = categorizeApp(packageName, appName)
                result.add(
                    AppUsage(
                        packageName = packageName,
                        appName = appName,
                        usageTimeMillis = totalTime,
                        category = category,
                        lastTimeUsed = usageStat.lastTimeUsed
                    )
                )
            }
        }

        return result
    }

    private fun categorizeApp(packageName: String, appName: String): AppCategory {
        val lowerPkg = packageName.lowercase()
        val lowerName = appName.lowercase()
        return when {
            lowerPkg.contains("social") || lowerPkg.contains("whatsapp") || lowerPkg.contains("instagram") ||
                    lowerPkg.contains("facebook") || lowerPkg.contains("twitter") || lowerPkg.contains("snapchat") ||
                    lowerPkg.contains("tiktok") || lowerName.contains("chat") || lowerName.contains("social") -> AppCategory.SOCIAL

            lowerPkg.contains("game") || lowerPkg.contains("play") || lowerName.contains("game") ||
                    lowerName.contains("craft") || lowerName.contains("subway") -> AppCategory.GAMES

            lowerPkg.contains("edu") || lowerPkg.contains("classroom") || lowerPkg.contains("duolingo") ||
                    lowerName.contains("learn") || lowerName.contains("school") -> AppCategory.EDUCATION

            lowerPkg.contains("video") || lowerPkg.contains("music") || lowerPkg.contains("netflix") ||
                    lowerPkg.contains("youtube") || lowerPkg.contains("spotify") || lowerName.contains("tube") -> AppCategory.ENTERTAINMENT

            lowerPkg.contains("mail") || lowerPkg.contains("docs") || lowerPkg.contains("calc") ||
                    lowerName.contains("office") || lowerName.contains("notes") -> AppCategory.PRODUCTIVITY

            else -> AppCategory.OTHER
        }
    }

    private fun calculateSummaryAndBreakdowns(usages: List<AppUsage>) {
        val totalMillis = usages.sumOf { it.usageTimeMillis }
        val dailyAverageMillis = (totalMillis * 0.9).toLong() // Simulated average
        val comparison = -8 // e.g. 8% less than yesterday

        _screenTimeSummary.value = ScreenTimeSummary(
            totalUsageMillis = totalMillis,
            dailyAverageMillis = dailyAverageMillis,
            comparisonToYesterdayPercent = comparison
        )

        val categoryMap = usages.groupBy { it.category }
        val breakdowns = AppCategory.values().map { cat ->
            val catTotal = categoryMap[cat]?.sumOf { it.usageTimeMillis } ?: 0L
            val percentage = if (totalMillis > 0) (catTotal.toFloat() / totalMillis.toFloat()) * 100f else 0f
            CategoryBreakdown(
                category = cat,
                usageTimeMillis = catTotal,
                percentage = percentage
            )
        }.filter { it.usageTimeMillis > 0 }
            .sortedByDescending { it.usageTimeMillis }

        _categoryBreakdowns.value = breakdowns
    }
}
