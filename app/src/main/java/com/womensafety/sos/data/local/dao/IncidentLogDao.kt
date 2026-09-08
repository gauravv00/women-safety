package com.womensafety.sos.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.womensafety.sos.data.entity.IncidentLog
import kotlinx.coroutines.flow.Flow

@Dao
interface IncidentLogDao {
    @Query("SELECT * FROM incident_logs ORDER BY timestamp DESC")
    fun getAllIncidents(): Flow<List<IncidentLog>>

    @Query("SELECT * FROM incident_logs WHERE status = 'ACTIVE' ORDER BY timestamp DESC LIMIT 1")
    fun getActiveIncident(): Flow<IncidentLog?>

    @Query("SELECT * FROM incident_logs WHERE status = 'ACTIVE' ORDER BY timestamp DESC LIMIT 1")
    suspend fun getActiveIncidentSync(): IncidentLog?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIncident(incident: IncidentLog): Long

    @Update
    suspend fun updateIncident(incident: IncidentLog)

    @Query("UPDATE incident_logs SET status = 'RESOLVED', resolvedAtTimestamp = :resolvedAt WHERE id = :id")
    suspend fun resolveIncident(id: Long, resolvedAt: Long = System.currentTimeMillis())

    @Query("UPDATE incident_logs SET audioCloudUrl = :cloudUrl WHERE id = :id")
    suspend fun updateAudioCloudUrl(id: Long, cloudUrl: String)
}
