import { serve } from "https://deno.land/std@0.168.0/http/server.ts";

type Provider = "NVIDIA" | "Google";

type ChatMessage = {
  role: "system" | "user" | "assistant";
  content: string;
};

type ModelInfo = {
  id: string;
  name: string;
  provider: Provider;
  description: string;
  is_free: boolean;
  is_active: boolean;
};

type AppSettings = {
  app_enabled: boolean;
  signup_enabled: boolean;
  maintenance_message: string;
  global_announcement: string;
  default_model_id: string;
  image_model_id: string;
  system_prompt: string;
  temperature: number;
  max_tokens: number;
  latest_version_code: number;
  latest_version_name: string;
  min_supported_version_code: number;
  update_required: boolean;
  update_title: string;
  update_message: string;
  update_apk_url: string;
  update_release_notes: string;
  update_apk_size_mb: number;
  update_sha256: string;
  update_published_at: string;
  update_channel: string;
};

type UsageLog = {
  user_id?: string | null;
  source: string;
  provider?: string | null;
  model_id?: string | null;
  request_type: string;
  status: string;
  prompt_tokens: number;
  completion_tokens: number;
  total_tokens: number;
  latency_ms: number;
  error_message?: string | null;
  metadata?: Record<string, unknown>;
};

const NVIDIA_API_KEY = Deno.env.get("NVIDIA_API_KEY") ?? "";
const GEMINI_API_KEY = Deno.env.get("GEMINI_API_KEY") ?? "";
const SUPABASE_URL = Deno.env.get("SUPABASE_URL") ?? "";
const SUPABASE_SERVICE_ROLE_KEY = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY") ??
  Deno.env.get("SUPABASE_SERVICE_ROLE") ?? "";

const DEFAULT_MODEL_ID = "meta/llama-3.1-8b-instruct";
const DEFAULT_IMAGE_MODEL_ID = "black-forest-labs/flux.1-schnell";
const DEFAULT_SETTINGS: AppSettings = {
  app_enabled: true,
  signup_enabled: true,
  maintenance_message: "M2HAI is temporarily unavailable. Please try again later.",
  global_announcement: "",
  default_model_id: DEFAULT_MODEL_ID,
  image_model_id: DEFAULT_IMAGE_MODEL_ID,
  system_prompt: "",
  temperature: 0.5,
  max_tokens: 1024,
  latest_version_code: 1,
  latest_version_name: "1.0",
  min_supported_version_code: 1,
  update_required: false,
  update_title: "Update available",
  update_message: "",
  update_apk_url: "",
  update_release_notes: "",
  update_apk_size_mb: 0,
  update_sha256: "",
  update_published_at: "",
  update_channel: "stable",
};

const FALLBACK_MODELS: ModelInfo[] = [
  {
    id: "gemini-2.5-flash",
    name: "Gemini 2.5 Flash",
    provider: "Google",
    description: "Fast Gemini chat model",
    is_free: true,
    is_active: false,
  },
  {
    id: "gemini-2.5-flash-lite",
    name: "Gemini 2.5 Flash-Lite",
    provider: "Google",
    description: "Cost-efficient Gemini chat model",
    is_free: true,
    is_active: false,
  },
  {
    id: "gemini-2.5-pro",
    name: "Gemini 2.5 Pro",
    provider: "Google",
    description: "Higher-capability Gemini chat model",
    is_free: true,
    is_active: false,
  },
  {
    id: "gemini-2.0-flash",
    name: "Gemini 2.0 Flash",
    provider: "Google",
    description: "Previous-generation Gemini chat model",
    is_free: true,
    is_active: false,
  },
  {
    id: DEFAULT_IMAGE_MODEL_ID,
    name: "FLUX.1 Schnell",
    provider: "NVIDIA",
    description: "Free NVIDIA image generation endpoint for fast text-to-image prompts",
    is_free: true,
    is_active: true,
  },
  {
    id: "meta/llama-3.1-70b-instruct",
    name: "Llama 3.1 70B Instruct",
    provider: "NVIDIA",
    description: "NVIDIA NIM chat model",
    is_free: true,
    is_active: true,
  },
  {
    id: "meta/llama-3.1-8b-instruct",
    name: "Llama 3.1 8B Instruct",
    provider: "NVIDIA",
    description: "Fast NVIDIA NIM chat model",
    is_free: true,
    is_active: true,
  },
  {
    id: "mistralai/mixtral-8x7b-instruct-v0.1",
    name: "Mixtral 8X7B Instruct",
    provider: "NVIDIA",
    description: "Mistral model via NVIDIA NIM",
    is_free: true,
    is_active: true,
  },
];

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers":
    "authorization, x-client-info, apikey, content-type, x-gemini-api-key, x-nvidia-api-key",
  "Access-Control-Allow-Methods": "GET, POST, OPTIONS",
};

