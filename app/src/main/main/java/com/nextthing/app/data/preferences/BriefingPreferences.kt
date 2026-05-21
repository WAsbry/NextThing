package com.nextthing.app.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BriefingPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private val Context.briefingDataStore by preferencesDataStore(name = "briefing_prefs")
        private val KEY_ENABLED = booleanPreferencesKey("briefing_enabled")
        private val KEY_MORNING_HOUR = intPreferencesKey("morning_hour")
        private val KEY_MORNING_MINUTE = intPreferencesKey("morning_minute")
        private val KEY_EVENING_HOUR = intPreferencesKey("evening_hour")
        private val KEY_EVENING_MINUTE = intPreferencesKey("evening_minute")
    }

    val enabled: Flow<Boolean> = context.briefingDataStore.data.map { it[KEY_ENABLED] ?: false }

    val morningHour: Flow<Int> = context.briefingDataStore.data.map { it[KEY_MORNING_HOUR] ?: 8 }
    val morningMinute: Flow<Int> = context.briefingDataStore.data.map { it[KEY_MORNING_MINUTE] ?: 0 }

    val eveningHour: Flow<Int> = context.briefingDataStore.data.map { it[KEY_EVENING_HOUR] ?: 21 }
    val eveningMinute: Flow<Int> = context.briefingDataStore.data.map { it[KEY_EVENING_MINUTE] ?: 0 }

    suspend fun isEnabledOnce(): Boolean = enabled.first()
    suspend fun getMorningHourOnce(): Int = morningHour.first()
    suspend fun getMorningMinuteOnce(): Int = morningMinute.first()
    suspend fun getEveningHourOnce(): Int = eveningHour.first()
    suspend fun getEveningMinuteOnce(): Int = eveningMinute.first()

    suspend fun setEnabled(enabled: Boolean) {
        context.briefingDataStore.edit { it[KEY_ENABLED] = enabled }
    }

    suspend fun setMorningTime(hour: Int, minute: Int) {
        context.briefingDataStore.edit {
            it[KEY_MORNING_HOUR] = hour
            it[KEY_MORNING_MINUTE] = minute
        }
    }

    suspend fun setEveningTime(hour: Int, minute: Int) {
        context.briefingDataStore.edit {
            it[KEY_EVENING_HOUR] = hour
            it[KEY_EVENING_MINUTE] = minute
        }
    }
}
