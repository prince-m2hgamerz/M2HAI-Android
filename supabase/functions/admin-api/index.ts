import { serve } from "https://deno.land/std@0.168.0/http/server.ts";

const SUPABASE_URL = Deno.env.get("SUPABASE_URL") ?? "";
const SUPABASE_SERVICE_ROLE_KEY = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY") ??
  Deno.env.get("SUPABASE_SERVICE_ROLE") ?? "";

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
  "Access-Control-Allow-Methods": "POST, OPTIONS",
};

const publicActions = new Set(["platformOverview", "getAppSettings", "listModels", "listAppReleases"]);

serve(async (req) => {
  if (req.method === "OPTIONS") {
    return new Response("ok", { headers: corsHeaders });
  }

  if (req.method !== "POST") {
    return jsonResponse({ error: "Method not allowed" }, 405);
  }

  try {
    const body = await req.json().catch(() => ({}));
    const action = String(body.action ?? "");

    if (!publicActions.has(action) && !isServiceRoleRequest(req)) {
      return jsonResponse({ error: "Unauthorized admin request" }, 401);
    }

    if (action === "listFeedback") {
      const page = Math.max(Number(body.page ?? 0), 0);
      const pageSize = Math.min(Math.max(Number(body.pageSize ?? 20), 1), 100);
      return jsonResponse({ data: await listFeedback(page, pageSize) });
    }

    if (action === "feedbackStats") {
      return jsonResponse({ data: await feedbackStats() });
    }

    if (action === "dashboardStats") {
      return jsonResponse({ data: await dashboardStats() });
    }

    if (action === "platformOverview") {
      return jsonResponse({ data: await platformOverview() });
    }

    if (action === "listUsers") {
      const page = Math.max(Number(body.page ?? 0), 0);
      const pageSize = Math.min(Math.max(Number(body.pageSize ?? 20), 1), 100);
      const search = String(body.search ?? "");
      return jsonResponse({ data: await listRows("profiles", page, pageSize, search, ["email", "full_name"], "updated_at.desc") });
    }

    if (action === "updateUser") {
      const userId = String(body.userId ?? "");
      if (!userId) return jsonResponse({ error: "userId is required" }, 400);
      await updateRow("profiles", "id", userId, safeObject(body.updates));
      return jsonResponse({ data: null });
    }

    if (action === "deleteUser") {
      const userId = String(body.userId ?? "");
      if (!userId) return jsonResponse({ error: "userId is required" }, 400);
      await deleteRow("profiles", "id", userId);
      return jsonResponse({ data: null });
    }

    if (action === "listChats") {
      const page = Math.max(Number(body.page ?? 0), 0);
      const pageSize = Math.min(Math.max(Number(body.pageSize ?? 20), 1), 100);
      const search = String(body.search ?? "");
      return jsonResponse({ data: await listRows("chats", page, pageSize, search, ["title", "model_id"], "created_at.desc") });
    }

    if (action === "updateChat") {
      const chatId = String(body.chatId ?? "");
      if (!chatId) return jsonResponse({ error: "chatId is required" }, 400);
      await updateRow("chats", "id", chatId, safeObject(body.updates));
      return jsonResponse({ data: null });
    }

    if (action === "deleteChat") {
      const chatId = String(body.chatId ?? "");
      if (!chatId) return jsonResponse({ error: "chatId is required" }, 400);
      await deleteRow("chats", "id", chatId);
      return jsonResponse({ data: null });
    }

    if (action === "chatMessages") {
      const chatId = String(body.chatId ?? "");
      if (!chatId) return jsonResponse({ error: "chatId is required" }, 400);
      return jsonResponse({ data: await selectRows("messages", {
        select: "*",
        filters: [`chat_id=eq.${chatId}`],
        order: "created_at.asc",
      }) });
    }

    if (action === "listModels") {
      return jsonResponse({ data: await selectRows("ai_models", { select: "*", order: "name.asc" }) });
    }

    if (action === "createModel") {
      return jsonResponse({ data: await insertRows("ai_models", safeObject(body.model), "return=minimal") });
    }

    if (action === "updateModel") {
      const modelId = String(body.modelId ?? "");
      if (!modelId) return jsonResponse({ error: "modelId is required" }, 400);
      await updateRow("ai_models", "id", modelId, safeObject(body.updates));
      return jsonResponse({ data: null });
    }

    if (action === "deleteModel") {
      const modelId = String(body.modelId ?? "");
      if (!modelId) return jsonResponse({ error: "modelId is required" }, 400);
      await deleteRow("ai_models", "id", modelId);
      return jsonResponse({ data: null });
    }

    if (action === "upsertModels") {
      const models = Array.isArray(body.models) ? body.models.map(safeObject) : [];
      if (models.length === 0) return jsonResponse({ data: 0 });
      await upsertRows("ai_models", models, "id");
      return jsonResponse({ data: models.length });
    }

    if (action === "activateOnlyModels") {
      const modelIds = Array.isArray(body.modelIds) ? body.modelIds.map(String) : [];
      await activateOnlyModels(modelIds);
      return jsonResponse({ data: null });
    }

    if (action === "getAppSettings") {
      return jsonResponse({ data: await appSettings() });
    }

    if (action === "listAppReleases") {
      return jsonResponse({ data: await listAppReleases() });
    }

    if (action === "updateAppSettings") {
      await upsertRows("app_settings", { ...safeObject(body.settings), id: "global", updated_at: new Date().toISOString() }, "id");
      return jsonResponse({ data: null });
    }

    if (action === "userGrowth") {
      return jsonResponse({ data: await userGrowth() });
    }

    return jsonResponse({ error: "Unsupported action" }, 400);
  } catch (error) {
    return jsonResponse({ error: error instanceof Error ? error.message : String(error) }, 500);
  }
});

