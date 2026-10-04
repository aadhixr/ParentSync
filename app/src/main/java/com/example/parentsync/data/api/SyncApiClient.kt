package com.example.parentsync.data.api

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

object SyncApiClient {
    const val DEFAULT_BASE_URL = "https://iheemahzwatffkhjjouw.supabase.co/"
    const val DEFAULT_API_KEY = "sb_publishable_vxQ1syKNpoaJWnYTvf8bNw_zfBrEUmb"

    fun create(baseUrl: String = DEFAULT_BASE_URL): SyncApiService {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
            .create(SyncApiService::class.java)
    }
}
