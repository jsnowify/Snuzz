    package com.snowi.snuzznoise.presentation.feature.home.components

    import androidx.compose.foundation.layout.*
    import androidx.compose.material.icons.Icons
    import androidx.compose.material.icons.filled.Add
    import androidx.compose.material3.*
    import androidx.compose.runtime.Composable
    import androidx.compose.ui.Modifier
    import androidx.compose.ui.unit.dp
    import com.snowi.snuzznoise.presentation.feature.profile.components.ActivityProfile

    @Composable
    fun ProfileSection(
        profiles: List<ActivityProfile>,
        selectedProfile: ActivityProfile?,
        isLoading: Boolean,
        onSetupThreshold: () -> Unit,
        onProfileSelected: (ActivityProfile) -> Unit
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        when {
            isLoading -> {
                Box(modifier = Modifier.height(48.dp))
            }
            profiles.isEmpty() -> {
                TextButton(onClick = onSetupThreshold) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Set an Alert Threshold",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
            else -> {
                ActivitySelector(
                    profiles = profiles,
                    selectedProfile = selectedProfile,
                    onProfileSelected = onProfileSelected
                )
            }
        }
    }