package com.snowi.snuzznoise.presentation.feature.notification

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.snowi.snuzznoise.data.repository.Result
import com.snowi.snuzznoise.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NotificationScreenState(
    val notifications: List<NotificationItem> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)
@HiltViewModel
class NotificationViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {
    private val _state = MutableStateFlow(NotificationScreenState())
    val state: StateFlow<NotificationScreenState> = _state.asStateFlow()
    init {
        observeNotifications()
        markNotificationsAsViewed()
    }
    private fun markNotificationsAsViewed() {
        viewModelScope.launch {
            settingsRepository.markAllNotificationsAsViewed()
        }
    }
    private fun observeNotifications() {
        viewModelScope.launch {
            settingsRepository.getNotifications().collect { result ->
                when (result) {
                    is Result.Success -> {
                        _state.value = NotificationScreenState(notifications = result.data, isLoading = false)
                    }
                    is Result.Error -> {
                        _state.value = NotificationScreenState(error = result.exception.message, isLoading = false)
                    }
                }
            }
        }
    }
}