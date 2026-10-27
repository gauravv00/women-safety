package com.womensafety.sos.service

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.womensafety.sos.data.firebase.FirebaseAuthManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AppFirebaseMessagingService : FirebaseMessagingService() {

    private val scope = CoroutineScope(Dispatchers.IO)

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "New FCM Token received: $token")
        scope.launch {
            FirebaseAuthManager.getInstance(applicationContext).updateFcmToken(token)
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d(TAG, "FCM Message received from: ${remoteMessage.from}")

        val data = remoteMessage.data
        val notification = remoteMessage.notification

        val title = data["title"] ?: notification?.title ?: "Safety Alert"
        val body = data["body"] ?: notification?.body ?: "New notification received"
        val type = data["type"] ?: "GENERIC"
        val refId = data["refId"] ?: data["emergencyId"] ?: ""

        val notificationId = if (refId.isNotBlank()) refId.hashCode() else (System.currentTimeMillis() % 100000).toInt()

        when (type) {
            "EMERGENCY" -> {
                NotificationHelper.showEmergencyNotification(
                    context = applicationContext,
                    notificationId = notificationId,
                    title = title,
                    body = body,
                    emergencyId = refId
                )
            }
            "RESOLVED" -> {
                // Instantly dismiss any ringing emergency alarm notification
                NotificationHelper.cancelEmergencyNotification(applicationContext, refId)
                NotificationHelper.showPairingNotification(
                    context = applicationContext,
                    notificationId = ("resolved_$refId").hashCode(),
                    title = title,
                    body = body
                )
            }
            "PAIR_REQUEST", "PAIR_ACCEPTED", "PAIR_REJECTED" -> {
                NotificationHelper.showPairingNotification(
                    context = applicationContext,
                    notificationId = notificationId,
                    title = title,
                    body = body
                )
            }
            else -> {
                NotificationHelper.showPairingNotification(
                    context = applicationContext,
                    notificationId = notificationId,
                    title = title,
                    body = body
                )
            }
        }
    }

    companion object {
        private const val TAG = "AppFirebaseMessaging"
    }
}
