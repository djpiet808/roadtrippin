import "jsr:@supabase/functions-js/edge-runtime.d.ts";
import { createClient } from "npm:@supabase/supabase-js@2.110.7";

const jsonHeaders = { "Content-Type": "application/json" };
const bucketName = "roadtrippin-journal-photos";
const confirmationPhrase = "DELETE MY SHARED ACCOUNT";

function response(status: number, body: Record<string, unknown>): Response {
  return new Response(JSON.stringify(body), { status, headers: jsonHeaders });
}

Deno.serve(async (request: Request) => {
  if (request.method !== "POST") return response(405, { error: "Method not allowed" });

  const authorization = request.headers.get("Authorization");
  if (!authorization?.startsWith("Bearer ")) return response(401, { error: "Authentication required" });

  const payload = await request.json().catch(() => null) as { confirmation?: string } | null;
  if (payload?.confirmation !== confirmationPhrase) {
    return response(400, { error: "The exact deletion confirmation phrase is required" });
  }

  const supabaseUrl = Deno.env.get("SUPABASE_URL");
  const serviceRoleKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY");
  if (!supabaseUrl || !serviceRoleKey) return response(500, { error: "Server configuration is incomplete" });

  const admin = createClient(supabaseUrl, serviceRoleKey, {
    auth: { autoRefreshToken: false, persistSession: false },
  });
  const token = authorization.slice("Bearer ".length);
  const { data: userData, error: userError } = await admin.auth.getUser(token);
  const user = userData.user;
  if (userError || !user) return response(401, { error: "The session is invalid or expired" });

  try {
    const storage = admin.storage.from(bucketName);
    const paths: string[] = [];

    async function collectFiles(prefix: string): Promise<void> {
      let offset = 0;
      while (true) {
        const { data, error } = await storage.list(prefix, { limit: 1000, offset, sortBy: { column: "name", order: "asc" } });
        if (error) throw error;
        const items = data ?? [];
        for (const item of items) {
          const path = `${prefix}/${item.name}`;
          if (item.id) paths.push(path);
          else await collectFiles(path);
        }
        if (items.length < 1000) break;
        offset += items.length;
      }
    }

    await collectFiles(user.id);
    for (let index = 0; index < paths.length; index += 100) {
      const { error } = await storage.remove(paths.slice(index, index + 100));
      if (error) throw error;
    }

    const roadtrippin = admin.schema("roadtrippin");
    const tables = [
      "journal_entry_tags",
      "journal_photos",
      "achievement_awards",
      "plate_sightings",
      "journal_entries",
      "travelers",
      "tags",
      "sync_tombstones",
      "trips",
    ];
    for (const table of tables) {
      const { error } = await roadtrippin.from(table).delete().eq("user_id", user.id);
      if (error) throw error;
    }

    const { error: deletionError } = await admin.auth.admin.deleteUser(user.id, false);
    if (deletionError) throw deletionError;

    return response(200, { deleted: true });
  } catch (error) {
    console.error("Shared account deletion failed", error instanceof Error ? error.message : "unknown error");
    return response(500, { error: "Account deletion could not be completed" });
  }
});
