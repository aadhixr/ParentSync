package com.example.parentsync.service

import android.content.Context
import com.example.parentsync.data.local.AppUsageLimitDao
import com.example.parentsync.data.repository.SyncRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

class RemoteCommandPoller(
    private val context: Context,
    private val syncRepository: SyncRepository,
    private val appUsageLimitDao: AppUsageLimitDao,
    private val childId: String,
    private val apiKey: String,
    private val authToken: String
) {
    private val pollerJob = Job()
    private val pollerScope = CoroutineScope(Dispatchers.IO + pollerJob)
    private val receiver = RemoteCommandReceiver(context, appUsageLimitDao)

    fun startPolling(intervalSeconds: Long = 30) {
        pollerScope.launch {
            while (isActive) {
                try {
                    val result = syncRepository.pollCommands(childId, apiKey, authToken)
                    if (result.isSuccess) {
                        val commands = result.getOrNull() ?: emptyList()
                        if (commands.isNotEmpty()) {
                            receiver.receiveCommands(commands)
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                delay(intervalSeconds.seconds)
            }
        }
    }

    fun stopPolling() {
        pollerJob.cancel()
    }
}
