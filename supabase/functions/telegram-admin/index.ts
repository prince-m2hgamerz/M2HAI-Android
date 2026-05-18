import { serve } from "https://deno.land/std@0.168.0/http/server.ts";

const SUPABASE_URL = Deno.env.get("SUPABASE_URL") ?? "";
const SUPABASE_SERVICE_ROLE_KEY = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY") ??
  Deno.env.get("SUPABASE_SERVICE_ROLE") ?? "";
const TELEGRAM_WEBHOOK_SECRET = Deno.env.get("TELEGRAM_WEBHOOK_SECRET") ?? "";

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
  "Access-Control-Allow-Methods": "POST, OPTIONS",
};

const BOT_SELECT = "id,name,username,token_hint,is_enabled,model_id,fallback_model_id,system_prompt,personality,temperature,max_tokens,welcome_message,commands,rate_limit_per_minute,daily_message_limit,enable_voice,enable_images,enable_files,language_mode,webhook_url,avatar_url,banner_url,created_at,updated_at";

serve(async (req) => {
  if (req.method === "OPTIONS") {
    return new Response("ok", { headers: corsHeaders });
  }

  if (req.method !== "POST") {
    return jsonResponse({ error: "Method not allowed" }, 405);
  }

  try {
    const body = await req.json();
    const action = String(body.action ?? "");

    if (action === "listBots") {
      return jsonResponse({ data: await listBots() });
    }

    if (action === "upsertBot") {
      const bot = await upsertBot((body as { bot?: Record<string, unknown> }).bot ?? {});
      return jsonResponse({ data: bot });
    }

    if (action === "updateBot") {
      const botId = String(body.botId ?? "");
      const updates = (body as { updates?: Record<string, unknown> }).updates ?? {};
      if (!botId) return jsonResponse({ error: "botId is required" }, 400);
      await patchBot(botId, sanitizeBotPayload(updates, true));
      return jsonResponse({ data: null });
    }

    if (action === "deleteBot") {
      const botId = String(body.botId ?? "");
      if (!botId) return jsonResponse({ error: "botId is required" }, 400);
      await deleteBot(botId);
      return jsonResponse({ data: null });
    }

    if (action === "listLogs") {
      const botId = body.botId ? String(body.botId) : "";
      const limit = Math.min(Math.max(Number(body.limit ?? 80), 1), 200);
      return jsonResponse({ data: await listLogs(botId, limit) });
    }

    if (action !== "setWebhook") {
      return jsonResponse({ error: "Unsupported action" }, 400);
    }

    const botId = String(body.botId ?? "");
    const botToken = String(body.botToken ?? "");
    const webhookBaseUrl = String(body.webhookBaseUrl ?? "").replace(/\/$/, "");

    if (!botId || !botToken || !webhookBaseUrl) {
      return jsonResponse({ error: "botId, botToken, and webhookBaseUrl are required" }, 400);
    }

    const webhookUrl = `${webhookBaseUrl}/functions/v1/telegram-bot?bot_id=${encodeURIComponent(botId)}${TELEGRAM_WEBHOOK_SECRET ? `&secret=${encodeURIComponent(TELEGRAM_WEBHOOK_SECRET)}` : ""}`;
    const telegramResponse = await fetch(`https://api.telegram.org/bot${botToken}/setWebhook`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        url: webhookUrl,
        allowed_updates: ["message", "callback_query"],
        drop_pending_updates: false,
      }),
    });

    const telegramPayload = await telegramResponse.json().catch(() => ({}));
    if (!telegramResponse.ok || telegramPayload.ok === false) {
      await insertLog(botId, "error", "webhook_failed", safeMessage(JSON.stringify(telegramPayload)));
      return jsonResponse({ error: "Telegram webhook setup failed", details: telegramPayload }, 502);
    }

    await patchBot(botId, {
      webhook_url: webhookUrl,
      token_hint: maskToken(botToken),
      token_secret: botToken,
      updated_at: new Date().toISOString(),
    });
    await insertLog(botId, "info", "webhook_configured", "Telegram webhook configured successfully");

    return jsonResponse({ webhookUrl, telegram: telegramPayload });
  } catch (error) {
    return jsonResponse({ error: error instanceof Error ? error.message : String(error) }, 500);
  }
});

async function patchBot(botId: string, payload: Record<string, unknown>): Promise<void> {
  if (!SUPABASE_URL || !SUPABASE_SERVICE_ROLE_KEY) return;
  await fetch(`${SUPABASE_URL}/rest/v1/telegram_bots?id=eq.${encodeURIComponent(botId)}`, {
    method: "PATCH",
    headers: {
      "apikey": SUPABASE_SERVICE_ROLE_KEY,
      "Authorization": `Bearer ${SUPABASE_SERVICE_ROLE_KEY}`,
      "Content-Type": "application/json",
      "Prefer": "return=minimal",
    },
    body: JSON.stringify(payload),
  });
}

