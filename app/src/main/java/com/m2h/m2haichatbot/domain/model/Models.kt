package com.m2h.m2haichatbot.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class User(
    val id: String,
    val email: String,
    val fullName: String? = null,
    val avatarUrl: String? = null
)

@Serializable
data class AIModel(
    val id: String,
    val name: String,
    val provider: String,
    val description: String? = null,
    val isFree: Boolean = true
)

@Serializable
data class Chat(
    val id: String,
    val userId: String,
    val title: String = "New Chat",
    val modelId: String,
    val isArchived: Boolean = false,
    val isPinned: Boolean = false,
    val createdAt: String,
    val updatedAt: String
)

@Serializable
data class Message(
    val id: String,
    val chatId: String,
    val role: MessageRole,
    val content: String,
    val attachments: List<Attachment> = emptyList(),
    val createdAt: String
)

@Serializable
data class Attachment(
    val id: String,
    val type: String, // 'image', 'file'
    val url: String,
    val name: String? = null
)

enum class MessageRole {
    USER, ASSISTANT, SYSTEM
}

data class ChatWithMessages(
    val chat: Chat,
    val messages: List<Message>
)

data class StreamResponse(
    val content: String,
    val isComplete: Boolean = false
)
