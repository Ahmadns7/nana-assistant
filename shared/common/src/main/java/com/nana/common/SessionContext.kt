package com.nana.common

import kotlinx.serialization.Serializable

/**
 * In-memory context for a voice session.
 * Survives across multiple turns in a conversation.
 */
@Serializable
data class SessionContext(
    val sessionId: String = java.util.UUID.randomUUID().toString(),
    val createdAt: Long = System.currentTimeMillis(),
    var lastIntent: String? = null,
    var lastScreenState: ScreenState? = null,
    var lastActionResult: ActionLog.ActionResult? = null,
    var referencedEntities: MutableMap<String, EntityReference> = mutableMapOf(),
    var pendingConfirmation: PendingConfirmation? = null,
    var stepCount: Int = 0
) {
    @Serializable
    data class EntityReference(
        val type: EntityType,
        val identifier: String, // name, phone number, package name, etc.
        val displayName: String,
        val metadata: Map<String, String> = emptyMap()
    ) {
        enum class EntityType {
            CONTACT, APP, CHAT, MESSAGE, MEDIA_ITEM, ALARM, TIMER, FILE
        }
    }

    @Serializable
    data class PendingConfirmation(
        val action: String,
        val params: Map<String, String>,
        val description: String,
        val expiresAt: Long = System.currentTimeMillis() + 30000 // 30 seconds
    ) {
        fun isExpired(): Boolean = System.currentTimeMillis() > expiresAt
    }

    fun addEntity(type: EntityReference.EntityType, identifier: String, displayName: String, metadata: Map<String, String> = emptyMap()) {
        referencedEntities[identifier] = EntityReference(type, identifier, displayName, metadata)
    }

    fun resolveEntity(identifier: String): EntityReference? {
        return referencedEntities[identifier] ?: referencedEntities.values.find { it.displayName.lowercase() == identifier.lowercase() }
    }

    fun clearExpired() {
        pendingConfirmation?.let { if (it.isExpired()) pendingConfirmation = null }
    }
}
