package com.snowi.snuzznoise.utils

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaMetadataRetriever // Import this
import android.media.SoundPool
import android.net.Uri
import com.snowi.snuzznoise.R
import java.util.concurrent.atomic.AtomicBoolean

object SoundPlayer {

    private var soundPool: SoundPool? = null
    private var tapSoundId: Int? = null
    private var alertSoundId: Int? = null

    // Store the exact duration of the alert file
    private var alertDurationMs: Long = 3000L // Default fallback (3s)

    private val isLoaded = AtomicBoolean(false)

    fun init(context: Context) {
        if (soundPool != null) return

        // --- 1. CALCULATE DURATION DYNAMICALLY ---
        try {
            val retriever = MediaMetadataRetriever()
            // Create URI for the raw resource
            val uri = Uri.parse("android.resource://${context.packageName}/${R.raw.alert_sound}")
            retriever.setDataSource(context, uri)

            val timeString = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            val time = timeString?.toLongOrNull() ?: 3000L

            // Add a small buffer (500ms) to ensure sound is fully finished
            alertDurationMs = time + 500L

            retriever.release()
        } catch (e: Exception) {
            e.printStackTrace()
            alertDurationMs = 3000L // Fallback on error
        }

        // --- 2. SETUP SOUNDPOOL (Existing Code) ---
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        val pool = SoundPool.Builder()
            .setMaxStreams(3)
            .setAudioAttributes(audioAttributes)
            .build()

        pool.setOnLoadCompleteListener { _, _, status ->
            if (status == 0) isLoaded.set(true)
        }

        tapSoundId = pool.load(context.applicationContext, R.raw.tap_sound, 1)
        alertSoundId = pool.load(context.applicationContext, R.raw.alert_sound, 1)

        soundPool = pool
    }

    fun play() {
        if (isLoaded.get()) {
            val id = tapSoundId ?: return
            soundPool?.play(id, 1f, 1f, 1, 0, 1f)
        }
    }

    fun playAlert() {
        if (isLoaded.get()) {
            val id = alertSoundId ?: return
            soundPool?.play(id, 1f, 1f, 2, 0, 1f)
        }
    }

    // --- NEW GETTER FOR SERVICE ---
    fun getAlertDuration(): Long {
        return alertDurationMs
    }

    fun release() {
        soundPool?.release()
        soundPool = null
        tapSoundId = null
        alertSoundId = null
        isLoaded.set(false)
    }
}