package com.example.ui

import androidx.compose.runtime.mutableStateMapOf
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.realtime.broadcastFlow
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

/**
 * مستمع "الكتابة" على مستوى التطبيق كله (مش مربوط بدورة حياة شاشة معينة).
 * كان الاستماع في الشاشة الرئيسية يتوقف كلما دخلنا/خرجنا من المحادثة، فتضيع حالة "typing".
 * هنا نشترك مرة وحدة في قناة global_typing ونحتفظ بالحالة في map تقراها الشاشة الرئيسية مباشرة.
 * وإذا ما وصلتش رسالة "توقف" (false) نطفّيو المؤشر تلقائياً بعد 6 ثواني.
 */
object TypingHub {
    val map = mutableStateMapOf<String, Boolean>()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val timers = HashMap<String, Job>()
    private var job: Job? = null

    fun start() {
        if (job?.isActive == true) return
        job = scope.launch {
            try {
                val myId = com.example.supabase.auth.currentUserOrNull()?.id
                val channel = com.example.supabase.channel("global_typing")
                channel.broadcastFlow<TypingEvent>("typing")
                    .onEach { ev ->
                        if (ev.user_id == null || ev.user_id != myId) {
                            update(ev.chat_id, ev.is_typing)
                        }
                    }
                    .launchIn(this)
                com.example.supabase.realtime.connect()
                channel.subscribe()
                awaitCancellation()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                e.printStackTrace()
                job = null
            }
        }
    }

    @Synchronized
    private fun update(chatId: String, typing: Boolean) {
        timers.remove(chatId)?.cancel()
        map[chatId] = typing
        if (typing) {
            timers[chatId] = scope.launch {
                delay(6000)
                map[chatId] = false
            }
        }
    }
}
