package com.example.parentsync.data.api

import com.example.parentsync.data.model.RemoteCommandDto
import com.example.parentsync.data.model.SyncPayloadDto
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Query

interface SyncApiService {
    @Headers("Prefer: return=minimal")
    @POST("rest/v1/device_reports") // Supabase REST API endpoint for timeline insert device reports
    suspend fun syncDeviceData(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Header("Prefer") prefer: String = "return=minimal",
        @Body payload: SyncPayloadDto
    ): Response<ResponseBody>

    @Headers("Prefer: return=minimal")
    @POST("api/v1/sync") // Generic REST fallback endpoint
    suspend fun genericSync(
        @Header("X-API-Key") apiKey: String,
        @Header("Prefer") prefer: String = "return=minimal",
        @Body payload: SyncPayloadDto
    ): Response<ResponseBody>

    @GET("rest/v1/remote_commands")
    suspend fun fetchCommands(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Query("childId") childId: String
    ): Response<List<RemoteCommandDto>>

    @GET("api/v1/commands")
    suspend fun genericFetchCommands(
        @Header("X-API-Key") apiKey: String,
        @Query("childId") childId: String
    ): Response<List<RemoteCommandDto>>
}
