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
    val userName: String? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferenceRepository: PreferenceRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                preferenceRepository.themeMode,
                preferenceRepository.defaultModel,
                preferenceRepository.isHapticEnabled
            ) { theme, model, haptic ->
                _uiState.update { it.copy(
                    themeMode = theme,
                    defaultModel = model,
                    isHapticEnabled = haptic
                ) }
            }.collect()
        }

        viewModelScope.launch {
            authRepository.getUserProfile().onSuccess { user ->
                _uiState.update { it.copy(
                    userEmail = user.email,
                    userName = user.fullName
                ) }
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
}
