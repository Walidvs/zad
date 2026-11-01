package com.example.data

import java.util.Calendar
import java.util.Locale
import kotlin.math.*

enum class CalculationMethod(val titleAr: String, val fajrAngle: Double, val ishaAngle: Double, val ishaIntervalMin: Int = 0) {
    UMM_AL_QURA("أم القرى (مكة المكرمة)", 18.5, 0.0, 90),
    EGYPTIAN("الهيئة المصرية العامة للمساحة", 19.5, 17.5),
    MUSLIM_WORLD_LEAGUE("رابطة العالم الإسلامي", 18.0, 17.0),
    KARACHI("جامعة العلوم الإسلامية بكراتشي", 18.0, 18.0),
    ISNA("الجمعية الإسلامية لأمريكا الشمالية (ISNA)", 15.0, 15.0)
}

enum class JuristicMethod(val titleAr: String, val shadowMultiplier: Double) {
    STANDARD("الشافعي والمالكي والحنبلي", 1.0),
    HANAFI("الحنفي", 2.0)
}

data class PrayerTime(
    val id: String,
    val nameAr: String,
    val timeFormatted: String, // HH:mm
    val hour: Int,
    val minute: Int,
    val timestampMillis: Long,
    val isNext: Boolean = false,
    val iconName: String = ""
)

data class PrayerTimesResult(
    val fajr: PrayerTime,
    val sunrise: PrayerTime,
    val dhuhr: PrayerTime,
    val asr: PrayerTime,
    val maghrib: PrayerTime,
    val isha: PrayerTime,
    val nextPrayer: PrayerTime,
    val timeToNextMillis: Long,
    val progressToNext: Float, // 0f to 1f
    val hijriDate: String,
    val gregorianDate: String
)

object PrayerCalculationEngine {
    // Kaaba Coordinates
    const val KAABA_LATITUDE = 21.422487
    const val KAABA_LONGITUDE = 39.826206

    /**
     * Calculates Qibla direction (true azimuth in degrees 0-360) from any location
     */
    fun calculateQibla(latitude: Double, longitude: Double): Double {
        val latRad = Math.toRadians(latitude)
        val kaabaLatRad = Math.toRadians(KAABA_LATITUDE)
        val lonDiffRad = Math.toRadians(KAABA_LONGITUDE - longitude)

        val y = sin(lonDiffRad)
        val x = cos(latRad) * tan(kaabaLatRad) - sin(latRad) * cos(lonDiffRad)
        var qibla = Math.toDegrees(atan2(y, x))
        qibla = (qibla + 360.0) % 360.0
        return qibla
    }

