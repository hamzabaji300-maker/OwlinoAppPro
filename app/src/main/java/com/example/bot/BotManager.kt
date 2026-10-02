package com.example.bot

import android.content.Context
import android.net.Uri
import com.example.supabase
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.storage.storage
import kotlinx.serialization.Serializable
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.util.UUID

/**
 * البوت المدير (BotFather الخاص بـ Owlino).
 * محادثة محلية كاملة: لا تمر برسائل Supabase العادية. الدردشة هي واجهة الإدارة.
 */
object BotManager {
    const val CHAT_ID = "botmanager"
    /** معرّف البوت الرسمي في جدول bots (نفس القيمة في ملف SQL) */
    const val OFFICIAL_ID = "00000000-0000-0000-0000-0000000000b0"
    const val NAME = "BotManager"

    @Serializable
    data class Command(val command: String, val description: String)

    /** أوامر البوت المدير نفسه (مدمجة، مثل BotFather). باقي البوتات تجلب أوامرها من قاعدة البيانات. */
    val defaultCommands = listOf(
        Command("/start", "Show the welcome message"),
        Command("/newbot", "Create a new bot"),
        Command("/mybots", "List your bots"),
        Command("/revoke", "Generate a new token for a bot"),
        Command("/setcommands", "Change the commands list of a bot"),
        Command("/cancel", "Cancel the current operation")
    )

    /** أزرار مضمّنة تظهر تحت رسالة /start (callback_data = الأمر نفسه) */
    const val START_MARKUP = "{\"inline_keyboard\":[[{\"text\":\"New bot\",\"callback_data\":\"/newbot\"},{\"text\":\"My bots\",\"callback_data\":\"/mybots\"}],[{\"text\":\"Revoke token\",\"callback_data\":\"/revoke\"},{\"text\":\"Set commands\",\"callback_data\":\"/setcommands\"}]]}"

    private enum class Step { IDLE, WAITING_NAME, WAITING_AVATAR, WAITING_REVOKE_PICK, WAITING_COMMANDS_BOT, WAITING_COMMANDS_TEXT }

    @Serializable
    private data class BotRow(val id: String, val name: String)

    private var step = Step.IDLE
    private var pendingName = ""
    private var revokeChoices: List<BotRow> = emptyList()
    private var commandsTarget: BotRow? = null

    // ---------- حالة "Start" لكل بوت ----------
    private const val PREFS = "bot_prefs"

    fun isStarted(context: Context, chatId: String): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean("started_$chatId", false)

    fun markStarted(context: Context, chatId: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean("started_$chatId", true).apply()
    }

