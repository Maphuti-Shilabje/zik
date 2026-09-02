# Zik 🎵

[![Platform](https://img.shields.io/badge/Platform-Android-3DDC84?logo=android&logoColor=white)](https://www.android.com/)
[![Language](https://img.shields.io/badge/Language-Kotlin-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![UI](https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4?logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Audio](https://img.shields.io/badge/Engine-AndroidX%20Media3%20ExoPlayer-FF6F00)](https://developer.android.com/media/media3)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)
[![Privacy](https://img.shields.io/badge/Telemetry-ZERO-brightgreen)](Mandate.md)

> **Straight talk.** You paid for the phone. You deserve a music app that respects that. No AI fluff, no telemetry, no cloud lock-in. Just pure, offline music playback with an uncompromising glassmorphic AMOLED aesthetic.

---

## ✨ Features

### 🔒 100% Air-Gapped Privacy & Offline-First
* **Zero Internet Permissions**: `android.permission.INTERNET` is completely absent from the manifest. No data leaves your device—ever.
* **Minimal Permissions**: Only requests access to local audio files. Zero location, zero contacts, zero telemetry SDKs.

### 🎨 Glassmorphic & AMOLED Pure Black UI
* **AMOLED Pure Black (`#000000`)**: Deep blacks maximize battery life and contrast on OLED displays.
* **Thin-Glass Surfaces**: Translucent tile surfaces with 1px micro-sheen borders.
* **3D Cover Flip**: Tap the center artwork card to flip it 180° into an Apple Music-style kinetic scrolling lyrics view.
* **Harmonic Sine-Wave Seekbar**: Live mathematical sine wave progress indicator that oscillates during playback with smooth touch scrubbing.
* **Gesture Navigation**: Swipe left or right anywhere across the center modal to skip tracks.

### 🎚️ 5-Band Parametric Equalizer & Audio FX
* **Real-time Spline Visualizer**: Dynamic cubic-bezier frequency curve rendering real-time response on Canvas.
* **5-Band Graphic EQ**: Independent vertical slider bars (`60Hz`, `230Hz`, `910Hz`, `3.6kHz`, `14kHz`) with dB readouts.
* **One-Tap Presets**: *Flat, Rock, Pop, Jazz, Electronic, Vocal, Bass Boost, Classical, and Custom*.
* **Bass Boost & 3D Surround Virtualizer**: Sub-bass rumble enhancement and spatial widening.
* **Persistent Engine**: Custom curves and audio effect levels are automatically preserved across app restarts.

### 📋 Live Playback Queue
* **Slide-Up Queue Sheet**: View your upcoming playlist from the top bar queue button.
* **Pinned "Now Playing" Card**: Highlights active track with animated equalizer waves.
* **Queue Controls**: Instant tap-to-jump, one-tap item removal, and "Clear Next" button.
* **Quick Actions**: 3-dots popup on every track with *Play Next* and *Add to Queue*.

### 🗂️ First-Class Library & Organization
* **Folder-First Navigation**: Drill down into folders directly without metadata fragmentation.
* **Reorderable Tabs**: Drag-and-drop category tabs (*Folders, Favorites, Songs, Albums*) with real-time neighbor slot displacement.
* **System-Wide Favorites**: Dedicated Favorites tab with instant heart toggles.
* **Alphabetical Fast-Scroller**: Interactive side-rail scrubber with touch magnifier bubble.
* **Multi-Select Batch Mode**: Long-press any track to enter multi-select mode and queue batches at once.
* **Smart Metadata Sanitizer**: Automatically cleans messy download prefixes, `[y2mate]`, `(320kbps)`, and file extensions.

---

## 🛠️ Tech Stack & Architecture

* **UI Framework**: [Jetpack Compose](https://developer.android.com/jetpack/compose) with Material 3 Design Tokens
* **Audio Playback**: [AndroidX Media3](https://developer.android.com/media/media3) (`MediaSessionService`, `ExoPlayer`)
* **Audio FX**: Android Native `android.media.audiofx.Equalizer`, `BassBoost`, `Virtualizer`
* **Architecture**: MVVM with Unidirectional Data Flow (`StateFlow`, `SharedFlow`, Coroutines)
* **Image Loading**: [Coil Compose](https://coil-kt.github.io/coil/compose/) (local embedded artwork decoding)
* **Storage Access**: Android `MediaStore` API with recursive folder indexing

---

## 🚀 Getting Started

### Prerequisites
* Android Studio Ladybug / Meerkat or later
* Android SDK 34+
* JDK 17

### Building from Source

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

## 📜 Philosophy & Mandate

Read our complete product principles in [Mandate.md](Mandate.md):
- **Zero Ads. Zero Tracking. Zero Cloud Gatekeeping.**
- **Offline First. Battery First. Respect User Attention.**

---

## 📄 License

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
