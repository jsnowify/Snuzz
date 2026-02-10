package com.snowi.snuzznoise

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
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
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
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
import kotlinx.coroutines.tasks.await
import java.security.MessageDigest
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var auth: FirebaseAuth

    private val mainViewModel: MainViewModel by viewModels()

    @OptIn(ExperimentalPermissionsApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        SoundPlayer.init(this)

        // =========================================================================
        // 🕵️ SPY CODE v2: FIND BOTH KEYS (SHA-1 AND SHA-256)
        // 1. Run App on Phone.
        // 2. Filter Logcat for "MY_REAL_KEYS".
        // 3. Add BOTH keys to Firebase Console.
        // =========================================================================
        try {
            val info = packageManager.getPackageInfo(
                packageName,
                PackageManager.GET_SIGNATURES
            )
            for (signature in info.signatures!!) {
                // Get SHA-1
                val md = MessageDigest.getInstance("SHA-1")
                md.update(signature.toByteArray())
                val digest = md.digest()
                val hexString = StringBuilder()
                for (b in digest) { hexString.append(String.format("%02X:", b)) }
                Log.e("17:D0:52:63:63:8F:3C:D9:2C:0D:43:4E:A3:84:71:BB:6F:2D:0D:FA", "SHA-1:   ${hexString.toString().dropLast(1)}")

                // Get SHA-256 (REQUIRED for new Sign In)
                val md256 = MessageDigest.getInstance("SHA-256")
                md256.update(signature.toByteArray())
                val digest256 = md256.digest()
                val hexString256 = StringBuilder()
                for (b in digest256) { hexString256.append(String.format("%02X:", b)) }
                Log.e("F5:0C:6E:72:BF:13:DD:03:1A:AB:10:58:F7:A1:3E:B0:CB:7A:F5:DB:FA:07:8D:40:47:B6:F9:80:5D:47:78:DC", "SHA-256: ${hexString256.toString().dropLast(1)}")
            }
        } catch (e: Exception) {
            Log.e("MY_REAL_KEYS", "Error getting keys", e)
        }
        // =========================================================================

        splashScreen.setKeepOnScreenCondition {
            mainViewModel.isLoading.value
        }

        setContent {
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
                    ensureUserSignedIn()
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

    // ✅ FIX: Make this a suspend function and use await() for proper async handling
    private suspend fun ensureUserSignedIn() {
        if (auth.currentUser == null) {
            Log.d("MainActivity", "No user found, signing in anonymously...")
            try {
                auth.signInAnonymously().await()
                Log.d("MainActivity", "✅ Anonymous sign-in success - UID: ${auth.currentUser?.uid}")
            } catch (e: Exception) {
                Log.e("MainActivity", "❌ Anonymous sign-in failure", e)
            }
        } else {
            Log.d("MainActivity", "User already exists. UID: ${auth.currentUser?.uid}, isAnonymous: ${auth.currentUser?.isAnonymous}")
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