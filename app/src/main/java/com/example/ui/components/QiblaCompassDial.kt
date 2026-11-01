package com.example.ui.components

import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Mosque
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GoldAccent
import kotlin.math.*

@Composable
fun QiblaCompassDial(
    azimuth: Float,
    qiblaBearing: Float,
    modifier: Modifier = Modifier,
    onAligned: () -> Unit = {}
) {
    val context = LocalContext.current
    val vibrator = remember { context.getSystemService(Vibrator::class.java) }

    // Calculate relative angle to Qibla
    var relativeQiblaAngle = (qiblaBearing - azimuth + 360f) % 360f
    val deviation = abs(if (relativeQiblaAngle > 180f) relativeQiblaAngle - 360f else relativeQiblaAngle)
    val isAligned = deviation <= 3.5f

    // Trigger haptic feedback once when alignment is achieved
    var wasAligned by remember { mutableStateOf(false) }
    LaunchedEffect(isAligned) {
        if (isAligned && !wasAligned) {
            wasAligned = true
            onAligned()
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(100)
                }
            } catch (_: Exception) {}
        } else if (!isAligned) {
            wasAligned = false
        }
    }

    // Smooth needle animation
    val animatedAzimuth by animateFloatAsState(
        targetValue = -azimuth,
        animationSpec = spring(stiffness = 300f, dampingRatio = 0.8f),
        label = "compassRotation"
    )

    val ringColor by animateColorAsState(
        targetValue = if (isAligned) EmeraldPrimary else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
        label = "ringColor"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.testTag("qibla_compass_dial")
    ) {
        Box(
            modifier = Modifier
                .size(290.dp)
                .shadow(16.dp, CircleShape, spotColor = if (isAligned) EmeraldPrimary else Color.Black.copy(alpha = 0.15f))
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface),
            contentAlignment = Alignment.Center
        ) {
            // Rotating Compass Dial Canvas
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .rotate(animatedAzimuth)
            ) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val radius = size.minDimension / 2f - 16.dp.toPx()

                // Outer border circle
                drawCircle(
                    color = ringColor,
                    radius = radius,
                    center = center,
                    style = Stroke(width = 3.dp.toPx())
                )

                // Degree ticks around perimeter
                for (i in 0 until 360 step 5) {
                    val angleRad = Math.toRadians((i - 90).toDouble())
                    val isMajor = i % 30 == 0
                    val isCardinal = i % 90 == 0

                    val tickLength = if (isCardinal) 18.dp.toPx() else if (isMajor) 12.dp.toPx() else 6.dp.toPx()
                    val tickColor = if (i == 0) Color(0xFFE53935) else if (isMajor) Color.Gray else Color.LightGray.copy(alpha = 0.6f)
                    val strokeW = if (isCardinal) 3.dp.toPx() else if (isMajor) 2.dp.toPx() else 1.dp.toPx()

                    val startX = center.x + (radius - tickLength) * cos(angleRad).toFloat()
                    val startY = center.y + (radius - tickLength) * sin(angleRad).toFloat()
                    val endX = center.x + radius * cos(angleRad).toFloat()
                    val endY = center.y + radius * sin(angleRad).toFloat()

                    drawLine(
                        color = tickColor,
                        start = Offset(startX, startY),
                        end = Offset(endX, endY),
                        strokeWidth = strokeW,
                        cap = StrokeCap.Round
                    )
                }

                // Kaaba marker at exact qiblaBearing
                val qiblaRad = Math.toRadians((qiblaBearing - 90).toDouble())
                val kaabaMarkerRadius = radius - 26.dp.toPx()
                val kaabaX = center.x + kaabaMarkerRadius * cos(qiblaRad).toFloat()
                val kaabaY = center.y + kaabaMarkerRadius * sin(qiblaRad).toFloat()

                // Draw glowing Kaaba target point
                drawCircle(
                    color = GoldAccent,
                    radius = 12.dp.toPx(),
                    center = Offset(kaabaX, kaabaY)
                )
                drawCircle(
                    color = Color(0xFF263238),
                    radius = 8.dp.toPx(),
                    center = Offset(kaabaX, kaabaY)
                )

                // Line connecting center to Kaaba marker
                drawLine(
                    color = GoldAccent.copy(alpha = 0.7f),
                    start = center,
                    end = Offset(kaabaX, kaabaY),
                    strokeWidth = 2.5.dp.toPx(),
                    cap = StrokeCap.Round
                )

                // Magnetic North needle (Red) and South needle (Silver)
                val needleLength = radius - 45.dp.toPx()
                val needleWidth = 14.dp.toPx()

                // North arrow (Points to 0°)
                val northPath = Path().apply {
                    moveTo(center.x, center.y - needleLength)
                    lineTo(center.x - needleWidth / 2f, center.y)
                    lineTo(center.x + needleWidth / 2f, center.y)
                    close()
                }
                drawPath(northPath, brush = Brush.verticalGradient(listOf(Color(0xFFE53935), Color(0xFFC62828))))

                // South arrow
                val southPath = Path().apply {
                    moveTo(center.x, center.y + needleLength)
                    lineTo(center.x - needleWidth / 2f, center.y)
                    lineTo(center.x + needleWidth / 2f, center.y)
                    close()
                }
                drawPath(southPath, brush = Brush.verticalGradient(listOf(Color(0xFFB0BEC5), Color(0xFF78909C))))

                // Center hub
                drawCircle(color = Color.White, radius = 9.dp.toPx(), center = center)
                drawCircle(color = EmeraldPrimary, radius = 6.dp.toPx(), center = center)
            }

            // Fixed Center Overlay (Shows Kaaba Icon & Alignment Status)
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .background(
                        if (isAligned) EmeraldPrimary.copy(alpha = 0.95f)
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isAligned) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = "تم محاذاة القبلة",
                        tint = GoldAccent,
                        modifier = Modifier.size(38.dp)
                    )
                } else {
                    Icon(
                        Icons.Default.Mosque,
                        contentDescription = "اتجاه الكعبة",
                        tint = if (isAligned) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(34.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Alignment Banner
        Surface(
            shape = CircleShape,
            color = if (isAligned) EmeraldPrimary else MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isAligned) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = GoldAccent,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "أنت الآن باتجاه القبلة مباشرة!",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color.White
                    )
                } else {
                    Text(
                        text = "قم بتدوير الهاتف بمقدار ${deviation.roundToInt()}° لتوجيهه نحو القبلة",
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
