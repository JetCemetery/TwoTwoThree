package com.jetcemetery.twotwothree

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

class SettingsManager(private val context: Context) {
    companion object {
        val WORK_DAY_LABEL = stringPreferencesKey("work_day_label")
        val OFF_DAY_LABEL = stringPreferencesKey("off_day_label")
    }

    val workDayLabel: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[WORK_DAY_LABEL] ?: "Work Day"
    }

    val offDayLabel: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[OFF_DAY_LABEL] ?: "Off Day"
    }

    suspend fun updateWorkDayLabel(label: String) {
        context.dataStore.edit { preferences ->
            preferences[WORK_DAY_LABEL] = label
        }
    }

    suspend fun updateOffDayLabel(label: String) {
        context.dataStore.edit { preferences ->
            preferences[OFF_DAY_LABEL] = label
        }
    }
}
