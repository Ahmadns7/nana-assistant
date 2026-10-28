# ProGuard rules for NaNa Assistant

# Keep application class
-keep class com.nana.assistant.NaNaApplication { *; }

# Keep model classes for Room/Serialization
-keep class com.nana.common.** { *; }
-keep class com.nana.storage.** { *; }

# Keep services
-keep class com.nana.wakeword.WakeWordService { *; }
-keep class com.nana.stt.STTService { *; }
-keep class com.nana.accessibility.NanaAccessibilityService { *; }
-keep class com.nana.notifications.NotificationListener { *; }
-keep class com.nana.agent.AgentService { *; }

# Keep Hilt generated classes
-keep class dagger.hilt.** { *; }

# Keep Media3 classes
-keep class androidx.media3.** { *; }

# Keep ML Kit classes
-keep class com.google.mlkit.** { *; }

# Keep Vosk classes
-keep class com.alphacephei.vosk.** { *; }

# Keep Kotlin coroutines
-keep class kotlinx.coroutines.** { *; }

# Keep Kotlinx serialization
-keep class kotlinx.serialization.** { *; }

# Keep Okio
-keep class okio.** { *; }

# Don't warn about missing classes for optional dependencies
-dontwarn com.google.mediapipe.**
-dontwarn com.alphacephei.vosk.**
-dontwarn com.google.mlkit.**

# Keep native library loading
-keep class com.nana.tts.PiperEngine { *; }
-keep class com.nana.llm.LLMRunner { *; }

# Optimization
-optimizationpasses 5
-allowaccessmodification
-mergeinterfacesaggressively
