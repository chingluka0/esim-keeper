package com.baohao.esimkeeper.data

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant
import java.time.LocalDate

@Entity(tableName = "esim_cards")
data class ESimCard(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val phoneNumber: String,
    val countryName: String,
    val countryCode: String,
    val flagEmoji: String,
    val balanceText: String,
    val startDate: LocalDate,
    val cycleDays: Int?,
    val expiryDate: LocalDate,
    val reminderDaysBefore: Int?,
    val createdAt: Instant,
    val updatedAt: Instant,
    @Embedded(prefix = "tariff_")
    val tariff: Tariff = Tariff.EMPTY,
)
