package com.m2h.m2haichatbot.domain.repository

import com.m2h.m2haichatbot.domain.model.*
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val currentUser: Flow<User?>

    suspend fun signUp(email: String, password: String): Result<User>
    suspend fun signIn(email: String, password: String): Result<User>
    suspend fun signOut(): Result<Unit>
    suspend fun getCurrentUser(): User?
    fun isUserLoggedIn(): Boolean
    
    suspend fun getUserProfile(): Result<User>
    suspend fun updateUserProfile(fullName: String?, avatarUrl: String?): Result<Unit>
    suspend fun uploadAvatar(bytes: ByteArray): Result<String>
}

interface ChatRepository {
    suspend fun getChats(): Flow<List<Chat>>
    suspend fun getChatById(chatId: String): Chat?
    suspend fun createChat(modelId: String, title: String = "New Chat"): Result<Chat>
    suspend fun updateChat(chat: Chat): Result<Unit>
    suspend fun updateChatTitle(chatId: String, title: String): Result<Unit>
    suspend fun deleteChat(chatId: String): Result<Unit>
    suspend fun togglePin(chatId: String, isPinned: Boolean): Result<Unit>
    suspend fun toggleArchive(chatId: String, isArchived: Boolean): Result<Unit>
    
    suspend fun getMessages(chatId: String): Flow<List<Message>>
    suspend fun saveMessage(chatId: String, role: MessageRole, content: String, attachments: List<Attachment> = emptyList()): Result<Message>
    suspend fun streamChatResponse(chatId: String, messages: List<Message>, modelId: String): Flow<StreamResponse>
    
    suspend fun uploadAttachment(fileName: String, bytes: ByteArray, mimeType: String): Result<Attachment>
    suspend fun syncChats(): Result<Unit>
}

interface ModelRepository {
    suspend fun getAvailableModels(): List<AIModel>
    suspend fun getModels(): List<AIModel> // Alias for convenience
    suspend fun getModelById(modelId: String): AIModel?
}
