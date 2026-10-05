package com.example.bot

import com.example.ui.i18n.TranslationManager

/**
 * كل نصوص البوت المدير وواجهة البوتات، بلغة التطبيق الحالية (en / ar / fr).
 * تتبدل تلقائيا مع لغة المستخدم في الإعدادات.
 */
object BotManagerStrings {
    private fun lang(): String = try {
        TranslationManager.currentLanguageCode.value.lowercase().take(2)
    } catch (e: Exception) { "en" }

    private fun tr(en: String, ar: String, fr: String): String = when (lang()) {
        "ar" -> ar
        "fr" -> fr
        else -> en
    }

    val START get() = tr("START", "ابدأ", "DÉMARRER")
    val RESTART get() = tr("RESTART", "إعادة البدء", "REDÉMARRER")
    val MENU get() = tr("Menu", "القائمة", "Menu")
    val NOT_FOUND get() = tr("User or bot not found", "لم يتم العثور على المستخدم أو البوت", "Utilisateur ou bot introuvable")
    val COPIED_TOAST get() = tr("Copied to clipboard", "تم النسخ إلى الحافظة", "Copié dans le presse-papiers")
    val SEND_FAILED get() = tr("Couldn't send. Check your connection and try again.", "تعذر الإرسال. تحقق من الاتصال وحاول مرة أخرى.", "Échec de l'envoi. Vérifiez votre connexion et réessayez.")
    val COPIED get() = tr("Copied", "تم النسخ", "Copié")
    val BOT_LABEL get() = tr("bot", "بوت", "bot")
    val TAP_START get() = tr("Tap START to begin", "اضغط ابدأ للبدء", "Appuyez sur DÉMARRER pour commencer")

    val WELCOME get() = tr(
        "I can help you create and manage Owlino bots.\n\n" +
            "You can control me by sending these commands:\n\n" +
            "/newbot - create a new bot\n" +
            "/mybots - list your bots\n" +
            "/revoke - generate a new token for a bot\n" +
            "/setcommands - set the commands list of a bot\n" +
            "/deletebot - delete a bot\n" +
            "/cancel - cancel the current operation",
        "يمكنني مساعدتك في إنشاء وإدارة بوتات Owlino.\n\n" +
            "يمكنك التحكم بي بإرسال هذه الأوامر:\n\n" +
            "/newbot - إنشاء بوت جديد\n" +
            "/mybots - عرض بوتاتك\n" +
            "/revoke - إنشاء توكن جديد لبوت\n" +
            "/setcommands - تحديد قائمة أوامر البوت\n" +
            "/deletebot - حذف بوت\n" +
            "/cancel - إلغاء العملية الحالية",
        "Je peux vous aider à créer et gérer vos bots Owlino.\n\n" +
            "Vous pouvez me contrôler avec ces commandes :\n\n" +
            "/newbot - créer un nouveau bot\n" +
            "/mybots - afficher vos bots\n" +
            "/revoke - générer un nouveau jeton pour un bot\n" +
            "/setcommands - définir la liste des commandes d'un bot\n" +
            "/deletebot - supprimer un bot\n" +
            "/cancel - annuler l'opération en cours"
    )

