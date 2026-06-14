package com.baohao.esimkeeper.data

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class CardSorterTest {
    @Test
    fun expiryDateSortsEarliestFirst() {
        val cards = listOf(
            card(name = "Later", expiryDate = LocalDate.of(2026, 7, 1)),
            card(name = "Sooner", expiryDate = LocalDate.of(2026, 6, 1)),
            card(name = "Middle", expiryDate = LocalDate.of(2026, 6, 15)),
        )

        val sortedNames = CardSorter.sort(cards, CardSortOrder.ExpiryDate).map { it.name }

        assertEquals(listOf("Sooner", "Middle", "Later"), sortedNames)
    }

    @Test
    fun createdAtSortsNewestFirst() {
        val cards = listOf(
            card(name = "Old", createdAt = Instant.parse("2026-01-01T00:00:00Z")),
            card(name = "New", createdAt = Instant.parse("2026-03-01T00:00:00Z")),
            card(name = "Middle", createdAt = Instant.parse("2026-02-01T00:00:00Z")),
        )

        val sortedNames = CardSorter.sort(cards, CardSortOrder.CreatedAt).map { it.name }

        assertEquals(listOf("New", "Middle", "Old"), sortedNames)
    }

    @Test
    fun nameSortsAscendingIgnoringCase() {
        val cards = listOf(
            card(name = "delta"),
            card(name = "Alpha"),
            card(name = "beta"),
        )

        val sortedNames = CardSorter.sort(cards, CardSortOrder.Name).map { it.name }

        assertEquals(listOf("Alpha", "beta", "delta"), sortedNames)
    }

    private fun card(
        name: String,
        expiryDate: LocalDate = LocalDate.of(2026, 6, 1),
        createdAt: Instant = Instant.parse("2026-01-01T00:00:00Z"),
    ) = ESimCard(
        id = 0,
        name = name,
        phoneNumber = "+10000000000",
        countryName = "United States",
        countryCode = "US",
        flagEmoji = "US",
        balanceText = "Not set",
        startDate = LocalDate.of(2026, 1, 1),
        cycleDays = null,
        expiryDate = expiryDate,
        reminderDaysBefore = null,
        createdAt = createdAt,
        updatedAt = createdAt,
    )
}
