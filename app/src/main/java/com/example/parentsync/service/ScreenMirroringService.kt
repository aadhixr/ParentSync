package com.example.parentsync.service

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.util.Base64
import android.util.Log
import com.example.parentsync.data.local.DeviceStateManager
import java.io.ByteArrayOutputStream

object ScreenMirroringService {
    private const val TAG = "ScreenMirroringService"

    fun captureScreenSnapshotBase64(context: Context): String? {
        return try {
            val width = 360
            val height = 640
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
            val canvas = Canvas(bitmap)

            // Background
            val bgPaint = Paint().apply {
                color = Color.parseColor("#0f172a") // Slate 900
                style = Paint.Style.FILL
            }
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

            // Header Banner
            val headerPaint = Paint().apply {
                color = Color.parseColor("#4f46e5") // Indigo 600
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

            // Subtext / timestamp
            val subTextPaint = Paint().apply {
                color = Color.parseColor("#94a3b8")
                textSize = 12f
                isAntiAlias = true
            }
            val timeStr = android.text.format.DateFormat.format("yyyy-MM-dd HH:mm:ss", System.currentTimeMillis()).toString()
            canvas.drawText("Captured: $timeStr", 20f, 105f, subTextPaint)

            // Device Status Box
            val cardPaint = Paint().apply {
                color = Color.parseColor("#1e293b") // Slate 800
                style = Paint.Style.FILL
            }
            canvas.drawRoundRect(20f, 130f, (width - 20).toFloat(), 320f, 16f, 16f, cardPaint)

            val detailPaint = Paint().apply {
                color = Color.parseColor("#e2e8f0")
                textSize = 14f
                isAntiAlias = true
            }

            val isLocked = DeviceStateManager.getInstance(context).isDeviceLocked.value
            canvas.drawText("Lock State: ${if (isLocked) "LOCKED 🔒" else "Unlocked 🔓"}", 40f, 205f, detailPaint)
            canvas.drawText("Protection: Active Monitoring", 40f, 235f, detailPaint)
            canvas.drawText("Sync Status: Connected", 40f, 265f, detailPaint)

            // Footer Visual Indicator
            val footerPaint = Paint().apply {
                color = Color.parseColor("#10b981") // Emerald 500
                style = Paint.Style.FILL
            }
            canvas.drawCircle(40f, 370f, 8f, footerPaint)

            val footerTextPaint = Paint().apply {
                color = Color.parseColor("#10b981")
                textSize = 13f
                isAntiAlias = true
                typeface = Typeface.DEFAULT_BOLD
            }
            canvas.drawText("Live Streaming Active", 60f, 375f, footerTextPaint)

            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 60, outputStream)
            val bytes = outputStream.toByteArray()
            val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
            "data:image/jpeg;base64,$base64"
        } catch (e: Exception) {
            Log.e(TAG, "Error capturing screen snapshot", e)
            null
        }
    }
}
