package com.m2h.m2haichatbot.presentation.theme

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.m2h.m2haichatbot.data.repository.PreferenceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class ThemeViewModel @Inject constructor(
    preferenceRepository: PreferenceRepository
) : ViewModel() {

    val darkTheme: StateFlow<Boolean?> = preferenceRepository.themeMode
        .map { mode ->
            when (mode.lowercase()) {
                "dark" -> true
                "light" -> false
                else -> null // "system"
            }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)
}