serve(async (req) => {
  if (req.method === "OPTIONS") {
    return new Response("ok", { headers: corsHeaders });
  }

  try {
    if (req.method === "GET") {
      const models = await getAvailableModels();
      return jsonResponse({ data: models });
    }

    if (req.method !== "POST") {
      return jsonResponse({ error: "Method not allowed" }, 405);
    }

    const requestBody = await parseJsonBody(req);
    const appSettings = await getAppSettings();

    if (!appSettings.app_enabled) {
      return jsonResponse({ error: appSettings.maintenance_message }, 503);
    }

    const messages = sanitizeMessages(requestBody.messages ?? []);
    if (messages.length === 0) {
      return jsonResponse({ error: "Messages array is empty" }, 400);
    }

    const model = String(requestBody.model || appSettings.default_model_id || DEFAULT_MODEL_ID);
    const provider = getProvider(model);
    const imageRequest = isImageModel(model);
    const stream = imageRequest ? false : requestBody.stream !== false;
    const temperature = toNumber(requestBody.temperature, appSettings.temperature);
    const maxTokens = toInteger(requestBody.max_tokens, appSettings.max_tokens);
    const systemPrompt = String(requestBody.system_prompt ?? appSettings.system_prompt ?? "");
    const finalMessages = applySystemPrompt(messages, systemPrompt, provider);
    const imagePrompt = messages.filter((message) => message.role === "user").at(-1)?.content ?? "";

    const startedAt = Date.now();
    const providerResponse = imageRequest
      ? await callNvidiaImage({
        apiKey: req.headers.get("x-nvidia-api-key") || NVIDIA_API_KEY,
        model,
        prompt: imagePrompt,
      })
      : provider === "Google"
      ? await callGemini({
        apiKey: req.headers.get("x-gemini-api-key") || GEMINI_API_KEY,
        model,
        messages: finalMessages,
        stream,
        temperature,
        maxTokens,
      })
      : await callNvidia({
        apiKey: req.headers.get("x-nvidia-api-key") || NVIDIA_API_KEY,
        model,
        messages: finalMessages,
        stream,
        temperature,
        maxTokens,
      });

    if (!providerResponse.ok) {
      const details = await providerResponse.text();
      await logUsage({
        source: imageRequest ? "image" : "chat",
        provider,
        model_id: model,
        request_type: imageRequest ? "image" : "chat",
        status: "error",
        prompt_tokens: estimateTokens(messages),
        completion_tokens: 0,
        total_tokens: estimateTokens(messages),
        latency_ms: Date.now() - startedAt,
        error_message: safeProviderError(details),
        metadata: { imageRequest },
      });
      return jsonResponse(
        {
          error: `${imageRequest ? "NVIDIA image" : provider} API failed`,
          details: safeProviderError(details),
        },
        providerResponse.status,
      );
    }

    if (!stream) {
      if (imageRequest) {
        const payload = await normalizeNvidiaImageJson(providerResponse);
        await logUsage({
          source: "image",
          provider,
          model_id: model,
          request_type: "image",
          status: "success",
          prompt_tokens: estimateTokens(messages),
          completion_tokens: 0,
          total_tokens: estimateTokens(messages),
          latency_ms: Date.now() - startedAt,
          metadata: { imageRequest: true },
        });
        return jsonResponse(payload);
      }
      if (provider === "Google") {
        const payload = await normalizeGeminiJson(providerResponse);
        await logUsage({
          source: "chat",
          provider,
          model_id: model,
          request_type: "chat",
          status: "success",
          prompt_tokens: estimateTokens(messages),
          completion_tokens: estimateTokensFromText(JSON.stringify(payload)),
          total_tokens: estimateTokens(messages) + estimateTokensFromText(JSON.stringify(payload)),
          latency_ms: Date.now() - startedAt,
        });
        return jsonResponse(payload);
      }
      const response = new Response(providerResponse.body, {
        headers: { ...corsHeaders, "Content-Type": "application/json" },
      });
      await logUsage({
        source: "chat",
        provider,
        model_id: model,
        request_type: "chat",
        status: "success",
        prompt_tokens: estimateTokens(messages),
        completion_tokens: 0,
        total_tokens: estimateTokens(messages),
        latency_ms: Date.now() - startedAt,
      });
      return response;
    }

    const normalized = provider === "Google"
      ? normalizeGeminiStream(providerResponse)
      : normalizeOpenAIStream(providerResponse);

    void logUsage({
      source: imageRequest ? "image" : "chat",
      provider,
      model_id: model,
      request_type: imageRequest ? "image" : "chat",
      status: "success",
      prompt_tokens: estimateTokens(messages),
      completion_tokens: 0,
      total_tokens: estimateTokens(messages),
      latency_ms: Date.now() - startedAt,
      metadata: { streaming: true },
    });

    return new Response(normalized, {
      headers: {
        ...corsHeaders,
        "Content-Type": "text/event-stream",
        "Cache-Control": "no-cache",
        "Connection": "keep-alive",
      },
    });
  } catch (error) {
    const message = error instanceof Error ? error.message : String(error);
    return jsonResponse({ error: message }, 500);
  }
});

