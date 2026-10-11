package com.example.util

import com.example.ui.MessageInsert
import com.example.ui.MessageInsertWithId
import com.example.ui.MessageRow
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.CancellationException

/**
 * إدخال رسالة بشكل "آمن من التكرار".
 *
 * التطبيق يولّد معرّف الرسالة (UUID) بنفسه ويرسله مع الإدخال. فإذا أُعيد الإرسال
 * (من الشاشة أو من عامل الإرسال المؤجّل) والرسالة قد وصلت أصلًا، يرفض الخادم
 * التكرار فنقرأ الرسالة الموجودة ونعدّها نجاحًا، بدل أن تظهر عند الطرف الآخر مرتين.
 */
object MessageSender {

    // إن تبيّن أن جدول الرسائل لا يقبل معرّفًا من التطبيق نعود للسلوك القديم تلقائيًا
    @Volatile
    private var clientIdsSupported = true

    suspend fun insert(supabase: SupabaseClient, data: MessageInsert, clientId: String): MessageRow {
        if (clientIdsSupported) {
            try {
                return supabase.postgrest["messages"]
                    .insert(data.withId(clientId)) { select() }
                    .decodeSingle<MessageRow>()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (isDuplicate(e)) {
                    // الرسالة وصلت من محاولة سابقة: نقرؤها ونعدّها نجاحًا
                    val existing = supabase.postgrest["messages"]
                        .select { filter { eq("id", clientId) } }
                        .decodeSingleOrNull<MessageRow>()
                    if (existing != null) return existing
                    throw e
                }
                // خطأ شبكة: نرمي الخطأ ليعيد المحاولة لاحقًا بنفس المعرّف
                if (NetworkUtils.isNetworkError(null, e)) throw e
                // خطأ آخر: ربما العمود لا يقبل معرّفًا من التطبيق، نجرّب بدونه
                val row = supabase.postgrest["messages"]
                    .insert(data) { select() }
                    .decodeSingle<MessageRow>()
                clientIdsSupported = false
                return row
            }
        }
        return supabase.postgrest["messages"]
            .insert(data) { select() }
            .decodeSingle<MessageRow>()
    }

    private fun isDuplicate(e: Throwable): Boolean {
        val m = (e.message ?: "").lowercase()
        return "23505" in m || "duplicate key" in m || "already exists" in m
    }

    private fun MessageInsert.withId(id: String) = MessageInsertWithId(
        id = id,
        chat_id = chat_id,
        sender_id = sender_id,
        content = content,
        message_type = message_type,
        media_url = media_url,
        thumbnail_url = thumbnail_url,
        reply_to_id = reply_to_id,
        media_aspect_ratio = media_aspect_ratio,
        media_group_id = media_group_id
    )
}
