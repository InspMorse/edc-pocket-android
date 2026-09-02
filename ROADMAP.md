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

Branch: `cursor/finish-edc-pocket-android-f18a` · PR #2

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

## Phase 2 — Smart connectivity (next)

**Goal:** Right host, right network, no lost sends.

| # | Item |
|---|------|
| 2.1 | Auto Home/Away on Wi‑Fi or Tailscale |
| 2.2 | Re-probe on network change |
| 2.3 | Offline outbox for clip/list/photo sends |
| 2.4 | Outbox UI (pending, retry, clear) |
| 2.5 | Smarter polling (back off when idle) |
| 2.6 | Flush outbox on resume |
| 2.7 | Custom URL validation |
| 2.8 | Tailscale hint when Away fails |

**Exit criteria:** Phone switches Home/Away without manual Find host; sends survive brief outages.

---

## Phase 3 — Glanceable access

**Goal:** Use the house clipboard without opening the full app.

| # | Item |
|---|------|
| 3.1 | Home screen widget — latest clip preview |
| 3.2 | Widget actions — Copy latest, Open app |
| 3.3 | Quick Settings tile — copy house clipboard |
| 3.4 | Optional notification on new clip (WorkManager) |
| 3.5 | Notification actions — Copy / Open |
| 3.6 | Configurable background poll (off / conservative / active) |

**Exit criteria:** Latest clip reachable from home screen or notification in one tap.

---

## Phase 4 — Richer send & receive

**Goal:** Photos and Incoming feel first-class.

| # | Item |
|---|------|
| 4.1 | Incoming image thumbnails |
| 4.2 | Multi-photo upload into session folder |
| 4.3 | Share target: session folder prompt |
| 4.4 | Share multiple images (`ACTION_SEND_MULTIPLE`) |
| 4.5 | Download Incoming to phone / share out |
| 4.6 | Upload progress for large photos |
| 4.7 | Clip history search (local filter) |
| 4.8 | Undo delete todo |

---

## Phase 5 — Deeper house integration

**Goal:** Pocket stays in sync as Everyday Clipboard evolves.

| # | Item |
|---|------|
| 5.1 | Capability discovery from health / future API |
| 5.2 | Graceful degradation when endpoints missing |
| 5.3 | Identity sync if host exposes users |
| 5.4 | Deep link to host dashboard item |
| 5.5 | Optional HTTPS to host |
| 5.6 | Host push via FCM (needs host work) |

---

## Phase 6 — Quality, trust & release

**Goal:** Safe to install long-term; easy to ship updates.

| # | Item |
|---|------|
| 6.1 | EdcClient MockWebServer integration tests |
| 6.2 | Compose UI smoke tests |
| 6.3 | Signed release build |
| 6.4 | GitHub Actions CI |
| 6.5 | Changelog / GitHub Releases |
| 6.6 | Play internal track (optional) |
| 6.7 | Privacy / permissions audit in README |
| 6.8 | Crash reporting (optional) |

---

## Version naming

| Version | Scope |
|---------|--------|
| **0.1** | Phase 0 |
| **0.2** | Phase 1 (current) |
| **0.3** | Phase 2 |
| **0.4** | Phase 3 |
| **0.5** | Phase 4 |
| **1.0** | Phase 6 release |

---

## Host API reference

| Endpoint | Used for |
|----------|----------|
| `GET /api/health` | Probe, find host, dashboard URL |
| `GET /api/clipboard` | Clip tab, widget (future) |
| `POST /api/clipboard` | Send, share target, outbox |
| `GET /api/todo` | List tab |
| `POST /api/todo` | Add, share target |
| `PATCH/POST /api/todo/{id}` | Toggle done |
| `DELETE /api/todo/{id}` | Remove done |
| `GET /api/todo/text` | Share whole list |
| `GET /api/incoming` | Incoming list |
| Upload paths | Photos to Incoming |

---

## Delivery order

```
Phase 1 → Phase 2 → Phase 3 (widget OR tile first) → Phase 4 → Phase 5 → Phase 6
```

Parallel tracks: **UX** (1+4), **Connectivity** (2), **Surfaces** (3), **Engineering** (6).
