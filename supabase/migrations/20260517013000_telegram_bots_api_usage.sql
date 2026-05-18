create extension if not exists "uuid-ossp";

create table if not exists public.telegram_bots (
  id uuid default uuid_generate_v4() primary key,
  name text not null,
  username text,
  token_hint text,
  token_secret text,
  is_enabled boolean default true not null,
  model_id text not null default 'meta/llama-3.1-8b-instruct',
  fallback_model_id text,
  system_prompt text default '' not null,
  personality text default 'Helpful, concise, and professional.' not null,
  temperature numeric default 0.5 not null,
  max_tokens integer default 1024 not null,
  welcome_message text default 'Welcome to M2HAI. Send a message to start.' not null,
  commands text[] default array['start', 'help', 'model', 'image']::text[] not null,
  rate_limit_per_minute integer default 12 not null,
  daily_message_limit integer default 300 not null,
  enable_voice boolean default false not null,
  enable_images boolean default true not null,
  enable_files boolean default false not null,
  language_mode text default 'auto' not null,
  webhook_url text,
  avatar_url text,
  banner_url text,
  created_at timestamp with time zone default timezone('utc'::text, now()) not null,
  updated_at timestamp with time zone default timezone('utc'::text, now()) not null
);

create table if not exists public.telegram_bot_logs (
  id uuid default uuid_generate_v4() primary key,
  bot_id uuid references public.telegram_bots(id) on delete cascade,
  level text check (level in ('info', 'warning', 'error')) default 'info' not null,
  event text not null,
  message text not null,
  telegram_user_id text,
  chat_id text,
  metadata jsonb default '{}'::jsonb,
  created_at timestamp with time zone default timezone('utc'::text, now()) not null
);

create table if not exists public.api_usage (
  id uuid default uuid_generate_v4() primary key,
  user_id uuid,
  source text default 'web' not null,
  provider text,
  model_id text,
  request_type text default 'chat' not null,
  status text default 'success' not null,
  prompt_tokens integer default 0 not null,
  completion_tokens integer default 0 not null,
  total_tokens integer default 0 not null,
  latency_ms integer default 0 not null,
  error_message text,
  metadata jsonb default '{}'::jsonb,
  created_at timestamp with time zone default timezone('utc'::text, now()) not null
);

create index if not exists telegram_bots_enabled_idx on public.telegram_bots (is_enabled, updated_at desc);
create index if not exists telegram_bot_logs_bot_created_idx on public.telegram_bot_logs (bot_id, created_at desc);
create index if not exists api_usage_created_idx on public.api_usage (created_at desc);
create index if not exists api_usage_model_idx on public.api_usage (model_id, created_at desc);

alter table public.telegram_bots enable row level security;
alter table public.telegram_bot_logs enable row level security;
alter table public.api_usage enable row level security;

drop policy if exists "Authenticated users can view enabled Telegram bots" on public.telegram_bots;

drop policy if exists "Service role can manage Telegram bots" on public.telegram_bots;
create policy "Service role can manage Telegram bots"
  on public.telegram_bots
  for all
  using (auth.role() = 'service_role')
  with check (auth.role() = 'service_role');

drop policy if exists "Service role can manage Telegram bot logs" on public.telegram_bot_logs;
create policy "Service role can manage Telegram bot logs"
  on public.telegram_bot_logs
  for all
  using (auth.role() = 'service_role')
  with check (auth.role() = 'service_role');

drop policy if exists "Service role can manage API usage" on public.api_usage;
create policy "Service role can manage API usage"
  on public.api_usage
  for all
  using (auth.role() = 'service_role')
  with check (auth.role() = 'service_role');
