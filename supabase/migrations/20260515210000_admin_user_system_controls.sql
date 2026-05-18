alter table public.profiles
  add column if not exists is_disabled boolean default false;

alter table public.profiles
  add column if not exists admin_notes text;

alter table public.app_settings
  add column if not exists signup_enabled boolean default true not null;

alter table public.app_settings
  add column if not exists global_announcement text default '' not null;

update public.app_settings
set
  signup_enabled = coalesce(signup_enabled, true),
  global_announcement = coalesce(global_announcement, ''),
  updated_at = timezone('utc'::text, now())
where id = 'global';
