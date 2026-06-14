package com.baohao.esimkeeper.data

import org.junit.Assert.assertEquals
import org.junit.Test

class CardSortOrderTest {
    @Test
    fun defaultOrderIsExpiryDate() {
        assertEquals(CardSortOrder.ExpiryDate, CardSortOrder.default)
    }

    @Test
    fun preferenceValueRestoresSortOrder() {
        assertEquals(CardSortOrder.CreatedAt, CardSortOrder.fromPreferenceValue("created_at"))
        assertEquals(CardSortOrder.Name, CardSortOrder.fromPreferenceValue("name"))
        assertEquals(CardSortOrder.ExpiryDate, CardSortOrder.fromPreferenceValue("expiry_date"))
    }

    @Test
    fun invalidPreferenceValueFallsBackToDefault() {
        assertEquals(CardSortOrder.default, CardSortOrder.fromPreferenceValue(null))
        assertEquals(CardSortOrder.default, CardSortOrder.fromPreferenceValue(""))
        assertEquals(CardSortOrder.default, CardSortOrder.fromPreferenceValue("balance"))
    }
}
