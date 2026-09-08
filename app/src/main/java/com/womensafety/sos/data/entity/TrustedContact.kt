package com.womensafety.sos.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "trusted_contacts")
data class TrustedContact(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val phoneNumber: String,
    val priorityRank: Int, // 1 = highest priority
    val photoUri: String? = null,
    val relationship: String = "Family"
)
