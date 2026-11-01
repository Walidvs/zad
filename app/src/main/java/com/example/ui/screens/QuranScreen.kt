package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.audio.AdhanPlayer
import com.example.data.PreferencesManager
import com.example.data.QuranData
import com.example.data.Surah
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GoldAccent
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuranScreen(
    adhanPlayer: AdhanPlayer,
    preferencesManager: PreferencesManager,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val bookmarkedSurahNum by preferencesManager.bookmarkSurahFlow.collectAsState(initial = 1)

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("all") } // all, makki, madani
    var activeSurahForReading by remember { mutableStateOf<Surah?>(null) }
    var initialReaderMode by remember { mutableStateOf(1) } // 0: Reading, 1: Tafseer

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
                .clickable {
                    initialReaderMode = 1
                    activeSurahForReading = bookmarkedSurah
                }
                .testTag("bookmark_card")
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
                            text = "آخر قراءة وتفسير محفوظ",
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
                    onClick = {
                        initialReaderMode = 1
                        activeSurahForReading = bookmarkedSurah
                    },
                    colors = ButtonDefaults.filledTonalButtonColors(containerColor = EmeraldPrimary, contentColor = Color.White),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(text = "فتح التفسير", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("ابحث عن سورة بالاسم أو الرقم لتفسيرها...") },
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
                .testTag("quran_search_field")
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Filter Chips
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

        // Surah List
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(filteredSurahs, key = { it.number }) { surah ->
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            initialReaderMode = 1 // Open directly to Tafseer mode
                            activeSurahForReading = surah
                        }
                        .testTag("surah_item_${surah.number}")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Surah Number inside Circle
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
                                    text = "${surah.revelationType} • ${surah.versesCount} آية • التفسير الميسر",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Action Pills
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = EmeraldContainer.copy(alpha = 0.6f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.MenuBook,
                                        contentDescription = null,
                                        tint = EmeraldPrimary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "تفسير الآيات",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = EmeraldPrimary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Surah Reader & Tafseer Dialog
    activeSurahForReading?.let { surah ->
        SurahReaderAndTafseerDialog(
            surah = surah,
            adhanPlayer = adhanPlayer,
            initialMode = initialReaderMode,
            isBookmarked = bookmarkedSurahNum == surah.number,
            onBookmark = {
                coroutineScope.launch {
                    preferencesManager.saveBookmark(surah.number)
                }
            },
            onDismiss = {
                adhanPlayer.stop()
                activeSurahForReading = null
            }
        )
    }
}

@Composable
fun SurahReaderAndTafseerDialog(
    surah: Surah,
    adhanPlayer: AdhanPlayer,
    initialMode: Int,
    isBookmarked: Boolean,
    onBookmark: () -> Unit,
    onDismiss: () -> Unit
) {
    val playerState by adhanPlayer.playerState.collectAsState()
    var currentMode by remember { mutableIntStateOf(initialMode) } // 0: Quran Reading, 1: Ayah by Ayah Tafseer
    var fontSizeSp by remember { mutableFloatStateOf(19f) }

    // Sheikh Mishary Alafasy audio URL for this surah
    val fullSurahAudioUrl = remember(surah.number) {
        val formattedNum = String.format("%03d", surah.number)
        "https://server8.mp3quran.net/afs/$formattedNum.mp3"
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
                .padding(vertical = 8.dp)
                .testTag("surah_reader_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Top Action Bar
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
                            text = "${surah.revelationType} • ${surah.versesCount} آيات • ص ${surah.pageNumber}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(onClick = onBookmark) {
                        Icon(
                            imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Outlined.BookmarkBorder,
                            contentDescription = "حفظ العلامة",
                            tint = if (isBookmarked) GoldAccent else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Mode Tabs: 0 -> قراءة المصحف, 1 -> كل آية وتفسيرها
                TabRow(
                    selectedTabIndex = currentMode,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp)),
                    divider = {}
                ) {
                    Tab(
                        selected = currentMode == 1,
                        onClick = { currentMode = 1 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.MenuBook, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("الآية وتفسيرها", fontWeight = if (currentMode == 1) FontWeight.Bold else FontWeight.Normal)
                            }
                        }
                    )
                    Tab(
                        selected = currentMode == 0,
                        onClick = { currentMode = 0 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.AutoStories, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("قراءة السورة كاملة", fontWeight = if (currentMode == 0) FontWeight.Bold else FontWeight.Normal)
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Audio Playback Bar & Font Controls
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            FilledIconButton(
                                onClick = {
                                    if (playerState.isPlaying) {
                                        adhanPlayer.pause()
                                    } else {
                                        adhanPlayer.playAudioUrl(
                                            "سورة ${surah.nameAr}",
                                            "الشيخ مشاري راشد العفاسي",
                                            fullSurahAudioUrl
                                        )
                                    }
                                },
                                colors = IconButtonDefaults.filledIconButtonColors(containerColor = EmeraldPrimary),
                                modifier = Modifier.size(36.dp)
                            ) {
                                if (playerState.isBuffering) {
                                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                } else if (playerState.isPlaying) {
                                    Icon(Icons.Default.Pause, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                                } else {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "تلاوة كامل السورة",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        // Font sizing
                        Row {
                            IconButton(onClick = { if (fontSizeSp > 14f) fontSizeSp -= 2f }) {
                                Icon(Icons.Default.TextDecrease, contentDescription = "تصغير الخط", modifier = Modifier.size(18.dp))
                            }
                            IconButton(onClick = { if (fontSizeSp < 32f) fontSizeSp += 2f }) {
                                Icon(Icons.Default.TextIncrease, contentDescription = "تكبير الخط", modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Content View according to mode
                if (currentMode == 1) {
                    // Ayah by Ayah with Tafseer view
                    SurahTafseerView(
                        surah = surah,
                        adhanPlayer = adhanPlayer,
                        fontSizeSp = fontSizeSp,
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    // Continuous Quran Mushaf Reading
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .padding(vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (surah.number != 9) {
                            Text(
                                text = "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ",
                                fontSize = (fontSizeSp + 3).sp,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldPrimary,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp)
                            )
                        }

                        if (surah.sampleVerses.isNotEmpty()) {
                            surah.sampleVerses.forEachIndexed { idx, verse ->
                                Text(
                                    text = "$verse ﴿${idx + 1}﴾",
                                    fontSize = fontSizeSp.sp,
                                    textAlign = TextAlign.Center,
                                    lineHeight = (fontSizeSp * 1.8f).sp,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp)
                                )
                            }
                        } else {
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "«اقْرَؤُوا القُرْآنَ فإنَّه يَأْتي يَومَ القِيامَةِ شَفِيعًا لأَصْحابِهِ»",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = EmeraldPrimary,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "يمكنك التبديل إلى تبويب «الآية وتفسيرها» بالأعلى للاطلاع على تفسير كل آية بالتفصيل، أو الاستماع للتلاوة الكاملة لسورة ${surah.nameAr} بصوت الشيخ مشاري العفاسي.",
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center,
                                        lineHeight = 22.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
