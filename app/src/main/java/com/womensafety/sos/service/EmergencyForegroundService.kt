package com.womensafety.sos.service

import android.annotation.SuppressLint
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

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                for (location in locationResult.locations) {
                    _currentLocation.value = location
                    val newHistory = _locationHistory.value + Pair(location.latitude, location.longitude)
                    _locationHistory.value = newHistory

                    // Sync to Cloud
                    serviceScope.launch {
                        try {
                            if (currentIncidentId != -1L) {
                                ServiceLocator.repository.syncIncidentToCloud(
                                    incident = ServiceLocator.repository.getActiveIncidentSync()
                                        ?: return@launch,
                                    currentLat = location.latitude,
                                    currentLng = location.longitude
                                )
                            }
                        } catch (e: Exception) {
                            Log.e("EmergencyService", "Location sync error: ${e.localizedMessage}")
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

    private fun scheduleAutoCall() {
        val settings = ServiceLocator.userPreferences.settings.value
        if (!settings.autoCallTopContact) return

        autoCallJob = serviceScope.launch {
            delay(settings.autoCallDelaySeconds * 1000L)
            val contacts = ServiceLocator.repository.getContactsList()
            if (contacts.isNotEmpty()) {
                val topContact = contacts.first()
                makeEmergencyPhoneCall(topContact.phoneNumber)
            }
        }
    }

    private fun makeEmergencyPhoneCall(phoneNumber: String) {
        try {
            val callIntent = Intent(Intent.ACTION_CALL).apply {
                data = Uri.parse("tel:$phoneNumber")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(callIntent)
        } catch (e: Exception) {
            Log.e("EmergencyService", "Call permission absent or call failed, falling back to dialer: ${e.localizedMessage}")
            val dialIntent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:$phoneNumber")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(dialIntent)
        }
    }

    private fun stopEmergencyMode() {
        _isServiceRunning.value = false
        autoCallJob?.cancel()

        try {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        } catch (e: Exception) {
            Log.e("EmergencyService", "Remove location updates error: ${e.localizedMessage}")
        }

        val audioPath = audioRecorder?.stopRecording()
        if (audioPath != null && currentIncidentId != -1L) {
            serviceScope.launch {
                ServiceLocator.repository.uploadAudioEvidence(currentIncidentId, audioPath)
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
