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
class IFlyPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private val Context.iflyDataStore by preferencesDataStore(name = "ifly_prefs")
        private val KEY_APP_ID     = stringPreferencesKey("ifly_app_id")
        private val KEY_API_KEY    = stringPreferencesKey("ifly_api_key")
        private val KEY_API_SECRET = stringPreferencesKey("ifly_api_secret")
        // 方言 accent 代码：mandarin=普通话, lmz=四川话, cantonese=粤语
        // 完整列表参考讯飞文档：https://www.xfyun.cn/doc/asr/voicedictation/API.html
        private val KEY_ACCENT     = stringPreferencesKey("ifly_accent")
    }

    val appId: Flow<String>     = context.iflyDataStore.data.map { it[KEY_APP_ID]     ?: "" }
    val apiKey: Flow<String>    = context.iflyDataStore.data.map { it[KEY_API_KEY]    ?: "" }
    val apiSecret: Flow<String> = context.iflyDataStore.data.map { it[KEY_API_SECRET] ?: "" }
    val accent: Flow<String>    = context.iflyDataStore.data.map { it[KEY_ACCENT]     ?: "lmz" }

    suspend fun getAppIdOnce()     = appId.first()
    suspend fun getApiKeyOnce()    = apiKey.first()
    suspend fun getApiSecretOnce() = apiSecret.first()
    suspend fun getAccentOnce()    = accent.first()

    suspend fun setAppId(v: String)     { context.iflyDataStore.edit { it[KEY_APP_ID]     = v } }
    suspend fun setApiKey(v: String)    { context.iflyDataStore.edit { it[KEY_API_KEY]    = v } }
    suspend fun setApiSecret(v: String) { context.iflyDataStore.edit { it[KEY_API_SECRET] = v } }
    suspend fun setAccent(v: String)    { context.iflyDataStore.edit { it[KEY_ACCENT]     = v } }

    suspend fun isConfigured(): Boolean =
        getAppIdOnce().isNotBlank() && getApiKeyOnce().isNotBlank() && getApiSecretOnce().isNotBlank()
}
