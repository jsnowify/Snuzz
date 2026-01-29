package com.snowi.snuzznoise.presentation.feature.history.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.snowi.snuzznoise.presentation.feature.history.NoiseEvent
import com.snowi.snuzznoise.presentation.feature.history.NoiseStats
import com.snowi.snuzznoise.presentation.feature.history.TimeRange
import java.text.SimpleDateFormat
import java.util.*

// --- 1. THE ALERT CARD ---
@Composable
fun AlertEventItem(event: NoiseEvent) {
    val timeFormatter = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }
    val dateFormatter = remember { SimpleDateFormat("MMM d", Locale.getDefault()) }
    val severityColor = if (event.decibelLevel > 80) MaterialTheme.colorScheme.error else Color(0xFFFFC107)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(severityColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getHistoryNoiseIcon(event.noiseType),
                    contentDescription = null,
                    tint = severityColor,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if(event.noiseType == "Unknown") "High Noise Detected" else event.noiseType,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${dateFormatter.format(Date(event.timestamp))} • ${timeFormatter.format(Date(event.timestamp))}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = severityColor.copy(alpha = 0.1f)
            ) {
                Text(
                    text = "${event.decibelLevel.toInt()} dB",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Black,
                    color = severityColor,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }
    }
}

private fun getHistoryNoiseIcon(type: String): ImageVector {
    return when {
        type.contains("Traffic", true) -> Icons.Default.Warning
        type.contains("Talk", true) -> Icons.Default.GraphicEq
        type.contains("Loud", true) -> Icons.Default.VolumeUp
        else -> Icons.Default.Notifications
    }
}

// --- 2. LEGEND ---
@Composable
fun ChartLegend(stats: NoiseStats) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            LegendItem("Peak", "${stats.peakLevel.toInt()} dB", MaterialTheme.colorScheme.error)
            LegendItem("Average", "${stats.averageLevel.toInt()} dB", MaterialTheme.colorScheme.primary)
            LegendItem("Alerts", stats.alertEvents.toString(), MaterialTheme.colorScheme.secondary)
        }
    }
}

@Composable
fun LegendItem(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = color)
    }
}

// --- 3. HEADERS & EMPTY STATES ---
@Composable
fun HistoryHeader(selectedTimeRange: TimeRange, onTimeRangeSelected: (TimeRange) -> Unit) {
    Column {
        Text("History", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
        TimeRangeSelector(selectedTimeRange, onTimeRangeSelected)
    }
}

@Composable
fun SignificantEventsHeader(alertCount: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.WarningAmber, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text("Significant Events ($alertCount)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
fun EmptyHistoryState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("No Data Yet", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun NoSignificantEventsCard() {
    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
        Text("No alerts in this period. All quiet!", color = MaterialTheme.colorScheme.secondary)
    }
}

// --- 4. TIME RANGE SELECTOR ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeRangeSelector(selected: TimeRange, onSelect: (TimeRange) -> Unit) {
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        TimeRange.entries.forEachIndexed { index, range ->
            SegmentedButton(
                shape = SegmentedButtonDefaults.itemShape(index, TimeRange.entries.size),
                onClick = { onSelect(range) },
                selected = range == selected
            ) { Text(range.shortLabel) }
        }
    }
}