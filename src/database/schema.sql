-- M2HAI Database Schema
-- Run this in Supabase SQL Editor

-- Enable UUID extension
create extension if not exists "uuid-ossp";

-- PROFILES table: Extends Supabase Auth users
create table if not exists public.profiles (
  id uuid references auth.users on delete cascade primary key,
  email text unique not null,
  full_name text,
  avatar_url text,
  is_disabled boolean default false,
  admin_notes text,
  updated_at timestamp with time zone default timezone('utc'::text, now()) not null
);

alter table public.profiles
  add column if not exists is_disabled boolean default false;

alter table public.profiles
  add column if not exists admin_notes text;

-- AI MODELS table: Available NVIDIA NIM Models
create table if not exists public.ai_models (
  id text primary key,
  name text not null,
  provider text not null,
  description text,
  is_free boolean default true,
  is_active boolean default true
);

alter table public.ai_models
  add column if not exists is_active boolean default true;

-- APP SETTINGS table: Admin-controlled runtime behavior for the Android app
create table if not exists public.app_settings (
  id text primary key default 'global',
  app_enabled boolean default true not null,
  signup_enabled boolean default true not null,
  maintenance_message text default 'M2HAI is temporarily unavailable. Please try again later.' not null,
  global_announcement text default '' not null,
  default_model_id text default 'meta/llama-3.1-8b-instruct' not null,
  image_model_id text default 'black-forest-labs/flux.1-schnell' not null,
  system_prompt text default '' not null,
  temperature numeric default 0.5 not null,
  max_tokens integer default 1024 not null,
  latest_version_code integer default 1 not null,
  latest_version_name text default '1.0' not null,
  min_supported_version_code integer default 1 not null,
  update_required boolean default false not null,
  update_title text default 'Update available' not null,
  update_message text default '' not null,
  update_apk_url text default '' not null,
  update_release_notes text default '' not null,
  update_apk_size_mb numeric default 0 not null,
  update_sha256 text default '' not null,
  update_published_at text default '' not null,
  update_channel text default 'stable' not null,
  updated_at timestamp with time zone default timezone('utc'::text, now()) not null
);

alter table public.app_settings
  add column if not exists image_model_id text default 'black-forest-labs/flux.1-schnell' not null;

alter table public.app_settings
  add column if not exists latest_version_code integer default 1 not null;

alter table public.app_settings
  add column if not exists latest_version_name text default '1.0' not null;

alter table public.app_settings
  add column if not exists min_supported_version_code integer default 1 not null;

alter table public.app_settings
  add column if not exists update_required boolean default false not null;

alter table public.app_settings
  add column if not exists update_title text default 'Update available' not null;

alter table public.app_settings
  add column if not exists update_message text default '' not null;

alter table public.app_settings
  add column if not exists update_apk_url text default '' not null;

alter table public.app_settings
  add column if not exists update_release_notes text default '' not null;

alter table public.app_settings
  add column if not exists update_apk_size_mb numeric default 0 not null;

alter table public.app_settings
  add column if not exists update_sha256 text default '' not null;

alter table public.app_settings
  add column if not exists update_published_at text default '' not null;

alter table public.app_settings
  add column if not exists update_channel text default 'stable' not null;

insert into public.app_settings (
  id,
  app_enabled,
  signup_enabled,
  maintenance_message,
  global_announcement,
  default_model_id,
  image_model_id,
  system_prompt,
  temperature,
  max_tokens,
  latest_version_code,
  latest_version_name,
  min_supported_version_code,
  update_required,
  update_title,
  update_message,
  update_apk_url,
  update_release_notes,
  update_apk_size_mb,
  update_sha256,
  update_published_at,
  update_channel
) values (
  'global',
  true,
  true,
  'M2HAI is temporarily unavailable. Please try again later.',
  '',
  'meta/llama-3.1-8b-instruct',
  'black-forest-labs/flux.1-schnell',
  '',
  0.5,
  1024,
  1,
  '1.0',
  1,
  false,
  'Update available',
  '',
  '',
  '',
  0,
  '',
  '',
  ''
) on conflict (id) do nothing;

