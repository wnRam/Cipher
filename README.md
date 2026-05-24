# Cipher

> **Social deduction. One spy. One location. No internet required.**

Cipher is an Android party game inspired by *Spyfall*. Up to ten players share
a secret location — except for the spy, who must blend in and uncover the
location before being caught. The app supports **local multiplayer over
Wi-Fi** (no internet, no server) and a **single-device Offline Mode** where
players pass the phone around.

Built with Jetpack Compose, Hilt, and Kotlin Coroutines. Designed for Android
8.0+ (API 26).

---

## Features

### 🎮 Two ways to play
- **Multiplayer over Wi-Fi** — host creates a room, peers join via QR code or
  6-digit room code. Discovery uses NSD (Bonjour-style mDNS); messaging is
  raw TCP with a JSON line protocol. Works on any LAN — no internet needed.
- **Offline Mode** — single device, pass the phone around. Player count,
  word pack, spy count, and round length are all configurable per round.

### 🎭 Game design
- **Configurable word packs** — Locations, Food & Drink, Sports, Movies, plus
  custom packs you define inline.
- **Custom roles** — define your own list of roles (with isSpy flags) for
  variant rules.
- **Win conditions** — toggle "Spy wins by hiding" and "Spy wins by guessing
  the location" independently.
- **Adjustable timer** — 1 to 15 minutes per round.
- **1–3 spies** with automatic bound clamping by player count.

### 🌐 Localization
- English, Russian, Uzbek — full translations including UI, How-to-Play,
  and error messages.
- Language switching happens live (no app restart).

### 🎨 Design
- "Spy-thriller luxury" palette: deep ink + gold accents.
- Cormorant-Garamond-style serif display + DM-Sans body + Space Mono code.
- Edge-to-edge layout with status-bar safe insets on every screen.
- Subtle motion: pulsing accents, staggered fade-ins, shimmering CTAs,
  animated section headers in the How-to-Play screen.

### 🔐 Privacy & data
- **No network requests off your local Wi-Fi.** No analytics, no telemetry,
  no third-party SDKs reporting home.
- `FLAG_SECURE` is applied during role reveal so screenshots and
  screen-recordings of the spy's secret are blocked at the OS level.
- All preferences (nickname, avatar, language, theme, sound) stored locally
  via Jetpack DataStore.

### ⚡ Performance
- Compose state split per render frequency so per-second timer ticks don't
  invalidate the player list.
- Lifecycle-aware Flow collection (`collectAsStateWithLifecycle`).
- QR code generation off-thread with bulk `setPixels` (~50–100× faster than
  the naive per-pixel write).
