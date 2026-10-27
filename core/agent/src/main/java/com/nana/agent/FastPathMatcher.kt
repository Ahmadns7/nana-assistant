package com.nana.agent

import com.nana.common.Result
import com.nana.common.VoiceIntent
import com.nana.logging.NanaLogger

/**
 * Rule-based fast path for common commands to avoid LLM latency.
 * Targets < 500ms response for simple actions.
 */
object FastPathMatcher {
    private val mediaPatterns = mapOf(
        "play" to VoiceIntent.MediaControl.MediaAction.PLAY,
        "pause" to VoiceIntent.MediaControl.MediaAction.PAUSE,
        "stop" to VoiceIntent.MediaControl.MediaAction.PAUSE,
        "next" to VoiceIntent.MediaControl.MediaAction.NEXT,
        "skip" to VoiceIntent.MediaControl.MediaAction.NEXT,
        "previous" to VoiceIntent.MediaControl.MediaAction.PREVIOUS,
        "back" to VoiceIntent.MediaControl.MediaAction.PREVIOUS,
        "volume up" to VoiceIntent.MediaControl.MediaAction.VOLUME_UP,
        "louder" to VoiceIntent.MediaControl.MediaAction.VOLUME_UP,
        "volume down" to VoiceIntent.MediaControl.MediaAction.VOLUME_DOWN,
        "quieter" to VoiceIntent.MediaControl.MediaAction.VOLUME_DOWN,
        "what'?s playing" to VoiceIntent.MediaControl.MediaAction.WHATS_PLAYING,
        "what song" to VoiceIntent.MediaControl.MediaAction.WHATS_PLAYING,
        "now playing" to VoiceIntent.MediaControl.MediaAction.WHATS_PLAYING
    )

    private val appPatterns = mapOf(
        "open" to VoiceIntent.AppControl.AppAction.OPEN,
        "launch" to VoiceIntent.AppControl.AppAction.OPEN,
        "start" to VoiceIntent.AppControl.AppAction.OPEN,
        "close" to VoiceIntent.AppControl.AppAction.CLOSE,
        "quit" to VoiceIntent.AppControl.AppAction.CLOSE,
        "switch to" to VoiceIntent.AppControl.AppAction.SWITCH_TO
    )

    private val phonePatterns = mapOf(
        "call" to VoiceIntent.PhoneBasic.PhoneAction.CALL,
        "dial" to VoiceIntent.PhoneBasic.PhoneAction.CALL,
        "text" to VoiceIntent.PhoneBasic.PhoneAction.SMS,
        "sms" to VoiceIntent.PhoneBasic.PhoneAction.SMS,
        "message" to VoiceIntent.PhoneBasic.PhoneAction.SMS,
        "alarm" to VoiceIntent.PhoneBasic.PhoneAction.SET_ALARM,
        "timer" to VoiceIntent.PhoneBasic.PhoneAction.SET_TIMER,
        "flashlight" to VoiceIntent.PhoneBasic.PhoneAction.TOGGLE_FLASHLIGHT,
        "torch" to VoiceIntent.PhoneBasic.PhoneAction.TOGGLE_FLASHLIGHT,
        "wifi" to VoiceIntent.PhoneBasic.PhoneAction.TOGGLE_WIFI,
        "bluetooth" to VoiceIntent.PhoneBasic.PhoneAction.TOGGLE_BLUETOOTH,
        "do not disturb" to VoiceIntent.PhoneBasic.PhoneAction.TOGGLE_DND,
        "dnd" to VoiceIntent.PhoneBasic.PhoneAction.TOGGLE_DND,
        "battery" to VoiceIntent.PhoneBasic.PhoneAction.GET_BATTERY,
        "storage" to VoiceIntent.PhoneBasic.PhoneAction.GET_STORAGE
    )

    fun match(text: String): Result<VoiceIntent> {
        val lower = text.lowercase().trim()
        
        // Media
        mediaPatterns.entries.find { (pattern, _) -> lower.contains(pattern) }?.let { (_, action) ->
            return Result.Success(VoiceIntent.MediaControl(action))
        }
        
        // App control
        appPatterns.entries.find { (pattern, _) -> lower.startsWith(pattern) }?.let { (pattern, action) ->
            val target = lower.substring(pattern.length).trim()
            if (target.isNotBlank()) {
                return Result.Success(VoiceIntent.AppControl(action, target))
            }
        }
        
        // Phone basics
        phonePatterns.entries.find { (pattern, _) -> lower.contains(pattern) }?.let { (_, action) ->
            return Result.Success(VoiceIntent.PhoneBasic(action))
        }
        
        // Messaging
        if (lower.contains("whatsapp") || lower.contains("message")) {
            if (lower.contains("read") && lower.contains("last")) {
                return Result.Success(VoiceIntent.Messaging(VoiceIntent.Messaging.MessagingAction.READ_LAST, null))
            }
            if (lower.contains("reply") || lower.contains("send")) {
                return Result.Success(VoiceIntent.Messaging(VoiceIntent.Messaging.MessagingAction.REPLY, null))
            }
            if (lower.contains("open") || lower.contains("find")) {
                return Result.Success(VoiceIntent.Messaging(VoiceIntent.Messaging.MessagingAction.OPEN, null))
            }
        }
        
        // Screen queries
        if (lower.contains("what'?s on") || lower.contains("read screen") || lower.contains("screen")) {
            return Result.Success(VoiceIntent.ScreenQuery(lower))
        }

        Result.Failure(Result.Error("NO_FAST_MATCH", "No fast path match"))
    }
}
