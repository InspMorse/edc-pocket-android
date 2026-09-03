# EDC pocket — automation & surfaces API

Use these intents from **Tasker**, **MacroDroid**, **NFC Tools**, or shell `am broadcast`.

All automation broadcasts use package `house.edc.pocket`.

## Broadcast actions (Tasker / MacroDroid)

| Action | Extra | Effect |
|--------|-------|--------|
| `house.edc.pocket.action.AUTOMATION_COPY` | — | Copy latest house clip to phone clipboard |
| `house.edc.pocket.action.AUTOMATION_SEND_CLIP` | `android.intent.extra.TEXT` | Send text to house clipboard |
| `house.edc.pocket.action.AUTOMATION_SEND_LIST` | `android.intent.extra.TEXT` | Add text to shared to-do list |
| `house.edc.pocket.action.AUTOMATION_OPEN_LIST` | — | Open EDC pocket on List tab |
| `house.edc.pocket.action.AUTOMATION_OPEN_SEND` | — | Open EDC pocket on Send tab |

Example (adb):

```bash
adb shell am broadcast -a house.edc.pocket.action.AUTOMATION_COPY
adb shell am broadcast -a house.edc.pocket.action.AUTOMATION_SEND_LIST --es android.intent.extra.TEXT "milk"
```

## Activity shortcuts (launcher / Assistant)

| Action | Extra | Effect |
|--------|-------|--------|
| `house.edc.pocket.action.COPY_LATEST` | — | Copy latest clip |
| `house.edc.pocket.action.OPEN_LIST` | — | Open list tab |
| `house.edc.pocket.action.OPEN_SEND` | — | Open send tab |
| `house.edc.pocket.action.OPEN_SEND_CAMERA` | — | Open send tab + camera |
| `house.edc.pocket.action.SEND_TO_LIST` | `android.intent.extra.TEXT` | Send/add list item |
| `house.edc.pocket.action.SEND_TO_CLIP` | `android.intent.extra.TEXT` | Send to clipboard |

## NFC URI scheme

Write NDEF URI records to tags:

| URI | Effect |
|-----|--------|
| `edc://copy` | Copy latest clip |
| `edc://open` | Open app |
| `edc://list` | Open list tab |
| `edc://send?text=hello` | Send text to house clipboard |

If the tag has no URI, the default action from **Settings → NFC tag action** is used.

## Wear OS data layer

When Google Play services is available, the phone publishes:

- Path: `/edc/latest_clip`
- Fields: `text`, `from`, `ts`, `updatedAt`

A Wear tile/complication can read this DataItem for “copy latest on watch” (watch app not bundled yet).

## Direct share session shortcuts

In **Settings**, pin comma-separated session names (e.g. `holiday, receipts`).  
Each becomes a **direct share** target for photos → Incoming in that folder.

## Widget & tiles

- **Home widget** — resizeable; optional open-todo count; tap action configurable in Settings
- **Lock screen widget** — add from widget picker (Android 14+ where supported)
- **Quick Settings** — add tiles: **EDC clip**, **EDC list**, **EDC photo**
