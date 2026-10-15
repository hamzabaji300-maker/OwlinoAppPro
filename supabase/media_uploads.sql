-- =====================================================================
-- media_uploads.sql — حصة الرفع اليومية + تحديد نوع المحادثة من الخادم
-- =====================================================================
-- ماذا يفعل هذا الملف (بكلام بسيط):
--   1) ينشئ جدولًا يسجّل كل عملية رفع (من، أين، الحجم، متى).
--   2) ينشئ دالة تسمّى reserve_media_upload يسألها الـ Worker قبل كل رفع، وهي تقرر:
--        - هل المستخدم مسجّل دخوله؟ وهل هو عضو في المحادثة؟
--        - في القنوات: المشرفون فقط يرفعون (المشتركون لا).
--        - هل المحادثة فردية أم مجموعة/قناة؟  ← يحدده الخادم، فلا يستطيع التطبيق التلاعب
--          (فردية = تُحذف الملفات بعد 30 يومًا، مجموعة/قناة = 180 يومًا).
--        - هل تجاوز المستخدم حصته؟ (حد يومي بالحجم والعدد + حد في الدقيقة لمنع الإغراق)
--   3) لا يستطيع أحد قراءة الجدول أو تعديله من التطبيق، فقط الدالة.
--   4) ينشئ الدالة can_read_media التي تحمي التحميل: لا يفتح الملف إلا عضو أو قارئ قناة عامة.
--
-- لتغيير الحصة لاحقًا (مثلًا لمشتركي Pro) عدّل الدالة media_daily_limit_bytes فقط.
-- آمن للتكرار: يمكن تنفيذه أكثر من مرة.
-- شغّله في Supabase > SQL Editor > Run.
-- =====================================================================

create table if not exists public.media_uploads (
  id          uuid primary key default gen_random_uuid(),
  user_id     uuid not null,
  chat_id     text not null,
  kind        text not null,
  scope       text not null,
  size_bytes  bigint not null,
  created_at  timestamptz not null default now()
);

create index if not exists media_uploads_user_time_idx
  on public.media_uploads (user_id, created_at desc);

alter table public.media_uploads enable row level security;
-- لا توجد سياسات عمدًا: لا أحد يصل للجدول مباشرة، فقط الدالة أدناه (security definer)
revoke all on public.media_uploads from anon, authenticated;

-- الحصة اليومية بالبايت لكل مستخدم (الخطة المجانية الآن: 200 ميغابايت).
-- عند إضافة الاشتراكات: اقرأ خطة المستخدم هنا وأرجع قيمة أكبر لمشتركي Pro.
create or replace function public.media_daily_limit_bytes(p_user uuid)
returns bigint
language sql
stable
as $$
  select (200 * 1024 * 1024)::bigint;
$$;

create or replace function public.reserve_media_upload(p_chat text, p_kind text, p_size bigint)
returns jsonb
language plpgsql
security definer
set search_path = public
as $$
declare
  uid           uuid := auth.uid();
  v_type        text;
  v_role        text;
  v_scope       text;
  v_bytes_24h   bigint;
  v_count_24h   int;
  v_count_1m    int;
  v_limit_bytes bigint;
  v_limit_count constant int := 400;   -- أقصى عدد عمليات رفع في 24 ساعة (يشمل المصغرات)
  v_limit_burst constant int := 40;    -- أقصى عدد في الدقيقة الواحدة
begin
  if uid is null then
    return jsonb_build_object('allowed', false, 'reason', 'unauthorized');
  end if;

  if p_kind is null or p_kind not in ('image', 'thumb', 'voice', 'video', 'file')
     or p_size is null or p_size <= 0 then
    return jsonb_build_object('allowed', false, 'reason', 'bad_request');
  end if;

  -- عضوية المحادثة
  select m.role into v_role
  from public.chat_members m
  where m.chat_id::text = p_chat and m.user_id::text = uid::text
  limit 1;
  if not found then
    return jsonb_build_object('allowed', false, 'reason', 'forbidden');
  end if;

  -- نوع المحادثة من الخادم
  select c.type into v_type from public.chats c where c.id::text = p_chat;
  if v_type is null then
    return jsonb_build_object('allowed', false, 'reason', 'forbidden');
  end if;

  -- القنوات: المشرفون فقط ينشرون الوسائط
  if v_type = 'channel' and coalesce(v_role, '') <> 'admin' then
    return jsonb_build_object('allowed', false, 'reason', 'forbidden');
  end if;

  v_scope := case when v_type in ('group', 'channel') then 'group' else 'direct' end;

  -- الحصة
  select coalesce(sum(size_bytes), 0), count(*)
    into v_bytes_24h, v_count_24h
  from public.media_uploads
  where user_id = uid and created_at > now() - interval '24 hours';

  select count(*) into v_count_1m
  from public.media_uploads
  where user_id = uid and created_at > now() - interval '1 minute';

  v_limit_bytes := public.media_daily_limit_bytes(uid);

  if v_bytes_24h + p_size > v_limit_bytes
     or v_count_24h >= v_limit_count
     or v_count_1m >= v_limit_burst then
    return jsonb_build_object(
      'allowed', false, 'reason', 'quota',
      'used', v_bytes_24h, 'limit', v_limit_bytes
    );
  end if;

  insert into public.media_uploads (user_id, chat_id, kind, scope, size_bytes)
  values (uid, p_chat, p_kind, v_scope, p_size);

  return jsonb_build_object(
    'allowed', true, 'scope', v_scope,
    'used', v_bytes_24h + p_size, 'limit', v_limit_bytes
  );
end;
$$;

revoke all on function public.reserve_media_upload(text, text, bigint) from public, anon;
grant execute on function public.reserve_media_upload(text, text, bigint) to authenticated;

-- ---------------------------------------------------------------------
-- حق قراءة ملفات المحادثة: يسألها الـ Worker عند كل تحميل.
-- مسموح لـ: أعضاء المحادثة، وأي مستخدم مسجّل لقنوات (type = 'channel') لأنها عامة في تطبيقك.
-- (إن أضفت قنوات خاصة لاحقًا فقيّد الشرط الثاني بعمود يميّز القنوات العامة.)
-- النتيجة: الرابط المسرّب لا يفتح لغير الأعضاء، ومن غادر المحادثة يفقد الوصول خلال دقيقة.
-- ---------------------------------------------------------------------
create or replace function public.can_read_media(p_chat text)
returns boolean
language plpgsql
security definer
set search_path = public
stable
as $$
declare
  uid uuid := auth.uid();
begin
  if uid is null then
    return false;
  end if;
  if exists (
    select 1 from public.chat_members m
    where m.chat_id::text = p_chat and m.user_id::text = uid::text
  ) then
    return true;
  end if;
  return exists (
    select 1 from public.chats c
    where c.id::text = p_chat and c.type = 'channel'
  );
end;
$$;

revoke all on function public.can_read_media(text) from public, anon;
grant execute on function public.can_read_media(text) to authenticated;

-- تنظيف السجلات القديمة (اختياري، شغّله من حين لآخر أو اجعله مجدولًا):
--   delete from public.media_uploads where created_at < now() - interval '3 days';
