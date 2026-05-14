package com.m2h.m2haichatbot.data.repository

import android.util.Log
import com.m2h.m2haichatbot.data.local.dao.AIModelDao
import com.m2h.m2haichatbot.data.local.dao.ChatDao
import com.m2h.m2haichatbot.data.local.dao.MessageDao
import com.m2h.m2haichatbot.data.local.entities.AIModelEntity
import com.m2h.m2haichatbot.data.local.entities.ChatEntity
import com.m2h.m2haichatbot.data.local.entities.MessageEntity
import com.m2h.m2haichatbot.data.remote.*
import com.m2h.m2haichatbot.domain.model.*
import com.m2h.m2haichatbot.domain.repository.AuthRepository
import com.m2h.m2haichatbot.domain.repository.ChatRepository as DomainChatRepository
import com.m2h.m2haichatbot.domain.repository.ModelRepository as DomainModelRepository
import com.m2h.m2haichatbot.utils.TelegramLogger
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.gotrue.SessionStatus
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.decodeRecord
import io.github.jan.supabase.storage.Storage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val supabaseService: SupabaseService,
    private val storage: Storage,
    private val auth: Auth
) : AuthRepository {
    
    override val currentUser: Flow<User?> = auth.sessionStatus
        .map { status ->
            when (status) {
                is SessionStatus.Authenticated -> {
                    val user = status.session.user
                    if (user != null) {
                        User(id = user.id, email = user.email ?: "")
                    } else null
                }
                else -> null
            }
        }

    override suspend fun signUp(email: String, password: String): Result<User> = runCatching {
        val profile = supabaseService.signUp(email, password)
        User(
            id = profile.id,
            email = profile.email,
            fullName = profile.full_name,
            avatarUrl = profile.avatar_url
        )
    }.onFailure {
        Log.e("AuthRepositoryImpl", "Signup failed", it)
        TelegramLogger.logError("Auth: Signup failed", it, "SignUp")
    }

    override suspend fun signIn(email: String, password: String): Result<User> = runCatching {
        val profile = supabaseService.signIn(email, password)
        User(
            id = profile.id,
            email = profile.email,
            fullName = profile.full_name,
            avatarUrl = profile.avatar_url
        )
    }.onFailure {
        Log.e("AuthRepositoryImpl", "Signin failed", it)
        TelegramLogger.logError("Auth: Signin failed", it, "Login")
    }

    override suspend fun signOut(): Result<Unit> = runCatching {
        supabaseService.signOut()
    }

    override suspend fun getCurrentUser(): User? {
        val user = supabaseService.getCurrentUser() ?: return null
        return User(id = user.id, email = user.email ?: "")
    }

    override fun isUserLoggedIn(): Boolean {
        return supabaseService.isLoggedIn()
    }

    override suspend fun getUserProfile(): Result<User> = runCatching {
        val userId = supabaseService.getCurrentUserId() ?: throw Exception("User not logged in")
        val profile = supabaseService.getProfile(userId) ?: throw Exception("Profile not found")
        User(id = profile.id, email = profile.email, fullName = profile.full_name, avatarUrl = profile.avatar_url)
    }

    override suspend fun updateUserProfile(fullName: String?, avatarUrl: String?): Result<Unit> = runCatching {
        val userId = supabaseService.getCurrentUserId() ?: throw Exception("User not logged in")
        supabaseService.updateProfile(userId, fullName, avatarUrl)
    }

    override suspend fun uploadAvatar(bytes: ByteArray): Result<String> = runCatching {
        val userId = supabaseService.getCurrentUserId() ?: throw Exception("User not logged in")
        val fileName = "${userId}_avatar.jpg"
        val bucket = storage["profile-images"]
        bucket.upload(fileName, bytes, upsert = true)
        bucket.publicUrl(fileName)
    }
}

