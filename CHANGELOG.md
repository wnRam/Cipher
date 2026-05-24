# Changelog

All notable changes to Cipher are documented here. Format based on
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/); the project
follows [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.0] – Initial release

### Added
- **Multiplayer over Wi-Fi**
  - Host / join room flow with 6-digit codes and QR codes.
  - NSD service discovery (`_ciphergame._tcp.`) over mDNS.
  - Heartbeat-based connection monitoring (5 s ping / 15 s timeout).
  - Role reveal with `FLAG_SECURE` protection against screenshots.
  - Voting phase with auto-tally on timer expiry.
  - Spy's last-chance location guess after being voted out.
  - End-of-round result + persistent scoreboard for the session.
- **Offline Mode** — single-device pass-the-phone variant with its own
  setup / reveal / game / result flow, no network usage.
- **Settings** — dark / light theme toggle, sound on/off, language
  switching (en / ru / uz) without app restart.
- **Profile** — nickname + 12-icon avatar picker.
- **How to Play** — animated three-section guide covering gameplay,
  multiplayer setup, and Offline Mode.

### UX details
- Edge-to-edge layout with status-bar safe insets across every screen.
- Subtle motion: pulsing accents, shimmering primary CTAs, staggered
  fade-in for How to Play sections, animated gold underlines on section
  headers, gradient gold step badges.
- Back-press in Offline Mode game shows a confirm dialog and pauses the
  timer while the user decides.

### Engineering
- Single-Activity Compose architecture with Hilt + nested navigation
  graphs.
- Per-frequency Compose state split in `GameViewModel` (timer state
  isolated from low-frequency UI state) so per-second ticks don't
  invalidate the player list or action buttons.
- All `MutableSharedFlow` navigation channels use `replay = 0` to prevent
  stale signals after rotation / reattachment.
- Lifecycle-aware Flow collection (`collectAsStateWithLifecycle`)
  throughout.
- QR generation moved off-thread; bulk `setPixels` (~50–100× faster than
  per-pixel writes); source `Bitmap` recycled on `LobbyViewModel.onCleared()`.
- Buffered socket I/O in `HostSocketManager` and `PeerSocketManager`;
  JSON encoding + writes off the main thread.
- Live Wi-Fi connectivity observation via
  `ConnectivityManager.NetworkCallback`.
- `SoundPool` auto-paused / auto-resumed with the foreground Activity.
- Crash guards: empty-pack `randomOrNull`, atomic double-tap guards on
  Call Vote / End Round / Start Game.
- R8 + resource shrinking enabled for release builds.
- ProGuard rules covering Hilt, kotlinx.serialization, ZXing, ML Kit,
  CameraX.

### Languages
- English, Russian, Uzbek — full strings.xml parity.

### Known limitations
- `SoundManager` does not ship audio assets yet; calls are no-ops.
- Release build is signed with the debug key for sideloading; swap in a
  real keystore before publishing.
- Multiplayer requires both devices on the same Wi-Fi network with
  client-to-client traffic allowed. Captive / enterprise / guest
  networks that block multicast or peer traffic will fail discovery.

[1.0.0]: https://example.invalid/cipher/releases/tag/v1.0.0
