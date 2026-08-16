package com.nextthing.app.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import com.nextthing.app.data.security.KeystoreSecretCipher
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
    DEEPSEEK("DeepSeek", "https://api.deepseek.com/", "deepseek-v4-flash"),
    QWEN("通义千问", "https://dashscope.aliyuncs.com/compatible-mode/v1/", "qwen-turbo"),
    ZHIPU("智谱", "https://open.bigmodel.cn/api/paas/v4/", "glm-4.5-air");

    fun resolveModel(configuredModel: String): String {
        val normalized = configuredModel.trim()
        if (normalized.isBlank()) return defaultModel
        if (this == DEEPSEEK && normalized in LEGACY_DEEPSEEK_MODELS) return defaultModel
        return normalized
    }

    private companion object {
        val LEGACY_DEEPSEEK_MODELS = setOf("deepseek-chat", "deepseek-reasoner")
    }
}

@Singleton
class AIPreferences @Inject constructor(
    @ApplicationContext private val context: Context,
    private val secretCipher: KeystoreSecretCipher
) {
    companion object {
        private val Context.aiDataStore by preferencesDataStore(name = "ai_prefs")
        private val KEY_PROVIDER = stringPreferencesKey("ai_provider")
        private val KEY_API_KEY = stringPreferencesKey("ai_api_key")
        private val KEY_MODEL = stringPreferencesKey("ai_model")
        private const val API_KEY_AAD = "ai_api_key"
    }

    val provider: Flow<AIProvider> = context.aiDataStore.data.map { prefs ->
        val name = prefs[KEY_PROVIDER] ?: AIProvider.DEEPSEEK.name
        try { AIProvider.valueOf(name) } catch (_: Exception) { AIProvider.DEEPSEEK }
    }

    val apiKey: Flow<String> = context.aiDataStore.data.map { prefs ->
        decodeApiKey(prefs[KEY_API_KEY].orEmpty())
    }

    val model: Flow<String> = context.aiDataStore.data.map { prefs ->
        prefs[KEY_MODEL] ?: ""
    }

    suspend fun getProviderOnce(): AIProvider = provider.first()
    suspend fun getApiKeyOnce(): String {
        val stored = context.aiDataStore.data.first()[KEY_API_KEY].orEmpty()
        if (stored.isBlank()) return ""
        return try {
            val plainText = secretCipher.decrypt(stored, API_KEY_AAD)
            if (!secretCipher.isEncrypted(stored)) {
                setApiKey(plainText)
            }
            plainText
        } catch (_: Exception) {
            context.aiDataStore.edit { it.remove(KEY_API_KEY) }
            ""
        }
    }
    suspend fun getModelOnce(): String = model.first()

    suspend fun setProvider(provider: AIProvider) {
        context.aiDataStore.edit { it[KEY_PROVIDER] = provider.name }
    }

    suspend fun setApiKey(key: String) {
        val normalized = key.trim()
        context.aiDataStore.edit { prefs ->
            if (normalized.isBlank()) {
                prefs.remove(KEY_API_KEY)
            } else {
                prefs[KEY_API_KEY] = secretCipher.encrypt(normalized, API_KEY_AAD)
            }
        }
    }

    suspend fun setModel(model: String) {
        context.aiDataStore.edit { it[KEY_MODEL] = model }
    }

    suspend fun isConfigured(): Boolean = getApiKeyOnce().isNotBlank()

    private fun decodeApiKey(stored: String): String {
        if (stored.isBlank()) return ""
        return runCatching { secretCipher.decrypt(stored, API_KEY_AAD) }.getOrDefault("")
    }

}
