package com.womensafety.sos.data.model

enum class NotificationType {
    PAIR_REQUEST,
    PAIR_ACCEPTED,
    PAIR_REJECTED,
    EMERGENCY,
    RESOLVED
}

enum class NotificationStatus {
    PENDING,
    ACTIVE,
    RESOLVED,
    HANDLED
}

data class NotificationItem(
    val id: String = "",
    val type: NotificationType = NotificationType.PAIR_REQUEST,
    val title: String = "",
    val body: String = "",
    val refId: String = "",
    val fromUid: String = "",
    val fromName: String = "",
    val status: NotificationStatus = NotificationStatus.PENDING,
    val read: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toMap(): Map<String, Any> {
        return mapOf(
            "id" to id,
            "type" to type.name,
            "title" to title,
            "body" to body,
            "refId" to refId,
            "fromUid" to fromUid,
            "fromName" to fromName,
            "status" to status.name,
            "read" to read,
            "createdAt" to createdAt
        )
    }

    companion object {
        fun fromMap(id: String, map: Map<String, Any?>): NotificationItem {
            val typeStr = map["type"] as? String ?: NotificationType.PAIR_REQUEST.name
            val type = try {
                NotificationType.valueOf(typeStr)
            } catch (e: Exception) {
                NotificationType.PAIR_REQUEST
            }

            val statusStr = map["status"] as? String ?: NotificationStatus.PENDING.name
            val status = try {
                NotificationStatus.valueOf(statusStr)
            } catch (e: Exception) {
                NotificationStatus.PENDING
            }

            val createdAtLong = when (val c = map["createdAt"]) {
                is Long -> c
                is Number -> c.toLong()
                else -> System.currentTimeMillis()
            }

            return NotificationItem(
                id = id,
                type = type,
                title = map["title"] as? String ?: "",
                body = map["body"] as? String ?: "",
                refId = map["refId"] as? String ?: "",
                fromUid = map["fromUid"] as? String ?: "",
                fromName = map["fromName"] as? String ?: "",
                status = status,
                read = (map["read"] as? Boolean) ?: false,
                createdAt = createdAtLong
            )
        }
    }
}
