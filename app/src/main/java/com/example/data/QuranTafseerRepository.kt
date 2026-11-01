package com.example.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class AyahWithTafseer(
    val ayahNumber: Int,
    val textUthmani: String,
    val tafseer: String,
    val audioUrl: String = ""
)

data class SurahTafseerResult(
    val surahNumber: Int,
    val surahNameAr: String,
    val revelationType: String,
    val totalAyahs: Int,
    val ayahs: List<AyahWithTafseer>
)

object QuranTafseerRepository {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    // In-memory cache for loaded Surah Tafseer
    private val cache = mutableMapOf<Int, SurahTafseerResult>()

    // Offline bundled Tafseer for Al-Fatihah and Short Surahs
    private val FALLBACK_FATIHAH = SurahTafseerResult(
        surahNumber = 1,
        surahNameAr = "الفاتحة",
        revelationType = "مكية",
        totalAyahs = 7,
        ayahs = listOf(
            AyahWithTafseer(
                1,
                "بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ",
                "أبتدئ قراءة القرآن باسم الله مستعيناً به، (الله) علم على الرب تبارك وتعالى، (الرحمن) ذو الرحمة الشاملة لجميع الخلائق، (الرحيم) بالمؤمنين.",
                "https://cdn.islamic.network/quran/audio/128/ar.alafasy/1.mp3"
            ),
            AyahWithTafseer(
                2,
                "ٱلْحَمْدُ لِلَّهِ رَبِّ ٱلْعَٰلَمِينَ",
                "الثناء والشكر الكامل لله وحده على نعمه، وهو المربي لجميع خلقه بتدبيره وإحسانه.",
                "https://cdn.islamic.network/quran/audio/128/ar.alafasy/2.mp3"
            ),
            AyahWithTafseer(
                3,
                "ٱلرَّحْمَٰنِ ٱلرَّحِيمِ",
                "(الرحمن) الذي وسعت رحمته كل شيء، (الرحيم) الذي يرحم عباده المؤمنين رحمة خاصة.",
                "https://cdn.islamic.network/quran/audio/128/ar.alafasy/3.mp3"
            ),
            AyahWithTafseer(
                4,
                "مَٰلِكِ يَوْمِ ٱلدِّينِ",
                "هو سبحانه المتصرف والمالك ليوم الجزاء والحساب، يوم لا يملك أحد لأحد شيئاً إلا بإذنه.",
                "https://cdn.islamic.network/quran/audio/128/ar.alafasy/4.mp3"
            ),
            AyahWithTafseer(
                5,
                "إِيَّاكَ نَعْبُدُ وَإِيَّاكَ نَسْتَعِينُ",
                "نخصك وحدك بالعبادة والطاعة، ونستعين بك وحدك في جميع أمورنا وقضاء حوائجنا.",
                "https://cdn.islamic.network/quran/audio/128/ar.alafasy/5.mp3"
            ),
            AyahWithTafseer(
                6,
                "ٱهْدِنَا ٱلصِّرَٰطَ ٱلْمُسْتَقِيمَ",
                "وفقنا وثبتنا على الطريق الواضح المستقيم الذي لا عوج فيه، وهو دين الإسلام الحق.",
                "https://cdn.islamic.network/quran/audio/128/ar.alafasy/6.mp3"
            ),
            AyahWithTafseer(
                7,
                "صِرَٰطَ ٱلَّذِينَ أَنْعَمْتَ عَلَيْهِمْ غَيْرِ ٱلْمَغْضُوبِ عَلَيْهِمْ وَلَا ٱلضَّآلِّينَ",
                "طريق الذين أنعمت عليهم من النبيين والصديقين والشهداء والصالحين، غير طريق المغضوب عليهم (اليهود ومن شابههم ممن عرف الحق ولم يعمل به)، ولا طريق الضالين (النصارى ومن تبعهم ممن عبد الله بجهل وضلال).",
                "https://cdn.islamic.network/quran/audio/128/ar.alafasy/7.mp3"
            )
        )
    )

    private val FALLBACK_IKHLAS = SurahTafseerResult(
        surahNumber = 112,
        surahNameAr = "الإخلاص",
        revelationType = "مكية",
        totalAyahs = 4,
        ayahs = listOf(
            AyahWithTafseer(1, "قُلْ هُوَ اللَّهُ أَحَدٌ", "قل أيها الرسول لمن سألك عن ربك: هو الله الواحد المتفرد بالألوهية والربوبية والأسماء والصفات، لا شريك له."),
            AyahWithTafseer(2, "اللَّهُ الصَّمَدُ", "السيد الذي كمل في سؤدده وعظمته، والذي تصمد إليه وتلجأ جميع الخلائق في حوائجها."),
            AyahWithTafseer(3, "لَمْ يَلِدْ وَلَمْ يُولَدْ", "ليس له ولد ولا والد ولا صاحبة، منزه عن صفات المخلوقين."),
            AyahWithTafseer(4, "وَلَمْ يَكُن لَّهُ كُفُوًا أَحَدٌ", "ولم يكن له مماثل ولا شبيه ولا نظير في ذاته وأفعاله وصفاته سبحانه.")
        )
    )

