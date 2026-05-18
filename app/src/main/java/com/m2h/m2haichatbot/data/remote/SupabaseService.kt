package com.m2h.m2haichatbot.data.remote

import android.util.Log
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.builtin.Email
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import io.github.jan.supabase.functions.functions
import io.ktor.client.call.body
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpMethod
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import javax.inject.Inject

@Serializable
data class ProfileDto(
    val id: String,
    val email: String,
    val full_name: String? = null,
    val avatar_url: String? = null,
    val is_disabled: Boolean = false,
    val admin_notes: String? = null
)

@Serializable
data class ChatDto(
    val id: String,
    val user_id: String,
    val title: String,
    val model_id: String,
    val is_archived: Boolean,
    val is_pinned: Boolean,
    val created_at: String,
    val updated_at: String
)

@Serializable
data class MessageDto(
    val id: String,
    val chat_id: String,
    val role: String,
    val content: String,
    val attachments: List<String> = emptyList(),
    val created_at: String
)

@Serializable
data class AIModelDto(
    val id: String,
    val name: String,
    val provider: String,
    val description: String? = null,
    val is_free: Boolean = true,
    val is_active: Boolean = true
)

@Serializable
data class AppSettingsDto(
    val id: String = "global",
    val app_enabled: Boolean = true,
    val signup_enabled: Boolean = true,
    val maintenance_message: String = "M2HAI is temporarily unavailable. Please try again later.",
    val global_announcement: String = "",
    val default_model_id: String = "meta/llama-3.1-8b-instruct",
    val image_model_id: String = "black-forest-labs/flux.1-schnell",
    val system_prompt: String = "",
    val temperature: Double = 0.5,
    val max_tokens: Int = 1024,
    val latest_version_code: Int = 1,
    val latest_version_name: String = "1.0",
    val min_supported_version_code: Int = 1,
    val update_required: Boolean = false,
    val update_title: String = "Update available",
    val update_message: String = "",
    val update_apk_url: String = "",
    val update_release_notes: String = "",
    val update_apk_size_mb: Double = 0.0,
    val update_sha256: String = "",
    val update_published_at: String = "",
    val update_channel: String = "stable",
    val updated_at: String? = null
)

@Serializable
data class CreateChatRequest(
    val user_id: String,
    val title: String,
    val model_id: String
)

@Serializable
data class CreateMessageRequest(
    val chat_id: String,
    val role: String,
    val content: String,
    val attachments: List<String> = emptyList()
)

@Serializable
data class MessageFeedbackRequest(
    val message_id: String,
    val chat_id: String,
    val user_id: String,
    val action: String,
    val model_id: String? = null
)

