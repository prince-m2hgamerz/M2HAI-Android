package com.m2h.m2haichatbot.presentation.chat

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.m2h.m2haichatbot.domain.model.Message
import com.m2h.m2haichatbot.domain.model.MessageRole
import com.m2h.m2haichatbot.domain.repository.ChatRepository
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
    val modelId: String = ""
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val chatId: String = savedStateHandle.get<String>("chatId") ?: ""

    private val _state = MutableStateFlow(ChatState(chatId = chatId))
    val state: StateFlow<ChatState> = _state.asStateFlow()

    init {
        loadChat()
        loadMessages()
    }

    private fun loadChat() {
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

            chatRepository.saveMessage(chatId, MessageRole.USER, content)
                .onSuccess {
                    streamAIResponse()
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

    private fun streamAIResponse() {
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
                val modelId = _state.value.modelId

                chatRepository.streamChatResponse(chatId, messages, modelId)
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
