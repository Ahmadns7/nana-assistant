package com.nana.storage

import androidx.room.TypeConverter
import com.nana.common.ActionLog
import com.nana.common.VoiceMacro
import kotlinx.serialization.json.Json

class Converters {
    private val json = Json { ignoreUnknownKeys = true }

    @TypeConverter
    fun fromActionLogCategory(value: Int): ActionLog.IntentCategory = ActionLog.IntentCategory.values()[value]

    @TypeConverter
    fun toActionLogCategory(category: ActionLog.IntentCategory): Int = category.ordinal

    @TypeConverter
    fun fromActionResult(value: Int): ActionLog.ActionResult = ActionLog.ActionResult.values()[value]

    @TypeConverter
    fun toActionResult(result: ActionLog.ActionResult): Int = result.ordinal

    @TypeConverter
    fun fromMacroActions(value: String): List<VoiceMacro.MacroAction> = 
        json.decodeFromString<List<VoiceMacro.MacroAction>>(value)

    @TypeConverter
    fun toMacroActions(actions: List<VoiceMacro.MacroAction>): String = 
        json.encodeToString(actions)
}
