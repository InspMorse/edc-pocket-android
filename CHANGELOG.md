# Changelog

All notable changes to **EDC pocket** are documented here.

Format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).  
Versioning: **0.x** = pre-release phases · **1.0** = Phase 6 trust release · **1.x–2.x** = Phases 7–12.

---

## [1.5.0] — Phase 9

### Added

- Widget v2 — open todo count, configurable tap action (open app / list / clip / copy)
- Lock screen Glance widget (where OS supports keyguard widgets)
- Quick Settings tiles — **EDC list**, **EDC photo** (plus existing EDC clip)
- Optional persistent notification with latest clip preview
- Share target v2 — default destination + skip chooser; remembers last choice
- Voice/App shortcut — “Send to house list”
- Tasker/MacroDroid automation broadcasts — see [AUTOMATION.md](AUTOMATION.md)
- Wear Data Layer publisher for `/edc/latest_clip` (phone-side bridge)
- NFC dispatch for `edc://copy`, `edc://open`, `edc://list`, `edc://send`
- Direct share shortcuts for pinned Incoming session folders

---

## [1.3.0] — Phase 8

### Added

- Room cache for clipboard, list, and incoming with last-synced timestamps
- Conditional fetch via `ETag` / `If-Modified-Since` (`SyncCoordinator`)
- Outbox v2 — per-item retry count, failure reason, exponential backoff
- Connection doctor in Settings (per-endpoint latency + copy debug log)
- Staleness UI — “cached 5 min ago” in status bar and offline banner
- Conflict hint when house data changed while queued sends were pending
- Adaptive foreground/background sync (`SyncPolicy`) — slower poll when SSE active
- SSE live stream client (`/api/events`) with graceful fallback to polling
- FCM push registration stub when host exposes `push` capability *(needs Firebase + host)*

## [1.1.0] — Phase 7

### Added

- First-run onboarding wizard (identity, host, connection test, widget/tile tips)
- Swipe clip rows for copy / share / dashboard; swipe todos to complete
- Rich clip actions — link domain preview, call phone numbers, open addresses in Maps
- Pin / star clips and list items (stored locally)
- List sort modes and person filter
- Clearer offline and host error messages
- Host accent colour from health/capabilities JSON
- Tablet two-pane layout (clip + list side by side on wide screens)
- Animated clip expand and haptic feedback on pin/swipe/onboarding

---

## [1.0.0] — Phase 6

### Added

- MockWebServer integration tests for `EdcClient` (health, load, send, capabilities merge)
- Compose UI smoke test (main tabs render offline)
- GitHub Actions CI — unit tests, debug + release APK builds
- Release signing via optional `keystore.properties` (see `keystore.properties.example`)
- Privacy & permissions section in README
- This changelog

### Changed

- Version **1.0** (`versionCode` 7) — first trust/release milestone

---

## [0.6.0] — Phase 5

### Added

- Host capability discovery from `/api/health` and optional `/api/capabilities`
- Graceful UI degradation when host features are off
- Identity chips synced from host `users` when exposed
- Dashboard deep links on clip and list rows
- Optional HTTPS toggle in Settings

---

## [0.5.0] — Phase 4

### Added

- Incoming image thumbnails (Coil)
- Multi-photo upload with progress
- Share target session folder prompt and `ACTION_SEND_MULTIPLE`
- Download / share / save Incoming files
- Clip history search
- Undo delete for done todos

---

## [0.4.0] — Phase 3

### Added

- Home screen widget (latest clip preview)
- Quick Settings tile — copy house clipboard
- Background clip notifications (WorkManager)
- Configurable background poll modes

---

## [0.3.0] — Phase 2

### Added

- Auto Home / Away host switching
- Offline outbox for clip, list, and photo sends
- Network change re-probe
- Smarter polling intervals
- Custom URL validation and Tailscale hints

---

## [0.2.0] — Phase 1

### Added

- Adaptive launcher icon and share-target icon
- App shortcuts (send to clip/list, copy latest)
- Pull-to-refresh, tap-to-copy, haptics
- Host info and dashboard button in Settings
- Clip filter memory and connection label

---

## [0.1.0] — Phase 0

### Added

- Initial Android client — Clip, List, Send, Settings
- HTTP integration with Everyday Clipboard host
- Share target, health probe, find host, polling

[1.0.0]: https://github.com/InspMorse/edc-pocket-android/compare/v0.6.0...v1.0.0
[0.6.0]: https://github.com/InspMorse/edc-pocket-android/compare/v0.5.0...v0.6.0
[0.5.0]: https://github.com/InspMorse/edc-pocket-android/compare/v0.4.0...v0.5.0
[0.4.0]: https://github.com/InspMorse/edc-pocket-android/compare/v0.3.0...v0.4.0
[0.3.0]: https://github.com/InspMorse/edc-pocket-android/compare/v0.2.0...v0.3.0
[0.2.0]: https://github.com/InspMorse/edc-pocket-android/compare/v0.1.0...v0.2.0
[0.1.0]: https://github.com/InspMorse/edc-pocket-android/releases/tag/v0.1.0
