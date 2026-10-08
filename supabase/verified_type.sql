-- =====================================================================
-- verified_type.sql  —  التوثيق بنوعين: أزرق (أشخاص) وأحمر (شركات)
-- =====================================================================
-- ماذا يفعل هذا الملف (بكلام بسيط):
--   1) يضيف عمودًا جديدًا في جدول المستخدمين اسمه verified_type
--      قيمه المسموحة فقط: none (بلا توثيق) / blue (أزرق) / red (أحمر).
--   2) كل من كان موثّقًا سابقًا يصبح أزرق تلقائيًا.
--   3) يضع حماية: لا يستطيع أي مستخدم تغيير توثيقه (ولا توثيق غيره)
--      من التطبيق. التغيير يتم فقط من لوحة Supabase (SQL Editor)
--      أو من الخادم بمفتاح service_role.
--   4) يبقي العمود القديم is_verified متوافقًا تلقائيًا مع الجديد.
--
-- الاستعمال بعد التنفيذ (من SQL Editor):
--   منح أزرق :  update public.profiles set verified_type = 'blue' where username = 'اسم_المستخدم';
--   منح أحمر :  update public.profiles set verified_type = 'red'  where username = 'اسم_الشركة';
--   سحب التوثيق: update public.profiles set verified_type = 'none' where username = 'اسم_المستخدم';
--
-- يمكن تنفيذ الملف أكثر من مرة دون ضرر (آمن للتكرار).
-- =====================================================================

-- 1) العمود الجديد
alter table public.profiles
  add column if not exists verified_type text not null default 'none';

-- قيد القيم المسموحة
do $$
begin
  if not exists (
    select 1 from pg_constraint
    where conname = 'profiles_verified_type_check'
      and conrelid = 'public.profiles'::regclass
  ) then
    alter table public.profiles
      add constraint profiles_verified_type_check
      check (verified_type in ('none', 'blue', 'red'));
  end if;
end $$;

-- 2) نقل البيانات القديمة: الموثّق سابقًا = أزرق
update public.profiles
set verified_type = 'blue'
where coalesce(is_verified, false) = true
  and verified_type = 'none';

-- 3) الحماية + المزامنة بين العمودين
create or replace function public.protect_verified_columns()
returns trigger
language plpgsql
as $$
declare
  is_privileged boolean;
begin
  -- "مصرّح" = أي دور غير anon/authenticated، أي:
  --   * SQL Editor في لوحة Supabase (الدور postgres)
  --   * الخادم بمفتاح service_role
  --   * عمليات النظام الداخلية (مثل إنشاء الحساب عند التسجيل)
  -- أي طلب قادم من التطبيق (مسجّل أو زائر) يعمل بدور anon أو authenticated
  -- فلا يُعدّ مصرّحًا مهما كان محتواه.
  is_privileged := current_user not in ('anon', 'authenticated');

  if tg_op = 'INSERT' then
    if not is_privileged then
      new.verified_type := 'none';
      new.is_verified := false;
    else
      -- مزامنة أولية
      if coalesce(new.verified_type, 'none') = 'none' and coalesce(new.is_verified, false) then
        new.verified_type := 'blue';
      end if;
      new.is_verified := (coalesce(new.verified_type, 'none') <> 'none');
    end if;
    return new;
  end if;

  -- UPDATE
  if not is_privileged then
    -- لا نرفع خطأ (حتى لا ينكسر تعديل الاسم/الصورة/الخصوصية العادي)،
    -- بل نتجاهل أي محاولة لتغيير حقول التوثيق ونُبقي القيم القديمة.
    new.verified_type := old.verified_type;
    new.is_verified := old.is_verified;
    return new;
  end if;

  -- تعديل مصرّح: نُبقي العمودين متطابقين
  if new.verified_type is distinct from old.verified_type then
    new.is_verified := (new.verified_type <> 'none');
  elsif new.is_verified is distinct from old.is_verified then
    new.verified_type := case
      when coalesce(new.is_verified, false) then
        case when old.verified_type = 'none' then 'blue' else old.verified_type end
      else 'none'
    end;
  end if;
  return new;
end;
$$;

drop trigger if exists trg_protect_verified_columns on public.profiles;
create trigger trg_protect_verified_columns
before insert or update on public.profiles
for each row execute function public.protect_verified_columns();

-- 4) اختبار سريع بعد التنفيذ (اختياري):
--   select id, username, is_verified, verified_type from public.profiles
--   where verified_type <> 'none';