async function parseJsonBody(req: Request): Promise<Record<string, unknown>> {
  try {
    return await req.json();
  } catch (error) {
    const message = error instanceof Error ? error.message : String(error);
    throw new Error(`Failed to parse request body: ${message}`);
  }
}

async function logUsage(entry: UsageLog): Promise<void> {
  if (!SUPABASE_URL || !SUPABASE_SERVICE_ROLE_KEY) return;

  try {
    await fetch(`${SUPABASE_URL}/rest/v1/api_usage`, {
      method: "POST",
      headers: {
        "apikey": SUPABASE_SERVICE_ROLE_KEY,
        "Authorization": `Bearer ${SUPABASE_SERVICE_ROLE_KEY}`,
        "Content-Type": "application/json",
        "Prefer": "return=minimal",
      },
      body: JSON.stringify({
        ...entry,
        created_at: timezoneNow(),
      }),
    });
  } catch {
    // Ignore logging failures so chat responses are unaffected.
  }
}

function estimateTokens(messages: ChatMessage[]): number {
  return messages.reduce((total, message) => total + Math.ceil(message.content.length / 4), 0);
}

function estimateTokensFromText(text: string): number {
  return Math.ceil(text.length / 4);
}

function timezoneNow(): string {
  return new Date().toISOString();
}

async function getAvailableModels(): Promise<ModelInfo[]> {
  const [nvidiaModels, geminiModels] = await Promise.all([
    fetchNvidiaModels(),
    fetchGeminiModels(),
  ]);

  return dedupeModels([
    ...geminiModels,
    ...nvidiaModels,
    ...FALLBACK_MODELS,
  ]).sort((a, b) => `${a.provider}:${a.name}`.localeCompare(`${b.provider}:${b.name}`));
}

