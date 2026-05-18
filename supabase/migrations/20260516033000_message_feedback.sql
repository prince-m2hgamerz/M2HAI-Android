create table if not exists public.message_feedback (
  id uuid default uuid_generate_v4() primary key,
  message_id uuid references public.messages(id) on delete cascade not null,
  chat_id uuid references public.chats(id) on delete cascade not null,
  user_id uuid references public.profiles(id) on delete cascade not null,
  action text check (action in ('copy', 'like', 'unlike', 'share')) not null,
  model_id text,
  created_at timestamp with time zone default timezone('utc'::text, now()) not null
);

alter table public.message_feedback enable row level security;

do $$
begin
  if not exists (select 1 from pg_policies where schemaname = 'public' and tablename = 'message_feedback' and policyname = 'Users can insert feedback for their chat messages') then
    create policy "Users can insert feedback for their chat messages" on public.message_feedback for insert
    with check (
      auth.uid() = user_id
      and exists (select 1 from public.chats where id = message_feedback.chat_id and user_id = auth.uid())
    );
  end if;

  if not exists (select 1 from pg_policies where schemaname = 'public' and tablename = 'message_feedback' and policyname = 'Users can view their own message feedback') then
    create policy "Users can view their own message feedback" on public.message_feedback for select
    using (auth.uid() = user_id);
  end if;
end $$;