async function listFeedback(page: number, pageSize: number): Promise<{ data: unknown[]; count: number }> {
  ensureServiceRole();

  const from = page * pageSize;
  const to = from + pageSize - 1;
  const params = new URLSearchParams({
    select: "*",
    order: "created_at.desc",
    offset: String(from),
    limit: String(pageSize),
  });

  const response = await fetch(`${SUPABASE_URL}/rest/v1/message_feedback?${params.toString()}`, {
    headers: {
      ...serviceHeaders(),
      "Prefer": "count=exact",
      "Range-Unit": "items",
      "Range": `${from}-${to}`,
    },
  });

  if (!response.ok) throw new Error(await response.text());

  return {
    data: await response.json(),
    count: parseContentRange(response.headers.get("content-range")),
  };
}

async function feedbackStats(): Promise<Record<string, number>> {
  ensureServiceRole();

  const [total, copy, like, unlike, share] = await Promise.all([
    countFeedback(),
    countFeedback("copy"),
    countFeedback("like"),
    countFeedback("unlike"),
    countFeedback("share"),
  ]);

  return { total, copy, like, unlike, share };
}

async function dashboardStats(): Promise<Record<string, number>> {
  ensureServiceRole();
  const today = startOfToday();
  const [totalUsers, totalChats, totalMessages, totalModels, activeModels, disabledUsers, newUsersToday, activeChatsToday] = await Promise.all([
    countTable("profiles"),
    countTable("chats"),
    countTable("messages"),
    countTable("ai_models"),
    countTable("ai_models", ["is_active=eq.true"]),
    countTable("profiles", ["is_disabled=eq.true"]),
    countTable("profiles", [`updated_at=gte.${today}`]),
    countTable("chats", [`created_at=gte.${today}`]),
  ]);

  return {
    totalUsers,
    totalChats,
    totalMessages,
    totalModels,
    activeModels,
    disabledUsers,
    newUsersToday,
    activeChatsToday,
  };
}

async function platformOverview(): Promise<Record<string, number | string>> {
  ensureServiceRole();
  const today = startOfToday();
  const [totalUsers, activeChats, aiRequests, activeModels, telegramBots, apiUsage, newUsersToday, activeChatsToday] = await Promise.all([
    countTable("profiles"),
    countTable("chats", ["is_archived=eq.false"]),
    countTable("messages", ["role=eq.assistant"]),
    countTable("ai_models", ["is_active=eq.true"]),
    countTable("telegram_bots", ["is_enabled=eq.true"]),
    countTable("api_usage"),
    countTable("profiles", [`updated_at=gte.${today}`]),
    countTable("chats", [`created_at=gte.${today}`]),
  ]);

  return {
    totalUsers,
    activeChats,
    aiRequests,
    activeModels,
    telegramBots,
    apiUsage: apiUsage || aiRequests,
    systemUptime: "Live",
    newUsersToday,
    activeChatsToday,
  };
}

