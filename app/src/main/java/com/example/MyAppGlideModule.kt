package com.example

import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.module.AppGlideModule

// وجود هذا الكلاس (حتى فارغ) يجبر معالج Glide على فحص كل المكتبات
// وتفعيل فك تشفير الإيموجي المتحرك (APNG) تلقائياً في كل استعمالات Glide بالتطبيق.
@GlideModule
class MyAppGlideModule : AppGlideModule()