    /** يضمن وجود محادثة البوت المدير في قائمة المحادثات. */
    suspend fun ensureChat(context: Context) {
        try {
            val dao = com.example.data.DatabaseProvider.getDatabase(context).chatDao()
            if (dao.getChatById(CHAT_ID) == null) {
                dao.insert(
                    com.example.data.ChatEntity(
                        id = CHAT_ID,
                        name = NAME,
                        message = "Tap START to begin",
                        time = "Now",
                        timestamp = System.currentTimeMillis(),
                        isBot = true,
                        isVerified = true
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // ---------- State machine ----------

    suspend fun onText(raw: String): List<String> {
        val text = raw.trim()
        if (text.startsWith("/")) {
            return when (text.substringBefore(' ').substringBefore('@').lowercase()) {
                "/start" -> { reset(); listOf(BotManagerStrings.WELCOME) }
                "/newbot" -> { reset(); step = Step.WAITING_NAME; listOf(BotManagerStrings.ASK_NAME) }
                "/cancel" -> { reset(); listOf(BotManagerStrings.CANCELLED) }
                "/mybots" -> { reset(); listBots() }
                "/revoke" -> { reset(); startRevoke() }
                "/setcommands" -> { reset(); startSetCommands() }
                else -> listOf(BotManagerStrings.UNKNOWN_COMMAND)
            }
        }
        return when (step) {
            Step.WAITING_NAME -> when {
                text.isEmpty() -> listOf(BotManagerStrings.EMPTY_NAME)
                text.length > 64 -> listOf(BotManagerStrings.NAME_TOO_LONG)
                else -> { pendingName = text; step = Step.WAITING_AVATAR; listOf(BotManagerStrings.ASK_AVATAR) }
            }
            Step.WAITING_AVATAR -> listOf(BotManagerStrings.NOT_AN_IMAGE)
            Step.WAITING_REVOKE_PICK -> pickRevoke(text)
            Step.WAITING_COMMANDS_BOT -> pickCommandsBot(text)
            Step.WAITING_COMMANDS_TEXT -> saveCommands(text)
            Step.IDLE -> listOf(BotManagerStrings.UNKNOWN_COMMAND)
        }
    }

    /** الصورة جاية من زر المرفقات الحالي. uri=null أو ليست صورة => يمرّر isImage=false */
    suspend fun onImage(context: Context, uri: Uri?, isImage: Boolean): List<String> {
        if (step != Step.WAITING_AVATAR) return listOf(BotManagerStrings.SEND_NEWBOT_FIRST)
        if (uri == null || !isImage) return listOf(BotManagerStrings.NOT_AN_IMAGE)

        val userId = supabase.auth.currentSessionOrNull()?.user?.id
            ?: return listOf(BotManagerStrings.NOT_LOGGED_IN)

        val avatarUrl = try {
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: return listOf(BotManagerStrings.UPLOAD_FAILED)
            val path = "$userId/${UUID.randomUUID()}.jpg"
            supabase.storage["bot-avatars"].upload(path, bytes)
            supabase.storage["bot-avatars"].publicUrl(path)
        } catch (e: Exception) {
            e.printStackTrace()
            return listOf(BotManagerStrings.UPLOAD_FAILED)
        }

        return try {
            val result = supabase.postgrest.rpc(
                "create_bot",
                buildJsonObject {
                    put("p_name", pendingName)
                    put("p_avatar_url", avatarUrl)
                }
            )
            val token = result.decodeAs<String>()
            val name = pendingName
            reset()
            listOf(BotManagerStrings.success(name, token))
        } catch (e: Exception) {
            e.printStackTrace()
            listOf(BotManagerStrings.GENERIC_ERROR)
        }
    }

    // ---------- helpers ----------

    private fun reset() {
        step = Step.IDLE
        pendingName = ""
        revokeChoices = emptyList()
        commandsTarget = null
    }

    private suspend fun fetchBots(): List<BotRow>? = try {
        supabase.postgrest["bots"].select().decodeList<BotRow>()
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }

    private suspend fun listBots(): List<String> {
        val bots = fetchBots() ?: return listOf(BotManagerStrings.GENERIC_ERROR)
        if (bots.isEmpty()) return listOf(BotManagerStrings.NO_BOTS)
        return listOf(BotManagerStrings.botList(bots.map { it.name }))
    }

    private suspend fun startRevoke(): List<String> {
        val bots = fetchBots() ?: return listOf(BotManagerStrings.GENERIC_ERROR)
        if (bots.isEmpty()) return listOf(BotManagerStrings.NO_BOTS)
        revokeChoices = bots
        step = Step.WAITING_REVOKE_PICK
        return listOf(BotManagerStrings.revokeList(bots.map { it.name }))
    }

    private suspend fun pickRevoke(text: String): List<String> {
        val byIndex = text.toIntOrNull()?.let { revokeChoices.getOrNull(it - 1) }
        val bot = byIndex ?: revokeChoices.firstOrNull { it.name.equals(text, ignoreCase = true) }
            ?: return listOf(BotManagerStrings.INVALID_CHOICE)
        return try {
            val result = supabase.postgrest.rpc(
                "regenerate_bot_token",
                buildJsonObject { put("p_bot_id", bot.id) }
            )
            val token = result.decodeAs<String>()
            reset()
            listOf(BotManagerStrings.tokenRevoked(token))
        } catch (e: Exception) {
            e.printStackTrace()
            listOf(BotManagerStrings.GENERIC_ERROR)
        }
    }

    // ---------- أوامر البوتات من قاعدة البيانات ----------

    /** يجلب أوامر بوت من Supabase (عامة لكل المستخدمين، مثل تيليجرام). */
    suspend fun fetchCommands(botId: String): List<Command> = try {
        supabase.postgrest.rpc("get_bot_commands", buildJsonObject { put("p_bot_id", botId) })
            .decodeAs<List<Command>>()
    } catch (e: Exception) {
        e.printStackTrace()
        emptyList()
    }

    private suspend fun startSetCommands(): List<String> {
        val bots = fetchBots() ?: return listOf(BotManagerStrings.GENERIC_ERROR)
        if (bots.isEmpty()) return listOf(BotManagerStrings.NO_BOTS)
        revokeChoices = bots
        step = Step.WAITING_COMMANDS_BOT
        return listOf(BotManagerStrings.commandsBotList(bots.map { it.name }))
    }

    private fun pickCommandsBot(text: String): List<String> {
        val bot = text.toIntOrNull()?.let { revokeChoices.getOrNull(it - 1) }
            ?: revokeChoices.firstOrNull { it.name.equals(text, ignoreCase = true) }
            ?: return listOf(BotManagerStrings.INVALID_CHOICE)
        commandsTarget = bot
        step = Step.WAITING_COMMANDS_TEXT
        return listOf(BotManagerStrings.ASK_COMMANDS_TEXT)
    }

    private suspend fun saveCommands(text: String): List<String> {
        val bot = commandsTarget ?: return listOf(BotManagerStrings.GENERIC_ERROR)
        val parsed = if (text.trim().equals("/empty", true)) emptyList() else {
            val rx = Regex("^/?([a-zA-Z0-9_]{1,32})\\s*[-:]\\s*(.{1,256})$")
            val items = text.lines().map { it.trim() }.filter { it.isNotEmpty() }.map { rx.matchEntire(it) }
            if (items.isEmpty() || items.any { it == null }) return listOf(BotManagerStrings.COMMANDS_INVALID)
            items.map { Command("/" + it!!.groupValues[1].lowercase(), it.groupValues[2].trim()) }
        }
        return try {
            val arr = JsonArray(parsed.map {
                JsonObject(mapOf("command" to JsonPrimitive(it.command), "description" to JsonPrimitive(it.description)))
            })
            supabase.postgrest.rpc("set_bot_commands", buildJsonObject {
                put("p_bot_id", bot.id)
                put("p_commands", arr)
            })
            reset()
            listOf(BotManagerStrings.COMMANDS_SAVED)
        } catch (e: Exception) {
            e.printStackTrace()
            listOf(BotManagerStrings.GENERIC_ERROR)
        }
    }

    // ---------- بوتات المستخدمين (يشغّلها أصحابها في سيرفراتهم) ----------

    @Serializable
    data class BotMsgRow(
        val id: Long,
        val direction: String,
        val text: String? = null,
        val reply_markup: JsonElement? = null,
        val edited_at: String? = null,
        val created_at: String
    )

    fun botChatId(botId: String) = "bot_$botId"
    fun isUserBotChat(chatId: String) = chatId.startsWith("bot_")
    fun botIdOf(chatId: String) = chatId.removePrefix("bot_")

    /** يضمن وجود محادثة لهذا البوت في قائمة المحادثات المحلية. */
    suspend fun ensureBotChat(context: Context, botId: String, name: String) {
        try {
            val dao = com.example.data.DatabaseProvider.getDatabase(context).chatDao()
            if (dao.getChatById(botChatId(botId)) == null) {
                dao.insert(
                    com.example.data.ChatEntity(
                        id = botChatId(botId),
                        name = name,
                        message = "Tap START to begin",
                        time = "Now",
                        timestamp = System.currentTimeMillis(),
                        isBot = true,
                        participantIds = botId
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /** آخر 100 رسالة بين المستخدم والبوت (الوارد والصادر)، من الأقدم للأحدث. */
    suspend fun fetchThread(botId: String): List<BotMsgRow> = try {
        supabase.postgrest["bot_messages"].select {
            filter { eq("bot_id", botId) }
            order("id", Order.DESCENDING)
            limit(100)
        }.decodeList<BotMsgRow>().reversed()
    } catch (e: Exception) {
        e.printStackTrace()
        emptyList()
    }

    /** المستخدم يرسل رسالة للبوت. يرجع معرّف الرسالة أو null عند الفشل. */
    suspend fun sendToBot(botId: String, text: String): Long? = try {
        supabase.postgrest.rpc(
            "send_to_bot",
            buildJsonObject { put("p_bot_id", botId); put("p_text", text) }
        ).decodeAs<Long>()
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }

    /** المستخدم يضغط زر مضمّن => callback_query لسيرفر البوت. */
    suspend fun sendCallback(botId: String, messageId: Long, data: String): Boolean = try {
        supabase.postgrest.rpc(
            "bot_callback",
            buildJsonObject { put("p_bot_id", botId); put("p_message_id", messageId); put("p_data", data) }
        )
        true
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }
}
