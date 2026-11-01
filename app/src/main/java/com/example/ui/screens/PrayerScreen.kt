package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.location.Geocoder
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.audio.AdhanPlayer
import com.example.data.*
import com.example.ui.components.AdhanDialog
import com.example.ui.theme.EmeraldLight
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GoldAccent
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrayerScreen(
    adhanPlayer: AdhanPlayer,
    preferencesManager: PreferencesManager,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val currentCity by preferencesManager.selectedCityFlow.collectAsState(initial = CityData.defaultCity)
    val calcMethod by preferencesManager.calculationMethodFlow.collectAsState(initial = CalculationMethod.UMM_AL_QURA)

    var showAdhanDialog by remember { mutableStateOf(false) }
    var showCityPicker by remember { mutableStateOf(false) }
    var showMethodPicker by remember { mutableStateOf(false) }

    // Live clock ticker
    var currentTimeMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000L)
            currentTimeMillis = System.currentTimeMillis()
        }
    }

    // Calculate prayer times
    val prayerResult = remember(currentCity, calcMethod, currentTimeMillis / 60000L) {
        PrayerCalculationEngine.calculatePrayerTimes(
            latitude = currentCity.latitude,
            longitude = currentCity.longitude,
            calendar = Calendar.getInstance(),
            method = calcMethod
        )
    }

    // Permission launcher for GPS
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            try {
                val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
                fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
                    if (loc != null) {
                        coroutineScope.launch {
                            var localityName = "موقعي الحالي"
                            try {
                                val geocoder = Geocoder(context, Locale("ar"))
                                val addresses = geocoder.getFromLocation(loc.latitude, loc.longitude, 1)
                                if (!addresses.isNullOrEmpty()) {
                                    localityName = addresses[0].locality ?: addresses[0].adminArea ?: "موقعي الحالي"
                                }
                            } catch (_: Exception) {}
                            preferencesManager.saveAutoLocation(loc.latitude, loc.longitude, localityName)
                            preferencesManager.saveCity(
                                City("gps", localityName, "", loc.latitude, loc.longitude, calcMethod)
                            )
                        }
                    }
                }
            } catch (_: SecurityException) {}
        }
    }

    val prayersList = listOf(
        prayerResult.fajr,
        prayerResult.sunrise,
        prayerResult.dhuhr,
        prayerResult.asr,
        prayerResult.maghrib,
        prayerResult.isha
    )

    // Notification states per prayer
    var notifSettings by remember {
        mutableStateOf(mutableMapOf("fajr" to true, "dhuhr" to true, "asr" to true, "maghrib" to true, "isha" to true))
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Date and Location Header
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // City / GPS Button
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { showCityPicker = true }
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                        .testTag("city_selector_btn"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = "الموقع",
                        tint = EmeraldPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = currentCity.nameAr,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                }

                // Hijri Date
                Text(
                    text = prayerResult.hijriDate,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Hero Card: Next Prayer & Countdown
        Card(
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("next_prayer_hero_card")
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(
                            listOf(EmeraldPrimary, EmeraldLight)
                        )
                    )
                    .padding(22.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Next Prayer Tag
                    Surface(
                        color = GoldAccent.copy(alpha = 0.25f),
                        shape = CircleShape,
                        border = ButtonDefaults.outlinedButtonBorder.copy(brush = Brush.linearGradient(listOf(GoldAccent, GoldAccent)))
                    ) {
                        Text(
                            text = "الصلاة القادمة",
                            color = GoldAccent,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = prayerResult.nextPrayer.nameAr,
                        color = Color.White,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold
                    )

                    Text(
                        text = prayerResult.nextPrayer.timeFormatted,
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Time Remaining Display
                    val hours = (prayerResult.timeToNextMillis / (1000 * 60 * 60)) % 24
                    val minutes = (prayerResult.timeToNextMillis / (1000 * 60)) % 60
                    val seconds = (prayerResult.timeToNextMillis / 1000) % 60
                    val timeFormatted = String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.Black.copy(alpha = 0.25f),
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.AccessTime, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "متبقي $timeFormatted",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Listen to Adhan Action Button
                    Button(
                        onClick = { showAdhanDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = GoldAccent),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.testTag("listen_adhan_button")
                    ) {
                        Icon(Icons.Default.VolumeUp, contentDescription = null, tint = Color.Black)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "استمع لصوت الأذان والنداء",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Daily Prayer Times List Title
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "مواقيت اليوم",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            TextButton(
                onClick = { showMethodPicker = true },
                modifier = Modifier.testTag("change_calc_method_btn")
            ) {
                Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = "طريقة الحساب", fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Individual Prayer Cards
        prayersList.forEach { prayer ->
            val isNext = prayer.isNext
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isNext) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = if (isNext) 4.dp else 1.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 5.dp)
                    .testTag("prayer_item_${prayer.id}")
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
                                .background(
                                    if (isNext) EmeraldPrimary else MaterialTheme.colorScheme.surfaceVariant
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = when (prayer.id) {
                                    "fajr" -> Icons.Default.WbTwilight
                                    "sunrise" -> Icons.Default.WbSunny
                                    "dhuhr" -> Icons.Default.LightMode
                                    "asr" -> Icons.Default.Cloud
                                    "maghrib" -> Icons.Default.NightsStay
                                    else -> Icons.Default.Bedtime
                                },
                                contentDescription = null,
                                tint = if (isNext) GoldAccent else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column {
                            Text(
                                text = prayer.nameAr,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium,
                                color = if (isNext) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                            )
                            if (isNext) {
                                Text(
                                    text = "الصلاة الحالية / القادمة",
                                    fontSize = 11.sp,
                                    color = EmeraldPrimary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = prayer.timeFormatted,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = if (isNext) EmeraldPrimary else MaterialTheme.colorScheme.onSurface
                        )

                        if (prayer.id != "sunrise") {
                            Spacer(modifier = Modifier.width(10.dp))
                            val isBellOn = notifSettings[prayer.id] ?: true
                            IconButton(
                                onClick = {
                                    notifSettings = notifSettings.toMutableMap().apply {
                                        put(prayer.id, !isBellOn)
                                    }
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = if (isBellOn) Icons.Outlined.NotificationsActive else Icons.Outlined.Notifications,
                                    contentDescription = "تنبيه الأذان",
                                    tint = if (isBellOn) EmeraldPrimary else MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Adhan Dialog
    if (showAdhanDialog) {
        AdhanDialog(
            adhanPlayer = adhanPlayer,
            onDismissRequest = { showAdhanDialog = false }
        )
    }

    // City Selector Dialog
    if (showCityPicker) {
        CityPickerDialog(
            currentCity = currentCity,
            onCitySelected = { selected ->
                coroutineScope.launch {
                    preferencesManager.saveCity(selected)
                }
                showCityPicker = false
            },
            onUseGps = {
                locationPermissionLauncher.launch(
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
                )
                showCityPicker = false
            },
            onDismiss = { showCityPicker = false }
        )
    }

    // Calculation Method Dialog
    if (showMethodPicker) {
        CalculationMethodDialog(
            currentMethod = calcMethod,
            onMethodSelected = { selected ->
                coroutineScope.launch {
                    preferencesManager.saveCalculationMethod(selected)
                }
                showMethodPicker = false
            },
            onDismiss = { showMethodPicker = false }
        )
    }
}

@Composable
fun CityPickerDialog(
    currentCity: City,
    onCitySelected: (City) -> Unit,
    onUseGps: () -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredCities = remember(searchQuery) {
        if (searchQuery.isBlank()) CityData.popularCities
        else CityData.popularCities.filter {
            it.nameAr.contains(searchQuery.trim()) || it.countryAr.contains(searchQuery.trim())
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.8f)
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Text(
                    text = "اختر المدينة لمواقيت الصلاة والقبلة",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(12.dp))

                // GPS Auto Button
                FilledTonalButton(
                    onClick = onUseGps,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.MyLocation, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "تحديد موقعي تلقائياً عبر GPS")
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("ابحث عن مدينة أو دولة...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(filteredCities) { city ->
                        val isSelected = city.id == currentCity.id
                        ListItem(
                            headlineContent = { Text(city.nameAr, fontWeight = FontWeight.Bold) },
                            supportingContent = { Text(city.countryAr) },
                            trailingContent = {
                                if (isSelected) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = EmeraldPrimary)
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onCitySelected(city) }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    }
                }
            }
        }
    }
}

@Composable
fun CalculationMethodDialog(
    currentMethod: CalculationMethod,
    onMethodSelected: (CalculationMethod) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "طريقة حساب مواقيت الصلاة",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(12.dp))

                CalculationMethod.values().forEach { method ->
                    val isSelected = method == currentMethod
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onMethodSelected(method) },
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = isSelected, onClick = { onMethodSelected(method) })
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = method.titleAr,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        }
    }
}