async function fetchNvidiaModels(): Promise<ModelInfo[]> {
  if (!NVIDIA_API_KEY) return [];

  try {
    const response = await fetch("https://integrate.api.nvidia.com/v1/models", {
      headers: { "Authorization": `Bearer ${NVIDIA_API_KEY}` },
    });

    if (!response.ok) return [];
    const data = await response.json();
    const items = Array.isArray(data?.data) ? data.data : [];

    return items
      .map((item: { id?: string }) => item.id)
      .filter((id: string | undefined): id is string => Boolean(id))
      .filter(isSupportedNvidiaModel)
      .map((id: string) => ({
        id,
        name: formatModelName(id),
        provider: "NVIDIA" as Provider,
        description: "NVIDIA NIM chat model",
        is_free: true,
        is_active: true,
      }));
  } catch {
    return [];
  }
}

async function fetchGeminiModels(): Promise<ModelInfo[]> {
  if (!GEMINI_API_KEY) return [];

  try {
    const response = await fetch("https://generativelanguage.googleapis.com/v1beta/models", {
      headers: { "x-goog-api-key": GEMINI_API_KEY },
    });

    if (!response.ok) return [];
    const data = await response.json();
    const items = Array.isArray(data?.models) ? data.models : [];

    return items
      .filter((item: { name?: string; supportedGenerationMethods?: string[] }) =>
        item.name && item.supportedGenerationMethods?.includes("generateContent")
      )
      .map((item: { name: string; displayName?: string; description?: string }) => {
        const id = item.name.replace(/^models\//, "");
        return {
          id,
          name: item.displayName || formatModelName(id),
          provider: "Google" as Provider,
          description: item.description || "Google Gemini chat model",
          is_free: true,
          is_active: true,
        };
      });
  } catch {
    return [];
  }
}

function dedupeModels(models: ModelInfo[]): ModelInfo[] {
  const byId = new Map<string, ModelInfo>();
  for (const model of models) {
    if (!byId.has(model.id)) {
      byId.set(model.id, model);
    }
  }
  return Array.from(byId.values());
}

function isSupportedNvidiaModel(modelId: string): boolean {
  const lower = modelId.toLowerCase();
  if (lower.includes("embedding")) return false;
  if (lower.includes("rerank")) return false;
  if (lower.includes("tts")) return false;
  if (lower.includes("stt")) return false;
  if (lower.includes("diffusion")) return false;
  if (lower.includes("sdxl")) return false;
  return true;
}

async function getAppSettings(): Promise<AppSettings> {
  if (!SUPABASE_URL || !SUPABASE_SERVICE_ROLE_KEY) {
    return DEFAULT_SETTINGS;
  }

  try {
    const url = `${SUPABASE_URL}/rest/v1/app_settings?id=eq.global&select=*`;
    const response = await fetch(url, {
      headers: {
        "apikey": SUPABASE_SERVICE_ROLE_KEY,
        "Authorization": `Bearer ${SUPABASE_SERVICE_ROLE_KEY}`,
      },
    });

    if (!response.ok) return DEFAULT_SETTINGS;
    const rows = await response.json();
    const row = Array.isArray(rows) ? rows[0] : undefined;
    if (!row) return DEFAULT_SETTINGS;

    return {
      app_enabled: row.app_enabled ?? DEFAULT_SETTINGS.app_enabled,
      signup_enabled: row.signup_enabled ?? DEFAULT_SETTINGS.signup_enabled,
      maintenance_message: row.maintenance_message ?? DEFAULT_SETTINGS.maintenance_message,
      global_announcement: row.global_announcement ?? DEFAULT_SETTINGS.global_announcement,
      default_model_id: row.default_model_id ?? DEFAULT_SETTINGS.default_model_id,
      image_model_id: row.image_model_id ?? DEFAULT_SETTINGS.image_model_id,
      system_prompt: row.system_prompt ?? DEFAULT_SETTINGS.system_prompt,
      temperature: toNumber(row.temperature, DEFAULT_SETTINGS.temperature),
      max_tokens: toInteger(row.max_tokens, DEFAULT_SETTINGS.max_tokens),
      latest_version_code: toInteger(row.latest_version_code, DEFAULT_SETTINGS.latest_version_code),
      latest_version_name: row.latest_version_name ?? DEFAULT_SETTINGS.latest_version_name,
      min_supported_version_code: toInteger(
        row.min_supported_version_code,
        DEFAULT_SETTINGS.min_supported_version_code,
      ),
      update_required: row.update_required ?? DEFAULT_SETTINGS.update_required,
      update_title: row.update_title ?? DEFAULT_SETTINGS.update_title,
      update_message: row.update_message ?? DEFAULT_SETTINGS.update_message,
      update_apk_url: row.update_apk_url ?? DEFAULT_SETTINGS.update_apk_url,
      update_release_notes: row.update_release_notes ?? DEFAULT_SETTINGS.update_release_notes,
      update_apk_size_mb: toNumber(row.update_apk_size_mb, DEFAULT_SETTINGS.update_apk_size_mb),
      update_sha256: row.update_sha256 ?? DEFAULT_SETTINGS.update_sha256,
      update_published_at: row.update_published_at ?? DEFAULT_SETTINGS.update_published_at,
      update_channel: row.update_channel ?? DEFAULT_SETTINGS.update_channel,
    };
  } catch {
    return DEFAULT_SETTINGS;
  }
}

function sanitizeMessages(input: unknown): ChatMessage[] {
  if (!Array.isArray(input)) return [];

  const messages: ChatMessage[] = [];
  let lastRole: ChatMessage["role"] | null = null;

  for (const item of input) {
    if (!item || typeof item !== "object") continue;

    const rawRole = String((item as { role?: unknown }).role ?? "user").toLowerCase();
    const role: ChatMessage["role"] = rawRole === "assistant" || rawRole === "system"
      ? rawRole
      : "user";
    const content = String((item as { content?: unknown }).content ?? "").trim();

    if (!content) continue;

    if (lastRole === role && role === "user" && messages.length > 0) {
      const previous = messages[messages.length - 1];
      messages[messages.length - 1] = {
        ...previous,
        content: `${previous.content}\n${content}`,
      };
      continue;
    }

    if (lastRole === role && role === "assistant") continue;

    messages.push({ role, content });
    lastRole = role;
  }

  return messages;
}

function applySystemPrompt(
  messages: ChatMessage[],
  systemPrompt: string,
  provider: Provider,
): ChatMessage[] {
  const prompt = systemPrompt.trim();
  if (!prompt) return messages;

  if (provider === "Google") {
    const firstUserIndex = messages.findIndex((message) => message.role === "user");
    if (firstUserIndex === -1) return messages;

    return messages.map((message, index) =>
      index === firstUserIndex
        ? { ...message, content: `${prompt}\n\n${message.content}` }
        : message
    );
  }

  if (messages[0]?.role === "system") {
    return [{ ...messages[0], content: `${messages[0].content}\n\n${prompt}` }, ...messages.slice(1)];
  }

  return [{ role: "system", content: prompt }, ...messages];
}

async function callGemini(options: {
  apiKey: string;
  model: string;
  messages: ChatMessage[];
  stream: boolean;
  temperature: number;
  maxTokens: number;
}): Promise<Response> {
  if (!options.apiKey) throw new Error("GEMINI_API_KEY is not set.");

  const endpoint = options.stream ? "streamGenerateContent?alt=sse" : "generateContent";
  const modelId = options.model.startsWith("models/") ? options.model : `models/${options.model}`;
  const contents = options.messages
    .filter((message) => message.role !== "system")
    .map((message) => ({
      role: message.role === "assistant" ? "model" : "user",
      parts: [{ text: message.content }],
    }));

  return fetch(`https://generativelanguage.googleapis.com/v1beta/${modelId}:${endpoint}`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      "x-goog-api-key": options.apiKey,
    },
    body: JSON.stringify({
      contents,
      generationConfig: {
        temperature: options.temperature,
        maxOutputTokens: options.maxTokens,
      },
    }),
  });
}

