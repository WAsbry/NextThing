package com.nextthing.app.presentation.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nextthing.app.data.preferences.ThemePreferences
import com.nextthing.app.domain.model.ThemeMode
import com.nextthing.app.domain.model.WeatherCondition
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ThemeSettingsUiState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val customPrimaries: Map<WeatherCondition, Long> = emptyMap(),
    /** 当前正在预览的天气（点击卡片后设置） */
    val previewCondition: WeatherCondition? = null,
    /** 当前正在编辑颜色的天气 */
    val editingCondition: WeatherCondition? = null
)

@HiltViewModel
class ThemeSettingsViewModel @Inject constructor(
    private val themePreferences: ThemePreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(ThemeSettingsUiState())
    val uiState: StateFlow<ThemeSettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                themePreferences.themeMode,
                themePreferences.weatherCustomPrimaries
            ) { mode, primaries -> mode to primaries }
                .collect { (mode, primaries) ->
                    _uiState.value = _uiState.value.copy(
                        themeMode = mode,
                        customPrimaries = primaries
                    )
                }
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { themePreferences.setThemeMode(mode) }
    }

    fun setPreviewCondition(condition: WeatherCondition?) {
        _uiState.value = _uiState.value.copy(previewCondition = condition)
    }

    fun openColorEditor(condition: WeatherCondition) {
        _uiState.value = _uiState.value.copy(editingCondition = condition)
    }

    fun closeColorEditor() {
        _uiState.value = _uiState.value.copy(editingCondition = null)
    }

    fun setCustomPrimary(condition: WeatherCondition, colorArgb: Long) {
        viewModelScope.launch {
            themePreferences.setWeatherCustomPrimary(condition, colorArgb)
        }
        _uiState.value = _uiState.value.copy(editingCondition = null)
    }

    fun resetCustomPrimary(condition: WeatherCondition) {
        viewModelScope.launch { themePreferences.resetWeatherCustomPrimary(condition) }
    }
}
