package com.womensafety.sos.data.repository

import android.content.Context
import android.net.Uri
import android.telephony.SmsManager
import android.util.Log
import com.google.firebase.database.FirebaseDatabase
import com.womensafety.sos.data.firebase.FirebaseDatabaseHelper
import com.google.firebase.storage.FirebaseStorage
import com.womensafety.sos.data.entity.IncidentLog
import com.womensafety.sos.data.entity.PairedWard
import com.womensafety.sos.data.entity.TrustedContact
import com.womensafety.sos.data.local.dao.IncidentLogDao
import com.womensafety.sos.data.local.dao.PairedWardDao
import com.womensafety.sos.data.local.dao.TrustedContactDao
import com.womensafety.sos.data.util.PhoneUtils
import com.womensafety.sos.domain.repository.SafetyRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File

class SafetyRepositoryImpl(
    private val context: Context,
    private val contactDao: TrustedContactDao,
    private val incidentDao: IncidentLogDao,
    private val pairedWardDao: PairedWardDao
) : SafetyRepository {

    private val TAG = "SafetyRepositoryImpl"

    override fun getAllContacts(): Flow<List<TrustedContact>> = contactDao.getAllContacts()

    override suspend fun getContactsList(): List<TrustedContact> = withContext(Dispatchers.IO) {
        contactDao.getAllContactsList()
    }

    override suspend fun addContact(contact: TrustedContact): Long = withContext(Dispatchers.IO) {
        contactDao.insertContact(contact)
    }

    override suspend fun updateContact(contact: TrustedContact): Unit = withContext(Dispatchers.IO) {
        contactDao.updateContact(contact)
    }

    override suspend fun deleteContact(id: Long): Unit = withContext(Dispatchers.IO) {
        contactDao.deleteById(id)
    }

    override suspend fun reorderContacts(contacts: List<TrustedContact>): Unit = withContext(Dispatchers.IO) {
        contacts.forEachIndexed { index, contact ->
            contactDao.updateContact(contact.copy(priorityRank = index + 1))
        }
    }

    override fun getAllIncidents(): Flow<List<IncidentLog>> = incidentDao.getAllIncidents()

    override fun getActiveIncident(): Flow<IncidentLog?> = incidentDao.getActiveIncident()

    override suspend fun getActiveIncidentSync(): IncidentLog? = withContext(Dispatchers.IO) {
        incidentDao.getActiveIncidentSync()
    }

    override suspend fun createIncident(lat: Double, lng: Double, triggerSource: String): Long = withContext(Dispatchers.IO) {
        val incident = IncidentLog(
            status = "ACTIVE",
            latitude = lat,
            longitude = lng,
            triggerSource = triggerSource
        )
        val id = incidentDao.insertIncident(incident)
        try {
            syncIncidentToCloud(incident.copy(id = id), lat, lng)
        } catch (e: Exception) {
            Log.e(TAG, "Firebase sync failed (offline fallback active): ${e.localizedMessage}")
        }
        id
    }

    override suspend fun resolveIncident(id: Long): Unit = withContext(Dispatchers.IO) {
        incidentDao.resolveIncident(id)
        try {
            val db = FirebaseDatabaseHelper.getInstance().getReference("incidents").child(id.toString())
            db.child("status").setValue("RESOLVED")
            db.child("resolvedAt").setValue(System.currentTimeMillis())
        } catch (e: Exception) {
            Log.e(TAG, "Firebase status update error: ${e.localizedMessage}")
        }
    }

    override suspend fun updateAudioPath(id: Long, path: String): Unit = withContext(Dispatchers.IO) {
        val existing = incidentDao.getActiveIncidentSync()
        if (existing != null && existing.id == id) {
            incidentDao.updateIncident(existing.copy(audioFilePath = path))
        }
    }

    override suspend fun syncIncidentToCloud(incident: IncidentLog, currentLat: Double, currentLng: Double): Unit = withContext(Dispatchers.IO) {
        try {
            val ref = FirebaseDatabaseHelper.getInstance().getReference("incidents").child(incident.id.toString())
            val map = mapOf(
                "incidentId" to incident.id,
                "timestamp" to incident.timestamp,
                "status" to incident.status,
                "latitude" to currentLat,
                "longitude" to currentLng,
                "triggerSource" to incident.triggerSource,
                "trackingUrl" to "https://maps.google.com/?q=$currentLat,$currentLng"
            )
            ref.updateChildren(map)
        } catch (e: Exception) {
            Log.w(TAG, "Firebase unavailable or offline: ${e.message}")
        }
    }

    override suspend fun uploadAudioEvidence(incidentId: Long, localAudioPath: String): Unit = withContext(Dispatchers.IO) {
        val file = File(localAudioPath)
        if (!file.exists()) return@withContext

        try {
            val storageRef = FirebaseStorage.getInstance("gs://wosafety-17b7e.firebasestorage.app").reference.child("audio_evidence/incident_${incidentId}_${file.name}")
            storageRef.putFile(Uri.fromFile(file)).await()
            val downloadUrl = storageRef.downloadUrl.await().toString()
            incidentDao.updateAudioCloudUrl(incidentId, downloadUrl)

            val db = FirebaseDatabaseHelper.getInstance().getReference("incidents").child(incidentId.toString())
            db.child("audioCloudUrl").setValue(downloadUrl)

            // Sync audio URL to active Firestore emergency doc for live guardian playback
            val activeSessionId = com.womensafety.sos.service.EmergencyForegroundService.activeEmergencySessionId
            if (activeSessionId.isNotBlank()) {
                try {
                    com.google.firebase.firestore.FirebaseFirestore.getInstance()
                        .collection("emergencies")
                        .document(activeSessionId)
                        .update("audioUrl", downloadUrl)
                        .await()
                    Log.d(TAG, "Audio evidence synced to Firestore emergencies/$activeSessionId")
                } catch (e: Exception) {
                    Log.w(TAG, "Failed syncing audio to Firestore emergencies: ${e.message}")
                }
            }

            // Sync audio URL to RTDB guardians child
            try {
                val sharedPrefs = context.getSharedPreferences("women_safety_prefs", Context.MODE_PRIVATE)
                val myCode = sharedPrefs.getString("my_pairing_code", "") ?: ""
                if (myCode.isNotBlank()) {
                    FirebaseDatabaseHelper.getInstance()
                        .getReference("guardians")
                        .child(myCode)
                        .child("audioCloudUrl")
                        .setValue(downloadUrl)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed syncing audio to RTDB guardians: ${e.message}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed uploading audio evidence to cloud: ${e.localizedMessage}")
        }
    }

    override suspend fun sendEmergencySmsToContacts(lat: Double, lng: Double, incidentId: Long): Unit = withContext(Dispatchers.IO) {
        val contacts = contactDao.getAllContactsList()
        if (contacts.isEmpty()) {
            Log.w(TAG, "No trusted contacts found to send emergency SMS")
            return@withContext
        }

        // Check SEND_SMS runtime permission before attempting dispatch
        val hasSmsPermission = androidx.core.content.ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.SEND_SMS
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

        if (!hasSmsPermission) {
            Log.e(TAG, "Cannot send emergency SMS: SEND_SMS permission is not granted")
            return@withContext
        }

        val mapsUrl = if (lat != 0.0 && lng != 0.0) {
            "https://maps.google.com/?q=$lat,$lng"
        } else {
            "Location unavailable"
        }

        val message = if (lat != 0.0 && lng != 0.0) {
            "EMERGENCY SOS! I need help. My live location: $mapsUrl"
        } else {
            "EMERGENCY SOS! I am in danger and need immediate help. Please reach out to me immediately!"
        }

        // Robust SmsManager retrieval across all Android versions
        val smsManager: SmsManager? = try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                context.getSystemService(SmsManager::class.java)
            } else {
                @Suppress("DEPRECATION")
                SmsManager.getDefault()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed retrieving SmsManager: ${e.localizedMessage}")
            try {
                @Suppress("DEPRECATION")
                SmsManager.getDefault()
            } catch (fallbackEx: Exception) {
                null
            }
        }

        if (smsManager == null) {
            Log.e(TAG, "SmsManager is completely null on this device. Cannot send emergency SMS.")
            return@withContext
        }

        Log.i(TAG, "Broadcasting emergency SMS to all ${contacts.size} trusted contacts. Location: ($lat, $lng)")

        contacts.forEach { contact ->
            val cleanPhone = PhoneUtils.sanitizePhoneNumber(contact.phoneNumber)
            if (cleanPhone.isNotBlank()) {
                try {
                    // Use divideMessage + sendMultipartTextMessage to guarantee delivery even if carriers split messages
                    val parts = smsManager.divideMessage(message)
                    if (parts.size > 1) {
                        smsManager.sendMultipartTextMessage(cleanPhone, null, parts, null, null)
                    } else {
                        smsManager.sendTextMessage(cleanPhone, null, message, null, null)
                    }
                    Log.i(TAG, "Emergency SMS with location successfully dispatched to ${contact.name} ($cleanPhone)")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed sending emergency SMS to ${contact.name} ($cleanPhone): ${e.localizedMessage}", e)
                }
            } else {
                Log.w(TAG, "Skipping empty/invalid phone number for contact ${contact.name}")
            }
        }
    }

    // Guardian Pairing Implementations
    override fun getAllPairedWards(): Flow<List<PairedWard>> = pairedWardDao.getAllPairedWards()

    override suspend fun getAllPairedWardsSync(): List<PairedWard> = withContext(Dispatchers.IO) {
        pairedWardDao.getAllPairedWardsSync()
    }

    override suspend fun addPairedWard(wardPairingCode: String, wardName: String, wardUid: String): Long = withContext(Dispatchers.IO) {
        val cleanCode = wardPairingCode.trim().uppercase()
        val ward = PairedWard(
            wardPairingCode = cleanCode,
            wardName = wardName.trim(),
            wardUid = wardUid,
            pairedAt = System.currentTimeMillis()
        )
        pairedWardDao.insertWard(ward)
    }

    override suspend fun deletePairedWard(id: Long): Unit = withContext(Dispatchers.IO) {
        pairedWardDao.deleteById(id)
    }

    override suspend fun updateWardStatus(
        code: String,
        status: String,
        lat: Double,
        lng: Double,
        timestamp: Long,
        incidentId: Long?
    ): Unit = withContext(Dispatchers.IO) {
        pairedWardDao.updateWardStatus(code, status, lat, lng, timestamp, incidentId)
    }

    override suspend fun broadcastSosToGuardians(
        myPairingCode: String,
        myName: String,
        incidentId: Long,
        lat: Double,
        lng: Double,
        status: String
    ): Unit = withContext(Dispatchers.IO) {
        if (myPairingCode.isBlank()) return@withContext
        try {
            val ref = FirebaseDatabaseHelper.getInstance().getReference("guardians").child(myPairingCode)
            val data = mapOf(
                "pairingCode" to myPairingCode,
                "wardName" to myName,
                "status" to status,
                "incidentId" to incidentId,
                "latitude" to lat,
                "longitude" to lng,
                "timestamp" to System.currentTimeMillis(),
                "trackingUrl" to "https://maps.google.com/?q=$lat,$lng"
            )
            ref.setValue(data)
            Log.d(TAG, "Broadcasted SOS status $status for pairing code $myPairingCode to RTDB")
        } catch (e: Exception) {
            Log.e(TAG, "Failed broadcasting SOS to guardians: ${e.localizedMessage}")
        }
    }
}