package com.snowi.snuzznoise.presentation.feature.history

import com.google.firebase.firestore.PropertyName

data class NoiseEvent(
    val timestamp: Long = 0L,
    val decibelLevel: Double = 0.0,
    @get:PropertyName("isAlert")
    @set:PropertyName("isAlert")
    var isAlert: Boolean = false,
    val noiseType: String = "Unknown"
)