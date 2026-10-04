package com.example.parentsync.data.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class DeviceStatusDto(
    val deviceId: String,
    val batteryLevel: Float, // 0.0 to 1.0
    val isCharging: Boolean,
    val isOnline: Boolean,
    val timestamp: Long
)

@JsonClass(generateAdapter = true)
data class AppUsageReportDto(
    val packageName: String,
    val appName: String,
    val usageTimeMillis: Long,
    val category: String,
    val timestamp: Long
)

@JsonClass(generateAdapter = true)
data class SyncPayloadDto(
    val childId: String,
    val deviceStatus: DeviceStatusDto,
    val appUsageReports: List<AppUsageReportDto>,
    val syncTimestamp: Long
)

@JsonClass(generateAdapter = true)
data class SyncResponseDto(
    val success: Boolean,
    val message: String?,
    val serverTimestamp: Long
)
