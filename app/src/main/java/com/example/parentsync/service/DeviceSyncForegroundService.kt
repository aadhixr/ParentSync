package com.example.parentsync.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.parentsync.MainActivity
import com.example.parentsync.data.local.ParentSyncDatabase
import com.example.parentsync.data.repository.AppUsageRepositoryImpl
import com.example.parentsync.data.repository.SyncRepositoryImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

class DeviceSyncForegroundService : Service() {

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)

    private lateinit var syncRepository: SyncRepositoryImpl
    private var commandPoller: RemoteCommandPoller? = null

    companion object {
        const val CHANNEL_ID = "ParentSyncServiceChannel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_START_SYNC = "ACTION_START_SYNC"
        const val ACTION_STOP_SYNC = "ACTION_STOP_SYNC"

        const val EXTRA_CHILD_ID = "EXTRA_CHILD_ID"
        const val EXTRA_API_KEY = "EXTRA_API_KEY"
        const val EXTRA_AUTH_TOKEN = "EXTRA_AUTH_TOKEN"

        fun startService(context: Context, childId: String, apiKey: String, authToken: String) {
            val intent = Intent(context, DeviceSyncForegroundService::class.java).apply {
                action = ACTION_START_SYNC
                putExtra(EXTRA_CHILD_ID, childId)
                putExtra(EXTRA_API_KEY, apiKey)
                putExtra(EXTRA_AUTH_TOKEN, authToken)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, DeviceSyncForegroundService::class.java).apply {
                action = ACTION_STOP_SYNC
            }
            context.startService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        val appUsageRepository = AppUsageRepositoryImpl(applicationContext)
        syncRepository = SyncRepositoryImpl(applicationContext, appUsageRepository)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        if (action == ACTION_STOP_SYNC) {
            commandPoller?.stopPolling()
            ScreenMirroringService.stopSnapshotBroadcasting()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }

        val childId = intent?.getStringExtra(EXTRA_CHILD_ID) ?: "default_child_device"
        val apiKey = intent?.getStringExtra(EXTRA_API_KEY) ?: com.example.parentsync.data.api.SyncApiClient.SUPABASE_PUBLISHABLE_KEY
        val authToken = intent?.getStringExtra(EXTRA_AUTH_TOKEN) ?: com.example.parentsync.data.api.SyncApiClient.SUPABASE_SECRET_KEY

        val notification = createNotification()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        }

        serviceScope.launch {
            while (isActive) {
                syncAndCapture(childId, apiKey, authToken)
                delay(1000L)
            }
        }
        ScreenMirroringService.onStartCommand(applicationContext, childId, apiKey, authToken)

        val appUsageLimitDao = ParentSyncDatabase.getDatabase(applicationContext).appUsageLimitDao()
        commandPoller?.stopPolling()
        commandPoller = RemoteCommandPoller(applicationContext, syncRepository, appUsageLimitDao, childId, apiKey, authToken)
        commandPoller?.startPolling(30)

        return START_STICKY
    }

    private suspend fun syncAndCapture(childId: String, apiKey: String, authToken: String) {
        try {
            val result = syncRepository.syncNow(childId, apiKey, authToken)
            if (result.isFailure) {
                syncRepository.queueSyncData(childId)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun createNotificationChannel() {
        val serviceChannel = NotificationChannel(
            CHANNEL_ID,
            "ParentSync Background Service Channel",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Keeps ParentSync running in the background to monitor screen time and sync safety reports."
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager?.createNotificationChannel(serviceChannel)
    }

    private fun createNotification(): Notification {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("ParentSync Child Protection")
            .setContentText("ParentSync is actively syncing device status and usage.")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        commandPoller?.stopPolling()
        ScreenMirroringService.stopSnapshotBroadcasting()
        serviceJob.cancel()
    }
}
