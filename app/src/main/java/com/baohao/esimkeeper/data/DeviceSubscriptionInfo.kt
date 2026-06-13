package com.baohao.esimkeeper.data

data class DeviceSubscriptionInfo(
    val carrierName: String,
    val phoneNumber: String,
    val countryIso: String,
    val isEmbedded: Boolean,
    val slotIndex: Int,
    val subscriptionId: Int,
) {
    fun displayTitle(defaultEsimTitle: String, defaultSimTitle: String): String =
        carrierName.ifBlank {
            if (isEmbedded) defaultEsimTitle else defaultSimTitle
        }
}
