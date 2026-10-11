package com.example.ui

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.ui.i18n.Translation

/**
 * إشعار موحّد من الأسفل لكل أزرار التفعيل/الإلغاء عبر شاشات الإعدادات
 * (الخصوصية، الأمان، الإشعارات، الطاقة، التخزين).
 * يعرض: "<اسم الميزة>: تم التفعيل/تم الإلغاء" مترجم حسب لغة التطبيق الحالية،
 * بنفس أيقونة الميزة نفسها لي جنب السطر (ماشي أيقونة عامة)، متحركة.
 * الشكل والحركة معرّفين فـ OwlinoToast.kt، بنفس ستايل توست النسخ/التثبيت فالمحادثة.
 */
fun showToggleToast(context: Context, label: String, checked: Boolean, t: Translation, icon: ImageVector? = null) {
    val status = if (checked) t.toggleEnabled else t.toggleDisabled
    showOwlinoToast("$label: $status", icon, Color(0xFF4B5563))
}

/**
 * إشعار خاص بشركة Owlino يُستعمل فقط فمفاتيح الترجمة (شاشة اللغة):
 * "Show translate button" و "Translate entire chat".
 */
fun showOwlinoToggleToast(context: Context, checked: Boolean, t: Translation, icon: ImageVector? = null) {
    val message = if (checked) t.owlinoWorkingNotice else t.owlinoCancelledNotice
    showOwlinoToast(message, icon, Color(0xFF4B5563))
}
