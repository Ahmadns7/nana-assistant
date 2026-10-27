package com.nana.common

object Constants {
    // Wake word
    const val WAKE_WORD_SENSITIVITY = 0.5f
    const val WAKE_WORD_MODEL_NAME = "hey_nana.ppn"

    // STT
    const val STT_SAMPLE_RATE = 16000
    const val STT_MAX_DURATION_MS = 10000
    const val STT_MODEL_DIR = "vosk-model-small-en-us-0.15"

    // TTS
    const val TTS_DEFAULT_RATE = 1.0f
    const val TTS_DEFAULT_PITCH = 1.0f
    const val TTS_PIPER_MODEL = "piper/en_US-lessac-medium.onnx"

    // LLM
    const val LLM_MAX_TOKENS = 512
    const val LLM_TEMPERATURE = 0.3f
    const val LLM_TOP_P = 0.9f
    const val LLM_MODEL_GEMMA_2B = "gemma-2b-it-q4_k_m.gguf"
    const val LLM_MODEL_GEMMA_3_1B = "gemma-3-1b-it-q4_k_m.gguf"
    const val LLM_MODEL_PHI_3_MINI = "phi-3-mini-4k-instruct-q4_k_m.gguf"

    // Agent
    const val AGENT_MAX_STEPS = 10
    const val AGENT_STEP_TIMEOUT_MS = 5000
    const val AGENT_VERIFICATION_RETRIES = 2

    // Accessibility
    const val A11Y_EVENT_TYPES = intArrayOf(
        android.view.accessibility.AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,
        android.view.accessibility.AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED,
        android.view.accessibility.AccessibilityEvent.TYPE_VIEW_CLICKED,
        android.view.accessibility.AccessibilityEvent.TYPE_VIEW_SCROLLED,
        android.view.accessibility.AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED
    )

    // Media
    const val MEDIA_PACKAGE_WHATSAPP = "com.whatsapp"
    const val MEDIA_PACKAGE_SPOTIFY = "com.spotify.music"
    const val MEDIA_PACKAGE_YOUTUBE_MUSIC = "com.google.android.apps.youtube.music"

    // Storage
    const val DB_NAME = "nana.db"
    const val ACTION_LOG_TABLE = "action_log"
    const val MACRO_TABLE = "voice_macros"
    const val SETTINGS_DATASTORE = "nana_settings"

    // Battery aware
    const val BATTERY_LOW_THRESHOLD = 0.20f
    const val BATTERY_CRITICAL_THRESHOLD = 0.10f

    // Permissions
    const val PERMISSION_RECORD_AUDIO = android.Manifest.permission.RECORD_AUDIO
    const val PERMISSION_ACCESSIBILITY = "android.permission.BIND_ACCESSIBILITY_SERVICE"
    const val PERMISSION_NOTIFICATION_LISTENER = "android.permission.BIND_NOTIFICATION_LISTENER_SERVICE"
    const val PERMISSION_FOREGROUND_SERVICE = android.Manifest.permission.FOREGROUND_SERVICE
    const val PERMISSION_FOREGROUND_SERVICE_MICROPHONE = "android.permission.FOREGROUND_SERVICE_MICROPHONE"
    const val PERMISSION_SYSTEM_ALERT_WINDOW = android.Manifest.permission.SYSTEM_ALERT_WINDOW
    const val PERMISSION_CALL_PHONE = android.Manifest.permission.CALL_PHONE
    const val PERMISSION_SEND_SMS = android.Manifest.permission.SEND_SMS
    const val PERMISSION_READ_CONTACTS = android.Manifest.permission.READ_CONTACTS
    const val PERMISSION_READ_CALENDAR = android.Manifest.permission.READ_CALENDAR

    // Actions
    const val ACTION_WAKE_WORD_DETECTED = "com.nana.assistant.WAKE_WORD_DETECTED"
    const val ACTION_STT_RESULT = "com.nana.assistant.STT_RESULT"
    const val ACTION_AGENT_COMMAND = "com.nana.assistant.AGENT_COMMAND"
    const val ACTION_TTS_SPEAK = "com.nana.assistant.TTS_SPEAK"
    const val ACTION_KILL_SWITCH = "com.nana.assistant.KILL_SWITCH"

    // Model files directory
    const val MODELS_DIR = "models"
}
