package com.example

import com.example.data.CityData
import com.example.data.PrayerCalculationEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class ExampleCalculationUnitTest {

    @Test
    fun `verify prayer times and qibla calculation`() {
        val cairo = CityData.popularCities.first { it.id == "cairo" }
        val qibla = PrayerCalculationEngine.calculateQibla(cairo.latitude, cairo.longitude)
        // Cairo Qibla is roughly 136° SE
        assertTrue("Cairo Qibla angle should be between 130 and 145 degrees", qibla in 130.0..145.0)

        val makkah = CityData.defaultCity
        val distanceToSelf = PrayerCalculationEngine.calculateDistanceToKaaba(makkah.latitude, makkah.longitude)
        assertTrue("Distance from Makkah to Kaaba should be less than 5 km", distanceToSelf < 5.0)

        val prayerTimes = PrayerCalculationEngine.calculatePrayerTimes(
            latitude = cairo.latitude,
            longitude = cairo.longitude,
            calendar = Calendar.getInstance()
        )
        assertTrue(prayerTimes.fajr.hour in 3..6)
        assertTrue(prayerTimes.dhuhr.hour in 11..13)
        assertTrue(prayerTimes.maghrib.hour in 16..20)
        assertTrue(prayerTimes.hijriDate.isNotEmpty())
    }
}
