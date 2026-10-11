-- =====================================================================
-- profiles_rls_suggestion.sql  —  اقتراح فقط، لا تنفّذه قبل قراءة التحذيرات
-- =====================================================================
-- الوضع: لم نجد في ملفات المشروع أي سياسات (RLS) لجدول profiles.
-- قد تكون موجودة أصلًا في لوحة Supabase دون ملف. تحقّق أولًا من:
--   Supabase > Authentication > Policies > profiles
--   أو:  select * from pg_policies where tablename = 'profiles';
--
-- الفكرة المقترحة:
--   * القراءة: لأي مستخدم مسجّل دخوله فقط (لا للزوار غير المسجلين).
--   * التعديل: المستخدم يعدّل صفه هو فقط.
--   * الإضافة والحذف: من النظام فقط (لا من التطبيق).
--   * حقول التوثيق محمية بملف verified_type.sql (trigger) بغض النظر عن السياسات.
--
-- ⚠️ مخاطر يجب أن تعرفها قبل التنفيذ:
--   1) إذا فعّلت RLS على جدول لا سياسات له، يتوقف كل شيء فجأة
--      (التطبيق يصبح فارغًا ولا أحد يستطيع تسجيل الدخول أو رؤية الملفات).
--      لذلك فعّل RLS وأضف السياسات في عملية واحدة، وجرّب فورًا.
--   2) إذا كان إنشاء الملف الشخصي عند التسجيل يتم من التطبيق (insert)،
--      فإن منع الإضافة سيعطّل التسجيل. (في الكود الحالي لم أجد insert من التطبيق،
--      لكن تحقق بنفسك بإنشاء حساب تجريبي.)
--   3) سياسة القراءة المفتوحة للمسجّلين تعني أن أي مستخدم يستطيع قراءة كل أعمدة
--      الجميع، ومنها fcm_token وcontact_email وcontact_phone وwallet_address.
--      الحل الصحيح لاحقًا: نقل الأعمدة الحساسة إلى جدول خاص. اتركه لمرحلة الحماية.
--   4) جرّب أولًا على مشروع Supabase تجريبي أو بنسخة احتياطية.
-- =====================================================================

-- ابدأ بالتحقق مما هو موجود:
-- select policyname, cmd, qual from pg_policies where tablename = 'profiles';

alter table public.profiles enable row level security;

drop policy if exists "profiles_select_authenticated" on public.profiles;
create policy "profiles_select_authenticated"
  on public.profiles for select
  to authenticated
  using (true);

drop policy if exists "profiles_update_own" on public.profiles;
create policy "profiles_update_own"
  on public.profiles for update
  to authenticated
  using (auth.uid() = id)
  with check (auth.uid() = id);

-- لا توجد سياسة insert/delete عمدًا: الإنشاء من trigger التسجيل (security definer)
-- والحذف من الخادم فقط.
