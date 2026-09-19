package com.womensafety.sos.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.womensafety.sos.MainActivity
import com.womensafety.sos.data.entity.PairedWard
import com.womensafety.sos.di.ServiceLocator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class GuardianMonitorService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
    private val activeListeners = mutableMapOf<String, ValueEventListener>()

    companion object {
        private const val TAG = "GuardianMonitorService"
        const val ACTION_START_MONITORING = "ACTION_START_MONITORING"
        const val ACTION_STOP_MONITORING = "ACTION_STOP_MONITORING"
        private const val ONGOING_NOTIFICATION_ID = 8888
        private const val CHANNEL_MONITOR_ID = "guardian_monitor_service_channel"
        private const val CHANNEL_ALERT_ID = "guardian_danger_alerts_channel"

        private val _isMonitoring = MutableStateFlow(false)
        val isMonitoring: StateFlow<Boolean> = _isMonitoring.asStateFlow()

        fun start(context: Context) {
            val intent = Intent(context, GuardianMonitorService::class.java).apply {
                action = ACTION_START_MONITORING
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, GuardianMonitorService::class.java).apply {
                action = ACTION_STOP_MONITORING
            }
            context.startService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_MONITORING -> startMonitoring()
            ACTION_STOP_MONITORING -> stopMonitoring()
        }
        return START_STICKY
    }

    private fun startMonitoring() {
        _isMonitoring.value = true
        val notification = buildOngoingNotification()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            } else {
                0
            }
            if (type != 0) {
                startForeground(ONGOING_NOTIFICATION_ID, notification, type)
            } else {
                startForeground(ONGOING_NOTIFICATION_ID, notification)
            }
        } else {
            startForeground(ONGOING_NOTIFICATION_ID, notification)
        }

        observePairedWards()
    }

    private fun observePairedWards() {
        serviceScope.launch {
            try {
                ServiceLocator.repository.getAllPairedWards().collect { wards ->
                    syncFirebaseListeners(wards)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error observing paired wards: ${e.localizedMessage}")
            }
        }
    }

    private fun syncFirebaseListeners(wards: List<PairedWard>) {
        val currentCodes = wards.map { it.wardPairingCode }.toSet()

        // Remove listeners no longer paired
        val removedCodes = activeListeners.keys - currentCodes
        for (code in removedCodes) {
            val listener = activeListeners.remove(code)
            if (listener != null) {
                FirebaseDatabase.getInstance().getReference("guardians").child(code)
                    .removeEventListener(listener)
            }
        }

        // Add listeners for new paired wards
        for (ward in wards) {
            if (!activeListeners.containsKey(ward.wardPairingCode)) {
                attachWardListener(ward)
            }
        }
    }

    private fun attachWardListener(ward: PairedWard) {
        val ref = FirebaseDatabase.getInstance().getReference("guardians").child(ward.wardPairingCode)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) return

                val status = snapshot.child("status").getValue(String::class.java) ?: "SAFE"
                val wardName = snapshot.child("wardName").getValue(String::class.java) ?: ward.wardName
                val lat = snapshot.child("latitude").getValue(Double::class.java) ?: 0.0
                val lng = snapshot.child("longitude").getValue(Double::class.java) ?: 0.0
                val timestamp = snapshot.child("timestamp").getValue(Long::class.java) ?: System.currentTimeMillis()
                val incidentId = snapshot.child("incidentId").getValue(Long::class.java)

                serviceScope.launch {
                    ServiceLocator.repository.updateWardStatus(
                        code = ward.wardPairingCode,
                        status = status,
                        lat = lat,
                        lng = lng,
                        timestamp = timestamp,
                        incidentId = incidentId
                    )
                }

                if (status.equals("ACTIVE", ignoreCase = true)) {
                    triggerDangerAlertNotification(ward.wardPairingCode, wardName, lat, lng)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w(TAG, "Ward listener cancelled for ${ward.wardPairingCode}: ${error.message}")
            }
        }

        ref.addValueEventListener(listener)
        activeListeners[ward.wardPairingCode] = listener
    }

    private fun triggerDangerAlertNotification(code: String, wardName: String, lat: Double, lng: Double) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("EXTRA_WARD_CODE", code)
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            code.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val notification = NotificationCompat.Builder(this, CHANNEL_ALERT_ID)
            .setContentTitle("🚨 EMERGENCY ALERT: $wardName IS IN DANGER!")
            .setContentText("Emergency SOS triggered! Live location: $lat, $lng. Tap to open map.")
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setSound(soundUri)
            .setVibrate(longArrayOf(0, 500, 200, 500, 200, 500))
            .build()

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(code.hashCode(), notification)
    }

    private fun stopMonitoring() {
        _isMonitoring.value = false
        // Remove all listeners
        for ((code, listener) in activeListeners) {
            FirebaseDatabase.getInstance().getReference("guardians").child(code)
                .removeEventListener(listener)
        }
        activeListeners.clear()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Monitor Channel (Low importance ongoing)
            val monitorChannel = NotificationChannel(
                CHANNEL_MONITOR_ID,
                "Guardian Protection Monitoring",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows persistent status while Guardian Protection actively monitors paired wards"
                setSound(null, null)
            }
            manager.createNotificationChannel(monitorChannel)

            // Alert Channel (High importance emergency)
            val alertSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_ALARM)
                .build()

            val alertChannel = NotificationChannel(
                CHANNEL_ALERT_ID,
                "Guardian Emergency Danger Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "High priority danger alerts when a paired ward triggers Emergency SOS"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 200, 500, 200, 500)
                setSound(alertSoundUri, audioAttributes)
            }
            manager.createNotificationChannel(alertChannel)
        }
    }

    private fun buildOngoingNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_MONITOR_ID)
            .setContentTitle("Guardian Mode Active")
            .setContentText("Monitoring safety of your paired friends and family")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    override fun onDestroy() {
        stopMonitoring()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
