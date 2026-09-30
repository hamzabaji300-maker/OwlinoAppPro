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
