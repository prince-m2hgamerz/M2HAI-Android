import { supabase } from './supabase';

export interface DashboardStats {
  totalUsers: number;
  totalChats: number;
  totalMessages: number;
  totalModels: number;
  activeModels: number;
  disabledUsers: number;
  newUsersToday: number;
  activeChatsToday: number;
}

export interface User {
  id: string;
  email: string;
  full_name: string | null;
  avatar_url: string | null;
  is_disabled: boolean;
  admin_notes: string | null;
  updated_at: string;
}

export interface Chat {
  id: string;
  user_id: string;
  title: string;
  model_id: string;
  is_archived: boolean;
  is_pinned: boolean;
  created_at: string;
  updated_at: string;
}

export interface Message {
  id: string;
  chat_id: string;
  role: string;
  content: string;
  created_at: string;
}

export interface MessageFeedback {
  id: string;
  message_id: string;
  chat_id: string;
  user_id: string;
  action: 'copy' | 'like' | 'unlike' | 'share';
  model_id: string | null;
  created_at: string;
}

export interface FeedbackStats {
  total: number;
  copy: number;
  like: number;
  unlike: number;
  share: number;
}

export interface PlatformOverview {
  totalUsers: number;
  activeChats: number;
  aiRequests: number;
  activeModels: number;
  telegramBots: number;
  apiUsage: number;
  systemUptime: string;
  newUsersToday: number;
  activeChatsToday: number;
}

export interface TelegramBot {
  id: string;
  name: string;
  username: string | null;
  token_hint: string | null;
  token_secret?: string | null;
  is_enabled: boolean;
  model_id: string;
  fallback_model_id: string | null;
  system_prompt: string;
  personality: string;
  temperature: number;
  max_tokens: number;
  welcome_message: string;
  commands: string[];
  rate_limit_per_minute: number;
  daily_message_limit: number;
  enable_voice: boolean;
  enable_images: boolean;
  enable_files: boolean;
  language_mode: string;
  webhook_url: string | null;
  avatar_url: string | null;
  banner_url: string | null;
  created_at: string;
  updated_at: string;
}

export interface TelegramBotLog {
  id: string;
  bot_id: string;
  level: 'info' | 'warning' | 'error';
  event: string;
  message: string;
  telegram_user_id: string | null;
  chat_id: string | null;
  metadata: Record<string, unknown> | null;
  created_at: string;
}

export type TelegramBotUpsert = Omit<TelegramBot, 'id' | 'created_at' | 'updated_at'> & {
  id?: string;
  bot_token?: string;
};

export interface AIModel {
  id: string;
  name: string;
  provider: string;
  description: string | null;
  is_free: boolean;
  is_active: boolean;
}

export interface AppSettings {
  id: 'global';
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
  updated_at?: string;
}

export interface AppRelease {
  id: string;
  versionName: string;
  versionCode: number;
  channel: string;
  apkUrl: string;
  fileName: string;
  sizeMb: number;
  sha256: string;
  releaseNotes: string;
  publishedAt: string;
  required: boolean;
  minSupportedVersionCode: number;
  isLatest: boolean;
  source: 'settings' | 'storage';
}

export const DEFAULT_APP_SETTINGS: AppSettings = {
  id: 'global',
  app_enabled: true,
  signup_enabled: true,
  maintenance_message: 'M2HAI is temporarily unavailable. Please try again later.',
  global_announcement: '',
  default_model_id: 'meta/llama-3.1-8b-instruct',
  image_model_id: 'black-forest-labs/flux.1-schnell',
  system_prompt: '',
  temperature: 0.5,
  max_tokens: 1024,
  latest_version_code: 1,
  latest_version_name: '1.0',
  min_supported_version_code: 1,
  update_required: false,
  update_title: 'Update available',
  update_message: '',
  update_apk_url: '',
  update_release_notes: '',
  update_apk_size_mb: 0,
  update_sha256: '',
  update_published_at: '',
  update_channel: 'stable',
};

// ─── Dashboard Stats ────────────────────────────────────────────────────────
export async function getDashboardStats(): Promise<DashboardStats> {
  return adminApi<DashboardStats>({ action: 'dashboardStats' });
}

