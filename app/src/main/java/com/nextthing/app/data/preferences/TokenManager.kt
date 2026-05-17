package com.nextthing.app.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.tokenDataStore by preferencesDataStore(name = "auth_tokens")

@Singleton
class TokenManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private val ACCESS_TOKEN_KEY = stringPreferencesKey("access_token")
        private val REFRESH_TOKEN_KEY = stringPreferencesKey("refresh_token")
        private val SERVER_USER_ID_KEY = stringPreferencesKey("server_user_id")
    }

    val accessToken: Flow<String?> = context.tokenDataStore.data.map { it[ACCESS_TOKEN_KEY] }
    val refreshToken: Flow<String?> = context.tokenDataStore.data.map { it[REFRESH_TOKEN_KEY] }

    suspend fun saveTokens(accessToken: String, refreshToken: String) {
        context.tokenDataStore.edit { prefs ->
            prefs[ACCESS_TOKEN_KEY] = accessToken
            prefs[REFRESH_TOKEN_KEY] = refreshToken
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
}
