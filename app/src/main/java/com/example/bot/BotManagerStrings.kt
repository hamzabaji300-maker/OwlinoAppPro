package com.example.bot

/** كل رسائل البوت المدير بالإنجليزية، في مكان واحد. بدّل هنا فقط إذا حبيت تغيّر النص. */
object BotManagerStrings {
    const val START = "START"
    const val MENU = "Menu"

    const val WELCOME = "I can help you create and manage Owlino bots.\n\n" +
        "You can control me by sending these commands:\n\n" +
        "/newbot - create a new bot\n" +
        "/mybots - list your bots\n" +
        "/revoke - generate a new token for a bot\n" +
        "/setcommands - set the commands list of a bot\n" +
        "/deletebot - delete a bot\n" +
        "/cancel - cancel the current operation"

    const val ASK_NAME = "Alright, a new bot. How are we going to call it? Please choose a name for your bot."
    const val EMPTY_NAME = "The name can't be empty. Please try again."
    const val NAME_TOO_LONG = "The name is too long (max 64 characters). Please try again."
    const val ASK_AVATAR = "Good. Now send me a profile picture for your bot."
    const val NOT_AN_IMAGE = "Please send an image."
    const val UPLOAD_FAILED = "Failed to upload the picture. Please try again."
    const val NOT_LOGGED_IN = "You must be logged in to create a bot."
    const val CANCELLED = "The current operation was cancelled."
    const val UNKNOWN_COMMAND = "I don't understand that command. Try /newbot."
    const val SEND_NEWBOT_FIRST = "I wasn't expecting an image. Send /newbot to create a bot first."
    const val NO_BOTS = "You have no bots yet. Use /newbot to create one."
    const val ASK_REVOKE = "Choose the bot to revoke the token for. Send its number or its name:"
    const val INVALID_CHOICE = "I couldn't find that bot. Send a number from the list, or /cancel."
    const val ASK_COMMANDS_BOT = "Choose the bot to set commands for. Send its number or its name:"
    const val ASK_COMMANDS_TEXT = "OK. Send me a list of commands for your bot, one per line, in this format:\n\n" +
        "command1 - Description\ncommand2 - Another description\n\nSend /empty to remove all commands."
    const val COMMANDS_INVALID = "I couldn't read that list. Use one command per line: command - Description (letters, digits and _ only)."
    const val COMMANDS_SAVED = "Success! Command list updated. It will show when users type / in a chat with your bot."
    const val GENERIC_ERROR = "Something went wrong. Please try again."

    const val ASK_DELETE = "Choose the bot to delete. Send its number or its name:"
    const val DELETE_CONFIRM_WORD = "Yes, delete it"
    const val DELETE_CANCELLED = "OK, the bot was not deleted."
    const val BOT_DELETED = "Done! The bot was deleted and its token no longer works."

    fun deleteList(names: List<String>) =
        ASK_DELETE + "\n\n" + names.mapIndexed { i, n -> "${i + 1}. $n" }.joinToString("\n")

    fun askDeleteConfirm(name: String) =
        "OK, you selected $name. Are you sure?\n\nSend \"$DELETE_CONFIRM_WORD\" to confirm, or /cancel to keep it.\n\nAll its chats and data will be removed."

    fun botList(names: List<String>) =
        "Your bots:\n\n" + names.mapIndexed { i, n -> "${i + 1}. $n" }.joinToString("\n")

    fun commandsBotList(names: List<String>) =
        ASK_COMMANDS_BOT + "\n\n" + names.mapIndexed { i, n -> "${i + 1}. $n" }.joinToString("\n")

    fun revokeList(names: List<String>) =
        ASK_REVOKE + "\n\n" + names.mapIndexed { i, n -> "${i + 1}. $n" }.joinToString("\n")

    fun success(name: String, token: String) = """
        Done! Congratulations on your new bot.

        Name: $name

        Use this token to access the API:
        $token

        Keep your token secure. Anyone with it can control your bot.
    """.trimIndent()

    fun tokenRevoked(token: String) = """
        Your token was revoked. Here is the new token:
        $token
    """.trimIndent()
}