async function listRows(
  table: string,
  page: number,
  pageSize: number,
  search: string,
  searchColumns: string[],
  order: string,
): Promise<{ data: unknown[]; count: number }> {
  ensureServiceRole();

  const from = page * pageSize;
  const to = from + pageSize - 1;
  const params = new URLSearchParams({
    select: "*",
    order,
    offset: String(from),
    limit: String(pageSize),
  });

  const trimmedSearch = search.trim();
  if (trimmedSearch && searchColumns.length > 0) {
    params.set("or", `(${searchColumns.map((column) => `${column}.ilike.*${escapeFilter(trimmedSearch)}*`).join(",")})`);
  }

  const response = await fetch(`${SUPABASE_URL}/rest/v1/${table}?${params.toString()}`, {
    headers: {
      ...serviceHeaders(),
      "Prefer": "count=exact",
      "Range-Unit": "items",
      "Range": `${from}-${to}`,
    },
  });

  if (!response.ok) throw new Error(await response.text());
  return {
    data: await response.json(),
    count: parseContentRange(response.headers.get("content-range")),
  };
}

async function selectRows(table: string, options: {
  select: string;
  filters?: string[];
  order?: string;
  limit?: number;
}): Promise<unknown[]> {
  ensureServiceRole();
  const params = new URLSearchParams({ select: options.select });
  if (options.order) params.set("order", options.order);
  if (options.limit) params.set("limit", String(options.limit));
  for (const filter of options.filters ?? []) {
    const [key, ...rest] = filter.split("=");
    if (key && rest.length > 0) params.set(key, rest.join("="));
  }

  const response = await fetch(`${SUPABASE_URL}/rest/v1/${table}?${params.toString()}`, {
    headers: serviceHeaders(),
  });
  if (!response.ok) throw new Error(await response.text());
  return await response.json();
}

async function insertRows(table: string, payload: unknown, prefer = "return=minimal"): Promise<unknown> {
  ensureServiceRole();
  const response = await fetch(`${SUPABASE_URL}/rest/v1/${table}`, {
    method: "POST",
    headers: {
      ...serviceHeaders(),
      "Content-Type": "application/json",
      "Prefer": prefer,
    },
    body: JSON.stringify(payload),
  });
  if (!response.ok) throw new Error(await response.text());
  if (prefer.includes("return=representation")) return await response.json();
  return null;
}

async function upsertRows(table: string, payload: unknown, conflict: string): Promise<void> {
  ensureServiceRole();
  const response = await fetch(`${SUPABASE_URL}/rest/v1/${table}?on_conflict=${encodeURIComponent(conflict)}`, {
    method: "POST",
    headers: {
      ...serviceHeaders(),
      "Content-Type": "application/json",
      "Prefer": "resolution=merge-duplicates,return=minimal",
    },
    body: JSON.stringify(payload),
  });
  if (!response.ok) throw new Error(await response.text());
}

async function updateRow(table: string, column: string, value: string, updates: Record<string, unknown>): Promise<void> {
  ensureServiceRole();
  const response = await fetch(`${SUPABASE_URL}/rest/v1/${table}?${column}=eq.${encodeURIComponent(value)}`, {
    method: "PATCH",
    headers: {
      ...serviceHeaders(),
      "Content-Type": "application/json",
      "Prefer": "return=minimal",
    },
    body: JSON.stringify({ ...updates, updated_at: new Date().toISOString() }),
  });
  if (!response.ok) throw new Error(await response.text());
}

async function deleteRow(table: string, column: string, value: string): Promise<void> {
  ensureServiceRole();
  const response = await fetch(`${SUPABASE_URL}/rest/v1/${table}?${column}=eq.${encodeURIComponent(value)}`, {
    method: "DELETE",
    headers: serviceHeaders(),
  });
  if (!response.ok) throw new Error(await response.text());
}

async function activateOnlyModels(modelIds: string[]): Promise<void> {
  ensureServiceRole();
  const rows = await selectRows("ai_models", { select: "id" });
  for (const row of rows) {
    const modelId = String((row as { id?: unknown }).id ?? "");
    if (!modelId) continue;
    await updateRow("ai_models", "id", modelId, { is_active: modelIds.includes(modelId) });
  }
}

