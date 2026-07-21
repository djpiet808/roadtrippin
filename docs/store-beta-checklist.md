# Store beta checklist

## Automated and local checks

- [x] Exact 53-entry catalog: 50 states, D.C., B.C., and Mexico.
- [x] Country order, alphabetical order, regional assignments, colors, and progress denominators tested.
- [x] Quick start, idempotent first sighting, missing location, correction/removal, journal quantities, photo limit, lifecycle, and achievement revocation unit tested.
- [x] Durable outbox persistence, deletion tombstones, and deterministic conflict tie-breaking unit tested.
- [x] Android API 29+ debug APK builds.
- [x] Unsigned Android release AAB builds and passes release lint.
- [x] iOS arm64 simulator host builds, links the shared Compose framework, installs, launches, and renders the safety screen.
- [x] Bundled Census state vectors and offline map rendering compile on both targets.
- [x] Android and iOS camera/library adapters resize to a 2,048-pixel maximum, re-encode as JPEG to strip source EXIF, enforce five photos, and store files privately.
- [x] Local-first Supabase outbox reconciliation, retry, tombstones, photo upload/download, and achievement award reconciliation are implemented.
- [x] Android Keystore and iOS Keychain session storage are implemented.
- [x] Native Apple sign-in bridge and Android/iOS OAuth callback handling are implemented.
- [x] Offline journal-map pan/zoom/pin selection and coordinate-free vector map sharing are implemented.
- [x] Privacy/account-deletion route returns a final `200 OK` and existing Bent Nail Studio routes remain healthy.
- [x] Production Roadtrippin schema, RLS, indexes, PostgREST exposure, and private Storage policies applied and inspected.
- [x] Production Supabase advisors run: no Roadtrippin-schema security warning; only expected unused-index notices before beta traffic. The project-wide leaked-password warning and pre-existing Muddlist performance warnings remain tracked below.

## Credentials and provider setup

- [ ] Supply production Supabase publishable key through CI/store build secrets.
- [ ] Configure Google OAuth redirect/client IDs for Android and iOS.
- [ ] Configure Apple Sign in capability/provider credentials and verify the implemented native iOS flow against production Auth.
- [ ] Supply scrubbed Sentry DSN and upload Android mapping/native symbols and iOS dSYMs.
- [ ] Choose an online tile provider before enabling the provider-neutral online map interface.

## Security and destructive-flow testing

- [ ] Apply migrations to an isolated Supabase branch/project.
- [ ] Deploy `delete-shared-account` there with JWT verification enabled.
- [ ] Prove missing/expired JWT and wrong confirmation cannot delete data.
- [ ] Prove two users cannot read or mutate each other's database rows or Storage objects.
- [ ] Verify photo, Roadtrippin, Auth, and Muddlist cascades match the warning exactly.
- [ ] Re-run Supabase security and performance advisors after isolated two-user test data is present.
- [ ] Enable [leaked-password protection](https://supabase.com/docs/guides/auth/password-security#password-strength-and-leaked-password-protection) in the shared Supabase Auth project after confirming the impact on Muddlist.
- [ ] Triage the advisor's pre-existing `public.tasks`/`public.deleted_tasks` indexing and RLS-performance findings with the Muddlist owner; do not change them from the Roadtrippin rollout.
- [ ] Only then deploy the deletion function to production and enable the in-app confirmation action.

## Product acceptance still requiring device/external testing

- [ ] Verify camera/library capture, EXIF stripping, and 2,048-pixel resize with representative real photos on physical Android and iOS devices.
- [ ] Verify outbox upload/download/conflict/tombstone behavior and replacement-phone restoration against two isolated test accounts/devices.
- [ ] Verify Google login on both platforms and native Apple login on iOS.
- [ ] Exercise airplane-mode process death and queued photo retries.
- [ ] Verify TalkBack, VoiceOver, large text, high contrast, reduced motion, dark mode, and silent mode on physical devices.
- [ ] Force scrubbed test crashes and nonfatal sync errors; verify symbolicated Sentry events on both platforms.
- [ ] Verify shared map-image output on representative devices and confirm no coordinates are present.
- [ ] Produce signed AAB/archive, install from Play internal testing and TestFlight, and run the final smoke suite.
