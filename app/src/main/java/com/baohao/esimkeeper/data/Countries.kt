package com.baohao.esimkeeper.data

object Countries {
    val common = listOf(
        CountryOption("中国大陆", "Mainland China", "CN", "+86", "🇨🇳"),
        CountryOption("香港", "Hong Kong", "HK", "+852", "🇭🇰"),
        CountryOption("澳门", "Macao", "MO", "+853", "🇲🇴"),
        CountryOption("台湾", "Taiwan", "TW", "+886", "🇹🇼"),
        CountryOption("美国", "United States", "US", "+1", "🇺🇸"),
        CountryOption("加拿大", "Canada", "CA", "+1", "🇨🇦"),
        CountryOption("英国", "United Kingdom", "GB", "+44", "🇬🇧"),
        CountryOption("法国", "France", "FR", "+33", "🇫🇷"),
        CountryOption("德国", "Germany", "DE", "+49", "🇩🇪"),
        CountryOption("意大利", "Italy", "IT", "+39", "🇮🇹"),
        CountryOption("西班牙", "Spain", "ES", "+34", "🇪🇸"),
        CountryOption("荷兰", "Netherlands", "NL", "+31", "🇳🇱"),
        CountryOption("瑞士", "Switzerland", "CH", "+41", "🇨🇭"),
        CountryOption("土耳其", "Turkey", "TR", "+90", "🇹🇷"),
        CountryOption("日本", "Japan", "JP", "+81", "🇯🇵"),
        CountryOption("韩国", "South Korea", "KR", "+82", "🇰🇷"),
        CountryOption("新加坡", "Singapore", "SG", "+65", "🇸🇬"),
        CountryOption("马来西亚", "Malaysia", "MY", "+60", "🇲🇾"),
        CountryOption("泰国", "Thailand", "TH", "+66", "🇹🇭"),
        CountryOption("越南", "Vietnam", "VN", "+84", "🇻🇳"),
        CountryOption("菲律宾", "Philippines", "PH", "+63", "🇵🇭"),
        CountryOption("印度尼西亚", "Indonesia", "ID", "+62", "🇮🇩"),
        CountryOption("印度", "India", "IN", "+91", "🇮🇳"),
        CountryOption("阿联酋", "United Arab Emirates", "AE", "+971", "🇦🇪"),
        CountryOption("沙特阿拉伯", "Saudi Arabia", "SA", "+966", "🇸🇦"),
        CountryOption("澳大利亚", "Australia", "AU", "+61", "🇦🇺"),
        CountryOption("新西兰", "New Zealand", "NZ", "+64", "🇳🇿"),
        CountryOption("墨西哥", "Mexico", "MX", "+52", "🇲🇽"),
        CountryOption("巴西", "Brazil", "BR", "+55", "🇧🇷"),
    )

    fun search(query: String, useChinese: Boolean): List<CountryOption> {
        val keyword = query.trim()
        if (keyword.isBlank()) return common
        return common.filter { option ->
            option.displayName(useChinese).contains(keyword, ignoreCase = true) ||
                option.countryName.contains(keyword, ignoreCase = true) ||
                option.englishName.contains(keyword, ignoreCase = true) ||
                option.countryCode.contains(keyword, ignoreCase = true) ||
                option.dialCode.contains(keyword)
        }
    }

    fun findByIso(countryIso: String): CountryOption? {
        val normalized = countryIso.trim().uppercase()
        return common.firstOrNull { it.countryCode == normalized }
    }
}
