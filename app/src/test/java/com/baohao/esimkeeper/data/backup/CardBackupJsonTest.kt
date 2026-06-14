package com.baohao.esimkeeper.data.backup

import com.baohao.esimkeeper.data.ESimCard
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class CardBackupJsonTest {
    @Test
    fun encodeWritesVersionExportTimeAndCards() {
        val json = CardBackupJson.encode(
            cards = listOf(card(name = "Travel SIM")),
            exportedAt = Instant.parse("2026-06-14T08:00:00Z"),
        )

        assertTrue(json.contains("\"version\": 1"))
        assertTrue(json.contains("\"exportedAt\": \"2026-06-14T08:00:00Z\""))
        assertTrue(json.contains("\"cards\""))
        assertTrue(json.contains("\"name\": \"Travel SIM\""))
    }

    @Test
    fun decodeRestoresCardsAsNewEntities() {
        val original = card(
            id = 42,
            name = "Japan eSIM",
            reminderDaysBefore = 3,
            createdAt = Instant.parse("2026-05-01T00:00:00Z"),
            updatedAt = Instant.parse("2026-05-02T00:00:00Z"),
        )
        val json = CardBackupJson.encode(
            cards = listOf(original),
            exportedAt = Instant.parse("2026-06-14T08:00:00Z"),
        )

        val imported = CardBackupJson.decodeCards(
            json = json,
            importedAt = Instant.parse("2026-06-14T09:00:00Z"),
        ).getOrThrow()

        assertEquals(1, imported.size)
        assertEquals(0, imported.single().id)
        assertEquals(original.name, imported.single().name)
        assertEquals(original.phoneNumber, imported.single().phoneNumber)
        assertEquals(original.countryName, imported.single().countryName)
        assertEquals(original.countryCode, imported.single().countryCode)
        assertEquals(original.flagEmoji, imported.single().flagEmoji)
        assertEquals(original.balanceText, imported.single().balanceText)
        assertEquals(original.startDate, imported.single().startDate)
        assertEquals(original.cycleDays, imported.single().cycleDays)
        assertEquals(original.expiryDate, imported.single().expiryDate)
        assertEquals(original.reminderDaysBefore, imported.single().reminderDaysBefore)
        assertEquals(original.createdAt, imported.single().createdAt)
        assertEquals(original.updatedAt, imported.single().updatedAt)
    }

    @Test
    fun decodeRejectsMalformedJson() {
        val result = CardBackupJson.decodeCards(
            json = "{not json",
            importedAt = Instant.parse("2026-06-14T09:00:00Z"),
        )

        assertTrue(result.isFailure)
    }

    @Test
    fun decodeRejectsUnsupportedVersion() {
        val result = CardBackupJson.decodeCards(
            json = """
                {
                  "version": 2,
                  "exportedAt": "2026-06-14T08:00:00Z",
                  "cards": []
                }
            """.trimIndent(),
            importedAt = Instant.parse("2026-06-14T09:00:00Z"),
        )

        assertTrue(result.isFailure)
    }

    private fun card(
        id: Long = 0,
        name: String,
        reminderDaysBefore: Int? = null,
        createdAt: Instant = Instant.parse("2026-01-01T00:00:00Z"),
        updatedAt: Instant = Instant.parse("2026-01-02T00:00:00Z"),
    ) = ESimCard(
        id = id,
        name = name,
        phoneNumber = "+819012345678",
        countryName = "Japan",
        countryCode = "JP",
        flagEmoji = "JP",
        balanceText = "JPY 1000",
        startDate = LocalDate.of(2026, 1, 1),
        cycleDays = 90,
        expiryDate = LocalDate.of(2026, 4, 1),
        reminderDaysBefore = reminderDaysBefore,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
}
