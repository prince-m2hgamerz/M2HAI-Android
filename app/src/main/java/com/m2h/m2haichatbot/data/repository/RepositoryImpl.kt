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
import kotlinx.coroutines.CancellationException
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
        val settings = supabaseService.getAppSettings()
        if (!settings.app_enabled) {
            throw Exception(settings.maintenance_message)
        }
        if (!settings.signup_enabled) {
            throw Exception("Signups are temporarily disabled by the administrator.")
        }

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
        if (profile.is_disabled) {
            supabaseService.signOut()
            throw Exception("This account has been disabled by the administrator.")
        }
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
        val fileName = "$userId/${userId}_avatar.jpg"
        
        try {
            storage.createBucket("profile-images") {
                public = true
            }
        } catch (e: Exception) {
            // Bucket might already exist or permission denied
        }

        val bucket = storage["profile-images"]
        try {
            bucket.upload(fileName, bytes, upsert = true)
            bucket.publicUrl(fileName)
        } catch (e: Exception) {
            if (e.message?.contains("row level security", ignoreCase = true) == true) {
                throw Exception("Failed to upload: RLS Policy missing. Please run the SQL fix in Supabase Dashboard.")
            }
            throw e
        }
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
    ): Result<Message> = runCatching {
        val request = CreateMessageRequest(
            chat_id = chatId,
            role = role.name.lowercase(),
            content = content,
            attachments = attachments.map { Json.encodeToString(it) }
        )
        
        val messageDto = supabaseService.createMessage(request)
        val domainMessage = messageDto.toDomain()
        messageDao.insertMessage(domainMessage.toEntity())
        domainMessage
    }

    override suspend fun streamChatResponse(
        chatId: String,
        messages: List<Message>,
        modelId: String
    ): Flow<StreamResponse> = flow {
        val settings = supabaseService.getAppSettings()
        if (!settings.app_enabled) {
            throw Exception(settings.maintenance_message)
        }
        val userId = supabaseService.getCurrentUserId()
        if (userId != null && supabaseService.getProfile(userId)?.is_disabled == true) {
            throw Exception("This account has been disabled by the administrator.")
        }

        val chatMessages = normalizeHistoryForProvider(messages).map {
            ChatMessage(role = it.role.name.lowercase(), content = it.content)
        }
        val imageRequest = shouldUseImageModel(messages, settings.image_model_id)
        val targetModelId = if (imageRequest) {
            settings.image_model_id
        } else {
            modelId.ifBlank { settings.default_model_id }
        }
        val registeredModels = supabaseService.getAIModels()
        if (!imageRequest && registeredModels.isNotEmpty() && registeredModels.none { it.id == targetModelId && it.is_active }) {
            throw Exception("This AI model is disabled by the administrator.")
        }
        
        var aiContent = ""
        
        try {
            nvidiaAIService.streamChat(
                messages = chatMessages,
                modelId = targetModelId,
                systemPrompt = settings.system_prompt,
                temperature = settings.temperature,
                maxTokens = settings.max_tokens
            ).collect { chunk ->
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
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            TelegramLogger.logError("Chat: Streaming failed", e, "Chat")
            throw e
        }
    }

    override suspend fun submitMessageFeedback(
        message: Message,
        action: String,
        modelId: String?
    ): Result<Unit> = runCatching {
        val userId = supabaseService.getCurrentUserId() ?: throw Exception("User not logged in")
        val chatModelId = modelId ?: chatDao.getChatById(message.chatId)?.modelId
        supabaseService.submitMessageFeedback(
            MessageFeedbackRequest(
                message_id = message.id,
                chat_id = message.chatId,
                user_id = userId,
                action = action,
                model_id = chatModelId
            )
        )
    }.onFailure {
        TelegramLogger.logError("Chat: Failed to submit message feedback", it, "Chat")
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

    private fun normalizeHistoryForProvider(messages: List<Message>): List<Message> {
        val cleaned = messages
            .filter { it.content.isNotBlank() && it.role != MessageRole.SYSTEM }
            .dropWhile { it.role != MessageRole.USER }

        if (cleaned.isEmpty()) return emptyList()

        val normalized = mutableListOf<Message>()
        for (message in cleaned) {
            val previous = normalized.lastOrNull()
            if (previous?.role == message.role) {
                if (message.role == MessageRole.USER) {
                    normalized[normalized.lastIndex] = previous.copy(
                        content = listOf(previous.content, message.content).joinToString("\n\n")
                    )
                }
            } else {
                normalized.add(message)
            }
        }
        return normalized
    }

    private fun shouldUseImageModel(messages: List<Message>, imageModelId: String): Boolean {
        if (imageModelId.isBlank()) return false
        val latestUserMessage = messages.lastOrNull { it.role == MessageRole.USER }?.content ?: return false
        val prompt = latestUserMessage.lowercase()
        val imageTerms = listOf(
            "generate image",
            "generate an image",
            "generate me an image",
            "generate one image",
            "generate a picture",
            "generate a photo",
            "generate a logo",
            "generate a poster",
            "generate wallpaper",
            "create image",
            "create an image",
            "create a picture",
            "create a photo",
            "create a logo",
            "create a poster",
            "make image",
            "make an image",
            "make a picture",
            "make a photo",
            "make a logo",
            "make a poster",
            "design a logo",
            "design poster",
            "draw",
            "illustration",
            "picture",
            "photo",
            "poster",
            "wallpaper",
            "logo",
            "render",
            "text-to-image",
            "image generation"
        )
        return imageTerms.any { prompt.contains(it) }
    }
}

class ModelRepositoryImpl @Inject constructor(
    private val supabaseService: SupabaseService,
    private val aiModelDao: AIModelDao
) : DomainModelRepository {

    override suspend fun getAvailableModels(): List<AIModel> {
        return try {
            val remoteModels = supabaseService.getAIModels()
            val entities = remoteModels.map { it.toEntity() }
            // Optional: aiModelDao.clear() if needed, but insertModels with conflict strategy REPLACE works
            aiModelDao.insertModels(entities)
            remoteModels.map { it.toDomain() }.filter { it.isActive }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            val localModels = aiModelDao.getAllModels()
            localModels.map { it.toDomain() }.filter { it.isActive }
        }
    }

    override suspend fun getModels(): List<AIModel> = getAvailableModels()

    override suspend fun getModelById(modelId: String): AIModel? {
        return aiModelDao.getModelById(modelId)?.toDomain()
    }

    override suspend fun getAppSettings(): AppSettings {
        val settings = supabaseService.getAppSettings()
        return AppSettings(
            appEnabled = settings.app_enabled,
            signupEnabled = settings.signup_enabled,
            maintenanceMessage = settings.maintenance_message,
            globalAnnouncement = settings.global_announcement,
            defaultModelId = settings.default_model_id,
            imageModelId = settings.image_model_id,
            systemPrompt = settings.system_prompt,
            temperature = settings.temperature,
            maxTokens = settings.max_tokens,
            latestVersionCode = settings.latest_version_code,
            latestVersionName = settings.latest_version_name,
            minSupportedVersionCode = settings.min_supported_version_code,
            updateRequired = settings.update_required,
            updateTitle = settings.update_title,
            updateMessage = settings.update_message,
            updateApkUrl = settings.update_apk_url,
            updateReleaseNotes = settings.update_release_notes,
            updateApkSizeMb = settings.update_apk_size_mb,
            updateSha256 = settings.update_sha256,
            updatePublishedAt = settings.update_published_at,
            updateChannel = settings.update_channel
        )
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
    isFree = isFree,
    isActive = isActive
)

private fun AIModelDto.toEntity() = AIModelEntity(
    id = id,
    name = name,
    provider = provider,
    description = description,
    isFree = is_free,
    isActive = is_active
)

private fun AIModelDto.toDomain() = AIModel(
    id = id,
    name = name,
    provider = provider,
    description = description,
    isFree = is_free,
    isActive = is_active
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