async function appSettings(): Promise<Record<string, unknown> | null> {
  const rows = await selectRows("app_settings", {
    select: "*",
    filters: ["id=eq.global"],
    limit: 1,
  });
  return (rows[0] as Record<string, unknown> | undefined) ?? null;
}

async function listAppReleases(): Promise<Array<Record<string, unknown>>> {
  ensureServiceRole();
  const settings = (await appSettings()) ?? {};
  const releases: Array<Record<string, unknown>> = [];

  const latestUrl = String(settings.update_apk_url ?? "");
  const latestVersionCode = Number(settings.latest_version_code ?? 0);
  if (latestUrl) {
    releases.push({
      id: `latest-${latestVersionCode}`,
      versionName: String(settings.latest_version_name ?? "Latest"),
      versionCode: latestVersionCode,
      channel: String(settings.update_channel ?? "stable"),
      apkUrl: latestUrl,
      fileName: fileNameFromUrl(latestUrl),
      sizeMb: Number(settings.update_apk_size_mb ?? 0),
      sha256: String(settings.update_sha256 ?? ""),
      releaseNotes: String(settings.update_release_notes ?? settings.update_message ?? ""),
      publishedAt: String(settings.update_published_at ?? settings.updated_at ?? ""),
      required: Boolean(settings.update_required),
      minSupportedVersionCode: Number(settings.min_supported_version_code ?? 0),
      isLatest: true,
      source: "settings",
    });
  }

  const storageItems = await listStorageObjects("app-updates", "android");
  for (const item of storageItems) {
    const name = String(item.name ?? "");
    if (!name.toLowerCase().endsWith(".apk")) continue;

    const path = `android/${name}`;
    const apkUrl = publicStorageUrl("app-updates", path);
    if (releases.some((release) => normalizeUrl(String(release.apkUrl ?? "")) === normalizeUrl(apkUrl))) continue;

    const parsed = parseApkFileName(name);
    const metadata = safeObject(item.metadata);
    releases.push({
      id: path,
      versionName: parsed.versionName,
      versionCode: parsed.versionCode,
      channel: "archive",
      apkUrl,
      fileName: name,
      sizeMb: bytesToMb(Number(metadata.size ?? 0)),
      sha256: "",
      releaseNotes: "",
      publishedAt: String(item.updated_at ?? item.created_at ?? ""),
      required: false,
      minSupportedVersionCode: 0,
      isLatest: false,
      source: "storage",
    });
  }

  return releases.sort((a, b) => {
    if (Boolean(a.isLatest) !== Boolean(b.isLatest)) return Boolean(a.isLatest) ? -1 : 1;
    const aCode = Number(a.versionCode ?? 0);
    const bCode = Number(b.versionCode ?? 0);
    if (aCode !== bCode) return bCode - aCode;
    return new Date(String(b.publishedAt ?? 0)).getTime() - new Date(String(a.publishedAt ?? 0)).getTime();
  });
}

async function userGrowth(): Promise<Array<{ date: string; users: number; chats: number }>> {
  ensureServiceRole();
  const result = [];
  for (let i = 6; i >= 0; i -= 1) {
    const date = new Date();
    date.setDate(date.getDate() - i);
    date.setHours(0, 0, 0, 0);
    const nextDate = new Date(date);
    nextDate.setDate(nextDate.getDate() + 1);

    const [users, chats] = await Promise.all([
      countTable("profiles", [`updated_at=gte.${date.toISOString()}`, `updated_at=lt.${nextDate.toISOString()}`]),
      countTable("chats", [`created_at=gte.${date.toISOString()}`, `created_at=lt.${nextDate.toISOString()}`]),
    ]);

    result.push({
      date: date.toLocaleDateString("en-US", { weekday: "short", month: "short", day: "numeric" }),
      users,
      chats,
    });
  }
  return result;
}

async function countFeedback(action?: string): Promise<number> {
  const params = new URLSearchParams({ select: "id" });
  if (action) params.set("action", `eq.${action}`);

  const response = await fetch(`${SUPABASE_URL}/rest/v1/message_feedback?${params.toString()}`, {
    method: "HEAD",
    headers: {
      ...serviceHeaders(),
      "Prefer": "count=exact",
    },
  });

  if (!response.ok) throw new Error(await response.text());
  return parseContentRange(response.headers.get("content-range"));
}

