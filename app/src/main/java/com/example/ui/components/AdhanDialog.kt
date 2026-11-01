package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.example.audio.AdhanReciter
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GoldAccent

@Composable
fun AdhanDialog(
    adhanPlayer: AdhanPlayer,
    onDismissRequest: () -> Unit
) {
    val playerState by adhanPlayer.playerState.collectAsState()
    var selectedReciterIndex by remember { mutableIntStateOf(0) }
    var showDuaSection by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = {
        adhanPlayer.stop()
        onDismissRequest()
    }) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .testTag("adhan_dialog_card")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top close icon & title
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            adhanPlayer.stop()
                            onDismissRequest()
                        },
                        modifier = Modifier.testTag("close_adhan_dialog")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق")
                    }

                    Text(
                        text = "الأذان وصوت النداء",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Icon(
                        Icons.Default.VolumeUp,
                        contentDescription = null,
                        tint = GoldAccent,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Reciters List
                Text(
                    text = "اختر المؤذن الشريف:",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Start
                )

                Spacer(modifier = Modifier.height(8.dp))

                AdhanPlayer.ADHAN_RECITERS.forEachIndexed { index, reciter ->
                    val isSelected = selectedReciterIndex == index
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                selectedReciterIndex = index
                                adhanPlayer.playAdhan(reciter)
                            }
                            .testTag("reciter_item_$index"),
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = {
                                    selectedReciterIndex = index
                                    adhanPlayer.playAdhan(reciter)
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = reciter.titleAr,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = reciter.subtitleAr,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Playback Controls
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (playerState.isPlaying) "جاري تشغيل: ${playerState.currentTitle}"
                            else if (playerState.isBuffering) "جاري تحميل الأذان المبارك..."
                            else "اضغط للاستماع إلى الأذان",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Stop button
                            FilledIconButton(
                                onClick = { adhanPlayer.stop() },
                                colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("stop_adhan_btn")
                            ) {
                                Icon(Icons.Default.Stop, contentDescription = "إيقاف", tint = MaterialTheme.colorScheme.onSurface)
                            }

                            Spacer(modifier = Modifier.width(20.dp))

                            // Play / Pause main button
                            FilledIconButton(
                                onClick = {
                                    if (playerState.isPlaying) {
                                        adhanPlayer.pause()
                                    } else {
                                        if (playerState.currentTitle.isEmpty()) {
                                            adhanPlayer.playAdhan(AdhanPlayer.ADHAN_RECITERS[selectedReciterIndex])
                                        } else {
                                            adhanPlayer.resume()
                                        }
                                    }
                                },
                                colors = IconButtonDefaults.filledIconButtonColors(containerColor = EmeraldPrimary),
                                modifier = Modifier
                                    .size(64.dp)
                                    .testTag("play_pause_adhan_btn")
                            ) {
                                if (playerState.isBuffering) {
                                    CircularProgressIndicator(
                                        color = Color.White,
                                        modifier = Modifier.size(28.dp),
                                        strokeWidth = 2.dp
                                    )
                                } else if (playerState.isPlaying) {
                                    Icon(Icons.Default.Pause, contentDescription = "إيقاف مؤقت", tint = Color.White, modifier = Modifier.size(32.dp))
                                } else {
                                    Icon(Icons.Default.PlayArrow, contentDescription = "تشغيل", tint = Color.White, modifier = Modifier.size(36.dp))
                                }
                            }
                        }

                        playerState.errorMessage?.let { error ->
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = error,
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Dua After Azan Toggle
                OutlinedButton(
                    onClick = { showDuaSection = !showDuaSection },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("toggle_dua_btn")
                ) {
                    Icon(
                        if (showDuaSection) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "دعاء ما بعد الأذان المستحب",
                        fontWeight = FontWeight.SemiBold
                    )
                }

                AnimatedVisibility(visible = showDuaSection) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "«اللَّهُمَّ رَبَّ هَذِهِ الدَّعْوَةِ التَّامَّةِ، وَالصَّلَاةِ القَائِمَةِ، آتِ مُحَمَّدًا الوَسِيلَةَ وَالفَضِيلَةَ، وَابْعَثْهُ مَقَامًا مَحْمُودًا الَّذِي وَعَدْتَهُ»",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                textAlign = TextAlign.Center,
                                lineHeight = 26.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "من قالها حين يسمع النداء حلت له شفاعة النبي ﷺ يوم القيامة (صحيح البخاري)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}