async function callNvidia(options: {
  apiKey: string;
  model: string;
  messages: ChatMessage[];
  stream: boolean;
  temperature: number;
  maxTokens: number;
}): Promise<Response> {
  if (!options.apiKey) throw new Error("NVIDIA_API_KEY is not set.");

  return fetch("https://integrate.api.nvidia.com/v1/chat/completions", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      "Authorization": `Bearer ${options.apiKey}`,
    },
    body: JSON.stringify({
      model: options.model,
      messages: options.messages,
      stream: options.stream,
      temperature: options.temperature,
      max_tokens: options.maxTokens,
    }),
  });
}

async function callNvidiaImage(options: {
  apiKey: string;
  model: string;
  prompt: string;
}): Promise<Response> {
  if (!options.apiKey) throw new Error("NVIDIA_API_KEY is not set.");
  if (!options.prompt.trim()) throw new Error("Image prompt is empty.");

  const endpointModel = options.model || DEFAULT_IMAGE_MODEL_ID;
  return fetch(`https://ai.api.nvidia.com/v1/genai/${endpointModel}`, {
    method: "POST",
    headers: {
      "Accept": "application/json",
      "Content-Type": "application/json",
      "Authorization": `Bearer ${options.apiKey}`,
    },
    body: JSON.stringify({
      prompt: options.prompt,
      width: 1024,
      height: 1024,
      steps: 4,
      samples: 1,
    }),
  });
}

