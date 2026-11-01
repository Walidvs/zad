package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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

@Composable
fun TafseerScreen(
    adhanPlayer: AdhanPlayer,
    preferencesManager: PreferencesManager,
    modifier: Modifier = Modifier
) {
    var selectedSurah by remember { mutableStateOf(QuranData.allSurahs[0]) } // Defaults to Al-Fatihah
    var showSurahSelectorDialog by remember { mutableStateOf(false) }
    var fontSizeSp by remember { mutableFloatStateOf(18f) }

    val popularSurahNumbers = listOf(1, 18, 36, 55, 56, 67, 112, 113, 114)
    val popularSurahs = remember {
        QuranData.allSurahs.filter { it.number in popularSurahNumbers }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Active Surah Picker Banner
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showSurahSelectorDialog = true }
                .testTag("tafseer_surah_picker_banner")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(EmeraldPrimary),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${selectedSurah.number}",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "تفسير سورة ${selectedSurah.nameAr}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Icon(
                                Icons.Default.ArrowDropDown,
                                contentDescription = null,
                                tint = EmeraldPrimary
                            )
                        }
                        Text(
                            text = "${selectedSurah.revelationType} • ${selectedSurah.versesCount} آية • اضغط لتغيير السورة",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }

                // Font size controls
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { if (fontSizeSp > 14f) fontSizeSp -= 2f },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.TextDecrease, contentDescription = "تصغير", modifier = Modifier.size(16.dp))
                    }
                    IconButton(
                        onClick = { if (fontSizeSp < 30f) fontSizeSp += 2f },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.TextIncrease, contentDescription = "تكبير", modifier = Modifier.size(16.dp))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Quick Surah Chips Carousel
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(popularSurahs, key = { it.number }) { surah ->
                val isSelected = surah.number == selectedSurah.number
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedSurah = surah },
                    label = { Text("سورة ${surah.nameAr}", fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = EmeraldPrimary,
                        selectedLabelColor = Color.White
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Ayah by Ayah Tafseer View
        SurahTafseerView(
            surah = selectedSurah,
            adhanPlayer = adhanPlayer,
            fontSizeSp = fontSizeSp,
            modifier = Modifier.weight(1f)
        )
    }

    // Surah Selector Modal Dialog
    if (showSurahSelectorDialog) {
        SurahSelectorModal(
            currentSurahNumber = selectedSurah.number,
            onSurahSelected = { chosen ->
                selectedSurah = chosen
                showSurahSelectorDialog = false
            },
            onDismiss = { showSurahSelectorDialog = false }
        )
    }
}

@Composable
fun SurahSelectorModal(
    currentSurahNumber: Int,
    onSurahSelected: (Surah) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filtered = remember(searchQuery) {
        if (searchQuery.isBlank()) QuranData.allSurahs
        else QuranData.allSurahs.filter {
            it.nameAr.contains(searchQuery.trim()) || it.number.toString() == searchQuery.trim()
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .padding(vertical = 12.dp)
                .testTag("tafseer_surah_selector_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "اختر سورة لعرض تفسير آياتها",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("ابحث باسم السورة أو رقمها...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(filtered, key = { it.number }) { surah ->
                        val isSelected = surah.number == currentSurahNumber
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSurahSelected(surah) }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "${surah.number}.",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = EmeraldPrimary,
                                        modifier = Modifier.width(32.dp)
                                    )
                                    Text(
                                        text = "سورة ${surah.nameAr}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "(${surah.revelationType} • ${surah.versesCount} آية)",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                if (isSelected) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = EmeraldPrimary)
                                }
                            }
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                    }
                }
            }
        }
    }
}
