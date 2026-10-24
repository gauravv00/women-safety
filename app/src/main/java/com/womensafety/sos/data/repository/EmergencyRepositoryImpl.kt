package com.womensafety.sos.data.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.womensafety.sos.data.firebase.FirebaseAuthManager
import com.womensafety.sos.data.model.EmergencyEvent
import com.womensafety.sos.data.model.NotificationItem
import com.womensafety.sos.data.model.NotificationStatus
import com.womensafety.sos.data.model.NotificationType
import com.womensafety.sos.domain.repository.EmergencyRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID

class EmergencyRepositoryImpl(
    private val authManager: FirebaseAuthManager,
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : EmergencyRepository {

    override suspend fun startEmergency(wardName: String): Result<String> {
        return try {
            val wardUid = authManager.ensureAuthenticated()
            if (wardUid.isBlank()) return Result.failure(Exception("Not authenticated"))

            val emergencyId = UUID.randomUUID().toString()
            val event = EmergencyEvent(
                id = emergencyId,
                wardUid = wardUid,
                wardName = wardName.ifBlank { "Ward" },
                status = "ACTIVE",
                startedAt = System.currentTimeMillis()
            )

            val batch = firestore.batch()
            val emergencyRef = firestore.collection("emergencies").document(emergencyId)
            batch.set(emergencyRef, event.toMap())

            // Query pairs where wardUid == current user
            val pairsSnapshot = firestore.collection("pairs")
                .whereEqualTo("wardUid", wardUid)
                .get()
                .await()

            for (pairDoc in pairsSnapshot.documents) {
                val guardianUid = pairDoc.getString("guardianUid") ?: continue
                val notifRef = firestore.collection("notifications")
                    .document(guardianUid)
                    .collection("items")
                    .document()
                val notif = NotificationItem(
                    id = notifRef.id,
                    type = NotificationType.EMERGENCY,
                    title = "🚨 SOS ALERT!",
                    body = "${wardName.ifBlank { "Ward" }} is in danger! Tap to track live location.",
                    refId = emergencyId,
                    fromUid = wardUid,
                    fromName = wardName,
                    status = NotificationStatus.ACTIVE,
                    read = false,
                    createdAt = System.currentTimeMillis()
                )
                batch.set(notifRef, notif.toMap())
            }

            batch.commit().await()
            Result.success(emergencyId)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start emergency", e)
            Result.failure(e)
        }
    }

    override suspend fun updateLocation(
        emergencyId: String,
        lat: Double,
        lng: Double,
        accuracy: Float
    ): Result<Unit> {
        return try {
            if (emergencyId.isBlank()) return Result.failure(Exception("Invalid emergencyId"))
            val updates = mapOf(
                "lastLat" to lat,
                "lastLng" to lng,
                "lastAccuracy" to accuracy.toDouble(),
                "lastLocationTs" to System.currentTimeMillis()
            )
            firestore.collection("emergencies").document(emergencyId)
                .update(updates)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update location for $emergencyId", e)
            Result.failure(e)
        }
    }

    override suspend fun resolveEmergency(emergencyId: String): Result<Unit> {
        return try {
            val wardUid = authManager.ensureAuthenticated()
            if (wardUid.isBlank() || emergencyId.isBlank()) return Result.failure(Exception("Invalid state"))

            val emergencyRef = firestore.collection("emergencies").document(emergencyId)
            val emergencySnap = emergencyRef.get().await()
            val wardName = emergencySnap.getString("wardName") ?: "Ward"

            val batch = firestore.batch()
            batch.update(
                emergencyRef,
                mapOf(
                    "status" to "RESOLVED",
                    "resolvedAt" to System.currentTimeMillis()
                )
            )

            val pairsSnapshot = firestore.collection("pairs")
                .whereEqualTo("wardUid", wardUid)
                .get()
                .await()

            for (pairDoc in pairsSnapshot.documents) {
                val guardianUid = pairDoc.getString("guardianUid") ?: continue
                val notifRef = firestore.collection("notifications")
                    .document(guardianUid)
                    .collection("items")
                    .document()
                val notif = NotificationItem(
                    id = notifRef.id,
                    type = NotificationType.RESOLVED,
                    title = "✓ SOS Resolved",
                    body = "$wardName is now safe. Emergency resolved.",
                    refId = emergencyId,
                    fromUid = wardUid,
                    fromName = wardName,
                    status = NotificationStatus.RESOLVED,
                    read = false,
                    createdAt = System.currentTimeMillis()
                )
                batch.set(notifRef, notif.toMap())
            }

            batch.commit().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to resolve emergency $emergencyId", e)
            Result.failure(e)
        }
    }

    override fun observeEmergency(emergencyId: String): Flow<EmergencyEvent?> = callbackFlow {
        if (emergencyId.isBlank()) {
            trySend(null)
            close()
            return@callbackFlow
        }
        val registration = firestore.collection("emergencies").document(emergencyId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error observing emergency $emergencyId", error)
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    val data = snapshot.data ?: emptyMap()
                    trySend(EmergencyEvent.fromMap(snapshot.id, data))
                } else {
                    trySend(null)
                }
            }
        awaitClose { registration.remove() }
    }

    override fun observeActiveEmergencies(): Flow<List<EmergencyEvent>> = callbackFlow {
        val uid = authManager.currentUserId
        if (uid.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        var emergenciesListener: ListenerRegistration? = null

        val pairsListener = firestore.collection("pairs")
            .whereEqualTo("guardianUid", uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error listening to pairs for active emergencies", error)
                    return@addSnapshotListener
                }
                val wardUids = snapshot?.documents?.mapNotNull { it.getString("wardUid") }?.distinct() ?: emptyList()
                emergenciesListener?.remove()

                if (wardUids.isEmpty()) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val chunks = wardUids.take(30)
                emergenciesListener = firestore.collection("emergencies")
                    .whereIn("wardUid", chunks)
                    .whereEqualTo("status", "ACTIVE")
                    .addSnapshotListener emListener@{ emSnapshot, emError ->
                        if (emError != null) {
                            Log.e(TAG, "Error listening to active emergencies", emError)
                            return@emListener
                        }
                        val events = emSnapshot?.documents?.mapNotNull { doc ->
                            doc.data?.let { EmergencyEvent.fromMap(doc.id, it) }
                        } ?: emptyList()
                        trySend(events)
                    }
            }

        awaitClose {
            emergenciesListener?.remove()
            pairsListener.remove()
        }
    }

    companion object {
        private const val TAG = "EmergencyRepoImpl"
    }
}
