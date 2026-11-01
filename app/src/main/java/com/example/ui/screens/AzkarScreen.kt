package com.example.ui.screens

import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AzkarCategory
import com.example.data.AzkarData
import com.example.data.DhikrItem
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GoldAccent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AzkarScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val vibrator = remember { context.getSystemService(Vibrator::class.java) }

    var selectedCategoryIndex by remember { mutableIntStateOf(0) }
    val currentCategory = AzkarData.allCategories[selectedCategoryIndex]

    // Tracks remaining count for each dhikr id
    var countMap by remember {
        mutableStateOf(
            AzkarData.allCategories.flatMap { it.items }.associate { it.id to it.targetCount }.toMutableMap()
        )
    }

    val completedCount = remember(currentCategory, countMap) {
        currentCategory.items.count { (countMap[it.id] ?: 0) == 0 }
    }
    val progress = remember(completedCount, currentCategory) {
        if (currentCategory.items.isNotEmpty()) completedCount.toFloat() / currentCategory.items.size else 0f
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Categories Scrollable TabRow
        ScrollableTabRow(
            selectedTabIndex = selectedCategoryIndex,
            edgePadding = 0.dp,
            divider = {},
            containerColor = Color.Transparent
        ) {
            AzkarData.allCategories.forEachIndexed { index, category ->
                Tab(
                    selected = selectedCategoryIndex == index,
                    onClick = { selectedCategoryIndex = index },
                    text = {
                        Text(
                            text = category.titleAr,
                            fontWeight = if (selectedCategoryIndex == index) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Progress Card
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "المكتمل: $completedCount من ${currentCategory.items.size} أذكار",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .height(6.dp)
                            .clip(CircleShape),
                        color = EmeraldPrimary,
                        trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                    )
                }

                TextButton(
                    onClick = {
                        countMap = countMap.toMutableMap().apply {
                            currentCategory.items.forEach { put(it.id, it.targetCount) }
                        }
                    }
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "إعادة ضبط", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "إعادة", fontSize = 12.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Adhkar List
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(currentCategory.items, key = { it.id }) { dhikr ->
                val remaining = countMap[dhikr.id] ?: dhikr.targetCount
                val isDone = remaining == 0

                val cardBg by animateColorAsState(
                    targetValue = if (isDone) EmeraldContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surface,
                    label = "cardBg"
                )

                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    elevation = CardDefaults.cardElevation(defaultElevation = if (isDone) 0.dp else 2.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dhikr_card_${dhikr.id}")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = dhikr.textAr,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            lineHeight = 28.sp,
                            textAlign = TextAlign.Start
                        )

                        if (dhikr.fadlAr.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Star,
                                        contentDescription = null,
                                        tint = GoldAccent,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "${dhikr.fadlAr} (${dhikr.sourceAr})",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Interactive Counter Button
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            FilledTonalButton(
                                onClick = {
                                    if (remaining > 0) {
                                        val nextCount = remaining - 1
                                        countMap = countMap.toMutableMap().apply {
                                            put(dhikr.id, nextCount)
                                        }
                                        try {
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                                vibrator?.vibrate(VibrationEffect.createOneShot(30, VibrationEffect.DEFAULT_AMPLITUDE))
                                            } else {
                                                @Suppress("DEPRECATION")
                                                vibrator?.vibrate(30)
                                            }
                                        } catch (_: Exception) {}
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = if (isDone) EmeraldPrimary else MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = if (isDone) Color.White else MaterialTheme.colorScheme.onPrimaryContainer
                                ),
                                modifier = Modifier.testTag("dhikr_counter_btn_${dhikr.id}")
                            ) {
                                if (isDone) {
                                    Icon(Icons.Default.Check, contentDescription = "تم", modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(text = "تم بحمد الله", fontWeight = FontWeight.Bold)
                                } else {
                                    Icon(Icons.Default.TouchApp, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "المتبقي: $remaining / ${dhikr.targetCount}",
                                        fontWeight = FontWeight.Bold
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
