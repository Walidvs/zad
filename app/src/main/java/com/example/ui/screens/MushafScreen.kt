package com.example.ui.screens

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
fun MushafScreen(
    adhanPlayer: AdhanPlayer,
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
                            text = "آخر قراءة في المصحف الشريف",
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
            placeholder = { Text("ابحث في سور المصحف الشريف...") },
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

        // Surahs List
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

    // Pure Mushaf Reader Dialog
    activeSurahForReading?.let { surah ->
        PureMushafReaderDialog(
            surah = surah,
            adhanPlayer = adhanPlayer,
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
fun PureMushafReaderDialog(
    surah: Surah,
    adhanPlayer: AdhanPlayer,
    isBookmarked: Boolean,
    onBookmark: () -> Unit,
    onDismiss: () -> Unit
) {
    val playerState by adhanPlayer.playerState.collectAsState()
    var fontSizeSp by remember { mutableFloatStateOf(20f) }

    val audioUrl = remember(surah.number) {
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
                .testTag("pure_mushaf_reader_card")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header
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
                            text = "المصحف الشريف • ${surah.revelationType} • ${surah.versesCount} آيات • ص ${surah.pageNumber}",
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

                HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))

                // Audio Player & Font Resizer Bar
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
                                        adhanPlayer.playAudioUrl("سورة ${surah.nameAr}", "الشيخ مشاري راشد العفاسي", audioUrl)
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
                                text = "تلاوة الشيخ مشاري العفاسي",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

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

                // Quranic Text in Uthmanic Frame
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (surah.number != 9) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
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
                                    text = "يمكنك الاستماع للتلاوة الكاملة المباركة لسورة ${surah.nameAr} بصوت الشيخ مشاري راشد العفاسي عبر المشغل بالأعلى، أو متابعة التلاوة في المصحف الشريف المطبوع من الصفحة ${surah.pageNumber}، ولتفسير الآيات بالتفصيل تفضل بزيارة صفحة التفسير في التطبيق.",
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
