package com.nextthing.app.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.nextthing.app.data.security.KeystoreSecretCipher
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.tokenDataStore by preferencesDataStore(name = "auth_tokens")

@Singleton
class TokenManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val secretCipher: KeystoreSecretCipher
) {
    companion object {
        private val ACCESS_TOKEN_KEY = stringPreferencesKey("access_token")
        private val REFRESH_TOKEN_KEY = stringPreferencesKey("refresh_token")
        private val SERVER_USER_ID_KEY = stringPreferencesKey("server_user_id")
        private const val ACCESS_TOKEN_AAD = "auth_access_token"
        private const val REFRESH_TOKEN_AAD = "auth_refresh_token"
    }

    val accessToken: Flow<String?> = context.tokenDataStore.data.map {
        decodeSecret(it[ACCESS_TOKEN_KEY], ACCESS_TOKEN_AAD)
    }
    val refreshToken: Flow<String?> = context.tokenDataStore.data.map {
        decodeSecret(it[REFRESH_TOKEN_KEY], REFRESH_TOKEN_AAD)
    }

    suspend fun saveTokens(accessToken: String, refreshToken: String) {
        context.tokenDataStore.edit { prefs ->
            prefs[ACCESS_TOKEN_KEY] = secretCipher.encrypt(accessToken, ACCESS_TOKEN_AAD)
            prefs[REFRESH_TOKEN_KEY] = secretCipher.encrypt(refreshToken, REFRESH_TOKEN_AAD)
        }
    }

    suspend fun getAccessTokenOnce(): String? {
        val prefs = context.tokenDataStore.data.first()
        val storedAccess = prefs[ACCESS_TOKEN_KEY] ?: return null
        return try {
            val plainAccess = secretCipher.decrypt(storedAccess, ACCESS_TOKEN_AAD)
            val storedRefresh = prefs[REFRESH_TOKEN_KEY]
            if (!secretCipher.isEncrypted(storedAccess) ||
                (storedRefresh != null && !secretCipher.isEncrypted(storedRefresh))
            ) {
                val plainRefresh = storedRefresh?.let {
                    secretCipher.decrypt(it, REFRESH_TOKEN_AAD)
                }.orEmpty()
                saveTokens(plainAccess, plainRefresh)
            }
            plainAccess
        } catch (_: Exception) {
            clear()
            null
        }
    }

    suspend fun saveServerUserId(userId: Long) {
        context.tokenDataStore.edit { prefs ->
            prefs[SERVER_USER_ID_KEY] = userId.toString()
        }
    }

    val serverUserId: Flow<Long?> = context.tokenDataStore.data.map {
        it[SERVER_USER_ID_KEY]?.toLongOrNull()
    }

    suspend fun clear() {
        context.tokenDataStore.edit { prefs ->
            prefs.clear()
        }
    }

    private fun decodeSecret(value: String?, associatedData: String): String? {
        if (value.isNullOrBlank()) return null
        return runCatching { secretCipher.decrypt(value, associatedData) }.getOrNull()
    }

}