export async function getPlatformOverview(): Promise<PlatformOverview> {
  return adminApi<PlatformOverview>({ action: 'platformOverview' });
}

export function subscribeToPlatformOverview(onChange: () => void): () => void {
  const tables = ['profiles', 'chats', 'messages', 'ai_models', 'telegram_bots', 'api_usage', 'message_feedback'];
  const channel = tables.reduce((nextChannel, table) => (
    nextChannel.on('postgres_changes', { event: '*', schema: 'public', table }, onChange)
  ), supabase.channel('platform-overview'));

  channel.subscribe();
  return () => {
    void supabase.removeChannel(channel);
  };
}

// ─── Users ──────────────────────────────────────────────────────────────────
export async function getUsers(page = 0, pageSize = 20, search = ''): Promise<{ data: User[]; count: number }> {
  return adminApi<{ data: User[]; count: number }>({
    action: 'listUsers',
    page,
    pageSize,
    search,
  });
}

export async function deleteUser(userId: string): Promise<void> {
  await adminApi<void>({ action: 'deleteUser', userId });
}

export async function updateUser(userId: string, updates: Partial<Pick<User, 'full_name' | 'avatar_url' | 'is_disabled' | 'admin_notes'>>): Promise<void> {
  await adminApi<void>({ action: 'updateUser', userId, updates });
}

// ─── Chats ──────────────────────────────────────────────────────────────────
export async function getChats(page = 0, pageSize = 20, search = ''): Promise<{ data: Chat[]; count: number }> {
  return adminApi<{ data: Chat[]; count: number }>({
    action: 'listChats',
    page,
    pageSize,
    search,
  });
}

export async function deleteChat(chatId: string): Promise<void> {
  await adminApi<void>({ action: 'deleteChat', chatId });
}

export async function updateChat(chatId: string, updates: Partial<Pick<Chat, 'title' | 'is_archived' | 'is_pinned' | 'model_id'>>): Promise<void> {
  await adminApi<void>({ action: 'updateChat', chatId, updates });
}

export async function getChatMessages(chatId: string): Promise<Message[]> {
  return adminApi<Message[]>({ action: 'chatMessages', chatId });
}

export async function getMessageFeedback(page = 0, pageSize = 20): Promise<{ data: MessageFeedback[]; count: number }> {
  return adminApi<{ data: MessageFeedback[]; count: number }>({
    action: 'listFeedback',
    page,
    pageSize,
  });
}

export async function getFeedbackStats(): Promise<FeedbackStats> {
  return adminApi<FeedbackStats>({ action: 'feedbackStats' });
}

// ─── AI Models ──────────────────────────────────────────────────────────────
export async function getModels(): Promise<AIModel[]> {
  return adminApi<AIModel[]>({ action: 'listModels' });
}

export async function createModel(model: AIModel): Promise<void> {
  await adminApi<void>({ action: 'createModel', model });
}

export async function updateModel(id: string, updates: Partial<AIModel>): Promise<void> {
  await adminApi<void>({ action: 'updateModel', modelId: id, updates });
}

export async function deleteModel(id: string): Promise<void> {
  await adminApi<void>({ action: 'deleteModel', modelId: id });
}

export async function syncProviderModels(): Promise<number> {
  const response = await fetch(`${import.meta.env.VITE_SUPABASE_URL}/functions/v1/chat`, {
    method: 'GET',
    headers: functionHeaders(),
  });

  if (!response.ok) {
    throw new Error(await response.text());
  }

  const payload = await response.json();
  const models = ((payload.data ?? []) as Partial<AIModel>[])
    .filter((model): model is AIModel => Boolean(model.id && model.name && model.provider))
    .map(model => ({
      id: model.id,
      name: model.name,
      provider: model.provider,
      description: model.description ?? null,
      is_free: model.is_free ?? true,
      is_active: model.is_active ?? true,
    }));

  if (models.length === 0) return 0;

  await adminApi<number>({ action: 'upsertModels', models });
  return models.length;
}

