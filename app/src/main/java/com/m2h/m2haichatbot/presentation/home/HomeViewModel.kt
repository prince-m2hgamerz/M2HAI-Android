package com.m2h.m2haichatbot.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.m2h.m2haichatbot.domain.model.AIModel
import com.m2h.m2haichatbot.domain.model.Chat
import com.m2h.m2haichatbot.domain.repository.AuthRepository
import com.m2h.m2haichatbot.domain.repository.ChatRepository
import com.m2h.m2haichatbot.domain.repository.ModelRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeState(
    val chats: List<Chat> = emptyList(),
    val models: List<AIModel> = emptyList(),
    val selectedModel: AIModel? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val modelRepository: ModelRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _state = MutableStateFlow(HomeState())
    val state: StateFlow<HomeState> = _state.asStateFlow()

    init {
        loadModels()
        loadChats()
    }

    private fun loadModels() {
        viewModelScope.launch {
            try {
                val models = modelRepository.getAvailableModels()
                _state.update { it.copy(models = models, selectedModel = models.firstOrNull()) }
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message) }
            }
        }
    }

    private fun loadChats() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            try {
                chatRepository.getChats().collect { chats ->
                    _state.update { it.copy(chats = chats, isLoading = false) }
                }
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message, isLoading = false) }
            }
        }
    }

    fun createNewChat(onChatCreated: (String) -> Unit) {
        viewModelScope.launch {
            val modelId = _state.value.selectedModel?.id ?: return@launch
            
            chatRepository.createChat(modelId)
                .onSuccess { chat ->
                    onChatCreated(chat.id)
                }
                .onFailure { error ->
                    _state.update { it.copy(error = error.message) }
                }
        }
    }

    fun deleteChat(chatId: String) {
        viewModelScope.launch {
            chatRepository.deleteChat(chatId)
        }
    }

    fun selectModel(model: AIModel) {
        _state.update { it.copy(selectedModel = model) }
    }

    fun logout(onLogout: () -> Unit) {
        viewModelScope.launch {
            authRepository.signOut()
            onLogout()
        }
    }
}
