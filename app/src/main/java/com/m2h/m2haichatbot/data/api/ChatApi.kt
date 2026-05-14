package com.m2h.m2haichatbot.data.api

import kotlinx.serialization.Serializable
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Streaming

@Serializable
data class ChatRequest(
    val messages: List<ChatMessage>,
    val model: String,
    val stream: Boolean = true
)

@Serializable
data class ChatMessage(
    val role: String,
    val content: String
)

interface ChatApi {
    @Streaming
    @POST("functions/v1/chat")
    suspend fun streamChat(
        @Header("Authorization") token: String,
        @Body request: ChatRequest
    ): Response<ResponseBody>
}
