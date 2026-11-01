package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.AdhanPlayer
import com.example.data.AyahWithTafseer
import com.example.data.QuranTafseerRepository
import com.example.data.Surah
import com.example.data.SurahTafseerResult
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GoldAccent
import kotlinx.coroutines.launch

@Composable
fun SurahTafseerView(
    surah: Surah,
    adhanPlayer: AdhanPlayer,
    fontSizeSp: Float,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    var isLoading by remember { mutableStateOf(true) }
    var tafseerResult by remember { mutableStateOf<SurahTafseerResult?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var playingAyahNumber by remember { mutableIntStateOf(-1) }
    var filterAyahQuery by remember { mutableStateOf("") }

    val playerState by adhanPlayer.playerState.collectAsState()

    LaunchedEffect(playerState.isPlaying) {
        if (!playerState.isPlaying) {
            playingAyahNumber = -1
        }
    }

    fun loadTafseer() {
        isLoading = true
        errorMessage = null
        coroutineScope.launch {
            val result = QuranTafseerRepository.getSurahTafseer(surah.number)
            result.onSuccess { data ->
                tafseerResult = data
                isLoading = false
            }.onFailure { err ->
                errorMessage = "تعذر تحميل التفسير، يرجى التحقق من الاتصال بالإنترنت (${err.localizedMessage})"
                isLoading = false
            }
        }
    }

    LaunchedEffect(surah.number) {
        loadTafseer()
    }

    val displayedAyahs = remember(tafseerResult, filterAyahQuery) {
        val all = tafseerResult?.ayahs ?: emptyList()
        if (filterAyahQuery.isBlank()) all
        else {
            val q = filterAyahQuery.trim()
            val numQuery = q.toIntOrNull()
            all.filter {
                (numQuery != null && it.ayahNumber == numQuery) ||
                        it.textUthmani.contains(q) ||
                        it.tafseer.contains(q)
            }
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Search & Jump in Ayahs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = filterAyahQuery,
                onValueChange = { filterAyahQuery = it },
                placeholder = { Text("بحث في الآيات أو التفسير، أو رقم الآية...", fontSize = 12.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                trailingIcon = {
                    if (filterAyahQuery.isNotEmpty()) {
                        IconButton(onClick = { filterAyahQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .testTag("tafseer_search_input")
            )
        }

        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = EmeraldPrimary)
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "جاري تحميل نص السورة والتفسير الميسر لكل آية...",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else if (errorMessage != null && tafseerResult == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.CloudOff,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = errorMessage ?: "حدث خطأ أثناء تحميل التفسير",
                        textAlign = TextAlign.Center,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { loadTafseer() },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("إعادة المحاولة")
                    }
                }
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Basmala header (except Surah 9 At-Tawbah)
                if (surah.number != 9 && filterAyahQuery.isEmpty()) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ",
                                fontSize = (fontSizeSp + 3).sp,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldPrimary,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp)
                            )
                        }
                    }
                }

                items(displayedAyahs, key = { it.ayahNumber }) { item ->
                    AyahTafseerCard(
                        ayah = item,
                        surahName = surah.nameAr,
                        fontSizeSp = fontSizeSp,
                        isPlaying = playingAyahNumber == item.ayahNumber,
                        onPlayAudio = {
                            if (playingAyahNumber == item.ayahNumber && playerState.isPlaying) {
                                adhanPlayer.stop()
                                playingAyahNumber = -1
                            } else {
                                playingAyahNumber = item.ayahNumber
                                adhanPlayer.playAudioUrl(
                                    title = "سورة ${surah.nameAr} - آية ${item.ayahNumber}",
                                    subtitle = "الشيخ مشاري راشد العفاسي",
                                    url = item.audioUrl
                                )
                            }
                        },
                        onCopy = {
                            val textToCopy = "﴿${item.textUthmani}﴾ [سورة ${surah.nameAr}: ${item.ayahNumber}]\n\nالتفسير الميسر:\n${item.tafseer}"
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Ayah with Tafseer", textToCopy)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "تم نسخ الآية وتفسيرها بنجاح", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun AyahTafseerCard(
    ayah: AyahWithTafseer,
    surahName: String,
    fontSizeSp: Float,
    isPlaying: Boolean,
    onPlayAudio: () -> Unit,
    onCopy: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("ayah_tafseer_card_${ayah.ayahNumber}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Ayah Top Bar: Ayah Number Badge, Audio Play, Copy
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Ayah Number Badge in Islamic Style
                Surface(
                    shape = CircleShape,
                    color = EmeraldContainer,
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "${ayah.ayahNumber}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = EmeraldPrimary
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Play this Ayah Audio button
                    FilledTonalIconButton(
                        onClick = onPlayAudio,
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = if (isPlaying) EmeraldPrimary else MaterialTheme.colorScheme.primaryContainer,
                            contentColor = if (isPlaying) Color.White else EmeraldPrimary
                        ),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Stop else Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = "استماع للآية",
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Copy Ayah & Tafseer
                    IconButton(
                        onClick = onCopy,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Default.ContentCopy,
                            contentDescription = "نسخ الآية والتفسير",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // The Holy Ayah Text (Uthmanic Script)
            Text(
                text = "${ayah.textUthmani} ﴿${ayah.ayahNumber}﴾",
                fontSize = (fontSizeSp + 1).sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = (fontSizeSp * 1.8f).sp,
                textAlign = TextAlign.Start,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Tafseer Container (التفسير الميسر)
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.MenuBook,
                            contentDescription = null,
                            tint = GoldAccent,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "التفسير الميسر",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = EmeraldPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = ayah.tafseer,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 22.sp,
                        textAlign = TextAlign.Start
                    )
                }
            }
        }
    }
}
