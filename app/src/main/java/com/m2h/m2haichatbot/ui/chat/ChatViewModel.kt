package com.m2h.m2haichatbot.ui.chat

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.m2h.m2haichatbot.domain.model.*
import com.m2h.m2haichatbot.domain.repository.ChatRepository
import com.m2h.m2haichatbot.domain.repository.ModelRepository
import com.m2h.m2haichatbot.utils.VoiceToTextParser
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class ChatUiState(
    val chats: List<Chat> = emptyList(),
    val filteredChats: List<Chat> = emptyList(),
    val currentChat: Chat? = null,
    val messages: List<Message> = emptyList(),
    val availableModels: List<AIModel> = emptyList(),
    val isLoading: Boolean = false,
    val isStreaming: Boolean = false,
    val selectedModelId: String = "meta/llama-3.1-8b-instruct",
    val searchQuery: String = "",
    val isRecording: Boolean = false,
    val recordedText: String = ""
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val repository: ChatRepository,
    private val modelRepository: ModelRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var streamingJob: Job? = null
    private val voiceParser = VoiceToTextParser(context)

    init {
        observeChats()
        fetchModels()
        syncData()
        
        viewModelScope.launch {
            voiceParser.state.collect { voiceState ->
                _uiState.update { it.copy(
                    isRecording = voiceState.isSpeaking,
                    recordedText = voiceState.spokenText
                ) }
            }
        }
    }

    private fun observeChats() {
        viewModelScope.launch {
            repository.getChats().collect { chats ->
                _uiState.update { state ->
                    state.copy(
                        chats = chats,
                        filteredChats = if (state.searchQuery.isEmpty()) chats else chats.filter {
                            it.title.contains(state.searchQuery, ignoreCase = true)
                        }
                    )
                }
            }
        }
    }

    private fun fetchModels() {
        viewModelScope.launch {
            val models = modelRepository.getModels()
            if (models.isNotEmpty()) {
                _uiState.update { it.copy(availableModels = models) }
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { state ->
            state.copy(
                searchQuery = query,
                filteredChats = state.chats.filter { it.title.contains(query, ignoreCase = true) }
            )
        }
    }

    fun selectChat(chat: Chat) {
        stopStreaming()
        _uiState.update { it.copy(currentChat = chat, selectedModelId = chat.modelId) }
        observeMessages(chat.id)
    }

    private fun observeMessages(chatId: String) {
        viewModelScope.launch {
            repository.getMessages(chatId).collect { messages ->
                _uiState.update { it.copy(messages = messages) }
            }
        }
    }

    fun sendMessage(content: String, uris: List<Uri> = emptyList()) {
        if (content.isBlank() && uris.isEmpty()) return
        
        val currentState = _uiState.value
        val modelId = currentState.selectedModelId
        
        streamingJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            var chat = currentState.currentChat
            if (chat == null) {
                val result = repository.createChat(modelId, if (content.isNotEmpty()) content.take(30) else "New Chat")
                chat = result.getOrNull()
                if (chat != null) {
                    _uiState.update { it.copy(currentChat = chat) }
                    observeMessages(chat.id)
                } else {
                    _uiState.update { it.copy(isLoading = false) }
                    return@launch
                }
            }

            val attachments = mutableListOf<Attachment>()
            uris.forEach { uri ->
                try {
                    val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    val mimeType = context.contentResolver.getType(uri) ?: "application/octet-stream"
                    val fileName = uri.lastPathSegment ?: "file"
                    if (bytes != null) {
                        repository.uploadAttachment(fileName, bytes, mimeType).onSuccess {
                            attachments.add(it)
                        }
                    }
                } catch (e: Exception) {}
            }

            repository.saveMessage(chat.id, MessageRole.USER, content, attachments)
                .onSuccess { savedMessage ->
                    val snapshot = if (_uiState.value.messages.any { it.id == savedMessage.id }) {
                        _uiState.value.messages
                    } else {
                        _uiState.value.messages + savedMessage
                    }
                    _uiState.update { it.copy(messages = snapshot) }
                    startAIResponse(chat, modelId, snapshot)
                }
                .onFailure {
                    _uiState.update { it.copy(isLoading = false) }
                }
        }
    }

    fun regenerateResponse() {
        val chat = _uiState.value.currentChat ?: return
        val messages = _uiState.value.messages
        if (messages.isEmpty()) return

        stopStreaming()
        streamingJob = viewModelScope.launch {
            startAIResponse(chat, _uiState.value.selectedModelId, messages)
        }
    }

    private suspend fun startAIResponse(chat: Chat, modelId: String, messagesForApi: List<Message>? = null) {
        _uiState.update { it.copy(isStreaming = true, isLoading = false) }
        
        val aiMessageId = UUID.randomUUID().toString()
        
        // Use the messages BEFORE adding the temporary one for the API call
        val historyForApi = (messagesForApi ?: _uiState.value.messages).filter { it.content.isNotBlank() }

        val tempAiMessage = Message(
            id = aiMessageId,
            chatId = chat.id,
            role = MessageRole.ASSISTANT,
            content = "",
            createdAt = java.time.Instant.now().toString()
        )
        
        _uiState.update { it.copy(messages = it.messages + tempAiMessage) }

        try {
            var aiContent = ""
            repository.streamChatResponse(chat.id, historyForApi, modelId).collect { response ->
                aiContent = response.content
                _uiState.update { state ->
                    state.copy(
                        messages = state.messages.map { 
                            if (it.id == aiMessageId) it.copy(content = aiContent) else it 
                        }
                    )
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            _uiState.update { state ->
                state.copy(
                    messages = state.messages.filter { it.id != aiMessageId }
                )
            }
        } finally {
            _uiState.update { it.copy(isStreaming = false) }
        }
    }

    fun startVoiceInput() {
        voiceParser.startListening()
    }

    fun stopVoiceInput() {
        voiceParser.stopListening()
    }

    fun stopStreaming() {
        streamingJob?.cancel()
        _uiState.update { it.copy(isStreaming = false) }
    }

    fun updateModel(modelId: String) {
        _uiState.update { it.copy(selectedModelId = modelId) }
    }

    fun createNewChat() {
        stopStreaming()
        _uiState.update { it.copy(currentChat = null, messages = emptyList()) }
    }

    fun syncData() {
        viewModelScope.launch {
            try {
                repository.syncChats()
            } catch (e: Exception) {}
        }
    }

    fun deleteChat(chatId: String) {
        viewModelScope.launch {
            repository.deleteChat(chatId)
            if (_uiState.value.currentChat?.id == chatId) {
                createNewChat()
            }
        }
    }

    fun renameChat(chatId: String, newTitle: String) {
        viewModelScope.launch {
            repository.updateChatTitle(chatId, newTitle)
        }
    }

    fun togglePin(chat: Chat) {
        viewModelScope.launch {
            repository.togglePin(chat.id, !chat.isPinned)
        }
    }

    fun toggleArchive(chat: Chat) {
        viewModelScope.launch {
            repository.toggleArchive(chat.id, !chat.isArchived)
        }
    }
}
