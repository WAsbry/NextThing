package com.nextthing.app.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.syncDataStore by preferencesDataStore(name = "sync_prefs")

@Singleton
class SyncPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private val LAST_SYNC_TIMESTAMP = longPreferencesKey("last_sync_timestamp")
    }

    val lastSyncTimestamp: Flow<Long?> = context.syncDataStore.data.map { it[LAST_SYNC_TIMESTAMP] }

    suspend fun saveLastSyncTimestamp(timestamp: Long) {
        context.syncDataStore.edit { it[LAST_SYNC_TIMESTAMP] = timestamp }
    }

    suspend fun clear() {
        context.syncDataStore.edit { it.clear() }
    }
}
