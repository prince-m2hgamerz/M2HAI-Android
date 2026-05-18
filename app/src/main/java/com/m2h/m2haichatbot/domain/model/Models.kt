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
    val isFree: Boolean = true,
    val isActive: Boolean = true
)

@Serializable
data class AppSettings(
    val appEnabled: Boolean = true,
    val signupEnabled: Boolean = true,
    val maintenanceMessage: String = "M2HAI is temporarily unavailable. Please try again later.",
    val globalAnnouncement: String = "",
    val defaultModelId: String = "meta/llama-3.1-8b-instruct",
    val imageModelId: String = "black-forest-labs/flux.1-schnell",
    val systemPrompt: String = "",
    val temperature: Double = 0.5,
    val maxTokens: Int = 1024,
    val latestVersionCode: Int = 1,
    val latestVersionName: String = "1.0",
    val minSupportedVersionCode: Int = 1,
    val updateRequired: Boolean = false,
    val updateTitle: String = "Update available",
    val updateMessage: String = "",
    val updateApkUrl: String = "",
    val updateReleaseNotes: String = "",
    val updateApkSizeMb: Double = 0.0,
    val updateSha256: String = "",
    val updatePublishedAt: String = "",
    val updateChannel: String = "stable"
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
