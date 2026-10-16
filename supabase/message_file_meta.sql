-- اسم الملف ونوعه وحجمه يصلون إلى المستقبل (قبل هذا كان يرى الملف باسم عشوائي وبلا نوع).
-- شغّله مرة واحدة في Supabase: SQL Editor ثم Run. آمن للتكرار.
alter table public.messages
  add column if not exists media_file_name text,
  add column if not exists media_file_size bigint,
  add column if not exists media_mime_type text;