async function listStorageObjects(bucket: string, prefix: string): Promise<Array<Record<string, unknown>>> {
  const response = await fetch(`${SUPABASE_URL}/storage/v1/object/list/${bucket}`, {
    method: "POST",
    headers: {
      ...serviceHeaders(),
      "Content-Type": "application/json",
    },
    body: JSON.stringify({
      prefix,
      limit: 100,
      offset: 0,
      sortBy: { column: "updated_at", order: "desc" },
    }),
  });

  if (!response.ok) throw new Error(await response.text());
  const data = await response.json();
  return Array.isArray(data) ? data as Array<Record<string, unknown>> : [];
}

function parseApkFileName(fileName: string): { versionName: string; versionCode: number } {
  const match = fileName.match(/^m2hai-(.+)-(\d+)\.apk$/i);
  if (!match) return { versionName: fileName.replace(/\.apk$/i, ""), versionCode: 0 };
  return { versionName: match[1], versionCode: Number(match[2]) || 0 };
}

function publicStorageUrl(bucket: string, path: string): string {
  return `${SUPABASE_URL}/storage/v1/object/public/${bucket}/${path.split("/").map(encodeURIComponent).join("/")}`;
}

function fileNameFromUrl(url: string): string {
  try {
    return decodeURIComponent(new URL(url).pathname.split("/").pop() || "m2hai.apk");
  } catch {
    return "m2hai.apk";
  }
}

function normalizeUrl(url: string): string {
  return url.trim().replace(/\?.*$/, "").replace(/\/+$/, "");
}

function bytesToMb(bytes: number): number {
  if (!Number.isFinite(bytes) || bytes <= 0) return 0;
  return Number((bytes / 1024 / 1024).toFixed(1));
}

function serviceHeaders(): Record<string, string> {
  return {
    "apikey": SUPABASE_SERVICE_ROLE_KEY,
    "Authorization": `Bearer ${SUPABASE_SERVICE_ROLE_KEY}`,
  };
}

function ensureServiceRole(): void {
  if (!SUPABASE_URL || !SUPABASE_SERVICE_ROLE_KEY) {
    throw new Error("Supabase service role environment is not configured.");
  }
}

function isServiceRoleRequest(req: Request): boolean {
  if (!SUPABASE_SERVICE_ROLE_KEY) return false;
  const auth = req.headers.get("authorization") ?? "";
  const bearer = auth.toLowerCase().startsWith("bearer ") ? auth.slice(7).trim() : "";
  const apikey = req.headers.get("apikey") ?? "";
  return bearer === SUPABASE_SERVICE_ROLE_KEY || apikey === SUPABASE_SERVICE_ROLE_KEY;
}

function safeObject(value: unknown): Record<string, unknown> {
  if (!value || typeof value !== "object" || Array.isArray(value)) return {};
  return value as Record<string, unknown>;
}

function escapeFilter(value: string): string {
  return value.replace(/[%*,()]/g, "");
}

async function countTable(table: string, filters: string[] = []): Promise<number> {
  const params = new URLSearchParams({ select: "id" });
  for (const filter of filters) {
    const [key, ...rest] = filter.split("=");
    if (!key || rest.length === 0) continue;
    params.set(key, rest.join("="));
  }

  const response = await fetch(`${SUPABASE_URL}/rest/v1/${table}?${params.toString()}`, {
    method: "HEAD",
    headers: {
      ...serviceHeaders(),
      "Prefer": "count=exact",
      "Range-Unit": "items",
      "Range": "0-0",
    },
  });

  if (!response.ok) return 0;
  return parseContentRange(response.headers.get("content-range"));
}

function parseContentRange(value: string | null): number {
  if (!value) return 0;
  const total = value.split("/").pop();
  if (!total || total === "*") return 0;
  const parsed = Number(total);
  return Number.isFinite(parsed) ? parsed : 0;
}

function startOfToday(): string {
  const today = new Date();
  today.setHours(0, 0, 0, 0);
  return today.toISOString();
}

function jsonResponse(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { ...corsHeaders, "Content-Type": "application/json" },
  });
}
