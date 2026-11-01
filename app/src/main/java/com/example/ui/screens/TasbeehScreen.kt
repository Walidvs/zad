package com.example.ui.screens

import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.PreferencesManager
import com.example.ui.theme.EmeraldLight
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GoldAccent
import kotlinx.coroutines.launch

@Composable
fun TasbeehScreen(
    preferencesManager: PreferencesManager,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val vibrator = remember { context.getSystemService(Vibrator::class.java) }
    val coroutineScope = rememberCoroutineScope()

    val tasbeehTriple by preferencesManager.tasbeehStateFlow.collectAsState(initial = Triple(0, 33, 0))
    var currentCount by remember { mutableIntStateOf(0) }
    var targetGoal by remember { mutableIntStateOf(33) }
    var totalCount by remember { mutableIntStateOf(0) }

    LaunchedEffect(tasbeehTriple) {
        currentCount = tasbeehTriple.first
        targetGoal = tasbeehTriple.second
        totalCount = tasbeehTriple.third
    }

    val dhikrPhrases = listOf(
        "سُبْحَانَ اللَّهِ",
        "الْحَمْدُ لِلَّهِ",
        "لَا إِلَٰهَ إِلَّا اللَّهُ",
        "اللَّهُ أَكْبَرُ",
        "أَسْتَغْفِرُ اللَّهَ وَأَتُوبُ إِلَيْهِ",
        "لَا حَوْلَ وَلَا قُوَّةَ إِلَّا بِاللَّهِ",
        "سُبْحَانَ اللَّهِ وَبِحَمْدِهِ ، سُبْحَانَ اللَّهِ الْعَظِيمِ",
        "اللَّهُمَّ صَلِّ وَسَلِّمْ عَلَى نَبِيِّنَا مُحَمَّدٍ"
    )
    var selectedDhikrIndex by remember { mutableIntStateOf(0) }

    // Click scale animation
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1f,
        animationSpec = spring(stiffness = 500f),
        label = "buttonScale"
    )

    fun onCounterTap() {
        isPressed = true
        val nextCount = currentCount + 1
        val nextTotal = totalCount + 1
        currentCount = nextCount
        totalCount = nextTotal

        // Haptic feedback
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (targetGoal > 0 && nextCount % targetGoal == 0) {
                    vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 80, 80, 80), -1))
                } else {
                    vibrator?.vibrate(VibrationEffect.createOneShot(25, VibrationEffect.DEFAULT_AMPLITUDE))
                }
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(25)
            }
        } catch (_: Exception) {}

        coroutineScope.launch {
            preferencesManager.saveTasbeehCount(nextCount, nextTotal)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Dhikr phrase selection card
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "الذكر الحالي",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = dhikrPhrases[selectedDhikrIndex],
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = EmeraldPrimary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Quick selector buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    FilledTonalButton(
                        onClick = {
                            selectedDhikrIndex = (selectedDhikrIndex - 1 + dhikrPhrases.size) % dhikrPhrases.size
                            currentCount = 0
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.size(36.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Icon(Icons.Default.ChevronRight, contentDescription = "السابق")
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Text(
                        text = "${selectedDhikrIndex + 1} / ${dhikrPhrases.size}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.align(Alignment.CenterVertically)
                    )

                    Spacer(modifier = Modifier.width(16.dp))

                    FilledTonalButton(
                        onClick = {
                            selectedDhikrIndex = (selectedDhikrIndex + 1) % dhikrPhrases.size
                            currentCount = 0
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.size(36.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Icon(Icons.Default.ChevronLeft, contentDescription = "التالي")
                    }
                }
            }
        }

        // Target Goals Row (33, 100, 1000, مفتوح)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            listOf(33 to "٣٣", 100 to "١٠٠", 1000 to "١٠٠٠", 0 to "حر").forEach { (target, label) ->
                val isSelected = targetGoal == target
                FilterChip(
                    selected = isSelected,
                    onClick = {
                        targetGoal = target
                        coroutineScope.launch { preferencesManager.saveTasbeehTarget(target) }
                    },
                    label = { Text(label, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
        }

        // Large Interactive Misbaha Tap Area
        Box(
            modifier = Modifier
                .size(240.dp)
                .scale(scale)
                .shadow(16.dp, CircleShape, spotColor = EmeraldPrimary.copy(alpha = 0.5f))
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(EmeraldLight, EmeraldPrimary)
                    )
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {
                        onCounterTap()
                        isPressed = false
                    }
                )
                .testTag("tasbeeh_tap_button"),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "$currentCount",
                    fontSize = 58.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )

                if (targetGoal > 0) {
                    Text(
                        text = "الهدف: $targetGoal",
                        fontSize = 14.sp,
                        color = GoldAccent,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "المس للعد",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.75f)
                )
            }
        }

        // Stats & Reset Controls
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "إجمالي التسبيح",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "$totalCount تسبيحة",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = EmeraldPrimary
                    )
                }

                Row {
                    OutlinedButton(
                        onClick = {
                            currentCount = 0
                            coroutineScope.launch {
                                preferencesManager.saveTasbeehCount(0, totalCount)
                            }
                        },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("تصفير الدورة")
                    }
                }
            }
        }
    }
}
