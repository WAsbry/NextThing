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

enum class AIProvider(
    val displayName: String,
    val baseUrl: String,
    val defaultModel: String
) {
    DEEPSEEK("DeepSeek", "https://api.deepseek.com/", "deepseek-chat"),
    QWEN("通义千问", "https://dashscope.aliyuncs.com/compatible-mode/v1/", "qwen-turbo")
}

@Singleton
class AIPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private val Context.aiDataStore by preferencesDataStore(name = "ai_prefs")
        private val KEY_PROVIDER = stringPreferencesKey("ai_provider")
        private val KEY_API_KEY = stringPreferencesKey("ai_api_key")
        private val KEY_MODEL = stringPreferencesKey("ai_model")
    }

    val provider: Flow<AIProvider> = context.aiDataStore.data.map { prefs ->
        val name = prefs[KEY_PROVIDER] ?: AIProvider.DEEPSEEK.name
        try { AIProvider.valueOf(name) } catch (_: Exception) { AIProvider.DEEPSEEK }
    }

    val apiKey: Flow<String> = context.aiDataStore.data.map { prefs ->
        prefs[KEY_API_KEY] ?: ""
    }

    val model: Flow<String> = context.aiDataStore.data.map { prefs ->
        prefs[KEY_MODEL] ?: ""
    }

    suspend fun getProviderOnce(): AIProvider = provider.first()
    suspend fun getApiKeyOnce(): String = apiKey.first()
    suspend fun getModelOnce(): String = model.first()

    suspend fun setProvider(provider: AIProvider) {
        context.aiDataStore.edit { it[KEY_PROVIDER] = provider.name }
    }

    suspend fun setApiKey(key: String) {
        context.aiDataStore.edit { it[KEY_API_KEY] = key }
    }

    suspend fun setModel(model: String) {
        context.aiDataStore.edit { it[KEY_MODEL] = model }
    }

    suspend fun isConfigured(): Boolean = getApiKeyOnce().isNotBlank()
}
