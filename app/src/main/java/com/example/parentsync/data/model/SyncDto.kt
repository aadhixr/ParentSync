package com.example.parentsync.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.serialization.SerialName

@JsonClass(generateAdapter = true)
data class InstalledAppDto(
    @Json(name = "package_name") @SerialName("package_name") val packageName: String,
    @Json(name = "app_name") @SerialName("app_name") val appName: String,
    @Json(name = "is_system_app") @SerialName("is_system_app") val isSystemApp: Boolean
)

@JsonClass(generateAdapter = true)
data class DeviceStatusDto(
    @Json(name = "device_id") @SerialName("device_id") val deviceId: String,
    @Json(name = "battery_level") @SerialName("battery_level") val batteryLevel: Int, // 0 to 100
    @Json(name = "is_charging") @SerialName("is_charging") val isCharging: Boolean,
    @Json(name = "battery_health") @SerialName("battery_health") val batteryHealth: String,
    @Json(name = "active_foreground_package") @SerialName("active_foreground_package") val activeForegroundPackage: String?,
    @Json(name = "device_locked") @SerialName("device_locked") val deviceLocked: Boolean,
    @Json(name = "installed_apps") @SerialName("installed_apps") val installedApps: List<InstalledAppDto>,
    @Json(name = "is_online") @SerialName("is_online") val isOnline: Boolean,
    @Json(name = "timestamp") @SerialName("timestamp") val timestamp: Long
)

@JsonClass(generateAdapter = true)
data class AppUsageReportDto(
    @Json(name = "package_name") @SerialName("package_name") val packageName: String,
    @Json(name = "app_name") @SerialName("app_name") val appName: String,
    @Json(name = "usage_time_millis") @SerialName("usage_time_millis") val usageTimeMillis: Long,
    @Json(name = "category") @SerialName("category") val category: String,
    @Json(name = "timestamp") @SerialName("timestamp") val timestamp: Long
)

@JsonClass(generateAdapter = true)
data class SyncPayloadDto(
    @Json(name = "device_id") @SerialName("device_id") val deviceId: String,
    @Json(name = "battery_level") @SerialName("battery_level") val batteryLevel: Int,
    @Json(name = "is_charging") @SerialName("is_charging") val isCharging: Boolean,
    @Json(name = "app_usage_json") @SerialName("app_usage_json") val appUsageReports: List<AppUsageReportDto>,
    @Json(name = "device_status") @SerialName("device_status") val deviceStatus: DeviceStatusDto,
    @Json(name = "sync_timestamp") @SerialName("sync_timestamp") val syncTimestamp: Long
)

@JsonClass(generateAdapter = true)
data class SyncResponseDto(
    @Json(name = "success") @SerialName("success") val success: Boolean,
    @Json(name = "message") @SerialName("message") val message: String?,
    @Json(name = "server_timestamp") @SerialName("server_timestamp") val serverTimestamp: Long
)