async function listBots(): Promise<unknown[]> {
  if (!SUPABASE_URL || !SUPABASE_SERVICE_ROLE_KEY) return [];
  const response = await fetch(`${SUPABASE_URL}/rest/v1/telegram_bots?select=${BOT_SELECT}&order=updated_at.desc`, {
    headers: serviceHeaders(),
  });
  if (!response.ok) throw new Error(await response.text());
  return await response.json();
}

async function upsertBot(input: Record<string, unknown>): Promise<unknown> {
  if (!SUPABASE_URL || !SUPABASE_SERVICE_ROLE_KEY) {
    throw new Error("Supabase service role environment is not configured.");
  }

  const payload = sanitizeBotPayload(input);
  const response = await fetch(`${SUPABASE_URL}/rest/v1/telegram_bots?on_conflict=id&select=${BOT_SELECT}`, {
    method: "POST",
    headers: {
      ...serviceHeaders(),
      "Content-Type": "application/json",
      "Prefer": "resolution=merge-duplicates,return=representation",
    },
    body: JSON.stringify(payload),
  });

  if (!response.ok) throw new Error(await response.text());
  const rows = await response.json();
  return Array.isArray(rows) ? rows[0] : rows;
}

async function deleteBot(botId: string): Promise<void> {
  if (!SUPABASE_URL || !SUPABASE_SERVICE_ROLE_KEY) return;
  const response = await fetch(`${SUPABASE_URL}/rest/v1/telegram_bots?id=eq.${encodeURIComponent(botId)}`, {
    method: "DELETE",
    headers: serviceHeaders(),
  });
  if (!response.ok) throw new Error(await response.text());
}

async function listLogs(botId: string, limit: number): Promise<unknown[]> {
  if (!SUPABASE_URL || !SUPABASE_SERVICE_ROLE_KEY) return [];
  const params = new URLSearchParams({
    select: "*",
    order: "created_at.desc",
    limit: String(limit),
  });
  if (botId) params.set("bot_id", `eq.${botId}`);
  const response = await fetch(`${SUPABASE_URL}/rest/v1/telegram_bot_logs?${params.toString()}`, {
    headers: serviceHeaders(),
  });
  if (!response.ok) throw new Error(await response.text());
  return await response.json();
}

function sanitizeBotPayload(input: Record<string, unknown>, partial = false): Record<string, unknown> {
  const allowed = [
    "id",
    "name",
    "username",
    "token_hint",
    "is_enabled",
    "model_id",
    "fallback_model_id",
    "system_prompt",
    "personality",
    "temperature",
    "max_tokens",
    "welcome_message",
    "commands",
    "rate_limit_per_minute",
    "daily_message_limit",
    "enable_voice",
    "enable_images",
    "enable_files",
    "language_mode",
    "webhook_url",
    "avatar_url",
    "banner_url",
    "updated_at",
  ];
  const payload: Record<string, unknown> = {};
  for (const key of allowed) {
    if (key in input) payload[key] = input[key];
  }

  const botToken = typeof input.bot_token === "string" ? input.bot_token.trim() : "";
  if (botToken) {
    payload.token_hint = maskToken(botToken);
    payload.token_secret = botToken;
  }

  if (!partial) {
    payload.updated_at = new Date().toISOString();
  }

  if (typeof payload.username === "string") {
    payload.username = payload.username.replace(/^@/, "").trim() || null;
  }

  return payload;
}

function serviceHeaders(): Record<string, string> {
  return {
    "apikey": SUPABASE_SERVICE_ROLE_KEY,
    "Authorization": `Bearer ${SUPABASE_SERVICE_ROLE_KEY}`,
  };
}

async function insertLog(botId: string, level: "info" | "warning" | "error", event: string, message: string): Promise<void> {
  if (!SUPABASE_URL || !SUPABASE_SERVICE_ROLE_KEY) return;
  await fetch(`${SUPABASE_URL}/rest/v1/telegram_bot_logs`, {
    method: "POST",
    headers: {
      "apikey": SUPABASE_SERVICE_ROLE_KEY,
      "Authorization": `Bearer ${SUPABASE_SERVICE_ROLE_KEY}`,
      "Content-Type": "application/json",
      "Prefer": "return=minimal",
    },
    body: JSON.stringify({
      bot_id: botId,
      level,
      event,
      message,
    }),
  });
}

function maskToken(token: string): string {
  const trimmed = token.trim();
  if (trimmed.length <= 10) return "configured";
  return `${trimmed.slice(0, 6)}...${trimmed.slice(-4)}`;
}

function safeMessage(value: string): string {
  return value.replace(/bot[0-9]+:[0-9A-Za-z_-]+/g, "bot[redacted]");
}

function jsonResponse(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { ...corsHeaders, "Content-Type": "application/json" },
  });
}
