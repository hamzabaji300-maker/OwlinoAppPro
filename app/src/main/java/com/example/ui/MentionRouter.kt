package com.example.ui

import android.content.Context
import android.widget.Toast
import com.example.bot.BotManager
import com.example.bot.BotManagerStrings
import com.example.supabase
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * الضغط على @username الأزرق: يبحث عن بوت بهذا الاسم، وإذا لم يجده يبحث عن مستخدم،
 * ثم يفتح المحادثة مباشرة (ينشئها للمستخدم إن لم تكن موجودة).
 */
object MentionRouter {
    private fun str(o: JsonObject, k: String): String? = (o[k] as? JsonPrimitive)?.content?.takeIf { it != "null" }

    suspend fun open(context: Context, mention: String, navigate: (chatId: String, name: String) -> Unit) {
        val username = mention.trim().removePrefix("@")
        if (username.isEmpty()) return
        try {
            // 1) بوت؟
            val bots = withContext(Dispatchers.IO) {
                supabase.postgrest.rpc("search_bots", buildJsonObject { put("q", username) }).decodeList<JsonObject>()
            }
            val bot = bots.firstOrNull { str(it, "username").equals(username, ignoreCase = true) }
            if (bot != null) {
                val id = str(bot, "id") ?: return
                val name = str(bot, "name") ?: username
                if (id == BotManager.OFFICIAL_ID) {
                    BotManager.ensureChat(context)
                    navigate(BotManager.CHAT_ID, name)
                } else {
                    BotManager.ensureBotChat(context, id, name)
                    navigate(BotManager.botChatId(id), name)
                }
                return
            }

            // 2) مستخدم؟
            val myId = supabase.auth.currentUserOrNull()?.id
            val users = withContext(Dispatchers.IO) {
                supabase.postgrest["profiles"].select(Columns.list("id", "username", "full_name")) {
                    filter { eq("username", username) }
                    limit(1)
                }.decodeList<JsonObject>()
            }.ifEmpty {
                withContext(Dispatchers.IO) {
                    supabase.postgrest["profiles"].select(Columns.list("id", "username", "full_name")) {
                        filter { eq("username", username.lowercase()) }
                        limit(1)
                    }.decodeList<JsonObject>()
                }
            }
            val user = users.firstOrNull()
            val userId = user?.let { str(it, "id") }
            if (user == null || userId == null || myId == null) {
                Toast.makeText(context, BotManagerStrings.NOT_FOUND, Toast.LENGTH_SHORT).show()
                return
            }
            if (userId == myId) return
            val realName = str(user, "full_name") ?: username
            val chatId = withContext(Dispatchers.IO) { findOrCreateDirectChat(myId, userId) }
            if (chatId != null) navigate(chatId, realName)
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, BotManagerStrings.GENERIC_ERROR, Toast.LENGTH_SHORT).show()
        }
    }

    private suspend fun findOrCreateDirectChat(myId: String, userId: String): String? {
        val myChats = supabase.postgrest["chat_members"].select(Columns.list("chat_id")) {
            filter { eq("user_id", myId) }
        }.decodeList<ChatMemberRow>().map { it.chat_id }
        if (myChats.isNotEmpty()) {
            val common = supabase.postgrest["chat_members"].select(Columns.list("chat_id")) {
                filter {
                    eq("user_id", userId)
                    isIn("chat_id", myChats)
                }
            }.decodeList<ChatMemberRow>().map { it.chat_id }
            if (common.isNotEmpty()) {
                val direct = supabase.postgrest["chats"].select(Columns.list("id")) {
                    filter {
                        isIn("id", common)
                        eq("type", "direct")
                    }
                }.decodeList<ChatRow>()
                if (direct.isNotEmpty()) return direct.first().id
            }
        }
        val newChat = supabase.postgrest["chats"].insert(ChatInsert(type = "direct", created_by = myId)) {
            select()
        }.decodeSingle<ChatRow>()
        supabase.postgrest["chat_members"].insert(listOf(
            ChatMemberRow(chat_id = newChat.id, user_id = myId, role = "admin"),
            ChatMemberRow(chat_id = newChat.id, user_id = userId, role = "member")
        ))
        return newChat.id
    }
}
