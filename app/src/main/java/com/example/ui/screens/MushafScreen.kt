package com.example.ui.screens

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
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.BookmarkBorder
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
import androidx.compose.ui.window.Dialog
import com.example.audio.QuranAudioManager
import com.example.data.*
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GoldAccent
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MushafScreen(
    quranAudioManager: QuranAudioManager,
    preferencesManager: PreferencesManager,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val bookmarkedSurahNum by preferencesManager.bookmarkSurahFlow.collectAsState(initial = 1)

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("all") } // all, makki, madani
    var activeSurahForReading by remember { mutableStateOf<Surah?>(null) }

    val filteredSurahs = remember(searchQuery, selectedFilter) {
        QuranData.allSurahs.filter { surah ->
            val matchesSearch = searchQuery.isBlank() ||
                    surah.nameAr.contains(searchQuery.trim()) ||
                    surah.number.toString() == searchQuery.trim()
            val matchesFilter = when (selectedFilter) {
                "makki" -> surah.revelationType == "مكية"
                "madani" -> surah.revelationType == "مدنية"
                else -> true
            }
            matchesSearch && matchesFilter
        }
    }

    val bookmarkedSurah = remember(bookmarkedSurahNum) {
        QuranData.allSurahs.find { it.number == bookmarkedSurahNum } ?: QuranData.allSurahs[0]
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Last Read Bookmark Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .clickable { activeSurahForReading = bookmarkedSurah }
                .testTag("mushaf_bookmark_card")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Bookmark,
                        contentDescription = null,
                        tint = EmeraldPrimary,
                        modifier = Modifier.size(26.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "آخر قراءة محفوظة في المصحف",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                        Text(
                            text = "سورة ${bookmarkedSurah.nameAr} (صفحة ${bookmarkedSurah.pageNumber})",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                FilledTonalButton(
                    onClick = { activeSurahForReading = bookmarkedSurah },
                    colors = ButtonDefaults.filledTonalButtonColors(containerColor = EmeraldPrimary, contentColor = Color.White),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(text = "متابعة التلاوة", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("ابحث في الـ 114 سورة بالاسم أو الرقم...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Clear, contentDescription = "مسح")
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("mushaf_search_field")
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Filters: All, Makki, Madani
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedFilter == "all",
                onClick = { selectedFilter = "all" },
                label = { Text("جميع السور (${QuranData.allSurahs.size})") }
            )
            FilterChip(
                selected = selectedFilter == "makki",
                onClick = { selectedFilter = "makki" },
                label = { Text("مكية") }
            )
            FilterChip(
                selected = selectedFilter == "madani",
                onClick = { selectedFilter = "madani" },
                label = { Text("مدنية") }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Surahs List (All 114 Surahs)
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(filteredSurahs, key = { it.number }) { surah ->
                val hasOfflineAudio = quranAudioManager.isSurahOfflineAvailable(surah.number)
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { activeSurahForReading = surah }
                        .testTag("mushaf_surah_${surah.number}")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(EmeraldContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = surah.number.toString(),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = EmeraldPrimary
                                )
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column {
                                Text(
                                    text = "سورة ${surah.nameAr}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${surah.revelationType} • ${surah.versesCount} آية • صفحة ${surah.pageNumber}",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (hasOfflineAudio) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = EmeraldContainer,
                                    modifier = Modifier.padding(end = 8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.CloudDone, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(12.dp))
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text("بدون نت", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = EmeraldPrimary)
                                    }
                                }
                            }

                            Icon(
                                Icons.AutoMirrored.Filled.MenuBook,
                                contentDescription = "قراءة المصحف",
                                tint = EmeraldPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    // Pure Mushaf Reader Dialog with ALL complete verses & offline audio controls
    activeSurahForReading?.let { surah ->
        CompleteMushafReaderDialog(
            surah = surah,
            quranAudioManager = quranAudioManager,
            isBookmarked = bookmarkedSurahNum == surah.number,
            onBookmark = {
                coroutineScope.launch {
                    preferencesManager.saveBookmark(surah.number)
                }
            },
            onDismiss = {
                quranAudioManager.stop()
                activeSurahForReading = null
            }
        )
    }
}

@Composable
fun CompleteMushafReaderDialog(
    surah: Surah,
    quranAudioManager: QuranAudioManager,
    isBookmarked: Boolean,
    onBookmark: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val audioState by quranAudioManager.state.collectAsState()

    var fontSizeSp by remember { mutableFloatStateOf(21f) }
    var fullSurahContent by remember { mutableStateOf<FullSurahContent?>(null) }
    var isLoadingVerses by remember { mutableStateOf(true) }

    val isCurrentSurahPlaying = audioState.isPlaying && audioState.currentSurahNumber == surah.number
    val isOfflineAudio = remember(audioState.isOfflineAvailable, surah.number) {
        quranAudioManager.isSurahOfflineAvailable(surah.number)
    }

    // Load full complete verses of this Surah from offline assets
    LaunchedEffect(surah.number) {
        isLoadingVerses = true
        val content = OfflineQuranRepository.getSurahVerses(context, surah.number)
        fullSurahContent = content
        isLoadingVerses = false
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.94f)
                .padding(vertical = 6.dp)
                .testTag("complete_mushaf_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق")
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "سورة ${surah.nameAr}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "المصحف الشريف • ${surah.revelationType} • ${fullSurahContent?.versesCount ?: surah.versesCount} آيات • ص ${surah.pageNumber}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(onClick = onBookmark) {
                        Icon(
                            imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Outlined.BookmarkBorder,
                            contentDescription = "حفظ علامة القراءة",
                            tint = if (isBookmarked) GoldAccent else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))

                // Audio Player & Offline Download Controls
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Play / Pause Audio
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                FilledIconButton(
                                    onClick = {
                                        if (isCurrentSurahPlaying) {
                                            quranAudioManager.pause()
                                        } else if (audioState.currentSurahNumber == surah.number && !audioState.isPlaying && audioState.durationMs > 0) {
                                            quranAudioManager.resume()
                                        } else {
                                            quranAudioManager.playSurah(surah.number, surah.nameAr)
                                        }
                                    },
                                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = EmeraldPrimary),
                                    modifier = Modifier.size(38.dp)
                                ) {
                                    if (audioState.isBuffering && audioState.currentSurahNumber == surah.number) {
                                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                    } else if (isCurrentSurahPlaying) {
                                        Icon(Icons.Default.Pause, contentDescription = "إيقاف مؤقت", tint = Color.White, modifier = Modifier.size(20.dp))
                                    } else {
                                        Icon(Icons.Default.PlayArrow, contentDescription = "تشغيل التلاوة", tint = Color.White, modifier = Modifier.size(22.dp))
                                    }
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                Column {
                                    Text(
                                        text = "تلاوة بصوت الشيخ مشاري العفاسي",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (isOfflineAudio) {
                                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(12.dp))
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text("متاح بدون إنترنت (أوفلاين)", fontSize = 10.sp, color = EmeraldPrimary, fontWeight = FontWeight.Bold)
                                        } else {
                                            Text("استماع مباشر عبر الإنترنت", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                }
                            }

                            // Download for Offline Button / Status
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (!isOfflineAudio) {
                                    if (audioState.isDownloading) {
                                        CircularProgressIndicator(
                                            progress = { audioState.downloadProgress },
                                            modifier = Modifier.size(28.dp),
                                            color = EmeraldPrimary,
                                            strokeWidth = 3.dp
                                        )
                                    } else {
                                        FilledTonalButton(
                                            onClick = {
                                                coroutineScope.launch {
                                                    val success = quranAudioManager.downloadSurahForOffline(surah.number)
                                                    if (success) {
                                                        Toast.makeText(context, "تم حفظ سورة ${surah.nameAr} للاستماع بدون نت بنجاح!", Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                            modifier = Modifier.height(32.dp)
                                        ) {
                                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("حفظ بدون نت", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.width(4.dp))

                                // Font sizing buttons
                                IconButton(
                                    onClick = { if (fontSizeSp > 15f) fontSizeSp -= 2f },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.TextDecrease, contentDescription = "تصغير", modifier = Modifier.size(16.dp))
                                }
                                IconButton(
                                    onClick = { if (fontSizeSp < 36f) fontSizeSp += 2f },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.TextIncrease, contentDescription = "تكبير", modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // The Complete Verses of the Surah
                if (isLoadingVerses) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = EmeraldPrimary)
                    }
                } else {
                    val content = fullSurahContent
                    if (content != null && content.ayahs.isNotEmpty()) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Basmala header (all Surahs except Surah 9 At-Tawbah)
                            if (surah.number != 9) {
                                item {
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 8.dp)
                                    ) {
                                        Text(
                                            text = "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ",
                                            fontSize = (fontSizeSp + 3).sp,
                                            fontWeight = FontWeight.Bold,
                                            color = EmeraldPrimary,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 10.dp)
                                        )
                                    }
                                }
                            }

                            // Render every single Ayah in the Surah
                            items(content.ayahs, key = { it.globalNumber }) { ayah ->
                                Text(
                                    text = "${ayah.textUthmani} ﴿${ayah.numberInSurah}﴾",
                                    fontSize = fontSizeSp.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    textAlign = TextAlign.Start,
                                    lineHeight = (fontSizeSp * 1.8f).sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp, horizontal = 6.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
