package com.womensafety.sos.service

import android.annotation.SuppressLint
import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.telecom.TelecomManager
import android.telephony.TelephonyManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.location.Location
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.womensafety.sos.MainActivity
import com.womensafety.sos.R
import com.womensafety.sos.di.ServiceLocator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class EmergencyForegroundService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private var audioRecorder: AudioRecorderService? = null
    private var mediaPlayer: MediaPlayer? = null

    private var currentIncidentId: Long = -1
    private var autoCallJob: Job? = null

    companion object {
        const val ACTION_START_SOS = "ACTION_START_SOS"
        const val ACTION_STOP_SOS = "ACTION_STOP_SOS"
        const val EXTRA_INCIDENT_ID = "EXTRA_INCIDENT_ID"
        const val NOTIFICATION_ID = 9999
        const val CHANNEL_ID = "sos_service_channel"

        private val _currentLocation = MutableStateFlow<Location?>(null)
        val currentLocation: StateFlow<Location?> = _currentLocation.asStateFlow()

        private val _locationHistory = MutableStateFlow<List<Pair<Double, Double>>>(emptyList())
        val locationHistory: StateFlow<List<Pair<Double, Double>>> = _locationHistory.asStateFlow()

        private val _isServiceRunning = MutableStateFlow(false)
        val isServiceRunning: StateFlow<Boolean> = _isServiceRunning.asStateFlow()
    }

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        audioRecorder = AudioRecorderService(this)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_SOS -> {
                currentIncidentId = intent.getLongExtra(EXTRA_INCIDENT_ID, -1)
                startEmergencyMode()
            }
            ACTION_STOP_SOS -> {
                stopEmergencyMode()
            }
        }
        return START_STICKY
    }

    private fun startEmergencyMode() {
        _isServiceRunning.value = true
        val notification = buildNotification("EMERGENCY SOS ACTIVE - Live tracking & broadcasting location")
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION or ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            } else {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            }
            startForeground(NOTIFICATION_ID, notification, type)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        startLocationTracking()
        startAudioRecording()
        playAlarmIfConfigured()
        scheduleAutoCall()
    }

    @SuppressLint("MissingPermission")
    private fun startLocationTracking() {
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000L)
            .setMinUpdateIntervalMillis(3000L)
            .build()

        var lastSyncedLocation: Location? = null
        var lastSyncTimestampMs: Long = 0L

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                for (location in locationResult.locations) {
                    _currentLocation.value = location
                    val newHistory = (_locationHistory.value + Pair(location.latitude, location.longitude)).takeLast(500)
                    _locationHistory.value = newHistory

                    // Sync to Cloud with throttling: only if moved >= 10m or >= 10s since last write
                    val now = System.currentTimeMillis()
                    val dist = lastSyncedLocation?.distanceTo(location) ?: Float.MAX_VALUE
                    val timeSinceSync = now - lastSyncTimestampMs

                    if (dist >= 10f || timeSinceSync >= 10_000L) {
                        lastSyncedLocation = location
                        lastSyncTimestampMs = now

                        serviceScope.launch {
                            try {
                                val settings = ServiceLocator.userPreferences.settings.value
                                ServiceLocator.repository.broadcastSosToGuardians(
                                    myPairingCode = settings.myPairingCode,
                                    myName = settings.displayName,
                                    incidentId = currentIncidentId,
                                    lat = location.latitude,
                                    lng = location.longitude,
                                    status = "ACTIVE"
                                )
                            } catch (e: Exception) {
                                Log.e("EmergencyService", "Guardian location broadcast error: ${e.localizedMessage}")
                            }

                            try {
                                if (currentIncidentId != -1L) {
                                    val incident = ServiceLocator.repository.getActiveIncidentSync()
                                    if (incident != null) {
                                        ServiceLocator.repository.syncIncidentToCloud(
                                            incident = incident,
                                            currentLat = location.latitude,
                                            currentLng = location.longitude
                                        )
                                    }
                                }
                            } catch (e: Exception) {
                                Log.e("EmergencyService", "Location sync error: ${e.localizedMessage}")
                            }
                        }
                    }
                }
            }
        }

        try {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
        } catch (e: Exception) {
            Log.e("EmergencyService", "Location update error: ${e.localizedMessage}")
        }
    }

    private fun startAudioRecording() {
        val audioPath = audioRecorder?.startRecording()
        if (audioPath != null && currentIncidentId != -1L) {
            serviceScope.launch {
                ServiceLocator.repository.updateAudioPath(currentIncidentId, audioPath)
            }
        }
    }

    private fun playAlarmIfConfigured() {
        val settings = ServiceLocator.userPreferences.settings.value
        if (settings.loudSirenEnabled) {
            try {
                val alarmUri: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                mediaPlayer = MediaPlayer().apply {
                    setDataSource(applicationContext, alarmUri)
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    isLooping = true
                    prepare()
                    start()
                }
            } catch (e: Exception) {
                Log.e("EmergencyService", "Failed playing alarm sound: ${e.localizedMessage}")
            }
        }
    }

    private fun isPhoneInCall(): Boolean {
        try {
            val telecomManager = getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
            if (checkSelfPermission(Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
                if (telecomManager?.isInCall == true) return true
            }
        } catch (e: Exception) {
            // Silently fallback
        }

        try {
            val telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
            val state = telephonyManager?.callState ?: TelephonyManager.CALL_STATE_IDLE
            return state != TelephonyManager.CALL_STATE_IDLE
        } catch (e: Exception) {
            return false
        }
    }

    private fun scheduleAutoCall() {
        val settings = ServiceLocator.userPreferences.settings.value
        if (!settings.autoCallTopContact && !settings.sequentialEscalationEnabled) {
            Log.d("EmergencyService", "Both auto-call and sequential escalation are disabled in settings.")
            return
        }

        autoCallJob?.cancel()
        autoCallJob = serviceScope.launch {
            val initialDelaySeconds = if (settings.autoCallTopContact) {
                settings.autoCallDelaySeconds.coerceAtLeast(3)
            } else {
                5
            }
            Log.d("EmergencyService", "Starting auto-call / sequential escalation countdown (${initialDelaySeconds}s)")

            for (sec in 0 until initialDelaySeconds) {
                if (!_isServiceRunning.value) return@launch
                delay(1000L)
            }

            val contacts = ServiceLocator.repository.getContactsList()
            if (contacts.isEmpty()) {
                Log.w("EmergencyService", "Sequential escalation: No trusted contacts found.")
                return@launch
            }

            if (!settings.sequentialEscalationEnabled) {
                val topContact = contacts.first()
                if (_isServiceRunning.value) {
                    val cleanPhone = com.womensafety.sos.data.util.PhoneUtils.sanitizePhoneNumber(topContact.phoneNumber)
                    Log.i("EmergencyService", "Auto-calling top contact: ${topContact.name} ($cleanPhone)")
                    updateNotification("Calling Priority #1: ${topContact.name}")
                    makeEmergencyPhoneCall(cleanPhone)
                }
                return@launch
            }

            // Sequential Escalation: Dial contacts one by one in priority order
            for ((index, contact) in contacts.withIndex()) {
                if (!_isServiceRunning.value) {
                    Log.d("EmergencyService", "Emergency cancelled, stopping sequential escalation.")
                    break
                }

                val cleanPhone = com.womensafety.sos.data.util.PhoneUtils.sanitizePhoneNumber(contact.phoneNumber)
                if (cleanPhone.isBlank()) continue

                Log.i(
                    "EmergencyService",
                    "Sequential Escalation [${index + 1}/${contacts.size}]: Dialing Priority #${contact.priorityRank} ${contact.name} ($cleanPhone)"
                )
                updateNotification("Escalating [${index + 1}/${contacts.size}]: Dialing ${contact.name} (#${contact.priorityRank})")
                makeEmergencyPhoneCall(cleanPhone)

                if (index < contacts.size - 1) {
                    val nextContact = contacts[index + 1]

                    // 1. Give 5 seconds for telecom stack to connect and initiate the outgoing call
                    var elapsedSec = 0
                    while (_isServiceRunning.value && elapsedSec < 5) {
                        delay(1000L)
                        elapsedSec++
                    }

                    // 2. Wait for the call to finish / disconnect (up to 35 seconds max carrier ring timeout)
                    val maxWaitSec = 35
                    var waitingCallSec = 5
                    while (_isServiceRunning.value && waitingCallSec < maxWaitSec) {
                        val inCall = isPhoneInCall()
                        if (!inCall && waitingCallSec >= 8) {
                            Log.i("EmergencyService", "Call to ${contact.name} disconnected at ${waitingCallSec}s. Entering cooldown buffer.")
                            break
                        }
                        updateNotification("Calling ${contact.name} (#${contact.priorityRank}) [${waitingCallSec}s] • Next: ${nextContact.name}")
                        delay(1000L)
                        waitingCallSec++
                    }

                    if (!_isServiceRunning.value) break

                    // 3. ESSENTIAL 3.5s COOLDOWN BUFFER:
                    // After previous call disconnects, Android Telecom and cellular radio require 2-3s
                    // to completely clear the audio/modem channel from DISCONNECTING to IDLE.
                    // This prevents the carrier/dialer "Wait till I try to connect this call" error!
                    Log.i("EmergencyService", "Waiting 3.5s cooldown buffer for telecom modem to clear before calling ${nextContact.name}...")
                    for (c in 3 downTo 1) {
                        if (!_isServiceRunning.value) break
                        updateNotification("Next in ${c}s: Preparing to dial ${nextContact.name} (#${nextContact.priorityRank})...")
                        delay(1200L)
                    }

                    // 4. Verify line is truly idle before firing next call
                    var idleWaitCount = 0
                    while (_isServiceRunning.value && isPhoneInCall() && idleWaitCount < 4) {
                        Log.w("EmergencyService", "Modem line still settling, waiting additional 1s before dialing...")
                        updateNotification("Waiting for line to clear before calling ${nextContact.name}...")
                        delay(1000L)
                        idleWaitCount++
                    }
                }
            }

            if (_isServiceRunning.value) {
                updateNotification("Sequential escalation finished (${contacts.size} contacts dialed).")
                Log.i("EmergencyService", "Sequential escalation completed for all ${contacts.size} contacts.")
            }
        }
    }

    private fun updateNotification(contentText: String) {
        try {
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            manager?.notify(NOTIFICATION_ID, buildNotification(contentText))
        } catch (e: Exception) {
            Log.e("EmergencyService", "Failed to update notification: ${e.localizedMessage}")
        }
    }

    private fun makeEmergencyPhoneCall(phoneNumber: String) {
        val cleanPhone = com.womensafety.sos.data.util.PhoneUtils.sanitizePhoneNumber(phoneNumber)
        if (cleanPhone.isBlank()) return

        try {
            // 1. Direct TelecomManager placeCall (bypasses background activity start restrictions)
            val telecomManager = getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
            if (checkSelfPermission(Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED && telecomManager != null) {
                val uri = Uri.fromParts("tel", cleanPhone, null)
                val extras = Bundle().apply {
                    putBoolean(TelecomManager.EXTRA_START_CALL_WITH_SPEAKERPHONE, true)
                }
                telecomManager.placeCall(uri, extras)
                Log.i("EmergencyService", "Successfully placed call via TelecomManager to $cleanPhone")
                return
            }
        } catch (e: Exception) {
            Log.w("EmergencyService", "TelecomManager.placeCall failed, trying ACTION_CALL: ${e.localizedMessage}")
        }

        try {
            val callIntent = Intent(Intent.ACTION_CALL).apply {
                data = Uri.parse("tel:$cleanPhone")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            startActivity(callIntent)
        } catch (e: Exception) {
            Log.e("EmergencyService", "ACTION_CALL failed, falling back to DIAL: ${e.localizedMessage}")
            val dialIntent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:$cleanPhone")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            startActivity(dialIntent)
        }
    }

    private fun stopEmergencyMode() {
        _isServiceRunning.value = false
        autoCallJob?.cancel()

        try {
            if (::locationCallback.isInitialized) {
                fusedLocationClient.removeLocationUpdates(locationCallback)
            }
        } catch (e: Exception) {
            Log.e("EmergencyService", "Remove location updates error: ${e.localizedMessage}")
        }

        val audioPath = audioRecorder?.stopRecording()

        serviceScope.launch {
            try {
                if (audioPath != null && currentIncidentId != -1L) {
                    ServiceLocator.repository.uploadAudioEvidence(currentIncidentId, audioPath)
                }
                val settings = ServiceLocator.userPreferences.settings.value
                val loc = _currentLocation.value
                ServiceLocator.repository.broadcastSosToGuardians(
                    myPairingCode = settings.myPairingCode,
                    myName = settings.displayName,
                    incidentId = currentIncidentId,
                    lat = loc?.latitude ?: 0.0,
                    lng = loc?.longitude ?: 0.0,
                    status = "RESOLVED"
                )
            } catch (e: Exception) {
                Log.e("EmergencyService", "Error during stop broadcast: ${e.localizedMessage}")
            }
        }

        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
        } catch (e: Exception) {
            Log.e("EmergencyService", "Error stopping alarm: ${e.localizedMessage}")
        }

        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Emergency SOS Service Channel",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Shows persistent alert while Emergency SOS is actively broadcasting location"
                setSound(null, null)
                enableVibration(true)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(contentText: String): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("EMERGENCY SOS ACTIVE")
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
