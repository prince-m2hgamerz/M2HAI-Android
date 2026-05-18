import { serve } from "https://deno.land/std@0.168.0/http/server.ts";

type TelegramUpdate = {
  message?: TelegramMessage;
  edited_message?: TelegramMessage;
  callback_query?: {
    id: string;
    data?: string;
    message?: TelegramMessage;
    from?: TelegramUser;
  };
};

type TelegramMessage = {
  message_id: number;
  chat: { id: number | string; type: string };
  text?: string;
  caption?: string;
  from?: TelegramUser;
};

type TelegramUser = {
  id: number;
  first_name?: string;
  username?: string;
  language_code?: string;
};

type TelegramBotRow = {
  id: string;
  name: string;
  username: string | null;
  token_secret: string | null;
  is_enabled: boolean;
  model_id: string;
  fallback_model_id: string | null;
  system_prompt: string;
  personality: string;
  temperature: number;
  max_tokens: number;
  welcome_message: string;
  commands: string[];
  enable_images: boolean;
  enable_voice: boolean;
  enable_files: boolean;
  language_mode: string;
};

const SUPABASE_URL = Deno.env.get("SUPABASE_URL") ?? "";
const SUPABASE_SERVICE_ROLE_KEY = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY") ??
  Deno.env.get("SUPABASE_SERVICE_ROLE") ?? "";
const NVIDIA_API_KEY = Deno.env.get("NVIDIA_API_KEY") ?? "";
const GEMINI_API_KEY = Deno.env.get("GEMINI_API_KEY") ?? "";
const TELEGRAM_WEBHOOK_SECRET = Deno.env.get("TELEGRAM_WEBHOOK_SECRET") ?? "";

const CHAT_ENDPOINT = `${SUPABASE_URL}/functions/v1/chat`;

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
  "Access-Control-Allow-Methods": "POST, OPTIONS, GET",
};

