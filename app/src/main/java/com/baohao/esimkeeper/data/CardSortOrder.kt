package com.baohao.esimkeeper.data

enum class CardSortOrder(val preferenceValue: String) {
    ExpiryDate("expiry_date"),
    CreatedAt("created_at"),
    Name("name");

    companion object {
        val default: CardSortOrder = ExpiryDate

        fun fromPreferenceValue(value: String?): CardSortOrder =
            entries.firstOrNull { it.preferenceValue == value } ?: default
    }
}
