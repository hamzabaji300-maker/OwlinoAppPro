package com.example.ui.i18n

import androidx.compose.runtime.Composable

/**
 * Newer UI strings. They live outside the huge Translation data class on purpose:
 * Translation already has ~240 constructor parameters and the JVM/ART limit is 255,
 * adding more breaks the class at startup (VerifyError in TranslationKt.<clinit>).
 */
data class ExtraStrings(
    val pinMessageTitle: String = "Pin message",
    val pinForMeOnly: String = "Pin for me only",
    val pinForBothFmt: String = "Pin for me and %s",
    val pinForEveryone: String = "Pin for everyone",
    val mediaFolderTitle: String = "Create the Owlino folder",
    val mediaFolderBody: String = "To save photos, videos and files in the Owlino folder (like Telegram and WhatsApp), allow access to files.",
    val mediaFolderAllow: String = "Allow",
    val mediaFolderLater: String = "Later",
    val msgDeleteTitle: String = "Delete message",
    val msgDeleteTitleFmt: String = "Delete %d messages",
    val msgDeleteBody: String = "Are you sure you want to delete this message?",
    val msgDeleteBodyFmt: String = "Are you sure you want to delete these %d messages?",
    val msgAlsoDeleteForFmt: String = "Also delete for %s",
    val msgAlsoDeleteEveryone: String = "Also delete for everyone",
    val dlgDelete: String = "Delete",
    val dlgCancel: String = "Cancel",
    val msgDeletedToast: String = "Message deleted",
    val chatArchivedToast: String = "Chat archived",
    val chatUnarchivedToast: String = "Chat unarchived",
    val undoLabel: String = "Undo",
    val actionFailedToast: String = "Couldn't complete the action. Check your connection.",
    val newFollowerLabel: String = "New",
    val owlinoFeatherTitle: String = "Owlino Feather",
    val owlinoFeatherSub: String = "Buy, gift and redeem feathers",
    val owlinoPlusTitle: String = "Owlino Plus",
    val owlinoPlusSub: String = "Unlock exclusive features",
    val rdNoResults: String = "No results",
    val rdTryDifferent: String = "Try a different name or username",
    val rdSearchResults: String = "Search results",
    val rdSearchRemnants: String = "Search remnants",
    val rdKindBot: String = "bot",
    val rdKindChannel: String = "channel",
)

val extraStringsEn = ExtraStrings(
    pinMessageTitle = "Pin message",
    pinForMeOnly = "Pin for me only",
    pinForBothFmt = "Pin for me and %s",
    pinForEveryone = "Pin for everyone",
    mediaFolderTitle = "Create the Owlino folder",
    mediaFolderBody = "To save photos, videos and files in the Owlino folder (like Telegram and WhatsApp), allow access to files.",
    mediaFolderAllow = "Allow",
    mediaFolderLater = "Later",
    msgDeleteTitle = "Delete message",
    msgDeleteTitleFmt = "Delete %d messages",
    msgDeleteBody = "Are you sure you want to delete this message?",
    msgDeleteBodyFmt = "Are you sure you want to delete these %d messages?",
    msgAlsoDeleteForFmt = "Also delete for %s",
    msgAlsoDeleteEveryone = "Also delete for everyone",
    dlgDelete = "Delete",
    dlgCancel = "Cancel",
    msgDeletedToast = "Message deleted",
    chatArchivedToast = "Chat archived",
    chatUnarchivedToast = "Chat unarchived",
    undoLabel = "Undo",
    actionFailedToast = "Couldn't complete the action. Check your connection.",
    newFollowerLabel = "New",
)

