package com.womensafety.sos.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "paired_wards")
data class PairedWard(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val wardPairingCode: String,
    val wardName: String,
    val pairedAt: Long = System.currentTimeMillis(),
    val lastKnownLat: Double = 0.0,
    val lastKnownLng: Double = 0.0,
    val lastStatus: String = "SAFE",
    val lastActiveTimestamp: Long = 0L,
    val activeIncidentId: Long? = null
)