export async function testModel(modelId: string, timeoutMs = 45000): Promise<string> {
  const controller = new AbortController();
  const timeoutId = window.setTimeout(() => controller.abort(), timeoutMs);
  let response: Response;

  try {
    response = await fetch(`${import.meta.env.VITE_SUPABASE_URL}/functions/v1/chat`, {
      method: 'POST',
      headers: {
        ...functionHeaders(),
        'Content-Type': 'application/json',
      },
      signal: controller.signal,
      body: JSON.stringify({
        model: modelId,
        messages: [{ role: 'user', content: 'Reply with exactly: OK' }],
        stream: false,
        max_tokens: 32,
      }),
    });
  } catch (error) {
    if (error instanceof DOMException && error.name === 'AbortError') {
      throw new Error(`Timed out after ${Math.round(timeoutMs / 1000)}s`, { cause: error });
    }
    throw error;
  } finally {
    window.clearTimeout(timeoutId);
  }

  const text = await response.text();
  if (!response.ok) {
    throw new Error(text);
  }

  try {
    const payload = JSON.parse(text);
    return payload?.choices?.[0]?.message?.content
      ?? payload?.choices?.[0]?.delta?.content
      ?? 'OK';
  } catch {
    return text || 'OK';
  }
}

export interface ModelTestResult {
  id: string;
  ok: boolean;
  reply: string;
}

export async function testModels(
  models: AIModel[],
  onResult?: (result: ModelTestResult, index: number, total: number) => void,
): Promise<ModelTestResult[]> {
  const results: ModelTestResult[] = [];
  for (let index = 0; index < models.length; index += 1) {
    const model = models[index];
    let result: ModelTestResult;
    try {
      const reply = await testModel(model.id);
      result = { id: model.id, ok: true, reply };
    } catch (error) {
      result = { id: model.id, ok: false, reply: error instanceof Error ? error.message : String(error) };
    }
    results.push(result);
    onResult?.(result, index + 1, models.length);
  }
  return results;
}

export async function activateOnlyModels(modelIds: string[]): Promise<void> {
  await adminApi<void>({ action: 'activateOnlyModels', modelIds });
}

export async function getAppSettings(): Promise<AppSettings> {
  const data = await adminApi<Partial<AppSettings> | null>({ action: 'getAppSettings' });
  return { ...DEFAULT_APP_SETTINGS, ...(data as Partial<AppSettings> | null), id: 'global' };
}

export async function getAppReleases(): Promise<AppRelease[]> {
  try {
    return await adminApi<AppRelease[]>({ action: 'listAppReleases' });
  } catch (error) {
    const message = error instanceof Error ? error.message : String(error);
    if (!message.includes('Unsupported action')) throw error;
    return getAppReleasesFromStorage();
  }
}

export async function getSystemHealth(): Promise<{
  edgeCatalogOk: boolean;
  edgeChatOk: boolean;
  databaseOk: boolean;
  activeModels: number;
  defaultModel: string;
  details: string;
}> {
  const settings = await getAppSettings();
  const models = await getModels();
  let edgeCatalogOk = false;
  let edgeChatOk = false;
  const details: string[] = [];

  try {
    const response = await fetch(`${import.meta.env.VITE_SUPABASE_URL}/functions/v1/chat`, {
      method: 'GET',
      headers: functionHeaders(),
    });
    edgeCatalogOk = response.ok;
    if (!response.ok) details.push(`Catalog HTTP ${response.status}`);
  } catch (error) {
    details.push(error instanceof Error ? error.message : String(error));
  }

  try {
    await testModel(settings.default_model_id);
    edgeChatOk = true;
  } catch (error) {
    details.push(error instanceof Error ? error.message : String(error));
  }

  return {
    edgeCatalogOk,
    edgeChatOk,
    databaseOk: true,
    activeModels: models.filter(model => model.is_active).length,
    defaultModel: settings.default_model_id,
    details: details.join(' | ') || 'All checks passed',
  };
}

export async function updateAppSettings(settings: AppSettings): Promise<void> {
  await adminApi<void>({ action: 'updateAppSettings', settings });
}

