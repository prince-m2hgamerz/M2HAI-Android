update public.ai_models
set is_active = false;

insert into public.ai_models (id, name, provider, description, is_free, is_active)
values
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

update public.app_settings
set
  default_model_id = 'meta/llama-3.1-8b-instruct',
  updated_at = timezone('utc'::text, now())
where id = 'global';
