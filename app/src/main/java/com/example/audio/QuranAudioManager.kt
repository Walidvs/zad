package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import com.example.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

data class QuranAudioPlayerState(
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val isDownloading: Boolean = false,
    val downloadProgress: Float = 0f,
    val currentSurahNumber: Int = 0,
    val currentSurahName: String = "",
    val isOfflineAvailable: Boolean = false,
    val durationMs: Int = 0,
    val currentPositionMs: Int = 0,
    val errorMessage: String? = null
)

class QuranAudioManager(private val context: Context) {
    private var mediaPlayer: MediaPlayer? = null
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val _state = MutableStateFlow(QuranAudioPlayerState())
    val state: StateFlow<QuranAudioPlayerState> = _state.asStateFlow()

    private val audioDir: File
        get() {
            val dir = File(context.filesDir, "quran_audio")
            if (!dir.exists()) dir.mkdirs()
            return dir
        }

    fun isSurahOfflineAvailable(surahNumber: Int): Boolean {
        // Check raw bundled resources
        if (hasRawResource(surahNumber)) return true
        // Check local storage file
        val file = getSurahFile(surahNumber)
        return file.exists() && file.length() > 1000
    }

    private fun hasRawResource(surahNumber: Int): Boolean {
        return when (surahNumber) {
            1, 112, 113, 114 -> true
            else -> false
        }
    }

    private fun getRawResourceId(surahNumber: Int): Int? {
        return when (surahNumber) {
            1 -> R.raw.surah_001
            112 -> R.raw.surah_112
            113 -> R.raw.surah_113
            114 -> R.raw.surah_114
            else -> null
        }
    }

    private fun getSurahFile(surahNumber: Int): File {
        val formattedNum = String.format("%03d", surahNumber)
        return File(audioDir, "surah_$formattedNum.mp3")
    }

    fun playSurah(surahNumber: Int, surahName: String) {
        stop()

        val isOffline = isSurahOfflineAvailable(surahNumber)
        _state.value = _state.value.copy(
            isBuffering = true,
            isPlaying = false,
            currentSurahNumber = surahNumber,
            currentSurahName = surahName,
            isOfflineAvailable = isOffline,
            errorMessage = null
        )

        try {
            val rawResId = getRawResourceId(surahNumber)
            val localFile = getSurahFile(surahNumber)

            if (rawResId != null) {
                // 100% Offline: Play directly from bundled APK resources!
                mediaPlayer = MediaPlayer.create(context, rawResId).apply {
                    setOnCompletionListener {
                        _state.value = _state.value.copy(isPlaying = false, currentPositionMs = 0)
                    }
                    start()
                }
                _state.value = _state.value.copy(
                    isPlaying = true,
                    isBuffering = false,
                    isOfflineAvailable = true,
                    durationMs = mediaPlayer?.duration ?: 0
                )
            } else if (localFile.exists() && localFile.length() > 1000) {
                // 100% Offline: Play directly from locally downloaded file!
                mediaPlayer = MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .build()
                    )
                    setDataSource(context, Uri.fromFile(localFile))
                    setOnPreparedListener { mp ->
                        mp.start()
                        _state.value = _state.value.copy(
                            isPlaying = true,
                            isBuffering = false,
                            isOfflineAvailable = true,
                            durationMs = mp.duration
                        )
                    }
                    setOnCompletionListener {
                        _state.value = _state.value.copy(isPlaying = false, currentPositionMs = 0)
                    }
                    setOnErrorListener { _, _, _ ->
                        _state.value = _state.value.copy(isPlaying = false, isBuffering = false, errorMessage = "تعذر تشغيل الملف الصوتي المحفوظ")
                        true
                    }
                    prepareAsync()
                }
            } else {
                // Stream from online CDN and prompt for offline download
                val formattedNum = String.format("%03d", surahNumber)
                val url = "https://server8.mp3quran.net/afs/$formattedNum.mp3"

                mediaPlayer = MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .build()
                    )
                    setDataSource(context, Uri.parse(url))
                    setOnPreparedListener { mp ->
                        mp.start()
                        _state.value = _state.value.copy(
                            isPlaying = true,
                            isBuffering = false,
                            durationMs = mp.duration
                        )
                    }
                    setOnCompletionListener {
                        _state.value = _state.value.copy(isPlaying = false, currentPositionMs = 0)
                    }
                    setOnErrorListener { _, _, _ ->
                        _state.value = _state.value.copy(isPlaying = false, isBuffering = false, errorMessage = "تأكد من الاتصال بالإنترنت للاستماع أو حمّل السورة مسبقاً")
                        true
                    }
                    prepareAsync()
                }
            }
        } catch (e: Exception) {
            _state.value = _state.value.copy(
                isPlaying = false,
                isBuffering = false,
                errorMessage = e.localizedMessage
            )
        }
    }

    suspend fun downloadSurahForOffline(surahNumber: Int, onProgress: (Float) -> Unit = {}): Boolean = withContext(Dispatchers.IO) {
        if (isSurahOfflineAvailable(surahNumber)) return@withContext true

        _state.value = _state.value.copy(isDownloading = true, downloadProgress = 0f)

        try {
            val formattedNum = String.format("%03d", surahNumber)
            val url = "https://server8.mp3quran.net/afs/$formattedNum.mp3"
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()

            if (!response.isSuccessful) {
                _state.value = _state.value.copy(isDownloading = false, errorMessage = "فشل تحميل التلاوة من الخادم")
                return@withContext false
            }

            val body = response.body ?: return@withContext false
            val contentLength = body.contentLength()
            val destinationFile = getSurahFile(surahNumber)
            val tempFile = File(audioDir, "temp_${destinationFile.name}")

            val inputStream = body.byteStream()
            val outputStream = FileOutputStream(tempFile)
            val buffer = ByteArray(8 * 1024)
            var bytesRead: Int
            var totalBytesRead = 0L

            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
                totalBytesRead += bytesRead
                if (contentLength > 0) {
                    val progress = (totalBytesRead.toFloat() / contentLength).coerceIn(0f, 1f)
                    withContext(Dispatchers.Main) {
                        _state.value = _state.value.copy(downloadProgress = progress)
                        onProgress(progress)
                    }
                }
            }

            outputStream.flush()
            outputStream.close()
            inputStream.close()

            tempFile.renameTo(destinationFile)

            withContext(Dispatchers.Main) {
                _state.value = _state.value.copy(
                    isDownloading = false,
                    isOfflineAvailable = true,
                    downloadProgress = 1f
                )
            }
            return@withContext true
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                _state.value = _state.value.copy(
                    isDownloading = false,
                    errorMessage = "فشل التحميل، تحقق من اتصالك بالإنترنت"
                )
            }
            return@withContext false
        }
    }

    fun deleteOfflineSurah(surahNumber: Int) {
        if (hasRawResource(surahNumber)) return // Built-in cannot be deleted
        val file = getSurahFile(surahNumber)
        if (file.exists()) {
            file.delete()
            _state.value = _state.value.copy(isOfflineAvailable = false)
        }
    }

    fun pause() {
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.pause()
                _state.value = _state.value.copy(isPlaying = false)
            }
        }
    }

    fun resume() {
        mediaPlayer?.let {
            it.start()
            _state.value = _state.value.copy(isPlaying = true)
        }
    }

    fun stop() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (_: Exception) {}
        mediaPlayer = null
        _state.value = _state.value.copy(isPlaying = false, isBuffering = false)
    }

    fun release() {
        stop()
    }
}