-- Insert supported NVIDIA and Gemini chat models
insert into public.ai_models (id, name, provider, description, is_free, is_active)
values
('gemini-2.5-flash', 'Gemini 2.5 Flash', 'Google', 'Fast Gemini chat model', true, false),
('gemini-2.5-flash-lite', 'Gemini 2.5 Flash-Lite', 'Google', 'Cost-efficient Gemini chat model', true, false),
('gemini-2.5-pro', 'Gemini 2.5 Pro', 'Google', 'Higher-capability Gemini chat model', true, false),
('gemini-2.0-flash', 'Gemini 2.0 Flash', 'Google', 'Previous-generation Gemini chat model', true, false),
('black-forest-labs/flux.1-schnell', 'FLUX.1 Schnell', 'NVIDIA', 'Free NVIDIA text-to-image generation model', true, true),
('deepseek-ai/deepseek-v4-flash', 'DeepSeek V4 Flash', 'DeepSeek', 'Verified NVIDIA NIM chat model', true, true),
('meta/llama-3.1-70b-instruct', 'Llama 3.1 70B', 'NVIDIA', 'Balanced high performance model', true, true),
('meta/llama-3.1-8b-instruct', 'Llama 3.1 8B', 'NVIDIA', 'Fast and efficient for simple tasks', true, true),
('meta/llama-3.2-90b-vision-instruct', 'Llama 3.2 90B Vision', 'Meta', 'Verified NVIDIA NIM chat model', true, true),
('meta/llama-3.3-70b-instruct', 'Llama 3.3 70B', 'Meta', 'Verified NVIDIA NIM chat model', true, true),
('meta/llama-4-maverick-17b-128e-instruct', 'Llama 4 Maverick', 'Meta', 'Verified NVIDIA NIM chat model', true, true),
('mistralai/mistral-medium-3.5-128b', 'Mistral Medium 3.5', 'Mistral AI', 'Verified NVIDIA NIM chat model', true, true),
('mistralai/mistral-small-4-119b-2603', 'Mistral Small 4', 'Mistral AI', 'Verified NVIDIA NIM chat model', true, true),
('mistralai/mixtral-8x22b-instruct-v0.1', 'Mixtral 8x22B', 'Mistral', 'Verified NVIDIA NIM chat model', true, true),
('mistralai/mixtral-8x7b-instruct-v0.1', 'Mixtral 8x7B', 'NVIDIA', 'Mistral model via NVIDIA NIM', true, true),
('qwen/qwen3-next-80b-a3b-instruct', 'Qwen3 Next 80B Instruct', 'Qwen', 'Verified NVIDIA NIM chat model', true, true),
('qwen/qwen3-next-80b-a3b-thinking', 'Qwen3 Next 80B Thinking', 'Qwen', 'Verified NVIDIA NIM chat model', true, true)
on conflict (id) do update set
  name = excluded.name,
  provider = excluded.provider,
  description = excluded.description,
  is_free = excluded.is_free,
  is_active = excluded.is_active;

-- CHATS table: Conversation headers
create table if not exists public.chats (
  id uuid default uuid_generate_v4() primary key,
  user_id uuid references public.profiles(id) on delete cascade not null,
  title text default 'New Chat',
  model_id text references public.ai_models(id) not null,
  is_archived boolean default false,
  is_pinned boolean default false,
  created_at timestamp with time zone default timezone('utc'::text, now()) not null,
  updated_at timestamp with time zone default timezone('utc'::text, now()) not null
);

-- MESSAGES table: Actual chat messages
create table if not exists public.messages (
  id uuid default uuid_generate_v4() primary key,
  chat_id uuid references public.chats(id) on delete cascade not null,
  role text check (role in ('user', 'assistant', 'system')) not null,
  content text not null,
  attachments jsonb default '[]'::jsonb,
  created_at timestamp with time zone default timezone('utc'::text, now()) not null
);

-- MESSAGE FEEDBACK table: User actions on assistant responses
create table if not exists public.message_feedback (
  id uuid default uuid_generate_v4() primary key,
  message_id uuid references public.messages(id) on delete cascade not null,
  chat_id uuid references public.chats(id) on delete cascade not null,
  user_id uuid references public.profiles(id) on delete cascade not null,
  action text check (action in ('copy', 'like', 'unlike', 'share')) not null,
  model_id text,
  created_at timestamp with time zone default timezone('utc'::text, now()) not null
);

-- Trigger to automatically create a profile when a new user signs up
create or replace function public.handle_new_user()
returns trigger as $$
begin
  insert into public.profiles (id, email, full_name, avatar_url)
  values (new.id, new.email, new.raw_user_meta_data->>'full_name', new.raw_user_meta_data->>'avatar_url');
  return new;
end;
$$ language plpgsql security definer;

-- Only create the trigger if it doesn't exist
do $$
begin
  if not exists (select 1 from pg_trigger where tgname = 'on_auth_user_created') then
    create trigger on_auth_user_created
      after insert on auth.users
      for each row execute procedure public.handle_new_user();
  end if;
end $$;

-- Row Level Security (RLS) Policies

-- Profiles
alter table public.profiles enable row level security;
create policy "Users can view their own profile" on public.profiles for select using (auth.uid() = id);
create policy "Users can update their own profile" on public.profiles for update using (auth.uid() = id);
create policy "Users can insert their own profile" on public.profiles for insert with check (auth.uid() = id);

-- AI Models
alter table public.ai_models enable row level security;
create policy "Anyone can view AI models" on public.ai_models for select using (true);

-- App Settings
alter table public.app_settings enable row level security;
create policy "Anyone can view app settings" on public.app_settings for select using (true);

-- Chats
alter table public.chats enable row level security;
create policy "Users can manage their own chats" on public.chats for all using (auth.uid() = user_id);

-- Messages
alter table public.messages enable row level security;
create policy "Users can view messages in their chats" on public.messages for select
using (exists (select 1 from public.chats where id = messages.chat_id and user_id = auth.uid()));

create policy "Users can insert messages in their chats" on public.messages for insert
with check (exists (select 1 from public.chats where id = messages.chat_id and user_id = auth.uid()));

-- Message feedback
alter table public.message_feedback enable row level security;
create policy "Users can insert feedback for their chat messages" on public.message_feedback for insert
with check (
  auth.uid() = user_id
  and exists (select 1 from public.chats where id = message_feedback.chat_id and user_id = auth.uid())
);

create policy "Users can view their own message feedback" on public.message_feedback for select
using (auth.uid() = user_id);
