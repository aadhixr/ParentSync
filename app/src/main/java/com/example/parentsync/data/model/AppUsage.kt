package com.example.parentsync.data.model

import androidx.annotation.DrawableRes

enum class AppCategory(val displayName: String) {
    SOCIAL("Social"),
    GAMES("Games"),
    PRODUCTIVITY("Productivity"),
    ENTERTAINMENT("Entertainment"),
    EDUCATION("Education"),
    OTHER("Other")
}

data class AppUsage(
    val packageName: String,
    val appName: String,
    val usageTimeMillis: Long,
    val category: AppCategory,
    @DrawableRes val iconRes: Int? = null,
    val lastTimeUsed: Long = System.currentTimeMillis()
)

data class ScreenTimeSummary(
    val totalUsageMillis: Long,
    val dailyAverageMillis: Long,
    val comparisonToYesterdayPercent: Int // e.g. -12% or +15%
)

data class CategoryBreakdown(
    val category: AppCategory,
    val usageTimeMillis: Long,
    val percentage: Float
)
