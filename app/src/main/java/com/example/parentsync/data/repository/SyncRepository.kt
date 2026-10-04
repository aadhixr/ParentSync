package com.example.parentsync.data.repository

import com.example.parentsync.data.model.RemoteCommandDto
import com.example.parentsync.data.model.SyncResponseDto

interface SyncRepository {
    suspend fun syncNow(childId: String, apiKey: String, authToken: String): Result<SyncResponseDto>
    suspend fun queueSyncData(childId: String)
    suspend fun pollCommands(childId: String, apiKey: String, authToken: String): Result<List<RemoteCommandDto>>
    suspend fun executeCommand(command: RemoteCommandDto)
}
