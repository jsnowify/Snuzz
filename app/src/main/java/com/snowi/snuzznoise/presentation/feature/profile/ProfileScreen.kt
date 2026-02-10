package com.snowi.snuzznoise.presentation.feature.profile

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.snowi.snuzznoise.presentation.feature.profile.components.*

@Composable
fun ProfileScreen(viewModel: ProfileViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    val notificationsEnabled by viewModel.notificationsEnabled.collectAsState()
    val currentTheme by viewModel.currentTheme.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()

    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    var showEditDialog by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var selectedProfile by remember { mutableStateOf<ActivityProfile?>(null) }

    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val horizontalPadding = if (screenWidth > 600.dp) 80.dp else 16.dp

    // Show error messages in Snackbar
    LaunchedEffect(state.error) {
        state.error?.let { error ->
            snackbarHostState.showSnackbar(
                message = error,
                duration = SnackbarDuration.Long
            )
            // Clear error after showing
            viewModel.clearError()
        }
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

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            icon = { Icon(Icons.Default.ExitToApp, contentDescription = null) },
            title = { Text("Sign Out?") },
            text = { Text("You will return to Guest mode.") },
            confirmButton = {
                Button(onClick = {
                    viewModel.signOut()
                    showLogoutDialog = false
                }) {
                    Text("Sign Out")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = horizontalPadding, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // =================================================================
            // ✅ UPDATED HEADER: Passing photoUrl instead of 'avatar' painter
            // =================================================================
            ProfileHeader(
                photoUrl = currentUser?.photoUrl,
                name = if (currentUser != null && !currentUser!!.isAnonymous)
                    currentUser?.displayName ?: "User"
                else "Guest User",
                email = if (currentUser != null && !currentUser!!.isAnonymous)
                    currentUser?.email ?: ""
                else "Tap to Sign In with Google",
                onClick = {
                    if (currentUser == null) {
                        Log.w("ProfileScreen", "Auth not ready yet")
                        return@ProfileHeader
                    }

                    if (currentUser!!.isAnonymous) {
                        viewModel.signInWithGoogle(context)
                    } else {
                        showLogoutDialog = true
                    }
                }
            )


            Spacer(modifier = Modifier.height(24.dp))

            // APPEARANCE
            SettingsSection(title = "Appearance") {
                ThemeSelector(
                    currentTheme = currentTheme,
                    onThemeSelected = { viewModel.setTheme(it) }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // GENERAL SETTINGS
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

            // ACTIVITY THRESHOLDS
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