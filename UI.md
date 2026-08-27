Direct. Clean. Functional. Here’s exactly how to build a modern minimalist UI that doesn’t insult adult users:

### 🎨 Design Tokens (Hard Values)
- **Background:** `#000000` (true black for AMOLED)  
- **Surfaces:** `#0A0A0A`, `#141414`, `#1F1F1F` (step by 8-bit increments, never gradients)  
- **Text:** `#FFFFFF` (primary), `#A0A0A0` (secondary), `#606060` (disabled)  
- **Accent:** `#4A90D9` (muted blue) OR desaturated extraction from album art at ≤40% opacity. Never neon.  
- **Spacing:** Strict 8pt grid. Use `4`, `8`, `12`, `16`, `24`, `32`, `48`. No arbitrary values.  
- **Corners:** `4dp` for lists/cards, `8dp` for buttons, `12dp` for player bar. No pills unless it’s a single toggle.

### 🔤 Typography
- **Font:** `Inter` or `Roboto Flex` (variable weight, free, system-friendly)  
- **Scale:** 3 tiers max per screen  
  - Title: `20sp`, `Medium`, line-height `1.25`  
  - Body: `16sp`, `Regular`, line-height `1.4`  
  - Caption/Meta: `12sp`, `Regular`, line-height `1.33`, tracking `+0.5`  
- **Rules:** Never use ALL CAPS. Never use light/ultralight weights for UI. Keep contrast ≥ `4.5:1`.

### 📐 Layout & Navigation
- **Bottom Nav:** Exactly 4 items. `Library` | `Search` | `Playlists` | `Settings`. No badges, no animations, fixed height `64dp`.  
- **Mini-Player:** Persistent `48dp` bar at bottom when browsing. Swipe up → full player. Tap → play/pause.  
- **Player Screen:**  
  - Top `60%`: Album art (square, `8dp` radius, no shadow) + title/artist centered  
  - Bottom `40%`: Progress bar (`2dp` track, `3dp` fill), transport controls (`48x48dp` touch targets), hidden advanced panel (EQ/crossfade) behind a single `⚙️` icon or swipe down  
- **No Carousels. No "For You". No horizontal scrolling feeds.** Lists are vertical. Folders are default.

### 🖱️ Interactions & Motion
- **Duration:** `200-250ms` max  
- **Easing:** `FastOutSlowIn` (Compose: `MotionTokens.StandardEasing`)  
- **Only animate:** Screen transitions, play/pause state, progress scrubbing, bottom sheet slide  
- **Never animate:** Backgrounds, icons, text, album art scaling, list items on scroll  
- **Haptics:** `VibrationEffect.createPredefined(Effect.CLICK)` on play/pause, skip, playlist create. Keep it tactile, not gamified.

### 🛠️ Compose Implementation Rules
```kotlin
// Override Material3 defaults immediately
val AppTheme = MaterialTheme(
    colorScheme = darkColorScheme(
        background = Color(0xFF000000),
        surface = Color(0xFF141414),
        onSurface = Color(0xFFFFFFFF),
        primary = Color(0xFF4A90D9)
    ),
    typography = Typography(
        titleLarge = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Medium, lineHeight = 25.sp),
        bodyLarge = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Normal, lineHeight = 22.sp),
        labelSmall = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Normal, lineHeight = 16.sp)
    ),
    shapes = Shapes(
        small = RoundedCornerShape(4.dp),
        medium = RoundedCornerShape(8.dp)
    )
)
```
- Use `Modifier.clickable`, `Modifier.semantics` for accessibility, not `Button` everywhere.  
- `LazyColumn` with `itemContentPadding = PaddingValues(16.dp)` for breathing room.  
- `rememberLazyListState` to preserve scroll position across navigation.  
- `Crossfade` or `AnimatedContent` only for player screen transitions. No shared element explosions.

### 🚫 Hard Anti-Patterns (Delete on Sight)
- Glassmorphism, blur backgrounds, drop shadows > `1dp`  
- Gradient buttons, animated icons, confetti, "level up" badges  
- Hidden gesture-only controls that break discoverability  
- Bottom sheets stacked on bottom sheets  
- Onboarding slides, rating prompts, "enable notifications" walls  
- Album art with heavy drop shadows, rounded corners > `12dp`, or aspect ratio distortion  
- Dynamic color that overrides text contrast or creates pastel mush

### ✅ Final Check Before Shipping UI
1. Does every screen work at `12px` font size?  
2. Can a user operate it with one thumb without scrolling past 3 screens?  
3. Is the album art the loudest visual element, or is your UI competing with it?  
4. Does it look identical at 2AM and 2PM? (AMOLED black should be absolute.)

Build it flat, functional, and quiet. Let the music be the UI. If it doesn’t serve playback, metadata, or navigation, cut it.
