package com.snowi.snuzznoise.data.repository

import android.app.Application
import com.snowi.snuzznoise.SnuzzNoiseApplication
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class NoiseRepository @Inject constructor(
    private val application: Application
) {
    val decibelLevel: StateFlow<Double>
        get() = (application as SnuzzNoiseApplication).decibelFlow

    val noiseLabel: StateFlow<String>
        get() = (application as SnuzzNoiseApplication).noiseLabelFlow
}
