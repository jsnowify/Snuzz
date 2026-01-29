package com.snowi.snuzznoise.presentation.feature.history.components

import android.graphics.Color as AndroidColor
import android.graphics.drawable.GradientDrawable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.snowi.snuzznoise.presentation.feature.history.NoiseEvent
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

@Composable
fun NoiseChart(events: List<NoiseEvent>) {
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val primaryColor = MaterialTheme.colorScheme.primary
    val configuration = LocalConfiguration.current
    val chartHeight = (configuration.screenHeightDp.dp * 0.35f).coerceIn(220.dp, 400.dp)

    Card(
        modifier = Modifier.fillMaxWidth().height(chartHeight),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.5f))
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            factory = { context ->
                LineChart(context).apply {
                    description.isEnabled = false
                    legend.isEnabled = false
                    isDragEnabled = true
                    isScaleXEnabled = true
                    isScaleYEnabled = false
                    setPinchZoom(true)
                    setTouchEnabled(true)

                    xAxis.position = XAxis.XAxisPosition.BOTTOM
                    xAxis.setDrawGridLines(false)
                    xAxis.textColor = onSurfaceColor.toArgb()
                    xAxis.valueFormatter = SmartTimestampFormatter(events)
                    xAxis.granularity = 1f

                    axisLeft.textColor = onSurfaceColor.toArgb()
                    axisLeft.setDrawGridLines(true)
                    axisLeft.gridColor = onSurfaceColor.copy(alpha = 0.1f).toArgb()
                    axisLeft.axisMinimum = 30f // Audio floor

                    axisRight.isEnabled = false
                    setExtraOffsets(0f, 0f, 0f, 10f)
                }
            },
            update = { chart ->
                if (events.isEmpty()) return@AndroidView

                val entries = events.map { Entry(it.timestamp.toFloat(), it.decibelLevel.toFloat()) }

                // Gradient Fill
                val gradientDrawable = GradientDrawable(
                    GradientDrawable.Orientation.TOP_BOTTOM,
                    intArrayOf(primaryColor.copy(alpha = 0.4f).toArgb(), AndroidColor.TRANSPARENT)
                )

                val dataSet = LineDataSet(entries, "Noise").apply {
                    color = primaryColor.toArgb()
                    setDrawValues(false)
                    setDrawCircles(false)
                    setDrawFilled(true)
                    fillDrawable = gradientDrawable
                    lineWidth = 2f
                    mode = LineDataSet.Mode.CUBIC_BEZIER
                    cubicIntensity = 0.2f
                }

                chart.data = LineData(dataSet)

                // Zoom logic
                val timeRange = events.maxOf { it.timestamp } - events.minOf { it.timestamp }
                chart.fitScreen()
                when {
                    timeRange > TimeUnit.HOURS.toMillis(24) -> chart.setVisibleXRangeMaximum(TimeUnit.HOURS.toMillis(12).toFloat())
                    timeRange > TimeUnit.HOURS.toMillis(6) -> chart.setVisibleXRangeMaximum(TimeUnit.HOURS.toMillis(4).toFloat())
                }
                chart.moveViewToX(entries.last().x)
                chart.invalidate()
            }
        )
    }
}

class SmartTimestampFormatter(private val events: List<NoiseEvent>) : ValueFormatter() {
    private val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
    private val dateFormat = SimpleDateFormat("MMM d", Locale.getDefault())

    override fun getFormattedValue(value: Float): String {
        return if (events.isNotEmpty()) {
            val range = events.maxOf { it.timestamp } - events.minOf { it.timestamp }
            if (range > TimeUnit.DAYS.toMillis(1)) dateFormat.format(Date(value.toLong()))
            else timeFormat.format(Date(value.toLong()))
        } else ""
    }
}