package com.example.util

import com.example.ui.AttachmentType

/**
 * حدود الرفع. كلها الآن للخطة المجانية.
 * لاحقًا عند إضافة Pro نمرّر الخطة هنا فقط، ولا نلمس بقية الكود.
 */
object UploadLimits {

    enum class Plan { FREE }

    private const val MB = 1024L * 1024L

    /** أقصى حجم (بالبايت) للملف الأصلي قبل أي معالجة، أو null إن لم يكن هناك حد. */
    fun maxBytes(type: AttachmentType, plan: Plan = Plan.FREE): Long? = when (type) {
        AttachmentType.VIDEO -> 50 * MB
        AttachmentType.DOCUMENT -> 25 * MB
        AttachmentType.AUDIO, AttachmentType.VOICE -> 10 * MB
        // الصور تُضغط قبل الرفع، لكن نضع حدًا للملف الأصلي حتى لا نحاول فك صورة ضخمة جدًا
        AttachmentType.IMAGE -> 40 * MB
    }

    fun limitMessage(type: AttachmentType, plan: Plan = Plan.FREE): String? = when (type) {
        AttachmentType.VIDEO -> "عذراً، حجم الفيديو يتجاوز الحد المسموح (50 ميغابايت)."
        AttachmentType.DOCUMENT -> "عذراً، حجم الملف يتجاوز الحد المسموح (25 ميغابايت)."
        AttachmentType.AUDIO, AttachmentType.VOICE -> "عذراً، حجم المقطع الصوتي يتجاوز الحد المسموح (10 ميغابايت)."
        AttachmentType.IMAGE -> "عذراً، حجم الصورة كبير جداً."
    }

    /** يرجع رسالة الخطأ إن تجاوز الحجم الحد، وإلا null. */
    fun check(type: AttachmentType, sizeBytes: Long, plan: Plan = Plan.FREE): String? {
        val max = maxBytes(type, plan) ?: return null
        return if (sizeBytes > max) limitMessage(type, plan) else null
    }
}

/** استثناء يحمل رسالة عربية جاهزة للعرض للمستخدم. */
class UploadLimitException(message: String) : Exception(message)
