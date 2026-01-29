package com.snowi.snuzznoise.presentation.feature.notification

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import java.text.SimpleDateFormat
import java.util.*

// --- HELPER FUNCTIONS ---

private fun getNoiseIcon(noiseType: String): ImageVector {
    return when {
        noiseType.contains("Traffic", ignoreCase = true) -> Icons.Default.Warning
        noiseType.contains("Talk", ignoreCase = true) -> Icons.Default.GraphicEq
        noiseType.contains("Loud", ignoreCase = true) -> Icons.Default.VolumeUp
        else -> Icons.Default.Notifications
    }
}

private fun getFormattedDate(timestamp: Long): String {
    val messageCal = Calendar.getInstance().apply { timeInMillis = timestamp }
    val nowCal = Calendar.getInstance()

    return when {
        messageCal.get(Calendar.YEAR) == nowCal.get(Calendar.YEAR) && messageCal.get(Calendar.DAY_OF_YEAR) == nowCal.get(Calendar.DAY_OF_YEAR) -> "Today"
        messageCal.get(Calendar.YEAR) == nowCal.get(Calendar.YEAR) && messageCal.get(Calendar.DAY_OF_YEAR) == nowCal.get(Calendar.DAY_OF_YEAR) - 1 -> "Yesterday"
        else -> SimpleDateFormat("MMMM d, yyyy", Locale.getDefault()).format(Date(timestamp))
    }
}

@Composable
fun getDecibelColor(db: Int): Color {
    return when {
        db < 50 -> MaterialTheme.colorScheme.primary // Safe
        db < 70 -> Color(0xFFFFC107) // Amber (Warning)
        else -> MaterialTheme.colorScheme.error // Red (Danger)
    }
}

fun getNoiseAdvice(db: Int): String {
    return when {
        db < 50 -> "Comfortable level. No action needed."
        db < 70 -> "Distracting level. Can affect focus."
        db < 85 -> "Loud. Annoying over time."
        else -> "Dangerous! Prolonged exposure can harm hearing."
    }
}

// --- MAIN SCREEN COMPOSABLE ---

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun NotificationScreen(
    navController: NavController,
    viewModel: NotificationViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    val sortedNotifications = remember(state.notifications) {
        state.notifications.sortedByDescending { it.timestamp }
    }

    val groupedNotifications = remember(sortedNotifications) {
        sortedNotifications.groupBy { getFormattedDate(it.timestamp) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notifications") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)) {
            when {
                state.isLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                state.error != null -> {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = "Error", tint = MaterialTheme.colorScheme.error)
                        Text("Error loading notifications", style = MaterialTheme.typography.titleMedium)
                    }
                }
                groupedNotifications.isEmpty() -> {
                    EmptyState()
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        groupedNotifications.forEach { (date, notifications) ->
                            stickyHeader {
                                DateHeader(text = date)
                            }
                            items(notifications) { notification ->
                                NotificationCard(item = notification)
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- UI COMPONENTS ---

@Composable
fun DateHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(vertical = 8.dp)
    )
}

@Composable
fun NotificationCard(item: NotificationItem) {
    var expanded by remember { mutableStateOf(false) }
    val timeFormatter = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }

    // Helpers for the detailed view
    val intensityProgress = (item.decibelLevel / 120f).coerceIn(0f, 1f)
    val intensityColor = getDecibelColor(item.decibelLevel)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
            .clickable { expanded = !expanded },
        colors = CardDefaults.cardColors(
            containerColor = if (expanded) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(if (expanded) 6.dp else 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // --- HEADER ROW (Always Visible) ---
            Row(verticalAlignment = Alignment.Top) {
                // Icon Box
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(intensityColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = getNoiseIcon(item.noiseType),
                        contentDescription = "Noise Icon",
                        tint = intensityColor,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    // --- CLEAN NUMBER DISPLAY ---
                    // This shows "85 dB" cleanly under the title (No "LEVEL:", No Red)
                    if (item.decibelLevel > 0) {
                        Text(
                            text = "${item.decibelLevel} dB",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Message text below
                    Text(
                        text = item.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = if (expanded) Int.MAX_VALUE else 1
                    )
                }

                Text(
                    text = timeFormatter.format(Date(item.timestamp)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }

            // --- EXPANDED DETAILS (Visible on Click) ---
            if (expanded && item.decibelLevel > 0) {
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                Spacer(modifier = Modifier.height(16.dp))

                // Visual Progress Bar
                LinearProgressIndicator(
                    progress = { intensityProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = intensityColor,
                    trackColor = intensityColor.copy(alpha = 0.2f),
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Advice / Context
                Row(verticalAlignment = Alignment.Top) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Info",
                        modifier = Modifier
                            .size(16.dp)
                            .padding(top = 2.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = getNoiseAdvice(item.decibelLevel),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Actions Row
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    OutlinedButton(
                        onClick = { /* TODO: Delete Action */ },
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Icon(Icons.Default.Delete, null, Modifier.size(14.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Dismiss", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.Notifications,
                contentDescription = "No Notifications",
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.surfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "No notifications yet",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}