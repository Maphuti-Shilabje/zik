<p align="center">
  <img src="zik-wordmark-assets/png/primary/zik-wordmark-primary-dark@2x.png" alt="Zik Wordmark" width="420"/>
</p>

<p align="center">
  <a href="https://www.android.com/"><img src="https://img.shields.io/badge/Platform-Android-3DDC84?logo=android&logoColor=white" alt="Platform"/></a>
  <a href="https://kotlinlang.org/"><img src="https://img.shields.io/badge/Language-Kotlin-7F52FF?logo=kotlin&logoColor=white" alt="Language"/></a>
  <a href="https://developer.android.com/jetpack/compose"><img src="https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4?logo=jetpackcompose&logoColor=white" alt="UI"/></a>
  <a href="https://developer.android.com/media/media3"><img src="https://img.shields.io/badge/Engine-AndroidX%20Media3%20ExoPlayer-FF6F00" alt="Audio Engine"/></a>
  <a href="LICENSE"><img src="https://img.shields.io/badge/License-Apache%202.0-blue.svg" alt="License"/></a>
  <a href="Mandate.md"><img src="https://img.shields.io/badge/Telemetry-ZERO-brightgreen" alt="Privacy"/></a>
</p>

<p align="center">
  <a href="https://github.com/Maphuti-Shilabje/zik/releases/latest/download/zik-v1.0.0.apk">
    <img src="https://img.shields.io/badge/Download%20APK-v1.0.0%20(Latest)-357ABD?style=for-the-badge&logo=android&logoColor=white" alt="Download APK"/>
  </a>
</p>

> **Straight talk.** You paid for the phone. You deserve a music app that respects that. No AI fluff, no telemetry, no cloud lock-in. Just pure, offline music playback with an uncompromising glassmorphic AMOLED aesthetic.

---

## Direct Download & Installation

You can download and install the Zik APK directly on any Android device without cloning or compiling the source code:

* **Direct APK Download**: [Download zik-v1.0.0.apk](https://github.com/Maphuti-Shilabje/zik/releases/latest/download/zik-v1.0.0.apk)
* **Latest Binary Mirror**: [Download zik-latest.apk](https://github.com/Maphuti-Shilabje/zik/releases/latest/download/zik-latest.apk)
* **All Releases**: [GitHub Releases Page](https://github.com/Maphuti-Shilabje/zik/releases)

### SHA-256 Checksum Verification
```text
983cfe1308f87de6c6882edfff8bc7e5bdb6ebff3945ff6b54573e9a73a8abd3  zik-v1.0.0.apk
```

---

## Key Features

### 100% Air-Gapped Privacy & Offline-First
* **Zero Internet Permissions**: `android.permission.INTERNET` is completely absent from the manifest. No data leaves your device—ever.
* **Minimal Permissions**: Only requests access to local audio files. Zero location, zero contacts, zero telemetry SDKs.

### Glassmorphic & AMOLED Pure Black UI
* **AMOLED Pure Black (`#000000`)**: Deep blacks maximize battery life and contrast on OLED displays.
* **Thin-Glass Surfaces**: Translucent tile surfaces with 1px micro-sheen borders.
* **3D Cover Flip**: Tap the center artwork card to flip it 180 degrees into an Apple Music-style kinetic scrolling lyrics view.
* **Harmonic Sine-Wave Seekbar**: Live mathematical sine wave progress indicator that oscillates during playback with smooth touch scrubbing.
* **Gesture Navigation**: Swipe left or right anywhere across the center modal to skip tracks.

### 5-Band Parametric Equalizer & Audio FX Suite
* **Real-Time Spline Visualizer**: Dynamic cubic-bezier frequency curve rendering real-time response on Canvas.
* **5-Band Graphic EQ**: Independent vertical slider bars (`60Hz`, `230Hz`, `910Hz`, `3.6kHz`, `14kHz`) with dB readouts and guaranteed 0 dB Flat curve snapping.
* **One-Tap Presets**: Flat, Rock, Pop, Jazz, Electronic, Vocal, Bass Boost, Classical, and Custom.
* **Bass Boost & 3D Surround Virtualizer**: Sub-bass rumble enhancement and spatial widening.

### Audio Codec & Metadata Inspector
* **Low-Level Hardware Probe**: Analyzes exact container format, bitrate, sample rate, bit depth, channel configuration, lossless/lossy status, and Hi-Res badges.
* **Storage Path Actions**: View formatted file sizes and copy direct local storage paths to clipboard with one tap.

### Sleep Timer with Gradual Volume Fade-Out
* **Smart Volume Fade-Out**: Smoothly eases volume from 100% down to 0% linearly over the final 30 seconds before pausing.
* **End of Current Track Mode**: Automatically calculates remaining track duration and stops playback at the end of the song.

### Home Screen Music Widgets
* **AMOLED Glass Widget Card**: Resizable 4x1/4x2 widget displaying rounded album artwork, track title, artist name, and playback transport controls.
* **Direct Service Synchronization**: Synchronized directly with `PlaybackService`.

### Persistent State & Cold Boot Restoration
* **Automatic State Recovery**: Preserves last active track, playback position, and queue across process restarts.
* **Reorderable Tabs**: Drag-and-drop category tabs (*Folders, Favorites, Songs, Albums, Artists*) with real-time neighbor slot displacement and persistent order storage.
* **System-Wide Favorites**: Dedicated Favorites tab with instant heart toggles.
* **Smart Metadata Sanitizer**: Automatically cleans messy download prefixes, `[y2mate]`, `(320kbps)`, and file extensions.

---

## Tech Stack & Architecture

* **UI Framework**: [Jetpack Compose](https://developer.android.com/jetpack/compose) with Material 3 Design Tokens
* **Audio Playback**: [AndroidX Media3](https://developer.android.com/media/media3) (`MediaSessionService`, `ExoPlayer`)
* **Audio FX**: Android Native `android.media.audiofx.Equalizer`, `BassBoost`, `Virtualizer`
* **Architecture**: MVVM with Unidirectional Data Flow (`StateFlow`, Coroutines)
* **Image Loading**: [Coil Compose](https://coil-kt.github.io/coil/compose/) (local embedded artwork decoding)
* **Storage Access**: Android `MediaStore` API with recursive folder indexing

---

## Building from Source

### Prerequisites
* Android Studio Ladybug / Meerkat or later
* Android SDK 34+
* JDK 17

### Build Steps

1. Clone the repository:
   ```bash
   git clone https://github.com/Maphuti-Shilabje/zik.git
   cd zik
   ```

2. Build debug APK:
   ```bash
   ./gradlew assembleDebug
   ```

3. Run unit tests:
   ```bash
   ./gradlew test
   ```

4. Install directly to a connected Android device:
   ```bash
   ./gradlew installDebug
   ```

The compiled APK will be located at:
```text
app/build/outputs/apk/debug/app-debug.apk
```

---

## Philosophy & Mandate

Read our complete product principles in [Mandate.md](Mandate.md):
- **Zero Ads. Zero Tracking. Zero Cloud Gatekeeping.**
- **Offline First. Battery First. Respect User Attention.**

---

## License

```text
Copyright 2026 Maphuti Shilabje

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
