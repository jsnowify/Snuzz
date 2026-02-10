package com.snowi.snuzznoise.utils

import android.content.Context
import android.util.Log
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import java.io.BufferedReader
import java.io.InputStreamReader

class AudioClassifier(context: Context) {
    private var interpreter: Interpreter? = null
    private var labels: MutableList<String> = mutableListOf()
    private val MODEL_FILE = "yamnet.tflite"
    private val LABEL_FILE = "yamnet_class_map.csv"
    private val EXPECTED_INPUT_LENGTH = 15600

    init {
        try {
            val mappedByteBuffer = FileUtil.loadMappedFile(context, MODEL_FILE)
            interpreter = Interpreter(mappedByteBuffer)
            loadLabels(context)
            Log.d("AudioClassifier", "YAMNet loaded! Known classes: ${labels.size}")
        } catch (e: Exception) {
            Log.e("AudioClassifier", "Error loading YAMNet: ${e.message}")
        }
    }

    private fun loadLabels(context: Context) {
        try {
            val inputStream = context.assets.open(LABEL_FILE)
            val reader = BufferedReader(InputStreamReader(inputStream))
            reader.readLine()

            var line = reader.readLine()
            while (line != null) {
                val tokens = line.split(",")
                if (tokens.size >= 3) {
                    labels.add(tokens[2])
                } else {
                    labels.add("Unknown")
                }
                line = reader.readLine()
            }
        } catch (e: Exception) {
            Log.e("AudioClassifier", "Error loading labels", e)
        }
    }

    fun classify(rawAudioBuffer: FloatArray): String {
        if (interpreter == null) return "Unknown"
        try {

            val inputBuffer = FloatArray(EXPECTED_INPUT_LENGTH)
            val lengthToCopy = minOf(rawAudioBuffer.size, EXPECTED_INPUT_LENGTH)
            System.arraycopy(rawAudioBuffer, 0, inputBuffer, 0, lengthToCopy)

            val outputBuffer = Array(1) { FloatArray(521) }
            interpreter?.run(inputBuffer, outputBuffer)

            val predictions = outputBuffer[0]
            var maxIndex = -1
            var maxScore = 0f

            for (i in predictions.indices) {
                if (predictions[i] > maxScore) {
                    maxScore = predictions[i]
                    maxIndex = i
                }
            }

            if (maxIndex == -1) return "Unknown"

            // Only trust if confidence > 20%
            if (maxScore < 0.2f) return "Unknown"

            // Get the raw AI name (e.g., "Heart sounds")
            val rawLabel = labels.getOrElse(maxIndex) { "Unknown" }

            Log.d("AudioClassifier", "Raw AI Output: $rawLabel ($maxScore)")

            // 4. Normalize to clean UI strings
            return normalizeLabel(rawLabel)

        } catch (e: Exception) {
            Log.e("AudioClassifier", "Error: ${e.message}")
            return "Unknown"
        }
    }

    // --- KEY FIX: Maps complex AI names to clean UI strings ---
    private fun normalizeLabel(label: String): String {
        val s = label.lowercase()
        return when {
            // 🚫 IGNORE: Environmental/Vague labels (Don't show these on the card)
            s == "inside" || s.contains("silence") || s.contains("room") ||
                    s.contains("environment") || s.contains("background") -> "Unknown"

            // 🟢 GREEN ZONE (Safe / White Noise)
            s.contains("heart") -> "Heartbeat" // Added Heartbeat
            s.contains("type") || s.contains("typing") -> "Typing"
            s.contains("rain") -> "Rain"
            s.contains("fan") -> "Fan"
            s.contains("breath") -> "Breathing"
            s.contains("air condition") -> "Air Conditioner"
            s.contains("white noise") || s.contains("static") -> "White Noise"

            // 🔴 RED ZONE (Critical / Danger)
            s.contains("cry") -> "Baby Crying"
            s.contains("scream") || s.contains("shout") || s.contains("yell") -> "Screaming"
            s.contains("glass") -> "Glass Breaking"
            s.contains("gun") -> "Gunshot"
            s.contains("siren") || s.contains("alarm") -> "Siren"

            // 🟡 YELLOW ZONE (Normal / Human)
            s.contains("laugh") -> "Laughter"
            s.contains("talk") || s.contains("speech") || s.contains("conversation") -> "Talking"
            s.contains("sing") -> "Singing"
            s.contains("clap") -> "Clapping"
            s.contains("music") -> "Music"
            s.contains("footstep") || s.contains("walk") -> "Footsteps"

            // Fallback: If it's something specific we didn't list, show it.
            // If it was "Inside", it is now "Unknown".
            else -> label
        }
    }
}