package com.nextthing.app.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ASRPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private val Context.asrDataStore by preferencesDataStore(name = "asr_prefs")
        private val KEY_PROVIDER = stringPreferencesKey("asr_provider")
    }

    // 端侧 ASR 始终可用
    val provider: Flow<String> = context.asrDataStore.data.map { prefs ->
        prefs[KEY_PROVIDER] ?: "SHERPA"
    }

    suspend fun getProviderOnce(): String = provider.first()

    suspend fun setProvider(provider: String) {
        context.asrDataStore.edit { it[KEY_PROVIDER] = provider }
    }
}
