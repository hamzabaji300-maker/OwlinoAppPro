package com.example.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.icons.lucide.ArrowLeft
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Smartphone
import com.composables.icons.lucide.Wifi
import com.example.nearby.NearbyShare
import com.example.ui.i18n.TranslationManager

/** نصوص «الإرسال القريب» (عربي / فرنسي / إنجليزي). */
object NearbyStrings {
    fun pick(ar: String, fr: String, en: String): String =
        when (TranslationManager.currentLanguageCode.value) { "ar" -> ar; "fr" -> fr; else -> en }

    fun title() = pick("الإرسال القريب", "Envoi à proximité", "Nearby share")
    fun subtitle() = pick("نقل الملفات بدون إنترنت (بلوتوث وWi-Fi مباشر)", "Fichiers sans internet (Bluetooth et Wi-Fi direct)", "Files without internet (Bluetooth & Wi-Fi Direct)")
}

@Composable
fun NearbyShareScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val lang by TranslationManager.currentLanguageCode.collectAsState()
    fun t(ar: String, fr: String, en: String): String = when (lang) { "ar" -> ar; "fr" -> fr; else -> en }
    val accent = LocalSettingsTheme.current.accent
    val deviceName = remember { "Owlino · " + android.os.Build.MODEL }

    var hasPerms by remember { mutableStateOf(NearbyShare.hasPermissions(context)) }
    var role by remember { mutableStateOf(0) } // 0 = لم يختر، 1 = إرسال، 2 = استقبال
    var selected by remember { mutableStateOf<List<Uri>>(emptyList()) }

    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        hasPerms = NearbyShare.hasPermissions(context)
    }
    val pickLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isNotEmpty()) selected = uris
    }

    val mode by NearbyShare.mode.collectAsState()
    val peers by NearbyShare.peers.collectAsState()
    val incoming by NearbyShare.incoming.collectAsState()
    val connected by NearbyShare.connected.collectAsState()
    val transfers by NearbyShare.transfers.collectAsState()
    val info by NearbyShare.info.collectAsState()

    DisposableEffect(Unit) {
        onDispose { NearbyShare.stopAll() }
    }

    Column(modifier = Modifier.fillMaxSize().systemBarsPadding().background(SettingsColors.background)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Lucide.ArrowLeft, contentDescription = "Back", tint = SettingsColors.textPrimary,
                modifier = Modifier.size(24.dp).clickable { onBack() }
            )
            Spacer(Modifier.width(14.dp))
            Text(NearbyStrings.title(), fontSize = 17.sp, fontWeight = FontWeight.Medium, color = SettingsColors.textPrimary)
        }
        HorizontalDivider(color = SettingsColors.divider, thickness = 1.dp)

        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
            NbCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Lucide.Wifi, contentDescription = null, tint = accent, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(
                        t("نقل ملفات بدون إنترنت", "Transfert sans internet", "Transfer without internet"),
                        color = SettingsColors.textPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    t("يعمل بين هاتفين قريبين (بضعة أمتار): يكتشف البلوتوث الجهاز، ثم ينقل الملفات بسرعة عبر Wi-Fi مباشر دون راوتر ولا إنترنت. يلزم أن يكون Owlino مثبّتًا على الهاتفين، مع تشغيل البلوتوث والواي فاي، وإبقاء الشاشة مفتوحة أثناء النقل.",
                        "Fonctionne entre deux téléphones proches : le Bluetooth trouve l'appareil, puis les fichiers passent en Wi-Fi direct, sans routeur ni internet. Owlino doit être installé sur les deux, avec Bluetooth et Wi-Fi activés, écran allumé pendant le transfert.",
                        "Works between two nearby phones: Bluetooth finds the device, then files move over Wi-Fi Direct with no router or internet. Owlino must be installed on both, with Bluetooth and Wi-Fi on, and the screen kept on during the transfer."),
                    color = SettingsColors.textSecondary, fontSize = 13.sp, lineHeight = 18.sp
                )
            }

            if (!hasPerms) {
                Spacer(Modifier.height(14.dp))
                NbCard {
                    Text(
                        t("نحتاج إذن «الأجهزة القريبة» (والموقع في الإصدارات القديمة) لاكتشاف الهاتف الآخر. لا يتم تتبع موقعك.",
                            "Nous avons besoin de l'autorisation « appareils à proximité » (et de la localisation sur les anciennes versions) pour trouver l'autre téléphone. Votre position n'est pas suivie.",
                            "We need the “Nearby devices” permission (and location on older Android) to find the other phone. Your location is not tracked."),
                        color = SettingsColors.textPrimary, fontSize = 14.sp, lineHeight = 19.sp
                    )
                    Spacer(Modifier.height(12.dp))
                    NbButton(t("السماح", "Autoriser", "Allow"), accent, true) {
                        permLauncher.launch(NearbyShare.requiredPermissions().toTypedArray())
                    }
                }
                return@Column
            }

            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                RoleCard(Modifier.weight(1f), t("إرسال", "Envoyer", "Send"), role == 1, accent) {
                    if (role != 1) { NearbyShare.stopAll(); role = 1 }
                }
                RoleCard(Modifier.weight(1f), t("استقبال", "Recevoir", "Receive"), role == 2, accent) {
                    if (role != 2) { NearbyShare.stopAll(); role = 2 }
                }
            }

            if (role == 2) {
                Spacer(Modifier.height(14.dp))
                NbCard {
                    Text(t("اسم جهازك", "Nom de l'appareil", "Your device name"), color = SettingsColors.textSecondary, fontSize = 12.sp)
                    Text(deviceName, color = SettingsColors.textPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(12.dp))
                    if (mode == NearbyShare.Mode.RECEIVING) {
                        Text(
                            t("هاتفك ظاهر الآن للمرسلين القريبين. اطلب من صديقك أن يختار «إرسال» ثم يضغط على اسم جهازك.",
                                "Votre téléphone est visible. Demandez à votre ami de choisir « Envoyer » puis d'appuyer sur votre nom.",
                                "Your phone is now visible. Ask your friend to choose “Send” and tap your device name."),
                            color = SettingsColors.textSecondary, fontSize = 13.sp, lineHeight = 18.sp
                        )
                        Spacer(Modifier.height(12.dp))
                        NbButton(t("إيقاف الاستقبال", "Arrêter", "Stop receiving"), accent, false) { NearbyShare.stopAll() }
                    } else {
                        NbButton(t("ابدأ الاستقبال", "Commencer à recevoir", "Start receiving"), accent, true) {
                            NearbyShare.startReceiving(context, deviceName)
                        }
                    }
                }
            }

            if (role == 1) {
                Spacer(Modifier.height(14.dp))
                NbCard {
                    Text(t("1) اختر الملفات", "1) Choisir les fichiers", "1) Pick files"), color = SettingsColors.textPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    NbButton(
                        if (selected.isEmpty()) t("اختيار ملفات", "Choisir", "Choose files")
                        else t("${selected.size} ملف مختار - تغيير", "${selected.size} fichier(s) - changer", "${selected.size} selected - change"),
                        accent, selected.isEmpty()
                    ) { pickLauncher.launch(arrayOf("*/*")) }
                    Spacer(Modifier.height(16.dp))
                    Text(t("2) ابحث عن الهاتف الآخر", "2) Trouver l'autre téléphone", "2) Find the other phone"), color = SettingsColors.textPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    if (mode == NearbyShare.Mode.SENDING) {
                        Text(
                            if (peers.isEmpty()) t("جارٍ البحث… اطلب من الطرف الآخر اختيار «استقبال».", "Recherche… demandez à l'autre de choisir « Recevoir ».", "Searching… ask the other person to choose “Receive”.")
                            else t("اضغط على اسم الجهاز لإرسال الملفات:", "Appuyez sur l'appareil pour envoyer :", "Tap the device to send:"),
                            color = SettingsColors.textSecondary, fontSize = 13.sp
                        )
                        Spacer(Modifier.height(8.dp))
                        peers.forEach { p ->
                            val isConnected = connected.containsKey(p.id)
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(accent.copy(alpha = 0.12f))
                                    .clickable(enabled = !isConnected) { NearbyShare.connectTo(p) }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Lucide.Smartphone, contentDescription = null, tint = accent, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(10.dp))
                                Text(p.name, color = SettingsColors.textPrimary, fontSize = 15.sp, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                if (isConnected) Text(t("متصل", "Connecté", "Connected"), color = accent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        NbButton(t("إيقاف", "Arrêter", "Stop"), accent, false) { NearbyShare.stopAll() }
                    } else {
                        NbButton(t("ابحث عن أجهزة قريبة", "Chercher des appareils", "Search nearby devices"), accent, selected.isNotEmpty(), enabled = selected.isNotEmpty()) {
                            NearbyShare.startSending(context, deviceName, selected)
                        }
                    }
                }
            }

            info?.let {
                Spacer(Modifier.height(12.dp))
                Text(it, color = Color(0xFFE5484D), fontSize = 13.sp, lineHeight = 18.sp)
            }

            if (transfers.isNotEmpty()) {
                Spacer(Modifier.height(18.dp))
                Text(t("عمليات النقل", "Transferts", "Transfers"), color = SettingsColors.textSecondary, fontSize = 13.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(start = 6.dp, bottom = 8.dp))
                transfers.forEach { tr ->
                    NbCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(tr.name, color = SettingsColors.textPrimary, fontSize = 14.sp, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                when (tr.state) {
                                    NearbyShare.State.DONE -> if (tr.outgoing) t("أُرسل", "Envoyé", "Sent") else t("تم الحفظ", "Enregistré", "Saved")
                                    NearbyShare.State.FAILED -> t("فشل", "Échec", "Failed")
                                    else -> if (tr.outgoing) t("يُرسل…", "Envoi…", "Sending…") else t("يُستقبل…", "Réception…", "Receiving…")
                                },
                                color = if (tr.state == NearbyShare.State.FAILED) Color(0xFFE5484D) else accent,
                                fontSize = 12.sp, fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        val frac = when {
                            tr.state == NearbyShare.State.DONE -> 1f
                            tr.total > 0 -> (tr.done.toFloat() / tr.total).coerceIn(0f, 1f)
                            else -> 0f
                        }
                        Box(modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(accent.copy(alpha = 0.18f))) {
                            Box(modifier = Modifier.fillMaxWidth(frac).fillMaxHeight().background(accent))
                        }
                        if (tr.savedPath != null) {
                            Spacer(Modifier.height(6.dp))
                            Text(tr.savedPath, color = SettingsColors.textSecondary, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    incoming?.let { inc ->
        AlertDialog(
            onDismissRequest = { NearbyShare.rejectIncoming() },
            title = { Text(t("طلب استقبال ملفات", "Demande de réception", "Incoming transfer")) },
            text = {
                Text(
                    t("«${inc.name}» يريد إرسال ملفات إليك. هل تقبل؟",
                        "« ${inc.name} » veut vous envoyer des fichiers. Accepter ?",
                        "“${inc.name}” wants to send you files. Accept?")
                )
            },
            confirmButton = { TextButton(onClick = { NearbyShare.acceptIncoming() }) { Text(t("قبول", "Accepter", "Accept"), color = accent) } },
            dismissButton = { TextButton(onClick = { NearbyShare.rejectIncoming() }) { Text(t("رفض", "Refuser", "Decline")) } }
        )
    }
}

@Composable
private fun NbCard(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(SettingsColors.cardRadius))
            .background(SettingsColors.surface)
            .padding(16.dp)
    ) { content() }
}

@Composable
private fun RoleCard(modifier: Modifier, label: String, selected: Boolean, accent: Color, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(SettingsColors.cardRadius))
            .background(if (selected) accent.copy(alpha = 0.18f) else SettingsColors.surface)
            .clickable { onClick() }
            .padding(vertical = 22.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = if (selected) accent else SettingsColors.textPrimary)
    }
}

@Composable
private fun NbButton(text: String, accent: Color, filled: Boolean, enabled: Boolean = true, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(50))
            .background(if (!enabled) SettingsColors.divider else if (filled) accent else accent.copy(alpha = 0.15f))
            .clickable(enabled = enabled) { onClick() }
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text, fontSize = 14.sp, fontWeight = FontWeight.Bold,
            color = if (!enabled) SettingsColors.textSecondary else if (filled) Color.White else accent
        )
    }
}