export async function uploadReleaseApk(file: File, versionName: string, versionCode: number): Promise<{
  publicUrl: string;
  sizeMb: number;
  sha256: string;
}> {
  const safeVersion = versionName.replace(/[^0-9A-Za-z._-]/g, '-');
  const path = `android/m2hai-${safeVersion}-${versionCode}.apk`;
  const { error } = await supabase.storage
    .from('app-updates')
    .upload(path, file, {
      cacheControl: '3600',
      contentType: 'application/vnd.android.package-archive',
      upsert: true,
    });

  if (error) throw error;

  const { data } = supabase.storage.from('app-updates').getPublicUrl(path);
  return {
    publicUrl: data.publicUrl,
    sizeMb: Number((file.size / 1024 / 1024).toFixed(1)),
    sha256: await sha256File(file),
  };
}

async function sha256File(file: File): Promise<string> {
  const buffer = await file.arrayBuffer();
  const hash = await crypto.subtle.digest('SHA-256', buffer);
  return Array.from(new Uint8Array(hash))
    .map(byte => byte.toString(16).padStart(2, '0'))
    .join('');
}

async function getAppReleasesFromStorage(): Promise<AppRelease[]> {
  const settings = await getAppSettings();
  const releases: AppRelease[] = [];

  if (settings.update_apk_url) {
    releases.push({
      id: `latest-${settings.latest_version_code}`,
      versionName: settings.latest_version_name || 'Latest',
      versionCode: Number(settings.latest_version_code || 0),
      channel: settings.update_channel || 'stable',
      apkUrl: settings.update_apk_url,
      fileName: fileNameFromUrl(settings.update_apk_url),
      sizeMb: Number(settings.update_apk_size_mb || 0),
      sha256: settings.update_sha256 || '',
      releaseNotes: settings.update_release_notes || settings.update_message || '',
      publishedAt: settings.update_published_at || settings.updated_at || '',
      required: Boolean(settings.update_required),
      minSupportedVersionCode: Number(settings.min_supported_version_code || 0),
      isLatest: true,
      source: 'settings',
    });
  }

  const { data, error } = await supabase.storage
    .from('app-updates')
    .list('android', {
      limit: 100,
      sortBy: { column: 'updated_at', order: 'desc' },
    });

  if (error) {
    if (releases.length > 0) return releases;
    throw error;
  }

  for (const item of data ?? []) {
    if (!item.name.toLowerCase().endsWith('.apk')) continue;
    const path = `android/${item.name}`;
    const { data: publicData } = supabase.storage.from('app-updates').getPublicUrl(path);
    const parsed = parseApkFileName(item.name);
    const apkUrl = publicData.publicUrl;

    if (releases.some(release => normalizeUrl(release.apkUrl) === normalizeUrl(apkUrl))) continue;

    const metadata = (item.metadata ?? {}) as Record<string, unknown>;
    releases.push({
      id: path,
      versionName: parsed.versionName,
      versionCode: parsed.versionCode,
      channel: 'archive',
      apkUrl,
      fileName: item.name,
      sizeMb: bytesToMb(Number(metadata.size ?? 0)),
      sha256: '',
      releaseNotes: '',
      publishedAt: item.updated_at ?? item.created_at ?? '',
      required: false,
      minSupportedVersionCode: 0,
      isLatest: false,
      source: 'storage',
    });
  }

  return sortAppReleases(releases);
}

function parseApkFileName(fileName: string): { versionName: string; versionCode: number } {
  const match = fileName.match(/^m2hai-(.+)-(\d+)\.apk$/i);
  if (!match) return { versionName: fileName.replace(/\.apk$/i, ''), versionCode: 0 };
  return { versionName: match[1], versionCode: Number(match[2]) || 0 };
}

function fileNameFromUrl(url: string): string {
  try {
    return decodeURIComponent(new URL(url).pathname.split('/').pop() || 'm2hai.apk');
  } catch {
    return 'm2hai.apk';
  }
}

function normalizeUrl(url: string): string {
  return url.trim().replace(/\?.*$/, '').replace(/\/+$/, '');
}

function bytesToMb(bytes: number): number {
  if (!Number.isFinite(bytes) || bytes <= 0) return 0;
  return Number((bytes / 1024 / 1024).toFixed(1));
}