class SupabaseService @Inject constructor(
    private val supabase: SupabaseClient
) {
    private val TAG = "SupabaseService"

    suspend fun signUp(email: String, password: String): ProfileDto {
        try {
            supabase.auth.signUpWith(Email) {
                this.email = email
                this.password = password
            }
            val user = supabase.auth.currentUserOrNull()
                ?: throw Exception("Failed to create user: Auth session null")
            
            val profile = ProfileDto(
                id = user.id,
                email = email
            )
            
            // Attempt to create profile manually (in case trigger isn't set up)
            try {
                supabase.from("profiles").upsert(profile)
            } catch (e: Exception) {
                Log.w(TAG, "Profile creation via upsert failed/skipped (might already exist via trigger): ${e.message}")
            }
            
            return profile
        } catch (e: Exception) {
            Log.e(TAG, "Error in signUp", e)
            throw e
        }
    }

    suspend fun signIn(email: String, password: String): ProfileDto {
        try {
            supabase.auth.signInWith(Email) {
                this.email = email
                this.password = password
            }
            val user = supabase.auth.currentUserOrNull()
                ?: throw Exception("Failed to sign in: Auth session null")
            
            return getProfile(user.id) ?: throw Exception("Profile not found")
        } catch (e: Exception) {
            Log.e(TAG, "Error in signIn", e)
            throw e
        }
    }

    suspend fun getProfile(userId: String): ProfileDto? {
        return try {
            supabase.from("profiles")
                .select(columns = Columns.ALL) {
                    filter { eq("id", userId) }
                }
                .decodeSingleOrNull<ProfileDto>()
        } catch (e: Exception) {
            Log.e(TAG, "Error in getProfile", e)
            null
        }
    }

    suspend fun updateProfile(userId: String, fullName: String?, avatarUrl: String?) {
        try {
            val updates = mutableMapOf<String, String>()
            fullName?.let { updates["full_name"] = it }
            avatarUrl?.let { updates["avatar_url"] = it }
            
            if (updates.isNotEmpty()) {
                supabase.from("profiles")
                    .update(updates) {
                        filter { eq("id", userId) }
                    }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in updateProfile", e)
        }
    }

    suspend fun signOut() {
        try {
            supabase.auth.signOut()
        } catch (e: Exception) {
            Log.e(TAG, "Error in signOut", e)
        }
    }

    fun getCurrentUser() = supabase.auth.currentUserOrNull()

    fun isLoggedIn(): Boolean = supabase.auth.currentSessionOrNull() != null

    fun getCurrentUserId(): String? {
        return supabase.auth.currentUserOrNull()?.id
    }

    suspend fun getChats(userId: String): List<ChatDto> {
        try {
            return supabase.from("chats")
                .select {
                    filter { eq("user_id", userId) }
                    order("updated_at", Order.DESCENDING)
                }
                .decodeList<ChatDto>()
        } catch (e: Exception) {
            Log.e(TAG, "Error in getChats", e)
            return emptyList()
        }
    }

    suspend fun createChat(request: CreateChatRequest): ChatDto {
        Log.d(TAG, "Creating chat for model: ${request.model_id}")
        ensureModelExists(request.model_id)
        
        return try {
            supabase.from("chats")
                .insert(request) {
                    select()
                }
                .decodeSingle<ChatDto>()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create chat in Supabase", e)
            val errorMsg = e.message ?: ""
            if (errorMsg.contains("chats_model_id_fkey")) {
                Log.w(TAG, "FK Violation! Attempting emergency model sync for ${request.model_id}...")
                try {
                    ensureModelExists(request.model_id)
                    // Recursive call - be careful, but we only do it once
                    return supabase.from("chats").insert(request) { select() }.decodeSingle<ChatDto>()
                } catch (inner: Exception) {
                    Log.e(TAG, "Emergency sync and retry failed", inner)
                    throw Exception("Model '${request.model_id}' is not registered in the system. Please try another model or contact support.")
                }
            }
            throw e
        }
    }

    private suspend fun ensureModelExists(modelId: String) {
        if (modelId.isEmpty()) return
        try {
            Log.d(TAG, "Ensuring model exists in DB: $modelId")
            
            // First check if it exists to avoid unnecessary upsert attempts if RLS is tight
            val exists = try {
                supabase.from("ai_models").select {
                    filter { eq("id", modelId) }
                }.decodeList<AIModelDto>().isNotEmpty()
            } catch (e: Exception) {
                false
            }

            if (!exists) {
                supabase.from("ai_models").upsert(mapOf(
                    "id" to modelId,
                    "name" to (modelId.split("/").lastOrNull() ?: modelId),
                    "provider" to "AI",
                    "description" to "Auto-registered model",
                    "is_free" to true,
                    "is_active" to true
                ))
                Log.i(TAG, "Successfully auto-registered model: $modelId")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to auto-register model: $modelId", e)
            // If it's a permission issue, we can't do much, but we've tried.
        }
    }

    suspend fun syncModelsToDb(models: List<AIModelDto>) {
        if (models.isEmpty()) return
        try {
            Log.d(TAG, "Syncing ${models.size} models to Supabase DB")
            supabase.from("ai_models").upsert(models)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync models to DB", e)
        }
    }

    suspend fun updateChat(chatId: String, title: String) {
        try {
            supabase.from("chats")
                .update({
                    set("title", title)
                    set("updated_at", "now()")
                }) {
                    filter { eq("id", chatId) }
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error in updateChat", e)
        }
    }

    suspend fun deleteChat(chatId: String) {
        try {
            supabase.from("chats")
                .delete {
                    filter { eq("id", chatId) }
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error in deleteChat", e)
        }
    }

    suspend fun getMessages(chatId: String): List<MessageDto> {
        try {
            return supabase.from("messages")
                .select {
                    filter { eq("chat_id", chatId) }
                    order("created_at", Order.ASCENDING)
                }
                .decodeList<MessageDto>()
        } catch (e: Exception) {
            Log.e(TAG, "Error in getMessages", e)
            return emptyList()
        }
    }

    suspend fun createMessage(request: CreateMessageRequest): MessageDto {
        try {
            return supabase.from("messages")
                .insert(request) {
                    select()
                }
                .decodeSingle<MessageDto>()
        } catch (e: Exception) {
            Log.e(TAG, "Error in createMessage", e)
            throw e
        }
    }

    suspend fun submitMessageFeedback(request: MessageFeedbackRequest) {
        try {
            supabase.from("message_feedback").insert(request)
        } catch (e: Exception) {
            Log.e(TAG, "Error in submitMessageFeedback", e)
            throw e
        }
    }

    suspend fun getAIModels(): List<AIModelDto> {
        // Admin-managed DB list is authoritative because it carries is_active/default controls.
        try {
            val dbModels = supabase.from("ai_models")
                .select()
                .decodeList<AIModelDto>()
            if (dbModels.isNotEmpty()) {
                return dbModels
            }
        } catch (e: Exception) {
            Log.w(TAG, "DB model list unavailable, falling back to edge catalog", e)
        }

        try {
            // Fallback: fetch provider catalog from Edge Function.
            val response: HttpResponse = supabase.functions.invoke("chat") {
                method = HttpMethod.Get
            }
            val body = response.bodyAsText()
            
            // Note: NVIDIA returns { "data": [ { "id": "...", ... } ] }
            val json = Json { ignoreUnknownKeys = true }
            val modelsData = json.decodeFromString<kotlinx.serialization.json.JsonObject>(body)
            val dataArray = modelsData["data"]?.jsonArray
            
            if (dataArray != null) {
                val models = dataArray.map { 
                    val obj = it.jsonObject
                    val modelId = obj["id"]?.jsonPrimitive?.contentOrNull ?: ""
                    AIModelDto(
                        id = modelId,
                        name = obj["name"]?.jsonPrimitive?.contentOrNull
                            ?: modelId.split("/").lastOrNull()?.replace("-", " ")?.replaceFirstChar { char -> char.uppercase() }
                            ?: "Unknown",
                        provider = obj["provider"]?.jsonPrimitive?.contentOrNull
                            ?: inferProvider(modelId),
                        description = obj["description"]?.jsonPrimitive?.contentOrNull
                            ?: if (modelId.startsWith("gemini", true)) "Google Gemini chat model" else "NVIDIA NIM chat model",
                        is_free = obj["is_free"]?.jsonPrimitive?.booleanOrNull ?: true,
                        is_active = obj["is_active"]?.jsonPrimitive?.booleanOrNull ?: true
                    )
                }.filter { it.id.isNotEmpty() }
                
                // Try to sync these to DB in background
                syncModelsToDb(models)
                
                return models
            }
        } catch (e: CancellationException) {
            Log.d(TAG, "Edge model fetch cancelled")
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching models from edge function", e)
            com.m2h.m2haichatbot.utils.TelegramLogger.logError("Failed to fetch models from NVIDIA via Edge Function", e)
        }

        // Fallback to DB
        try {
            return supabase.from("ai_models")
                .select()
                .decodeList<AIModelDto>()
        } catch (e: Exception) {
            Log.e(TAG, "Error in getAIModels (DB fallback)", e)
            return emptyList()
        }
    }

    suspend fun getAppSettings(): AppSettingsDto {
        return try {
            supabase.from("app_settings")
                .select {
                    filter { eq("id", "global") }
                }
                .decodeSingleOrNull<AppSettingsDto>() ?: AppSettingsDto()
        } catch (e: Exception) {
            Log.e(TAG, "Error loading app settings; using defaults", e)
            AppSettingsDto()
        }
    }

    private fun inferProvider(modelId: String): String {
        return if (modelId.startsWith("gemini", ignoreCase = true) || modelId.startsWith("models/gemini", ignoreCase = true)) {
            "Google"
        } else {
            "NVIDIA"
        }
    }

    fun observeChatChanges(): Flow<PostgresAction> {
        val channel = supabase.realtime.channel("public:chats")
        return channel.postgresChangeFlow<PostgresAction>(schema = "public") {
            table = "chats"
        }
    }

    fun observeMessageChanges(chatId: String): Flow<PostgresAction> {
        val channel = supabase.realtime.channel("public:messages:$chatId")
        return channel.postgresChangeFlow<PostgresAction>(schema = "public") {
            table = "messages"
            filter("chat_id", FilterOperator.EQ, chatId)
        }
    }
    
    suspend fun startRealtime() {
        supabase.realtime.connect()
    }
}