    val ASK_NAME get() = tr(
        "Alright, a new bot. How are we going to call it? Please choose a name for your bot.",
        "حسنا، بوت جديد. ماذا سنسميه؟ اختر اسما لبوتك.",
        "D'accord, un nouveau bot. Comment va-t-on l'appeler ? Choisissez un nom pour votre bot."
    )
    val EMPTY_NAME get() = tr("The name can't be empty. Please try again.", "لا يمكن أن يكون الاسم فارغا. حاول مرة أخرى.", "Le nom ne peut pas être vide. Réessayez.")
    val NAME_TOO_LONG get() = tr("The name is too long (max 64 characters). Please try again.", "الاسم طويل جدا (الحد الأقصى 64 حرفا). حاول مرة أخرى.", "Le nom est trop long (64 caractères max). Réessayez.")
    val ASK_AVATAR get() = tr("Good. Now send me a profile picture for your bot.", "جيد. الآن أرسل لي صورة الملف الشخصي لبوتك.", "Bien. Envoyez-moi maintenant une photo de profil pour votre bot.")
    val NOT_AN_IMAGE get() = tr("Please send an image.", "من فضلك أرسل صورة.", "Veuillez envoyer une image.")
    val UPLOAD_FAILED get() = tr("Failed to upload the picture. Please try again.", "فشل رفع الصورة. حاول مرة أخرى.", "Échec de l'envoi de l'image. Réessayez.")
    val NOT_LOGGED_IN get() = tr("You must be logged in to create a bot.", "يجب تسجيل الدخول لإنشاء بوت.", "Vous devez être connecté pour créer un bot.")
    val CANCELLED get() = tr("The current operation was cancelled.", "تم إلغاء العملية الحالية.", "L'opération en cours a été annulée.")
    val UNKNOWN_COMMAND get() = tr("I don't understand that command. Try /newbot.", "لا أفهم هذا الأمر. جرّب /newbot.", "Je ne comprends pas cette commande. Essayez /newbot.")
    val SEND_NEWBOT_FIRST get() = tr("I wasn't expecting an image. Send /newbot to create a bot first.", "لم أكن أتوقع صورة. أرسل /newbot لإنشاء بوت أولا.", "Je n'attendais pas d'image. Envoyez d'abord /newbot pour créer un bot.")
    val NO_BOTS get() = tr("You have no bots yet. Use /newbot to create one.", "ليس لديك أي بوت بعد. استخدم /newbot لإنشاء واحد.", "Vous n'avez pas encore de bot. Utilisez /newbot pour en créer un.")
    val ASK_REVOKE get() = tr("Choose the bot to revoke the token for. Send its number or its name:", "اختر البوت الذي تريد إلغاء توكنه. أرسل رقمه أو اسمه:", "Choisissez le bot dont révoquer le jeton. Envoyez son numéro ou son nom :")
    val INVALID_CHOICE get() = tr("I couldn't find that bot. Send a number from the list, or /cancel.", "لم أجد هذا البوت. أرسل رقما من القائمة، أو /cancel.", "Je n'ai pas trouvé ce bot. Envoyez un numéro de la liste, ou /cancel.")
    val ASK_COMMANDS_BOT get() = tr("Choose the bot to set commands for. Send its number or its name:", "اختر البوت الذي تريد تحديد أوامره. أرسل رقمه أو اسمه:", "Choisissez le bot dont définir les commandes. Envoyez son numéro ou son nom :")
    val ASK_COMMANDS_TEXT get() = tr(
        "OK. Send me a list of commands for your bot, one per line, in this format:\n\n" +
            "command1 - Description\ncommand2 - Another description\n\nSend /empty to remove all commands.",
        "حسنا. أرسل لي قائمة أوامر بوتك، أمر في كل سطر، بهذا الشكل:\n\n" +
            "command1 - الوصف\ncommand2 - وصف آخر\n\nأرسل /empty لحذف كل الأوامر.",
        "D'accord. Envoyez-moi la liste des commandes de votre bot, une par ligne, au format :\n\n" +
            "command1 - Description\ncommand2 - Autre description\n\nEnvoyez /empty pour supprimer toutes les commandes."
    )
    val COMMANDS_INVALID get() = tr(
        "I couldn't read that list. Use one command per line: command - Description (letters, digits and _ only).",
        "لم أستطع قراءة القائمة. استخدم أمرا في كل سطر: command - الوصف (حروف إنجليزية وأرقام و _ فقط للأمر).",
        "Je n'ai pas pu lire cette liste. Une commande par ligne : command - Description (lettres, chiffres et _ uniquement)."
    )
    val COMMANDS_SAVED get() = tr(
        "Success! Command list updated. It will show when users type / in a chat with your bot.",
        "تم! تم تحديث قائمة الأوامر. ستظهر عندما يكتب المستخدمون / في محادثة مع بوتك.",
        "Succès ! Liste des commandes mise à jour. Elle s'affichera quand les utilisateurs tapent / dans une conversation avec votre bot."
    )
    val GENERIC_ERROR get() = tr("Something went wrong. Please try again.", "حدث خطأ ما. حاول مرة أخرى.", "Une erreur s'est produite. Réessayez.")

    val ASK_DELETE get() = tr("Choose the bot to delete. Send its number or its name:", "اختر البوت الذي تريد حذفه. أرسل رقمه أو اسمه:", "Choisissez le bot à supprimer. Envoyez son numéro ou son nom :")
    val DELETE_CONFIRM_WORD get() = tr("Yes, delete it", "نعم، احذفه", "Oui, supprime-le")
    val DELETE_CANCELLED get() = tr("OK, the bot was not deleted.", "حسنا، لم يتم حذف البوت.", "OK, le bot n'a pas été supprimé.")
    val BOT_DELETED get() = tr("Done! The bot was deleted and its token no longer works.", "تم! حُذف البوت ولم يعد توكنه يعمل.", "Terminé ! Le bot a été supprimé et son jeton ne fonctionne plus.")

    /** نقبل عبارة التأكيد بأي لغة من اللغات الثلاث. */
    fun isDeleteConfirm(text: String): Boolean {
        val t = text.trim().trimEnd('.', '!').lowercase()
        return t in setOf("yes, delete it", "نعم، احذفه", "نعم, احذفه", "oui, supprime-le")
    }

    fun deleteList(names: List<String>) =
        ASK_DELETE + "\n\n" + names.mapIndexed { i, n -> "${i + 1}. $n" }.joinToString("\n")

