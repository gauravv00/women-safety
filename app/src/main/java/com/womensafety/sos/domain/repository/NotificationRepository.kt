package com.womensafety.sos.domain.repository

import com.womensafety.sos.data.model.NotificationItem
import kotlinx.coroutines.flow.Flow

interface NotificationRepository {
    fun getNotifications(): Flow<List<NotificationItem>>
    suspend fun markRead(notificationId: String)
    suspend fun markAllRead()
    suspend fun deleteNotification(notificationId: String)
    suspend fun handlePairRequest(notification: NotificationItem, accept: Boolean): Result<Unit>
    /** Guardian calls this to initiate pairing. Looks up [wardPairingCode] in Firestore users,
     *  checks for duplicate, writes a PAIR_REQUEST notification to the ward's inbox. */
    suspend fun sendPairRequest(wardPairingCode: String, guardianName: String): Result<Unit>
    /** Returns all active pairs where current user is the guardian. */
    fun getMyWards(): Flow<List<Map<String, Any>>>
    /** Removes the pair document for [wardUid] and sends an UNPAIR notification. */
    suspend fun unpair(wardUid: String, wardName: String): Result<Unit>
}
