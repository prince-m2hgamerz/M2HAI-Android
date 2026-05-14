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
  updated_at timestamp with time zone default timezone('utc'::text, now()) not null
);

-- AI MODELS table: Available NVIDIA NIM Models
create table if not exists public.ai_models (
  id text primary key,
  name text not null,
  provider text not null,
  description text,
  is_free boolean default true
);

-- Insert Free NVIDIA Models (Checking NVIDIA NIM catalog for currently free/available models)
insert into public.ai_models (id, name, provider, description, is_free)
values
('meta/llama-3.1-405b-instruct', 'Llama 3.1 405B', 'NVIDIA', 'NVIDIA flagship high-capacity model', true),
('meta/llama-3.1-70b-instruct', 'Llama 3.1 70B', 'NVIDIA', 'Balanced high performance model', true),
('meta/llama-3.1-8b-instruct', 'Llama 3.1 8B', 'NVIDIA', 'Fast and efficient for simple tasks', true),
('nvidia/llama-3.1-nemotron-70b-instruct', 'Llama 3.1 Nemotron 70B', 'NVIDIA', 'NVIDIA optimized for helpfulness and correctness', true),
('mistralai/mixtral-8x7b-instruct-v0.1', 'Mixtral 8x7B', 'Mistral AI', 'High quality open weight model', true),
('mistralai/mistral-large-2-instruct', 'Mistral Large 2', 'Mistral AI', 'State-of-the-art flagship model from Mistral', true),
('google/gemma-2-27b-it', 'Gemma 2 27B', 'Google', 'Lightweight and highly capable', true),
('google/gemma-2-9b-it', 'Gemma 2 9B', 'Google', 'Compact yet powerful reasoning', true),
('microsoft/phi-3-medium-128k-instruct', 'Phi-3 Medium', 'Microsoft', 'Strong reasoning in a compact size', true),
('deepseek-ai/deepseek-v2-chat', 'DeepSeek V2', 'DeepSeek', 'High performance reasoning model', true),
('nvidia/nemotron-4-340b-instruct', 'Nemotron 4 340B', 'NVIDIA', 'NVIDIA proprietary high-scale model', true)
on conflict (id) do update set
  name = excluded.name,
  description = excluded.description,
  is_free = excluded.is_free;

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

-- Chats
alter table public.chats enable row level security;
create policy "Users can manage their own chats" on public.chats for all using (auth.uid() = user_id);

-- Messages
alter table public.messages enable row level security;
create policy "Users can view messages in their chats" on public.messages for select
using (exists (select 1 from public.chats where id = messages.chat_id and user_id = auth.uid()));

create policy "Users can insert messages in their chats" on public.messages for insert
with check (exists (select 1 from public.chats where id = messages.chat_id and user_id = auth.uid()));
