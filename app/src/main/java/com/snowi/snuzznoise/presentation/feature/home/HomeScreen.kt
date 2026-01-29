package com.snowi.snuzznoise.presentation.feature.home

import android.Manifest
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import com.snowi.snuzznoise.presentation.feature.home.components.HomeHeader
import com.snowi.snuzznoise.presentation.feature.home.components.MonitoringSection
import com.snowi.snuzznoise.presentation.feature.home.components.ProfileSection
import com.snowi.snuzznoise.presentation.navigation.Screen

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun HomeScreen(navController: NavController, viewModel: HomeViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    val currentDecibel by viewModel.currentDecibel.collectAsState()
    val isMonitoring by viewModel.isMonitoring.collectAsState()
    val noiseLabel by viewModel.noiseLabel.collectAsState()
    val monitoringDuration by viewModel.monitoringDuration.collectAsState()
    val unreadNotificationCount by viewModel.unreadNotificationCount.collectAsState()

    val recordAudioPermissionState = rememberPermissionState(Manifest.permission.RECORD_AUDIO)
    val lifecycleOwner = LocalLifecycleOwner.current

    // RESPONSIVE PADDING
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val horizontalPadding = when {
        screenWidth > 600.dp -> 64.dp // Tablet
        screenWidth > 400.dp -> 32.dp // Large Phone
        else -> 20.dp                 // Small/Standard Phone
    }

    LaunchedEffect(lifecycleOwner.lifecycle) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = horizontalPadding), // Apply responsive padding
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            HomeHeader(
                unreadNotificationCount = unreadNotificationCount,
                onNotificationClick = { navController.navigate(Screen.Notifications.route) }
            )

            ProfileSection(
                profiles = state.profiles,
                selectedProfile = state.selectedProfile,
                isLoading = state.isLoading,
                onSetupThreshold = {
                    navController.navigate(Screen.Profile.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                onProfileSelected = { profile -> viewModel.selectProfile(profile) }
            )

            // Pass the noiseLabel to the Monitoring Section
            MonitoringSection(
                isMonitoring = isMonitoring,
                currentDecibel = currentDecibel,
                noiseLabel = noiseLabel,
                monitoringDuration = monitoringDuration,
                selectedProfileName = state.selectedProfile?.name,
                hasAudioPermission = recordAudioPermissionState.status.isGranted,
                shouldShowRationale = recordAudioPermissionState.status.shouldShowRationale,
                modifier = Modifier.weight(1f),
                onIndicatorClick = {
                    if (isMonitoring) {
                        viewModel.stopMonitoring()
                    } else {
                        if (recordAudioPermissionState.status.isGranted) {
                            viewModel.startMonitoring()
                        } else {
                            recordAudioPermissionState.launchPermissionRequest()
                        }
                    }
                }
            )
        }
    }
}