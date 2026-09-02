# EDC pocket — Android app (build the APK here)

Open this folder in **Android Studio** and build an APK. The native app talks HTTP to the house host directly, so the browser mixed-content block does not apply.

See **[ROADMAP.md](ROADMAP.md)** for the full product plan (Phases 0–12 + moonshots).

## Build APK

1. Android Studio → **Open** → this repository folder.
2. Let Gradle sync (needs network the first time).
3. **Build → Build Bundle(s) / APK(s) → Build APK(s)**.
4. Install `app/build/outputs/apk/debug/app-debug.apk` on the phone (allow unknown sources).
5. Open **EDC pocket**. Pick **Mike** or **Mhairi**. Tap **Home Wi-Fi**. Test connection.

From the command line: `./gradlew assembleDebug`. The APK lands in the same `app/build/outputs/apk/debug/` folder.

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

See **[ROADMAP.md](ROADMAP.md)** for Phases 6–12 (next: 1.0 release, then everyday delight & live sync).

Client only. Never hosts.

## Hosts

- Home: `http://192.168.0.99:8765`
- Away: `http://100.70.53.87:8765` — Tailscale must be connected on the phone

minSdk 31. applicationId `house.edc.pocket`.
