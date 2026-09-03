# Product Principles & Technical Mandate

Straight talk. You paid for the phone. You deserve a music app that respects that. No AI fluff, no "smart" nonsense, no telemetry. Here is what matters:

---

## 1. Non-Negotiables (The Foundation)
* **Zero Ads. Zero Tracking. Zero Internet Permission Required**: `android.permission.INTERNET` is omitted from the manifest. No data leaves your device—ever.
* **Offline-First Architecture**: Operates 100% without connectivity. No cloud dependency, no sign-in gates.
* **Minimal Permissions**: Only requests local audio storage access (`READ_MEDIA_AUDIO` / `READ_EXTERNAL_STORAGE`). No location, contacts, or phone state.
* **Zero Emojis Mandate**: Strict technical, clean UI typography across the entire codebase and interface.

---

## 2. Library & Organization
* **Folder-Based Browsing as a First-Class View**: Direct directory drill-down with automatic subfolder tracking.
* **Reorderable Category Tabs**: Customizable tab order (*Folders, Favorites, Songs, Albums, Artists*) with real-time displacement and persistent storage.
* **Smart Metadata Sanitization**: Heuristically strips download junk, web tags, bitrates, and video artifacts from track titles while preserving file paths.
* **Fast Alphabetical Navigation**: Interactive side-rail scrubber with magnification preview.
* **Batch Operations**: Multi-select mode for adding or playing multiple tracks in bulk.

---

## 3. Playback & Audio Engineering
* **AndroidX Media3 Architecture**: Low-latency `MediaSessionService` with foreground service binding and seamless Bluetooth/car integration.
* **Parametric 5-Band Equalizer**: Real-time spline curve visualizer with guaranteed 0 dB flat reset and persistent gain levels.
* **Audio FX Suite**: Native Bass Boost and 3D Surround Virtualizer.
* **High-Res Codec & Metadata Inspector**: Low-level stream probe detecting FLAC, MP3, AAC, Opus, sample rate, bit depth, bitrates, channels, and lossless status.
* **Smart Sleep Timer**: Gradual volume fade-out over the final 30 seconds and End-of-Current-Track mode.
* **Synchronized Lyrics**: Kinetic scrolling with companion `.lrc` file detection and 3D card flip.

---

## 4. Design & UI System
* **AMOLED Pure Black (`#000000`)**: True black backgrounds across all screens, modals, sheets, and widgets for maximum OLED power efficiency.
* **Transparent Glass Surfaces**: 1px crisp micro-borders (`BorderStroke(1.dp, Color.White.copy(alpha = 0.12f..0.22f))`) with zero milky grey washes.
* **Muted Blue Accent (`#4A90D9` / `#357ABD`)**: Restrained color accents for active indicators and audio pulses.
* **Full-Screen Modal Scrims & Pointer Isolation**: Clean touch boundaries preventing ghost clicks across background layers.
* **Home Screen Music Widget**: Glance/RemoteViews playback control synchronized with `PlaybackService`.
