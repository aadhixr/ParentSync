package com.example.parentsync.data.local

import android.content.Context
import android.content.Context.MODE_PRIVATE
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class DeviceStateManager(context: Context) {
    private val prefs = context.getSharedPreferences("parent_sync_device_state", MODE_PRIVATE)

    private val _isDeviceLocked = MutableStateFlow(prefs.getBoolean("is_device_locked", false))
    val isDeviceLocked: StateFlow<Boolean> = _isDeviceLocked.asStateFlow()

    private val _bedtimeMode = MutableStateFlow(prefs.getBoolean("bedtime_mode", false))
    val bedtimeMode: StateFlow<Boolean> = _bedtimeMode.asStateFlow()

    fun setDeviceLocked(locked: Boolean) {
        prefs.edit().putBoolean("is_device_locked", locked).apply()
        _isDeviceLocked.value = locked
    }

    fun setBedtimeMode(enabled: Boolean) {
        prefs.edit().putBoolean("bedtime_mode", enabled).apply()
        _bedtimeMode.value = enabled
    }

    companion object {
        @Volatile
        private var INSTANCE: DeviceStateManager? = null

        fun getInstance(context: Context): DeviceStateManager {
            return INSTANCE ?: synchronized(this) {
                val instance = DeviceStateManager(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}
