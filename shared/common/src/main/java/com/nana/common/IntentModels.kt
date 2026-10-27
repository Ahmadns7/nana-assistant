package com.nana.common

import kotlinx.serialization.Serializable

@Serializable
sealed interface VoiceIntent {
    @Serializable data class MediaControl(
        val action: MediaAction,
        val target: String? = null // app package or "default"
    ) : VoiceIntent {
        enum class MediaAction { PLAY, PAUSE, NEXT, PREVIOUS, VOLUME_UP, VOLUME_DOWN, VOLUME_SET, WHATS_PLAYING }
    }

    @Serializable data class AppControl(
        val action: AppAction,
        val target: String, // package name or app name
        val extra: String? = null
    ) : VoiceIntent {
        enum class AppAction { OPEN, CLOSE, SWITCH_TO, GET_INFO }
    }

    @Serializable data class Messaging(
        val action: MessagingAction,
        val contact: String?,
        val message: String? = null,
        val app: String = Constants.MEDIA_PACKAGE_WHATSAPP
    ) : VoiceIntent {
        enum class MessagingAction { OPEN, FIND_CHAT, READ_LAST, REPLY, SEND, READ_UNREAD }
    }

    @Serializable data class PhoneBasic(
        val action: PhoneAction,
        val target: String? = null, // phone number, contact name
        val extra: String? = null // message body, alarm time, etc.
    ) : VoiceIntent {
        enum class PhoneAction {
            CALL, SMS, SET_ALARM, SET_TIMER, TOGGLE_FLASHLIGHT,
            TOGGLE_WIFI, TOGGLE_BLUETOOTH, TOGGLE_DND,
            SET_BRIGHTNESS, GET_BATTERY, GET_STORAGE,
            CALENDAR_EVENTS, LOOKUP_CONTACT
        }
    }

    @Serializable data class ScreenQuery(
        val question: String,
        val action: ScreenAction = ScreenAction.READ
    ) : VoiceIntent {
        enum class ScreenAction { READ, SUMMARIZE, COPY_TEXT, TRANSLATE, FIND_ELEMENT, OCR }
    }

    @Serializable data class MacroTrigger(
        val triggerPhrase: String
    ) : VoiceIntent

    @Serializable data class SettingsControl(
        val action: SettingsAction,
        val target: String,
        val value: String? = null
    ) : VoiceIntent {
        enum class SettingsAction { GET, SET, TOGGLE, RESET }
    }

    @Serializable data class Unknown(
        val rawText: String
    ) : VoiceIntent

    companion object {
        fun categorize(intent: VoiceIntent): ActionLog.IntentCategory = when (intent) {
            is MediaControl -> ActionLog.IntentCategory.MEDIA_CONTROL
            is AppControl -> ActionLog.IntentCategory.APP_CONTROL
            is Messaging -> ActionLog.IntentCategory.MESSAGING
            is PhoneBasic -> ActionLog.IntentCategory.PHONE_BASICS
            is ScreenQuery -> ActionLog.IntentCategory.SCREEN_QUERY
            is MacroTrigger -> ActionLog.IntentCategory.MACRO
            is SettingsControl -> ActionLog.IntentCategory.SETTINGS
            is Unknown -> ActionLog.IntentCategory.UNKNOWN
        }
    }
}
