package com.snowi.snuzznoise.presentation.feature.profile.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditProfileDialog(
    profile: ActivityProfile?,
    onDismiss: () -> Unit,
    onSave: (name: String, threshold: Int, iconName: String) -> Unit,
    onDelete: () -> Unit
) {
    var name by remember { mutableStateOf(profile?.name ?: "") }
    var threshold by remember { mutableFloatStateOf(profile?.threshold?.toFloat() ?: 75f) }
    var selectedIconName by remember { mutableStateOf(profile?.iconName ?: "Meeting") }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp
    val maxDialogHeight = (screenHeight * 0.9f)

    if (showDeleteConfirmDialog) {
        DeleteConfirmationDialog(
            profileName = name,
            onConfirm = onDelete,
            onDismiss = { showDeleteConfirmDialog = false }
        )
    } else {
        AlertDialog(
            onDismissRequest = onDismiss,
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            title = {
                Text(
                    text = if (profile == null) "Add New Profile" else "Edit Profile",
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = maxDialogHeight)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = {
                            name = it.replaceFirstChar { char ->
                                if (char.isLowerCase()) char.titlecase(Locale.getDefault()) else char.toString()
                            }
                        },
                        label = { Text("Profile Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Column {
                        Text(
                            text = "Select Icon",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        IconPicker(
                            icons = iconMap,
                            selectedIconName = selectedIconName,
                            onIconSelected = { selectedIconName = it }
                        )
                    }

                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Alert Threshold",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "${threshold.toInt()} dB",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Slider(
                            value = threshold,
                            onValueChange = { threshold = it },
                            valueRange = 30f..100f,
                            steps = 0,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Text(
                            text = when {
                                threshold < 40 -> "Very Quiet (Whisper level)"
                                threshold < 50 -> "Quiet (Library level)"
                                threshold < 60 -> "Moderate (Conversation)"
                                threshold < 70 -> "Loud (Busy office)"
                                threshold < 80 -> "Very Loud (Traffic)"
                                else -> "Extreme (Construction)"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { onSave(name, threshold.toInt(), selectedIconName) },
                    enabled = name.isNotBlank(),
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                Row {
                    if (profile != null) {
                        TextButton(
                            onClick = { showDeleteConfirmDialog = true },
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Text("Delete", color = MaterialTheme.colorScheme.error)
                        }
                    }
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                }
            }
        )
    }
}

@Composable
fun IconPicker(
    icons: Map<String, ImageVector>,
    selectedIconName: String,
    onIconSelected: (String) -> Unit
) {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp

    val itemSize = 48.dp
    val spacing = 8.dp
    val availableWidth = (screenWidth - 80.dp)
    val itemWidthPx = itemSize.value + spacing.value
    val columns = (availableWidth.value / itemWidthPx).toInt().coerceAtLeast(4)

    val iconList = icons.entries.toList()
    val rows = iconList.chunked(columns)

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(spacing)
    ) {
        rows.forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start
            ) {
                rowItems.forEach { (name, icon) ->
                    val isSelected = name == selectedIconName
                    Box(
                        modifier = Modifier
                            .padding(end = spacing)
                            .size(itemSize)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onIconSelected(name) }
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            )
                            .border(
                                width = if (isSelected) 1.dp else 0.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                shape = RoundedCornerShape(12.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = name,
                            modifier = Modifier.size(24.dp),
                            tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DeleteConfirmationDialog(
    profileName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        iconContentColor = MaterialTheme.colorScheme.error,
        icon = {
            Icon(
                Icons.Default.DeleteForever,
                contentDescription = "Delete Icon",
                modifier = Modifier.size(36.dp)
            )
        },
        title = {
            Text(
                text = "Delete Profile?",
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Text(
                text = "Are you sure you want to delete the \"$profileName\" profile? This action cannot be undone.",
                textAlign = TextAlign.Center
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Delete")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}