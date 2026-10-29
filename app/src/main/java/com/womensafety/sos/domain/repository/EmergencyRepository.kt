package com.womensafety.sos.domain.repository

import com.womensafety.sos.data.model.EmergencyEvent
import kotlinx.coroutines.flow.Flow

interface EmergencyRepository {
    /** Ward: creates emergency doc, fan-outs EMERGENCY notification to all paired guardians. Returns emergencyId. */
    suspend fun startEmergency(wardName: String): Result<String>
    /** Ward: updates location in emergencies/{id} every 5s. */
    suspend fun updateLocation(emergencyId: String, lat: Double, lng: Double, accuracy: Float): Result<Unit>
    /** Ward: updates audio recording URL in emergencies/{id}. */
    suspend fun updateEmergencyAudio(emergencyId: String, audioUrl: String): Result<Unit> = Result.success(Unit)
    /** Ward: marks emergency as resolved, notifies guardians. */
    suspend fun resolveEmergency(emergencyId: String): Result<Unit>
    /** Guardian: listens to a single emergency doc. */
    fun observeEmergency(emergencyId: String): Flow<EmergencyEvent?>
    /** Guardian: listens to all ACTIVE emergencies for wards this user is monitoring. */
    fun observeActiveEmergencies(): Flow<List<EmergencyEvent>>
}
