-- ===== القناة الرسمية الحقيقية (Owlino) — نفّذ هذا الملف كاملاً مرة واحدة في Supabase > SQL Editor =====

-- 1) أنشئ القناة (نفس المعرّف الثابت الموجود في التطبيق) واجعل حسابك أدمن
do $$
declare
  admin_id uuid;
begin
  -- الأدمن = أقدم حساب في التطبيق (حسابك أنت). إن أردت حساباً آخر: ضع معرّفه بدل السطر:  admin_id := 'ضع-الـ-UUID-هنا';
  select id into admin_id from auth.users order by created_at asc limit 1;
  if admin_id is null then
    raise exception 'لا يوجد أي مستخدم';
  end if;
  raise notice 'الأدمن هو: %', (select email from auth.users where id = admin_id);

  insert into public.chats (id, type, created_by, title)
  values ('00000000-0000-4000-8000-0000000000a1', 'channel', admin_id, 'قناة Owlino الرسمية')
  on conflict (id) do update set type = 'channel', title = excluded.title;

  -- حسابك أدمن (يكتب في القناة)
  delete from public.chat_members
   where chat_id = '00000000-0000-4000-8000-0000000000a1' and user_id = admin_id;
  insert into public.chat_members (chat_id, user_id, role)
  values ('00000000-0000-4000-8000-0000000000a1', admin_id, 'admin');
end $$;

-- 2) اشتراك كل المستخدمين الحاليين تلقائياً (متابعون)
insert into public.chat_members (chat_id, user_id, role)
select '00000000-0000-4000-8000-0000000000a1', p.id, 'member'
from public.profiles p
where not exists (
  select 1 from public.chat_members m
  where m.chat_id = '00000000-0000-4000-8000-0000000000a1' and m.user_id = p.id
);

-- 3) كل مستخدم جديد يشترك تلقائياً عند إنشاء حسابه
create or replace function public.join_official_channel()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
begin
  insert into public.chat_members (chat_id, user_id, role)
  select '00000000-0000-4000-8000-0000000000a1', new.id, 'member'
  where not exists (
    select 1 from public.chat_members m
    where m.chat_id = '00000000-0000-4000-8000-0000000000a1' and m.user_id = new.id
  );
  return new;
end;
$$;

drop trigger if exists trg_join_official_channel on public.profiles;
create trigger trg_join_official_channel
after insert on public.profiles
for each row execute function public.join_official_channel();

-- 4) في كل القنوات: الأدمن فقط يستطيع النشر (المشتركون يقرؤون فقط)
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

-- 5) تأكد أن الجداول تبث لحظياً (إن كانت مضافة سابقاً سيظهر تنبيه فقط)
do $$ begin
  begin alter publication supabase_realtime add table public.messages; exception when others then null; end;
  begin alter publication supabase_realtime add table public.chat_members; exception when others then null; end;
  begin alter publication supabase_realtime add table public.chats; exception when others then null; end;
end $$;
