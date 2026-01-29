package com.snowi.snuzznoise.presentation.feature.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.snowi.snuzznoise.data.repository.Result
import com.snowi.snuzznoise.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.collections.filter

// --- HELPER CLASSES ---

enum class TimeRange(val label: String, val shortLabel: String, val milliseconds: Long) {
    LAST_24_HOURS("Last 24 Hours", "24H", TimeUnit.HOURS.toMillis(24)),
    LAST_7_DAYS("Last 7 Days", "7D", TimeUnit.DAYS.toMillis(7)),
    LAST_30_DAYS("Last 30 Days", "30D", TimeUnit.DAYS.toMillis(30))
}

data class NoiseStats(
    val alertEvents: Int,
    val averageLevel: Double,
    val peakLevel: Double,
    val timeRangeLabel: String
)

data class HistoryScreenState(
    val chartEvents: List<NoiseEvent> = emptyList(),
    val alertEvents: List<NoiseEvent> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val selectedTimeRange: TimeRange = TimeRange.LAST_24_HOURS,
    val stats: NoiseStats? = null
)

// --- VIEWMODEL ---

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _state = MutableStateFlow(HistoryScreenState())
    val state: StateFlow<HistoryScreenState> = _state.asStateFlow()

    private var historyJob: Job? = null

    init {
        loadHistoryForSelectedRange()
    }

    fun selectTimeRange(timeRange: TimeRange) {
        if (_state.value.selectedTimeRange != timeRange) {
            _state.value = _state.value.copy(selectedTimeRange = timeRange)
            loadHistoryForSelectedRange()
        }
    }

    private fun loadHistoryForSelectedRange() {
        historyJob?.cancel()
        _state.value = _state.value.copy(isLoading = true)

        historyJob = viewModelScope.launch {
            val timeRange = _state.value.selectedTimeRange
            settingsRepository.getNoiseHistory(timeRange.milliseconds)
                .collectLatest { result ->
                    when (result) {
                        is Result.Success -> {
                            val rawEvents = result.data
                            val stats = calculateStats(rawEvents, timeRange)
                            val chartEvents = downsampleEvents(rawEvents, timeRange)

                            val allAlertEvents = rawEvents
                                .filter { it.isAlert }
                                .sortedByDescending { it.timestamp }

                            _state.value = _state.value.copy(
                                isLoading = false,
                                chartEvents = chartEvents,
                                alertEvents = allAlertEvents,
                                stats = stats,
                                error = null
                            )
                        }
                        is Result.Error -> {
                            _state.value = _state.value.copy(
                                error = result.exception.message,
                                isLoading = false,
                                chartEvents = emptyList(),
                                alertEvents = emptyList(),
                                stats = null
                            )
                        }
                    }
                }
        }
    }

    private fun downsampleEvents(events: List<NoiseEvent>, timeRange: TimeRange): List<NoiseEvent> {
        if (timeRange == TimeRange.LAST_24_HOURS || events.size < 200) {
            return events
        }
        val bucketSizeInMillis = when (timeRange) {
            TimeRange.LAST_7_DAYS -> TimeUnit.HOURS.toMillis(1)
            TimeRange.LAST_30_DAYS -> TimeUnit.HOURS.toMillis(3)
            else -> return events
        }
        // "it" works here because NoiseEvent is now properly defined in Step 1
        return events
            .groupBy { it.timestamp / bucketSizeInMillis }
            .map { (_, bucketEvents) ->
                NoiseEvent(
                    timestamp = bucketEvents.first().timestamp,
                    decibelLevel = bucketEvents.map { it.decibelLevel }.average(),
                    isAlert = bucketEvents.any { it.isAlert },
                    noiseType = bucketEvents.first().noiseType
                )
            }
    }

    private fun calculateStats(events: List<NoiseEvent>, timeRange: TimeRange): NoiseStats? {
        if (events.isEmpty()) return null
        return NoiseStats(
            alertEvents = events.count { it.isAlert },
            averageLevel = events.sumOf { it.decibelLevel } / events.size,
            peakLevel = events.maxOfOrNull { it.decibelLevel } ?: 0.0,
            timeRangeLabel = timeRange.label
        )
    }
}