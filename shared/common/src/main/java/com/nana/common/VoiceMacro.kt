package com.nana.common

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Entity(tableName = Constants.MACRO_TABLE)
@Serializable
data class VoiceMacro(
    @PrimaryKey val triggerPhrase: String,
    val name: String,
    val description: String,
    val actions: List<MacroAction>,
    val enabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val lastUsedAt: Long = 0,
    val useCount: Int = 0
) {
    @Serializable
    data class MacroAction(
        val type: ActionType,
        val params: Map<String, String> // JSON-serializable params
    ) {
        enum class ActionType {
            OPEN_APP, TOGGLE_SETTING, MEDIA_CONTROL, SEND_MESSAGE,
            SET_ALARM, SET_TIMER, TTS_SPEAK, WAIT, CONDITION
        }
    }

    fun matches(input: String): Boolean {
        val normalized = input.lowercase().trim()
        return triggerPhrase.lowercase().split("|").any { normalized.contains(it.trim()) }
    }
}
