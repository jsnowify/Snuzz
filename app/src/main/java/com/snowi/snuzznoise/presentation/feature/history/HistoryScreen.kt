package com.snowi.snuzznoise.presentation.feature.history

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.snowi.snuzznoise.presentation.feature.history.components.*

@Composable
fun HistoryScreen(viewModel: HistoryViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()

    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val horizontalPadding = if (screenWidth > 600.dp) 80.dp else 16.dp

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(horizontal = horizontalPadding, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. HEADER & FILTER
            item {
                HistoryHeader(
                    selectedTimeRange = state.selectedTimeRange,
                    onTimeRangeSelected = { viewModel.selectTimeRange(it) }
                )
                Spacer(modifier = Modifier.height(24.dp))
            }

            // 2. CHART AREA
            item {
                when {
                    state.isLoading -> {
                        Box(modifier = Modifier.fillMaxWidth().height(300.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                    state.chartEvents.isEmpty() -> {
                        EmptyHistoryState()
                    }
                    else -> {
                        Column {
                            NoiseChart(events = state.chartEvents)
                            Spacer(modifier = Modifier.height(16.dp))
                            state.stats?.let { ChartLegend(stats = it) }
                            Spacer(modifier = Modifier.height(32.dp))
                        }
                    }
                }
            }

            // 3. EVENTS LIST
            if (!state.isLoading && state.alertEvents.isNotEmpty()) {
                item {
                    SignificantEventsHeader(alertCount = state.alertEvents.size)
                    Spacer(modifier = Modifier.height(12.dp))
                }

                items(state.alertEvents) { event ->
                    AlertEventItem(event = event)
                    Spacer(modifier = Modifier.height(12.dp))
                }
            } else if (!state.isLoading && state.chartEvents.isNotEmpty()) {
                item {
                    NoSignificantEventsCard()
                }
            }
        }
    }
}