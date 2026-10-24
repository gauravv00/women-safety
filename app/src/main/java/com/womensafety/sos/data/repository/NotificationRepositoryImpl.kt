package com.womensafety.sos.data.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.womensafety.sos.data.firebase.FirebaseAuthManager
import com.womensafety.sos.data.model.NotificationItem
import com.womensafety.sos.data.model.NotificationStatus
import com.womensafety.sos.data.model.NotificationType
import com.womensafety.sos.domain.repository.NotificationRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID

class NotificationRepositoryImpl(
    private val authManager: FirebaseAuthManager,
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : NotificationRepository {

    override fun getNotifications(): Flow<List<NotificationItem>> = callbackFlow {
        val uid = authManager.ensureAuthenticated()
        if (uid.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val registration = firestore.collection("notifications")
            .document(uid)
            .collection("items")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error listening to notifications for $uid", error)
                    return@addSnapshotListener
                }

                val items = snapshot?.documents?.mapNotNull { doc ->
                    doc.data?.let { data ->
                        NotificationItem.fromMap(doc.id, data)
                    }
                } ?: emptyList()

                trySend(items)
            }

        awaitClose {
            registration.remove()
        }
    }

    override suspend fun markRead(notificationId: String) {
        val uid = authManager.currentUserId
        if (uid.isBlank() || notificationId.isBlank()) return
        try {
            firestore.collection("notifications")
                .document(uid)
                .collection("items")
                .document(notificationId)
                .update("read", true)
                .await()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to mark notification read", e)
        }
    }

    override suspend fun markAllRead() {
        val uid = authManager.currentUserId
        if (uid.isBlank()) return
        try {
            val unreadDocs = firestore.collection("notifications")
                .document(uid)
                .collection("items")
                .whereEqualTo("read", false)
                .get()
                .await()

            if (!unreadDocs.isEmpty) {
                val batch = firestore.batch()
                for (doc in unreadDocs.documents) {
                    batch.update(doc.reference, "read", true)
                }
                batch.commit().await()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to mark all read", e)
        }
    }

    override suspend fun deleteNotification(notificationId: String) {
        val uid = authManager.currentUserId
        if (uid.isBlank() || notificationId.isBlank()) return
        try {
            firestore.collection("notifications")
                .document(uid)
                .collection("items")
                .document(notificationId)
                .delete()
                .await()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete notification", e)
        }
    }

    override suspend fun handlePairRequest(notification: NotificationItem, accept: Boolean): Result<Unit> {
        return try {
            val wardUid = authManager.currentUserId
            val guardianUid = notification.fromUid
            val pairRequestId = notification.refId

            val batch = firestore.batch()

            // 1. Update the pair request status
            if (pairRequestId.isNotBlank()) {
                val reqRef = firestore.collection("pairRequests").document(pairRequestId)
                batch.update(reqRef, "status", if (accept) "accepted" else "rejected")
            }

            // 2. If accepted, create active pair document
            if (accept && guardianUid.isNotBlank() && wardUid.isNotBlank()) {
                val pairId = "${wardUid}_$guardianUid"
                val pairRef = firestore.collection("pairs").document(pairId)
                val pairData = mapOf(
                    "id" to pairId,
                    "wardUid" to wardUid,
                    "guardianUid" to guardianUid,
                    "wardName" to notification.title,
                    "createdAt" to System.currentTimeMillis()
                )
                batch.set(pairRef, pairData)
            }

            // 3. Write feedback notification for Guardian
            if (guardianUid.isNotBlank()) {
                val guardianNotifRef = firestore.collection("notifications")
                    .document(guardianUid)
                    .collection("items")
                    .document()

                val guardianItem = NotificationItem(
                    id = guardianNotifRef.id,
                    type = if (accept) NotificationType.PAIR_ACCEPTED else NotificationType.PAIR_REJECTED,
                    title = if (accept) "Pairing Accepted" else "Pairing Declined",
                    body = if (accept)
                        "${notification.fromName.ifBlank { "Ward" }} accepted your pairing request. You can now monitor their safety."
                    else
                        "${notification.fromName.ifBlank { "Ward" }} declined your pairing request.",
                    refId = pairRequestId,
                    fromUid = wardUid,
                    fromName = notification.title,
                    status = NotificationStatus.HANDLED,
                    read = false,
                    createdAt = System.currentTimeMillis()
                )
                batch.set(guardianNotifRef, guardianItem.toMap())
            }

            // 4. Update the current notification item to HANDLED and read
            val currentNotifRef = firestore.collection("notifications")
                .document(wardUid)
                .collection("items")
                .document(notification.id)
            batch.update(currentNotifRef, mapOf("status" to NotificationStatus.HANDLED.name, "read" to true))

            batch.commit().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error handling pair request", e)
            Result.failure(e)
        }
    }

    override suspend fun sendPairRequest(wardPairingCode: String, guardianName: String): Result<Unit> {
        return try {
            val guardianUid = authManager.ensureAuthenticated()
            if (guardianUid.isBlank()) return Result.failure(Exception("Not authenticated"))

            // 1. Look up ward by pairingCode field in users collection
            val wardQuery = firestore.collection("users")
                .whereEqualTo("pairingCode", wardPairingCode)
                .limit(1)
                .get()
                .await()

            if (wardQuery.isEmpty) {
                return Result.failure(Exception("No user found with pairing code: $wardPairingCode"))
            }

            val wardDoc = wardQuery.documents.first()
            val wardUid = wardDoc.id
            if (wardUid == guardianUid) {
                return Result.failure(Exception("You cannot pair with yourself."))
            }
            val wardName = wardDoc.getString("displayName") ?: "Ward"

            // 2. Duplicate check — is there already an active pair?
            val existingPair = firestore.collection("pairs")
                .document("${wardUid}_${guardianUid}")
                .get()
                .await()
            if (existingPair.exists()) {
                return Result.failure(Exception("Already paired with this ward."))
            }

            // 3. Check for pending request already sent
            val pendingReq = firestore.collection("notifications")
                .document(wardUid)
                .collection("items")
                .whereEqualTo("type", NotificationType.PAIR_REQUEST.name)
                .whereEqualTo("fromUid", guardianUid)
                .whereEqualTo("status", NotificationStatus.PENDING.name)
                .limit(1)
                .get()
                .await()
            if (!pendingReq.isEmpty) {
                return Result.failure(Exception("A pairing request is already pending with this ward."))
            }

            val requestId = UUID.randomUUID().toString()
            val batch = firestore.batch()

            // 4. Write pairRequests audit doc
            val pairReqRef = firestore.collection("pairRequests").document(requestId)
            batch.set(pairReqRef, mapOf(
                "id" to requestId,
                "guardianUid" to guardianUid,
                "guardianName" to guardianName,
                "wardUid" to wardUid,
                "wardPairingCode" to wardPairingCode,
                "status" to "pending",
                "createdAt" to System.currentTimeMillis()
            ))

            // 5. Write PAIR_REQUEST notification into ward's inbox
            val notifRef = firestore.collection("notifications")
                .document(wardUid)
                .collection("items")
                .document()
            val notifItem = NotificationItem(
                id = notifRef.id,
                type = NotificationType.PAIR_REQUEST,
                title = "New Pairing Request",
                body = "${guardianName.ifBlank { "Someone" }} wants to monitor your safety. Accept to allow them to see your live location during an emergency.",
                refId = requestId,
                fromUid = guardianUid,
                fromName = guardianName,
                status = NotificationStatus.PENDING,
                read = false,
                createdAt = System.currentTimeMillis()
            )
            batch.set(notifRef, notifItem.toMap())

            batch.commit().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error sending pair request", e)
            Result.failure(e)
        }
    }

    override fun getMyWards(): Flow<List<Map<String, Any>>> = callbackFlow {
        val uid = authManager.currentUserId
        if (uid.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val registration = firestore.collection("pairs")
            .whereEqualTo("guardianUid", uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error listening to wards", error)
                    return@addSnapshotListener
                }
                val wards = snapshot?.documents?.mapNotNull { doc ->
                    @Suppress("UNCHECKED_CAST")
                    doc.data as? Map<String, Any>
                } ?: emptyList()
                trySend(wards)
            }

        awaitClose { registration.remove() }
    }

    override suspend fun unpair(wardUid: String, wardName: String): Result<Unit> {
        return try {
            val guardianUid = authManager.currentUserId
            if (guardianUid.isBlank()) return Result.failure(Exception("Not authenticated"))

            val pairId = "${wardUid}_${guardianUid}"
            val batch = firestore.batch()

            // Delete pair document
            batch.delete(firestore.collection("pairs").document(pairId))

            // Notify ward that guardian unpairing
            val wardNotifRef = firestore.collection("notifications")
                .document(wardUid)
                .collection("items")
                .document()
            // Reuse PAIR_REJECTED type as "unpairing" signal
            val notifItem = NotificationItem(
                id = wardNotifRef.id,
                type = NotificationType.PAIR_REJECTED,
                title = "Guardian Removed",
                body = "A guardian has removed themselves from monitoring you.",
                refId = pairId,
                fromUid = guardianUid,
                fromName = "",
                status = NotificationStatus.HANDLED,
                read = false,
                createdAt = System.currentTimeMillis()
            )
            batch.set(wardNotifRef, notifItem.toMap())

            batch.commit().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error unpairing", e)
            Result.failure(e)
        }
    }

    companion object {
        private const val TAG = "NotificationRepoImpl"
    }
}