    /**
     * Calculates distance to Kaaba in kilometers
     */
    fun calculateDistanceToKaaba(latitude: Double, longitude: Double): Double {
        val r = 6371.0 // Earth's radius in km
        val dLat = Math.toRadians(KAABA_LATITUDE - latitude)
        val dLon = Math.toRadians(KAABA_LONGITUDE - longitude)
        val a = sin(dLat / 2).pow(2) + cos(Math.toRadians(latitude)) * cos(Math.toRadians(KAABA_LATITUDE)) * sin(dLon / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }

    /**
     * Calculates Prayer times for a given date, coordinates, and calculation method
     */
    fun calculatePrayerTimes(
        latitude: Double,
        longitude: Double,
        calendar: Calendar = Calendar.getInstance(),
        method: CalculationMethod = CalculationMethod.UMM_AL_QURA,
        juristic: JuristicMethod = JuristicMethod.STANDARD,
        timeZoneHours: Double? = null
    ): PrayerTimesResult {
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        val systemTz = calendar.timeZone.getOffset(calendar.timeInMillis) / 3600000.0
        val timeZone = timeZoneHours ?: (if (abs(systemTz) > 0.01) systemTz else (longitude / 15.0).roundToInt().toDouble())

        // Julian Day calculation
        val jd = julianDate(year, month, day) - longitude / (15.0 * 24.0)

        // Sun declination and Equation of time
        val d = jd - 2451545.0
        val g = fixAngle(357.529 + 0.98560028 * d)
        val q = fixAngle(280.459 + 0.98564736 * d)
        val l = fixAngle(q + 1.915 * sin(Math.toRadians(g)) + 0.020 * sin(Math.toRadians(2 * g)))
        val e = 23.439 - 0.00000036 * d
        val ra = fixAngle(Math.toDegrees(atan2(cos(Math.toRadians(e)) * sin(Math.toRadians(l)), cos(Math.toRadians(l))))) / 15.0

        val declination = Math.toDegrees(asin(sin(Math.toRadians(e)) * sin(Math.toRadians(l))))
        val eqTime = q / 15.0 - ra

        // Midday (Dhuhr)
        val dhuhrBase = fixHour(12.0 + timeZone - longitude / 15.0 - eqTime)

        // Sunrise & Sunset angles (center of sun -0.833 degrees)
        val sunriseHourDiff = sunAngleTime(-0.833, latitude, declination)
        val sunriseBase = dhuhrBase - sunriseHourDiff
        val maghribBase = dhuhrBase + sunriseHourDiff

        // Fajr angle
        val fajrHourDiff = sunAngleTime(-method.fajrAngle, latitude, declination)
        val fajrBase = dhuhrBase - fajrHourDiff

        // Asr angle
        val asrAngle = -Math.toDegrees(atan(1.0 / (juristic.shadowMultiplier + tan(Math.toRadians(abs(latitude - declination))))))
        val asrHourDiff = sunAngleTime(asrAngle, latitude, declination)
        val asrBase = dhuhrBase + asrHourDiff

        // Isha angle / interval
        val ishaBase = if (method.ishaIntervalMin > 0) {
            maghribBase + (method.ishaIntervalMin / 60.0)
        } else {
            val ishaHourDiff = sunAngleTime(-method.ishaAngle, latitude, declination)
            dhuhrBase + ishaHourDiff
        }

        val fajrTime = createPrayerTime("fajr", "الفجر", fajrBase, calendar, "wb_twilight")
        val sunriseTime = createPrayerTime("sunrise", "الشروق", sunriseBase, calendar, "wb_sunny")
        val dhuhrTime = createPrayerTime("dhuhr", "الظهر", dhuhrBase, calendar, "sunny")
        val asrTime = createPrayerTime("asr", "العصر", asrBase, calendar, "filter_drama")
        val maghribTime = createPrayerTime("maghrib", "المغرب", maghribBase, calendar, "nights_stay")
        val ishaTime = createPrayerTime("isha", "العشاء", ishaBase, calendar, "bedtime")

        val prayerList = listOf(fajrTime, sunriseTime, dhuhrTime, asrTime, maghribTime, ishaTime)
        val nowMillis = System.currentTimeMillis()

        // Determine next prayer
        var next = prayerList.firstOrNull { it.timestampMillis > nowMillis }
        var timeToNext = 0L
        var progress = 0f

        if (next == null) {
            // Next is tomorrow's Fajr
            val tomorrowFajrMillis = fajrTime.timestampMillis + 24 * 3600 * 1000L
            next = fajrTime.copy(isNext = true, timestampMillis = tomorrowFajrMillis)
            timeToNext = tomorrowFajrMillis - nowMillis
            val totalSpan = tomorrowFajrMillis - ishaTime.timestampMillis
            val elapsed = nowMillis - ishaTime.timestampMillis
            progress = if (totalSpan > 0) (elapsed.toFloat() / totalSpan).coerceIn(0f, 1f) else 0f
        } else {
            timeToNext = next.timestampMillis - nowMillis
            val prevIndex = prayerList.indexOf(next) - 1
            val prevMillis = if (prevIndex >= 0) prayerList[prevIndex].timestampMillis else (ishaTime.timestampMillis - 24 * 3600 * 1000L)
            val totalSpan = next.timestampMillis - prevMillis
            val elapsed = nowMillis - prevMillis
            progress = if (totalSpan > 0) (elapsed.toFloat() / totalSpan).coerceIn(0f, 1f) else 0f
        }

        val updatedList = prayerList.map { it.copy(isNext = it.id == next.id) }

        val hijriDateStr = calculateApproxHijriDate(calendar)
        val gregorianDateStr = String.format(
            Locale.getDefault(),
            "%02d %s %d",
            calendar.get(Calendar.DAY_OF_MONTH),
            getArabicMonthName(calendar.get(Calendar.MONTH)),
            calendar.get(Calendar.YEAR)
        )

        return PrayerTimesResult(
            fajr = updatedList[0],
            sunrise = updatedList[1],
            dhuhr = updatedList[2],
            asr = updatedList[3],
            maghrib = updatedList[4],
            isha = updatedList[5],
            nextPrayer = next,
            timeToNextMillis = timeToNext,
            progressToNext = progress,
            hijriDate = hijriDateStr,
            gregorianDate = gregorianDateStr
        )
    }

    private fun createPrayerTime(id: String, nameAr: String, hourDouble: Double, baseCalendar: Calendar, icon: String): PrayerTime {
        val totalMinutes = (fixHour(hourDouble) * 60).roundToInt()
        val h = (totalMinutes / 60) % 24
        val m = totalMinutes % 60

        val cal = baseCalendar.clone() as Calendar
        cal.set(Calendar.HOUR_OF_DAY, h)
        cal.set(Calendar.MINUTE, m)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)

        // 12-hour or 24-hour formatted string (e.g. 05:14 ص / م)
        val amPm = if (h < 12) "ص" else "م"
        val displayH = if (h % 12 == 0) 12 else h % 12
        val formatted = String.format(Locale.getDefault(), "%02d:%02d %s", displayH, m, amPm)

        return PrayerTime(
            id = id,
            nameAr = nameAr,
            timeFormatted = formatted,
            hour = h,
            minute = m,
            timestampMillis = cal.timeInMillis,
            iconName = icon
        )
    }

