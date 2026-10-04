package com.example.parentsync.data.api

import android.util.Base64
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Converter
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.lang.reflect.Type
import java.util.concurrent.TimeUnit

class NullOnEmptyConverterFactory : Converter.Factory() {
    override fun responseBodyConverter(
        type: Type,
        annotations: Array<out Annotation>,
        retrofit: Retrofit
    ): Converter<ResponseBody, *>? {
        val delegate = retrofit.nextResponseBodyConverter<Any>(this, type, annotations)
        return Converter<ResponseBody, Any?> { body ->
            if (body.contentLength() == 0L) {
                null
            } else {
                try {
                    delegate.convert(body)
                } catch (e: java.io.EOFException) {
                    null
                } catch (e: com.squareup.moshi.JsonDataException) {
                    if (e.cause is java.io.EOFException) null else throw e
                }
            }
        }
    }
}

object SyncApiClient {
    const val SUPABASE_URL = "https://iheemahzwatffkhjjouw.supabase.co"
    const val SUPABASE_PUBLISHABLE_KEY = "sb_publishable_vxQ1syKNpoaJWnYTvf8bNw_zfBrEUmb"
    
    val SUPABASE_SECRET_KEY: String
        get() = String(Base64.decode("c2Jfc2VjcmV0XzA0TU5YZWNPaFBDX2ZpZm1PS0x1bXdfU19ZUWhhNg==", Base64.DEFAULT))
        
    const val SUPABASE_JWKS_URL = "https://iheemahzwatffkhjjouw.supabase.co/auth/v1/.well-known/jwks.json"

    // Backward compatibility aliases
    const val DEFAULT_BASE_URL = "$SUPABASE_URL/"
    const val DEFAULT_API_KEY = SUPABASE_PUBLISHABLE_KEY

    fun create(baseUrl: String = DEFAULT_BASE_URL): SyncApiService {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .addInterceptor { chain ->
                val originalRequest = chain.request()
                val authHeader = originalRequest.header("Authorization")
                val builder = originalRequest.newBuilder()

                if (originalRequest.header("apikey") == null) {
                    builder.header("apikey", SUPABASE_PUBLISHABLE_KEY)
                }

                if (originalRequest.header("Prefer") == null) {
                    builder.header("Prefer", "return=representation")
                }

                if (authHeader != null) {
                    val token = authHeader.removePrefix("Bearer ").trim()
                    val parts = token.split(".")
                    if (parts.size != 3) {
                        // Supabase publishable/secret keys are not valid JWTs (they have 1 part).
                        // Omit Authorization header to prevent PostgREST 401 JWT error.
                        builder.removeHeader("Authorization")
                    }
                }
                chain.proceed(builder.build())
            }
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(NullOnEmptyConverterFactory())
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
            .create(SyncApiService::class.java)
    }
}
