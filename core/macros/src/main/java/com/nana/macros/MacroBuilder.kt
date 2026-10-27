package com.nana.macros

import com.nana.common.VoiceMacro

/**
 * Builder for creating voice macros programmatically.
 */
class MacroBuilder(private val triggerPhrase: String) {
    private var name = ""
    private var description = ""
    private val actions = mutableListOf<VoiceMacro.MacroAction>()
    private var enabled = true

    fun name(name: String): MacroBuilder {
        this.name = name
        return this
    }

    fun description(description: String): MacroBuilder {
        this.description = description
        return this
    }

    fun addAction(type: VoiceMacro.MacroAction.ActionType, params: Map<String, String>): MacroBuilder {
        actions.add(VoiceMacro.MacroAction(type, params))
        return this
    }

    fun openApp(packageName: String): MacroBuilder = addAction(VoiceMacro.MacroAction.ActionType.OPEN_APP, mapOf("package" to packageName))
    fun toggleWifi(enabled: Boolean = true): MacroBuilder = addAction(VoiceMacro.MacroAction.ActionType.TOGGLE_SETTING, mapOf("setting" to "wifi", "value" to enabled.toString()))
    fun toggleBluetooth(enabled: Boolean = true): MacroBuilder = addAction(VoiceMacro.MacroAction.ActionType.TOGGLE_SETTING, mapOf("setting" to "bluetooth", "value" to enabled.toString()))
    fun toggleDnd(enabled: Boolean = true): MacroBuilder = addAction(VoiceMacro.MacroAction.ActionType.TOGGLE_SETTING, mapOf("setting" to "dnd", "value" to enabled.toString()))
    fun toggleFlashlight(enabled: Boolean = true): MacroBuilder = addAction(VoiceMacro.MacroAction.ActionType.TOGGLE_SETTING, mapOf("setting" to "flashlight", "value" to enabled.toString()))
    fun mediaPlay(): MacroBuilder = addAction(VoiceMacro.MacroAction.ActionType.MEDIA_CONTROL, mapOf("action" to "play"))
    fun mediaPause(): MacroBuilder = addAction(VoiceMacro.MacroAction.ActionType.MEDIA_CONTROL, mapOf("action" to "pause"))
    fun mediaNext(): MacroBuilder = addAction(VoiceMacro.MacroAction.ActionType.MEDIA_CONTROL, mapOf("action" to "next"))
    fun mediaPrevious(): MacroBuilder = addAction(VoiceMacro.MacroAction.ActionType.MEDIA_CONTROL, mapOf("action" to "previous"))
    fun setAlarm(time: String): MacroBuilder = addAction(VoiceMacro.MacroAction.ActionType.SET_ALARM, mapOf("time" to time)) // HH:MM
    fun setTimer(duration: String): MacroBuilder = addAction(VoiceMacro.MacroAction.ActionType.SET_TIMER, mapOf("duration" to duration)) // 5m, 30s, 1h
    fun speak(text: String): MacroBuilder = addAction(VoiceMacro.MacroAction.ActionType.TTS_SPEAK, mapOf("text" to text))
    fun waitMs(ms: Long): MacroBuilder = addAction(VoiceMacro.MacroAction.ActionType.WAIT, mapOf("ms" to ms.toString()))
    fun enabled(enabled: Boolean): MacroBuilder {
        this.enabled = enabled
        return this
    }

    fun build(): VoiceMacro {
        return VoiceMacro(
            triggerPhrase = triggerPhrase,
            name = name.ifBlank { triggerPhrase },
            description = description.ifBlank { "Macro for $triggerPhrase" },
            actions = actions,
            enabled = enabled
        )
    }
}