    private val FALLBACK_FALAQ = SurahTafseerResult(
        surahNumber = 113,
        surahNameAr = "الفلق",
        revelationType = "مكية",
        totalAyahs = 5,
        ayahs = listOf(
            AyahWithTafseer(1, "قُلْ أَعُوذُ بِرَبِّ الْفَلَقِ", "قل: أعتصم وألتجئ برب الصبح وفالقه."),
            AyahWithTafseer(2, "مِن شَرِّ مَا خَلَقَ", "من شر جميع ما خلق الله من المخلوقات وأذاها."),
            AyahWithTafseer(3, "وَمِن شَرِّ غَاسِقٍ إِذَا وَقَبَ", "ومن شر الليل وما فيه من المخاوف والشرور إذا أظلم ودخل."),
            AyahWithTafseer(4, "وَمِن شَرِّ النَّفَّاثَاتِ فِي الْعُقَدِ", "ومن شر الساحرات اللاتي ينفثن في عقد السحر للإضرار بالناس."),
            AyahWithTafseer(5, "وَمِن شَرِّ حَاسِدٍ إِذَا حَسَدَ", "ومن شر الحاسد إذا أظهر حسده وتمنى زوال نعمة غيره.")
        )
    )

    private val FALLBACK_NAS = SurahTafseerResult(
        surahNumber = 114,
        surahNameAr = "الناس",
        revelationType = "مكية",
        totalAyahs = 6,
        ayahs = listOf(
            AyahWithTafseer(1, "قُلْ أَعُوذُ بِرَبِّ النَّاسِ", "قل: ألتجئ وأعتصم برب الناس وخالقهم ومدبر أمورهم."),
            AyahWithTafseer(2, "مَلِكِ النَّاسِ", "مالكهم وسيدهم المتصرف في شؤونهم جميعاً."),
            AyahWithTafseer(3, "إِلَٰهِ النَّاسِ", "معبودهم الحق الذي لا معبود سواه."),
            AyahWithTafseer(4, "مِن شَرِّ الْوَسْوَاسِ الْخَنَّاسِ", "من شر الشيطان الذي يوسوس عند الغفلة، ويخنس ويتوارى عند ذكر الله."),
            AyahWithTafseer(5, "الَّذِي يُوَسْوِسُ فِي صُدُورِ النَّاسِ", "الذي يبث الشر والشكوك والشهوات في قلوب الخلق."),
            AyahWithTafseer(6, "مِنَ الْجِنَّةِ وَالنَّاسِ", "سواء كان هذا الموسوس من شياطين الجن أو من شياطين الإنس.")
        )
    )

    init {
        cache[1] = FALLBACK_FATIHAH
        cache[112] = FALLBACK_IKHLAS
        cache[113] = FALLBACK_FALAQ
        cache[114] = FALLBACK_NAS
    }

    suspend fun getSurahTafseer(surahNumber: Int): Result<SurahTafseerResult> = withContext(Dispatchers.IO) {
        // Return from memory cache if available
        cache[surahNumber]?.let { return@withContext Result.success(it) }

        try {
            val url = "https://api.alquran.cloud/v1/surah/$surahNumber/editions/quran-uthmani,ar.muyassar"
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()

            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("HTTP ${response.code}"))
            }

            val body = response.body?.string() ?: return@withContext Result.failure(Exception("استجابة فارغة"))
            val json = JSONObject(body)

            if (json.optInt("code") != 200) {
                return@withContext Result.failure(Exception("خطأ في البيانات"))
            }

            val dataArray = json.getJSONArray("data")
            val uthmaniEdition = dataArray.getJSONObject(0)
            val muyassarEdition = dataArray.getJSONObject(1)

            val surahName = uthmaniEdition.optString("name", "سورة")
            val revelationType = if (uthmaniEdition.optString("revelationType") == "Meccan") "مكية" else "مدنية"
            val totalAyahs = uthmaniEdition.optInt("numberOfAyahs", 0)

            val uthmaniAyahs = uthmaniEdition.getJSONArray("ayahs")
            val muyassarAyahs = muyassarEdition.getJSONArray("ayahs")

            val ayahsList = mutableListOf<AyahWithTafseer>()
            val count = minOf(uthmaniAyahs.length(), muyassarAyahs.length())

            for (i in 0 until count) {
                val uthmaniItem = uthmaniAyahs.getJSONObject(i)
                val muyassarItem = muyassarAyahs.getJSONObject(i)

                val numInSurah = uthmaniItem.optInt("numberInSurah", i + 1)
                val globalNumber = uthmaniItem.optInt("number", 1)
                val textUthmani = uthmaniItem.optString("text", "")
                val tafseerText = muyassarItem.optString("text", "")
                val audioUrl = "https://cdn.islamic.network/quran/audio/128/ar.alafasy/$globalNumber.mp3"

                ayahsList.add(
                    AyahWithTafseer(
                        ayahNumber = numInSurah,
                        textUthmani = textUthmani,
                        tafseer = tafseerText,
                        audioUrl = audioUrl
                    )
                )
            }

            val result = SurahTafseerResult(
                surahNumber = surahNumber,
                surahNameAr = surahName,
                revelationType = revelationType,
                totalAyahs = totalAyahs,
                ayahs = ayahsList
            )

            cache[surahNumber] = result
            Result.success(result)
        } catch (e: Exception) {
            // If offline and fallback is available for this Surah, return it
            cache[surahNumber]?.let { return@withContext Result.success(it) }
            Result.failure(e)
        }
    }
}
