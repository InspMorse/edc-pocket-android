# Security & dependency cadence

EDC pocket is a personal sideload / Play-internal client. This document describes how dependencies and security are maintained.

## Update cadence

| Area | Cadence | Owner action |
|------|---------|--------------|
| Android Gradle Plugin / Kotlin | Review quarterly or before each phase release | Run `./gradlew testDebugUnitTest assembleRelease` |
| OkHttp, Room, Compose BOM | Monthly patch check | Bump in `app/build.gradle.kts`, run CI |
| Play Services (Wear, ML Kit) | Quarterly | Test Send tab scan + Wear bridge on device |
| Target SDK | Align with Play requirements annually | Update `targetSdk` in `app/build.gradle.kts` |

## Release checklist

1. All unit tests green (`./gradlew testDebugUnitTest`)
2. Release APK builds (`./gradlew assembleRelease`)
3. Manual smoke: connect Home host, send clip, add list item, export audit log
4. Review `CHANGELOG.md` and tag `v2.x`

## Secrets

- Release signing uses local `keystore.properties` (never committed)
- TLS pins stored in DataStore per device
- No third-party crash or analytics SDKs — optional telemetry stays on-device until export

## Reporting issues

Security-sensitive bugs: open a private issue with host URL redacted and attach **Settings → Copy audit log** output.
