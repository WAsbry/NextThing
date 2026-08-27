package com.nextthing.app.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.nextthing.app.domain.model.ThemeMode
import com.nextthing.app.domain.model.WeatherCondition
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.themeDataStore by preferencesDataStore(name = "theme_prefs")

@Singleton
class ThemePreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val themeModeKey = stringPreferencesKey("theme_mode")
    private val weatherConditionKey = stringPreferencesKey("weather_condition")

    val themeMode: Flow<ThemeMode> = context.themeDataStore.data.map { prefs ->
        val savedMode = prefs[themeModeKey] ?: ThemeMode.SYSTEM.name
        // Older builds may have persisted WEATHER. It is intentionally mapped to SYSTEM.
        if (savedMode == "WEATHER") ThemeMode.SYSTEM
        else runCatching { ThemeMode.valueOf(savedMode) }.getOrDefault(ThemeMode.SYSTEM)
    }

    val currentWeatherCondition: Flow<WeatherCondition> = context.themeDataStore.data.map { prefs ->
        val name = prefs[weatherConditionKey] ?: WeatherCondition.UNKNOWN.name
        runCatching { WeatherCondition.valueOf(name) }.getOrDefault(WeatherCondition.UNKNOWN)
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.themeDataStore.edit { prefs -> prefs[themeModeKey] = mode.name }
    }

    // Weather data is retained for the home data flow; it no longer controls app appearance.
    suspend fun setWeatherCondition(condition: WeatherCondition) {
        context.themeDataStore.edit { prefs -> prefs[weatherConditionKey] = condition.name }
    }
}
