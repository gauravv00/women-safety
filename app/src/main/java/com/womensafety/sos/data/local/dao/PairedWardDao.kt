package com.womensafety.sos.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.womensafety.sos.data.entity.PairedWard
import kotlinx.coroutines.flow.Flow

@Dao
interface PairedWardDao {
    @Query("SELECT * FROM paired_wards ORDER BY pairedAt DESC")
    fun getAllPairedWards(): Flow<List<PairedWard>>

    @Query("SELECT * FROM paired_wards")
    suspend fun getAllPairedWardsSync(): List<PairedWard>

    @Query("SELECT * FROM paired_wards WHERE wardPairingCode = :code LIMIT 1")
    suspend fun getWardByCode(code: String): PairedWard?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWard(ward: PairedWard): Long

    @Update
    suspend fun updateWard(ward: PairedWard)

    @Query("UPDATE paired_wards SET lastStatus = :status, lastKnownLat = :lat, lastKnownLng = :lng, lastActiveTimestamp = :timestamp, activeIncidentId = :incidentId WHERE wardPairingCode = :code")
    suspend fun updateWardStatus(code: String, status: String, lat: Double, lng: Double, timestamp: Long, incidentId: Long?)

    @Query("DELETE FROM paired_wards WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM paired_wards WHERE wardPairingCode = :code")
    suspend fun deleteByCode(code: String)
}
