package com.example.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "zad_preferences")

class PreferencesManager(private val context: Context) {

    companion object {
        val KEY_CITY_ID = stringPreferencesKey("city_id")
        val KEY_CITY_NAME = stringPreferencesKey("city_name")
        val KEY_LATITUDE = doublePreferencesKey("latitude")
        val KEY_LONGITUDE = doublePreferencesKey("longitude")
        val KEY_CALC_METHOD = stringPreferencesKey("calc_method")
        val KEY_JURISTIC_METHOD = stringPreferencesKey("juristic_method")
        val KEY_AUTO_LOCATION = booleanPreferencesKey("auto_location")

        val KEY_ADHAN_RECITER = stringPreferencesKey("adhan_reciter")
        val KEY_ADHAN_ENABLED_PREFIX = "adhan_enabled_"

        val KEY_TASBEEH_COUNT = intPreferencesKey("tasbeeh_count")
        val KEY_TASBEEH_TOTAL = intPreferencesKey("tasbeeh_total")
        val KEY_TASBEEH_TARGET = intPreferencesKey("tasbeeh_target")
        val KEY_TASBEEH_DHIKR_INDEX = intPreferencesKey("tasbeeh_dhikr_index")

        val KEY_BOOKMARK_SURAH = intPreferencesKey("bookmark_surah")
        val KEY_BOOKMARK_AYAH = intPreferencesKey("bookmark_ayah")
    }

    val selectedCityFlow: Flow<City> = context.dataStore.data.map { prefs ->
        val id = prefs[KEY_CITY_ID] ?: CityData.defaultCity.id
        val name = prefs[KEY_CITY_NAME] ?: CityData.defaultCity.nameAr
        val lat = prefs[KEY_LATITUDE] ?: CityData.defaultCity.latitude
        val lon = prefs[KEY_LONGITUDE] ?: CityData.defaultCity.longitude
        val methodStr = prefs[KEY_CALC_METHOD] ?: CalculationMethod.UMM_AL_QURA.name
        val method = try { CalculationMethod.valueOf(methodStr) } catch (_: Exception) { CalculationMethod.UMM_AL_QURA }
        City(id, name, "", lat, lon, method)
    }

    val autoLocationFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_AUTO_LOCATION] ?: false
    }

    val calculationMethodFlow: Flow<CalculationMethod> = context.dataStore.data.map { prefs ->
        val methodStr = prefs[KEY_CALC_METHOD] ?: CalculationMethod.UMM_AL_QURA.name
        try { CalculationMethod.valueOf(methodStr) } catch (_: Exception) { CalculationMethod.UMM_AL_QURA }
    }

    val tasbeehStateFlow: Flow<Triple<Int, Int, Int>> = context.dataStore.data.map { prefs ->
        Triple(
            prefs[KEY_TASBEEH_COUNT] ?: 0,
            prefs[KEY_TASBEEH_TARGET] ?: 33,
            prefs[KEY_TASBEEH_TOTAL] ?: 0
        )
    }

    val bookmarkSurahFlow: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[KEY_BOOKMARK_SURAH] ?: 1
    }

    suspend fun saveCity(city: City) {
        context.dataStore.edit { prefs ->
            prefs[KEY_CITY_ID] = city.id
            prefs[KEY_CITY_NAME] = city.nameAr
            prefs[KEY_LATITUDE] = city.latitude
            prefs[KEY_LONGITUDE] = city.longitude
            prefs[KEY_CALC_METHOD] = city.calculationMethod.name
            prefs[KEY_AUTO_LOCATION] = false
        }
    }

    suspend fun saveAutoLocation(lat: Double, lon: Double, localityName: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_AUTO_LOCATION] = true
            prefs[KEY_CITY_NAME] = localityName
            prefs[KEY_LATITUDE] = lat
            prefs[KEY_LONGITUDE] = lon
        }
    }

    suspend fun saveCalculationMethod(method: CalculationMethod) {
        context.dataStore.edit { prefs ->
            prefs[KEY_CALC_METHOD] = method.name
        }
    }

    suspend fun saveTasbeehCount(count: Int, total: Int) {
        context.dataStore.edit { prefs ->
            prefs[KEY_TASBEEH_COUNT] = count
            prefs[KEY_TASBEEH_TOTAL] = total
        }
    }

    suspend fun saveTasbeehTarget(target: Int) {
        context.dataStore.edit { prefs ->
            prefs[KEY_TASBEEH_TARGET] = target
        }
    }

    suspend fun saveBookmark(surahNumber: Int, ayahNumber: Int = 1) {
        context.dataStore.edit { prefs ->
            prefs[KEY_BOOKMARK_SURAH] = surahNumber
            prefs[KEY_BOOKMARK_AYAH] = ayahNumber
        }
    }
}
