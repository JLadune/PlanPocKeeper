package com.example.planpockeeper.utils

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

object PreferencesKeys {
    val DARK_MODE = booleanPreferencesKey("dark_mode")
    val NOTIF_PERIOD_END = booleanPreferencesKey("notif_period_end")
    val NOTIF_NO_EXPENSE = booleanPreferencesKey("notif_no_expense")
    val CURRENCY = stringPreferencesKey("currency")
}

class PreferencesManager(private val context: Context) {

    val darkMode: Flow<Boolean> = context.dataStore.data
        .map { it[PreferencesKeys.DARK_MODE] ?: false }

    val notifPeriodEnd: Flow<Boolean> = context.dataStore.data
        .map { it[PreferencesKeys.NOTIF_PERIOD_END] ?: true }

    val notifNoExpense: Flow<Boolean> = context.dataStore.data
        .map { it[PreferencesKeys.NOTIF_NO_EXPENSE] ?: true }

    val currency: Flow<String> = context.dataStore.data
        .map { it[PreferencesKeys.CURRENCY] ?: "EUR" }

    suspend fun setDarkMode(enabled: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.DARK_MODE] = enabled }
    }

    suspend fun setNotifPeriodEnd(enabled: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.NOTIF_PERIOD_END] = enabled }
    }

    suspend fun setNotifNoExpense(enabled: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.NOTIF_NO_EXPENSE] = enabled }
    }

    suspend fun setCurrency(currency: String) {
        context.dataStore.edit { it[PreferencesKeys.CURRENCY] = currency }
    }
}