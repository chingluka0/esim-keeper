package com.baohao.esimkeeper.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private const val LEGACY_PREFERENCES_NAME = "esim_hub_preferences"
private const val SETTINGS_DATASTORE_NAME = "esim_keeper_settings"

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = SETTINGS_DATASTORE_NAME,
    produceMigrations = { context ->
        listOf(
            SharedPreferencesMigration(
                context = context,
                sharedPreferencesName = LEGACY_PREFERENCES_NAME,
                keysToMigrate = SettingsPreferences.legacyKeys,
            ),
        )
    },
)

data class AppSettings(
    val isDarkMode: Boolean,
    val sortOrder: CardSortOrder,
)

object SettingsPreferences {
    val darkModeKey = booleanPreferencesKey("dark_mode")
    val sortOrderKey = stringPreferencesKey("sort_order")
    val legacyKeys: Set<String> = setOf(darkModeKey.name, sortOrderKey.name)

    fun toSettings(preferences: Preferences): AppSettings =
        AppSettings(
            isDarkMode = preferences[darkModeKey] ?: false,
            sortOrder = CardSortOrder.fromPreferenceValue(preferences[sortOrderKey]),
        )
}

class SettingsRepository(
    context: Context,
    private val dataStore: DataStore<Preferences> = context.settingsDataStore,
) {
    val settings: Flow<AppSettings> = dataStore.data.map(SettingsPreferences::toSettings)

    suspend fun setDarkMode(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[SettingsPreferences.darkModeKey] = enabled
        }
    }

    suspend fun setSortOrder(order: CardSortOrder) {
        dataStore.edit { preferences ->
            preferences[SettingsPreferences.sortOrderKey] = order.preferenceValue
        }
    }
}
