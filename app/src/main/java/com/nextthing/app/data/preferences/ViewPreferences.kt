package com.nextthing.app.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.viewDataStore by preferencesDataStore(name = "view_prefs")

@Singleton
class ViewPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val KEY_COLLAPSE_OVERDUE = booleanPreferencesKey("collapse_overdue")
    private val KEY_COLLAPSE_FUTURE = booleanPreferencesKey("collapse_future")

    val collapseOverdue: Flow<Boolean> = context.viewDataStore.data
        .map { it[KEY_COLLAPSE_OVERDUE] ?: false }

    val collapseFuture: Flow<Boolean> = context.viewDataStore.data
        .map { it[KEY_COLLAPSE_FUTURE] ?: false }

    suspend fun setCollapseOverdue(enabled: Boolean) {
        context.viewDataStore.edit { it[KEY_COLLAPSE_OVERDUE] = enabled }
    }

    suspend fun setCollapseFuture(enabled: Boolean) {
        context.viewDataStore.edit { it[KEY_COLLAPSE_FUTURE] = enabled }
    }
}