    private fun sunAngleTime(angle: Double, latitude: Double, declination: Double): Double {
        val latRad = Math.toRadians(latitude)
        val decRad = Math.toRadians(declination)
        val angRad = Math.toRadians(angle)
        val cosH = (sin(angRad) - sin(latRad) * sin(decRad)) / (cos(latRad) * cos(decRad))
        if (cosH > 1.0 || cosH < -1.0) return 0.0
        return Math.toDegrees(acos(cosH)) / 15.0
    }

    private fun fixAngle(a: Double): Double = (a % 360.0 + 360.0) % 360.0
    private fun fixHour(h: Double): Double = (h % 24.0 + 24.0) % 24.0

    private fun julianDate(year: Int, month: Int, day: Int): Double {
        var y = year
        var m = month
        if (m <= 2) {
            y -= 1
            m += 12
        }
        val a = floor(y / 100.0)
        val b = 2 - a + floor(a / 4.0)
        return floor(365.25 * (y + 4716)) + floor(30.6001 * (m + 1)) + day + b - 1524.5
    }

    private fun calculateApproxHijriDate(calendar: Calendar): String {
        val y = calendar.get(Calendar.YEAR)
        val m = calendar.get(Calendar.MONTH) + 1
        val d = calendar.get(Calendar.DAY_OF_MONTH)
        val jd = julianDate(y, m, d)
        val l = jd - 1948440 + 10632
        val n = floor((l - 1) / 10631.0)
        val lPrime = l - 10631 * n + 354
        val j = (floor((10985 - lPrime) / 5316.0)) * (floor((50 * lPrime) / 17719.0)) +
                (floor(lPrime / 5670.0)) * (floor((43 * lPrime) / 15238.0))
        val lDoublePrime = lPrime - (floor((30 - j) / 15.0)) * (floor((17719 * j) / 50.0)) -
                (floor(j / 16.0)) * (floor((15238 * j) / 43.0)) + 29
        val hijriMonth = floor((24 * lDoublePrime) / 709.0).toInt()
        val hijriDay = (lDoublePrime - floor((709 * hijriMonth) / 24.0)).toInt()
        val hijriYear = (30 * n + j - 30).toInt()

        val hijriMonths = listOf(
            "محرم", "صفر", "ربيع الأول", "ربيع الثاني", "جمادى الأولى", "جمادى الآخرة",
            "رجب", "شعبان", "رمضان", "شوال", "ذو القعدة", "ذو الحجة"
        )
        val monthName = hijriMonths.getOrElse(hijriMonth - 1) { "رمضان" }
        return "$hijriDay $monthName $hijriYear هـ"
    }

    private fun getArabicMonthName(monthIndex: Int): String {
        val months = listOf(
            "يناير", "فبراير", "مارس", "أبريل", "مايو", "يونيو",
            "يوليو", "أغسطس", "سبتمبر", "أكتوبر", "نوفمبر", "ديسمبر"
        )
        return months.getOrElse(monthIndex) { "" }
    }
}
