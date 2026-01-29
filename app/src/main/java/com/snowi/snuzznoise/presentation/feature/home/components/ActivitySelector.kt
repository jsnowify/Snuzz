package com.snowi.snuzznoise.presentation.feature.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.snowi.snuzznoise.presentation.feature.profile.components.ActivityProfile

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivitySelector(
    profiles: List<ActivityProfile>,
    selectedProfile: ActivityProfile?,
    onProfileSelected: (ActivityProfile) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val currentProfile = selectedProfile ?: profiles.firstOrNull() ?: return

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = Modifier.padding(top = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .menuAnchor()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = currentProfile.getIcon(),
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = currentProfile.name,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
        }

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(12.dp)
                )
        ) {
            profiles.forEachIndexed { index, profile ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = profile.name,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    },
                    onClick = {
                        onProfileSelected(profile)
                        expanded = false
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = profile.getIcon(),
                            contentDescription = profile.name,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    colors = MenuDefaults.itemColors(
                        textColor = MaterialTheme.colorScheme.onSurface,
                        leadingIconColor = MaterialTheme.colorScheme.primary
                    )
                )
                if (index < profiles.size - 1) {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                    )
                }
            }
        }
    }
}