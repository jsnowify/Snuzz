package com.snowi.snuzznoise

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels // Import this
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen // Import this
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.firebase.auth.FirebaseAuth
import com.snowi.snuzznoise.presentation.feature.history.HistoryScreen
import com.snowi.snuzznoise.presentation.feature.home.HomeScreen
import com.snowi.snuzznoise.presentation.feature.notification.NotificationScreen
import com.snowi.snuzznoise.presentation.feature.profile.ProfileScreen
import com.snowi.snuzznoise.presentation.navigation.Screen
import com.snowi.snuzznoise.presentation.theme.SnuzzNoiseTheme
import com.snowi.snuzznoise.utils.SoundPlayer
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var auth: FirebaseAuth

    // Initialize ViewModel here to access it before setContent
    private val mainViewModel: MainViewModel by viewModels()

    @OptIn(ExperimentalPermissionsApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        // 1. Install Splash Screen (Must be before super.onCreate)
        val splashScreen = installSplashScreen()

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        SoundPlayer.init(this)

        // 2. Keep the splash screen visible until the theme is fully loaded
        splashScreen.setKeepOnScreenCondition {
            mainViewModel.isLoading.value
        }

        setContent {
            // 3. Observe the current theme (it will be ready when splash screen goes away)
            val appTheme by mainViewModel.currentTheme.collectAsState()

            SnuzzNoiseTheme(appTheme = appTheme) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val notificationPermission = rememberPermissionState(
                        permission = Manifest.permission.POST_NOTIFICATIONS
                    )
                    SideEffect {
                        if (!notificationPermission.status.isGranted) {
                            notificationPermission.launchPermissionRequest()
                        }
                    }
                }

                LaunchedEffect(Unit) {
                    signInAnonymously()
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainAppContent()
                }
            }
        }
    }

    private fun signInAnonymously() {
        if (auth.currentUser == null) {
            auth.signInAnonymously()
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        Log.d("MainActivity", "signInAnonymously:success - UID: ${auth.currentUser?.uid}")
                    } else {
                        Log.w("MainActivity", "signInAnonymously:failure", task.exception)
                    }
                }
        } else {
            Log.d("MainActivity", "User already signed in. UID: ${auth.currentUser?.uid}")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        SoundPlayer.release()
    }
}

data class BottomNavItem(
    val name: String,
    val route: String,
    val icon: ImageVector,
    val badgeCount: Int = 0
)

@Composable
fun MainAppContent() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val shouldShowBottomBar = currentDestination?.hierarchy?.any {
        it.route == Screen.Home.route ||
                it.route == Screen.History.route ||
                it.route == Screen.Profile.route
    } == true

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (shouldShowBottomBar) {
                AppBottomNavigationBar(
                    navController = navController,
                    currentDestination = currentDestination
                )
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(paddingValues),
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None }
        ) {
            composable(Screen.Home.route) { HomeScreen(navController) }
            composable(Screen.History.route) { HistoryScreen() }
            composable(Screen.Profile.route) { ProfileScreen() }
            composable(Screen.Notifications.route) { NotificationScreen(navController) }
        }
    }
}

@Composable
fun AppBottomNavigationBar(
    navController: NavController,
    currentDestination: NavDestination?
) {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp

    val navBarHorizontalPadding = when {
        screenWidth > 600.dp -> 120.dp
        screenWidth > 380.dp -> 48.dp
        screenWidth > 340.dp -> 24.dp
        else -> 12.dp
    }

    val showLabels = screenWidth > 340.dp

    NavigationBar(
        modifier = Modifier
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(bottom = 16.dp)
            .padding(horizontal = navBarHorizontalPadding)
            .shadow(elevation = 8.dp, shape = CircleShape)
            .clip(CircleShape),
        windowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 0.dp
    ) {
        val bottomNavItemsContent = listOf(
            BottomNavItem("Home", Screen.Home.route, Icons.Filled.Home),
            BottomNavItem("History", Screen.History.route, Icons.AutoMirrored.Filled.ListAlt),
            BottomNavItem("Profile", Screen.Profile.route, Icons.Filled.Person)
        )

        bottomNavItemsContent.forEach { item ->
            val selected = currentDestination?.hierarchy?.any { it.route == item.route } == true

            NavigationBarItem(
                selected = selected,
                onClick = {
                    navController.navigate(item.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = {
                    BadgedBox(
                        badge = {
                            if (item.badgeCount > 0) {
                                Badge { Text(item.badgeCount.toString()) }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.name,
                        )
                    }
                },
                label = {
                    if (showLabels) {
                        Text(text = item.name)
                    }
                },
                alwaysShowLabel = showLabels,
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    unselectedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                )
            )
        }
    }
}