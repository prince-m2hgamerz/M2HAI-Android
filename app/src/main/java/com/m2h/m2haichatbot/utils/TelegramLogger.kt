package com.m2h.m2haichatbot.utils

import android.os.Build
import android.util.Log
import com.m2h.m2haichatbot.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.*

object TelegramLogger {
    private const val BOT_TOKEN = "8978612291:AAFuCccTj3TTpXyX3htDUKnmGnCQZ6D-7uQ"
    private const val CHAT_ID = "5798029484" 
    private val client = OkHttpClient()
    private val scope = CoroutineScope(Dispatchers.IO)
    private const val TAG = "TelegramLogger"

    fun logError(message: String, throwable: Throwable? = null, screen: String? = null, extra: String? = null) {
        val stackTrace = throwable?.let {
            val sw = StringWriter()
            it.printStackTrace(PrintWriter(sw))
            sw.toString()
        }

        val deviceLine = "📱 *Device:* ${Build.MANUFACTURER} ${Build.MODEL} (Android ${Build.VERSION.RELEASE})"
        val appLine = "📦 *App:* M2HAI ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})"
        val timeLine = "⏰ *Time:* ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}"
        val screenLine = screen?.let { "📺 *Screen:* $it" } ?: ""

        val fullMessage = buildString {
            append("⚠️ *ERROR DETECTED*\n\n")
            append("$timeLine\n")
            append("$deviceLine\n")
            append("$appLine\n")
            if (screenLine.isNotEmpty()) append("$screenLine\n")
            append("\n💬 *Message:* $message\n")
            if (extra != null) {
                append("\n📝 *Extra:* $extra\n")
            }
            if (stackTrace != null) {
                append("\n📂 *Stack Trace:*\n")
                append("```\n${stackTrace.take(3000)}\n```")
            }
        }

        sendToTelegram(fullMessage)
    }

    fun logCrash(throwable: Throwable) {
        val sw = StringWriter()
        throwable.printStackTrace(PrintWriter(sw))
        val stackTrace = sw.toString()

        val fullMessage = buildString {
            append("🚨 *APP CRASHED*\n\n")
            append("⏰ *Time:* ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}\n")
            append("📱 *Device:* ${Build.MANUFACTURER} ${Build.MODEL}\n")
            append("📦 *App Version:* ${BuildConfig.VERSION_NAME}\n\n")
            append("📂 *Stack Trace:*\n")
            append("```\n${stackTrace.take(3000)}\n```")
        }

        // We use a separate thread/blocking call for crashes to ensure it sends before the process dies
        val thread = Thread {
            try {
                val json = JSONObject().apply {
                    put("chat_id", CHAT_ID)
                    put("text", fullMessage)
                    put("parse_mode", "Markdown")
                }
                val body = json.toString().toRequestBody("application/json".toMediaType())
                val request = Request.Builder()
                    .url("https://api.telegram.org/bot$BOT_TOKEN/sendMessage")
                    .post(body)
                    .build()
                client.newCall(request).execute().close()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send crash report", e)
            }
        }
        thread.start()
        thread.join(5000) // Wait for up to 5 seconds
    }

    private fun sendToTelegram(message: String) {
        scope.launch {
            try {
                val json = JSONObject().apply {
                    put("chat_id", CHAT_ID)
                    put("text", message)
                    put("parse_mode", "Markdown")
                }
                val body = json.toString().toRequestBody("application/json".toMediaType())
                val request = Request.Builder()
                    .url("https://api.telegram.org/bot$BOT_TOKEN/sendMessage")
                    .post(body)
                    .build()
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        Log.e(TAG, "Telegram API error: ${response.code} ${response.message}")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send to Telegram", e)
            }
        }
    }
}
