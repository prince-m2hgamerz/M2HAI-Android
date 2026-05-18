update public.app_settings
set
  default_model_id = 'meta/llama-3.1-8b-instruct',
  updated_at = timezone('utc'::text, now())
where id = 'global'
  and default_model_id in ('gemini-1.5-flash', 'gemini-2.5-flash', 'gemini-2.5-pro', 'gemini-2.0-flash');

update public.ai_models
set is_active = false
where id in (
  'gemini-flash-latest',
  'gemini-2.5-flash',
  'gemini-2.5-flash-lite',
  'gemini-2.5-pro',
  'gemini-2.0-flash'
);

update public.ai_models
set is_active = true
where id in (
  'meta/llama-3.1-70b-instruct',
  'meta/llama-3.1-8b-instruct',
  'mistralai/mixtral-8x7b-instruct-v0.1'
);
