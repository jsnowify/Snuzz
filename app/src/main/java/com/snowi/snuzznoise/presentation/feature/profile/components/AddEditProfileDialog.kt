package com.snowi.snuzznoise.presentation.feature.profile.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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

    // Logic: Dialog should not exceed 90% height or 500dp width
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
            modifier = Modifier.widthIn(max = 500.dp), // Keeps it nice on tablets
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
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
                    enabled = name.isNotBlank()
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
    // LazyVerticalGrid handles columns automatically based on size
    // Adaptive(48.dp) means: fit as many 48dp items as possible
    val iconList = icons.entries.toList()

    Box(modifier = Modifier.height(200.dp)) { // Fixed height scrollable area
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 48.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(iconList) { (name, icon) ->
                val isSelected = name == selectedIconName
                Box(
                    modifier = Modifier
                        .size(48.dp)
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

@Composable
fun DeleteConfirmationDialog(
    profileName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Default.DeleteForever,
                contentDescription = "Delete",
                modifier = Modifier.size(32.dp)
            )
        },
        title = { Text("Delete Profile?") },
        text = { Text("Permanently delete \"$profileName\"?") },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) { Text("Delete") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}