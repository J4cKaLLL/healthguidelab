package com.healthguidelab.keto365.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "session_prefs")

class SessionStore(private val context: Context) {
    companion object {
        private val HAS_LOGGED_ONCE = booleanPreferencesKey("has_logged_once")
    }

    val hasLoggedOnce: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[HAS_LOGGED_ONCE] ?: false
    }

    suspend fun setLoggedOnce(value: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[HAS_LOGGED_ONCE] = value
        }
    }
}
