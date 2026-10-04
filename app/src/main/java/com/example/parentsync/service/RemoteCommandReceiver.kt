package com.example.parentsync.service

import android.app.ActivityManager
import android.content.Context
import com.example.parentsync.data.local.AppUsageLimitDao
import com.example.parentsync.data.local.DeviceStateManager
import com.example.parentsync.data.model.RemoteCommandDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class RemoteCommandReceiver(
    private val context: Context,
    private val appUsageLimitDao: AppUsageLimitDao,
    private val deviceStateManager: DeviceStateManager = DeviceStateManager.getInstance(context)
) {

    suspend fun receiveCommand(command: RemoteCommandDto) = withContext(Dispatchers.IO) {
        when (command.commandType.uppercase()) {
            "LOCK_DEVICE" -> {
                deviceStateManager.setDeviceLocked(true)
            }
            "UNLOCK_DEVICE" -> {
                deviceStateManager.setDeviceLocked(false)
            }
            "UPDATE_QUOTA" -> {
                val pkg = command.packageName
                val quota = command.quotaMinutes
                if (pkg != null && quota != null) {
                    appUsageLimitDao.updateQuota(pkg, quota)
                }
            }
            "BLOCK_APP" -> {
                val pkg = command.packageName
                if (pkg != null) {
                    appUsageLimitDao.updateBlockStatus(pkg, true)
                }
            }
            "UNLOCK_APP" -> {
                val pkg = command.packageName
                if (pkg != null) {
                    appUsageLimitDao.updateBlockStatus(pkg, false)
                }
            }
            "FORCE_CLOSE_APP" -> {
                val pkg = command.packageName
                if (pkg != null) {
                    try {
                        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
                        activityManager?.killBackgroundProcesses(pkg)
                        appUsageLimitDao.updateBlockStatus(pkg, true)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
            else -> {
                // Unknown command type, ignore or log
            }
        }
    }

    suspend fun receiveCommands(commands: List<RemoteCommandDto>) {
        for (cmd in commands) {
            receiveCommand(cmd)
        }
    }
}
