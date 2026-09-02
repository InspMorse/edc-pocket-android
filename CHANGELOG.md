# Changelog

All notable changes to **EDC pocket** are documented here.

Format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).  
Versioning: **0.x** = pre-release phases · **1.0** = Phase 6 trust release · **1.x–2.x** = Phases 7–12.

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
