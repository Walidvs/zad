package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.audio.AdhanPlayer
import com.example.audio.QuranAudioManager
import com.example.data.PreferencesManager
import com.example.ui.components.AdhanDialog
import com.example.ui.screens.*
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GoldAccent

enum class ZadTab(val titleAr: String) {
    PRAYER("المواقيت"),
    QIBLA("القبلة"),
    MUSHAF("المصحف"),
    TAFSEER("التفسير"),
    AZKAR("الأذكار")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val context = LocalContext.current
    val preferencesManager = remember { PreferencesManager(context) }
    val adhanPlayer = remember { AdhanPlayer(context) }
    val quranAudioManager = remember { QuranAudioManager(context) }

    DisposableEffect(Unit) {
        onDispose {
            adhanPlayer.release()
            quranAudioManager.release()
        }
    }

    var selectedTab by remember { mutableStateOf(ZadTab.PRAYER) }
    var showAdhanQuickDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }

    // RTL back handler returns to Prayer home tab
    BackHandler(enabled = selectedTab != ZadTab.PRAYER) {
        selectedTab = ZadTab.PRAYER
    }

    // Force Arabic RTL layout direction throughout Zad
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(GoldAccent.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Mosque,
                                    contentDescription = null,
                                    tint = GoldAccent,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "زاد المسلم",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 20.sp
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = { showAboutDialog = true },
                            modifier = Modifier.testTag("about_app_btn")
                        ) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = "عن التطبيق والمطور",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    actions = {
                        // Quick Adhan Audio button in Top Bar
                        FilledTonalIconButton(
                            onClick = { showAdhanQuickDialog = true },
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            ),
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .testTag("top_adhan_quick_btn")
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = "الأذان",
                                tint = EmeraldPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            },
            bottomBar = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 4.dp
                    ) {
                        // 1. Prayer Tab
                        NavigationBarItem(
                            selected = selectedTab == ZadTab.PRAYER,
                            onClick = { selectedTab = ZadTab.PRAYER },
                            icon = { Icon(Icons.Default.Schedule, contentDescription = "المواقيت") },
                            label = { Text(ZadTab.PRAYER.titleAr, fontSize = 11.sp, fontWeight = if (selectedTab == ZadTab.PRAYER) FontWeight.Bold else FontWeight.Normal) },
                            modifier = Modifier.testTag("nav_prayer")
                        )

                        // 2. Qibla Tab
                        NavigationBarItem(
                            selected = selectedTab == ZadTab.QIBLA,
                            onClick = { selectedTab = ZadTab.QIBLA },
                            icon = { Icon(Icons.Default.Explore, contentDescription = "القبلة") },
                            label = { Text(ZadTab.QIBLA.titleAr, fontSize = 11.sp, fontWeight = if (selectedTab == ZadTab.QIBLA) FontWeight.Bold else FontWeight.Normal) },
                            modifier = Modifier.testTag("nav_qibla")
                        )

                        // 3. Mushaf Tab (Dedicated page for Holy Quran reading)
                        NavigationBarItem(
                            selected = selectedTab == ZadTab.MUSHAF,
                            onClick = { selectedTab = ZadTab.MUSHAF },
                            icon = { Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = "المصحف") },
                            label = { Text(ZadTab.MUSHAF.titleAr, fontSize = 11.sp, fontWeight = if (selectedTab == ZadTab.MUSHAF) FontWeight.Bold else FontWeight.Normal) },
                            modifier = Modifier.testTag("nav_mushaf")
                        )

                        // 4. Tafseer Tab (Dedicated page for Ayah-by-Ayah Tafseer)
                        NavigationBarItem(
                            selected = selectedTab == ZadTab.TAFSEER,
                            onClick = { selectedTab = ZadTab.TAFSEER },
                            icon = { Icon(Icons.Default.AutoStories, contentDescription = "التفسير") },
                            label = { Text(ZadTab.TAFSEER.titleAr, fontSize = 11.sp, fontWeight = if (selectedTab == ZadTab.TAFSEER) FontWeight.Bold else FontWeight.Normal) },
                            modifier = Modifier.testTag("nav_tafseer")
                        )

                        // 5. Azkar and Worship Tab (Azkar, Tasbeeh, Guides)
                        NavigationBarItem(
                            selected = selectedTab == ZadTab.AZKAR,
                            onClick = { selectedTab = ZadTab.AZKAR },
                            icon = { Icon(Icons.Default.Favorite, contentDescription = "الأذكار") },
                            label = { Text(ZadTab.AZKAR.titleAr, fontSize = 11.sp, fontWeight = if (selectedTab == ZadTab.AZKAR) FontWeight.Bold else FontWeight.Normal) },
                            modifier = Modifier.testTag("nav_azkar")
                        )
                    }

                    // Developer Credit Footer Bar (Developed by H.WALID)
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showAboutDialog = true }
                            .padding(bottom = 6.dp, top = 2.dp)
                            .testTag("developed_by_footer")
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Developed by H.WALID",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldPrimary,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (selectedTab) {
                    ZadTab.PRAYER -> PrayerScreen(
                        adhanPlayer = adhanPlayer,
                        preferencesManager = preferencesManager
                    )
                    ZadTab.QIBLA -> QiblaScreen(
                        preferencesManager = preferencesManager
                    )
                    ZadTab.MUSHAF -> MushafScreen(
                        quranAudioManager = quranAudioManager,
                        preferencesManager = preferencesManager
                    )
                    ZadTab.TAFSEER -> TafseerScreen(
                        adhanPlayer = adhanPlayer,
                        preferencesManager = preferencesManager
                    )
                    ZadTab.AZKAR -> AzkarAndWorshipScreen(
                        preferencesManager = preferencesManager
                    )
                }
            }
        }

        if (showAdhanQuickDialog) {
            AdhanDialog(
                adhanPlayer = adhanPlayer,
                onDismissRequest = { showAdhanQuickDialog = false }
            )
        }

        // About Application & Developer Modal
        if (showAboutDialog) {
            Dialog(onDismissRequest = { showAboutDialog = false }) {
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .testTag("about_dialog")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(22.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(EmeraldContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Mosque,
                                contentDescription = null,
                                tint = EmeraldPrimary,
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "تطبيق زاد المسلم (Zad)",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = EmeraldContainer
                        ) {
                            Text(
                                text = "Developed by H.WALID",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = EmeraldPrimary,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "تطبيق إسلامي شامل يضم مواقيت الصلاة والأذان، بوصلة القبلة، المصحف الشريف، تفسير الآيات آية بآية، الأذكار، والسبحة الإلكترونية.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            lineHeight = 22.sp
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        Button(
                            onClick = { showAboutDialog = false },
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("إغلاق", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
