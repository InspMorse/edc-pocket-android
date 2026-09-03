# Beta channel

EDC pocket supports sideload betas and Play internal testing without a separate codebase.

## Sideload (fastest)

1. Build release APK: `./gradlew assembleRelease`
2. Install `app/build/outputs/apk/release/app-release.apk` on test phones
3. Enable **Settings → Trust & diagnostics → Export** to compare audit logs between builds

## Play internal testing (optional)

1. Upload the same `app-release.apk` to Play Console → **Internal testing**
2. Add testers by email or Google Group
3. Track version via `versionName` / `versionCode` in `app/build.gradle.kts`

## Firebase App Distribution (optional)

1. Create a Firebase project linked to `house.edc.pocket`
2. Upload APK: `firebase appdistribution:distribute app/build/outputs/apk/release/app-release.apk --app <APP_ID> --groups testers`
3. Include **Copy audit log** steps in release notes for testers

## What to test on beta

- Connect Home and Away hosts
- Send clip + list item; verify audit log entries
- Toggle telemetry opt-in; export JSON bundle
- Clear app data; confirm workers stop and onboarding returns
