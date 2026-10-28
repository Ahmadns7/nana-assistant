# NaNa Assistant

**An offline-first, voice-enabled phone assistant for Android.**

NaNa recognizes speech and a user-selected wake phrase on-device after you download a compact Vosk model. Model archives are downloaded only when you choose a model, through Android DownloadManager, and installed in app storage. Speech recognition and wake detection then work offline. This build does not include an active on-device LLM inference engine.

## Features

### Core Capabilities
- **Hands-free activation**: Wake word ("Hey NaNa") + tap-to-talk; works with screen off
- **Media control**: Play/pause/next/previous/volume; "What's playing?" answers from real MediaSession metadata
- **App control**: Open any installed app by name (fuzzy match); navigate inside apps
- **Screen understanding**: Read active screen via accessibility tree; answer "what does this say?" or "read my last message"
- **Multi-step agent loop**: Observe → Plan → Act → Verify → Repeat with step limit and "stuck" detection
- **WhatsApp messaging**: Open app, find contact, read messages, dictate reply, confirm before send
- **Phone basics**: Calls, SMS, alarms, timers, flashlight, Wi-Fi/Bluetooth/brightness toggles, DND, battery/storage status, calendar, contacts

### Additional Features
- **Custom voice macros**: "Good night" = DND + alarm + brightness down
- **Screen summarizer**: "What's on my screen?" with copy/translate (offline ML Kit)
- **Offline OCR**: For images and unreadable UI nodes
- **Notification reader & voice triage**: "Read notifications", "Reply to last one"
- **Context memory**: "Send it to him" after reading a chat
- **Accessibility mode**: Verbose spoken feedback, large visual captions
- **Clipboard, notes, reminders**: By voice; offline calculations
- **Per-app permission allowlist & action history log**: User-reviewable
- **Multilingual**: English + additional languages via swappable STT/TTS models
- **Battery-aware mode**: Lowers wake-word/model activity when battery is low

### Safety & Privacy
- **Offline processing** — speech recognition and wake detection run on-device; Android DownloadManager handles only model downloads explicitly started by the user
- **Confirm before irreversible actions**: Sending messages, calls, deleting, payments
- **Banking/payment screens blocked** — refused with explanation
- **Visible indicator & kill switch** when listening or controlling screen
- **Clear failure handling**: "I couldn't find that chat" > wrong action
- **Action audit log**: Every action logged with screen state for debugging

## Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                        NaNa App                                  │
│  ┌──────────┐ ┌──────────┐ ┌────────────┐ ┌──────────────────┐ │
│  │  Main    │ │ Settings │ │ Voice Macro│ │ Action History   │ │
│  └────┬─────┘ └────┬─────┘ └─────┬──────┘ └────────┬─────────┘ │
└───────┼─────────────┼─────────────┼─────────────────┼───────────┘
        ▼             ▼             ▼                 ▼
┌─────────────────────────────────────────────────────────────────┐
│                      Core Modules                                │
│ ┌─────────┐ ┌─────┐ ┌────┐ ┌────┐ ┌────────────┐ ┌──────────┐  │
│ │wakeword │ │ stt │ │tts │ │llm │ │accessibility│ │ media   │  │
│ └────┬────┘ └──┬──┘ └─┬──┘ └─┬──┘ └─────┬──────┘ └────┬────┘  │
│      │         │      │     │       │             │        │
│ ┌────┴────┐ ┌───┴────┐ ┌───┴───┐ ┌───┴────┐ ┌──────┴───┐ ┌───┴────┐│
│ │ agent   │ │storage │ │ phone │ │ macros │ │ ocr      │ │notifs  ││
│ └─────────┘ └────────┘ └───────┘ └────────┘ └──────────┘ └────────┘│
└─────────────────────────────────────────────────────────────────┘
        │         │         │         │         │           │
        ▼         ▼         ▼         ▼         ▼           ▼