function normalizeOpenAIStream(response: Response): ReadableStream<Uint8Array> {
  const source = response.body;
  if (!source) throw new Error("Provider returned an empty stream.");

  const decoder = new TextDecoder();
  const encoder = new TextEncoder();

  return new ReadableStream<Uint8Array>({
    async start(controller) {
      const reader = source.getReader();
      let buffer = "";

      while (true) {
        const { value, done } = await reader.read();
        if (done) break;

        buffer += decoder.decode(value, { stream: true });
        const lines = buffer.split(/\r?\n/);
        buffer = lines.pop() ?? "";

        for (const rawLine of lines) {
          const line = rawLine.trim();
          if (!line) continue;
          controller.enqueue(encoder.encode(line.startsWith("data:") ? `${line}\n\n` : `data: ${line}\n\n`));
        }
      }

      const tail = buffer.trim();
      if (tail) {
        controller.enqueue(encoder.encode(tail.startsWith("data:") ? `${tail}\n\n` : `data: ${tail}\n\n`));
      }
      controller.enqueue(encoder.encode("data: [DONE]\n\n"));
      controller.close();
    },
  });
}

function normalizeGeminiStream(response: Response): ReadableStream<Uint8Array> {
  const source = response.body;
  if (!source) throw new Error("Gemini returned an empty stream.");

  const decoder = new TextDecoder();
  const encoder = new TextEncoder();

  return new ReadableStream<Uint8Array>({
    async start(controller) {
      const reader = source.getReader();
      let buffer = "";

      while (true) {
        const { value, done } = await reader.read();
        if (done) break;

        buffer += decoder.decode(value, { stream: true });
        const lines = buffer.split(/\r?\n/);
        buffer = lines.pop() ?? "";

        for (const rawLine of lines) {
          const line = rawLine.trim();
          if (!line || !line.startsWith("data:")) continue;

          const payload = line.substring(5).trim();
          if (payload === "[DONE]") continue;

          try {
            const data = JSON.parse(payload);
            const text = data?.candidates?.[0]?.content?.parts
              ?.map((part: { text?: string }) => part.text ?? "")
              .join("") ?? "";

            if (text) {
              controller.enqueue(encoder.encode(
                `data: ${JSON.stringify({ choices: [{ delta: { content: text } }] })}\n\n`,
              ));
            }
          } catch {
            // Ignore malformed provider chunks.
          }
        }
      }

      const tail = buffer.trim();
      if (tail.startsWith("data:")) {
        try {
          const data = JSON.parse(tail.substring(5).trim());
          const text = data?.candidates?.[0]?.content?.parts
            ?.map((part: { text?: string }) => part.text ?? "")
            .join("") ?? "";
          if (text) {
            controller.enqueue(encoder.encode(
              `data: ${JSON.stringify({ choices: [{ delta: { content: text } }] })}\n\n`,
            ));
          }
        } catch {
          // Ignore malformed provider tail.
        }
      }

      controller.enqueue(encoder.encode("data: [DONE]\n\n"));
      controller.close();
    },
  });
}

