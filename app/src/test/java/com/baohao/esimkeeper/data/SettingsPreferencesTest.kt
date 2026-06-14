package com.baohao.esimkeeper.data

import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.preferencesOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsPreferencesTest {
    @Test
    fun emptyPreferencesUseDefaults() {
        val settings = SettingsPreferences.toSettings(emptyPreferences())

        assertFalse(settings.isDarkMode)
        assertEquals(CardSortOrder.default, settings.sortOrder)
    }

    @Test
    fun preferencesRestoreDarkModeAndSortOrder() {
        val settings = SettingsPreferences.toSettings(
            preferencesOf(
                SettingsPreferences.darkModeKey to true,
                SettingsPreferences.sortOrderKey to CardSortOrder.Name.preferenceValue,
            ),
        )

        assertTrue(settings.isDarkMode)
        assertEquals(CardSortOrder.Name, settings.sortOrder)
    }

    @Test
    fun invalidSortOrderFallsBackToDefault() {
        val settings = SettingsPreferences.toSettings(
            preferencesOf(SettingsPreferences.sortOrderKey to "balance"),
        )

        assertEquals(CardSortOrder.default, settings.sortOrder)
    }
}
