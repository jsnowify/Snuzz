package com.snowi.snuzznoise.presentation.auth

import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class GoogleAuthClient @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val auth: FirebaseAuth
) {
    // ✅ Matches "client_type": 3 in google-services.json
    private val WEB_CLIENT_ID = "562174036495-ahn9g2jh50k8q8gcb45navehksal3vrn.apps.googleusercontent.com"

    suspend fun signIn(context: Context): Result<AuthResult> {
        return try {
            Log.d("GoogleAuth", "Starting sign-in process...")

            // 1. Clear State (Ensures account selector appears)
            try {
                CredentialManager.create(context).clearCredentialState(ClearCredentialStateRequest())
            } catch (e: Exception) {
                Log.w("GoogleAuth", "Failed to clear credentials", e)
            }

            // 2. Configure Options
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(WEB_CLIENT_ID)
                .setAutoSelectEnabled(false)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            // 3. Show Selector
            val credentialManager = CredentialManager.create(context)
            val result = credentialManager.getCredential(request = request, context = context)
            val credential = result.credential

            when (credential) {
                is CustomCredential -> {
                    if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                        val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                        val firebaseCredential = GoogleAuthProvider.getCredential(googleIdTokenCredential.idToken, null)

                        val currentUser = auth.currentUser
                        val authResult: AuthResult

                        // 4. Link or Sign In (With Cleanup Logic)
                        if (currentUser != null && currentUser.isAnonymous) {
                            authResult = try {
                                Log.d("GoogleAuth", "🔗 Linking Guest account...")
                                currentUser.linkWithCredential(firebaseCredential).await()
                            } catch (e: FirebaseAuthUserCollisionException) {
                                Log.w("GoogleAuth", "⚠️ Account conflict. Switching users...")

                                // 🛑 ENGINEER FIX: Delete the abandoned Guest account before switching
                                try {
                                    Log.d("GoogleAuth", "🗑️ Cleaning up abandoned Guest account...")
                                    currentUser.delete().await()
                                } catch (delEx: Exception) {
                                    Log.e("GoogleAuth", "Failed to delete guest account (ignoring)", delEx)
                                }

                                auth.signInWithCredential(firebaseCredential).await()
                            }
                        } else {
                            authResult = auth.signInWithCredential(firebaseCredential).await()
                        }

                        // 5. Force Profile Refresh (Fixes "User" name bug)
                        val user = authResult.user
                        if (user != null) {
                            Log.d("GoogleAuth", "🔄 Reloading user data...")
                            user.reload().await() // Must reload first to get provider data
                            updateProfileIfNeeded(user)
                            delay(100)
                            Log.d("GoogleAuth", "✅ Final User: ${user.displayName}")
                        }

                        Result.success(authResult)
                    } else {
                        Result.failure(Exception("Unexpected credential type: ${credential.type}"))
                    }
                }
                else -> Result.failure(Exception("Unexpected credential class"))
            }

        } catch (e: GetCredentialException) {
            Log.e("GoogleAuth", "GetCredential failed", e)
            Result.failure(e)
        } catch (e: Exception) {
            Log.e("GoogleAuth", "Sign-in failed", e)
            Result.failure(e)
        }
    }

    private suspend fun updateProfileIfNeeded(user: FirebaseUser) {
        val googleProfile = user.providerData.find { it.providerId == GoogleAuthProvider.PROVIDER_ID }

        if (googleProfile != null) {
            val currentName = user.displayName
            val currentPhoto = user.photoUrl

            if (currentName.isNullOrEmpty() || currentName == "User" || currentPhoto == null) {
                Log.d("GoogleAuth", "🔧 Fixing missing profile data...")

                val profileUpdates = UserProfileChangeRequest.Builder()
                    .setDisplayName(googleProfile.displayName)
                    .setPhotoUri(googleProfile.photoUrl)
                    .build()

                try {
                    user.updateProfile(profileUpdates).await()
                    user.reload().await()
                    Log.d("GoogleAuth", "✅ Profile Fixed: ${user.displayName}")
                } catch (e: Exception) {
                    Log.e("GoogleAuth", "Failed to sync profile", e)
                }
            }
        }
    }

    fun getSignedInUser() = auth.currentUser

    suspend fun signOut() {
        try {
            CredentialManager.create(appContext).clearCredentialState(ClearCredentialStateRequest())
            auth.signOut()

            // 🛑 ENGINEER FIX: Do NOT create a new anonymous account here immediately.
            // Let the MainActivity or the next user action decide if a Guest account is needed.
            Log.d("GoogleAuth", "✅ Signed out. No new Guest account created yet.")

        } catch (e: Exception) {
            Log.e("GoogleAuth", "Sign Out Error", e)
        }
    }
}