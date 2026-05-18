import type { Session, User as SupabaseUser } from '@supabase/supabase-js';
import { userSupabase } from './supabase';
import { DEFAULT_APP_SETTINGS, type AIModel, type AppSettings, type Chat, type Message } from './api';

export interface UserProfile {
  id: string;
  email: string;
  full_name: string | null;
  avatar_url: string | null;
  is_disabled: boolean;
  admin_notes: string | null;
  updated_at: string;
}

export interface WebChatMessage {
  role: 'user' | 'assistant' | 'system';
  content: string;
}

export interface WebChatOptions {
  modelId: string;
  messages: WebChatMessage[];
  signal?: AbortSignal;
  onToken: (content: string) => void;
}

export function getCurrentSession(): Promise<Session | null> {
  return userSupabase.auth.getSession().then(({ data }) => data.session);
}

export function onUserSessionChange(callback: (session: Session | null) => void) {
  return userSupabase.auth.onAuthStateChange((_event, session) => callback(session));
}

export async function signInUser(email: string, password: string): Promise<void> {
  const { error } = await userSupabase.auth.signInWithPassword({ email, password });
  if (error) throw error;
}

export async function registerUser(email: string, password: string, fullName: string): Promise<void> {
  const { error } = await userSupabase.auth.signUp({
    email,
    password,
    options: {
      data: {
        full_name: fullName.trim() || null,
      },
    },
  });
  if (error) throw error;
}

export async function signOutUser(): Promise<void> {
  const { error } = await userSupabase.auth.signOut();
  if (error) throw error;
}

export async function getUserProfile(user: SupabaseUser): Promise<UserProfile> {
  const { data, error } = await userSupabase
    .from('profiles')
    .select('*')
    .eq('id', user.id)
    .maybeSingle();

  if (error) throw error;
  if (data) return data as UserProfile;

  const fallback = {
    id: user.id,
    email: user.email ?? '',
    full_name: user.user_metadata?.full_name ?? null,
    avatar_url: user.user_metadata?.avatar_url ?? null,
    is_disabled: false,
    admin_notes: null,
    updated_at: new Date().toISOString(),
  };
  const { data: inserted, error: insertError } = await userSupabase
    .from('profiles')
    .upsert(fallback)
    .select('*')
    .single();
  if (insertError) throw insertError;
  return inserted as UserProfile;
}

export async function updateUserProfile(userId: string, updates: Pick<UserProfile, 'full_name' | 'avatar_url'>): Promise<void> {
  const { error } = await userSupabase
    .from('profiles')
    .update({ ...updates, updated_at: new Date().toISOString() })
    .eq('id', userId);
  if (error) throw error;
}

export async function getUserAppSettings(): Promise<AppSettings> {
  const { data, error } = await userSupabase
    .from('app_settings')
    .select('*')
    .eq('id', 'global')
    .maybeSingle();
  if (error) throw error;
  return { ...DEFAULT_APP_SETTINGS, ...(data as Partial<AppSettings> | null), id: 'global' };
}

export async function getUserModels(): Promise<AIModel[]> {
  const { data, error } = await userSupabase
    .from('ai_models')
    .select('*')
    .eq('is_active', true)
    .order('provider')
    .order('name');
  if (error) throw error;
  return data as AIModel[];
}

export async function getUserChats(userId: string): Promise<Chat[]> {
  const { data, error } = await userSupabase
    .from('chats')
    .select('*')
    .eq('user_id', userId)
    .order('updated_at', { ascending: false });
  if (error) throw error;
  return data as Chat[];
}

export async function createUserChat(userId: string, modelId: string, title: string): Promise<Chat> {
  const { data, error } = await userSupabase
    .from('chats')
    .insert({
      user_id: userId,
      model_id: modelId,
      title,
    })
    .select('*')
    .single();
  if (error) throw error;
  return data as Chat;
}

export async function updateUserChat(chatId: string, updates: Partial<Pick<Chat, 'title' | 'is_archived' | 'is_pinned' | 'model_id'>>): Promise<void> {
  const { error } = await userSupabase
    .from('chats')
    .update({ ...updates, updated_at: new Date().toISOString() })
    .eq('id', chatId);
  if (error) throw error;
}

export async function deleteUserChat(chatId: string): Promise<void> {
  const { error } = await userSupabase.from('chats').delete().eq('id', chatId);
  if (error) throw error;
}

export async function getUserMessages(chatId: string): Promise<Message[]> {
  const { data, error } = await userSupabase
    .from('messages')
    .select('*')
    .eq('chat_id', chatId)
    .order('created_at', { ascending: true });
  if (error) throw error;
  return data as Message[];
}

