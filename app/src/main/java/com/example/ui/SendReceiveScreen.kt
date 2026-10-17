package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.icons.lucide.ArrowLeft
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Smartphone
import com.composables.icons.lucide.Wifi
import com.composables.icons.lucide.Zap
import com.example.ui.i18n.TranslationManager
import com.example.util.SendReceivePrefs
import kotlin.math.roundToInt

/** نصوص قسم «الإرسال والاستقبال» (عربي / فرنسي / إنجليزي حسب لغة التطبيق). */
object SendReceiveStrings {
    private fun pick(ar: String, fr: String, en: String): String =
        when (TranslationManager.currentLanguageCode.value) { "ar" -> ar; "fr" -> fr; else -> en }

    fun title() = pick("الإرسال والاستقبال", "Envoi et réception", "Send & Receive")
    fun subtitle() = pick("السرعة، التنزيل التلقائي، جودة الرفع", "Vitesse, téléchargement auto, qualité d'envoi", "Speed, auto-download, upload quality")
}

@Composable
fun SendReceiveScreen(onBack: () -> Unit, onNearbyClick: () -> Unit = {}) {
    val context = LocalContext.current
    val lang by TranslationManager.currentLanguageCode.collectAsState()
    fun t(ar: String, fr: String, en: String): String = when (lang) { "ar" -> ar; "fr" -> fr; else -> en }
    val accent = LocalSettingsTheme.current.accent

    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .background(SettingsColors.background)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Lucide.ArrowLeft,
                contentDescription = "Back",
                tint = SettingsColors.textPrimary,
                modifier = Modifier.size(24.dp).clickable { onBack() }
            )
            Spacer(modifier = Modifier.width(14.dp))
            Text(
                text = SendReceiveStrings.title(),
                fontSize = 17.sp,
                fontWeight = FontWeight.Medium,
                color = SettingsColors.textPrimary
            )
        }
        HorizontalDivider(color = SettingsColors.divider, thickness = 1.dp)

        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)
        ) {
            // ───────── وضع السرعة (شريط بثلاث محطات) ─────────
            SrCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Lucide.Zap, contentDescription = null, tint = accent, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(t("وضع السرعة", "Mode de vitesse", "Speed mode"), color = SettingsColors.textPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.height(6.dp))
                val modeDesc = when (SendReceivePrefs.speedMode) {
                    SendReceivePrefs.MODE_SAVER -> t(
                        "أقل استهلاك للبيانات: لا تنزيل تلقائي على بيانات الجوال، وصور أخف عند الإرسال.",
                        "Moins de données : pas de téléchargement auto en mobile, images plus légères à l'envoi.",
                        "Lowest data use: no auto-download on mobile data, lighter photos when sending."
                    )
                    SendReceivePrefs.MODE_FAST -> t(
                        "أسرع تجربة: ينزّل الصور والفيديوهات والملفات تلقائيًا عبر الواي فاي، ولا يسألك عن جودة الفيديو.",
                        "Expérience la plus rapide : photos, vidéos et fichiers téléchargés automatiquement en Wi-Fi, sans question sur la qualité vidéo.",
                        "Fastest experience: photos, videos and files download automatically on Wi-Fi, and video quality isn't asked."
                    )
                    else -> t(
                        "متوازن: صور وفيديوهات تلقائيًا عبر الواي فاي، وصور صغيرة فقط على بيانات الجوال.",
                        "Équilibré : photos et vidéos auto en Wi-Fi, petites photos seulement en mobile.",
                        "Balanced: photos and videos automatically on Wi-Fi, small photos only on mobile data."
                    )
                }
                Text(modeDesc, color = SettingsColors.textSecondary, fontSize = 13.sp, lineHeight = 18.sp)
                Spacer(Modifier.height(10.dp))
                // الشريط والعناوين دائمًا من اليسار لليمين حتى تطابق مواضع الكلمات موضع المقبض
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    Column {
                        Slider(
                            value = SendReceivePrefs.speedMode.toFloat(),
                            onValueChange = {
                                val m = it.roundToInt().coerceIn(0, 2)
                                if (m != SendReceivePrefs.speedMode) SendReceivePrefs.applyPreset(context, m)
                            },
                            valueRange = 0f..2f,
                            steps = 1,
                            colors = srSliderColors(accent)
                        )
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            val labels = listOf(
                                t("توفير", "Éco", "Saver"),
                                t("متوازن", "Équilibré", "Balanced"),
                                t("الأسرع", "Rapide", "Fastest")
                            )
                            labels.forEachIndexed { i, label ->
                                Text(
                                    label,
                                    fontSize = 12.sp,
                                    fontWeight = if (i == SendReceivePrefs.speedMode) FontWeight.Bold else FontWeight.Normal,
                                    color = if (i == SendReceivePrefs.speedMode) accent else SettingsColors.textSecondary
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))
            // ───────── الإرسال القريب بدون إنترنت (بلوتوث + Wi-Fi مباشر) ─────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(SettingsColors.cardRadius))
                    .background(SettingsColors.surface)
                    .clickable { onNearbyClick() }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Lucide.Smartphone, contentDescription = null, tint = accent, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(NearbyStrings.title(), color = SettingsColors.textPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(2.dp))
                    Text(NearbyStrings.subtitle(), color = SettingsColors.textSecondary, fontSize = 13.sp, lineHeight = 18.sp)
                }
                Text(t("فتح", "Ouvrir", "Open"), color = accent, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }

            SrSectionTitle(t("التنزيل التلقائي", "Téléchargement automatique", "Auto-download"))
            SrNetworkCard(
                icon = Lucide.Wifi,
                title = t("عبر الواي فاي", "En Wi-Fi", "On Wi-Fi"),
                accent = accent,
                photoLabel = t("الصور", "Photos", "Photos"),
                videoLabel = t("الفيديوهات", "Vidéos", "Videos"),
                fileLabel = t("الملفات", "Fichiers", "Files"),
                sizeLabel = t("أقصى حجم", "Taille max", "Max size"),
                photo = SendReceivePrefs.wifiPhoto, video = SendReceivePrefs.wifiVideo, file = SendReceivePrefs.wifiFile,
                maxIdx = SendReceivePrefs.wifiMaxIdx,
                onPhoto = { SendReceivePrefs.wifiPhoto = it; SendReceivePrefs.save(context) },
                onVideo = { SendReceivePrefs.wifiVideo = it; SendReceivePrefs.save(context) },
                onFile = { SendReceivePrefs.wifiFile = it; SendReceivePrefs.save(context) },
                onMax = { SendReceivePrefs.wifiMaxIdx = it; SendReceivePrefs.save(context) }
            )
            Spacer(Modifier.height(12.dp))
            SrNetworkCard(
                icon = Lucide.Smartphone,
                title = t("عبر بيانات الجوال", "En données mobiles", "On mobile data"),
                accent = accent,
                photoLabel = t("الصور", "Photos", "Photos"),
                videoLabel = t("الفيديوهات", "Vidéos", "Videos"),
                fileLabel = t("الملفات", "Fichiers", "Files"),
                sizeLabel = t("أقصى حجم", "Taille max", "Max size"),
                photo = SendReceivePrefs.mobPhoto, video = SendReceivePrefs.mobVideo, file = SendReceivePrefs.mobFile,
                maxIdx = SendReceivePrefs.mobMaxIdx,
                onPhoto = { SendReceivePrefs.mobPhoto = it; SendReceivePrefs.save(context) },
                onVideo = { SendReceivePrefs.mobVideo = it; SendReceivePrefs.save(context) },
                onFile = { SendReceivePrefs.mobFile = it; SendReceivePrefs.save(context) },
                onMax = { SendReceivePrefs.mobMaxIdx = it; SendReceivePrefs.save(context) }
            )
            Text(
                t("أثناء التجوال لا يوجد تنزيل تلقائي أبدًا. ما لم يُنزَّل تلقائيًا تنزّله بضغطة على زر التنزيل.",
                    "En itinérance, aucun téléchargement automatique. Le reste se télécharge d'un appui sur le bouton.",
                    "While roaming nothing downloads automatically. Anything else downloads with one tap."),
                color = SettingsColors.textSecondary, fontSize = 12.sp, lineHeight = 17.sp,
                modifier = Modifier.padding(top = 8.dp, start = 4.dp, end = 4.dp)
            )

            SrSectionTitle(t("جودة الإرسال", "Qualité d'envoi", "Upload quality"))
            SrCard {
                Text(t("جودة الصور", "Qualité des photos", "Photo quality"), color = SettingsColors.textPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(2.dp))
                val photoHint = when (SendReceivePrefs.photoQualityIdx) {
                    0 -> t("خفيفة: أسرع إرسال وأقل بيانات.", "Légère : envoi le plus rapide, moins de données.", "Light: fastest sending, least data.")
                    2 -> t("عالية: تفاصيل أكثر، والإرسال أبطأ.", "Haute : plus de détails, envoi plus lent.", "High: more detail, slower sending.")
                    else -> t("عادية: توازن بين الجودة والسرعة.", "Standard : équilibre qualité / vitesse.", "Standard: balance between quality and speed.")
                }
                Text(photoHint, color = SettingsColors.textSecondary, fontSize = 13.sp)
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    Column {
                        Slider(
                            value = SendReceivePrefs.photoQualityIdx.toFloat(),
                            onValueChange = {
                                val v = it.roundToInt().coerceIn(0, 2)
                                if (v != SendReceivePrefs.photoQualityIdx) { SendReceivePrefs.photoQualityIdx = v; SendReceivePrefs.save(context) }
                            },
                            valueRange = 0f..2f,
                            steps = 1,
                            colors = srSliderColors(accent)
                        )
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            listOf(t("خفيفة", "Légère", "Light"), t("عادية", "Standard", "Standard"), t("عالية", "Haute", "High"))
                                .forEachIndexed { i, label ->
                                    Text(label, fontSize = 12.sp,
                                        fontWeight = if (i == SendReceivePrefs.photoQualityIdx) FontWeight.Bold else FontWeight.Normal,
                                        color = if (i == SendReceivePrefs.photoQualityIdx) accent else SettingsColors.textSecondary)
                                }
                        }
                    }
                }

                Spacer(Modifier.height(18.dp))
                HorizontalDivider(color = SettingsColors.divider, thickness = 1.dp)
                Spacer(Modifier.height(14.dp))

                Text(t("جودة الفيديو", "Qualité des vidéos", "Video quality"), color = SettingsColors.textPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(2.dp))
                Text(
                    t("اختر جودة ثابتة لتُرسل الفيديوهات فورًا دون سؤال كل مرة.",
                        "Choisissez une qualité fixe pour envoyer sans question à chaque fois.",
                        "Pick a fixed quality to send videos instantly without being asked every time."),
                    color = SettingsColors.textSecondary, fontSize = 13.sp
                )
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        0 to t("اسألني", "Demander", "Ask me"),
                        1 to "720p",
                        2 to "1080p"
                    ).forEach { (mode, label) ->
                        val selected = SendReceivePrefs.videoMode == mode
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(if (selected) accent.copy(alpha = 0.18f) else SettingsColors.divider)
                                .clickable { SendReceivePrefs.videoMode = mode; SendReceivePrefs.save(context) }
                                .padding(horizontal = 16.dp, vertical = 9.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(label, fontSize = 13.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                color = if (selected) accent else SettingsColors.textPrimary)
                        }
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun srSliderColors(accent: Color) = SliderDefaults.colors(
    thumbColor = accent,
    activeTrackColor = accent,
    inactiveTrackColor = accent.copy(alpha = 0.22f),
    activeTickColor = Color.White.copy(alpha = 0.75f),
    inactiveTickColor = accent.copy(alpha = 0.5f)
)

@Composable
private fun SrSectionTitle(text: String) {
    Text(
        text, color = SettingsColors.textSecondary, fontSize = 13.sp, fontWeight = FontWeight.Medium,
        modifier = Modifier.padding(start = 6.dp, top = 22.dp, bottom = 8.dp)
    )
}

@Composable
private fun SrCard(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(SettingsColors.cardRadius))
            .background(SettingsColors.surface)
            .padding(16.dp)
    ) { content() }
}

