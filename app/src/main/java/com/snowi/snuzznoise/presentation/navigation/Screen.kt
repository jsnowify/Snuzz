package com.snowi.snuzznoise.presentation.navigation

sealed class Screen(val route: String) {
    object Home : Screen("homes_screen")
    object History : Screen("history_screen")
    object Profile : Screen("profile_screen")
    object Notifications : Screen("notification_screen")
}