function sortAppReleases(releases: AppRelease[]): AppRelease[] {
  return [...releases].sort((a, b) => {
    if (a.isLatest !== b.isLatest) return a.isLatest ? -1 : 1;
    if (a.versionCode !== b.versionCode) return b.versionCode - a.versionCode;
    return new Date(b.publishedAt || 0).getTime() - new Date(a.publishedAt || 0).getTime();
  });
}

function functionHeaders(): Record<string, string> {
  const key = import.meta.env.VITE_SUPABASE_SERVICE_ROLE || import.meta.env.VITE_SUPABASE_ANON_KEY;
  return {
    apikey: key,
    Authorization: `Bearer ${key}`,
  };
}

// ─── Growth Chart (last 7 days) ─────────────────────────────────────────────
export async function getUserGrowth(): Promise<{ date: string; users: number; chats: number }[]> {
  return adminApi<{ date: string; users: number; chats: number }[]>({ action: 'userGrowth' });
}

export async function getTelegramBots(): Promise<TelegramBot[]> {
  return telegramAdmin<TelegramBot[]>({ action: 'listBots' });
}

export async function upsertTelegramBot(bot: TelegramBotUpsert): Promise<TelegramBot> {
  const payload = {
    ...bot,
    token_hint: bot.bot_token ? maskToken(bot.bot_token) : bot.token_hint,
    updated_at: new Date().toISOString(),
  };

  return telegramAdmin<TelegramBot>({
    action: 'upsertBot',
    bot: payload,
  });
}

export async function updateTelegramBot(id: string, updates: Partial<TelegramBot>): Promise<void> {
  await telegramAdmin<void>({
    action: 'updateBot',
    botId: id,
    updates: { ...updates, updated_at: new Date().toISOString() },
  });
}

export async function deleteTelegramBot(id: string): Promise<void> {
  await telegramAdmin<void>({ action: 'deleteBot', botId: id });
}

export async function getTelegramBotLogs(botId?: string, limit = 80): Promise<TelegramBotLog[]> {
  return telegramAdmin<TelegramBotLog[]>({
    action: 'listLogs',
    botId,
    limit,
  });
}

export async function setupTelegramWebhook(botId: string, botToken: string, webhookBaseUrl: string): Promise<string> {
  const endpoint = `${import.meta.env.VITE_SUPABASE_URL}/functions/v1/telegram-admin`;
  const response = await fetch(endpoint, {
    method: 'POST',
    headers: {
      ...functionHeaders(),
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({
      action: 'setWebhook',
      botId,
      botToken,
      webhookBaseUrl,
    }),
  });

  if (!response.ok) throw new Error(await response.text());
  const payload = await response.json();
  return payload.webhookUrl ?? '';
}

async function telegramAdmin<T>(body: Record<string, unknown>): Promise<T> {
  const endpoint = `${import.meta.env.VITE_SUPABASE_URL}/functions/v1/telegram-admin`;
  const response = await fetch(endpoint, {
    method: 'POST',
    headers: {
      ...functionHeaders(),
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(body),
  });

  const text = await response.text();
  if (!response.ok) {
    throw new Error(text || `Telegram admin request failed with HTTP ${response.status}`);
  }
  if (!text) return undefined as T;
  const payload = JSON.parse(text);
  return (payload.data ?? payload) as T;
}

async function adminApi<T>(body: Record<string, unknown>): Promise<T> {
  const endpoint = `${import.meta.env.VITE_SUPABASE_URL}/functions/v1/admin-api`;
  const response = await fetch(endpoint, {
    method: 'POST',
    headers: {
      ...functionHeaders(),
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(body),
  });

  const text = await response.text();
  if (!response.ok) {
    throw new Error(text || `Admin API request failed with HTTP ${response.status}`);
  }
  if (!text) return undefined as T;
  const payload = JSON.parse(text);
  return (payload.data ?? payload) as T;
}

function maskToken(token: string): string {
  const trimmed = token.trim();
  if (trimmed.length <= 10) return 'configured';
  return `${trimmed.slice(0, 6)}...${trimmed.slice(-4)}`;
}
