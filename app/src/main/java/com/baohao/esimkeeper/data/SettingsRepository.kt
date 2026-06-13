package com.baohao.esimkeeper.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "esim_keeper_settings")

class SettingsRepository(private val context: Context) {

    private object Keys {
        val DARK_MODE = booleanPreferencesKey("dark_mode")
        val SORT_ORDER = stringPreferencesKey("sort_order")
    }

    val isDarkMode: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.DARK_MODE] ?: false
    }

    val sortOrder: Flow<SortOrder> = context.dataStore.data.map { prefs ->
        prefs[Keys.SORT_ORDER]?.let { name ->
            runCatching { SortOrder.valueOf(name) }.getOrNull()
        } ?: SortOrder.EXPIRY_ASC
    }

    suspend fun setDarkMode(enabled: Boolean) {
        context.dataStore.edit { it[Keys.DARK_MODE] = enabled }
    }

    suspend fun setSortOrder(order: SortOrder) {
        context.dataStore.edit { it[Keys.SORT_ORDER] = order.name }
    }
}

enum class SortOrder(val label: String) {
    EXPIRY_ASC("按到期时间"),
    CREATED_DESC("按创建时间"),
    NAME_ASC("按名称"),
    BALANCE_DESC("按余额"),
}