val extraStringsAr = ExtraStrings(
    pinMessageTitle = "تثبيت الرسالة",
    pinForMeOnly = "تثبيت لي فقط",
    pinForBothFmt = "تثبيت لي ولـ %s",
    pinForEveryone = "تثبيت للجميع",
    mediaFolderTitle = "إنشاء مجلد Owlino",
    mediaFolderBody = "لحفظ الصور والفيديوهات والملفات في مجلد Owlino (مثل تيليجرام وواتساب)، اسمح بالوصول إلى الملفات.",
    mediaFolderAllow = "السماح",
    mediaFolderLater = "لاحقاً",
    msgDeleteTitle = "حذف الرسالة",
    msgDeleteTitleFmt = "حذف %d رسائل",
    msgDeleteBody = "هل ترغب حقًا في حذف هذه الرسالة؟",
    msgDeleteBodyFmt = "هل ترغب حقًا في حذف هذه الرسائل (%d)؟",
    msgAlsoDeleteForFmt = "حذف من عند %s أيضًا",
    msgAlsoDeleteEveryone = "حذف من عند الجميع أيضًا",
    dlgDelete = "حذف",
    dlgCancel = "إلغاء",
    msgDeletedToast = "تم حذف الرسالة",
    chatArchivedToast = "تمت أرشفة المحادثة",
    chatUnarchivedToast = "تم إلغاء أرشفة المحادثة",
    undoLabel = "تراجع",
    actionFailedToast = "تعذّر تنفيذ العملية. تحقق من الاتصال.",
    newFollowerLabel = "جديد",
    owlinoFeatherTitle = "ريش أولينو",
    owlinoFeatherSub = "اشترِ الريش وأهدِه وفعّل الأكواد",
    owlinoPlusTitle = "أولينو بلس",
    owlinoPlusSub = "افتح الميزات الحصرية",
    rdNoResults = "لا توجد نتائج",
    rdTryDifferent = "جرّب اسماً أو اسم مستخدم آخر",
    rdSearchResults = "نتائج البحث",
    rdSearchRemnants = "عمليات البحث السابقة",
    rdKindBot = "بوت",
    rdKindChannel = "قناة",
)

val extraStringsFr = ExtraStrings(
    pinMessageTitle = "Épingler le message",
    pinForMeOnly = "Épingler pour moi uniquement",
    pinForBothFmt = "Épingler pour moi et %s",
    pinForEveryone = "Épingler pour tous",
    mediaFolderTitle = "Créer le dossier Owlino",
    mediaFolderBody = "Pour enregistrer photos, vidéos et fichiers dans le dossier Owlino (comme Telegram et WhatsApp), autorisez l'accès aux fichiers.",
    mediaFolderAllow = "Autoriser",
    mediaFolderLater = "Plus tard",
    msgDeleteTitle = "Supprimer le message",
    msgDeleteTitleFmt = "Supprimer %d messages",
    msgDeleteBody = "Voulez-vous vraiment supprimer ce message ?",
    msgDeleteBodyFmt = "Voulez-vous vraiment supprimer ces %d messages ?",
    msgAlsoDeleteForFmt = "Supprimer aussi pour %s",
    msgAlsoDeleteEveryone = "Supprimer aussi pour tout le monde",
    dlgDelete = "Supprimer",
    dlgCancel = "Annuler",
    msgDeletedToast = "Message supprimé",
    chatArchivedToast = "Discussion archivée",
    chatUnarchivedToast = "Discussion désarchivée",
    undoLabel = "Annuler",
    actionFailedToast = "Action impossible. Vérifiez votre connexion.",
    newFollowerLabel = "Nouveau",
    owlinoFeatherTitle = "Owlino Feather",
    owlinoFeatherSub = "Achetez, offrez et utilisez des plumes",
    owlinoPlusTitle = "Owlino Plus",
    owlinoPlusSub = "Débloquez des fonctionnalités exclusives",
    rdNoResults = "Aucun résultat",
    rdTryDifferent = "Essayez un autre nom ou nom d'utilisateur",
    rdSearchResults = "Résultats de la recherche",
    rdSearchRemnants = "Recherches récentes",
    rdKindBot = "bot",
    rdKindChannel = "chaîne",
)

@Composable
fun rememberExtraStrings(): ExtraStrings {
    val t = LocalTranslation.current
    return when {
        t === arabicTranslation -> extraStringsAr
        t === frenchTranslation -> extraStringsFr
        else -> extraStringsEn
    }
}
