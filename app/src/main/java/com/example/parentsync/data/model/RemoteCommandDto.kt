package com.example.parentsync.data.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class RemoteCommandDto(
    val commandId: String,
    val commandType: String, // "LOCK_DEVICE", "UNLOCK_DEVICE", "UPDATE_QUOTA", "BLOCK_APP", "UNLOCK_APP"
    val packageName: String? = null,
    val quotaMinutes: Int? = null,
    val timestamp: Long = System.currentTimeMillis()
)
