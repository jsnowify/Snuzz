package com.snowi.snuzznoise.service

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.snowi.snuzznoise.R
import com.snowi.snuzznoise.SnuzzNoiseApplication
import com.snowi.snuzznoise.data.repository.SettingsRepository
import com.snowi.snuzznoise.presentation.feature.history.NoiseEvent
import com.snowi.snuzznoise.presentation.feature.notification.NotificationItem
import com.snowi.snuzznoise.utils.AudioClassifier
import com.snowi.snuzznoise.utils.SoundPlayer
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import kotlin.math.log10
import kotlin.math.sqrt

@AndroidEntryPoint
class NoiseDetectionService : Service() {
    @Inject
    lateinit var settingsRepository: SettingsRepository

    private lateinit var audioClassifier: AudioClassifier
    private var currentNoiseLabel = "NORMAL"
    private var activityNameForNotifs = "Default Mode"

    private val aiInputBufferSize = 15600
    private val aiInputBuffer = FloatArray(aiInputBufferSize)
    private var aiBufferIndex = 0

    private var audioRecord: AudioRecord? = null
    private var isRecording = false

    // --- NEW FLAG: Prevents the app from hearing its own alarm ---
    private var isAlertPlaying = false

    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)
    private val repositoryScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var applicationFlow: SnuzzNoiseApplication
    private var previousDecibel = 0.0
    private val smoothingFactor = 0.4f
    private var lastNotificationUpdate = 0L
    private var lastLogTime = 0L
    private var lastAlertTime = 0L
    private var alertThreshold: Int = 70
    private var highNoiseStartTime: Long = 0L
    private var consecutiveHighReadings = 0

    companion object {
        private const val TAG = "NoiseDetectionService"
        private const val NOTIFICATION_ID = 1
        private const val ALERT_NOTIFICATION_ID = 2
        private const val NOTIFICATION_CHANNEL_ID = "noise_detection_channel"
        private const val NOTIFICATION_CHANNEL_NAME = "Noise Detection"
        private const val HIGH_PRIORITY_CHANNEL_ID = "high_noise_alert_channel"
        private const val HIGH_PRIORITY_CHANNEL_NAME = "High Noise Alerts"
        private const val SAMPLE_RATE = 16000
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        private const val UPDATE_INTERVAL_MS = 20L
        private const val NOTIFICATION_UPDATE_INTERVAL_MS = 1000L
        private const val NOISE_DURATION_THRESHOLD_MS = 5000L
        private const val LOG_INTERVAL_MS = 30000L
        private const val ALERT_COOLDOWN_MS = 60000L
        private const val CONSECUTIVE_READINGS_THRESHOLD = 5

        // --- NEW CONSTANT: How long to ignore audio after an alert (e.g. 4 seconds) ---
        private const val ALERT_SOUND_DURATION_MS = 4000L
    }

    override fun onCreate() {
        super.onCreate()
        applicationFlow = application as SnuzzNoiseApplication
        createNotificationChannel()

        try {
            SoundPlayer.init(this)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to init SoundPlayer", e)
        }

        try {
            audioClassifier = AudioClassifier(this)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to init classifier", e)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        alertThreshold = intent?.getIntExtra("ALERT_THRESHOLD", 70) ?: 70
        val actName = intent?.getStringExtra("ACTIVITY_NAME")
        activityNameForNotifs = if (!actName.isNullOrEmpty()) actName else "Default Mode"

        if (!isRecording) {
            startForeground(NOTIFICATION_ID, createNotification("Monitoring active").build())
            startRecording()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        stopRecording()
        serviceJob.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startRecording() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            stopSelf()
            return
        }

        val audioSource = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            MediaRecorder.AudioSource.UNPROCESSED
        } else {
            MediaRecorder.AudioSource.VOICE_RECOGNITION
        }

        val minBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
        val bufferSize = maxOf(minBufferSize, 4096)

        try {
            audioRecord = AudioRecord(audioSource, SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT, bufferSize)
            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                Log.w(TAG, "Unprocessed Audio failed. Falling back to MIC.")
                audioRecord = AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT, bufferSize)
            }
            audioRecord?.startRecording()
            isRecording = true
            serviceScope.launch { processAudio() }
        } catch (e: Exception) {
            Log.e(TAG, "Error starting recording", e)
            stopSelf()
        }
    }

    private suspend fun processAudio() {
        val bufferSize = audioRecord?.bufferSizeInFrames ?: return
        val buffer = ShortArray(bufferSize)

        while (serviceScope.isActive && isRecording) {
            try {
                val bytesRead = audioRecord?.read(buffer, 0, buffer.size) ?: -1
                if (bytesRead > 0) {

                    // --- UPDATED LOGIC: PAUSE IF ALERTING ---
                    if (isAlertPlaying) {
                        // While alert is playing, force UI to show low volume and SKIP processing
                        applicationFlow.updateDecibel(30.0)
                        delay(UPDATE_INTERVAL_MS)
                        continue
                    }

                    val decibel = calculateDecibel(buffer, bytesRead)
                    val smoothed = applySmoothingFilter(decibel)

                    applicationFlow.updateDecibel(smoothed)
                    checkNoiseLevel(smoothed)

                    for (i in 0 until bytesRead) {
                        if (aiBufferIndex < aiInputBufferSize) {
                            aiInputBuffer[aiBufferIndex] = buffer[i] / 32768.0f
                            aiBufferIndex++
                        }
                    }

                    if (aiBufferIndex >= aiInputBufferSize) {
                        val bufferCopy = aiInputBuffer.copyOf()
                        aiBufferIndex = 0
                        serviceScope.launch(Dispatchers.Default) {
                            processAI(bufferCopy)
                        }
                    }

                    val currentTime = System.currentTimeMillis()
                    if (currentTime - lastNotificationUpdate > NOTIFICATION_UPDATE_INTERVAL_MS) {
                        updateNotification("Current Level: %.1f dB".format(smoothed))
                        lastNotificationUpdate = currentTime
                    }
                    if (currentTime - lastLogTime > LOG_INTERVAL_MS) {
                        logNoiseEvent(smoothed, isAlert = false)
                        lastLogTime = currentTime
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Audio Loop Error", e)
            }
            delay(UPDATE_INTERVAL_MS)
        }
    }

    private fun processAI(inputBuffer: FloatArray) {
        try {
            val newLabel = audioClassifier.classify(inputBuffer)
            currentNoiseLabel = newLabel
            applicationFlow.updateNoiseLabel(newLabel)
        } catch (e: Exception) {
            Log.e(TAG, "AI Crash", e)
        }
    }

    private suspend fun checkNoiseLevel(currentDb: Double) {
        try {
            val notificationsEnabled = settingsRepository.notificationsEnabled.first()
            if (!notificationsEnabled) { resetAlertState(); return }

            val currentTime = System.currentTimeMillis()
            val userThreshold = alertThreshold

            var effectiveThreshold = userThreshold.toDouble()
            var shouldIgnoreDuration = false

            when (currentNoiseLabel) {
                "White Noise", "Rain", "Fan", "Typing", "Breathing", "Air Conditioner", "Heartbeat" -> {
                    effectiveThreshold = 95.0
                }
                "Screaming", "Glass Breaking", "Gunshot", "Siren", "Baby Crying" -> {
                    effectiveThreshold = 50.0
                    shouldIgnoreDuration = true
                }
                "Talking", "Speech", "Laughter", "Singing", "Clapping", "Music", "Footsteps" -> {
                    effectiveThreshold = userThreshold.toDouble()
                }
                else -> {
                    effectiveThreshold = userThreshold.toDouble()
                }
            }

            val isDanger = currentDb > effectiveThreshold

            if (isDanger) {
                consecutiveHighReadings++
                if (highNoiseStartTime == 0L) highNoiseStartTime = currentTime

                val requiredDuration = if (shouldIgnoreDuration) 1000L else NOISE_DURATION_THRESHOLD_MS
                val requiredReadings = if (shouldIgnoreDuration) 2 else CONSECUTIVE_READINGS_THRESHOLD

                if (consecutiveHighReadings >= requiredReadings &&
                    (currentTime - highNoiseStartTime) > requiredDuration &&
                    (currentTime - lastAlertTime) > ALERT_COOLDOWN_MS) {

                    showHighNoiseAlert(currentDb)
                    lastAlertTime = currentTime
                    resetAlertState()
                }
            } else {
                if (consecutiveHighReadings > 0) {
                    consecutiveHighReadings = 0
                    highNoiseStartTime = 0L
                }
            }
        } catch (e: Exception) { Log.e(TAG, "Check error", e) }
    }

    private fun showHighNoiseAlert(decibel: Double) {
        // 1. Pause Listening immediately
        isAlertPlaying = true

        val title = "High Noise Alert!"
        val msgType = if(currentNoiseLabel == "Screaming" || currentNoiseLabel == "Glass Breaking") "Critical Noise Detected" else "$activityNameForNotifs Threshold Exceeded"
        val message = "$msgType (%.1f dB)".format(decibel)

        val notificationItem = NotificationItem(
            title = title,
            message = message,
            timestamp = System.currentTimeMillis(),
            viewed = false,
            noiseType = currentNoiseLabel
        )
        repositoryScope.launch { settingsRepository.saveNotification(notificationItem) }
        logNoiseEvent(decibel, isAlert = true)

        // 2. Play Sound
        SoundPlayer.playAlert()

        // 3. Get Dynamic Duration and Wait
        val waitTime = SoundPlayer.getAlertDuration()

        serviceScope.launch {
            delay(waitTime) // Waits exactly as long as the file is!
            isAlertPlaying = false
            resetAlertState()
        }

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = NotificationCompat.Builder(this, HIGH_PRIORITY_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(R.drawable.logo)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .build()
        notificationManager.notify(ALERT_NOTIFICATION_ID, notification)
    }

    private fun stopRecording() {
        isRecording = false
        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (e: Exception) { Log.e(TAG, "Error stopping", e) }
        audioRecord = null
        stopForeground(true)
    }

    private fun logNoiseEvent(decibel: Double, isAlert: Boolean) {
        val event = NoiseEvent(
            timestamp = System.currentTimeMillis(),
            decibelLevel = decibel,
            isAlert = isAlert,
            noiseType = currentNoiseLabel
        )
        repositoryScope.launch {
            try { settingsRepository.logNoiseEvent(event) }
            catch (e: Exception) { Log.e(TAG, "Log failed", e) }
        }
    }

    private fun calculateDecibel(buffer: ShortArray, size: Int): Double {
        if (size <= 0) return 30.0
        var sum = 0.0
        for (i in 0 until size) sum += buffer[i] * buffer[i]
        val rms = sqrt(sum / size)
        if (rms < 1.0) return 30.0
        val db = 20 * log10(rms / 32767.0)
        return ((db + 96.0) * 1.25 - 20.0).coerceIn(30.0, 100.0)
    }

    private fun applySmoothingFilter(newDb: Double): Double {
        previousDecibel = if (previousDecibel == 0.0) newDb else previousDecibel * (1 - smoothingFactor) + newDb * smoothingFactor
        return previousDecibel
    }

    private fun resetAlertState() {
        highNoiseStartTime = 0L
        consecutiveHighReadings = 0
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val low = NotificationChannel(NOTIFICATION_CHANNEL_ID, NOTIFICATION_CHANNEL_NAME, NotificationManager.IMPORTANCE_LOW)
            val high = NotificationChannel(HIGH_PRIORITY_CHANNEL_ID, HIGH_PRIORITY_CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH).apply { enableVibration(true) }
            getSystemService(NotificationManager::class.java).apply {
                createNotificationChannel(low)
                createNotificationChannel(high)
            }
        }
    }

    private fun createNotification(text: String): NotificationCompat.Builder {
        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Snuzz Noise Monitoring")
            .setContentText(text)
            .setSmallIcon(R.drawable.logo)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setShowWhen(false)
    }

    private fun updateNotification(text: String) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, createNotification(text).build())
    }
}