┌─────────────────────────────────────────────────────────────────┐
│                    System Services                               │
│ ┌─────────────┐ ┌─────────────┐ ┌───────────────────────────┐  │
│ │WakeWordSvc  │ │ AgentSvc    │ │ AccessibilityService      │  │
│ │(Foreground) │ │(Foreground) │ │(System)                   │  │
│ └─────────────┘ └─────────────┘ └───────────────────────────┘  │
│ ┌─────────────┐ ┌─────────────┐ ┌───────────────────────────┐  │
│ │NotifListener│ │MediaSession │ │ TTS Engine                │  │
│ └─────────────┘ └─────────────┘ └───────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
```

## Tech Stack (All On-Device)

| Component | Technology | Model/Implementation |
|-----------|------------|---------------------|
| Wake Word | Vosk grammar | Uses the selected compact speech model |
| STT | Vosk | Downloadable small language models (about 36–45MB) |
| TTS | Android TTS / Piper | System / `piper/en_US-lessac-medium.onnx` (50MB) |
| LLM | llama.cpp / MediaPipe | Gemma 2B/3 1B / Phi-3 Mini (0.8-2.3GB) |
| Screen Control | AccessibilityService | Native Android API |
| Media | Media3 MediaController | Native Android API |
| OCR | ML Kit Text Recognition | Bundled (20MB) |
| Storage | Room (SQLCipher) + DataStore | Encrypted |

## Model Selection Table

| Model | Size | RAM | Latency (MID) | Latency (HIGH) | Device Tier |
|-------|------|-----|---------------|----------------|-------------|
| **Wake Word** | | | | | |
| Vosk small English | 40 MB | ~80 MB | Device dependent | Device dependent | Entry-level phones |
| **STT** | | | | | |
| Vosk Small EN-US | 40 MB | 80 MB | 300 ms | 200 ms | LOW+ |
| Vosk Small Multi | 80 MB | 120 MB | 400 ms | 300 ms | MID+ |
| **LLM** | | | | | |
| Gemma 3 1B IT (Q4_K_M) | 800 MB | 1000 MB | 1200 ms | 800 ms | MID+ |
| Gemma 2B IT (Q4_K_M) | 1.5 GB | 1800 MB | 2000 ms | 1500 ms | HIGH+ |
| Phi-3 Mini 4K (Q4_K_M) | 2.3 GB | 2500 MB | 2500 ms | 1800 ms | FLAGSHIP |
| **TTS** | | | | | |
| Android System TTS | 0 | 50 MB | 100 ms | 50 ms | All |
| Piper EN-US Lessac | 50 MB | 100 MB | 300 ms | 200 ms | MID+ |
| **OCR** | | | | | |
| ML Kit Text Recognition | 20 MB | 100 MB | 500 ms | 300 ms | All |

*Device Tiers: LOW (<3GB RAM), MID (3-6GB), HIGH (6-10GB), FLAGSHIP (10GB+)*

## Building

### Prerequisites
- Android Studio Ladybug+ or Gradle 8.4+
- JDK 17
- Android SDK 34
- NDK 26+ (for llama.cpp JNI if used)

### Model Setup

Speech models are **not included** in the APK. Choose and download one in Settings; Android DownloadManager stores the archive before NaNa extracts it to private app storage:

```
app/src/main/assets/models/
├── stt/
│   └── vosk-model-small-en-us-0.15/  # Installed from Android DownloadManager
├── llm/
│   ├── gemma-3-1b-it-q4_k_m.gguf     # From Hugging Face (gguf format)
│   ├── gemma-2b-it-q4_k_m.gguf
│   └── phi-3-mini-4k-instruct-q4_k_m.gguf
├── tts/
│   └── piper/
│       ├── en_US-lessac-medium.onnx   # From Piper releases
│       ├── en_US-lessac-medium.onnx.json
│       └── speaker_embeddings.bin
```

**Offline speech setup**: Open Settings → Offline speech models and download a small language model over Wi-Fi. Android DownloadManager performs the transfer and shows its own notification. Select the installed model, enter a wake phrase supported by its vocabulary, then enable wake-word detection. NaNa has no in-app HTTP downloader; Android requires the app's `INTERNET` manifest permission to enqueue system-managed downloads.

### Build Commands

```bash
# Debug build
./gradlew assembleDebug

