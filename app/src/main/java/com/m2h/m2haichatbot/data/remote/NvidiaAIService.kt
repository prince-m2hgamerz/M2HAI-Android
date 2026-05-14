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
                
                // If it's a 404, might be the function name or URL
                // If it's a 500, it's our Edge Function logic
                throw Exception("Streaming error (${response.code}): $errorBody")
            }

            response.body?.byteStream()?.bufferedReader()?.use { reader ->
                while (true) {
                    val line = withContext(Dispatchers.IO) { 
                        try {
                            reader.readLine() 
                        } catch (e: Exception) {
                            null
                        }
                    } ?: break
                    
                    if (line.startsWith("data: ")) {
                        val data = line.removePrefix("data: ")
                        if (data == "[DONE]") break
                        
                        try {
                            val chatResponse = json.decodeFromString<ChatResponse>(data)
                            chatResponse.choices.firstOrNull()?.delta?.content?.let { content ->
                                emit(content)
                            }
                        } catch (e: Exception) {
                            // Some providers send non-JSON data lines, ignore them
                        }
                    }
                }
            }
        }
    }
}
