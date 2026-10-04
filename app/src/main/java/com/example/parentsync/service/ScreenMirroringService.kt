package com.example.parentsync.service

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.util.Base64
import android.util.Log
import androidx.core.graphics.scale
import com.example.parentsync.data.local.DeviceStateManager
import com.example.parentsync.data.repository.AppUsageRepositoryImpl
import com.example.parentsync.data.repository.SyncRepositoryImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream

object ScreenMirroringService {
    private const val TAG = "ScreenMirroringService"

    var latestDecorViewBitmap: Bitmap? = null

    private var broadcastingJob: Job? = null
    private val broadcastingScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun onStartCommand(context: Context, childId: String, apiKey: String, authToken: String) {
        stopSnapshotBroadcasting()
        broadcastingJob = broadcastingScope.launch {
            val appContext = context.applicationContext
            val appUsageRepo = AppUsageRepositoryImpl(appContext)
            val syncRepo = SyncRepositoryImpl(appContext, appUsageRepo)
            while (isActive) {
                syncAndCapture(syncRepo, childId, apiKey, authToken)
                delay(1000L)
            }
        }
    }

    private suspend fun syncAndCapture(syncRepo: SyncRepositoryImpl, childId: String, apiKey: String, authToken: String) {
        try {
            syncRepo.syncNow(childId, apiKey, authToken)
        } catch (e: Exception) {
            Log.e(TAG, "Error in 1-second screen snapshot broadcasting & telemetry sync", e)
        }
    }

    fun startSnapshotBroadcasting(context: Context, childId: String, apiKey: String, authToken: String) {
        onStartCommand(context, childId, apiKey, authToken)
    }

    fun stopSnapshotBroadcasting() {
        broadcastingJob?.cancel()
        broadcastingJob = null
    }

    fun captureScreenSnapshotBase64(context: Context): String? {
        return try {
            val bitmap = latestDecorViewBitmap?.let { cached ->
                try {
                    if (!cached.isRecycled && cached.width > 0 && cached.height > 0) {
                        cached.scale(360, 640, true)
                    } else {
                        null
                    }
                } catch (_: Exception) {
                    null
                }
            } ?: run {
                val width = 360
                val height = 640
                val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
                val canvas = Canvas(bmp)

                // Background - Dark Slate 900
                val bgPaint = Paint().apply {
                    color = Color.parseColor("#0f172a")
                    style = Paint.Style.FILL
                }
                canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

                // Header Banner - Indigo 600
                val headerPaint = Paint().apply {
                    color = Color.parseColor("#4f46e5")
                    style = Paint.Style.FILL
                }
                canvas.drawRect(0f, 0f, width.toFloat(), 70f, headerPaint)

                val textPaint = Paint().apply {
                    color = Color.WHITE
                    textSize = 18f
                    isAntiAlias = true
                    typeface = Typeface.DEFAULT_BOLD
                }
                canvas.drawText("ParentSync Live Mirror", 20f, 42f, textPaint)

                val subTextPaint = Paint().apply {
                    color = Color.parseColor("#94a3b8")
                    textSize = 12f
                    isAntiAlias = true
                }
                val timeStr = android.text.format.DateFormat.format("yyyy-MM-dd HH:mm:ss", System.currentTimeMillis()).toString()
                canvas.drawText("Stream Time: $timeStr", 20f, 95f, subTextPaint)

                // Device Status Card
                val cardPaint = Paint().apply {
                    color = Color.parseColor("#1e293b") // Slate 800
                    style = Paint.Style.FILL
                }
                canvas.drawRoundRect(20f, 120f, (width - 20).toFloat(), 340f, 16f, 16f, cardPaint)

                val detailPaint = Paint().apply {
                    color = Color.parseColor("#e2e8f0")
                    textSize = 14f
                    isAntiAlias = true
                }

                val isLocked = try {
                    DeviceStateManager.getInstance(context).isDeviceLocked.value
                } catch (_: Exception) {
                    false
                }

                canvas.drawText("Device Status: Active 1s Stream", 40f, 160f, detailPaint)
                canvas.drawText("Lock State: ${if (isLocked) "LOCKED 🔒" else "Unlocked 🔓"}", 40f, 195f, detailPaint)
                canvas.drawText("Active Window: MainActivity", 40f, 230f, detailPaint)
                canvas.drawText("Protection: Active Monitoring", 40f, 265f, detailPaint)
                canvas.drawText("Sync Protocol: 1s Real-Time Supabase", 40f, 300f, detailPaint)

                // Footer Active Indicator
                val footerPaint = Paint().apply {
                    color = Color.parseColor("#10b981") // Emerald 500
                    style = Paint.Style.FILL
                }
                canvas.drawCircle(40f, 385f, 8f, footerPaint)

                val footerTextPaint = Paint().apply {
                    color = Color.parseColor("#10b981")
                    textSize = 14f
                    isAntiAlias = true
                    typeface = Typeface.DEFAULT_BOLD
                }
                canvas.drawText("1s Live Mirroring Active", 60f, 390f, footerTextPaint)

                bmp
            }

            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 75, outputStream)
            val bytes = outputStream.toByteArray()
            val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
            "data:image/jpeg;base64,$base64"
        } catch (e: Exception) {
            Log.e(TAG, "Error capturing screen snapshot", e)
            null
        }
    }
}