class ChatRepositoryImpl @Inject constructor(
    private val supabaseService: SupabaseService,
    private val nvidiaAIService: NvidiaAIService,
    private val chatDao: ChatDao,
    private val messageDao: MessageDao,
    private val storage: Storage,
    private val auth: Auth,
    private val postgrest: Postgrest
) : DomainChatRepository {

    private val repositoryScope = CoroutineScope(Dispatchers.IO)

    init {
        setupRealtime()
    }

    private fun setupRealtime() {
        repositoryScope.launch {
            supabaseService.startRealtime()
            
            supabaseService.observeChatChanges().onEach { action ->
                when (action) {
                    is PostgresAction.Insert -> {
                        val chatDto = action.decodeRecord<ChatDto>()
                        chatDao.insertChat(chatDto.toDomain().toEntity())
                    }
                    is PostgresAction.Update -> {
                        val chatDto = action.decodeRecord<ChatDto>()
                        chatDao.updateChat(chatDto.toDomain().toEntity())
                    }
                    is PostgresAction.Delete -> {
                        val id = action.oldRecord["id"]?.toString()?.trim('"') ?: return@onEach
                        chatDao.deleteChatById(id)
                    }
                    else -> {}
                }
            }.launchIn(this)
        }
    }

    override suspend fun getChats(): Flow<List<Chat>> {
        val userId = supabaseService.getCurrentUserId() ?: return flow { emit(emptyList()) }
        
        return chatDao.getChats(userId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getChatById(chatId: String): Chat? {
        return chatDao.getChatById(chatId)?.toDomain()
    }

    override suspend fun createChat(modelId: String, title: String): Result<Chat> = runCatching {
        val userId = supabaseService.getCurrentUserId() ?: throw Exception("User not logged in")
        
        val request = CreateChatRequest(
            user_id = userId,
            title = title,
            model_id = modelId
        )
        
        val chatDto = supabaseService.createChat(request)
        val chat = chatDto.toDomain()
        
        chatDao.insertChat(chat.toEntity())
        chat
    }.onFailure {
        TelegramLogger.logError("Chat: Failed to create chat", it, "Home/Chat")
    }

    override suspend fun updateChat(chat: Chat): Result<Unit> = runCatching {
        supabaseService.updateChat(chat.id, chat.title)
        chatDao.updateChat(chat.toEntity())
    }

    override suspend fun updateChatTitle(chatId: String, title: String): Result<Unit> = runCatching {
        supabaseService.updateChat(chatId, title)
        val entity = chatDao.getChatById(chatId)
        if (entity != null) {
            chatDao.updateChat(entity.copy(title = title))
        }
    }

    override suspend fun deleteChat(chatId: String): Result<Unit> = runCatching {
        supabaseService.deleteChat(chatId)
        chatDao.deleteChatById(chatId)
        messageDao.deleteMessagesByChatId(chatId)
    }

    override suspend fun togglePin(chatId: String, isPinned: Boolean): Result<Unit> = runCatching {
        postgrest["chats"].update({
            set("is_pinned", isPinned)
        }) {
            filter { eq("id", chatId) }
        }
        val entity = chatDao.getChatById(chatId)
        if (entity != null) {
            chatDao.updateChat(entity.copy(isPinned = isPinned))
        }
    }

    override suspend fun toggleArchive(chatId: String, isArchived: Boolean): Result<Unit> = runCatching {
        postgrest["chats"].update({
            set("is_archived", isArchived)
        }) {
            filter { eq("id", chatId) }
        }
        val entity = chatDao.getChatById(chatId)
        if (entity != null) {
            chatDao.updateChat(entity.copy(isArchived = isArchived))
        }
    }

    override suspend fun getMessages(chatId: String): Flow<List<Message>> {
        // Start observing message changes for this chat if not already
        observeMessagesRealtime(chatId)
        
        return messageDao.getMessages(chatId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    private val messageObservers = mutableSetOf<String>()

    private fun observeMessagesRealtime(chatId: String) {
        if (messageObservers.contains(chatId)) return
        messageObservers.add(chatId)
        
        supabaseService.observeMessageChanges(chatId).onEach { action ->
            when (action) {
                is PostgresAction.Insert -> {
                    val messageDto = action.decodeRecord<MessageDto>()
                    messageDao.insertMessage(messageDto.toDomain().toEntity())
                }
                is PostgresAction.Delete -> {
                    // supabase doesn't send id for delete unless configured, but we can clear chat locally
                    // For simplicity, we just sync if needed
                }
                else -> {}
            }
        }.launchIn(repositoryScope)
    }

    override suspend fun saveMessage(
        chatId: String,
        role: MessageRole,
        content: String,
        attachments: List<Attachment>
    ): Result<Unit> = runCatching {
        val request = CreateMessageRequest(
            chat_id = chatId,
            role = role.name.lowercase(),
            content = content,
            attachments = attachments.map { Json.encodeToString(it) }
        )
        
        val messageDto = supabaseService.createMessage(request)
        messageDao.insertMessage(messageDto.toDomain().toEntity())
    }

    override suspend fun streamChatResponse(
        chatId: String,
        messages: List<Message>,
        modelId: String
    ): Flow<StreamResponse> = flow {
        val chatMessages = messages.map { 
            ChatMessage(role = it.role.name.lowercase(), content = it.content)
        }
        
        var aiContent = ""
        
        try {
            nvidiaAIService.streamChat(chatMessages, modelId).collect { chunk ->
                aiContent += chunk
                emit(StreamResponse(content = aiContent, isComplete = false))
            }
            
            val assistantMessage = CreateMessageRequest(
                chat_id = chatId,
                role = "assistant",
                content = aiContent
            )
            
            val savedMessage = supabaseService.createMessage(assistantMessage)
            messageDao.insertMessage(savedMessage.toDomain().toEntity())
            
            emit(StreamResponse(content = aiContent, isComplete = true))
        } catch (e: Exception) {
            TelegramLogger.logError("Chat: Streaming failed", e, "Chat")
            throw e
        }
    }

    override suspend fun uploadAttachment(
        fileName: String,
        bytes: ByteArray,
        mimeType: String
    ): Result<Attachment> = runCatching {
        val bucket = storage["chat-attachments"]
        val path = "${UUID.randomUUID()}_$fileName"
        bucket.upload(path, bytes, upsert = true)
        val url = bucket.publicUrl(path)
        
        Attachment(
            id = UUID.randomUUID().toString(),
            type = if (mimeType.startsWith("image")) "image" else "file",
            url = url,
            name = fileName
        )
    }

    override suspend fun syncChats(): Result<Unit> = runCatching {
        val userId = supabaseService.getCurrentUserId() ?: throw Exception("Not logged in")
        val chats = supabaseService.getChats(userId)
        chatDao.insertChats(chats.map { it.toDomain().toEntity() })
    }
}

class ModelRepositoryImpl @Inject constructor(
    private val supabaseService: SupabaseService,
    private val aiModelDao: AIModelDao
) : DomainModelRepository {

    override suspend fun getAvailableModels(): List<AIModel> {
        val localModels = aiModelDao.getAllModels()
        
        if (localModels.isEmpty()) {
            val remoteModels = supabaseService.getAIModels()
            val entities = remoteModels.map { it.toEntity() }
            aiModelDao.insertModels(entities)
            return remoteModels.map { it.toDomain() }
        }
        
        return localModels.map { it.toDomain() }
    }

    override suspend fun getModels(): List<AIModel> = getAvailableModels()

    override suspend fun getModelById(modelId: String): AIModel? {
        return aiModelDao.getModelById(modelId)?.toDomain()
    }
}

// Extension functions for mapping
private fun ChatEntity.toDomain() = Chat(
    id = id,
    userId = userId,
    title = title,
    modelId = modelId,
    isArchived = isArchived,
    isPinned = isPinned,
    createdAt = createdAt,
    updatedAt = updatedAt
)

private fun Chat.toEntity() = ChatEntity(
    id = id,
    userId = userId,
    title = title,
    modelId = modelId,
    isArchived = isArchived,
    isPinned = isPinned,
    createdAt = createdAt,
    updatedAt = updatedAt
)

private fun MessageEntity.toDomain() = Message(
    id = id,
    chatId = chatId,
    role = MessageRole.valueOf(role.uppercase()),
    content = content,
    attachments = attachmentsJson?.let { Json.decodeFromString<List<Attachment>>(it) } ?: emptyList(),
    createdAt = createdAt
)

private fun Message.toEntity() = MessageEntity(
    id = id,
    chatId = chatId,
    role = role.name.lowercase(),
    content = content,
    attachmentsJson = Json.encodeToString(attachments),
    createdAt = createdAt
)

private fun AIModelEntity.toDomain() = AIModel(
    id = id,
    name = name,
    provider = provider,
    description = description,
    isFree = isFree
)

private fun AIModelDto.toEntity() = AIModelEntity(
    id = id,
    name = name,
    provider = provider,
    description = description,
    isFree = is_free
)

private fun AIModelDto.toDomain() = AIModel(
    id = id,
    name = name,
    provider = provider,
    description = description,
    isFree = is_free
)

private fun ChatDto.toDomain() = Chat(
    id = id,
    userId = user_id,
    title = title,
    modelId = model_id,
    isArchived = is_archived,
    isPinned = is_pinned,
    createdAt = created_at,
    updatedAt = updated_at
)

private fun MessageDto.toDomain() = Message(
    id = id,
    chatId = chat_id,
    role = MessageRole.valueOf(role.uppercase()),
    content = content,
    attachments = attachments.map { attachmentJson ->
        try {
            Json.decodeFromString<Attachment>(attachmentJson)
        } catch (e: Exception) {
            Attachment(UUID.randomUUID().toString(), "file", attachmentJson)
        }
    },
    createdAt = created_at
)
