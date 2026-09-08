package com.womensafety.sos.domain.repository

import com.womensafety.sos.data.entity.IncidentLog
import com.womensafety.sos.data.entity.TrustedContact
import kotlinx.coroutines.flow.Flow

interface SafetyRepository {
    fun getAllContacts(): Flow<List<TrustedContact>>
    suspend fun getContactsList(): List<TrustedContact>
    suspend fun addContact(contact: TrustedContact): Long
    suspend fun updateContact(contact: TrustedContact)
    suspend fun deleteContact(id: Long)
    suspend fun reorderContacts(contacts: List<TrustedContact>)

    fun getAllIncidents(): Flow<List<IncidentLog>>
    fun getActiveIncident(): Flow<IncidentLog?>
    suspend fun getActiveIncidentSync(): IncidentLog?
    suspend fun createIncident(lat: Double, lng: Double, triggerSource: String = "MANUAL_HOLD"): Long
    suspend fun resolveIncident(id: Long)
    suspend fun updateAudioPath(id: Long, path: String)
    suspend fun syncIncidentToCloud(incident: IncidentLog, currentLat: Double, currentLng: Double)
    suspend fun uploadAudioEvidence(incidentId: Long, localAudioPath: String)
    suspend fun sendEmergencySmsToContacts(lat: Double, lng: Double, incidentId: Long)
}