async function normalizeGeminiJson(response: Response): Promise<Record<string, unknown>> {
  const data = await response.json();
  const text = data?.candidates?.[0]?.content?.parts
    ?.map((part: { text?: string }) => part.text ?? "")
    .join("") ?? "";

  return {
    choices: [
      {
        message: {
          role: "assistant",
          content: text,
        },
      },
    ],
  };
}

async function normalizeNvidiaImageJson(response: Response): Promise<Record<string, unknown>> {
  const data = await response.json();
  const image = findImageReference(data);
  if (!image) {
    throw new Error("NVIDIA image API returned no image data.");
  }

  const content = `![Generated image](${image})`;
  return {
    choices: [
      {
        message: {
          role: "assistant",
          content,
        },
      },
    ],
    image,
  };
}

function findImageReference(value: unknown): string | null {
  if (!value) return null;

  if (typeof value === "string") {
    if (value.startsWith("http://") || value.startsWith("https://") || value.startsWith("data:image/")) {
      return value;
    }
    if (value.length > 256 && /^[A-Za-z0-9+/=_-]+$/.test(value)) {
      return `data:image/png;base64,${value}`;
    }
    return null;
  }

  if (Array.isArray(value)) {
    for (const item of value) {
      const image = findImageReference(item);
      if (image) return image;
    }
    return null;
  }

  if (typeof value === "object") {
    const object = value as Record<string, unknown>;
    const likelyKeys = [
      "url",
      "image_url",
      "base64",
      "b64_json",
      "image",
      "artifact",
      "artifacts",
      "data",
    ];
    for (const key of likelyKeys) {
      const image = findImageReference(object[key]);
      if (image) return image;
    }
    for (const item of Object.values(object)) {
      const image = findImageReference(item);
      if (image) return image;
    }
  }

  return null;
}

function getProvider(modelId: string): Provider {
  const lower = modelId.toLowerCase();
  if (lower.startsWith("gemini") || lower.startsWith("models/gemini")) {
    return "Google";
  }
  return "NVIDIA";
}

function isImageModel(modelId: string): boolean {
  const lower = modelId.toLowerCase();
  return lower.includes("flux") ||
    lower.includes("image") ||
    lower.includes("diffusion") ||
    lower.includes("sdxl") ||
    lower.includes("imagen");
}

function formatModelName(modelId: string): string {
  return modelId
    .replace(/^models\//, "")
    .split("/")
    .pop()!
    .replace(/[-_]/g, " ")
    .replace(/\b\w/g, (char) => char.toUpperCase());
}

function safeProviderError(details: string): string {
  return details
    .replace(/AIza[0-9A-Za-z_-]{20,}/g, "[redacted-gemini-key]")
    .replace(/nvapi-[0-9A-Za-z_-]{20,}/g, "[redacted-nvidia-key]");
}

function jsonResponse(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { ...corsHeaders, "Content-Type": "application/json" },
  });
}

function toNumber(value: unknown, fallback: number): number {
  const parsed = Number(value);
  return Number.isFinite(parsed) ? parsed : fallback;
}

function toInteger(value: unknown, fallback: number): number {
  const parsed = Number(value);
  return Number.isFinite(parsed) ? Math.trunc(parsed) : fallback;
}
