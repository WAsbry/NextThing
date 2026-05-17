package com.nextthing.app.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
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
    private val THEME_MODE_KEY = stringPreferencesKey("theme_mode")
    private val WEATHER_CONDITION_KEY = stringPreferencesKey("weather_condition")

    val themeMode: Flow<ThemeMode> = context.themeDataStore.data.map { prefs ->
        val name = prefs[THEME_MODE_KEY] ?: ThemeMode.SYSTEM.name
        runCatching { ThemeMode.valueOf(name) }.getOrDefault(ThemeMode.SYSTEM)
    }

    val currentWeatherCondition: Flow<WeatherCondition> = context.themeDataStore.data.map { prefs ->
        val name = prefs[WEATHER_CONDITION_KEY] ?: WeatherCondition.UNKNOWN.name
        runCatching { WeatherCondition.valueOf(name) }.getOrDefault(WeatherCondition.UNKNOWN)
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.themeDataStore.edit { prefs ->
            prefs[THEME_MODE_KEY] = mode.name
        }
    }

    suspend fun setWeatherCondition(condition: WeatherCondition) {
        context.themeDataStore.edit { prefs ->
            prefs[WEATHER_CONDITION_KEY] = condition.name
        }
    }

    // ── 每种天气的自定义主色（ARGB Long）────────────────────────
    private fun customPrimaryKey(condition: WeatherCondition) =
        longPreferencesKey("weather_custom_primary_${condition.name}")

    val weatherCustomPrimaries: kotlinx.coroutines.flow.Flow<Map<WeatherCondition, Long>> =
        context.themeDataStore.data.map { prefs ->
            WeatherCondition.values().mapNotNull { c ->
                prefs[customPrimaryKey(c)]?.let { c to it }
            }.toMap()
        }

    suspend fun setWeatherCustomPrimary(condition: WeatherCondition, colorArgb: Long) {
        context.themeDataStore.edit { prefs ->
            prefs[customPrimaryKey(condition)] = colorArgb
        }
    }

    suspend fun resetWeatherCustomPrimary(condition: WeatherCondition) {
        context.themeDataStore.edit { prefs ->
            prefs.remove(customPrimaryKey(condition))
        }
    }
}
