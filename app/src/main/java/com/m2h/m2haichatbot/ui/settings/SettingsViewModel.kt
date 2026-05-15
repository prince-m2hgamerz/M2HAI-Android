package com.m2h.m2haichatbot.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.m2h.m2haichatbot.data.repository.PreferenceRepository
import com.m2h.m2haichatbot.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val themeMode: String = "system",
    val defaultModel: String = "meta/llama-3.1-8b-instruct",
    val isHapticEnabled: Boolean = true,
    val userEmail: String? = null,
    val userName: String? = null,
    val avatarUrl: String? = null,
    val geminiApiKey: String = "",
    val openaiApiKey: String = "",
    val groqApiKey: String = ""
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferenceRepository: PreferenceRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val _error = MutableSharedFlow<String>()
    val error = _error.asSharedFlow()

    init {
        viewModelScope.launch {
            combine(
                preferenceRepository.themeMode,
                preferenceRepository.defaultModel,
                preferenceRepository.isHapticEnabled,
                preferenceRepository.geminiApiKey,
                preferenceRepository.openaiApiKey,
                preferenceRepository.groqApiKey
            ) { values ->
                val theme = values[0] as String
                val model = values[1] as String
                val haptic = values[2] as Boolean
                val gemini = values[3] as String
                val openai = values[4] as String
                val groq = values[5] as String
                
                _uiState.update { it.copy(
                    themeMode = theme,
                    defaultModel = model,
                    isHapticEnabled = haptic,
                    geminiApiKey = gemini,
                    openaiApiKey = openai,
                    groqApiKey = groq
                ) }
            }.collect()
        }

        viewModelScope.launch {
            authRepository.getUserProfile().onSuccess { user ->
                _uiState.update { it.copy(
                    userEmail = user.email,
                    userName = user.fullName,
                    avatarUrl = user.avatarUrl
                ) }
            }
        }
    }

    fun updateProfileName(name: String) {
        viewModelScope.launch {
            authRepository.updateUserProfile(fullName = name, avatarUrl = null).onSuccess {
                _uiState.update { it.copy(userName = name) }
            }.onFailure { e ->
                _error.emit("Failed to update name: ${e.message}")
            }
        }
    }

    fun uploadAvatar(bytes: ByteArray) {
        viewModelScope.launch {
            authRepository.uploadAvatar(bytes).onSuccess { url ->
                authRepository.updateUserProfile(fullName = null, avatarUrl = url).onSuccess {
                    _uiState.update { it.copy(avatarUrl = url) }
                }.onFailure { e ->
                    _error.emit("Failed to update profile with new photo: ${e.message}")
                }
            }.onFailure { e ->
                _error.emit("Failed to upload photo: ${e.message}")
            }
        }
    }

    fun setThemeMode(mode: String) {
        viewModelScope.launch {
            preferenceRepository.setThemeMode(mode)
        }
    }

    fun setDefaultModel(modelId: String) {
        viewModelScope.launch {
            preferenceRepository.setDefaultModel(modelId)
        }
    }

    fun setHapticEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferenceRepository.setHapticEnabled(enabled)
        }
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.signOut()
        }
    }

    fun setGeminiApiKey(key: String) {
        viewModelScope.launch {
            preferenceRepository.setGeminiApiKey(key)
        }
    }

    fun setOpenaiApiKey(key: String) {
        viewModelScope.launch {
            preferenceRepository.setOpenaiApiKey(key)
        }
    }

    fun setGroqApiKey(key: String) {
        viewModelScope.launch {
            preferenceRepository.setGroqApiKey(key)
        }
    }
}
