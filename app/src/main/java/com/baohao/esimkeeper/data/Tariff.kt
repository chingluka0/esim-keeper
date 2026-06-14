package com.baohao.esimkeeper.data

/**
 * Per-card "number charges" the user can fill in freely (e.g. "£1/min",
 * "30p/SMS", "Free", "Not supported"). Stored as plain text so any currency,
 * unit or note is allowed. Embedded into [ESimCard] with the `tariff_` column
 * prefix and persisted in Room + JSON backup.
 */
data class Tariff(
    val outgoingCall: String = "",
    val incomingCall: String = "",
    val outgoingSms: String = "",
    val incomingSms: String = "",
    val dataTraffic: String = "",
) {
    companion object {
        val EMPTY = Tariff()
    }
}
