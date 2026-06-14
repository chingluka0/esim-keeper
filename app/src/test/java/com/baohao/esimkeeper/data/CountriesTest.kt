package com.baohao.esimkeeper.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CountriesTest {
    @Test
    fun usPlusOneNumberResolvesToUnitedStates() {
        val option = Countries.findByPhoneNumber("+1 415 555 0123")
        assertEquals("US", option?.countryCode)
    }

    @Test
    fun hongKongNumberResolvesToHongKong() {
        val option = Countries.findByPhoneNumber("+852 9123 4567")
        assertEquals("HK", option?.countryCode)
    }

    @Test
    fun longestPrefixWinsOverShorterCode() {
        // +852 must not be shadowed by a shorter code.
        val option = Countries.findByPhoneNumber("+85291234567")
        assertEquals("HK", option?.countryCode)
    }

    @Test
    fun numberWithoutPlusReturnsNull() {
        assertNull(Countries.findByPhoneNumber("4155550123"))
    }

    @Test
    fun blankNumberReturnsNull() {
        assertNull(Countries.findByPhoneNumber(""))
    }
}
