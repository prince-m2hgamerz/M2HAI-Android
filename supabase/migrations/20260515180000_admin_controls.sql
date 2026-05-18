alter table public.ai_models
  add column if not exists is_active boolean default true;

create table if not exists public.app_settings (
  id text primary key default 'global',
  app_enabled boolean default true not null,
  maintenance_message text default 'M2HAI is temporarily unavailable. Please try again later.' not null,
  default_model_id text default 'meta/llama-3.1-8b-instruct' not null,
  system_prompt text default '' not null,
  temperature numeric default 0.5 not null,
  max_tokens integer default 1024 not null,
  updated_at timestamp with time zone default timezone('utc'::text, now()) not null
);

insert into public.app_settings (
  id,
  app_enabled,
  maintenance_message,
  default_model_id,
  system_prompt,
  temperature,
  max_tokens
) values (
  'global',
  true,
  'M2HAI is temporarily unavailable. Please try again later.',
  'meta/llama-3.1-8b-instruct',
  '',
  0.5,
  1024
) on conflict (id) do nothing;

drop policy if exists "Anyone can view app settings" on public.app_settings;
alter table public.app_settings enable row level security;
create policy "Anyone can view app settings"
  on public.app_settings
  for select
  using (true);

insert into public.ai_models (id, name, provider, description, is_free, is_active)
values
  ('gemini-2.5-flash', 'Gemini 2.5 Flash', 'Google', 'Fast Gemini chat model', true, false),
  ('gemini-2.5-flash-lite', 'Gemini 2.5 Flash-Lite', 'Google', 'Cost-efficient Gemini chat model', true, false),
  ('gemini-2.5-pro', 'Gemini 2.5 Pro', 'Google', 'Higher-capability Gemini chat model', true, false),
  ('gemini-2.0-flash', 'Gemini 2.0 Flash', 'Google', 'Previous-generation Gemini chat model', true, false),
  ('meta/llama-3.1-70b-instruct', 'Llama 3.1 70B', 'NVIDIA', 'Balanced high performance model', true, true),
  ('meta/llama-3.1-8b-instruct', 'Llama 3.1 8B', 'NVIDIA', 'Fast and efficient for simple tasks', true, true),
  ('mistralai/mixtral-8x7b-instruct-v0.1', 'Mixtral 8x7B', 'NVIDIA', 'Mistral model via NVIDIA NIM', true, true)
on conflict (id) do update set
  name = excluded.name,
  provider = excluded.provider,
  description = excluded.description,
  is_free = excluded.is_free,
  is_active = excluded.is_active;
