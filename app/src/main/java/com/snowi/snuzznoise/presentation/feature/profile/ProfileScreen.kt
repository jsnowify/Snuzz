package com.snowi.snuzznoise.presentation.feature.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.snowi.snuzznoise.R
import com.snowi.snuzznoise.presentation.feature.profile.components.ActivityProfile
import com.snowi.snuzznoise.presentation.feature.profile.components.ActivityThresholdsSection
import com.snowi.snuzznoise.presentation.feature.profile.components.AddEditProfileDialog
import com.snowi.snuzznoise.presentation.feature.profile.components.NotificationSettingsItem
import com.snowi.snuzznoise.presentation.feature.profile.components.ProfileHeader
import com.snowi.snuzznoise.presentation.feature.profile.components.SettingsItem
import com.snowi.snuzznoise.presentation.feature.profile.components.SettingsSection
import com.snowi.snuzznoise.presentation.feature.profile.components.ThemeSelector // Ensure this is imported

@Composable
fun ProfileScreen(viewModel: ProfileViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    val notificationsEnabled by viewModel.notificationsEnabled.collectAsState()

    // 1. Collect the current theme from ViewModel
    val currentTheme by viewModel.currentTheme.collectAsState()

    var showEditDialog by remember { mutableStateOf(false) }
    var selectedProfile by remember { mutableStateOf<ActivityProfile?>(null) }

    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp

    val horizontalPadding = when {
        screenWidth < 360.dp -> 16.dp
        screenWidth < 600.dp -> 24.dp
        else -> 0.dp
    }

    if (showEditDialog) {
        AddEditProfileDialog(
            profile = selectedProfile,
            onDismiss = { showEditDialog = false },
            onSave = { name, threshold, iconName ->
                viewModel.saveProfile(name, threshold, iconName, selectedProfile?.id)
                showEditDialog = false
            },
            onDelete = {
                selectedProfile?.id?.let { viewModel.deleteProfile(it) }
                showEditDialog = false
            }
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 600.dp)
                    .padding(horizontal = horizontalPadding)
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                ProfileHeader(
                    avatar = painterResource(id = R.drawable.avatar),
                    email = "snuzzversion@gmail.com"
                )

                Spacer(modifier = Modifier.height(24.dp))

                // --- NEW THEME SELECTOR SECTION ---
                SettingsSection(title = "Appearance") {
                    ThemeSelector(
                        currentTheme = currentTheme,
                        onThemeSelected = { newTheme ->
                            viewModel.setTheme(newTheme)
                        }
                    )
                }
                // ----------------------------------

                Spacer(modifier = Modifier.height(24.dp))

                SettingsSection(title = "General") {
                    NotificationSettingsItem(
                        checked = notificationsEnabled,
                        onCheckedChange = { viewModel.setNotificationsEnabled(it) }
                    )
                    SettingsItem(
                        icon = Icons.Default.Mic,
                        title = "Calibrate Microphone",
                        subtitle = "Adjust for your device's sensitivity",
                        onClick = { /* TODO */ }
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                ActivityThresholdsSection(
                    profiles = state.profiles,
                    isLoading = state.isLoadingProfiles,
                    onAddClick = {
                        selectedProfile = null
                        showEditDialog = true
                    },
                    onProfileClick = { profile ->
                        selectedProfile = profile
                        showEditDialog = true
                    }
                )
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}