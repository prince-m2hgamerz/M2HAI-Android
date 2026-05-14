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
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable
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
    val avatar_url: String? = null
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
    val is_free: Boolean = true
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
        try {
            return supabase.from("chats")
                .insert(request) {
                    select()
                }
                .decodeSingle<ChatDto>()
        } catch (e: Exception) {
            Log.e(TAG, "Error in createChat", e)
            throw e
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

    suspend fun getAIModels(): List<AIModelDto> {
        try {
            // First, try to fetch from Edge Function (NVIDIA API list)
            val response: HttpResponse = supabase.functions.invoke("chat") {
                method = HttpMethod.Get
            }
            val body = response.bodyAsText()
            
            // Note: NVIDIA returns { "data": [ { "id": "...", ... } ] }
            val json = Json { ignoreUnknownKeys = true }
            val modelsData = json.decodeFromString<kotlinx.serialization.json.JsonObject>(body)
            val dataArray = modelsData["data"]?.jsonArray
            
            if (dataArray != null) {
                return dataArray.map { 
                    val obj = it.jsonObject
                    val modelId = obj["id"]?.jsonPrimitive?.content ?: ""
                    AIModelDto(
                        id = modelId,
                        name = modelId.split("/").lastOrNull() ?: "Unknown",
                        provider = "NVIDIA",
                        description = "NIM Model",
                        is_free = true
                    )
                }.filter { it.id.isNotEmpty() }
            }
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
