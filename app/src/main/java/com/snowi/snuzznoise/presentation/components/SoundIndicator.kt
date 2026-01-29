package com.snowi.snuzznoise.presentation.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import androidx.compose.ui.window.Dialog
import com.snowi.snuzznoise.utils.SoundPlayer
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlin.math.*

@Composable
fun SoundIndicator(
    isMonitoring: Boolean,
    decibel: Double,
    monitoringDuration: Long,
    noiseLabel: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showInfoCard by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current

    if (showInfoCard) {
        InfoCardDialog(
            isMonitoring = isMonitoring,
            decibel = decibel,
            monitoringDuration = monitoringDuration,
            noiseLabel = noiseLabel,
            onDismissRequest = { showInfoCard = false }
        )
    }

    val normalizedDecibel = ((decibel.toFloat().coerceIn(30f, 90f) - 30f) / 60f).coerceIn(0f, 1f)

    val infiniteTransition = rememberInfiniteTransition(label = "IdlePulseTransition")
    val idlePulse by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "IdlePulse"
    )

    val shapeProgress by animateFloatAsState(
        targetValue = if (isMonitoring) 1f else 0f,
        animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
        label = "ShapeProgress"
    )

    val sunRayIntensity by animateFloatAsState(
        targetValue = if (isMonitoring) normalizedDecibel else 0f,
        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
        label = "SunRayIntensity"
    )

    val expansionScale by animateFloatAsState(
        targetValue = if (isMonitoring) 0.8f + (normalizedDecibel * 0.2f) else 1f,
        animationSpec = tween(durationMillis = 400, easing = LinearOutSlowInEasing),
        label = "ExpansionScale"
    )

    val reactivePulseScale by animateFloatAsState(
        targetValue = 1f + (normalizedDecibel * 0.25f),
        animationSpec = tween(durationMillis = 300, easing = LinearOutSlowInEasing),
        label = "ReactivePulseScale"
    )

    val pulseScale = if (shapeProgress < 0.5f) idlePulse else reactivePulseScale

    var rotation by remember { mutableStateOf(0f) }
    LaunchedEffect(isMonitoring, normalizedDecibel) {
        while (isMonitoring && currentCoroutineContext().isActive) {
            val speed = lerp(0.2f, 4.5f, normalizedDecibel)
            rotation = (rotation + speed) % 360f
            withFrameNanos {}
        }
    }

    Box(
        modifier = modifier
            .pointerInput(Unit) {
                detectTapGestures(
                    onLongPress = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        SoundPlayer.play()
                        showInfoCard = true
                    },
                    onTap = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        SoundPlayer.play()
                        onClick()
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize(expansionScale)
                .graphicsLayer(
                    scaleX = pulseScale,
                    scaleY = pulseScale,
                    rotationZ = rotation,
                    shape = SoundReactiveShape(shapeProgress, sunRayIntensity),
                    clip = true
                )
                .background(MaterialTheme.colorScheme.primary)
        )
    }
}

