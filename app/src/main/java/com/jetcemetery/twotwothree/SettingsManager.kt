package com.jetcemetery.twotwothree

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.LocalDate

private val Context.dataStore by preferencesDataStore(name = "settings")

class SettingsManager(private val context: Context) {
    companion object {
        val WORK_DAY_LABEL = stringPreferencesKey("work_day_label")
        val OFF_DAY_LABEL = stringPreferencesKey("off_day_label")
        val SWITCH_DATES = stringSetPreferencesKey("switch_dates")
        val SCHEDULE_TYPE = stringPreferencesKey("schedule_type")
    }

    val workDayLabel: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[WORK_DAY_LABEL] ?: "Work Day"
    }

    val offDayLabel: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[OFF_DAY_LABEL] ?: "Off Day"
    }

    val switchDates: Flow<Set<LocalDate>> = context.dataStore.data.map { preferences ->
        preferences[SWITCH_DATES]?.map { LocalDate.parse(it) }?.toSet() ?: emptySet()
    }

    val scheduleType: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[SCHEDULE_TYPE] ?: "2-2-3"
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

    suspend fun updateScheduleType(type: String) {
        context.dataStore.edit { preferences ->
            preferences[SCHEDULE_TYPE] = type
            preferences.remove(SWITCH_DATES) // Clear history as requested
        }
    }

    suspend fun toggleSwitchDate(date: LocalDate) {
        context.dataStore.edit { preferences ->
            val current = preferences[SWITCH_DATES]?.toMutableSet() ?: mutableSetOf()
            val dateStr = date.toString()
            if (current.contains(dateStr)) {
                current.remove(dateStr)
            } else {
                current.add(dateStr)
            }
            preferences[SWITCH_DATES] = current
        }
    }
}
