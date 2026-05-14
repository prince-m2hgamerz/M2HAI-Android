package com.m2h.m2haichatbot.data.remote

import android.util.Log
import com.m2h.m2haichatbot.BuildConfig
import io.github.jan.supabase.gotrue.Auth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject

@Serializable
data class ChatMessage(
    val role: String,
    val content: String
)

@Serializable
data class ChatRequest(
    val model: String,
    val messages: List<ChatMessage>,
    val stream: Boolean = true,
    val temperature: Double = 0.5,
    val max_tokens: Int = 1024
)

@Serializable
data class ChatChoice(
    val delta: Delta? = null,
    val message: ChatMessage? = null
)

@Serializable
data class Delta(
    val content: String? = null
)

@Serializable
data class ChatResponse(
    val choices: List<ChatChoice>
)

class NvidiaAIService @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val json: Json,
    private val auth: Auth
) {
    private val TAG = "NvidiaAIService"
    private val supabaseFunctionUrl = BuildConfig.EDGE_CHAT_FUNCTION_URL
    private val supabaseAnonKey = BuildConfig.SUPABASE_ANON_KEY

    fun streamChat(
        messages: List<ChatMessage>,
        modelId: String
    ): Flow<String> = flow {
        // Double check filtering
        val validMessages = messages.filter { it.content.isNotBlank() }
        
        if (validMessages.isEmpty()) {
            throw Exception("Cannot send an empty message history")
        }

        val request = ChatRequest(
            model = modelId,
            messages = validMessages,
            stream = true
        )

        val requestBody = json.encodeToString(ChatRequest.serializer(), request)
            .toRequestBody("application/json".toMediaType())

        val token = auth.currentSessionOrNull()?.accessToken ?: supabaseAnonKey

        Log.d(TAG, "Starting stream request with ${validMessages.size} messages")

        val httpRequest = Request.Builder()
            .url(supabaseFunctionUrl)
            .addHeader("apikey", supabaseAnonKey)
            .addHeader("Authorization", "Bearer $token")
            .post(requestBody)
            .build()

        val response = withContext(Dispatchers.IO) {
            try {
                okHttpClient.newCall(httpRequest).execute()
            } catch (e: Exception) {
                Log.e(TAG, "Network request failed", e)
                throw e
            }
        }

        response.use {
            if (!response.isSuccessful) {
                val errorBody = response.body?.string() ?: "No error body"
                Log.e(TAG, "API call failed: ${response.code} - $errorBody")
                throw Exception("Streaming error (${response.code}): $errorBody")
            }

            // Parse stream as Server-Sent Events or raw JSON chunks.
            // We read line-by-line, but we also tolerate non-JSON lines.
            response.body?.byteStream()?.bufferedReader()?.use { reader ->
                while (true) {
                    val rawLine = withContext(Dispatchers.IO) { reader.readLine() } ?: break

                    val line = rawLine.trim()
                    if (line.isBlank()) continue

                    // Prefer SSE-style: data: <payload>
                    // Strip SSE prefix if present. If not present, treat the whole line as payload.
                    var payload = line
                    if (payload.startsWith("data:")) {
                        payload = payload.removePrefix("data:").trim()
                    }

                    // Some providers send `data: {..}\ndata: {..}`; if we receive a server-side JSON error
                    // payload (e.g. {"error": ...}), surface it as an exception so the UI can display it.
                    if (payload.startsWith("{") && payload.contains("\"error\"")) {
                        Log.e(TAG, "Server returned error payload in stream: ${payload.take(400)}")
                        throw Exception("Streaming server error: $payload")
                    }


                    if (payload.isBlank()) continue
                    if (payload == "[DONE]") break

                    val truncated = if (payload.length > 220) payload.substring(0, 220) + "…" else payload

                    // Most chunks should be JSON objects. If parsing fails, try to salvage JSON inside.
                    // NOTE: keep this non-suspending; `emit(...)` is a suspend function.
                    fun parseContentFromJsonObject(jsonText: String): String? {
                        return try {
                            val chatResponse = json.decodeFromString<ChatResponse>(jsonText)
                            chatResponse.choices.firstOrNull()?.delta?.content
                        } catch (_: Exception) {
                            null
                        }
                    }



                    try {
                        // Some upstreams include multiple SSE frames on one line; split and parse each.
                        val segments = payload.split("data:").map { it.trim() }.filter { it.isNotEmpty() }
                        for (seg in segments) {
                            val direct = parseContentFromJsonObject(seg)
                            if (!direct.isNullOrEmpty()) {
                                emit(direct)
                            } else {
                                // salvage from first {..} within the segment
                                val start = seg.indexOf('{')
                                val end = seg.lastIndexOf('}')
                                if (start != -1 && end != -1 && end > start) {
                                    val candidate = seg.substring(start, end + 1)
                                    val salvaged = parseContentFromJsonObject(candidate)
                                    if (!salvaged.isNullOrEmpty()) emit(salvaged)
                                }
                            }
                        }
                        continue
                    } catch (e: Exception) {
                        // salvage block below
                    }

                    try {
                        val direct = parseContentFromJsonObject(payload)
                        if (!direct.isNullOrEmpty()) {
                            emit(direct)
                            continue
                        }


                        // If the payload wasn't valid JSON (or didn't decode), try salvage.
                        val start = payload.indexOf('{')
                        val end = payload.lastIndexOf('}')
                        if (start != -1 && end != -1 && end > start) {
                            val candidate = payload.substring(start, end + 1)
                            if (candidate != payload) {
                                val salvaged = parseContentFromJsonObject(candidate)
                                if (!salvaged.isNullOrEmpty()) emit(salvaged)
                            }
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Skipping non-JSON/partial chunk. raw='$truncated'", e)
                    }
                }
            }
        }
    }
}
