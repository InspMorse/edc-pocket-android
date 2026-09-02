# EDC pocket — product roadmap

Phone-side client for the house **Everyday Clipboard** host. The app never hosts — it only talks HTTP to the house.

**Hosts**

- Home: `http://192.168.0.99:8765`
- Away: `http://100.70.53.87:8765` (Tailscale on the phone)

---

## Vision

Mike and Mhairi can read and write the **house clipboard**, manage the shared **to-do list**, send text/links/photos into **Incoming**, and **share from any app** — at home or away — with graceful behaviour when the host is offline.

---

## Phase 0 — Foundation (done)

| Area | Status |
|------|--------|
| Core tabs | Clip, List, Send, Settings |
| Host APIs | clipboard, todo, incoming, health, todo/text, delete todo |
| Connectivity | Health probe, Find host (Home → Away), 5s polling, stale cache |
| Clip UX | Filters, links, expand, share |
| List UX | Add, tick, remove done, share whole list |
| Send UX | To clip/list, camera/library, session folder, open Incoming |
| Share target | Chooser: clipboard or list; photos → Incoming |
| Build | Gradle wrapper, unit tests, debug APK |

## At a glance

| | |
|---|---|
| **Current release** | **v0.6** (Phase 5) |
| **Shipped** | Phases **0–5** — foundation through host capabilities |
| **Next milestone** | Phase **6** → tagged **1.0** |
| **Through Phase 12** | **73** remaining items (6.1–12.10) |
| **Carried forward** | 5.6 FCM push → **8.3** (needs host API) |