serve(async (req) => {
  if (req.method === "OPTIONS") {
    return new Response("ok", { headers: corsHeaders });
  }

  if (req.method === "GET") {
    return jsonResponse({ ok: true });
  }

  if (req.method !== "POST") {
    return jsonResponse({ error: "Method not allowed" }, 405);
  }

  try {
    const url = new URL(req.url);
    const body = await req.json().catch(() => ({}));
    const update = body as TelegramUpdate;
    const botId = url.searchParams.get("bot_id")
      ?? String((body as Record<string, unknown>).bot_id ?? "");
    const secret = url.searchParams.get("secret") ?? "";

    if (!botId) {
      return jsonResponse({ error: "bot_id is required" }, 400);
    }

    if (TELEGRAM_WEBHOOK_SECRET && secret !== TELEGRAM_WEBHOOK_SECRET) {
      return jsonResponse({ error: "Invalid webhook secret" }, 401);
    }

    const bot = await getBot(botId);
    if (!bot || !bot.is_enabled) {
      return jsonResponse({ ok: true, skipped: true });
    }

    const message = update.message ?? update.edited_message ?? update.callback_query?.message;
    if (!message) {
      return jsonResponse({ ok: true, skipped: true });
    }

    const chatId = String(message.chat.id);
    const user = message.from;
    const text = (message.text ?? message.caption ?? "").trim();

    await insertLog(bot.id, "info", "incoming_message", text || "Telegram update received", user?.id?.toString() ?? null, chatId);

    if (!text) {
      await sendTelegramMessage(bot.token_secret ?? "", chatId, "Send text, and I will reply here.");
      await insertLog(bot.id, "info", "empty_message", "Telegram message had no text content", user?.id?.toString() ?? null, chatId);
      return jsonResponse({ ok: true });
    }

    const normalized = text.toLowerCase();
    if (normalized === "/start") {
      await sendTelegramMessage(bot.token_secret ?? "", chatId, bot.welcome_message || "Welcome to M2HAI.");
      await insertLog(bot.id, "info", "start_command", "Sent welcome message", user?.id?.toString() ?? null, chatId);
      return jsonResponse({ ok: true });
    }

    if (normalized === "/help") {
      await sendTelegramMessage(
        bot.token_secret ?? "",
        chatId,
        [
          "Available commands:",
          "/start - open the bot",
          "/help - show this message",
          "/model - show active model",
          "/image - force image generation for the next prompt",
        ].join("\n"),
      );
      await insertLog(bot.id, "info", "help_command", "Sent help message", user?.id?.toString() ?? null, chatId);
      return jsonResponse({ ok: true });
    }

    if (normalized === "/model") {
      await sendTelegramMessage(
        bot.token_secret ?? "",
        chatId,
        `Active model: ${bot.model_id}${bot.fallback_model_id ? `\nFallback: ${bot.fallback_model_id}` : ""}`,
      );
      await insertLog(bot.id, "info", "model_command", "Returned active model info", user?.id?.toString() ?? null, chatId);
      return jsonResponse({ ok: true });
    }

    const modelId = shouldUseImageModel(text) && bot.enable_images
      ? bot.fallback_model_id || bot.model_id
      : bot.model_id;
    const systemPrompt = [bot.system_prompt.trim(), `Personality: ${bot.personality.trim()}`]
      .filter(Boolean)
      .join("\n\n");

    const response = await fetch(CHAT_ENDPOINT, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        "apikey": SUPABASE_SERVICE_ROLE_KEY,
        "Authorization": `Bearer ${SUPABASE_SERVICE_ROLE_KEY}`,
        "x-nvidia-api-key": NVIDIA_API_KEY,
        "x-gemini-api-key": GEMINI_API_KEY,
      },
      body: JSON.stringify({
        model: modelId,
        stream: false,
        temperature: bot.temperature,
        max_tokens: bot.max_tokens,
        system_prompt: systemPrompt,
        messages: [
          {
            role: "user",
            content: text,
          },
        ],
      }),
    });

    if (!response.ok) {
      const errorText = await response.text();
      const fallbackContent = bot.fallback_model_id
        ? await getFallbackReply(bot, bot.fallback_model_id, systemPrompt, text)
        : "";
      if (fallbackContent) {
        await sendContentToTelegram(bot, chatId, fallbackContent);
        await insertLog(bot.id, "warning", "ai_fallback_reply", "Primary model failed; fallback model replied", user?.id?.toString() ?? null, chatId);
        return jsonResponse({ ok: true, fallback: true });
      }
      await sendTelegramMessage(bot.token_secret ?? "", chatId, "I could not reply right now. Please try again.");
      await insertLog(bot.id, "error", "ai_error", safeMessage(errorText), user?.id?.toString() ?? null, chatId);
      return jsonResponse({ ok: false }, response.status);
    }

    const payload = await response.json();
    const content = extractReply(payload);
    if (!content) {
      await sendTelegramMessage(bot.token_secret ?? "", chatId, "No response was generated.");
      await insertLog(bot.id, "warning", "empty_reply", "AI returned an empty response", user?.id?.toString() ?? null, chatId);
      return jsonResponse({ ok: true });
    }

    await sendContentToTelegram(bot, chatId, content);

    await insertLog(bot.id, "info", "ai_reply", "Sent AI response to Telegram user", user?.id?.toString() ?? null, chatId);
    return jsonResponse({ ok: true });
  } catch (error) {
    return jsonResponse({ error: error instanceof Error ? error.message : String(error) }, 500);
  }
});

async function getFallbackReply(
  bot: TelegramBotRow,
  fallbackModelId: string,
  systemPrompt: string,
  text: string,
): Promise<string> {
  try {
    const response = await fetch(CHAT_ENDPOINT, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        "apikey": SUPABASE_SERVICE_ROLE_KEY,
        "Authorization": `Bearer ${SUPABASE_SERVICE_ROLE_KEY}`,
        "x-nvidia-api-key": NVIDIA_API_KEY,
        "x-gemini-api-key": GEMINI_API_KEY,
      },
      body: JSON.stringify({
        model: fallbackModelId,
        stream: false,
        temperature: bot.temperature,
        max_tokens: bot.max_tokens,
        system_prompt: systemPrompt,
        messages: [{ role: "user", content: text }],
      }),
    });
    if (!response.ok) return "";
    return extractReply(await response.json());
  } catch {
    return "";
  }
}

