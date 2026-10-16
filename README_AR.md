# Owlino Messages Lab
تطبيق مستقل لتطوير شاشات الرسائل. أربعة أزرار سفلية: القنوات، المجموعات، البوتات، الدردشة.
- التصميم منسوخ حرفيًا من OwlinoFinal (ChatComponents / ChatDetailScreen / BotKeyboards ...).
- ChatScreenParts.kt = مقتطفات حرفية من ChatDetailScreen.kt (القائمة، الشريط العلوي، شريط الإدخال بالتسجيل الصوتي).
- LabMessagesScreen.kt = تركيب الشاشة + بيانات تجريبية محلية (بدون Supabase).
- التسجيل الصوتي: اضغط مطولًا على زر الميكروفون، اسحب لليسار للإلغاء أو للأعلى للقفل.
- المرفقات: زر + ثم صور / فيديو / صوت / ملف.
- البناء: GitHub Actions (build-apk.yml) أو gradle assembleDebug.
