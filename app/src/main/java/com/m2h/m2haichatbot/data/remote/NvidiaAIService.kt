package com.m2h.m2haichatbot.data.remote

import android.util.Log
import com.m2h.m2haichatbot.BuildConfig
import io.github.jan.supabase.gotrue.Auth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
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
    val max_tokens: Int = 1024,
    val system_prompt: String? = null
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
        modelId: String,
        systemPrompt: String = "",
        temperature: Double = 0.5,
        maxTokens: Int = 1024
    ): Flow<String> = flow {
        val imageModel = isImageModel(modelId)
        val validMessages = messages.filter { it.content.isNotBlank() }
        
        if (validMessages.isEmpty()) {
            throw Exception("Cannot send an empty message history")
        }

        val sanitizedMessages = sanitizeChatMessages(validMessages)
        if (sanitizedMessages.isEmpty()) {
            throw Exception("Cannot send an empty message history")
        }

        val request = ChatRequest(
            model = modelId,
            messages = sanitizedMessages,
            stream = !imageModel,
            temperature = temperature,
            max_tokens = maxTokens,
            system_prompt = systemPrompt.ifBlank { null }
        )

        val requestBody = json.encodeToString(ChatRequest.serializer(), request)
            .toRequestBody("application/json".toMediaType())

        val token = auth.currentSessionOrNull()?.accessToken ?: supabaseAnonKey
        
        // Prefer user-provided keys; BuildConfig fallbacks support local testing with the checked .env.
        val geminiKey = preferenceRepository.geminiApiKey.first().ifBlank { BuildConfig.GEMINI_API_KEY }
        val openaiKey = preferenceRepository.openaiApiKey.first()
        val groqKey = preferenceRepository.groqApiKey.first()
        val nvidiaKey = BuildConfig.NVIDIA_API_KEY

        Log.d(TAG, "Starting stream request with ${validMessages.size} messages for model $modelId")

        val httpRequestBuilder = Request.Builder()
            .url(supabaseFunctionUrl)
            .addHeader("apikey", supabaseAnonKey)
            .addHeader("Authorization", "Bearer $token")
            .post(requestBody)

        if (geminiKey.isNotBlank()) httpRequestBuilder.addHeader("x-gemini-api-key", geminiKey)
        if (openaiKey.isNotBlank()) httpRequestBuilder.addHeader("x-openai-api-key", openaiKey)
        if (groqKey.isNotBlank()) httpRequestBuilder.addHeader("x-groq-api-key", groqKey)
        if (nvidiaKey.isNotBlank()) httpRequestBuilder.addHeader("x-nvidia-api-key", nvidiaKey)

        val httpRequest = httpRequestBuilder.build()

        val response = withContext(Dispatchers.IO) {
            try {
                okHttpClient.newCall(httpRequest).execute()
            } catch (e: CancellationException) {
                throw e
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

            if (imageModel || !request.stream) {
                val body = response.body?.string() ?: ""
                val content = parseNonStreamingContent(body)
                if (content.isBlank()) throw Exception("Provider returned an empty response.")
                emit(content)
                return@use
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
                        
                        parseTopLevelServerError(segment)?.let { message ->
                            throw Exception("Server error: $message")
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

    private fun parseTopLevelServerError(segment: String): String? {
        return try {
            val root = json.parseToJsonElement(segment).jsonObject
            val errorElement = root["error"] ?: return null
            if (errorElement is JsonObject) {
                errorElement["message"]?.jsonPrimitive?.contentOrNull
                    ?: errorElement["code"]?.jsonPrimitive?.contentOrNull
                    ?: errorElement.toString()
            } else {
                errorElement.jsonPrimitive.contentOrNull ?: errorElement.toString()
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun sanitizeChatMessages(messages: List<ChatMessage>): List<ChatMessage> {
        val normalized = mutableListOf<ChatMessage>()
        val chatMessages = messages
            .filter { it.role != "system" }
            .dropWhile { it.role != "user" }

        for (message in chatMessages) {
            val role = when (message.role) {
                "assistant" -> "assistant"
                else -> "user"
            }
            val cleaned = message.copy(role = role, content = message.content.trim())
            val previous = normalized.lastOrNull()
            if (previous?.role == cleaned.role) {
                if (cleaned.role == "user") {
                    normalized[normalized.lastIndex] = previous.copy(
                        content = listOf(previous.content, cleaned.content).joinToString("\n\n")
                    )
                }
            } else {
                normalized.add(cleaned)
            }
        }

        return normalized.filter { it.content.isNotBlank() }
    }

    private fun isImageModel(modelId: String): Boolean {
        val lower = modelId.lowercase()
        return lower.contains("flux") ||
            lower.contains("image") ||
            lower.contains("diffusion") ||
            lower.contains("sdxl")
    }

    private fun parseNonStreamingContent(body: String): String {
        if (body.isBlank()) return ""

        runCatching {
            val chatResponse = json.decodeFromString<ChatResponse>(body)
            val choice = chatResponse.choices.firstOrNull()
            val content = choice?.message?.content ?: choice?.delta?.content
            if (!content.isNullOrBlank()) return content
        }

        findImageReference(body)?.let { image ->
            return "![Generated image]($image)"
        }

        val urlRegex = """"url"\s*:\s*"([^"]+)"""".toRegex()
        urlRegex.find(body)?.groupValues?.getOrNull(1)?.let { url ->
            return "![Generated image]($url)"
        }

        val base64Regex = """"(?:base64|b64_json)"\s*:\s*"([^"]+)"""".toRegex()
        base64Regex.find(body)?.groupValues?.getOrNull(1)?.let { data ->
            return "![Generated image](data:image/png;base64,$data)"
        }

        return body
    }

    private fun findImageReference(body: String): String? {
        return runCatching { findImageReference(json.parseToJsonElement(body)) }.getOrNull()
    }

    private fun findImageReference(element: kotlinx.serialization.json.JsonElement?): String? {
        if (element == null) return null
        return when (element) {
            is kotlinx.serialization.json.JsonPrimitive -> {
                val value = element.contentOrNull?.trim().orEmpty()
                when {
                    value.startsWith("http://") || value.startsWith("https://") || value.startsWith("data:image/") -> value
                    value.length > 256 && value.matches(Regex("^[A-Za-z0-9+/=_-]+$")) -> "data:image/png;base64,$value"
                    else -> null
                }
            }
            is kotlinx.serialization.json.JsonObject -> {
                val likelyKeys = listOf(
                    "url",
                    "image_url",
                    "base64",
                    "b64_json",
                    "image",
                    "artifact",
                    "artifacts",
                    "data",
                    "output"
                )
                for (key in likelyKeys) {
                    findImageReference(element[key])?.let { return it }
                }
                element.values.firstNotNullOfOrNull { findImageReference(it) }
            }
            is kotlinx.serialization.json.JsonArray -> element.jsonArray.firstNotNullOfOrNull { findImageReference(it) }
            else -> null
        }
    }
}
