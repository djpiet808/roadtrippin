# Roadtrippin Supabase

The migrations create the exposed `roadtrippin` schema, owner-only RLS policies, sync tombstones, and private journal-photo bucket in the existing `muddlist-pro` project.

The `delete-shared-account` Edge Function is intentionally checked in but not deployed to production. It permanently deletes the current Supabase Auth identity, which also cascades through the shared Muddlist `tasks` and `deleted_tasks` tables. Before beta:

1. Create or use an isolated Supabase development branch/project.
2. Apply both migrations there.
3. Deploy the function with JWT verification enabled.
4. Test no-JWT, expired-JWT, wrong-confirmation, two-user isolation, photo removal, Roadtrippin cascades, and Muddlist cascades.
5. Only after those tests pass, deploy it to production with JWT verification enabled.

The request must be `POST` with a valid bearer token and this exact JSON body:

```json
{ "confirmation": "DELETE MY SHARED ACCOUNT" }
```

Never place `SUPABASE_SERVICE_ROLE_KEY` in either mobile app. Supabase injects it only into the server-side Edge Function runtime.
