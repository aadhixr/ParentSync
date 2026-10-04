package com.example.parentsync.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_usage_limits")
data class AppUsageLimitEntity(
    @PrimaryKey
    val packageName: String,
    val appName: String,
    val category: String,
    val dailyQuotaMinutes: Int, // 0 = unlimited, 15, 30, 60, 120, etc.
    val isBlocked: Boolean = false
)

data class AppUsageLimit(
    val packageName: String,
    val appName: String,
    val category: AppCategory,
    val dailyQuotaMinutes: Int, // 0 = unlimited
    val isBlocked: Boolean,
    val usageTimeMillis: Long
) {
    val quotaMillis: Long
        get() = if (dailyQuotaMinutes > 0) dailyQuotaMinutes * 60 * 1000L else 0L

    val remainingTimeMillis: Long
        get() {
            if (dailyQuotaMinutes <= 0) return Long.MAX_VALUE
            return (quotaMillis - usageTimeMillis).coerceAtLeast(0L)
        }

    val isExhausted: Boolean
        get() = dailyQuotaMinutes > 0 && usageTimeMillis >= quotaMillis
}
