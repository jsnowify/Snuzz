package com.snowi.snuzznoise.data.repository

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import com.snowi.snuzznoise.presentation.feature.history.NoiseEvent
import com.snowi.snuzznoise.presentation.feature.notification.NotificationItem
import com.snowi.snuzznoise.presentation.feature.profile.components.ActivityProfile
import com.snowi.snuzznoise.presentation.theme.AppTheme
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.tasks.await
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

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

    companion object {
        private const val TAG = "SettingsRepository"
        private val NOTIFICATIONS_ENABLED_KEY = booleanPreferencesKey("notifications_enabled")
        private val APP_THEME_KEY = stringPreferencesKey("app_theme")
    }

    // --- 🚀 Reactive User ID Flow ---
    // This watches for Auth changes (Guest -> Google) instantly.
    private val currentUserId: Flow<String?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            trySend(firebaseAuth.currentUser?.uid)
        }
        auth.addAuthStateListener(listener)
        // Emit initial state immediately
        trySend(auth.currentUser?.uid)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    // Helper to get a collection reference for the *active* user
    private fun getUserSubCollection(uid: String, collectionName: String) =
        firestore.collection("users").document(uid).collection(collectionName)


    // ============================================================================================
    // FIREBASE: PROFILES (Updated to return Empty List instead of Error)
    // ============================================================================================

    @OptIn(ExperimentalCoroutinesApi::class)
    fun getActivityProfiles(): Flow<Result<List<ActivityProfile>>> {
        return currentUserId.flatMapLatest { uid ->
            if (uid == null) {
                // ✅ FIX: Don't error out. Just return an empty list for Guest/Loading state.
                flowOf(Result.Success(emptyList()))
            } else {
                getUserSubCollection(uid, "profiles").snapshots().map { snapshot ->
                    try {
                        val profiles = snapshot.toObjects(ActivityProfile::class.java)
                        Log.d(TAG, "getActivityProfiles: Loaded ${profiles.size} for user $uid")
                        Result.Success(profiles)
                    } catch (e: Exception) {
                        Result.Error(e)
                    }
                }
            }
        }
    }

    // Standard CRUD methods still use the synchronous currentUser check
    private val currentUser get() = auth.currentUser

    suspend fun saveActivityProfile(profile: ActivityProfile): Result<Unit> {
        val uid = currentUser?.uid ?: return Result.Error(Exception("No User"))
        return try {
            getUserSubCollection(uid, "profiles").document(profile.id).set(profile).await()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    suspend fun deleteActivityProfile(profileId: String): Result<Unit> {
        val uid = currentUser?.uid ?: return Result.Error(Exception("No User"))
        return try {
            getUserSubCollection(uid, "profiles").document(profileId).delete().await()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    // ============================================================================================
    // FIREBASE: NOTIFICATIONS (Updated to return Empty List instead of Error)
    // ============================================================================================

    @OptIn(ExperimentalCoroutinesApi::class)
    fun getNotifications(): Flow<Result<List<NotificationItem>>> {
        return currentUserId.flatMapLatest { uid ->
            if (uid == null) {
                // ✅ FIX: Don't error out.
                flowOf(Result.Success(emptyList()))
            } else {
                getUserSubCollection(uid, "notifications")
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .snapshots()
                    .map { snapshot ->
                        try {
                            val items = snapshot.toObjects(NotificationItem::class.java)
                            Result.Success(items)
                        } catch (e: Exception) {
                            Result.Error(e)
                        }
                    }
            }
        }
    }

    suspend fun saveNotification(notification: NotificationItem): Result<Unit> {
        val uid = currentUser?.uid ?: return Result.Error(Exception("No User"))
        return try {
            getUserSubCollection(uid, "notifications").add(notification).await()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    suspend fun deleteAllNotifications(): Result<Unit> {
        val uid = currentUser?.uid ?: return Result.Error(Exception("No User"))
        return try {
            val collection = getUserSubCollection(uid, "notifications")
            val snapshot = collection.get().await()
            val batch = firestore.batch()
            for (doc in snapshot.documents) {
                batch.delete(doc.reference)
            }
            batch.commit().await()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun getUnreadNotificationCount(): Flow<Result<Int>> {
        return currentUserId.flatMapLatest { uid ->
            if (uid == null) flowOf(Result.Success(0))
            else getUserSubCollection(uid, "notifications")
                .whereEqualTo("viewed", false)
                .snapshots()
                .map { Result.Success(it.size()) }
        }
    }

    suspend fun markAllNotificationsAsViewed(): Result<Unit> {
        val uid = currentUser?.uid ?: return Result.Error(Exception("No User"))
        return try {
            val collection = getUserSubCollection(uid, "notifications")
            val batch = firestore.batch()
            val snapshot = collection.whereEqualTo("viewed", false).get().await()
            snapshot.documents.forEach { doc ->
                batch.update(doc.reference, "viewed", true)
            }
            batch.commit().await()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    // ============================================================================================
    // DATASTORE (Local Settings)
    // ============================================================================================

    val notificationsEnabled: Flow<Boolean> = context.dataStore.data
        .catch { exception -> if (exception is IOException) emit(emptyPreferences()) else throw exception }
        .map { it[NOTIFICATIONS_ENABLED_KEY] ?: true }

    suspend fun setNotificationsEnabled(isEnabled: Boolean) {
        context.dataStore.edit { it[NOTIFICATIONS_ENABLED_KEY] = isEnabled }
    }

    fun getAppTheme(): Flow<AppTheme> = context.dataStore.data
        .catch { exception -> if (exception is IOException) emit(emptyPreferences()) else throw exception }
        .map {
            try { AppTheme.valueOf(it[APP_THEME_KEY] ?: AppTheme.SAGE.name) }
            catch (e: Exception) { AppTheme.SAGE }
        }

    suspend fun setAppTheme(theme: AppTheme) {
        context.dataStore.edit { it[APP_THEME_KEY] = theme.name }
    }

    // ============================================================================================
    // FIREBASE: HISTORY (Fixed null safety)
    // ============================================================================================

    suspend fun logNoiseEvent(event: NoiseEvent): Result<Unit> {
        val uid = currentUser?.uid ?: return Result.Error(Exception("No User"))
        return try {
            getUserSubCollection(uid, "history").add(event).await()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun getNoiseHistory(timeInMillis: Long): Flow<Result<List<NoiseEvent>>> {
        return currentUserId.flatMapLatest { uid ->
            if (uid == null) {
                // ✅ FIX: Return empty list, not error
                flowOf(Result.Success(emptyList()))
            } else {
                val startTime = System.currentTimeMillis() - timeInMillis
                getUserSubCollection(uid, "history")
                    .whereGreaterThanOrEqualTo("timestamp", startTime)
                    .orderBy("timestamp", Query.Direction.ASCENDING)
                    .snapshots()
                    .map {
                        try { Result.Success(it.toObjects(NoiseEvent::class.java)) }
                        catch (e: Exception) { Result.Error(e) }
                    }
            }
        }
    }
}