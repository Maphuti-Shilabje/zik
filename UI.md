# Zik UI & Design System Specification

Direct. Clean. Functional. This document specifies the single source of truth for the Zik design system and user interface architecture across the entire codebase.

---

## 1. Design Tokens (Hard Values)

### Color Palette
* **Root Background**: `#000000` (True AMOLED Pure Black across all screens, modals, bottom sheets, and widgets).
* **Card & Tile Surfaces**: `Color.Transparent` (Zero milky washes or grey backgrounds; all components use transparent glass).
* **Glass Micro-Borders**: `BorderStroke(1.dp, Color.White.copy(alpha = 0.12f..0.22f))` for clean, crisp edge definition.
* **Primary Text**: `#FFFFFF` (100% white for titles, track names, and primary labels).
* **Secondary Text**: `Color.White.copy(alpha = 0.60f..0.70f)` or `#A0A0A0` for artist names, album titles, and metadata.
* **Accent Color**: `#4A90D9` (Muted Blue) and `#357ABD` (Accent Indicator for active tabs, sliders, toggles, and audio pulse).
* **Selection / Badge Fill**: `Color.White.copy(alpha = 0.10f..0.15f)`.

### Typography
* **Font Family**: System default (Roboto / Inter variable weights).
* **Scale**:
  * Screen Titles: `22sp`, `FontWeight.Bold`, `Color.White`
  * Section Headers: `11sp`, `FontWeight.Bold`, `AccentMutedBlue`, all uppercase
  * Primary Track Titles: `15sp - 16sp`, `FontWeight.SemiBold`, `Color.White`
  * Secondary Metadata / Subtitles: `12sp - 13sp`, `FontWeight.Normal`, `Color.White.copy(alpha = 0.65f)`
  * Micro Badges & Timestamps: `10sp - 11sp`, `FontWeight.Medium`

### Corner Radii
* **18dp**: Track list items, tile cards, dialog surfaces, equalizer cards, bottom sheet containers.
* **20dp**: Selection mode context bars.
* **22dp**: Search input pill.
* **26dp**: Floating mini-player bar.
* **CircleShape**: Circular icon buttons, back arrows, and slider thumbs.

---

## 2. Layout & Screen Architecture

### MainScreen (`MainScreen.kt`)
* Single-screen core managing the root AMOLED pure-black frame.
* Touch isolation and non-blocking `detectTapGestures` root traps across foreground overlays.
* Full-screen modal scrims (`PureBlack.copy(alpha = 0.65f)`) with tap-outside dismissal for bottom sheets.
* Automatic `LocalFocusManager.clearFocus()` on screen transitions to prevent keyboard leaks.

### Library View (`LibraryScreen.kt`)
* **Header**: Compact transparent brand wordmark (`ic_zik_wordmark_compact`), search pill, and settings button.
* **Category Tabs**: Horizontal reorderable tab row (`Folders`, `Favorites`, `Songs`, `Albums`, `Artists`) with real-time neighbor displacement.
* **Fast Scroller**: Touch-interactive alphabetical scrubber with magnification bubble.
* **Track Item**: Artwork thumbnail, title, artist/album metadata, heart toggle, and 3-dots popup menu (`Play Next`, `Add to Queue`, `Song Info`).

### Persistent Mini-Player (`MiniPlayer.kt`)
* AMOLED Pure Black (`#000000`) floating surface with 1px glass border.
* Top-edge linear micro-progress line (`AccentMutedBlue`).
* Album artwork, scrolling marquee title/artist, play/pause toggle, and skip next.

### Expanded Full-Screen Player (`ExpandedPlayer.kt`)
* Ambient artwork backdrop with glassmorphic gradient depth.
* **3D Card Flip**: Center artwork flips 180 degrees into a kinetic auto-scrolling synced lyrics view.
* **Sine-Wave Seekbar**: Live mathematical harmonic oscillator track bar with touch scrubbing.
* **Floating Capsule Pill**: Holds Sleep Timer, Audio Inspector, Equalizer, and Loop buttons cleanly above the waveform.
* **Queue Sheet**: Slide-up modal sheet displaying upcoming tracks with reorder, swipe-to-delete, and jump-to-index.

### Equalizer & Audio FX Suite (`EqualizerScreen.kt`)
* Real-time cubic-bezier spline curve rendered directly on Canvas.
* 5 vertical graphic slider bands (`60Hz`, `230Hz`, `910Hz`, `3.6kHz`, `14kHz`).
* Guaranteed 0 dB "Flat" reset action.
* Bass Boost and 3D Surround Virtualizer dials.

### Audio Inspector Dialog (`AudioInfoSheet.kt`)
* Low-level hardware probe extracting container codec (FLAC, MP3, AAC, Opus), sample rate, bit depth, bitrate, channels, and Hi-Res status.
* Full local file path with one-tap clipboard copy action.

### Sleep Timer (`SleepTimerSheet.kt`)
* Linear volume fade-out easing volume down to 0% over the final 30 seconds before pausing.
* End of Current Track mode.

### Home Screen Widget (`ZikMusicWidgetProvider.kt`)
* 4x1 and 4x2 AMOLED pure-black widget synchronized in real-time with `PlaybackService`.

---

## 3. Mandatory Engineering & Style Rules

1. **Zero Emojis**: Never use emojis in code, UI strings, dialogs, resources, or documentation.
2. **Zero Internet**: Never declare `android.permission.INTERNET`. All metadata and lyrics must be processed locally.
3. **Zero Slate/Grey Regressions**: Never introduce slate grey backgrounds (`#0F131D`, `#181C26`, `#0A0D14`, `#121212`, `#1E1E1E`) or milky opaque card washes.
4. **State Persistence**: All user settings, tab orders, favorites, and playback states must persist across cold boots via `AppPreferences`.