- Buffered socket I/O, JSON encoding off the main thread.
- SoundPool auto-pause / auto-resume tied to app foreground.
- See [Performance audit notes](#performance-audit-notes) below.

---

## Screenshots

Add screenshots under `docs/screenshots/` and reference them here. Suggested
shots: Home, Create Room (Game Rules tab), Lobby with QR, Role Reveal,
Game Screen with timer, Voting, Round Result, Offline Setup, Offline Reveal,
How to Play scrolling.

```
docs/screenshots/01-home.png
docs/screenshots/02-create-room.png
docs/screenshots/03-lobby.png
…
```

---

## Tech Stack

| Layer            | Choice                                                |
| ---------------- | ----------------------------------------------------- |
| UI               | Jetpack Compose, Material 3                           |
| Navigation       | Navigation Compose (nested graph for Offline Mode)    |
| DI               | Hilt (KSP)                                            |
| Async            | Kotlin Coroutines + Flow                              |
| Persistence      | Jetpack DataStore (Preferences)                       |
| Serialization    | kotlinx.serialization (JSON, polymorphic sealed class)|
| Discovery        | Android NSD (`_ciphergame._tcp.`)                     |
| Transport        | Plain TCP (`ServerSocket` / `Socket`), line-delimited |
| QR generation    | ZXing core                                            |
| QR scanning      | CameraX + Google ML Kit Barcode                       |
| Min / Target SDK | 26 / 34 (compileSdk 36)                               |
| Kotlin / JVM     | Kotlin 2.x, JVM 17                                    |

---

## Architecture

```
app/src/main/java/uz/angrykitten/spygame/
├── MainActivity.kt              ← Single-Activity, Compose entry point
├── SpyApp.kt                    ← @HiltAndroidApp, locale + SoundPool wiring
├── di/                          ← Hilt module (Singleton bindings)
├── data/                        ← PreferencesRepository, SessionRepository,
│                                  WordPackRepository
├── model/                       ← @Serializable domain types + sealed Message
├── network/                     ← NsdHelper, HostSocketManager,
│                                  PeerSocketManager, NetworkRepository
├── navigation/AppNavGraph.kt    ← All routes incl. offline sub-graph
├── ui/
│   ├── home/                    ← Landing + Wi-Fi banner
│   ├── profile/                 ← Nickname + avatar
│   ├── howtoplay/               ← Animated rules screen
│   ├── settings/                ← Theme / sound / language
│   ├── room/
│   │   ├── create/              ← Game-rules + word-pack + role editor
│   │   ├── join/                ← Keypad + QR scanner
│   │   └── lobby/               ← QR + player list + Start
│   ├── game/
│   │   ├── reveal/              ← Own-Device role reveal (multiplayer)
│   │   ├── playing/             ← Timer + player list + Call Vote
│   │   ├── voting/              ← Vote grid + tally
│   │   └── result/              ← Spy reveal + spy-guess mini-game
│   ├── scoreboard/              ← End-of-game leaderboard
│   ├── offline/                 ← OfflineModeViewModel + 4 screens
│   ├── components/              ← Cipher buttons, AvatarIcons
│   └── theme/                   ← Colors, Typography, Theme
├── sound/SoundManager.kt        ← SoundPool wrapper
└── util/                        ← QRGenerator, QRScanner
```

**State separation in `GameViewModel`:** `GameUiState` (round, players,
isHost) and `GameTimerState` (timerSeconds, isPaused) are exposed as
independent `StateFlow`s so per-second ticks only invalidate the timer
composable.

**Single source of truth:** `SessionRepository` (singleton) holds the
current `GameState`. ViewModels read snapshots and call `updateGameState()`
when they mutate.

**Network protocol:** newline-delimited JSON over TCP. Every message
extends the sealed `Message` class with a `@SerialName`-tagged discriminator
so peers on different app versions stay compatible as long as the message
names haven't changed.

---

## Build & Run

### Prerequisites
- Android Studio Ladybug (or newer) with the Android Gradle Plugin matching
  the version in [`gradle/libs.versions.toml`](gradle/libs.versions.toml).
- JDK 17.
- An Android device or emulator running **API 26+** (Android 8.0).
- Wi-Fi for multiplayer. Both devices must be on the **same network** with
  client-to-client traffic allowed (most home routers do; some captive /
  enterprise networks block multicast or peer traffic).

### Run a debug build
```bash
./gradlew :app:installDebug
```

### Build a release APK
```bash
./gradlew :app:assembleRelease
```
The release APK lands in `app/build/outputs/apk/release/`. It is **R8 +
resource-shrunk** and currently signed with the debug key for sideloading
convenience — replace with a real keystore before publishing (see
[Release signing](#release-signing) below).

### Build a release AAB (for Play Store)
```bash
./gradlew :app:bundleRelease
```
Output: `app/build/outputs/bundle/release/app-release.aab`.

---

## Release signing

The `release` build type in `app/build.gradle.kts` currently inherits the
debug signing config. Before publishing, replace it with a real keystore.

1. Generate a keystore (one-time):
   ```bash
   keytool -genkey -v -keystore cipher-release.keystore \
       -alias cipher -keyalg RSA -keysize 2048 -validity 10000
   ```

2. Create `keystore.properties` at the repo root (already covered by
   `.gitignore`):
   ```properties
   storeFile=cipher-release.keystore
   storePassword=…
   keyAlias=cipher
   keyPassword=…
   ```

3. Replace the `release { signingConfig = signingConfigs.getByName("debug") }`
   line in `app/build.gradle.kts` with a `signingConfigs.create("release") { … }`
   block that reads from `keystore.properties`. Example:
   ```kotlin
   val keystoreProps = Properties().apply {
       val f = rootProject.file("keystore.properties")
       if (f.exists()) load(f.inputStream())
   }
   signingConfigs {
       if (keystoreProps.containsKey("storeFile")) {
           create("release") {
               storeFile = rootProject.file(keystoreProps["storeFile"] as String)
               storePassword = keystoreProps["storePassword"] as String
               keyAlias = keystoreProps["keyAlias"] as String
               keyPassword = keystoreProps["keyPassword"] as String
           }
       }
   }
   buildTypes {
       release {
           signingConfig = signingConfigs.findByName("release")
               ?: signingConfigs.getByName("debug")
           // …rest unchanged
       }
   }
   ```

**Never commit your keystore or `keystore.properties` to version control.**

---

## Permissions

Declared in [`AndroidManifest.xml`](app/src/main/AndroidManifest.xml):

| Permission                           | Why                                                  |
| ------------------------------------ | ---------------------------------------------------- |
| `INTERNET`                           | Local TCP sockets between host and peers             |
| `ACCESS_WIFI_STATE`                  | Detect Wi-Fi connectivity for the Home banner        |
| `CHANGE_WIFI_MULTICAST_STATE`        | NSD service discovery uses multicast DNS             |
| `ACCESS_NETWORK_STATE`               | `ConnectivityManager.NetworkCallback` for live state |
| `CAMERA` *(runtime, only when used)* | Scanning the host's QR code in Join Room             |

No background services. No location, contacts, storage, or notification
permissions are requested.

---

## Translations

Strings live in:
- `app/src/main/res/values/strings.xml` (English, default)
- `app/src/main/res/values-ru/strings.xml` (Russian)
- `app/src/main/res/values-uz/strings.xml` (Uzbek)

When adding strings, update all three files together. Plurals should use
`<plurals>`; placeholders use positional indices (`%1$d`, `%2$s`) so they
survive word-order changes between languages.

---

## Performance audit notes

The full optimization pass is recorded in commit history; key takeaways:

- `GameViewModel` state split per render frequency.
- `SoundManager.play` no longer calls `runBlocking { preferencesRepository… }`
  every tick — sound-enabled is now a cached `StateFlow`.
- All navigation `MutableSharedFlow`s use `replay = 0` so rotation /
  reattachment never re-fires a stale one-shot event.
- QR bitmap generated off-thread and exposed as a cached `ImageBitmap`;
  source `Bitmap` recycled on `LobbyViewModel.onCleared()`.
- WiFi connectivity is now observed live via `ConnectivityManager.NetworkCallback`
  rather than checked once.
- R8 + resource shrinking enabled for release builds.
- ProGuard rules cover Hilt, kotlinx.serialization, ZXing, ML Kit, CameraX.

---

## Roadmap

Out of scope for v1.0 but possible follow-ups:

- Real audio files for `SoundManager` (currently no-op placeholders).
- Persistent multi-round scoreboard with history.
- Add'l word packs (per locale).
- BLE fallback when both devices are off Wi-Fi.
- Tablet-optimized layouts.

---

## License

Add a `LICENSE` file before publishing. MIT or Apache-2.0 are the usual
choices for indie Android apps.

---

## Acknowledgments

Inspired by the tabletop game **Spyfall** (Alexandr Ushan, 2014). This is an
unaffiliated reimplementation built for friends and Wi-Fi parties.
