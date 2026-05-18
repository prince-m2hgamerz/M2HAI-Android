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

insert into public.ai_models (id, name, provider, description, is_free, is_active)
values (
  'black-forest-labs/flux.1-schnell',
  'FLUX.1 Schnell',
  'NVIDIA',
  'Free NVIDIA text-to-image generation model',
  true,
  true
)
on conflict (id) do update set
  name = excluded.name,
  provider = excluded.provider,
  description = excluded.description,
  is_free = excluded.is_free,
  is_active = excluded.is_active;

update public.app_settings
set
  image_model_id = coalesce(nullif(image_model_id, ''), 'black-forest-labs/flux.1-schnell'),
  latest_version_code = coalesce(latest_version_code, 1),
  latest_version_name = coalesce(nullif(latest_version_name, ''), '1.0'),
  min_supported_version_code = coalesce(min_supported_version_code, 1),
  update_title = coalesce(nullif(update_title, ''), 'Update available'),
  update_message = coalesce(update_message, ''),
  update_apk_url = coalesce(update_apk_url, ''),
  updated_at = timezone('utc'::text, now())
where id = 'global';
