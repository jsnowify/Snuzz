package com.snowi.snuzznoise.data.repository

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.snapshots
import com.snowi.snuzznoise.presentation.feature.history.NoiseEvent
import com.snowi.snuzznoise.presentation.feature.notification.NotificationItem
import com.snowi.snuzznoise.presentation.feature.profile.components.ActivityProfile
import com.snowi.snuzznoise.presentation.theme.AppTheme
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

// Setup DataStore
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

// Result Wrapper Class
sealed class Result<out T> {
    data class Success<out T>(val data: T) : Result<T>()
    data class Error(val exception: Exception) : Result<Nothing>()
}

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
    private val currentUser
        get() = auth.currentUser

    companion object {
        private const val TAG = "SettingsRepository"
        // Define DataStore Keys
        private val NOTIFICATIONS_ENABLED_KEY = booleanPreferencesKey("notifications_enabled")
        private val APP_THEME_KEY = stringPreferencesKey("app_theme")
    }

    private fun getUserSubCollection(collectionName: String) =
        currentUser?.uid?.let { uid ->
            firestore.collection("users").document(uid).collection(collectionName)
        }

    // ============================================================================================
    // FIREBASE: PROFILES
    // ============================================================================================

    private fun getProfilesCollection() = getUserSubCollection("profiles")

    fun getActivityProfiles(): Flow<Result<List<ActivityProfile>>> {
        val collection = getProfilesCollection()
            ?: return kotlinx.coroutines.flow.flowOf(Result.Error(Exception("User not authenticated")))

        return collection.snapshots().map { snapshot ->
            try {
                val profiles = snapshot.toObjects(ActivityProfile::class.java)
                Log.d(TAG, "getActivityProfiles: Success! Found ${profiles.size} profiles.")
                Result.Success(profiles)
            } catch (e: Exception) {
                Log.e(TAG, "getActivityProfiles: Error fetching profiles.", e)
                Result.Error(e)
            }
        }
    }

    suspend fun saveActivityProfile(profile: ActivityProfile): Result<Unit> {
        val collection = getProfilesCollection()
            ?: return Result.Error(Exception("User not authenticated"))
        return try {
            collection.document(profile.id).set(profile).await()
            Log.d(TAG, "saveActivityProfile: Success! Profile saved with ID: ${profile.id}")
            Result.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "saveActivityProfile: Error saving profile.", e)
            Result.Error(e)
        }
    }

    suspend fun deleteActivityProfile(profileId: String): Result<Unit> {
        val collection = getProfilesCollection()
            ?: return Result.Error(Exception("User not authenticated"))
        return try {
            collection.document(profileId).delete().await()
            Log.d(TAG, "deleteActivityProfile: Success! Profile deleted with ID: $profileId")
            Result.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "deleteActivityProfile: Error deleting profile.", e)
            Result.Error(e)
        }
    }

    // ============================================================================================
    // FIREBASE: NOTIFICATIONS
    // ============================================================================================

    private fun getNotificationsCollection() = getUserSubCollection("notifications")

    fun getNotifications(): Flow<Result<List<NotificationItem>>> {
        val collection = getNotificationsCollection()
            ?: return kotlinx.coroutines.flow.flowOf(Result.Error(Exception("User not authenticated")))

        return collection
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .snapshots()
            .map { snapshot ->
                try {
                    val notifications = snapshot.toObjects(NotificationItem::class.java)
                    Log.d(TAG, "getNotifications: Success! Found ${notifications.size} notifications.")
                    Result.Success(notifications)
                } catch (e: Exception) {
                    Log.e(TAG, "getNotifications: Error fetching notifications.", e)
                    Result.Error(e)
                }
            }
    }

    suspend fun saveNotification(notification: NotificationItem): Result<Unit> {
        val collection = getNotificationsCollection()
            ?: return Result.Error(Exception("User not authenticated"))
        return try {
            Log.d(TAG, "Saving Notification: ${notification.title} | dB: ${notification.decibelLevel}")
            collection.add(notification).await()
            Log.d(TAG, "saveNotification: Success! Notification saved.")
            Result.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "saveNotification: Error saving notification.", e)
            Result.Error(e)
        }
    }

    // --- NEW: Added this specifically to fix the "Clear History" button ---
    suspend fun deleteAllNotifications(): Result<Unit> {
        val collection = getNotificationsCollection()
            ?: return Result.Error(Exception("User not authenticated"))
        return try {
            val snapshot = collection.get().await()
            val batch = firestore.batch()
            for (doc in snapshot.documents) {
                batch.delete(doc.reference)
            }
            batch.commit().await()
            Log.d(TAG, "Deleted all notifications.")
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    fun getUnreadNotificationCount(): Flow<Result<Int>> {
        val collection = getNotificationsCollection()
            ?: return kotlinx.coroutines.flow.flowOf(Result.Error(Exception("User not authenticated")))

        return collection.whereEqualTo("viewed", false).snapshots().map { snapshot ->
            try {
                Result.Success(snapshot.size())
            } catch (e: Exception) {
                Result.Error(e)
            }
        }
    }

    suspend fun markAllNotificationsAsViewed(): Result<Unit> {
        val collection = getNotificationsCollection()
            ?: return Result.Error(Exception("User not authenticated"))
        return try {
            val batch = firestore.batch()
            val snapshot = collection.whereEqualTo("viewed", false).get().await()
            snapshot.documents.forEach { doc ->
                batch.update(doc.reference, "viewed", true)
            }
            batch.commit().await()
            Log.d(TAG, "Marked ${snapshot.size()} notifications as viewed.")
            Result.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error marking notifications as viewed.", e)
            Result.Error(e)
        }
    }

    // ============================================================================================
    // DATASTORE: LOCAL SETTINGS (Theme & Notifications)
    // ============================================================================================

    // 1. Notification Preference
    val notificationsEnabled: Flow<Boolean> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            preferences[NOTIFICATIONS_ENABLED_KEY] ?: true
        }

    suspend fun setNotificationsEnabled(isEnabled: Boolean) {
        context.dataStore.edit { settings ->
            settings[NOTIFICATIONS_ENABLED_KEY] = isEnabled
        }
    }

    // 2. App Theme Preference
    fun getAppTheme(): Flow<AppTheme> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            val themeName = preferences[APP_THEME_KEY] ?: AppTheme.SAGE.name
            try {
                AppTheme.valueOf(themeName)
            } catch (e: IllegalArgumentException) {
                AppTheme.SAGE // Fallback
            }
        }

    suspend fun setAppTheme(theme: AppTheme) {
        context.dataStore.edit { preferences ->
            preferences[APP_THEME_KEY] = theme.name
        }
    }

    // ============================================================================================
    // FIREBASE: HISTORY / LOGGING
    // ============================================================================================

    private fun getHistoryCollection() = getUserSubCollection("history")

    suspend fun logNoiseEvent(event: NoiseEvent): Result<Unit> {
        val collection = getHistoryCollection() ?: return Result.Error(Exception("User not authenticated"))
        return try {
            collection.add(event).await()
            Log.d(TAG, "logNoiseEvent: Success!")
            Result.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "logNoiseEvent: Error.", e)
            Result.Error(e)
        }
    }

    fun getNoiseHistory(timeInMillis: Long): Flow<Result<List<NoiseEvent>>> {
        val collection = getHistoryCollection()
            ?: return kotlinx.coroutines.flow.flowOf(Result.Error(Exception("User not authenticated")))
        val startTime = System.currentTimeMillis() - timeInMillis
        return collection.whereGreaterThanOrEqualTo("timestamp", startTime)
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .snapshots()
            .map { snapshot ->
                try {
                    val events = snapshot.toObjects(NoiseEvent::class.java)
                    Result.Success(events)
                } catch (e: Exception) {
                    Result.Error(e)
                }
            }
    }
}