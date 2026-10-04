package com.example.parentsync.data.repository

import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.util.Log
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

    companion object {
        private const val TAG = "SyncRepositoryImpl"
    }

    override suspend fun syncNow(childId: String, apiKey: String, authToken: String): Result<SyncResponseDto> = withContext(Dispatchers.IO) {
        try {
            val deviceStatus = try {
                gatherDeviceStatus(childId)
            } catch (e: Exception) {
                Log.e(TAG, "Error gathering device status", e)
                return@withContext Result.failure(e)
            }

            val appUsageReports = try {
                gatherAppUsageReports()
            } catch (e: Exception) {
                Log.e(TAG, "Error gathering app usage reports", e)
                emptyList()
            }

            val payload = SyncPayloadDto(
                deviceId = childId,
                batteryLevel = deviceStatus.batteryLevel,
                isCharging = deviceStatus.isCharging,
                appUsageReports = appUsageReports,
                deviceStatus = deviceStatus,
                syncTimestamp = System.currentTimeMillis()
            )

            val response = try {
                apiService.syncDeviceData(
                    apiKey = apiKey,
                    authorization = "Bearer $authToken",
                    payload = payload
                )
            } catch (e: Exception) {
                Log.e(TAG, "Sync failed", e)
                return@withContext Result.failure(e)
            }

            if (response.isSuccessful) {
                val syncResponse = response.body() ?: SyncResponseDto(
                    success = true,
                    message = "Sync successful",
                    serverTimestamp = System.currentTimeMillis()
                )
                Result.success(syncResponse)
            } else {
                val errorMsg = try {
                    response.errorBody()?.string() ?: "Unknown sync error"
                } catch (e: Exception) {
                    "Unknown sync error"
                }
                Log.e(TAG, "Sync failed with code ${response.code()}: $errorMsg")
                Result.failure(Exception("Sync failed: ${response.code()} - $errorMsg"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Critical syncNow exception", e)
            Result.failure(e)
        }
    }

    override suspend fun queueSyncData(childId: String): Unit = withContext(Dispatchers.IO) {
        try {
            val deviceStatus = try { gatherDeviceStatus(childId) } catch (e: Exception) { null }
            val appUsageReports = try { gatherAppUsageReports() } catch (e: Exception) { emptyList() }
            Log.i(TAG, "Queued sync data for child: $childId (status present: ${deviceStatus != null}, reports: ${appUsageReports.size})")
        } catch (e: Exception) {
            Log.e(TAG, "Error queueing sync data", e)
        }
    }

    override suspend fun pollCommands(childId: String, apiKey: String, authToken: String): Result<List<RemoteCommandDto>> = withContext(Dispatchers.IO) {
        try {
            val response = try {
                apiService.fetchCommands(apiKey, "Bearer $authToken", childId)
            } catch (e: Exception) {
                Log.w(TAG, "Primary fetchCommands failed, trying generic endpoint", e)
                try {
                    apiService.genericFetchCommands(apiKey, childId)
                } catch (inner: Exception) {
                    Log.e(TAG, "Generic fetchCommands also failed", inner)
                    null
                }
            }

            if (response != null && response.isSuccessful) {
                val commands = response.body() ?: emptyList()
                Result.success(commands)
            } else {
                Result.success(emptyList())
            }
        } catch (e: Exception) {
            Log.e(TAG, "Critical pollCommands exception", e)
            Result.success(emptyList())
        }
    }

    override suspend fun executeCommand(command: RemoteCommandDto): Unit = withContext(Dispatchers.IO) {
        try {
            val dao = ParentSyncDatabase.getDatabase(context).appUsageLimitDao()
            val receiver = RemoteCommandReceiver(context, dao)
            receiver.receiveCommand(command)
        } catch (e: Exception) {
            Log.e(TAG, "Error executing command", e)
        }
    }

    private fun gatherDeviceStatus(childId: String): DeviceStatusDto {
        val batteryStatus: Intent? = try {
            IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { filter ->
                context.registerReceiver(null, filter)
            }
        } catch (e: Exception) {
            null
        }

        val batteryLevelInt: Int = batteryStatus?.let { intent ->
            val level: Int = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale: Int = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            if (level >= 0 && scale > 0) {
                Math.round((level.toFloat() / scale.toFloat()) * 100).coerceIn(0, 100)
            } else {
                100
            }
        } ?: 100

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

        val deviceLocked = try {
            DeviceStateManager.getInstance(context).isDeviceLocked.value
        } catch (e: Exception) {
            false
        }

        val activeForegroundPkg = getActiveForegroundPackage()
        val installedApps = getInstalledAppsList()
        val isOnline = checkOnlineStatus()

        return DeviceStatusDto(
            deviceId = childId,
            batteryLevel = batteryLevelInt,
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
        return try {
            val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager ?: return null
            val endTime = System.currentTimeMillis()
            val startTime = endTime - 1000 * 60 * 5 // last 5 minutes
            val stats = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_BEST, startTime, endTime)
            if (stats.isNullOrEmpty()) return null
            stats.maxByOrNull { it.lastTimeUsed }?.packageName
        } catch (e: Exception) {
            Log.e(TAG, "Error getting active foreground package", e)
            null
        }
    }

    private fun getInstalledAppsList(): List<InstalledAppDto> {
        return try {
            val pm = context.packageManager
            val packages = pm.getInstalledPackages(0)
            packages.map { pkgInfo ->
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
        } catch (e: Exception) {
            Log.e(TAG, "Error getting installed apps list", e)
            emptyList()
        }
    }

    private suspend fun gatherAppUsageReports(): List<AppUsageReportDto> {
        return try {
            val usages = appUsageRepository.getAppUsageStats().first()
            usages.map { usage ->
                AppUsageReportDto(
                    packageName = usage.packageName,
                    appName = usage.appName,
                    usageTimeMillis = usage.usageTimeMillis,
                    category = usage.category.name,
                    timestamp = usage.lastTimeUsed
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error gathering app usage reports", e)
            emptyList()
        }
    }

    private fun checkOnlineStatus(): Boolean {
        return try {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
            val network = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } catch (e: Exception) {
            Log.e(TAG, "Error checking online status", e)
            false
        }
    }
}
