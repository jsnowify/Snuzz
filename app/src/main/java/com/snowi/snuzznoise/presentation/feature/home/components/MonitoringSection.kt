package com.snowi.snuzznoise.presentation.feature.home.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import com.snowi.snuzznoise.presentation.components.SoundIndicator

@Composable
fun MonitoringSection(
    isMonitoring: Boolean,
    currentDecibel: Double,
    noiseLabel: String, // This holds the REAL detecting sound (e.g., "Music")
    monitoringDuration: Long,
    selectedProfileName: String?,
    hasAudioPermission: Boolean,
    shouldShowRationale: Boolean,
    modifier: Modifier = Modifier,
    onIndicatorClick: () -> Unit
) {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val screenHeight = configuration.screenHeightDp.dp

    // RESPONSIVE SIZE CALCULATION:
    // 1. Take 70% of screen width OR 40% of screen height (whichever is smaller)
    // 2. Clamp it: Minimum 220dp (for small phones), Maximum 320dp (for tablets)
    val baseSize = min(screenWidth * 0.7f, screenHeight * 0.45f)
    val circleSize = baseSize.coerceIn(220.dp, 320.dp)

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // 1. The Circle Indicator
        SoundIndicator(
            isMonitoring = isMonitoring,
            decibel = currentDecibel,

            // FIX: Pass the actual detected sound (e.g., "Music") instead of the Profile Name
            noiseLabel = noiseLabel,

            monitoringDuration = monitoringDuration,
            modifier = Modifier
                .size(circleSize) // Apply responsive size here
                .clickable { onIndicatorClick() },
            onClick = onIndicatorClick
        )

        // Flexible spacing instead of fixed 32.dp
        Spacer(modifier = Modifier.height(if (screenHeight < 600.dp) 16.dp else 32.dp))

        // 2. The Dynamic Text
        // (This part is fine to keep as Profile Name, so the user knows WHICH rule is active)
        val statusText = when {
            isMonitoring -> "Monitoring in ${selectedProfileName ?: "Default Mode"}"
            hasAudioPermission -> "Tap to start listening"
            else -> "Permission required"
        }

        Text(
            text = statusText,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}