export async function saveUserMessage(chatId: string, role: 'user' | 'assistant' | 'system', content: string): Promise<Message> {
  const { data, error } = await userSupabase
    .from('messages')
    .insert({
      chat_id: chatId,
      role,
      content,
      attachments: [],
    })
    .select('*')
    .single();
  if (error) throw error;
  return data as Message;
}

export async function submitUserFeedback(message: Message, userId: string, action: 'copy' | 'like' | 'unlike' | 'share', modelId: string | null): Promise<void> {
  const { error } = await userSupabase
    .from('message_feedback')
    .insert({
      message_id: message.id,
      chat_id: message.chat_id,
      user_id: userId,
      action,
      model_id: modelId,
    });
  if (error) throw error;
}

export async function streamWebChat(options: WebChatOptions): Promise<string> {
  const session = await getCurrentSession();
  const key = import.meta.env.VITE_SUPABASE_ANON_KEY;
  const response = await fetch(`${import.meta.env.VITE_SUPABASE_URL}/functions/v1/chat`, {
    method: 'POST',
    headers: {
      apikey: key,
      Authorization: `Bearer ${session?.access_token ?? key}`,
      'Content-Type': 'application/json',
    },
    signal: options.signal,
    body: JSON.stringify({
      model: options.modelId,
      messages: normalizeMessages(options.messages),
      stream: !isImageModel(options.modelId),
    }),
  });

  if (!response.ok) {
    throw new Error(await response.text());
  }

  if (isImageModel(options.modelId)) {
    const payload = await response.json();
    const content = extractNonStreamingContent(payload);
    options.onToken(content);
    return content;
  }

  if (!response.body) throw new Error('The model returned an empty response stream.');

  const reader = response.body.getReader();
  const decoder = new TextDecoder();
  let buffer = '';
  let content = '';

  while (true) {
    const { value, done } = await reader.read();
    if (done) break;

    buffer += decoder.decode(value, { stream: true });
    const events = buffer.split(/\n\n/);
    buffer = events.pop() ?? '';

    for (const event of events) {
      for (const rawLine of event.split(/\r?\n/)) {
        const line = rawLine.trim();
        if (!line.startsWith('data:')) continue;
        const payload = line.slice(5).trim();
        if (!payload || payload === '[DONE]') continue;

        const token = extractStreamToken(payload);
        if (token) {
          content += token;
          options.onToken(content);
        }
      }
    }
  }

  return content;
}

export async function createUserFeedback(
  message: Message,
  userId: string,
  action: 'copy' | 'like' | 'unlike' | 'share',
  modelId: string | null,
): Promise<void> {
  await submitUserFeedback(message, userId, action, modelId);
}

export function shouldUseImageModel(prompt: string): boolean {
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

export function extractGeneratedImageUrl(content: string): string | null {
  const markdown = content.match(/!\[[^\]]*]\(([^)]+)\)/)?.[1];
  if (markdown) return markdown;
  const dataImage = content.match(/data:image\/[^;\s]+;base64,[A-Za-z0-9+/=_-]+/)?.[0];
  if (dataImage) return dataImage;
  return content.match(/https?:\/\/\S+\.(?:png|jpg|jpeg|webp)(?:\?\S*)?/i)?.[0] ?? null;
}

function normalizeMessages(messages: WebChatMessage[]): WebChatMessage[] {
  return messages
    .filter(message => message.content.trim())
    .slice(-18)
    .map(message => ({ role: message.role, content: message.content.trim() }));
}

function extractStreamToken(payload: string): string {
  try {
    const parsed = JSON.parse(payload);
    return parsed?.choices?.[0]?.delta?.content
      ?? parsed?.choices?.[0]?.message?.content
      ?? '';
  } catch {
    return '';
  }
}

function extractNonStreamingContent(payload: unknown): string {
  const value = payload as {
    image_url?: string;
    data?: Array<{ url?: string; b64_json?: string }>;
    choices?: Array<{ message?: { content?: string }; delta?: { content?: string } }>;
  };
  const imageUrl = value.image_url ?? value.data?.[0]?.url;
  if (imageUrl) return `![Generated image](${imageUrl})`;
  const base64 = value.data?.[0]?.b64_json;
  if (base64) return `![Generated image](data:image/png;base64,${base64})`;
  return value.choices?.[0]?.message?.content
    ?? value.choices?.[0]?.delta?.content
    ?? JSON.stringify(payload);
}

function isImageModel(modelId: string): boolean {
  const lower = modelId.toLowerCase();
  return lower.includes('flux') || lower.includes('image') || lower.includes('sdxl') || lower.includes('diffusion');
}
