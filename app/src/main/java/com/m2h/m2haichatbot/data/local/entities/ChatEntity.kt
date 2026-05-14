package com.m2h.m2haichatbot.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chats")
data class ChatEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val title: String,
    val modelId: String,
    val isArchived: Boolean,
    val isPinned: Boolean,
    val createdAt: String,
    val updatedAt: String
)

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey val id: String,
    val chatId: String,
    val role: String,
    val content: String,
    val attachmentsJson: String?, // Store list as JSON string
    val createdAt: String
)

@Entity(tableName = "ai_models")
data class AIModelEntity(
    @PrimaryKey val id: String,
    val name: String,
    val provider: String,
    val description: String?,
    val isFree: Boolean
)
