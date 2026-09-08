package com.womensafety.sos.data.repository

import android.content.Context
import android.net.Uri
import android.telephony.SmsManager
import android.util.Log
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.womensafety.sos.data.entity.IncidentLog
import com.womensafety.sos.data.entity.TrustedContact
import com.womensafety.sos.data.local.dao.IncidentLogDao
import com.womensafety.sos.data.local.dao.TrustedContactDao
import com.womensafety.sos.domain.repository.SafetyRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File

class SafetyRepositoryImpl(
    private val context: Context,
    private val contactDao: TrustedContactDao,
    private val incidentDao: IncidentLogDao
) : SafetyRepository {

    private val TAG = "SafetyRepositoryImpl"

    override fun getAllContacts(): Flow<List<TrustedContact>> = contactDao.getAllContacts()

    override suspend fun getContactsList(): List<TrustedContact> = withContext(Dispatchers.IO) {
        contactDao.getAllContactsList()
    }

    override suspend fun addContact(contact: TrustedContact): Long = withContext(Dispatchers.IO) {
        contactDao.insertContact(contact)
    }

    override suspend fun updateContact(contact: TrustedContact) = withContext(Dispatchers.IO) {
        contactDao.updateContact(contact)
    }

    override suspend fun deleteContact(id: Long) = withContext(Dispatchers.IO) {
        contactDao.deleteById(id)
    }

    override suspend fun reorderContacts(contacts: List<TrustedContact>) = withContext(Dispatchers.IO) {
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

    override suspend fun resolveIncident(id: Long) = withContext(Dispatchers.IO) {
        incidentDao.resolveIncident(id)
        try {
            val db = FirebaseDatabase.getInstance().getReference("incidents").child(id.toString())
            db.child("status").setValue("RESOLVED")
            db.child("resolvedAt").setValue(System.currentTimeMillis())
        } catch (e: Exception) {
            Log.e(TAG, "Firebase status update error: ${e.localizedMessage}")
        }
    }

    override suspend fun updateAudioPath(id: Long, path: String) = withContext(Dispatchers.IO) {
        val existing = incidentDao.getActiveIncidentSync()
        if (existing != null && existing.id == id) {
            incidentDao.updateIncident(existing.copy(audioFilePath = path))
        }
    }

    override suspend fun syncIncidentToCloud(incident: IncidentLog, currentLat: Double, currentLng: Double) = withContext(Dispatchers.IO) {
        try {
            val ref = FirebaseDatabase.getInstance().getReference("incidents").child(incident.id.toString())
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

    override suspend fun uploadAudioEvidence(incidentId: Long, localAudioPath: String) = withContext(Dispatchers.IO) {
        val file = File(localAudioPath)
        if (!file.exists()) return@withContext

        try {
            val storageRef = FirebaseStorage.getInstance().reference.child("audio_evidence/incident_${incidentId}_${file.name}")
            val uploadTask = storageRef.putFile(Uri.fromFile(file)).await()
            val downloadUrl = storageRef.downloadUrl.await().toString()
            incidentDao.updateAudioCloudUrl(incidentId, downloadUrl)

            val db = FirebaseDatabase.getInstance().getReference("incidents").child(incidentId.toString())
            db.child("audioCloudUrl").setValue(downloadUrl)
        } catch (e: Exception) {
            Log.e(TAG, "Failed uploading audio evidence to cloud: ${e.localizedMessage}")
        }
    }

    override suspend fun sendEmergencySmsToContacts(lat: Double, lng: Double, incidentId: Long) = withContext(Dispatchers.IO) {
        val contacts = contactDao.getAllContactsList()
        val mapsUrl = "https://maps.google.com/?q=$lat,$lng"
        val message = "EMERGENCY SOS ALERT! I need help. My live location: $mapsUrl (Coordinates: $lat, $lng). Track me immediately!"

        val smsManager = try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                context.getSystemService(SmsManager::class.java)
            } else {
                @Suppress("DEPRECATION")
                SmsManager.getDefault()
            }
        } catch (e: Exception) {
            Log.e(TAG, "SmsManager error: ${e.localizedMessage}")
            null
        }

        contacts.forEach { contact ->
            try {
                smsManager?.sendTextMessage(contact.phoneNumber, null, message, null, null)
                Log.d(TAG, "Emergency SMS dispatched to ${contact.phoneNumber}")
            } catch (e: Exception) {
                Log.e(TAG, "Failed sending SMS to ${contact.phoneNumber}: ${e.localizedMessage}")
            }
        }
    }
}
