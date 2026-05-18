update public.ai_models
set is_active = false;

update public.ai_models
set is_active = true
where id in (
  'meta/llama-3.1-70b-instruct',
  'meta/llama-3.1-8b-instruct',
  'mistralai/mixtral-8x7b-instruct-v0.1'
);

update public.app_settings
set
  default_model_id = 'meta/llama-3.1-8b-instruct',
  updated_at = timezone('utc'::text, now())
where id = 'global';
