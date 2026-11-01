package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class AdhanReciter(
    val id: String,
    val titleAr: String,
    val subtitleAr: String,
    val audioUrl: String
)

data class AdhanPlayerState(
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val currentTitle: String = "",
    val currentReciter: String = "",
    val durationMs: Int = 0,
    val currentPositionMs: Int = 0,
    val errorMessage: String? = null
)

class AdhanPlayer(private val context: Context) {
    private var mediaPlayer: MediaPlayer? = null
    private val _playerState = MutableStateFlow(AdhanPlayerState())
    val playerState: StateFlow<AdhanPlayerState> = _playerState.asStateFlow()

    companion object {
        val ADHAN_RECITERS = listOf(
            AdhanReciter(
                id = "makkah",
                titleAr = "أذان الحرم المكي الشريف",
                subtitleAr = "مكة المكرمة - الشيخ علي أحمد ملا",
                audioUrl = "https://media.blubrry.com/muslim_central_audio/podcasts.qurancentral.com/adhan/makkah-ali-ahmed-mullah.mp3"
            ),
            AdhanReciter(
                id = "madinah",
                titleAr = "أذان المسجد النبوي الشريف",
                subtitleAr = "المدينة المنورة - الشيخ عصام بخاري",
                audioUrl = "https://media.blubrry.com/muslim_central_audio/podcasts.qurancentral.com/adhan/madinah-adhan-1.mp3"
            ),
            AdhanReciter(
                id = "alquds",
                titleAr = "أذان المسجد الأقصى المبارك",
                subtitleAr = "القدس الشريف",
                audioUrl = "https://media.blubrry.com/muslim_central_audio/podcasts.qurancentral.com/adhan/alaqsa-adhan.mp3"
            ),
            AdhanReciter(
                id = "abdulbasit",
                titleAr = "أذان الشيخ عبد الباسط عبد الصمد",
                subtitleAr = "تلاوة وأذان خاشع ومؤثر",
                audioUrl = "https://media.blubrry.com/muslim_central_audio/podcasts.qurancentral.com/adhan/abdul-basit-adhan.mp3"
            )
        )
    }

    fun playAdhan(reciter: AdhanReciter = ADHAN_RECITERS[0]) {
        stop()
        _playerState.value = _playerState.value.copy(
            isBuffering = true,
            isPlaying = false,
            currentTitle = reciter.titleAr,
            currentReciter = reciter.subtitleAr,
            errorMessage = null
        )

        try {
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                setDataSource(context, Uri.parse(reciter.audioUrl))
                setOnPreparedListener { mp ->
                    mp.start()
                    _playerState.value = _playerState.value.copy(
                        isPlaying = true,
                        isBuffering = false,
                        durationMs = mp.duration
                    )
                }
                setOnCompletionListener {
                    _playerState.value = _playerState.value.copy(
                        isPlaying = false,
                        isBuffering = false,
                        currentPositionMs = 0
                    )
                }
                setOnErrorListener { _, what, extra ->
                    _playerState.value = _playerState.value.copy(
                        isPlaying = false,
                        isBuffering = false,
                        errorMessage = "تعذر تشغيل الصوت ($what)"
                    )
                    true
                }
                prepareAsync()
            }
        } catch (e: Exception) {
            _playerState.value = _playerState.value.copy(
                isPlaying = false,
                isBuffering = false,
                errorMessage = e.localizedMessage
            )
        }
    }

    fun playAudioUrl(title: String, subtitle: String, url: String) {
        stop()
        _playerState.value = _playerState.value.copy(
            isBuffering = true,
            isPlaying = false,
            currentTitle = title,
            currentReciter = subtitle,
            errorMessage = null
        )

        try {
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
                    _playerState.value = _playerState.value.copy(
                        isPlaying = true,
                        isBuffering = false,
                        durationMs = mp.duration
                    )
                }
                setOnCompletionListener {
                    _playerState.value = _playerState.value.copy(
                        isPlaying = false,
                        isBuffering = false,
                        currentPositionMs = 0
                    )
                }
                setOnErrorListener { _, _, _ ->
                    _playerState.value = _playerState.value.copy(
                        isPlaying = false,
                        isBuffering = false,
                        errorMessage = "تعذر تشغيل التسجيل الصوتي"
                    )
                    true
                }
                prepareAsync()
            }
        } catch (e: Exception) {
            _playerState.value = _playerState.value.copy(
                isPlaying = false,
                isBuffering = false,
                errorMessage = e.localizedMessage
            )
        }
    }

    fun pause() {
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.pause()
                _playerState.value = _playerState.value.copy(isPlaying = false)
            }
        }
    }

    fun resume() {
        mediaPlayer?.let {
            it.start()
            _playerState.value = _playerState.value.copy(isPlaying = true)
        }
    }

    fun stop() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (_: Exception) {}
        mediaPlayer = null
        _playerState.value = _playerState.value.copy(isPlaying = false, isBuffering = false)
    }

    fun release() {
        stop()
    }
}