async function sendContentToTelegram(bot: TelegramBotRow, chatId: string, content: string): Promise<void> {
  const imageUrl = extractImage(content);
  if (imageUrl) {
    const caption = content.replace(/!\[[^\]]*]\([^)]+\)/, '').replace(imageUrl, '').trim();
    await sendTelegramPhoto(bot.token_secret ?? "", chatId, imageUrl, caption || 'Generated image');
    return;
  }

  await sendTelegramMessage(bot.token_secret ?? "", chatId, content);
}

async function getBot(botId: string): Promise<TelegramBotRow | null> {
  if (!SUPABASE_URL || !SUPABASE_SERVICE_ROLE_KEY) return null;

  const response = await fetch(
    `${SUPABASE_URL}/rest/v1/telegram_bots?id=eq.${encodeURIComponent(botId)}&select=*`,
    {
      headers: {
        "apikey": SUPABASE_SERVICE_ROLE_KEY,
        "Authorization": `Bearer ${SUPABASE_SERVICE_ROLE_KEY}`,
      },
    },
  );

  if (!response.ok) return null;
  const rows = await response.json();
  return Array.isArray(rows) ? rows[0] ?? null : null;
}

async function sendTelegramMessage(token: string, chatId: string, text: string): Promise<void> {
  if (!token) return;

  const chunks = chunkText(text, 3900);
  for (const chunk of chunks) {
    await fetch(`https://api.telegram.org/bot${token}/sendMessage`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        chat_id: chatId,
        text: chunk,
        disable_web_page_preview: false,
      }),
    });
  }
}

async function sendTelegramPhoto(token: string, chatId: string, photo: string, caption: string): Promise<void> {
  if (!token) return;
  await fetch(`https://api.telegram.org/bot${token}/sendPhoto`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({
      chat_id: chatId,
      photo,
      caption: caption.slice(0, 1000),
    }),
  });
}

async function insertLog(
  botId: string,
  level: "info" | "warning" | "error",
  event: string,
  message: string,
  telegramUserId: string | null,
  chatId: string | null,
): Promise<void> {
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
      telegram_user_id: telegramUserId,
      chat_id: chatId,
    }),
  });
}

function extractReply(payload: unknown): string {
  const value = payload as {
    choices?: Array<{ message?: { content?: string }; delta?: { content?: string } }>;
    image?: string;
  };
  return value.choices?.[0]?.message?.content
    ?? value.choices?.[0]?.delta?.content
    ?? (typeof value.image === "string" ? `![Generated image](${value.image})` : '');
}

function extractImage(content: string): string | null {
  return content.match(/!\[[^\]]*]\(([^)]+)\)/)?.[1] ?? null;
}

function shouldUseImageModel(prompt: string): boolean {
  const text = prompt.toLowerCase();
  return [
    'generate image',
    'generate an image',
    'generate a picture',
    'generate a photo',
    'create image',
    'create an image',
    'create a picture',
    'create a photo',
    'draw',
    'illustration',
    'poster',
    'wallpaper',
    'logo',
    'text-to-image',
    'image generation',
  ].some(term => text.includes(term));
}

function chunkText(text: string, limit: number): string[] {
  if (text.length <= limit) return [text];
  const chunks: string[] = [];
  let remaining = text;
  while (remaining.length > limit) {
    let splitIndex = remaining.lastIndexOf('\n', limit);
    if (splitIndex < limit / 2) splitIndex = remaining.lastIndexOf(' ', limit);
    if (splitIndex < 1) splitIndex = limit;
    chunks.push(remaining.slice(0, splitIndex).trim());
    remaining = remaining.slice(splitIndex).trim();
  }
  if (remaining) chunks.push(remaining);
  return chunks;
}

function safeMessage(value: string): string {
  return value.replace(/AIza[0-9A-Za-z_-]{20,}/g, "[redacted-gemini-key]").replace(/nvapi-[0-9A-Za-z_-]{20,}/g, "[redacted-nvidia-key]");
}

function jsonResponse(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { ...corsHeaders, "Content-Type": "application/json" },
  });
}
