package com.m2h.m2haichatbot.presentation.chat

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.m2h.m2haichatbot.domain.model.AIModel
import com.m2h.m2haichatbot.domain.model.Message
import com.m2h.m2haichatbot.domain.model.MessageRole
import com.m2h.m2haichatbot.domain.repository.AuthRepository
import com.m2h.m2haichatbot.domain.repository.ChatRepository
import com.m2h.m2haichatbot.domain.repository.ModelRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChatState(
    val messages: List<Message> = emptyList(),
    val isLoading: Boolean = false,
    val isStreaming: Boolean = false,
    val streamingContent: String = "",
    val error: String? = null,
    val chatId: String = "",
    val modelId: String = "",
    val userAvatarUrl: String? = null,
    val appEnabled: Boolean = true,
    val maintenanceMessage: String = ""
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val authRepository: AuthRepository,
    private val modelRepository: ModelRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val chatId: String = savedStateHandle.get<String>("chatId") ?: ""

    private val _state = MutableStateFlow(ChatState(chatId = chatId, modelId = ""))
    val state: StateFlow<ChatState> = _state.asStateFlow()

    private val _availableModels = MutableStateFlow<List<AIModel>>(emptyList())
    val availableModels: StateFlow<List<AIModel>> = _availableModels.asStateFlow()

    private var streamingJob: Job? = null

    fun setModel(modelId: String) {
        _state.update { it.copy(modelId = modelId) }
    }

    init {
        loadUserProfile()
        loadMessages()
        loadModels()
        loadChat()
    }

    private fun loadUserProfile() {
        viewModelScope.launch {
            authRepository.getUserProfile().onSuccess { user ->
                _state.update { it.copy(userAvatarUrl = user.avatarUrl) }
            }
        }
    }

    private fun loadModels() {
        viewModelScope.launch {
            try {
                val allModels = modelRepository.getAvailableModels()
                val settings = modelRepository.getAppSettings()
                val models = allModels.sortedWith(compareBy<AIModel> { it.provider }.thenBy { it.name })
                _availableModels.value = models
                
                val currentModelId = _state.value.modelId
                val defaultId = models.firstOrNull { it.id == settings.defaultModelId }?.id
                    ?: models.firstOrNull()?.id
                    ?: ""

                if ((currentModelId.isEmpty() || models.none { it.id == currentModelId }) && defaultId.isNotEmpty()) {
                    _state.update {
                        it.copy(
                            modelId = defaultId,
                            appEnabled = settings.appEnabled,
                            maintenanceMessage = settings.maintenanceMessage
                        )
                    }
                } else {
                    _state.update {
                        it.copy(
                            appEnabled = settings.appEnabled,
                            maintenanceMessage = settings.maintenanceMessage
                        )
                    }
                }
            } catch (e: Exception) {
                // Ignore or handle
            }
        }
    }

    private fun loadChat() {
        if (chatId == "new" || chatId.isEmpty()) return
        viewModelScope.launch {
            try {
                val chat = chatRepository.getChatById(chatId)
                _state.update { it.copy(modelId = chat?.modelId ?: "") }
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message) }
            }
        }
    }

    private fun loadMessages() {
        if (chatId == "new" || chatId.isEmpty()) return
        viewModelScope.launch {
            try {
                chatRepository.getMessages(chatId).collect { messages ->
                    _state.update { it.copy(messages = messages) }
                }
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message) }
            }
        }
    }

    fun sendMessage(content: String) {
        if (content.isBlank()) return
        if (!_state.value.appEnabled) {
            _state.update { it.copy(error = it.maintenanceMessage.ifBlank { "M2HAI is temporarily unavailable. Please try again later." }) }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            val currentChatId = _state.value.chatId
            val baseModel = _state.value.modelId.ifBlank {
                _availableModels.value.firstOrNull { it.isActive }?.id
                    ?: runCatching { modelRepository.getAppSettings().defaultModelId }.getOrDefault("")
            }
            if (baseModel.isBlank()) {
                _state.update { it.copy(isLoading = false, error = "No active AI models are available. Ask the administrator to enable a model.") }
                return@launch
            }
            if (_state.value.modelId.isBlank()) {
                _state.update { it.copy(modelId = baseModel) }
            }
            val apiModel = baseModel
            
            val actualChatId = if (currentChatId == "new" || currentChatId.isEmpty()) {
                try {
                    val title = if (content.length > 30) "${content.take(27)}..." else content
                    // Create chat with the base model (e.g. Gemini), not the image model
                    val chat = chatRepository.createChat(baseModel, title).getOrThrow()
                    _state.update { it.copy(chatId = chat.id) }
                    chat.id
                } catch (e: Exception) {
                    _state.update { it.copy(isLoading = false, error = e.message ?: "Failed to create chat") }
                    return@launch
                }
            } else {
                currentChatId
            }

            chatRepository.saveMessage(actualChatId, MessageRole.USER, content)
                .onSuccess { savedMessage ->
                    val messagesSnapshot = if (_state.value.messages.any { it.id == savedMessage.id }) {
                        _state.value.messages
                    } else {
                        _state.value.messages + savedMessage
                    }
                    _state.update { it.copy(messages = messagesSnapshot) }
                    streamAIResponse(actualChatId, apiModel, messagesSnapshot)
                }
                .onFailure { error ->
                    _state.update { 
                        it.copy(
                            isLoading = false,
                            error = error.message ?: "Failed to send message"
                        )
                    }
                }
        }
    }

    private fun streamAIResponse(
        chatId: String,
        modelId: String? = null,
        messagesForApi: List<Message>? = null
    ) {
        streamingJob?.cancel()
        streamingJob = viewModelScope.launch {
            _state.update { 
                it.copy(
                    isLoading = false,
                    isStreaming = true,
                    streamingContent = ""
                )
            }

            try {
                val messages = messagesForApi ?: _state.value.messages
                val targetModelId = modelId ?: _state.value.modelId

                chatRepository.streamChatResponse(chatId, messages, targetModelId)
                    .collect { response ->
                        _state.update { 
                            it.copy(
                                streamingContent = response.content,
                                isStreaming = !response.isComplete
                            )
                        }

                        if (response.isComplete) {
                            _state.update { it.copy(streamingContent = "") }
                        }
                    }
            } catch (e: CancellationException) {
                _state.update { it.copy(isStreaming = false, streamingContent = "") }
                throw e
            } catch (e: Exception) {
                _state.update { 
                    it.copy(
                        isStreaming = false,
                        streamingContent = "",
                        error = e.message ?: "Streaming failed"
                    )
                }
            } finally {
                streamingJob = null
            }
        }
    }

    fun stopStreaming() {
        streamingJob?.cancel()
        streamingJob = null
        _state.update {
            it.copy(
                isLoading = false,
                isStreaming = false,
                streamingContent = ""
            )
        }
    }

    fun submitMessageAction(message: Message, action: String) {
        viewModelScope.launch {
            chatRepository.submitMessageFeedback(message, action, _state.value.modelId)
        }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }
}