    fun askDeleteConfirm(name: String) = tr(
        "OK, you selected $name. Are you sure?\n\nSend \"$DELETE_CONFIRM_WORD\" to confirm, or /cancel to keep it.\n\nAll its chats and data will be removed.",
        "حسنا، اخترت $name. هل أنت متأكد؟\n\nأرسل \"$DELETE_CONFIRM_WORD\" للتأكيد، أو /cancel للإبقاء عليه.\n\nسيتم حذف كل محادثاته وبياناته.",
        "OK, vous avez choisi $name. Êtes-vous sûr ?\n\nEnvoyez \"$DELETE_CONFIRM_WORD\" pour confirmer, ou /cancel pour le garder.\n\nToutes ses conversations et données seront supprimées."
    )

    fun botList(names: List<String>) =
        tr("Your bots:", "بوتاتك:", "Vos bots :") + "\n\n" + names.mapIndexed { i, n -> "${i + 1}. $n" }.joinToString("\n")

    fun commandsBotList(names: List<String>) =
        ASK_COMMANDS_BOT + "\n\n" + names.mapIndexed { i, n -> "${i + 1}. $n" }.joinToString("\n")

    fun revokeList(names: List<String>) =
        ASK_REVOKE + "\n\n" + names.mapIndexed { i, n -> "${i + 1}. $n" }.joinToString("\n")

    fun success(name: String, token: String) = tr(
        "Done! Congratulations on your new bot.\n\nName: $name\n\nUse this token to access the API:\n`$token`\n\nKeep your token secure. Anyone with it can control your bot.",
        "تم! مبروك على بوتك الجديد.\n\nالاسم: $name\n\nاستخدم هذا التوكن للوصول إلى الـ API:\n`$token`\n\nحافظ على سرية التوكن. أي شخص يملكه يستطيع التحكم ببوتك.",
        "Terminé ! Félicitations pour votre nouveau bot.\n\nNom : $name\n\nUtilisez ce jeton pour accéder à l'API :\n`$token`\n\nGardez votre jeton secret. Toute personne qui le possède peut contrôler votre bot."
    )

    fun tokenRevoked(token: String) = tr(
        "Your token was revoked. Here is the new token:\n`$token`",
        "تم إلغاء توكنك. هذا هو التوكن الجديد:\n`$token`",
        "Votre jeton a été révoqué. Voici le nouveau jeton :\n`$token`"
    )

    // ---------- أوامر البوت المدير + أزرار /start ----------
    fun managerCommandDescriptions(): Map<String, String> = mapOf(
        "/start" to tr("Show the welcome message", "عرض رسالة الترحيب", "Afficher le message de bienvenue"),
        "/newbot" to tr("Create a new bot", "إنشاء بوت جديد", "Créer un nouveau bot"),
        "/mybots" to tr("List your bots", "عرض بوتاتك", "Afficher vos bots"),
        "/revoke" to tr("Generate a new token for a bot", "إنشاء توكن جديد لبوت", "Générer un nouveau jeton pour un bot"),
        "/setcommands" to tr("Change the commands list of a bot", "تغيير قائمة أوامر بوت", "Modifier la liste des commandes d'un bot"),
        "/deletebot" to tr("Delete a bot", "حذف بوت", "Supprimer un bot"),
        "/cancel" to tr("Cancel the current operation", "إلغاء العملية الحالية", "Annuler l'opération en cours")
    )

    fun startMarkup(): String {
        val a = tr("New bot", "بوت جديد", "Nouveau bot")
        val b = tr("My bots", "بوتاتي", "Mes bots")
        val c = tr("Revoke token", "إلغاء التوكن", "Révoquer le jeton")
        val d = tr("Set commands", "تحديد الأوامر", "Définir les commandes")
        return "{\"inline_keyboard\":[[{\"text\":\"$a\",\"callback_data\":\"/newbot\"},{\"text\":\"$b\",\"callback_data\":\"/mybots\"}],[{\"text\":\"$c\",\"callback_data\":\"/revoke\"},{\"text\":\"$d\",\"callback_data\":\"/setcommands\"}]]}"
    }

    // ---------- عدد المستخدمين في الهيدر ----------
    /** 500 → "500" ، 1200 → "1.2K" ، 2_000_000 → "2M" */
    fun compactCount(n: Int): String = when {
        n >= 1_000_000 -> trimZero(n / 1_000_000.0) + "M"
        n >= 1_000 -> trimZero(n / 1_000.0) + "K"
        else -> n.toString()
    }

    private fun trimZero(v: Double): String {
        val s = String.format(java.util.Locale.US, "%.1f", v)
        return if (s.endsWith(".0")) s.dropLast(2) else s
    }

    fun usersLabel(n: Int): String {
        if (n <= 0) return BOT_LABEL
        val c = compactCount(n)
        return when (lang()) {
            "ar" -> when {
                n == 1 -> "مستخدم واحد"
                n == 2 -> "مستخدمان"
                n in 3..10 -> "$c مستخدمين"
                else -> "$c مستخدم"
            }
            "fr" -> if (n == 1) "1 utilisateur" else "$c utilisateurs"
            else -> if (n == 1) "1 user" else "$c users"
        }
    }
}