**Branch:** `cursor/finish-edc-pocket-android-f18a` · **PR:** [#2](https://github.com/InspMorse/edc-pocket-android/pull/2)

---

## Remaining roadmap (Phases 6–12)

Everything from **now** to **2.0**. Status: **Next** = immediate; **Planned** = after prior phase ships.

| Phase | Target version | Goal | Items | Status |
|-------|----------------|------|-------|--------|
| **6** | **1.0** | Quality, trust & release | 6.1–6.8 | **Next** |
| **7** | 1.1–1.2 | Everyday delight | 7.1–7.10 | Planned |
| **8** | 1.3–1.4 | Always in sync | 8.1–8.9 (+ 5.6) | Planned |
| **9** | 1.5–1.6 | More surfaces | 9.1–9.10 | Planned |
| **10** | 1.7–1.8 | Smarter house | 10.1–10.10 | Planned |
| **11** | 1.9–2.0 | Beyond clipboard | 11.1–11.10 | Planned |
| **12** | 2.x | Trust at scale | 12.1–12.10 | Planned |

**Spine:** `Phase 6 (1.0) → 7 → 8 → 9 → 10 → 11 → 12 (2.0)`

**Host-dependent batches** (coordinate Everyday Clipboard host repo):

- **8.2–8.4** — conditional fetch, FCM, SSE/WebSocket  
- **10.2, 10.4, 10.6** — mDNS, QR pairing, theme/branding  
- **11.1–11.5, 11.8** — richer todo/incoming APIs  

**Client-only batches** (no host changes required):

- **6** entire phase · **7** entire phase · **9.1, 9.3–9.5, 9.7** · **8.1, 8.6, 8.8–8.9** · **12.3–12.6, 12.9**

---

## Phase 1 — Polish & identity (done)

**Goal:** Feel native on Pixel; faster everyday actions.

| # | Item | Status |
|---|------|--------|
| 1.1 | Adaptive launcher icon (foreground/background/round/monochrome) | Done |
| 1.2 | Share-target icon | Done |
| 1.3 | App shortcuts (send to clip/list, copy latest) | Done |
| 1.4 | Pull-to-refresh on Clip + List | Done |
| 1.5 | Tap clip row → copy | Done |
| 1.6 | Haptic feedback on send/tick/delete | Done |
| 1.7 | Host info in Settings (version, hostname) | Done |
| 1.8 | Open dashboard button | Done |
| 1.9 | Remember clip filter per phone | Done |
| 1.10 | Connection label (Home LAN / Away / Offline cached) | Done |

**Exit criteria:** Icon correct on Pixel launcher; fewer taps for daily actions; Settings shows live host identity.

---

## Phase 2 — Smart connectivity (done)

**Goal:** Right host, right network, no lost sends.

| # | Item | Status |
|---|------|--------|
| 2.1 | Auto Home/Away on Wi‑Fi or Tailscale | Done |
| 2.2 | Re-probe on network change | Done |
| 2.3 | Offline outbox for clip/list/photo sends | Done |
| 2.4 | Outbox UI (pending, retry, clear) | Done |
| 2.5 | Smarter polling (back off when idle) | Done |
| 2.6 | Flush outbox on resume | Done |
| 2.7 | Custom URL validation | Done |
| 2.8 | Tailscale hint when Away fails | Done |

**Exit criteria:** Phone switches Home/Away without manual Find host; sends survive brief outages.

---

## Phase 3 — Glanceable access (done)

**Goal:** Use the house clipboard without opening the full app.

| # | Item | Status |
|---|------|--------|
| 3.1 | Home screen widget — latest clip preview | Done |
| 3.2 | Widget actions — Copy latest, Open app | Done |
| 3.3 | Quick Settings tile — copy house clipboard | Done |
| 3.4 | Optional notification on new clip (WorkManager) | Done |
| 3.5 | Notification actions — Copy / Open | Done |
| 3.6 | Configurable background poll (off / conservative / active) | Done |

**Exit criteria:** Latest clip reachable from home screen or notification in one tap.

---

## Phase 4 — Richer send & receive (done)

**Goal:** Photos and Incoming feel first-class.

| # | Item | Status |
|---|------|--------|
| 4.1 | Incoming image thumbnails | Done |
| 4.2 | Multi-photo upload into session folder | Done |
| 4.3 | Share target: session folder prompt | Done |
| 4.4 | Share multiple images (`ACTION_SEND_MULTIPLE`) | Done |
| 4.5 | Download Incoming to phone / share out | Done |
| 4.6 | Upload progress for large photos | Done |
| 4.7 | Clip history search (local filter) | Done |
| 4.8 | Undo delete todo | Done |

---

## Phase 5 — Deeper house integration (done)

**Goal:** Pocket stays in sync as Everyday Clipboard evolves.

| # | Item | Status |
|---|------|--------|
| 5.1 | Capability discovery from health / future API | Done |
| 5.2 | Graceful degradation when endpoints missing | Done |
| 5.3 | Identity sync if host exposes users | Done |
| 5.4 | Deep link to host dashboard item | Done |
| 5.5 | Optional HTTPS to host | Done |
| 5.6 | Host push via FCM (needs host work) | Skipped |

---

## Phase 6 — Quality, trust & release (**next** → **1.0**)

**Goal:** Safe to install long-term; easy to ship updates.

| # | Item | Status |
|---|------|--------|
| 6.1 | EdcClient MockWebServer integration tests | Pending |
| 6.2 | Compose UI smoke tests | Pending |
| 6.3 | Signed release build | Pending |
| 6.4 | GitHub Actions CI | Pending |
| 6.5 | Changelog / GitHub Releases | Pending |
| 6.6 | Play internal track (optional) | Pending |
| 6.7 | Privacy / permissions audit in README | Pending |
| 6.8 | Crash reporting (optional) | Pending |

**Exit criteria:** Tagged **1.0** release APK; CI green on every PR; integration tests cover host client flows.

---

## Phase 7 — Everyday delight (1.1–1.2)

**Goal:** First-run and daily use feel obvious, fast, and pleasant — not just functional.

| # | Item | Status |
|---|------|--------|
| 7.1 | First-run onboarding (identity → host → test → optional widget/tile) | Pending |
| 7.2 | Swipe actions on clip rows (copy / share / dashboard) | Pending |
| 7.3 | Swipe-to-complete on open todos | Pending |
| 7.4 | Rich clip previews — link unfurl, phone → dial, address → maps | Pending |
| 7.5 | Pin / star clips and list items (local or host-backed if API exists) | Pending |
| 7.6 | List sorting & filters (open first, by person, by date) | Pending |
| 7.7 | Better empty states and error copy (“host asleep”, “Tailscale off”) | Pending |
| 7.8 | Material You / host accent colour sync (if host exposes theme) | Pending |
| 7.9 | Tablet & foldable two-pane layout (clip + list side by side) | Pending |
| 7.10 | Haptic & animation polish pass | Pending |

**Exit criteria:** A new phone owner reaches “copy latest clip” in under 60 seconds without reading docs.

---

## Phase 8 — Always in sync (1.3–1.4)

**Goal:** Data feels live; outages are invisible; bandwidth stays low.

| # | Item | Status |
|---|------|--------|
| 8.1 | Local Room cache with explicit “last synced” timestamps | Pending |
| 8.2 | Conditional fetch (`ETag` / `If-Modified-Since` when host supports) | Pending |
| 8.3 | Push via FCM — host notifies phone of clip/list/incoming changes *(was 5.6)* | Pending · host |
| 8.4 | SSE or WebSocket stream as alternative to polling | Pending · host |
| 8.5 | Smarter background sync — event-driven + adaptive intervals | Pending |
| 8.6 | Outbox v2 — per-item retry, exponential backoff, failure reasons | Pending |
| 8.7 | Conflict hints when host data changed while editing offline | Pending |
| 8.8 | Connection doctor in Settings (latency, per-endpoint status, export debug log) | Pending |
| 8.9 | Staleness UI — “cached 12 min ago” on every tab | Pending |

**Exit criteria:** With FCM or SSE enabled, clip updates appear without opening the app; airplane-mode sends still drain the outbox reliably.

---

## Phase 9 — More surfaces (1.5–1.6)

**Goal:** House clipboard reachable from anywhere on the phone — not only inside the app.

| # | Item | Status |
|---|------|--------|
| 9.1 | Widget v2 — configurable size, show open todo count, tap filter | Pending |
| 9.2 | Lock screen widget / glance (where OS allows) | Pending |
| 9.3 | Quick Settings tiles — open list, snap photo to Incoming | Pending |
| 9.4 | Persistent “connected” notification with latest clip preview (optional) | Pending |
| 9.5 | Share target v2 — remember last destination; skip chooser option | Pending |
| 9.6 | App Actions / voice — “send to house list”, “copy house clipboard” | Pending |
| 9.7 | Tasker / MacroDroid / automation intent API (documented) | Pending |
| 9.8 | Wear OS tile or complication — copy latest | Pending |
| 9.9 | NFC tag at the door — open app or copy latest | Pending |
| 9.10 | Direct share shortcuts per session folder | Pending |

**Exit criteria:** Three one-tap paths to “copy latest” exist outside the launcher (widget, tile, voice or NFC).

---

## Phase 10 — Smarter house (1.7–1.8)

**Goal:** Multiple hosts, zero-config discovery, and tighter coupling with the house dashboard.

| # | Item | Status |
|---|------|--------|
| 10.1 | Multiple host profiles (home, holiday, parents) with quick switcher | Pending |
| 10.2 | mDNS / Bonjour host discovery on LAN (“EDC on this network”) | Pending · host |
| 10.3 | Tailscale MagicDNS hostname instead of raw IP | Pending |
| 10.4 | QR pairing — scan dashboard QR to set Custom URL + trust host | Pending · host |
| 10.5 | Embedded dashboard WebView panel (optional tab or sheet) | Pending |
| 10.6 | Host branding sync — name, logo, accent from capabilities | Pending · host |
| 10.7 | Geofence or “at home” hint (Wi‑Fi SSID + optional location) | Pending |
| 10.8 | Guest / temporary identity with expiry | Pending |
| 10.9 | Biometric lock for app or sensitive clips | Pending |
| 10.10 | Certificate pinning for HTTPS hosts | Pending |

**Exit criteria:** Visiting a friend’s house EDC instance is a QR scan, not manual URL editing.

---

## Phase 11 — Beyond clipboard (1.9–2.0)

**Goal:** List and Incoming become household utilities, not side features.

| # | Item | Status |
|---|------|--------|
| 11.1 | Todo notes, due dates, and sub-items (host API permitting) | Pending · host |
| 11.2 | Recurring list items (“milk every week”) | Pending |
| 11.3 | Shopping categories / aisles view | Pending |
| 11.4 | Link a todo to a clip (“buy ingredients” → recipe URL) | Pending |
| 11.5 | Incoming — video upload, PDF preview, audio inline | Pending · host |
| 11.6 | Document scan → Incoming (ML Kit) | Pending |
| 11.7 | Barcode / QR scan → send to clip or list | Pending |
| 11.8 | Bulk Incoming actions (select, download zip, delete on host) | Pending · host |
| 11.9 | Session gallery view — photos grouped by session folder | Pending |
| 11.10 | Markdown or code-block rendering in clips | Pending |

**Exit criteria:** A grocery run uses only the List tab; a event photo dump uses session gallery end-to-end.

---

## Phase 12 — Trust at scale (2.x)

**Goal:** Safe for years of daily use; observable when things go wrong.

| # | Item | Status |
|---|------|--------|
| 12.1 | Structured audit log (who sent what, when — local or host) | Pending |
| 12.2 | Rate-limit and abuse hints from host | Pending · host |
| 12.3 | Data export (settings + cached clips/todos) | Pending |
| 12.4 | Clear-data / reset without orphan workers | Pending |
| 12.5 | Screenshot / golden UI tests for regressions | Pending |
| 12.6 | Beta channel (Firebase App Distribution or Play internal) | Pending |
| 12.7 | Feature flags driven by host capabilities | Pending |
| 12.8 | Optional anonymised crash + connectivity telemetry (opt-in) | Pending |
| 12.9 | Dependency & security update cadence documented | Pending |
| 12.10 | **2.0** — breaking-change policy for host API versions | Pending |

**Exit criteria:** You can diagnose “why didn’t my send arrive?” from in-app logs without adb.

---

## Moonshots & wild ideas

Nothing off the table — park here until a host API or strong user pull exists.

| Idea | Why it’s interesting |
|------|----------------------|
| **Reverse push** — host asks phone to snap a photo or share location | “What’s in the fridge?” from the dashboard |
| **House ↔ phone clip sync** — optional mirror of system clipboard when at home | True “unified clipboard” without manual send |
| **Live Activities / Dynamic Island style** — trip mode showing open list count | Shopping trip awareness |
| **Car mode** — voice-only list tick-off (Android Auto constraints) | Hands full at shops |
| **Shared clip reactions** 👍 on a link someone pasted | Lightweight social layer on the house clip |
| **Scheduled clip** — “show this on the house dashboard at 7am” | Reminders without a separate app |
| **Clip templates** — “add to list: milk, bread, eggs” macro | Faster shopping |
| **Incoming → print** — send to house printer if host exposes it | Recipes, tickets |
| **Home-screen clip stack** — scroll recent clips in widget | History without opening app |
| **Cross-house relay** — forward clip from home EDC to holiday EDC | Multi-property households |
| **Desktop pairing** — same identity on Windows/Mac via companion | Beyond Android |
| **Offline-first CRDT list** — merge edits without a central winner | Extreme reliability nerd goal |

---

## Version naming

| Version | Scope | Status |
|---------|--------|--------|
| **0.1** | Phase 0 | Shipped |
| **0.2** | Phase 1 | Shipped |
| **0.3** | Phase 2 | Shipped |
| **0.4** | Phase 3 | Shipped |
| **0.5** | Phase 4 | Shipped |
| **0.6** | Phase 5 | **Current** |
| **1.0** | Phase 6 | **Next** |
| **1.1–1.2** | Phase 7 | Planned |
| **1.3–1.4** | Phase 8 | Planned |
| **1.5–1.6** | Phase 9 | Planned |
| **1.7–1.8** | Phase 10 | Planned |
| **1.9–2.0** | Phase 11 | Planned |
| **2.x** | Phase 12 | Planned |

Patch releases (`1.3.1`) for fixes; minor bumps track phase batches.

### Full remaining checklist (6.1 → 12.10)

<details>
<summary>73 items — expand to scan</summary>

**Phase 6 (1.0):** 6.1 integration tests · 6.2 UI smoke tests · 6.3 signed release · 6.4 CI · 6.5 changelog/releases · 6.6 Play internal · 6.7 privacy audit · 6.8 crash reporting  

**Phase 7:** 7.1 onboarding · 7.2 clip swipes · 7.3 swipe complete · 7.4 rich previews · 7.5 pin/star · 7.6 list sort/filter · 7.7 error copy · 7.8 theme sync · 7.9 tablet layout · 7.10 animation polish  

**Phase 8:** 8.1 Room cache · 8.2 conditional fetch · 8.3 FCM · 8.4 SSE/WS · 8.5 adaptive sync · 8.6 outbox v2 · 8.7 conflict hints · 8.8 connection doctor · 8.9 staleness UI  

**Phase 9:** 9.1 widget v2 · 9.2 lock screen · 9.3 QS tiles · 9.4 persistent notif · 9.5 share v2 · 9.6 voice · 9.7 Tasker API · 9.8 Wear · 9.9 NFC · 9.10 session shortcuts  

**Phase 10:** 10.1 multi-host · 10.2 mDNS · 10.3 MagicDNS · 10.4 QR pair · 10.5 WebView dashboard · 10.6 branding · 10.7 geofence · 10.8 guest identity · 10.9 biometric · 10.10 cert pinning  

**Phase 11:** 11.1 todo notes/dates · 11.2 recurring · 11.3 categories · 11.4 todo↔clip link · 11.5 video/PDF/audio · 11.6 doc scan · 11.7 barcode · 11.8 bulk Incoming · 11.9 session gallery · 11.10 markdown clips  

**Phase 12:** 12.1 audit log · 12.2 rate limits · 12.3 export · 12.4 reset · 12.5 golden tests · 12.6 beta channel · 12.7 feature flags · 12.8 telemetry · 12.9 security cadence · 12.10 API 2.0 policy  

</details>

---

## Host API reference

| Endpoint | Used for |
|----------|----------|
| `GET /api/health` | Probe, find host, dashboard URL, capabilities |
| `GET /api/capabilities` | Optional richer caps, users, dashboard link templates |
| `GET /api/clipboard` | Clip tab, widget |
| `POST /api/clipboard` | Send, share target, outbox |
| `GET /api/todo` | List tab |
| `POST /api/todo` | Add, share target |
| `PATCH/POST /api/todo/{id}` | Toggle done |
| `DELETE /api/todo/{id}` | Remove done |
| `GET /api/todo/text` | Share whole list |
| `GET /api/incoming` | Incoming list |
| Upload paths | Photos to Incoming |

### Likely future host endpoints (client ready when host is)

| Endpoint | Enables |
|----------|---------|
| `GET /api/events` (SSE) or WebSocket | Phase 8 live sync |
| `POST /api/push/register` | Phase 8 FCM |
| `GET /api/clipboard/{id}` | Pin, deep metadata, reactions |
| `PATCH /api/todo/{id}` fields | Notes, due dates, categories |
| `GET /api/theme` | Phase 7/10 branding |
| `GET /.well-known/edc` or mDNS `_edc._tcp` | Phase 10 discovery |

---

## Delivery order

**Sequential spine (required order for major releases)**

```
v0.6 (now) → Phase 6 (1.0) → 7 → 8 → 9 → 10 → 11 → 12 (2.0)
```

**Parallel tracks** — can overlap once **1.0** ships:

| Track | Phases | Focus |
|-------|--------|--------|
| **Platform** | 6, 12 | CI, release, telemetry, API policy |
| **Reliability** | 6, 8, 12 | Tests, cache, sync, outbox, doctor |
| **UX** | 7, 11 | Delight, list/incoming superpowers |
| **Surfaces** | 9 | Widget, tile, voice, Wear, NFC |
| **House** | 10 | Multi-host, discovery, dashboard |

**Recommended batches after 1.0**

| Batch | Items | Why |
|-------|-------|-----|
| Feel | 7.1, 7.2, 7.7 | Onboarding + swipes; no host work |
| Trust | 8.1, 8.6, 8.8, 8.9 | Cache, outbox v2, doctor, staleness |
| Reach | 9.1, 9.5, 9.3 | Widget v2, share memory, extra tiles |
| Live | 8.3, 8.4, 8.2 | Needs host push/stream/ETag APIs |
| Utility | 11.9, 11.6, 11.3 | Session gallery, scan, shopping aisles |

Host-dependent unlock: **8.3–8.4**, **10.2/10.4/10.6**, and much of **11** move fastest when Everyday Clipboard grows matching APIs — coordinate both repos.
