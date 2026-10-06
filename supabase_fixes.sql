-- ============================================================
-- Owlino - إصلاحات Supabase الشاملة (RLS + Check Constraint + Storage)
-- نفّذ هذا الملف كاملاً مرة واحدة في Supabase SQL Editor
-- ============================================================

-- 1) تفعيل RLS وسياسات جدول chats
alter table chats enable row level security;

drop policy if exists "Users can insert their own chats" on chats;
create policy "Users can insert their own chats"
on chats
for insert
to authenticated
with check (auth.uid() = created_by);

drop policy if exists "Users can view their chats" on chats;
create policy "Users can view their chats"
on chats
for select
to authenticated
using (
  auth.uid() = created_by
  or exists (
    select 1 from chat_members
    where chat_members.chat_id = chats.id
    and chat_members.user_id = auth.uid()
  )
);

drop policy if exists "Users can update their own chats" on chats;
create policy "Users can update their own chats"
on chats
for update
to authenticated
using (auth.uid() = created_by)
with check (auth.uid() = created_by);

drop policy if exists "Users can delete their own chats" on chats;
create policy "Users can delete their own chats"
on chats
for delete
to authenticated
using (auth.uid() = created_by);

-- 2) قيد نوع القناة/المجموعة/المحادثة الفردية
alter table chats drop constraint if exists chats_type_check;
alter table chats
add constraint chats_type_check
check (type in ('direct', 'group', 'channel'));

-- 3) صلاحيات bucket الصور الرمزية (avatars)
drop policy if exists "Authenticated users can upload avatars" on storage.objects;
create policy "Authenticated users can upload avatars"
on storage.objects for insert
to authenticated
with check (bucket_id = 'avatars');

drop policy if exists "Anyone can view avatars" on storage.objects;
create policy "Anyone can view avatars"
on storage.objects for select
to public
using (bucket_id = 'avatars');

-- 4) صلاحيات bucket وسائط الرسائل (media)
drop policy if exists "Authenticated users can upload media" on storage.objects;
create policy "Authenticated users can upload media"
on storage.objects for insert
to authenticated
with check (bucket_id = 'media');

drop policy if exists "Anyone can view media" on storage.objects;
create policy "Anyone can view media"
on storage.objects for select
to public
using (bucket_id = 'media');

-- ============================================================
-- انتهى. بعد التنفيذ: أعد بناء التطبيق وجرّب إنشاء قناة، الكتابة فيها، ورفع صورة.
-- ============================================================
