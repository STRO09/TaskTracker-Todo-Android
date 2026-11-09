package com.todoandroid.taskdiscipline.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_settings")

class UserPreferencesRepository(private val context: Context) {

    private object PreferencesKeys {
        val WIP_LIMIT = intPreferencesKey("wip_limit")
    }

    val wipLimitFlow: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.WIP_LIMIT] ?: 2
    }

    suspend fun setWipLimit(wipLimit: Int) {
        val validLimit = wipLimit.coerceIn(1, 3)
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.WIP_LIMIT] = validLimit
        }
    }
}
