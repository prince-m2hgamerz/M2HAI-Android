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

update public.app_settings
set
  update_release_notes = coalesce(update_release_notes, ''),
  update_apk_size_mb = coalesce(update_apk_size_mb, 0),
  update_sha256 = coalesce(update_sha256, ''),
  update_published_at = coalesce(update_published_at, ''),
  update_channel = coalesce(nullif(update_channel, ''), 'stable'),
  updated_at = timezone('utc'::text, now())
where id = 'global';
