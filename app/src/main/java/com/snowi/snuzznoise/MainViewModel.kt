package com.snowi.snuzznoise

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.snowi.snuzznoise.data.repository.SettingsRepository
import com.snowi.snuzznoise.presentation.theme.AppTheme
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    repository: SettingsRepository
) : ViewModel() {

    // Tracks if we are still reading the theme from storage
    private val _isLoading = MutableStateFlow(true)
    val isLoading = _isLoading.asStateFlow()

    // Global theme state
    // We map the repository flow to flip the isLoading flag once data arrives
    val currentTheme: StateFlow<AppTheme> = repository.getAppTheme()
        .map { theme ->
            _isLoading.value = false // Stop loading once we get the first value
            theme
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly, // Start listening immediately
            initialValue = AppTheme.SAGE
        )
}