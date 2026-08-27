package com.nextthing.app.presentation.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nextthing.app.data.preferences.ThemePreferences
import com.nextthing.app.domain.model.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ThemeSettingsUiState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM
)

@HiltViewModel
class ThemeSettingsViewModel @Inject constructor(
    private val themePreferences: ThemePreferences
) : ViewModel() {
    private val _uiState = MutableStateFlow(ThemeSettingsUiState())
    val uiState: StateFlow<ThemeSettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            themePreferences.themeMode.collect { mode ->
                _uiState.value = _uiState.value.copy(themeMode = mode)
            }
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { themePreferences.setThemeMode(mode) }
    }
}
