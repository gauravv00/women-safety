package com.womensafety.sos.data.firebase

import android.content.Context
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class FirebaseAuthManager private constructor(private val context: Context) {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val scope = CoroutineScope(Dispatchers.IO)

    private val _currentUser = MutableStateFlow<FirebaseUser?>(auth.currentUser)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser.asStateFlow()

    val currentUserId: String
        get() = auth.currentUser?.uid ?: ""

    init {
        // Enable Firestore offline persistence
        try {
            val settings = FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)
                .build()
            firestore.firestoreSettings = settings
        } catch (e: Exception) {
            Log.w(TAG, "Firestore persistence already configured: ${e.message}")
        }

        auth.addAuthStateListener { firebaseAuth ->
            _currentUser.value = firebaseAuth.currentUser
            if (firebaseAuth.currentUser != null) {
                scope.launch {
                    syncUserDocument()
                    syncFcmToken()
                }
            }
        }
    }

    suspend fun ensureAuthenticated(): String {
        val user = auth.currentUser
        if (user != null) {
            return user.uid
        }
        return try {
            val result = auth.signInAnonymously().await()
            val uid = result.user?.uid ?: ""
            _currentUser.value = result.user
            syncUserDocument()
            syncFcmToken()
            uid
        } catch (e: Exception) {
            Log.e(TAG, "Anonymous sign-in failed", e)
            ""
        }
    }

    suspend fun updateFcmToken(token: String) {
        val uid = currentUserId
        if (uid.isBlank() || token.isBlank()) return
        try {
            firestore.collection("users").document(uid)
                .set(mapOf("fcmToken" to token, "lastSeen" to System.currentTimeMillis()), SetOptions.merge())
                .await()
            Log.d(TAG, "FCM token synced to users/$uid")
        } catch (e: Exception) {
            Log.e(TAG, "Error updating FCM token in Firestore", e)
        }
    }

    private suspend fun syncFcmToken() {
        try {
            val token = FirebaseMessaging.getInstance().token.await()
            if (!token.isNullOrBlank()) {
                updateFcmToken(token)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Unable to fetch FCM token: ${e.message}")
        }
    }

    suspend fun syncUserDocument(displayName: String? = null) {
        val uid = currentUserId
        if (uid.isBlank()) return
        try {
            val sharedPrefs = context.getSharedPreferences("women_safety_prefs", Context.MODE_PRIVATE)
            val prefsName = sharedPrefs.getString("user_display_name", "User") ?: "User"
            val pairingCode = sharedPrefs.getString("my_pairing_code", "") ?: ""
            val finalName = displayName ?: prefsName

            val data = mutableMapOf<String, Any>(
                "uid" to uid,
                "displayName" to finalName,
                "lastSeen" to System.currentTimeMillis()
            )
            if (pairingCode.isNotBlank()) {
                data["pairingCode"] = pairingCode
            }
            firestore.collection("users").document(uid)
                .set(data, SetOptions.merge())
                .await()
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing user document", e)
        }
    }

    companion object {
        private const val TAG = "FirebaseAuthManager"

        @Volatile
        private var instance: FirebaseAuthManager? = null

        fun getInstance(context: Context): FirebaseAuthManager {
            return instance ?: synchronized(this) {
                instance ?: FirebaseAuthManager(context.applicationContext).also { instance = it }
            }
        }
    }
}
