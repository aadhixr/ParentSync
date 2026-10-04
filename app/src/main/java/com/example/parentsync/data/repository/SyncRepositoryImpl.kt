package com.example.parentsync.data.repository

import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import com.example.parentsync.data.api.SyncApiClient
import com.example.parentsync.data.api.SyncApiService
import com.example.parentsync.data.local.DeviceStateManager
import com.example.parentsync.data.local.ParentSyncDatabase
import com.example.parentsync.data.model.AppUsageReportDto
import com.example.parentsync.data.model.DeviceStatusDto
import com.example.parentsync.data.model.InstalledAppDto
import com.example.parentsync.data.model.RemoteCommandDto
import com.example.parentsync.data.model.SyncPayloadDto
import com.example.parentsync.data.model.SyncResponseDto
import com.example.parentsync.service.RemoteCommandReceiver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class SyncRepositoryImpl(
    private val context: Context,
    private val appUsageRepository: AppUsageRepository,
    private val apiService: SyncApiService = SyncApiClient.create()
) : SyncRepository {

    override suspend fun syncNow(childId: String, apiKey: String, authToken: String): Result<SyncResponseDto> = withContext(Dispatchers.IO) {
        try {
            val deviceStatus = gatherDeviceStatus(childId)
            val appUsageReports = gatherAppUsageReports()

            val payload = SyncPayloadDto(
                childId = childId,
                deviceStatus = deviceStatus,
                appUsageReports = appUsageReports,
                syncTimestamp = System.currentTimeMillis()
            )

            val response = try {
                apiService.syncDeviceData(
                    apiKey = apiKey,
                    authorization = "Bearer $authToken",
                    payload = payload
                )
            } catch (e: Exception) {
                apiService.updateDeviceData(
                    apiKey = apiKey,
                    authorization = "Bearer $authToken",
                    payload = payload
                )
            }

            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorMsg = response.errorBody()?.string() ?: "Unknown sync error"
                Result.failure(Exception("Sync failed: ${response.code()} - $errorMsg"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun queueSyncData(childId: String) = withContext(Dispatchers.IO) {
        val deviceStatus = gatherDeviceStatus(childId)
        val appUsageReports = gatherAppUsageReports()
        // Queueing logic for offline storage or local cache if needed
    }

    override suspend fun pollCommands(childId: String, apiKey: String, authToken: String): Result<List<RemoteCommandDto>> = withContext(Dispatchers.IO) {
        try {
            val response = try {
                apiService.fetchCommands(apiKey, "Bearer $authToken", childId)
            } catch (e: Exception) {
                apiService.genericFetchCommands(apiKey, childId)
            }

            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.success(emptyList())
            }
        } catch (e: Exception) {
            Result.success(emptyList())
        }
    }

    override suspend fun executeCommand(command: RemoteCommandDto) = withContext(Dispatchers.IO) {
        val dao = ParentSyncDatabase.getDatabase(context).appUsageLimitDao()
        val receiver = RemoteCommandReceiver(context, dao)
        receiver.receiveCommand(command)
    }

    private fun gatherDeviceStatus(childId: String): DeviceStatusDto {
        val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { filter ->
            context.registerReceiver(null, filter)
        }
        val batteryPct: Float = batteryStatus?.let { intent ->
            val level: Int = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale: Int = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            if (level >= 0 && scale > 0) level / scale.toFloat() else 1.0f
        } ?: 1.0f

        val isCharging: Boolean = batteryStatus?.let { intent ->
            val status: Int = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
        } ?: false

        val batteryHealthCode = batteryStatus?.getIntExtra(BatteryManager.EXTRA_HEALTH, BatteryManager.BATTERY_HEALTH_UNKNOWN) ?: BatteryManager.BATTERY_HEALTH_UNKNOWN
        val batteryHealthStr = when (batteryHealthCode) {
            BatteryManager.BATTERY_HEALTH_GOOD -> "Good"
            BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Overheat"
            BatteryManager.BATTERY_HEALTH_DEAD -> "Dead"
            BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "Over Voltage"
            BatteryManager.BATTERY_HEALTH_COLD -> "Cold"
            else -> "Unknown"
        }

        val deviceLocked = DeviceStateManager.getInstance(context).isDeviceLocked.value
        val activeForegroundPkg = getActiveForegroundPackage()
        val installedApps = getInstalledAppsList()
        val isOnline = checkOnlineStatus()

        return DeviceStatusDto(
            deviceId = childId,
            batteryLevel = batteryPct,
            isCharging = isCharging,
            batteryHealth = batteryHealthStr,
            activeForegroundPackage = activeForegroundPkg,
            deviceLocked = deviceLocked,
            installedApps = installedApps,
            isOnline = isOnline,
            timestamp = System.currentTimeMillis()
        )
    }

    private fun getActiveForegroundPackage(): String? {
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager ?: return null
        val endTime = System.currentTimeMillis()
        val startTime = endTime - 1000 * 60 * 5 // last 5 minutes
        val stats = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_BEST, startTime, endTime)
        if (stats.isNullOrEmpty()) return null
        return stats.maxByOrNull { it.lastTimeUsed }?.packageName
    }

    private fun getInstalledAppsList(): List<InstalledAppDto> {
        val pm = context.packageManager
        val packages = pm.getInstalledPackages(0)
        return packages.map { pkgInfo ->
            val appName = try {
                pkgInfo.applicationInfo?.let { pm.getApplicationLabel(it).toString() } ?: pkgInfo.packageName
            } catch (e: Exception) {
                pkgInfo.packageName
            }
            val isSystem = (pkgInfo.applicationInfo?.flags ?: 0) and android.content.pm.ApplicationInfo.FLAG_SYSTEM != 0
            InstalledAppDto(
                packageName = pkgInfo.packageName,
                appName = appName,
                isSystemApp = isSystem
            )
        }
    }

    private suspend fun gatherAppUsageReports(): List<AppUsageReportDto> {
        val usages = appUsageRepository.getAppUsageStats().first()
        return usages.map { usage ->
            AppUsageReportDto(
                packageName = usage.packageName,
                appName = usage.appName,
                usageTimeMillis = usage.usageTimeMillis,
                category = usage.category.name,
                timestamp = usage.lastTimeUsed
            )
        }
    }

    private fun checkOnlineStatus(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
