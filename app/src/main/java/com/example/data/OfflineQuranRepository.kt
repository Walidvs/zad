package com.example.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.InputStreamReader

data class FullAyah(
    val globalNumber: Int,
    val numberInSurah: Int,
    val textUthmani: String
)

data class FullSurahContent(
    val number: Int,
    val nameAr: String,
    val revelationType: String,
    val versesCount: Int,
    val ayahs: List<FullAyah>
)

object OfflineQuranRepository {
    private val surahContentCache = mutableMapOf<Int, FullSurahContent>()
    private var isLoaded = false

    suspend fun getSurahVerses(context: Context, surahNumber: Int): FullSurahContent = withContext(Dispatchers.IO) {
        surahContentCache[surahNumber]?.let { return@withContext it }

        if (!isLoaded) {
            loadFromAssets(context)
        }

        surahContentCache[surahNumber] ?: run {
            // Fallback if not loaded
            val surah = QuranData.allSurahs.firstOrNull { it.number == surahNumber } ?: QuranData.allSurahs[0]
            val ayahs = surah.sampleVerses.mapIndexed { index, text ->
                FullAyah(index + 1, index + 1, text)
            }
            FullSurahContent(
                number = surah.number,
                nameAr = surah.nameAr,
                revelationType = surah.revelationType,
                versesCount = if (ayahs.isNotEmpty()) ayahs.size else surah.versesCount,
                ayahs = ayahs
            )
        }
    }

    private fun loadFromAssets(context: Context) {
        try {
            val assetManager = context.assets
            val inputStream = assetManager.open("quran_uthmani.json")
            val reader = InputStreamReader(inputStream, "UTF-8")
            val content = reader.readText()
            reader.close()
            inputStream.close()

            val root = JSONObject(content)
            val dataObj = root.getJSONObject("data")
            val surahsArray = dataObj.getJSONArray("surahs")

            for (i in 0 until surahsArray.length()) {
                val sObj = surahsArray.getJSONObject(i)
                val sNum = sObj.getInt("number")
                var sName = sObj.getString("name").replace("سُورَةُ", "").trim()
                if (sName.startsWith("ٱلْ") || sName.startsWith("الْ") || sName.startsWith("ال")) {
                    // Normalize display name if needed
                }
                val revType = if (sObj.getString("revelationType") == "Meccan") "مكية" else "مدنية"
                val ayahsArray = sObj.getJSONArray("ayahs")

                val ayahsList = mutableListOf<FullAyah>()
                for (j in 0 until ayahsArray.length()) {
                    val aObj = ayahsArray.getJSONObject(j)
                    val globalNum = aObj.getInt("number")
                    val numInSurah = aObj.getInt("numberInSurah")
                    var text = aObj.getString("text")

                    // Clean leading BOM if present
                    text = text.trimStart('\uFEFF', ' ')

                    // In Surahs other than Al-Fatihah, if the first ayah starts with Basmala, keep it clean
                    if (sNum != 1 && sNum != 9 && numInSurah == 1) {
                        val basmalaPrefix = "بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ"
                        if (text.startsWith(basmalaPrefix)) {
                            text = text.substring(basmalaPrefix.length).trim()
                        }
                    }

                    ayahsList.add(FullAyah(globalNum, numInSurah, text))
                }

                // Match with catalog name for consistency
                val catalogName = QuranData.allSurahs.firstOrNull { it.number == sNum }?.nameAr ?: sName

                surahContentCache[sNum] = FullSurahContent(
                    number = sNum,
                    nameAr = catalogName,
                    revelationType = revType,
                    versesCount = ayahsList.size,
                    ayahs = ayahsList
                )
            }
            isLoaded = true
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
