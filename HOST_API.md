# Host API policy (EDC pocket 2.0)

EDC pocket **2.0** is the first release with an explicit host API compatibility policy.

## Version fields

Hosts may expose in `/api/health` or `/api/capabilities`:

```json
{
  "api_version": "1.2",
  "min_client_version": "1.7",
  "feature_flags": { "sse": false },
  "rate_limit_message": "Too many sends — wait 30s"
}
```

| Field | Meaning |
|-------|---------|
| `api_version` | Host API semver — breaking changes bump major |
| `min_client_version` | Oldest EDC pocket app version allowed to connect |
| `feature_flags` | Fine-grained toggles merged with `capabilities` |
| `rate_limit_message` / `retry_after` | Shown in-app when host returns 429 |

## Client behaviour

- **Capabilities** gate UI (tabs, upload, dashboard links)
- **Feature flags** can disable features even when capability defaults are on
- **Rate limits** surface in Settings and the status banner; sends queue to outbox when offline
- **Audit log** records sends, sync, outbox, and rate-limit events locally

## Breaking changes (major host bump)

When the host increments **major** `api_version`:

1. Document removed/changed endpoints in the host repo
2. Ship a matching EDC pocket minor/major release
3. Keep previous endpoints working for one host minor release when possible

## Non-breaking additions

New JSON fields on todos, clips, or incoming files are ignored by older clients. New endpoints may be probed with graceful 404 fallback (existing pattern in `EdcClient`).

## Compatibility matrix (indicative)

| EDC pocket | Host API | Notes |
|------------|----------|-------|
| 1.0–1.5 | 1.0 | Core clip/list/incoming |
| 1.7–1.9 | 1.1 | Profiles, conditional fetch, rich list/incoming |
| **2.0** | **1.2+** | Audit log, feature flags, rate-limit hints |
