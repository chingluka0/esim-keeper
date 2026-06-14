package com.baohao.esimkeeper.data.backup

import com.baohao.esimkeeper.data.ESimCard
import com.baohao.esimkeeper.data.Tariff
import com.google.gson.GsonBuilder
import com.google.gson.JsonParseException
import java.time.Instant
import java.time.LocalDate

object CardBackupJson {
    private const val CURRENT_VERSION = 1

    private val gson = GsonBuilder()
        .disableHtmlEscaping()
        .setPrettyPrinting()
        .create()

    fun encode(cards: List<ESimCard>, exportedAt: Instant): String =
        gson.toJson(
            BackupFileDto(
                version = CURRENT_VERSION,
                exportedAt = exportedAt.toString(),
                cards = cards.map { it.toBackupDto() },
            ),
        )

    fun decodeCards(json: String, importedAt: Instant): Result<List<ESimCard>> =
        runCatching {
            val file = try {
                gson.fromJson(json, BackupFileDto::class.java)
            } catch (exception: RuntimeException) {
                throw JsonParseException("Invalid backup JSON", exception)
            } ?: throw JsonParseException("Backup JSON is empty")

            if (file.version != CURRENT_VERSION) {
                throw JsonParseException("Unsupported backup version")
            }

            file.cards?.map { it.toCard(importedAt) } ?: throw JsonParseException("Backup cards are missing")
        }

    private fun ESimCard.toBackupDto(): BackupCardDto =
        BackupCardDto(
            name = name,
            phoneNumber = phoneNumber,
            countryName = countryName,
            countryCode = countryCode,
            flagEmoji = flagEmoji,
            balanceText = balanceText,
            startDate = startDate.toString(),
            cycleDays = cycleDays,
            expiryDate = expiryDate.toString(),
            reminderDaysBefore = reminderDaysBefore,
            createdAt = createdAt.toString(),
            updatedAt = updatedAt.toString(),
            tariffOutgoingCall = tariff.outgoingCall,
            tariffIncomingCall = tariff.incomingCall,
            tariffOutgoingSms = tariff.outgoingSms,
            tariffIncomingSms = tariff.incomingSms,
            tariffDataTraffic = tariff.dataTraffic,
        )

    private fun BackupCardDto.toCard(importedAt: Instant): ESimCard =
        ESimCard(
            id = 0,
            name = requiredText(name, "name"),
            phoneNumber = phoneNumber.orEmpty(),
            countryName = requiredText(countryName, "countryName"),
            countryCode = requiredText(countryCode, "countryCode"),
            flagEmoji = flagEmoji.orEmpty(),
            balanceText = requiredText(balanceText, "balanceText"),
            startDate = parseDate(startDate, "startDate"),
            cycleDays = cycleDays,
            expiryDate = parseDate(expiryDate, "expiryDate"),
            reminderDaysBefore = reminderDaysBefore,
            createdAt = parseInstantOrDefault(createdAt, importedAt),
            updatedAt = parseInstantOrDefault(updatedAt, importedAt),
            tariff = Tariff(
                outgoingCall = tariffOutgoingCall.orEmpty(),
                incomingCall = tariffIncomingCall.orEmpty(),
                outgoingSms = tariffOutgoingSms.orEmpty(),
                incomingSms = tariffIncomingSms.orEmpty(),
                dataTraffic = tariffDataTraffic.orEmpty(),
            ),
        )

    private fun requiredText(value: String?, field: String): String =
        value?.takeIf { it.isNotBlank() } ?: throw JsonParseException("Missing $field")

    private fun parseDate(value: String?, field: String): LocalDate =
        runCatching { LocalDate.parse(requiredText(value, field)) }
            .getOrElse { throw JsonParseException("Invalid $field", it) }

    private fun parseInstantOrDefault(value: String?, defaultValue: Instant): Instant =
        value?.let { runCatching { Instant.parse(it) }.getOrNull() } ?: defaultValue
}

data class BackupFileDto(
    val version: Int?,
    val exportedAt: String?,
    val cards: List<BackupCardDto>?,
)

data class BackupCardDto(
    val name: String?,
    val phoneNumber: String?,
    val countryName: String?,
    val countryCode: String?,
    val flagEmoji: String?,
    val balanceText: String?,
    val startDate: String?,
    val cycleDays: Int?,
    val expiryDate: String?,
    val reminderDaysBefore: Int?,
    val createdAt: String?,
    val updatedAt: String?,
    // Per-card number charges. Optional/nullable so backups created before this
    // field existed still import cleanly (missing values default to empty).
    val tariffOutgoingCall: String? = null,
    val tariffIncomingCall: String? = null,
    val tariffOutgoingSms: String? = null,
    val tariffIncomingSms: String? = null,
    val tariffDataTraffic: String? = null,
)
