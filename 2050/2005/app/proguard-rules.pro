# Keep Supabase/Kotlinx Serialization models
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keep,includedescriptorclasses class com.example.**$$serializer { *; }
-keepclassmembers class com.example.** {
    *** Companion;
}
-keepclasseswithmembers class com.example.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep Rive (JNI/native bindings must not be stripped or renamed by R8)
-keep class app.rive.runtime.kotlin.** { *; }
-keepclassmembers class app.rive.runtime.kotlin.** { *; }
-keepclasseswithmembernames,includedescriptorclasses class * {
    native <methods>;
}

# ===== إصلاح: java.lang.VerifyError على ChatDetailScreen (copy1 vX<-vY type=Reference: Composer) =====
# هاد الخطأ معروف: R8 كيدير register allocation غلط فالدوال الضخمة جداً لي عندها
# بزاف ديال المتغيرات المحلية (خصوصاً composables ضخمة بزاف كيفما كانت ChatDetailScreen قبل
# ما نقسموها)، وكيولّد bytecode كيرفضو verifier ديال ART بخطأ "copy1 v0<-v262".
# هاد السطر كيوقف بالضبط الـ optimization pass لي كيسبب هاد المشكل، بلا ما يأثر
# على بقية التحسينات ديال R8 (minify/shrink بقاو خدامين عاديين).
-optimizations !code/allocation/variable

# Backup: if minify is ever re-enabled, do not let R8 optimize (it produced invalid bytecode: VerifyError)
-dontoptimize
