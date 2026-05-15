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
    val userAvatarUrl: String? = null
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val authRepository: AuthRepository,
    private val modelRepository: ModelRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val chatId: String = savedStateHandle.get<String>("chatId") ?: ""

    private val _state = MutableStateFlow(ChatState(chatId = chatId, modelId = "gemini-flash-latest"))
    val state: StateFlow<ChatState> = _state.asStateFlow()

    private val _availableModels = MutableStateFlow<List<AIModel>>(emptyList())
    val availableModels: StateFlow<List<AIModel>> = _availableModels.asStateFlow()

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
                val allowedIds = setOf(
                    "gemini-flash-latest",
                    "meta/llama-3.1-70b-instruct",
                    "meta/llama-3.1-8b-instruct",
                    "meta/llama-3.2-90b-vision-instruct",
                    "google/gemma-2-9b-it",
                    "google/gemma-2-2b-it",
                    "nvidia/llama-3.1-nemotron-70b-instruct",
                    "stabilityai/sdxl"
                )
                val models = allModels.filter { it.id in allowedIds }.sortedBy { it.name }
                _availableModels.value = models
                
                if (_state.value.modelId.isEmpty() && models.isNotEmpty()) {
                    val defaultId = if (models.any { it.id == "gemini-flash-latest" }) "gemini-flash-latest" else models.first().id
                    _state.update { it.copy(modelId = defaultId) }
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

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            val currentChatId = _state.value.chatId
            val baseModel = _state.value.modelId
            var apiModel = baseModel
            
            // Auto-switch to image model if requested
            val imageKeywords = listOf("generate", "create", "draw", "make", "show", "paint")
            val isImageRequest = imageKeywords.any { content.contains(it, ignoreCase = true) } && 
                               (content.contains("image", ignoreCase = true) || content.contains("picture", ignoreCase = true) || content.contains("photo", ignoreCase = true))
            
            if (isImageRequest) {
                apiModel = "stabilityai/sdxl"
            }

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
                    _state.update { currentState -> 
                        val newMessages = if (currentState.messages.any { it.id == savedMessage.id }) {
                            currentState.messages
                        } else {
                            currentState.messages + savedMessage
                        }
                        currentState.copy(messages = newMessages)
                    }
                    streamAIResponse(actualChatId, apiModel)
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

    private fun streamAIResponse(chatId: String, modelId: String? = null) {
        viewModelScope.launch {
            _state.update { 
                it.copy(
                    isLoading = false,
                    isStreaming = true,
                    streamingContent = ""
                )
            }

            try {
                val messages = _state.value.messages
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
            } catch (e: Exception) {
                _state.update { 
                    it.copy(
                        isStreaming = false,
                        streamingContent = "",
                        error = e.message ?: "Streaming failed"
                    )
                }
            }
        }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }
}
