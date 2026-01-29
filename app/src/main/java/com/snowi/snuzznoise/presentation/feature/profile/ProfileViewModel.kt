package com.snowi.snuzznoise.presentation.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.snowi.snuzznoise.data.repository.Result
import com.snowi.snuzznoise.data.repository.SettingsRepository
import com.snowi.snuzznoise.presentation.feature.profile.components.ActivityProfile
import com.snowi.snuzznoise.presentation.theme.AppTheme // Import required for Theme logic
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class ProfileScreenState(
    val profiles: List<ActivityProfile> = emptyList(),
    val isLoadingProfiles: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ProfileScreenState())
    val state: StateFlow<ProfileScreenState> = _state.asStateFlow()

    val notificationsEnabled: StateFlow<Boolean> = settingsRepository.notificationsEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    // --- ADDED: Theme State ---
    val currentTheme: StateFlow<AppTheme> = settingsRepository.getAppTheme()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppTheme.SAGE)

    // --- ADDED: Theme Action ---
    fun setTheme(theme: AppTheme) {
        viewModelScope.launch {
            settingsRepository.setAppTheme(theme)
        }
    }

    init {
        observeProfiles()
    }

    fun setNotificationsEnabled(isEnabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setNotificationsEnabled(isEnabled)
        }
    }

    private fun observeProfiles() {
        viewModelScope.launch {
            settingsRepository.getActivityProfiles().collect { result ->
                when (result) {
                    is Result.Success -> {
                        _state.value = _state.value.copy(profiles = result.data, isLoadingProfiles = false)
                    }
                    is Result.Error -> {
                        _state.value = _state.value.copy(error = result.exception.message, isLoadingProfiles = false)
                    }
                }
            }
        }
    }

    fun saveProfile(name: String, threshold: Int, iconName: String, id: String? = null) {
        viewModelScope.launch {
            val profile = ActivityProfile(
                id = id ?: UUID.randomUUID().toString(),
                name = name,
                threshold = threshold,
                iconName = iconName
            )
            settingsRepository.saveActivityProfile(profile)
        }
    }

    fun deleteProfile(profileId: String) {
        viewModelScope.launch {
            settingsRepository.deleteActivityProfile(profileId)
        }
    }
}