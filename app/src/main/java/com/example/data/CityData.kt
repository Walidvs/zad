package com.example.data

data class City(
    val id: String,
    val nameAr: String,
    val countryAr: String,
    val latitude: Double,
    val longitude: Double,
    val calculationMethod: CalculationMethod = CalculationMethod.UMM_AL_QURA
)

object CityData {
    val defaultCity = City("makkah", "مكة المكرمة", "المملكة العربية السعودية", 21.4225, 39.8262, CalculationMethod.UMM_AL_QURA)

    val popularCities = listOf(
        defaultCity,
        City("madinah", "المدينة المنورة", "المملكة العربية السعودية", 24.4672, 39.6111, CalculationMethod.UMM_AL_QURA),
        City("riyadh", "الرياض", "المملكة العربية السعودية", 24.7136, 46.6753, CalculationMethod.UMM_AL_QURA),
        City("jeddah", "جدة", "المملكة العربية السعودية", 21.4858, 39.1925, CalculationMethod.UMM_AL_QURA),
        City("jerusalem", "القدس الشريف", "فلسطين", 31.7683, 35.2137, CalculationMethod.MUSLIM_WORLD_LEAGUE),
        City("gaza", "غزة", "فلسطين", 31.5017, 34.4668, CalculationMethod.MUSLIM_WORLD_LEAGUE),
        City("cairo", "القاهرة", "مصر", 30.0444, 31.2357, CalculationMethod.EGYPTIAN),
        City("alexandria", "الإسكندرية", "مصر", 31.2001, 29.9187, CalculationMethod.EGYPTIAN),
        City("dubai", "دبي", "الإمارات العربية المتحدة", 25.2048, 55.2708, CalculationMethod.UMM_AL_QURA),
        City("abu_dhabi", "أبو ظبي", "الإمارات العربية المتحدة", 24.4539, 54.3773, CalculationMethod.UMM_AL_QURA),
        City("casablanca", "الدار البيضاء", "المغرب", 33.5731, -7.5898, CalculationMethod.MUSLIM_WORLD_LEAGUE),
        City("rabat", "الرباط", "المغرب", 34.0209, -6.8416, CalculationMethod.MUSLIM_WORLD_LEAGUE),
        City("algiers", "الجزائر العاصمة", "الجزائر", 36.7538, 3.0588, CalculationMethod.MUSLIM_WORLD_LEAGUE),
        City("tunis", "تونس", "تونس", 36.8065, 10.1815, CalculationMethod.MUSLIM_WORLD_LEAGUE),
        City("tripoli", "طرابلس", "ليبيا", 32.8872, 13.1913, CalculationMethod.MUSLIM_WORLD_LEAGUE),
        City("khartoum", "الخرطوم", "السودان", 15.5007, 32.5599, CalculationMethod.MUSLIM_WORLD_LEAGUE),
        City("amman", "عمان", "الأردن", 31.9454, 35.9284, CalculationMethod.MUSLIM_WORLD_LEAGUE),
        City("damascus", "دمشق", "سوريا", 33.5138, 36.2765, CalculationMethod.MUSLIM_WORLD_LEAGUE),
        City("beirut", "بيروت", "لبنان", 33.8938, 35.5018, CalculationMethod.MUSLIM_WORLD_LEAGUE),
        City("baghdad", "بغداد", "العراق", 33.3152, 44.3661, CalculationMethod.MUSLIM_WORLD_LEAGUE),
        City("kuwait", "مدينة الكويت", "الكويت", 29.3759, 47.9774, CalculationMethod.UMM_AL_QURA),
        City("doha", "الدوحة", "قطر", 25.2854, 51.5310, CalculationMethod.UMM_AL_QURA),
        City("manama", "المنامة", "البحرين", 26.2285, 50.5860, CalculationMethod.UMM_AL_QURA),
        City("muscat", "مسقط", "عُمان", 23.5880, 58.3829, CalculationMethod.UMM_AL_QURA),
        City("sanaa", "صنعاء", "اليمن", 15.3694, 44.1910, CalculationMethod.UMM_AL_QURA),
        City("istanbul", "إسطنبول", "تركيا", 41.0082, 28.9784, CalculationMethod.MUSLIM_WORLD_LEAGUE),
        City("london", "لندن", "المملكة المتحدة", 51.5074, -0.1278, CalculationMethod.ISNA),
        City("paris", "باريس", "فرنسا", 48.8566, 2.3522, CalculationMethod.MUSLIM_WORLD_LEAGUE),
        City("new_york", "نيويورك", "الولايات المتحدة", 40.7128, -74.0060, CalculationMethod.ISNA),
        City("toronto", "تورونتو", "كندا", 43.6532, -79.3832, CalculationMethod.ISNA),
        City("kuala_lumpur", "كوالالمبور", "ماليزيا", 3.1390, 101.6869, CalculationMethod.MUSLIM_WORLD_LEAGUE),
        City("jakarta", "جاكرتا", "إندونيسيا", -6.2088, 106.8456, CalculationMethod.MUSLIM_WORLD_LEAGUE)
    )
}
