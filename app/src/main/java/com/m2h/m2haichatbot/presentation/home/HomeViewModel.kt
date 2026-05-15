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
    val error: String? = null,
    val userAvatarUrl: String? = null,
    val userFullName: String? = null
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
        loadUserProfile()
        loadModels()
        syncAndLoadChats()
    }

    private fun loadUserProfile() {
        viewModelScope.launch {
            authRepository.getUserProfile().onSuccess { user ->
                _state.update { it.copy(userAvatarUrl = user.avatarUrl, userFullName = user.fullName) }
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
                _state.update { it.copy(models = models, selectedModel = models.firstOrNull { it.id == "gemini-flash-latest" } ?: models.firstOrNull()) }
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message) }
            }
        }
    }

    private fun syncAndLoadChats() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            try {
                // Initial sync from server
                chatRepository.syncChats()
                
                // Observe local database for real-time updates
                chatRepository.getChats().collect { chats ->
                    _state.update { it.copy(chats = chats, isLoading = false) }
                }
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message, isLoading = false) }
            }
        }
    }

    fun createNewChat(onChatCreated: (String) -> Unit) {
        onChatCreated("new")
    }

    fun deleteChat(chatId: String) {
        viewModelScope.launch {
            chatRepository.deleteChat(chatId)
        }
    }

    fun renameChat(chatId: String, newTitle: String) {
        viewModelScope.launch {
            chatRepository.updateChatTitle(chatId, newTitle)
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
