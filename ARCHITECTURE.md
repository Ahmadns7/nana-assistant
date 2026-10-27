# NaNa Architecture

## Overview
NaNa is a fully offline, voice-controlled AI phone assistant for Android. All processing happens on-device with zero network permissions.

## Module Diagram

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              NaNa App (app)                                  │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐ │
│  │   MainActivity   │  SettingsActivity  │  VoiceMacroEditor  │  ActionHistoryViewer │
│  └──────┬──────┘  └──────┬──────┘  └──────┬──────┘  └──────────┬──────────┘ │
└─────────┼────────────────┼────────────────┼─────────────────────┼────────────┘
          │                │                │                     │
          ▼                ▼                ▼                     ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                            Shared / Common                                   │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────────────────┐ │
│  │  common: Data classes, extensions,      │  logging: Structured logging,│ │
│  │  constants, utilities, Result<T>        │  action audit trail          │ │
│  └─────────────────┘  └─────────────────┘  └─────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────────────────┘
          │                │                │                     │
          ▼                ▼                ▼                     ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                              Core Modules                                    │
│                                                                             │
│  ┌─────────────┐ ┌─────────┐ ┌──────┐ ┌─────┐ ┌──────────────┐ ┌────────┐ │
│  │  wakeword   │ │   stt   │ │ tts  │ │ llm │ │ accessibility│ │ media  │ │
│  │  (Porcupine)│ │ (Vosk)  │ │(TTS) │ │(LLM)│ │ (Accessibility│ │(Media3)│ │
│  └──────┬──────┘ └────┬────┘ └──┬───┘ └─┬───┘ └──────┬───────┘ └───┬────┘ │
│         │             │         │      │         │               │       │
│  ┌──────┴─────┐ ┌─────┴────┐ ┌────┴───┐ ┌─────┴────┐ ┌──────────┴──┐ ┌────┴───┐
│  │   agent    │ │ storage  │ │ phone  │ │ macros   │ │    ocr      │ │notifs  │
│  │ (AgentLoop)│ │(Room/DS) │ │(Calls, │ │ (Voice   │ │  (ML Kit)   │ │(Notif  │
│  └────────────┘ │          │ │ SMS,   │ │  Macros) │ │             │ │Listener)│
│                 └──────────┘ └────────┘ └──────────┘ └─────────────┘ └────────┘
└─────────────────────────────────────────────────────────────────────────────┘
          │                │                │                     │
          ▼                ▼                ▼                     ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                              System Services                                 │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────────────────┐ │
│  │ WakeWordService │  │  AgentService   │  │ AccessibilityService        │ │
│  │ (Foreground)    │  │ (Foreground)    │  │ (System)                    │ │
│  └─────────────────┘  └─────────────────┘  └─────────────────────────────┘ │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────────────────┐ │
│  │ Notification    │  │ MediaSession    │  │ TTS Engine                  │ │
│  │ ListenerService │  │ Manager         │  │ (System/Offline Piper)      │ │
│  └─────────────────┘  └─────────────────┘  └─────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────────────────┘
```

## Data Flow

### Voice Activation → Action
```
User says "Hey NaNa"
    │
    ▼
WakeWordService (Porcupine) detects wake word
    │
    ▼
STT Module (Vosk) starts recording → transcribes speech
    │
    ▼
AgentLoop receives intent text
    │
    ├─► Fast path: Rule-based matcher (open app, media controls, toggles) < 500ms
    │
    └─► Slow path: On-device LLM (Gemma 2B/3) for complex reasoning
            │
            ▼
      AccessibilityService reads screen hierarchy (AccessibilityNodeInfo)
            │
            ▼
      Agent plans actions (click, scroll, setText, globalAction)
            │
            ▼
      Execute actions → verify screen state → repeat or complete
            │
            ▼
      TTS speaks result / confirmation
```

### Media Query ("What's playing?")
```
User: "What song is playing?"
    │
    ▼
MediaController gets active MediaController from MediaSessionManager
    │
    ▼
Reads Metadata (title, artist, album, artwork, playbackState)
    │
    ▼
If playing: TTS speaks "Now playing [title] by [artist] from [app]"
    │
    └─ If nothing: TTS speaks "Nothing is currently playing"
```

### WhatsApp Flow
```
User: "Open WhatsApp, find John, read last message, reply 'On my way'"
    │
    ▼
AgentLoop: openApp("WhatsApp") → wait for UI
    │
    ▼
AccessibilityService: find chat list → scroll/search "John" → click
    │
    ▼
Read messages: traverse node tree for message bubbles → extract text
    │
    ▼
TTS reads last message
    │
    ▼
User dictates reply → STT transcribes
    │
    ▼
Agent: setText(reply) in input field → TTS reads back draft
    │
    ▼
User says "confirm" → click send button
    │
    ▼
Verify sent (check for sent indicator) → TTS confirms
```

## Module Responsibilities

| Module | Responsibility | Key Classes |
|--------|---------------|-------------|
| `wakeword` | Offline wake word detection via Porcupine | `WakeWordDetector`, `WakeWordService` |
| `stt` | Speech-to-text via Vosk (streaming) | `VoskRecognizer`, `STTService` |
| `tts` | Text-to-speech via Android TTS / Piper | `TTSManager`, `PiperEngine` |
| `llm` | On-device LLM inference (MediaPipe/llama.cpp) | `LLMRunner`, `PromptBuilder` |
| `accessibility` | Screen reading & control via AccessibilityService | `ScreenReader`, `ActionExecutor` |
| `media` | Media control & metadata via Media3 | `MediaController`, `MediaStateProvider` |
| `agent` | Multi-step agent loop (observe→plan→act→verify) | `AgentLoop`, `Planner`, `Verifier` |
| `storage` | Encrypted Room DB + DataStore for settings | `ActionLogDao`, `SettingsStore` |
| `phone` | Calls, SMS, alarms, toggles, system info | `PhoneController`, `SystemToggles` |
| `macros` | Voice macros (trigger → action sequence) | `MacroEngine`, `MacroStore` |
| `ocr` | Offline OCR via ML Kit | `OCREngine` |
| `notifications` | Notification reading & voice triage | `NotificationListener`, `NotificationTriage` |
| `settings` | User preferences, model management | `SettingsRepository` |

## Threading Model

- **Main thread**: UI only
- **IO thread**: Room, DataStore, file I/O
- **Wake word thread**: Audio capture + Porcupine processing (real-time priority)
- **STT thread**: Vosk recognition (CPU intensive)
- **LLM thread**: Model inference (can use GPU delegate)
- **Agent thread**: Coroutine context for agent loop steps
- **Accessibility thread**: System callback thread (must be fast)

## Model Management

Models stored in `/data/data/com.nana.assistant/files/models/`:
- `wakeword/hey_nana.ppn` (Porcupine)
- `stt/vosk-model-small-en-us-0.15` (Vosk)
- `llm/gemma-2b-it-q4_k_m.gguf` or `gemma-3-1b-it-q4_k_m.gguf` (llama.cpp/MediaPipe)
- `tts/piper/en_US-lessac-medium.onnx` (Piper, optional)
- `ocr/mlkit_text_recognition.tflite` (bundled in ML Kit)

## Security & Privacy

- **No INTERNET permission** in manifest
- All models bundled or downloaded once during setup (user-initiated)
- Encrypted Room database (SQLCipher)
- Action audit log: timestamp, intent, screen state hash, action taken, result
- Kill switch: long-press power + volume down stops all services
- Visual indicator: persistent notification when listening/acting
- Confirmation required: send message, call, delete, payment screens blocked

## Battery-Aware Mode

When battery < 20% or user enables:
- Wake word: increase sensitivity threshold, reduce CPU frequency
- STT: shorter max recording duration
- LLM: use smaller model (1B vs 2B), fewer reasoning steps
- Agent: reduce max steps from 10 to 5
- Polling intervals doubled