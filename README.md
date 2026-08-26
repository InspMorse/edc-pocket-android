# EDC pocket — Android app (build the APK here)

Open this folder in **Android Studio** and build an APK. The native app talks HTTP to the house host directly, so the browser mixed-content block does not apply.

## Build APK

1. Android Studio → **Open** → this repository folder.
2. Let Gradle sync (needs network the first time).
3. **Build → Build Bundle(s) / APK(s) → Build APK(s)**.
4. Install `app/build/outputs/apk/debug/app-debug.apk` on the phone (allow unknown sources).
5. Open **EDC pocket**. Pick **Mike** or **Mhairi**. Tap **Home Wi-Fi**. Test connection.

## What it does

- **Clip** — latest house clipboard, copy, send (manual only)
- **List** — shopping / to-do, tap to tick, add item
- **Send** — text/link to clipboard or list; camera or library photo to Incoming
- **Settings** — Home Wi-Fi / Away / Custom, identity sticky on this phone
- **Share** — from any app, **Send to EDC** (text, links, photos)

Client only. Never hosts.

## Hosts

- Home: `http://192.168.0.99:8765`
- Away: `http://100.70.53.87:8765` — Tailscale must be connected on the phone

minSdk 31. applicationId `house.edc.pocket`.
