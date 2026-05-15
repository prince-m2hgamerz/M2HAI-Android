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
import kotlinx.coroutines.flow.first

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
    private val auth: Auth,
    private val preferenceRepository: com.m2h.m2haichatbot.data.repository.PreferenceRepository
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

        // Ensure roles strictly alternate: user, assistant, user, assistant...
        val sanitizedMessages = mutableListOf<ChatMessage>()
        var lastRole: String? = null
        
        validMessages.forEach { msg ->
            if (msg.role != lastRole) {
                sanitizedMessages.add(msg)
                lastRole = msg.role
            } else if (msg.role == "user") {
                // Merge consecutive user messages
                val lastMsg = sanitizedMessages.removeAt(sanitizedMessages.size - 1)
                sanitizedMessages.add(lastMsg.copy(content = lastMsg.content + "\n" + msg.content))
            }
            // Skip consecutive assistant messages (usually shouldn't happen)
        }

        val request = ChatRequest(
            model = modelId,
            messages = sanitizedMessages,
            stream = true
        )

        val requestBody = json.encodeToString(ChatRequest.serializer(), request)
            .toRequestBody("application/json".toMediaType())

        val token = auth.currentSessionOrNull()?.accessToken ?: supabaseAnonKey
        
        // Use user-provided keys from settings, or fallback to system keys from BuildConfig
        val geminiKey = preferenceRepository.geminiApiKey.first().ifBlank { BuildConfig.GEMINI_API_KEY }
        val openaiKey = preferenceRepository.openaiApiKey.first().ifBlank { BuildConfig.NVIDIA_API_KEY } // Fallback to NVIDIA for OpenAI-compatible
        val groqKey = preferenceRepository.groqApiKey.first().ifBlank { BuildConfig.NVIDIA_API_KEY } // Fallback to NVIDIA for Groq
        val nvidiaKey = BuildConfig.NVIDIA_API_KEY

        Log.d(TAG, "Starting stream request with ${validMessages.size} messages for model $modelId")

        val httpRequest = Request.Builder()
            .url(supabaseFunctionUrl)
            .addHeader("apikey", supabaseAnonKey)
            .addHeader("Authorization", "Bearer $token")
            .addHeader("x-gemini-api-key", geminiKey)
            .addHeader("x-openai-api-key", openaiKey)
            .addHeader("x-groq-api-key", groqKey)
            .addHeader("x-nvidia-api-key", nvidiaKey)
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
                
                if (errorBody.contains("leaked", ignoreCase = true)) {
                    throw Exception("System API Key has been leaked. Please add your own API keys in Settings > Custom Providers for a better experience.")
                }
                
                throw Exception("API error (${response.code}): $errorBody")
            }

            // Handle Non-Streaming (Image Generation or small responses)
            val isImageModel = modelId.contains("sdxl", true) || modelId.contains("diffusion", true)
            
            if (isImageModel || !request.stream) {
                val body = response.body?.string() ?: ""
                if (body.contains("\"url\"")) {
                    val urlStart = body.indexOf("\"url\":\"") + 7
                    val urlEnd = body.indexOf("\"", urlStart)
                    if (urlStart > 6 && urlEnd > urlStart) {
                        val url = body.substring(urlStart, urlEnd)
                        emit("![Generated Image]($url)")
                        return@use
                    }
                }
            }

            // Standard Streaming (Chat)
            response.body?.byteStream()?.bufferedReader()?.use { reader ->
                while (true) {
                    val rawLine = withContext(Dispatchers.IO) { reader.readLine() } ?: break
                    val line = rawLine.trim()
                    if (line.isBlank()) continue
                    
                    // Handle multiple "data: " segments in a single line
                    val segments = line.split("data:").map { it.trim() }.filter { it.isNotEmpty() }
                    
                    for (segment in segments) {
                        if (segment == "[DONE]") break
                        
                        if (segment.startsWith("{") && segment.contains("\"error\"")) {
                            throw Exception("Server error: $segment")
                        }

                        try {
                            val chatResponse = json.decodeFromString<ChatResponse>(segment)
                            chatResponse.choices.firstOrNull()?.let { choice ->
                                choice.delta?.content?.let { emit(it) }
                                choice.message?.content?.let { emit(it) }
                            }
                        } catch (e: Exception) {
                            // Salvage logic for partial/malformed JSON
                            val start = segment.indexOf('{')
                            val end = segment.lastIndexOf('}')
                            if (start != -1 && end != -1 && end > start) {
                                try {
                                    val candidate = segment.substring(start, end + 1)
                                    val chatResponse = json.decodeFromString<ChatResponse>(candidate)
                                    chatResponse.choices.firstOrNull()?.delta?.content?.let { emit(it) }
                                } catch (_: Exception) {}
                            }
                        }
                    }
                }
            }
        }
    }
}
