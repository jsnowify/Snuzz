package com.snowi.snuzznoise

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@HiltAndroidApp
class SnuzzNoiseApplication : Application() {

    // --- EXISTING: Decibel Level ---
    private val _decibelFlow = MutableStateFlow(0.0)
    val decibelFlow: StateFlow<Double> = _decibelFlow.asStateFlow()

    // --- NEW: AI Classification Result ---
    private val _noiseLabelFlow = MutableStateFlow("Listening...")
    val noiseLabelFlow: StateFlow<String> = _noiseLabelFlow.asStateFlow()

    override fun onCreate() {
        super.onCreate()
    }

    // Helper to update Decibels (Used by Service)
    fun updateDecibel(decibel: Double) {
        _decibelFlow.value = decibel
    }

    // Helper to update AI Label (Used by Service)
    fun updateNoiseLabel(label: String) {
        _noiseLabelFlow.value = label
    }
}