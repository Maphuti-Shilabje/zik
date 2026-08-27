Straight answer: **Native Android with Kotlin + Jetpack Compose + AndroidX Media3**. Nothing else gives you the low-level audio control, system integration, and zero-bloat footprint you're demanding.

Here's the exact stack, layer by layer:

### 🔧 Core Stack
| Layer | Tech | Why |
|-------|------|-----|
| **Language** | Kotlin | Null-safe, coroutines, official Android standard, mature ecosystem |
| **UI** | Jetpack Compose | Declarative, fast rendering, built-in dark/AMOLED modes, gesture handling is clean, no XML overhead |
| **Audio Engine** | AndroidX Media3 (ExoPlayer successor) | Industry standard for gapless, crossfade, FLAC/ALAC/MP3/OGG, handles MediaSession, Bluetooth, Car Mode, and lock-screen controls natively |
| **Database** | Room (SQLite) | Offline-first, fast indexing, full-text search, perfect for large libraries + playlists + play history |
| **Async/State** | Coroutines + StateFlow | Efficient background scanning, smooth UI state, no callback hell |
| **Image Loading** | Coil | Async album art fetching (local only if you block network), Compose-optimized, lightweight |
| **DI** | Koin or Hilt | Keep it lean. Koin is simpler; Hilt is more structured. Pick one, don't overcomplicate |

### 🚫 What to Explicitly Skip
- **Flutter/React Native/KMP**: Audio players need tight MediaSession integration, precise gapless timing, and system-level Bluetooth/Car hooks. Cross-platform bridges add latency, break gapless/EQ consistency, and complicate background services. Not worth it.
- **Rust/C++ audio core**: Overkill unless you're building a DSP-heavy audiophile player. Media3 + software EQ is enough for 99% of use cases.
- **Cloud sync/DRM/Telemetry SDKs**: You said offline-first. Keep `INTERNET` permission out of `AndroidManifest.xml`. Period.

### ⚙️ How to Handle the Hard Parts (No Hand-Waving)
1. **Gapless & Crossfade**  
   Media3 supports gapless natively for MP3/FLAC/ALAC if you configure `DefaultLoadControl` and disable prefetch flushing. Crossfade requires a custom `AudioProcessor` that overlaps the last ~2s of track A with the first ~2s of track B. Implement it in Media3's `AudioRenderer` pipeline.

2. **EQ & ReplayGain**  
   - Android's hardware `Equalizer` API is fragmented. For consistent results, implement a **software EQ** using `AudioTrack` or Media3's `AudioProcessor` chain. Store user curves in Room, apply per-track.
   - ReplayGain: Read `REPLAYGAIN_TRACK_GAIN`/`ALBUM_GAIN` tags via `MediaMetadataRetriever` or `jaudiotagger`, apply gain offset in your DSP pipeline before output.

3. **Library Scanning**  
   - Use `ContentResolver` + `MediaStore.Audio.Media.EXTERNAL_CONTENT_URI` (Android 10+) or `READ_MEDIA_AUDIO` (13+).
   - Cache paths, durations, tags, and folder structure in Room.
   - Run scans via `WorkManager` with `Constraints` (idle/charging optional). Exclude `Android/data`, `WhatsApp`, etc. via user-configurable blocklist.

4. **Permissions**  
   Only request:
   - `READ_MEDIA_AUDIO` (API 33+)
   - `POST_NOTIFICATIONS` (if you want optional playback notifications)
   Zero others. If the app tries to access network, location, or phone state, the system will block it if the permission isn't declared. Declare nothing.

5. **Widgets & Car Mode**  
   - Widgets: Use `RemoteViews` + `MediaSession` controller. Keep it static (now playing, prev/next/pause). No live Compose in widgets.
   - Car Mode: Trigger via Bluetooth HFP profile change, USB audio connection, or manual toggle. Load a simplified Compose layout with 48dp+ touch targets, high contrast, no scrolling.

### 📦 Minimal Viable Build Order
1. Kotlin + Compose skeleton + Media3 player service
2. `ContentResolver` scanner → Room cache → Folder view
3. Gapless playback + MediaSession (lock screen/Bluetooth controls)
4. EQ + ReplayGain software pipeline
5. Playlists + M3U import/export
6. Widgets + Car mode toggle
7. Polish: gestures, AMOLED dark, battery-optimized scanning

### 🛠️ Libraries to Actually Use
- `androidx.media3:media3-exoplayer`
- `androidx.room:room-runtime`
- `androidx.work:work-runtime-kotlin`
- `androidx.lifecycle:lifecycle-viewmodel-compose`
- `io.coil-kt:coil-compose`
- `org.jaudiotagger:jaudiotagger` (for deep metadata editing)
- `com.github.matsuyoshi30:ripple` (optional, for clean tap feedback)

### 🔑 Final Rule
Keep `INTERNET` out of the manifest. If you ever need lyrics/album art fetching, make it a **separate optional plugin** that requests network permission at runtime. Core app stays air-gapped.

Build the player you're forced to tolerate every day. Ship v1 with folders, gapless, EQ, zero ads. Iterate from user feedback. The stack above will scale without turning into bloated crapware.
