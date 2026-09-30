package com.example
import io.github.jan.supabase.auth.auth

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.core.app.RemoteInput
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import io.github.jan.supabase.postgrest.postgrest
import java.util.UUID

class NotificationActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        
        val chatId = intent.getStringExtra("chatId")
        if (chatId == null) {
            return
        }
        
        val messageId = intent.getStringExtra("messageId") ?: ""
        
        val senderId = intent.getStringExtra("senderId") ?: supabase.auth.currentUserOrNull()?.id
        if (senderId == null) {
            return
        }

        when (action) {
            "ACTION_MARK_AS_READ" -> {
                GlobalScope.launch(Dispatchers.IO) {
                    try {
                        if (messageId.isNotEmpty()) {
                            supabase.postgrest["message_reads"].insert(
                                mapOf(
                                    "message_id" to messageId,
                                    "user_id" to senderId
                                )
                            )
                        }
                    } catch (e: Exception) {
                    }
                }
                NotificationManagerCompat.from(context).cancel(chatId.hashCode())
            }
            "ACTION_MUTE" -> {
                GlobalScope.launch(Dispatchers.IO) {
                    try {
                        supabase.postgrest["muted_chats"].insert(
                            mapOf(
                                "chat_id" to chatId,
                                "user_id" to senderId
                            )
                        )
                    } catch (e: Exception) {
                    }
                }
                NotificationManagerCompat.from(context).cancel(chatId.hashCode())
            }
            "ACTION_REPLY" -> {
                val remoteInput = RemoteInput.getResultsFromIntent(intent)
                val replyText = remoteInput?.getCharSequence("key_text_reply")?.toString()
                if (!replyText.isNullOrBlank()) {
                    GlobalScope.launch(Dispatchers.IO) {
                        try {
                            val msg = com.example.ui.MessageRow(
                                id = UUID.randomUUID().toString(),
                                chat_id = chatId,
                                sender_id = senderId,
                                content = replyText,
                                created_at = java.time.format.DateTimeFormatter.ISO_INSTANT.format(java.time.Instant.now())
                            )
                            supabase.postgrest["messages"].insert(msg)
                            
                            // Update notification to show success
                            val successNotification = androidx.core.app.NotificationCompat.Builder(context, "owlino_high_priority_v2")
                                .setSmallIcon(android.R.drawable.ic_menu_send)
                                .setContentTitle("Sent")
                                .setContentText(replyText)
                                .setTimeoutAfter(3000)
                                .build()
                            try {
                                NotificationManagerCompat.from(context).notify(chatId.hashCode(), successNotification)
                            } catch (e: SecurityException) {
                                NotificationManagerCompat.from(context).cancel(chatId.hashCode())
                            }
                        } catch (e: Exception) {
                            NotificationManagerCompat.from(context).cancel(chatId.hashCode())
                        }
                    }
                }
            }
        }
    }
}
