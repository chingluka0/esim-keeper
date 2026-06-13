package com.baohao.esimkeeper.data

data class CountryOption(
    val countryName: String,
    val englishName: String,
    val countryCode: String,
    val dialCode: String,
    val flagEmoji: String,
) {
    fun displayName(useChinese: Boolean): String = if (useChinese) countryName else englishName
}