# Release build (requires signing config in local.properties)
./gradlew assembleRelease

# Run tests
./gradlew test
./gradlew connectedAndroidTest
```

### Required Permissions (User Must Enable)

1. **Microphone** — Record audio for wake word/STT
2. **Accessibility Service** — Settings → Accessibility → NaNa Assistant → ON
3. **Notification Access** — Settings → Apps → Special access → Notification access → NaNa Notifications → ON
4. **Display over apps** — For visual indicator (Settings → Apps → Special access)
5. **Phone/SMS/Contacts/Calendar** — For corresponding features

## Usage

### Wake Word
Say **"Hey NaNa"** — the assistant activates, listens for your command, then responds.

### Tap-to-Talk
Open the app and tap the microphone button.

### Example Commands

| Category | Commands |
|----------|----------|
| **Media** | "Play", "Pause", "Next song", "Previous", "Volume up", "What's playing?" |
| **Apps** | "Open WhatsApp", "Launch Spotify", "Switch to Chrome" |
| **Messages** | "Open WhatsApp, find John, read last message", "Reply 'On my way'", "Send message to Mom 'I'm home'" |
| **Calls** | "Call 555-1234", "Call Mom" |
| **Alarms/Timers** | "Set alarm for 7 AM", "Set timer for 10 minutes" |
| **Toggles** | "Turn on flashlight", "Enable Wi-Fi", "Disable Bluetooth", "Do not disturb on" |
| **Info** | "Battery level", "Storage space", "What's on my screen?", "Read notifications" |
| **Screen** | "Read this", "Summarize screen", "Copy text", "Translate to Spanish" |
| **Macros** | "Good night" (custom) |

### WhatsApp Flow Example
```
User: "Open WhatsApp, find John, read last message, reply 'On my way'"
NaNa: [Opens WhatsApp] → [Searches chats for "John"] → [Opens chat] → [Reads last message aloud]
NaNa: "Last message from John: 'Are you coming?'"
User: "Reply 'On my way'"
NaNa: [Types "On my way" in input field] → [Reads back: "On my way"]
User: "Confirm"
NaNa: [Clicks send] → "Message sent to John"
```

## Project Structure

```
NaNa/
├── app/                    # Main application module
├── core/
│   ├── wakeword/           # Vosk phrase detection
│   ├── stt/                # Vosk speech-to-text
│   ├── tts/                # Android TTS + Piper
│   ├── llm/                # On-device LLM (llama.cpp/MediaPipe)
│   ├── accessibility/      # AccessibilityService (screen read/control)
│   ├── media/              # Media3 MediaController
│   ├── agent/              # Agent loop (observe→plan→act→verify)
│   ├── storage/            # Room (encrypted) + DataStore
│   ├── phone/              # Calls, SMS, alarms, toggles
│   ├── macros/             # Voice macros
│   ├── ocr/                # ML Kit OCR
│   ├── notifications/      # NotificationListener + triage
│   └── settings/           # Settings repository
├── shared/
│   ├── common/             # Data classes, Result<T>, constants
│   └── logging/            # Structured logging + action audit
├── ui/
│   ├── main/               # MainActivity + Compose UI
│   └── settings/           # SettingsActivity + Compose UI
└── tests/
    └── integration/        # Integration tests
