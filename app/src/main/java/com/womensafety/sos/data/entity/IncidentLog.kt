package com.womensafety.sos.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "incident_logs")
data class IncidentLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val status: String, // "ACTIVE", "RESOLVED", "CANCELLED"
    val latitude: Double,
    val longitude: Double,
    val audioFilePath: String? = null,
    val audioCloudUrl: String? = null,
    val resolvedAtTimestamp: Long? = null,
    val triggerSource: String = "MANUAL_HOLD" // "MANUAL_HOLD", "SHAKE", "CHECKIN_TIMER"
)
