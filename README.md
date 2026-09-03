# EDC pocket — frozen at v2.0 (reference)

**Status: dormant.** Native Android is **out of programme**. Android Studio is gone from the house.

The phone client of record is the host page on New (HTTP, same origin as the APIs):

- Home: `http://192.168.0.99:8765/pocket`
- Also: `http://192.168.0.99:8765/m`
- Away: Tailscale to New, then `/pocket` — live IP is in the host repo (`AGENT.md` / `POCKET.md`), not a hardcoded address here.

No APK required. The browser mixed-content block does not apply because `/pocket` is served from the same HTTP origin as `/api/*`.

This repository keeps the finished **v2.0** native app as a reference snapshot (Phases 0–12). Do not expect APK builds, Play uploads, or further native work unless Mike reopens the programme.

| | |
|---|---|
| **Last release** | **v2.0** (`versionName` 2.0, `versionCode` 13) |
| **Merged** | [PR #2](https://github.com/InspMorse/edc-pocket-android/pull/2) — Phase 12 trust at scale |
| **Host work** | Honest `/api/health`, ETag, theme, rate limits live in the **Basic** host repo as **H1** — not here |
| **Ignore** | Draft [PR #1](https://github.com/InspMorse/edc-pocket-android/pull/1) (Cloud Agent env) — superseded by v2.0 on `main` |

Roadmap freeze: [ROADMAP.md](ROADMAP.md). Trust docs (historical): [HOST_API.md](HOST_API.md), [SECURITY.md](SECURITY.md), [BETA.md](BETA.md). Automation intents: [AUTOMATION.md](AUTOMATION.md).

Client only. Never hosts.

---

# Historical: Android app (build the APK here)

Open this folder in **Android Studio** and build an APK. The native app talks HTTP to the house host directly, so the browser mixed-content block does not apply.

See **[ROADMAP.md](ROADMAP.md)** for the product plan (Phases 0–12 + moonshots). All phases are **done**.

## Build APK

1. Android Studio → **Open** → this repository folder.
2. Let Gradle sync (needs network the first time).
3. **Build → Build Bundle(s) / APK(s) → Build APK(s)**.
4. Install `app/build/outputs/apk/debug/app-debug.apk` on the phone (allow unknown sources).
5. Open **EDC pocket**. Pick **Mike** or **Mhairi**. Tap **Home Wi-Fi**. Test connection.

From the command line:

```bash
./gradlew testDebugUnitTest assembleDebug   # tests + debug APK
./gradlew assembleRelease                   # release APK (see signing below)
```

Debug APK: `app/build/outputs/apk/debug/app-debug.apk`  
Release APK: `app/build/outputs/apk/release/app-release.apk`

### Signed release (optional)

1. Copy `keystore.properties.example` → `keystore.properties` and fill in your keystore paths/passwords.
2. Run `./gradlew assembleRelease`.

Without `keystore.properties`, release builds use the debug keystore (fine for sideloading, not for Play Store).

CI runs on every push/PR — see [`.github/workflows/ci.yml`](.github/workflows/ci.yml).

## What it does

- **Clip** — latest house clipboard, filter by person, tap links, expand long text, copy/share, send (manual only)
- **List** — shopping / to-do, tap to tick, remove done items, copy/share the whole list
- **Send** — text/link to clipboard or list; camera or library photo to Incoming (optional session folder); tap Incoming files to open
- **Settings** — Home Wi-Fi / Away / Custom, test connection via `/api/health`, find host (tries Home then Away)
- **Share** — from any app, **Send to EDC** (choose clipboard or list; photos go to Incoming)
- **Background** — refreshes every 5s while open; keeps last known data if the host drops offline
- **v0.2** — adaptive icon, app shortcuts, pull-to-refresh, tap-to-copy, haptics, host info in Settings
- **v0.3** — auto Home/Away, offline outbox, network re-probe, smarter polling, URL validation
- **v0.4** — home screen widget, Quick Settings tile, background clip alerts
- **v0.5** — Incoming thumbnails, multi-photo, share session, clip search, undo delete
- **v0.6** — host capability discovery, graceful UI degradation, identity sync, dashboard deep links, optional HTTPS
- **v1.0** — integration tests, Compose smoke test, CI, release build, changelog ([CHANGELOG.md](CHANGELOG.md))
- **v1.1** — onboarding, swipe actions, pins, list sort, rich clips, host theme accent, tablet layout
- **v1.3** — Room cache, conditional fetch, outbox v2, connection doctor, staleness UI, SSE client
- **v1.5** — widget v2, lock screen widget, QS tiles, persistent preview, share v2, NFC, automation API
- **v1.7** — multi-host profiles, mDNS discovery, MagicDNS, QR pairing, dashboard WebView, guest mode, biometric lock, TLS pinning
- **v1.9** — todo notes/aisles/recurrence, session gallery, bulk Incoming, doc/barcode scan, markdown clips
- **v2.0** — audit log, data export/reset, feature flags, telemetry opt-in, HOST_API/SECURITY/BETA docs

## Privacy & permissions

EDC pocket is a **personal house client**. It talks only to hosts you configure (Home LAN, Tailscale Away, or Custom URL). No third-party analytics or cloud backend in the app.

| Permission | Why |
|------------|-----|
| **INTERNET** | Read/write clipboard, list, and Incoming on your house host |
| **ACCESS_NETWORK_STATE** | Auto Home/Away switching when Wi-Fi or VPN changes |
| **POST_NOTIFICATIONS** | Optional "new clip" alerts (Android 13+; only if you enable background poll) |
| **Camera** *(optional hardware)* | Send tab — take a photo to Incoming; not required to install |

**Data stored on device:** identity name, host URL preset, clip filter, outbox queue, widget/notification cache, and DataStore preferences. Queued photos are copied to app storage until sent.

**Cleartext HTTP:** enabled so the app can reach typical home LAN hosts (`http://192.168.x.x`). Use the Settings **Use HTTPS** toggle when your host serves TLS.

## Hosts (historical native-app defaults)

- Home: `http://192.168.0.99:8765`
- Away: confirm the live Tailscale IP in the host repo before using any address baked into old APKs

minSdk 31. applicationId `house.edc.pocket`.
