package com.snowi.snuzznoise.presentation.feature.home

import android.app.Application
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.snowi.snuzznoise.data.repository.NoiseRepository
import com.snowi.snuzznoise.data.repository.Result
import com.snowi.snuzznoise.data.repository.SettingsRepository
import com.snowi.snuzznoise.presentation.feature.profile.components.ActivityProfile
import com.snowi.snuzznoise.service.NoiseDetectionService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeScreenState(
    val profiles: List<ActivityProfile> = emptyList(),
    val selectedProfile: ActivityProfile? = null,
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val noiseRepository: NoiseRepository,
    private val settingsRepository: SettingsRepository,
    private val application: Application
) : ViewModel() {

    private val _isMonitoring = MutableStateFlow(false)
    val isMonitoring: StateFlow<Boolean> = _isMonitoring.asStateFlow()

    private val _currentDecibel = MutableStateFlow(0.0)
    val currentDecibel: StateFlow<Double> = _currentDecibel.asStateFlow()

    // --- NEW: AI Noise Label ---
    private val _noiseLabel = MutableStateFlow("Listening...")
    val noiseLabel: StateFlow<String> = _noiseLabel.asStateFlow()
    // ---------------------------

    private val _monitoringDuration = MutableStateFlow(0L)
    val monitoringDuration: StateFlow<Long> = _monitoringDuration.asStateFlow()

    private val _state = MutableStateFlow(HomeScreenState())
    val state: StateFlow<HomeScreenState> = _state.asStateFlow()

    private val _unreadNotificationCount = MutableStateFlow(0)
    val unreadNotificationCount: StateFlow<Int> = _unreadNotificationCount.asStateFlow()

    private var timerJob: Job? = null
    private var decibelCollectorJob: Job? = null
    private var noiseLabelCollectorJob: Job? = null

    init {
        observeDecibelLevel()
        observeNoiseLabel() // Start observing AI labels
        observeProfiles()
        observeUnreadNotificationCount()
    }

    private fun observeUnreadNotificationCount() {
        viewModelScope.launch {
            settingsRepository.getUnreadNotificationCount().collect { result ->
                if (result is Result.Success) {
                    _unreadNotificationCount.value = result.data
                }
            }
        }
    }

    private fun observeProfiles() {
        viewModelScope.launch {
            settingsRepository.getActivityProfiles().collect { result ->
                when (result) {
                    is Result.Success -> {
                        val profiles = result.data
                        val currentSelectedId = _state.value.selectedProfile?.id
                        val newSelectedProfile = profiles.find { it.id == currentSelectedId } ?: profiles.firstOrNull()

                        _state.value = HomeScreenState(
                            profiles = profiles,
                            selectedProfile = newSelectedProfile,
                            isLoading = false
                        )
                    }
                    is Result.Error -> {
                        _state.value = HomeScreenState(isLoading = false, error = result.exception.message)
                    }
                }
            }
        }
    }

    fun selectProfile(profile: ActivityProfile) {
        _state.value = _state.value.copy(selectedProfile = profile)
    }

    private fun observeDecibelLevel() {
        decibelCollectorJob?.cancel()
        decibelCollectorJob = viewModelScope.launch {
            noiseRepository.decibelLevel.collectLatest { db ->
                if (_isMonitoring.value) {
                    _currentDecibel.value = db
                }
            }
        }
    }

    // --- NEW: Observer for AI Label ---
    private fun observeNoiseLabel() {
        noiseLabelCollectorJob?.cancel()
        noiseLabelCollectorJob = viewModelScope.launch {
            noiseRepository.noiseLabel.collectLatest { label ->
                if (_isMonitoring.value) {
                    _noiseLabel.value = label
                }
            }
        }
    }

    fun startMonitoring() {
        if (_isMonitoring.value) return

        val threshold = state.value.selectedProfile?.threshold ?: 75

        _isMonitoring.value = true
        _noiseLabel.value = "Listening..." // Reset label on start

        val intent = Intent(application, NoiseDetectionService::class.java).apply {
            putExtra("ALERT_THRESHOLD", threshold)
        }
        application.startService(intent)

        _monitoringDuration.value = 0L
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                _monitoringDuration.value++
            }
        }
    }

    fun stopMonitoring() {
        if (!_isMonitoring.value) return
        _isMonitoring.value = false

        val intent = Intent(application, NoiseDetectionService::class.java)
        application.stopService(intent)

        timerJob?.cancel()
        _currentDecibel.value = 0.0
        _noiseLabel.value = "Paused" // Reset label on stop
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
        decibelCollectorJob?.cancel()
        noiseLabelCollectorJob?.cancel()
    }
}