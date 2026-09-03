# Technology Stack & Architecture Specification

Architecture: Native Android with Kotlin, Jetpack Compose Material 3, and AndroidX Media3.

---

## 1. Core Architecture Stack

| Layer | Technology | Rationale |
|---|---|---|
| **Language** | Kotlin (Coroutines, StateFlow) | Null-safe, modern asynchronous state management, zero callback hell |
| **UI Framework** | Jetpack Compose Material 3 | Declarative, AMOLED pure-black theme, custom Canvas rendering, hardware-accelerated gestures |
| **Audio Engine** | AndroidX Media3 (ExoPlayer) | Low-latency playback, gapless transitions, MediaSessionService lifecycle, notification synchronization |
| **Audio Effects** | Android Native AudioFX | Hardware-attached `Equalizer`, `BassBoost`, `Virtualizer`, and `LoudnessEnhancer` |
| **Persistence** | SharedPreferences (`AppPreferences`) | Fast, lightweight offline persistence for tab order, favorites, playback state, and user settings |
| **Metadata Extraction**| Android `MediaExtractor` & `MediaMetadataRetriever` | Low-level stream probing for codec specs, sample rate, bit depth, and storage paths |
| **Image Loading** | Coil Compose | Local embedded album art decoding with fast in-memory caching |
| **Widgets** | Android AppWidget Provider (RemoteViews) | Resizable home screen playback controls with low-latency `PendingIntent` triggers |

---

## 2. Hard Anti-Patterns & Exclusions

* **No Cross-Platform Bridges**: Flutter, React Native, and KMP are excluded to avoid audio session latency, Bluetooth lag, and background service fragmentation.
* **Zero Telemetry SDKs**: Firebase Analytics, Mixpanel, Sentry, and cloud SDKs are strictly prohibited.
* **Zero Internet Permissions**: `android.permission.INTERNET` is completely omitted from `AndroidManifest.xml`.
* **Zero Emojis**: Emojis are barred from the codebase, UI labels, resources, and documentation.
* **Zero Opaque/Grey Regressions**: Backgrounds must strictly remain AMOLED Pure Black (`#000000`), with transparent glass cards.

---

## 3. Storage & Permissions

Only the following permissions are declared:
* `READ_MEDIA_AUDIO` (Android 13+ / API 33+)
* `READ_EXTERNAL_STORAGE` (Android 12 and below)
* `POST_NOTIFICATIONS` (Android 13+ for playback controls)
* `FOREGROUND_SERVICE` and `FOREGROUND_SERVICE_MEDIA_PLAYBACK`