```

## Test Plan

### Unit Tests
- `FastPathMatcher` — all intent categories
- `PromptBuilder` — prompt templates
- `ActionExecutor` — action parsing
- `PhoneController` — toggle logic
- `SettingsStore` — persistence

### Integration Tests (Require Device)
1. **Wake Word → STT → "Open App" → TTS**
   - Start app, grant permissions
   - Say "Hey NaNa" → verify wake word detection
   - Say "Open Settings" → verify app opens
   - Verify TTS confirmation

2. **Media Control**
   - Start music in Spotify/YouTube Music
   - Say "Hey NaNa, what's playing?" → verify correct title/artist
   - Say "Next song" → verify track changes
   - Say "Pause" → verify paused

3. **WhatsApp Flow**
   - Have WhatsApp with test contact "Test Contact"
   - Say "Hey NaNa, open WhatsApp, find Test Contact, read last message"
   - Verify message read aloud
   - Say "Reply 'Test message'"
   - Verify draft read back
   - Say "Confirm"
   - Verify sent

4. **Screen Reading**
   - Open any app with text (Chrome, Settings)
   - Say "Hey NaNa, what's on my screen?"
   - Verify summary spoken

5. **Macro Execution**
   - Create macro "Good night" → DND on, alarm 7AM, brightness 10%
   - Say "Hey NaNa, good night"
   - Verify all three actions executed

6. **Notification Triage**
   - Send test notification
   - Say "Hey NaNa, read notifications"
   - Verify summary
   - Say "Reply 'OK'" to last
   - Verify reply sent

7. **Battery Aware Mode**
   - Enable battery aware mode
   - Simulate low battery (or test on device <20%)
   - Verify reduced wake word sensitivity, smaller model selected

### Demo Script
```bash
# 1. Play next song
"Hey NaNa, next song"

# 2. What's playing
"Hey NaNa, what's playing?"

# 3. Open WhatsApp, find contact, read last message, reply
"Hey NaNa, open WhatsApp, find Alice, read last message, reply 'See you soon', confirm"
```

## Known Limitations

1. **LLM Latency**: On-device 1-3B models take 1-3s on mid-tier devices. Fast path handles common commands (<500ms).

2. **Accessibility API Limits**: Some apps (banking, secure folders) block accessibility. NaNa detects and refuses.

3. **WhatsApp Structure Changes**: UI selectors may break on WhatsApp updates. Uses text/content-desc matching as fallback.

4. **No Internet**: Cannot fetch weather, search web, or sync cloud data. Purely local.

5. **Model Size**: LLM models require 1-3GB storage and 1-2.5GB RAM. Not suitable for low-end devices (<3GB RAM).

6. **Wake Phrase Recognition**: Detection uses the selected Vosk model; accuracy depends on the model language and phrase vocabulary.

7. **STT Accuracy**: Vosk small model ~90% WER on clear speech; degrades with noise/accents.

8. **Notification Replies**: Only works for notifications with `RemoteInput` (messaging apps).

9. **Screen OCR**: Requires MediaProjection permission (user dialog each session unless granted).

10. **Multilingual**: Requires separate STT/TTS models per language; not dynamic.

## Next Steps

### Short Term
- [ ] Complete llama.cpp JNI integration for LLM
- [ ] Add Piper TTS ONNX Runtime integration
- [ ] Implement WhatsApp-specific UI selectors with fallbacks
- [ ] Add macro editor UI
- [ ] Implement action history viewer UI

### Medium Term
- [ ] Support for more messaging apps (Signal, Telegram, SMS)
- [ ] On-device intent classification model (replace fast path rules)
- [ ] Streaming STT → LLM → TTS pipeline for lower latency
- [ ] Widget for quick actions
- [ ] Wear OS companion for remote control

### Long Term
- [ ] Federated learning for personalization (on-device)
- [ ] Plugin system for custom skills
- [ ] Cross-device sync via local network (optional)
- [ ] Support for more languages (STT/TTS model packs)
- [ ] Accessibility service fallback for non-root screen capture

## License

MIT License — see LICENSE file.

## Acknowledgments

- [Vosk models](https://alphacephei.com/vosk/models) — Offline speech recognition and wake phrases
- [Vosk](https://alphacephei.com/vosk/) — Offline STT
- [llama.cpp](https://github.com/ggerganov/llama.cpp) — LLM inference
- [Piper](https://github.com/rhasspy/piper) — Offline TTS
- [ML Kit](https://developers.google.com/ml-kit) — OCR
- [Media3](https://developer.android.com/media/media3) — Media control
- [Material 3](https://m3.material.io/) — UI components
