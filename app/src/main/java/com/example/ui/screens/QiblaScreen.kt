package com.example.ui.screens

import android.hardware.SensorManager
import androidx.compose.foundation.background
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.CityData
import com.example.data.PrayerCalculationEngine
import com.example.data.PreferencesManager
import com.example.sensor.CompassManager
import com.example.ui.components.QiblaCompassDial
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GoldAccent
import kotlin.math.roundToInt

@Composable
fun QiblaScreen(
    preferencesManager: PreferencesManager,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val compassManager = remember { CompassManager(context) }

    DisposableEffect(Unit) {
        compassManager.startListening()
        onDispose {
            compassManager.stopListening()
        }
    }

    val compassState by compassManager.compassState.collectAsState()
    val currentCity by preferencesManager.selectedCityFlow.collectAsState(initial = CityData.defaultCity)

    // Calculate Qibla bearing from current location
    val qiblaBearing = remember(currentCity) {
        PrayerCalculationEngine.calculateQibla(currentCity.latitude, currentCity.longitude).toFloat()
    }

    // Distance to Kaaba
    val distanceKm = remember(currentCity) {
        PrayerCalculationEngine.calculateDistanceToKaaba(currentCity.latitude, currentCity.longitude).roundToInt()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Location and Qibla Bearing Header Card
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = EmeraldPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = currentCity.nameAr,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "المسافة إلى الكعبة: $distanceKm كم",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Bearing Badge
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = EmeraldPrimary,
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "${qiblaBearing.roundToInt()}°",
                            color = GoldAccent,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp
                        )
                        Text(
                            text = "زاوية القبلة",
                            color = Color.White,
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Compass Dial Widget
        QiblaCompassDial(
            azimuth = compassState.azimuth,
            qiblaBearing = qiblaBearing,
            modifier = Modifier.padding(vertical = 10.dp)
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Live Telemetry Readout
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                // Device Current Heading
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "اتجاه الهاتف",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${compassState.azimuth.roundToInt()}°",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Sensor Accuracy Status
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "دقة الحساس",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    val (statusText, statusColor) = when (compassState.accuracy) {
                        SensorManager.SENSOR_STATUS_ACCURACY_HIGH -> "ممتازة" to EmeraldPrimary
                        SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM -> "متوسطة" to GoldAccent
                        else -> "تحتاج معايرة" to MaterialTheme.colorScheme.error
                    }
                    Text(
                        text = statusText,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = statusColor
                    )
                }

                // Kaaba Bearing
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "اتجاه مكة",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${qiblaBearing.roundToInt()}°",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = EmeraldPrimary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Calibration Advice Card
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Info,
                    contentDescription = null,
                    tint = EmeraldPrimary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "للحصول على أدق نتيجة، ضع الهاتف أفقياً بعيداً عن الأجهزة الإلكترونية والمعادن، وحرّكه على شكل الرقم (8) لمعايرة البوصلة.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp
                )
            }
        }
    }
}
