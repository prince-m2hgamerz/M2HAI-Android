package com.m2h.m2haichatbot.presentation.update

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.m2h.m2haichatbot.domain.model.AppSettings
import com.m2h.m2haichatbot.domain.repository.ModelRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppUpdateViewModel @Inject constructor(
    private val modelRepository: ModelRepository
) : ViewModel() {
    private val _settings = MutableStateFlow<AppSettings?>(null)
    val settings: StateFlow<AppSettings?> = _settings.asStateFlow()

    fun refresh() {
        viewModelScope.launch {
            runCatching { modelRepository.getAppSettings() }
                .onSuccess { _settings.value = it }
        }
    }
}
