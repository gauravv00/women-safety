package com.womensafety.sos.data.model

data class EmergencyEvent(
    val id: String = "",
    val wardUid: String = "",
    val wardName: String = "",
    val status: String = "ACTIVE",  // ACTIVE | RESOLVED
    val startedAt: Long = 0L,
    val resolvedAt: Long = 0L,
    val lastLat: Double = 0.0,
    val lastLng: Double = 0.0,
    val lastAccuracy: Float = 0f,
    val lastLocationTs: Long = 0L,
    val audioUrl: String? = null
) {
    fun toMap(): Map<String, Any> {
        val map = mutableMapOf<String, Any>(
            "id" to id,
            "wardUid" to wardUid,
            "wardName" to wardName,
            "status" to status,
            "startedAt" to startedAt,
            "resolvedAt" to resolvedAt,
            "lastLat" to lastLat,
            "lastLng" to lastLng,
            "lastAccuracy" to lastAccuracy.toDouble(),
            "lastLocationTs" to lastLocationTs
        )
        if (!audioUrl.isNullOrBlank()) {
            map["audioUrl"] = audioUrl
        }
        return map
    }

    companion object {
        fun fromMap(id: String, data: Map<String, Any?>): EmergencyEvent = EmergencyEvent(
            id = id,
            wardUid = data["wardUid"] as? String ?: "",
            wardName = data["wardName"] as? String ?: "",
            status = data["status"] as? String ?: "ACTIVE",
            startedAt = (data["startedAt"] as? Number)?.toLong() ?: 0L,
            resolvedAt = (data["resolvedAt"] as? Number)?.toLong() ?: 0L,
            lastLat = (data["lastLat"] as? Number)?.toDouble() ?: 0.0,
            lastLng = (data["lastLng"] as? Number)?.toDouble() ?: 0.0,
            lastAccuracy = (data["lastAccuracy"] as? Number)?.toFloat() ?: 0f,
            lastLocationTs = (data["lastLocationTs"] as? Number)?.toLong() ?: 0L,
            audioUrl = data["audioUrl"] as? String
        )
    }
}
