package com.snowi.snuzznoise.presentation.feature.notification

data class NotificationItem(
    val title: String = "",
    val message: String = "",
    val timestamp: Long = 0L,
    val viewed: Boolean = false,
    val noiseType: String = "Unknown",
    val decibelLevel: Int = 0
)