@Composable
private fun SrNetworkCard(
    icon: ImageVector,
    title: String,
    accent: Color,
    photoLabel: String, videoLabel: String, fileLabel: String, sizeLabel: String,
    photo: Boolean, video: Boolean, file: Boolean,
    maxIdx: Int,
    onPhoto: (Boolean) -> Unit, onVideo: (Boolean) -> Unit, onFile: (Boolean) -> Unit,
    onMax: (Int) -> Unit
) {
    SrCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
            Text(title, color = SettingsColors.textPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(6.dp))
        SrSwitchRow(photoLabel, photo, accent, onPhoto)
        SrSwitchRow(videoLabel, video, accent, onVideo)
        SrSwitchRow(fileLabel, file, accent, onFile)
        Spacer(Modifier.height(6.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(sizeLabel, color = SettingsColors.textPrimary, fontSize = 14.sp)
            Text("${SendReceivePrefs.SIZE_STEPS_MB[maxIdx.coerceIn(0, SendReceivePrefs.SIZE_STEPS_MB.size - 1)]} MB", color = accent, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Slider(
                value = maxIdx.toFloat(),
                onValueChange = {
                    val v = it.roundToInt().coerceIn(0, SendReceivePrefs.SIZE_STEPS_MB.size - 1)
                    if (v != maxIdx) onMax(v)
                },
                valueRange = 0f..(SendReceivePrefs.SIZE_STEPS_MB.size - 1).toFloat(),
                steps = SendReceivePrefs.SIZE_STEPS_MB.size - 2,
                colors = srSliderColors(accent)
            )
        }
    }
}

@Composable
private fun SrSwitchRow(label: String, checked: Boolean, accent: Color, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onChange(!checked) }.padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = SettingsColors.textPrimary, fontSize = 14.sp)
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(checkedTrackColor = accent, checkedThumbColor = Color.White)
        )
    }
}
