package com.snowi.snuzznoise.presentation.feature.profile

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.snowi.snuzznoise.data.repository.Result
import com.snowi.snuzznoise.data.repository.SettingsRepository
import com.snowi.snuzznoise.presentation.auth.GoogleAuthClient
import com.snowi.snuzznoise.presentation.feature.profile.components.ActivityProfile
import com.snowi.snuzznoise.presentation.theme.AppTheme
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class ProfileScreenState(
    val profiles: List<ActivityProfile> = emptyList(),
    val isLoadingProfiles: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val googleAuthClient: GoogleAuthClient,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _state = MutableStateFlow(ProfileScreenState())
    val state = _state.asStateFlow()

    val currentUser: StateFlow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener {
            trySend(it.currentUser)
        }
        auth.addAuthStateListener(listener)
        trySend(auth.currentUser)
        awaitClose { auth.removeAuthStateListener(listener) }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        auth.currentUser
    )

    val notificationsEnabled = settingsRepository.notificationsEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val currentTheme = settingsRepository.getAppTheme()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppTheme.SAGE)

    init {
        viewModelScope.launch {
            currentUser.collect { user ->
                if (user == null) {
                    _state.value = _state.value.copy(
                        profiles = emptyList(),
                        isLoadingProfiles = false
                    )
                    return@collect
                }

                observeProfiles()
            }
        }
    }

    fun signInWithGoogle(context: Context) {
        viewModelScope.launch {
            val result = googleAuthClient.signIn(context)

            result.onFailure {
                _state.value = _state.value.copy(
                    error = it.message ?: "Sign-in failed"
                )
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            googleAuthClient.signOut()

            _state.value = _state.value.copy(
                profiles = emptyList(),
                isLoadingProfiles = false
            )
        }
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }

    fun setTheme(theme: AppTheme) {
        viewModelScope.launch {
            settingsRepository.setAppTheme(theme)
        }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setNotificationsEnabled(enabled)
        }
    }

    private fun observeProfiles() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoadingProfiles = true)

            settingsRepository.getActivityProfiles().collect { result ->
                when (result) {
                    is Result.Success -> {
                        _state.value = _state.value.copy(
                            profiles = result.data,
                            isLoadingProfiles = false
                        )
                    }
                    is Result.Error -> {
                        _state.value = _state.value.copy(
                            error = result.exception.message,
                            isLoadingProfiles = false
                        )
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