# Store data disclosure working sheet

This is an implementation inventory, not a substitute for answering the current App Store Connect and Play Console questionnaires at submission time.

| Data category | Collection/use | Linked to account | Shared externally | User control |
|---|---|---:|---:|---|
| Precise location | First sighting, note, stop, and optional trip endpoints | When cloud sync is enabled | Supabase processor only; never in share output | Permission can be denied; values editable/removable |
| Photos | Optional journal attachments | When cloud sync is enabled | Private Supabase Storage processor only | Optional; editable/removable |
| Free-form content | Trip details, notes, stops, tags, travelers | When cloud sync is enabled | Supabase processor only | Editable/removable |
| Email/provider identity | Optional authentication and sync | Yes | Supabase/Auth provider | Account optional; full shared deletion available after gated rollout |
| Diagnostics | Crashes and nonfatal sync errors | Configured not to include app user identity | Sentry processor | No analytics, replay, or performance tracing in beta |

Roadtrippin does not record a continuous route, sell data, serve ads, or perform behavioral analytics. Shared-account deletion also removes the same identity's Muddlist tasks and deleted-task records; that consequence must remain prominent in the app, policy, and store account-deletion disclosures.
