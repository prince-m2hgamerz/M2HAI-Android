package com.m2h.m2haichatbot.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.m2h.m2haichatbot.domain.model.User
import com.m2h.m2haichatbot.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthState(
    val isLoading: Boolean = true, // Start in loading state
    val user: User? = null,
    val error: String? = null,
    val isSuccess: Boolean = false
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _state = MutableStateFlow(AuthState())
    val state: StateFlow<AuthState> = _state.asStateFlow()

    init {
        observeAuthStatus()
    }

    private fun observeAuthStatus() {
        authRepository.currentUser
            .onEach { user ->
                _state.update { it.copy(user = user, isLoading = false) }
            }
            .launchIn(viewModelScope)
    }

    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            
            authRepository.signIn(email, password)
                .onSuccess { user ->
                    _state.update { it.copy(user = user, isSuccess = true, isLoading = false) }
                }
                .onFailure { error ->
                    _state.update { it.copy(error = error.message ?: "Sign in failed", isLoading = false) }
                }
        }
    }

    fun signUp(email: String, password: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            
            authRepository.signUp(email, password)
                .onSuccess { user ->
                    _state.update { it.copy(user = user, isSuccess = true, isLoading = false) }
                }
                .onFailure { error ->
                    _state.update { it.copy(error = error.message ?: "Sign up failed", isLoading = false) }
                }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            authRepository.signOut()
            _state.update { AuthState(isLoading = false) }
        }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }
}
