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

enum class ASRProvider(val displayName: String) {
    IFLY("讯飞"),
    ZHIPU("智谱")
}

@Singleton
class ASRPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private val Context.asrDataStore by preferencesDataStore(name = "asr_prefs")
        private val KEY_PROVIDER = stringPreferencesKey("asr_provider")
        private val KEY_ZHIPU_API_KEY = stringPreferencesKey("zhipu_api_key")
    }

    val provider: Flow<ASRProvider> = context.asrDataStore.data.map { prefs ->
        val name = prefs[KEY_PROVIDER] ?: ASRProvider.IFLY.name
        try { ASRProvider.valueOf(name) } catch (_: Exception) { ASRProvider.IFLY }
    }

    val zhipuApiKey: Flow<String> = context.asrDataStore.data.map { it[KEY_ZHIPU_API_KEY] ?: "" }

    suspend fun getProviderOnce(): ASRProvider = provider.first()
    suspend fun getZhipuApiKeyOnce(): String = zhipuApiKey.first()

    suspend fun setProvider(provider: ASRProvider) {
        context.asrDataStore.edit { it[KEY_PROVIDER] = provider.name }
    }

    suspend fun setZhipuApiKey(key: String) {
        context.asrDataStore.edit { it[KEY_ZHIPU_API_KEY] = key }
    }

    suspend fun isZhiPuConfigured(): Boolean = getZhipuApiKeyOnce().isNotBlank()
}
