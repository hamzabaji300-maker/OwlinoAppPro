-- ===== نظام القنوات العامة (مثل تيليجرام): أي مستخدم ينشئ قناة، والبقية يبحثون وينضمون ويتابعون =====
-- نفّذه كاملاً مرة واحدة في Supabase > SQL Editor

-- 1) أعمدة إضافية للقناة
alter table public.chats add column if not exists username text;
alter table public.chats add column if not exists description text;
alter table public.chats add column if not exists is_public boolean not null default true;
create unique index if not exists chats_username_unique on public.chats (lower(username)) where username is not null;

-- 2) البحث عن القنوات العامة (يعمل بدون كشف كل الجداول)
create or replace function public.search_channels(q text)
returns table (id uuid, title text, avatar_url text, username text, subscribers bigint, is_member boolean)
language sql
stable
security definer
set search_path = public
as $$
  select c.id, c.title, c.avatar_url, c.username,
         (select count(*) from public.chat_members m where m.chat_id = c.id) as subscribers,
         exists (select 1 from public.chat_members m where m.chat_id = c.id and m.user_id = auth.uid()) as is_member
  from public.chats c
  where c.type = 'channel' and c.is_public
    and (coalesce(q, '') = '' or c.title ilike '%' || q || '%' or c.username ilike '%' || q || '%')
  order by (select count(*) from public.chat_members m where m.chat_id = c.id) desc
  limit 30;
$$;
grant execute on function public.search_channels(text) to authenticated;

-- 3) الانضمام للقناة ومغادرتها
create or replace function public.join_channel(p_chat_id uuid)
returns void
language plpgsql
security definer
set search_path = public
as $$
begin
  if auth.uid() is null then raise exception 'not authenticated'; end if;
  if not exists (select 1 from public.chats c where c.id = p_chat_id and c.type = 'channel' and c.is_public) then
    raise exception 'channel not found';
  end if;
  insert into public.chat_members (chat_id, user_id, role)
  select p_chat_id, auth.uid(), 'member'
  where not exists (select 1 from public.chat_members m where m.chat_id = p_chat_id and m.user_id = auth.uid());
end;
$$;
grant execute on function public.join_channel(uuid) to authenticated;

create or replace function public.leave_channel(p_chat_id uuid)
returns void
language sql
security definer
set search_path = public
as $$
  delete from public.chat_members
  where chat_id = p_chat_id and user_id = auth.uid() and role <> 'admin';
$$;
grant execute on function public.leave_channel(uuid) to authenticated;

-- 4) في كل القنوات: النشر للأدمن فقط (المشتركون يقرؤون ويتفاعلون)
drop policy if exists channel_admin_only_post on public.messages;
create policy channel_admin_only_post on public.messages
as restrictive
for insert
with check (
  not exists (select 1 from public.chats c where c.id = messages.chat_id and c.type = 'channel')
  or exists (
    select 1 from public.chat_members m
    where m.chat_id = messages.chat_id and m.user_id = auth.uid() and m.role = 'admin'
  )
);
