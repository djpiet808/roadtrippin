# Roadtrippin

Roadtrippin is a passenger-only road-trip plate tracker built with Kotlin Multiplatform and one shared Compose Multiplatform UI for Android and iOS. The app works offline, records the first time and location a plate is seen, keeps a trip journal, evaluates achievements, and renders an offline U.S. progress map from bundled Census vector data.

The application ID and bundle ID are `com.roadtrippin.app`. Android supports API 29 (Android 10) and newer; iOS supports iOS 17 and newer on arm64 devices and Apple-silicon simulators.

## Project layout

- `shared/` — Compose UI, domain logic, Room KMP database, cloud/account clients, achievements, maps, and platform interfaces.
- `androidApp/` — thin Android application host.
- `iosApp/` — thin SwiftUI/Xcode host embedding `RoadtrippinShared.framework`.
- `supabase/` — production-applied Roadtrippin schema migrations and the gated shared-account deletion function.
- `privacy-site/` — source for the live privacy/account-deletion route.
- `tools/` — deterministic Census vector conversion tooling.

## Local configuration

Use JDK 21 and the checked-in Gradle wrapper. Secrets must remain in ignored files or environment variables.

Android reads these optional properties from `local.properties` or the environment:

```properties
sdk.dir=/absolute/path/to/Android/sdk
SUPABASE_URL=https://PROJECT_REF.supabase.co
SUPABASE_PUBLISHABLE_KEY=sb_publishable_...
SENTRY_DSN=https://...
```

For iOS, copy `iosApp/Configuration/Local.xcconfig.example` to `iosApp/Configuration/Local.xcconfig` and fill in the Apple team and public client configuration. `Config.xcconfig` includes that ignored file when present.

Neither host may contain a Supabase service-role key. The publishable key and Sentry DSN are public client configuration, but they are still supplied outside source control so development and production environments remain separate.

## Regenerate offline map data

The committed map assets are deterministic derivatives of 2025 U.S. Census Bureau data. State boundaries use separate overview and close-detail files. Highway vectors keep only Interstate, U.S., and state routes, with national, regional, and one-degree close-detail levels.

```sh
python3 -m venv /tmp/roadtrippin-map-tools
/tmp/roadtrippin-map-tools/bin/pip install -r tools/requirements-road-vectors.txt
/tmp/roadtrippin-map-tools/bin/python tools/generate_highway_vectors.py \
  shared/src/commonMain/composeResources/files/us_highways_2025.bin \
  shared/src/commonMain/composeResources/files/us_highways_2025_manifest.json \
  --cache-dir /tmp/roadtrippin-census-2025
```

The generated manifest records every source URL and digest, route counts by state, level/tile counts, and the binary asset digest.

## Build and test

```sh
./gradlew --no-configuration-cache :shared:testAndroidHostTest :androidApp:assembleDebug
./gradlew --no-configuration-cache :shared:compileKotlinIosSimulatorArm64
xcodebuild \
  -project iosApp/iosApp.xcodeproj \
  -scheme iosApp \
  -configuration Debug \
  -sdk iphonesimulator \
  -destination 'generic/platform=iOS Simulator' \
  CODE_SIGNING_ALLOWED=NO \
  build
```

The first Xcode build downloads the exact Sentry Cocoa `8.58.2` XCFramework, verifies its published SHA-256 checksum, and caches it under ignored `iosApp/.build/`. Android and iOS use the Sentry KMP `0.27.0` API from shared code.

To create store artifacts after signing is configured:

```sh
./gradlew :androidApp:bundleRelease
xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -configuration Release -destination 'generic/platform=iOS' archive -archivePath build/Roadtrippin.xcarchive
```

## Data and safety

The Room database is authoritative and observed by the shared UI. Location permission is requested only when a user starts/logs an item, no continuous route is recorded, and a failed or denied location never prevents saving. Coordinates are saved before retryable reverse geocoding. Shared text omits precise coordinates.

Cloud accounts are optional and use the existing Muddlist Supabase Auth identity. Signing out retains local trips. The production shared-account deletion function intentionally remains undeployed until it passes the isolated-project tests described in `supabase/README.md`, because deleting that identity also deletes Muddlist tasks.

When an account is confirmed, the local-first sync engine reconciles normalized Supabase rows through a durable Room-backed outbox. It retries after connectivity returns, resolves competing edits by `modified_at` plus deterministic mutation UUID, carries deletions as tombstones, and uploads private journal photos only under the owning user's scoped Storage path. Android encrypts the persisted Auth session with Android Keystore AES/GCM; iOS stores it in Keychain.

The privacy policy and deletion instructions are live at <https://bentnail.studio/roadtrippin/privacy>.

## Release gates

See `docs/store-beta-checklist.md`. The app code, offline maps, camera/library pipeline, outbox sync, unsigned Android release bundle, and iOS simulator build are complete. Signing assets, provider credentials, isolated shared-deletion testing, real two-user RLS/sync testing, physical-device accessibility checks, Sentry verification, and TestFlight/Play Console submission remain release gates.