@Composable
private fun InfoCardDialog(
    isMonitoring: Boolean,
    decibel: Double,
    monitoringDuration: Long,
    noiseLabel: String,
    onDismissRequest: () -> Unit
) {
    fun formatDuration(seconds: Long): String {
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60
        return "%02d:%02d".format(minutes, remainingSeconds)
    }

    // --- DYNAMIC ICON LOGIC ---
    val sourceIcon = when (noiseLabel) {
        "Heartbeat" -> Icons.Default.Favorite
        "White Noise", "Static" -> Icons.Default.BlurOn
        "Fan", "Air Conditioner", "Breathing" -> Icons.Default.AcUnit
        "Rain", "Water" -> Icons.Default.Grain
        "Talking", "Speech", "Laughter", "Singing" -> Icons.Default.Mic
        "Typing", "Keyboard" -> Icons.Default.Keyboard
        "Music" -> Icons.Default.MusicNote
        "Footsteps", "Clapping" -> Icons.Default.DirectionsWalk
        "Baby Crying" -> Icons.Default.Face
        "Screaming", "Glass Breaking", "Gunshot", "Siren", "Alarm" -> Icons.Default.Warning
        else -> Icons.Default.GraphicEq // Covers "Unknown"
    }

    Dialog(onDismissRequest = onDismissRequest) {
        Card(
            shape = RoundedCornerShape(24.dp),
            // UPDATED: Using MaterialTheme colors (Respects User Preference/System Theme)
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh, // Slightly lighter surface for popup
                contentColor = MaterialTheme.colorScheme.onSurface
            ),
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant, // Subtle theme border
                    shape = RoundedCornerShape(24.dp)
                )
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary // Matches your app's primary color
                    )
                    Text(
                        text = "Monitoring Status",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                // Content Rows
                InfoRow(
                    icon = Icons.Default.VolumeUp,
                    label = "Status",
                    value = if (isMonitoring) "Active" else "Paused",
                    // Use semantically correct colors, but safe for light/dark mode
                    accentColor = if (isMonitoring) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error
                )
                InfoRow(
                    icon = Icons.Default.GraphicEq,
                    label = "Level",
                    value = if (isMonitoring) "%.1f dB".format(decibel) else "-- dB"
                )

                // Label: Shows "--" if Unknown, otherwise the Name
                InfoRow(
                    icon = sourceIcon,
                    label = "Noise Type",
                    value = if (isMonitoring && noiseLabel != "Unknown") noiseLabel else "--"
                )

                InfoRow(
                    icon = Icons.Default.Timer,
                    label = "Duration",
                    value = formatDuration(monitoringDuration)
                )
            }
        }
    }
}

@Composable
private fun InfoRow(
    icon: ImageVector,
    label: String,
    value: String,
    accentColor: Color? = null
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            modifier = Modifier.size(22.dp),
            // UPDATED: Matches the theme's primary color
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.width(16.dp))
        Text(
            text = "$label:",
            fontWeight = FontWeight.Medium,
            style = MaterialTheme.typography.bodyLarge,
            // UPDATED: Subtle variant color
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.weight(1f))
        Text(
            text = value,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.bodyLarge,
            // UPDATED: Uses provided accent or defaults to standard text color
            color = accentColor ?: MaterialTheme.colorScheme.onSurface
        )
    }
}

private class SoundReactiveShape(
    private val shapeProgress: Float,
    private val sunRayIntensity: Float,
) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val path = Path()
        val radius = size.minDimension / 2f
        val numPoints = 120
        val points = (0..numPoints).map { i ->
            val angle = i * (2 * PI / numPoints).toFloat()
            val baseRadius = radius
            val cookieAmplitude = 0.1f * radius
            val cookieRadius = baseRadius - cookieAmplitude + cookieAmplitude * sin(angle * 4)
            val sunAmplitude = 0.2f * radius
            val sunRadius = baseRadius - sunAmplitude + sunAmplitude * sin(angle * 10)
            val expressiveRadius = lerp(cookieRadius, sunRadius, sunRayIntensity)
            val finalRadius = lerp(baseRadius, expressiveRadius, shapeProgress)
            Offset(
                x = finalRadius * cos(angle) + (size.width / 2f),
                y = finalRadius * sin(angle) + (size.height / 2f)
            )
        }
        if (points.isNotEmpty()) {
            path.moveTo(points.first().x, points.first().y)
            for (i in 0 until numPoints) {
                val p0 = points[(i - 1 + numPoints) % numPoints]
                val p1 = points[i]
                val p2 = points[(i + 1) % numPoints]
                val p3 = points[(i + 2) % numPoints]
                for (t in 1..10) {
                    val tFloat = t / 10f
                    val x = 0.5f * ((2 * p1.x) + (-p0.x + p2.x) * tFloat + (2 * p0.x - 5 * p1.x + 4 * p2.x - p3.x) * tFloat * tFloat + (-p0.x + 3 * p1.x - 3 * p2.x + p3.x) * tFloat * tFloat * tFloat)
                    val y = 0.5f * ((2 * p1.y) + (-p0.y + p2.y) * tFloat + (2 * p0.y - 5 * p1.y + 4 * p2.y - p3.y) * tFloat * tFloat + (-p0.y + 3 * p1.y - 3 * p2.y + p3.y) * tFloat * tFloat * tFloat)
                    path.lineTo(x, y)
                }
            }
            path.close()
        }
        return Outline.Generic(path)
    }
}