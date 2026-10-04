package com.example.parentsync.ui.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ConnectedDevice(
    val id: String,
    val name: String,
    val deviceModel: String,
    val batteryLevel: Int,
    val isOnline: Boolean,
    val lastSyncTime: String,
    val screenTimeTodayMinutes: Long
)

class RemoteDashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val _isCloudSynced = MutableStateFlow(true)
    val isCloudSynced: StateFlow<Boolean> = _isCloudSynced.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _lastSyncTimestamp = MutableStateFlow(
        SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
    )
    val lastSyncTimestamp: StateFlow<String> = _lastSyncTimestamp.asStateFlow()

    private val _connectedDevices = MutableStateFlow(
        listOf(
            ConnectedDevice(
                id = "dev_1",
                name = "Alex's Phone",
                deviceModel = "Pixel 7",
                batteryLevel = 84,
                isOnline = true,
                lastSyncTime = "2 mins ago",
                screenTimeTodayMinutes = 185
            ),
            ConnectedDevice(
                id = "dev_2",
                name = "Emma's Tablet",
                deviceModel = "Galaxy Tab S9",
                batteryLevel = 62,
                isOnline = true,
                lastSyncTime = "5 mins ago",
                screenTimeTodayMinutes = 240
            ),
            ConnectedDevice(
                id = "dev_3",
                name = "Noah's Phone",
                deviceModel = "Moto G Power",
                batteryLevel = 19,
                isOnline = false,
                lastSyncTime = "2 hours ago",
                screenTimeTodayMinutes = 95
            )
        )
    )
    val connectedDevices: StateFlow<List<ConnectedDevice>> = _connectedDevices.asStateFlow()

    private val _bedtimeModeEnabled = MutableStateFlow(false)
    val bedtimeModeEnabled: StateFlow<Boolean> = _bedtimeModeEnabled.asStateFlow()

    private val _instantLockAll = MutableStateFlow(false)
    val instantLockAll: StateFlow<Boolean> = _instantLockAll.asStateFlow()

    fun toggleCloudSync(enabled: Boolean) {
        _isCloudSynced.value = enabled
    }

    fun triggerManualSync() {
        viewModelScope.launch {
            _isSyncing.value = true
            delay(1200) // simulate network sync
            _isSyncing.value = false
            _lastSyncTimestamp.value = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
        }
    }

    fun toggleBedtimeMode(enabled: Boolean) {
        _bedtimeModeEnabled.value = enabled
    }

    fun toggleInstantLockAll(locked: Boolean) {
        _instantLockAll.value = locked
    }
}
