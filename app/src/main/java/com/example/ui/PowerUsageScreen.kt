package com.example.ui

import com.example.ui.SettingsColors

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.*

import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import com.example.ui.i18n.LocalTranslation
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val ScreenBg = SettingsColors.LightBackground
private val CardBg = SettingsColors.LightSurface
private val TextGray = SettingsColors.LightMutedForeground
private val DividerColor = SettingsColors.LightDivider
private val BlueToggle = Color(0xFF007AFF)
private val BannerBg = Color(0xFFEFF6FF)
private val BannerText = Color(0xFF2563EB)




@OptIn(ExperimentalMaterial3Api::class)




@Composable
fun PowerUsageScreen(onBack: () -> Unit) {
        Scaffold(
        containerColor = ScreenBg,
        topBar = {
            TopAppBar(
                title = { Text("Power Usage", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Lucide.ArrowLeft, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ScreenBg,
                    titleContentColor = SettingsColors.textPrimary,
                    navigationIconContentColor = SettingsColors.textPrimary
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            // Banner
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(SettingsColors.cardRadius)).background(BannerBg)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            Lucide.BatteryLow,
                            contentDescription = null,
                            tint = BannerText,
                            modifier = Modifier.size(24.dp).padding(top = 2.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            "Optimize Cryptvora to save battery and data by disabling background sync, animations, and heavy effects.",
                            color = BannerText,
                            fontSize = 12.sp,
                            lineHeight = 20.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
            }

            // Battery Saver
            item {
                PowerSectionHeader("Battery Saver")
                Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(SettingsColors.cardRadius)).background(CardBg)) {
                    val isSaverEnabled by com.example.ui.GlobalAppState.batterySaverFlow.collectAsState()
                    PowerToggleRow(
                        icon = Lucide.Battery,
                        title = "Low Power Mode",
                        subtitle = "Disables animations, auto-play, etc",
                        isChecked = isSaverEnabled,
                        onCheckedChange = { com.example.ui.GlobalAppState.isBatterySaverEnabled = it }
                    )
                }
                Spacer(modifier = Modifier.height(32.dp))
            }

            // Animations & Effects
            item {
                PowerSectionHeader("Animations & Effects")
                Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(SettingsColors.cardRadius)).background(CardBg)) {
                    Column {
                        val (particles, setParticles) = rememberBooleanPreference("power_particles", true)
                        PowerToggleRow(
                            icon = Lucide.Sparkles,
                            title = "App Particles",
                            subtitle = "Floating UI particles",
                            isChecked = particles,
                            onCheckedChange = setParticles
                        )
                        HorizontalDivider(color = DividerColor, thickness = 0.5.dp, modifier = Modifier.padding(start = 56.dp))
                        val (smooth, setSmooth) = rememberBooleanPreference("power_smooth", true)
                        PowerToggleRow(
                            icon = Lucide.Bolt,
                            title = "Smooth Transitions",
                            subtitle = "Page and element transitions",
                            isChecked = smooth,
                            onCheckedChange = setSmooth
                        )
                        HorizontalDivider(color = DividerColor, thickness = 0.5.dp, modifier = Modifier.padding(start = 56.dp))
                        val (chatFx, setChatFx) = rememberBooleanPreference("power_chat_effects", true)
                        PowerToggleRow(
                            icon = Lucide.Sparkles,
                            title = "Chat Effects",
                            subtitle = "Confetti, balloons, and sparks",
                            isChecked = chatFx,
                            onCheckedChange = setChatFx
                        )
                        HorizontalDivider(color = DividerColor, thickness = 0.5.dp, modifier = Modifier.padding(start = 56.dp))
                        val (callAnim, setCallAnim) = rememberBooleanPreference("power_call_anim", true)
                        PowerToggleRow(
                            icon = Lucide.Sparkles,
                            title = "Call Animations",
                            subtitle = "Radar rings and sound waves",
                            isChecked = callAnim,
                            onCheckedChange = setCallAnim
                        )
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
            }

            // Media Autoplay
            item {
                PowerSectionHeader("Media Autoplay")
                Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(SettingsColors.cardRadius)).background(CardBg)) {
                    Column {
                        val (autoVid, setAutoVid) = rememberBooleanPreference("power_auto_video", true)
                        PowerToggleRow(
                            icon = Lucide.Video,
                            title = "Autoplay Videos",
                            subtitle = "",
                            isChecked = autoVid,
                            onCheckedChange = setAutoVid
                        )
                        HorizontalDivider(color = DividerColor, thickness = 0.5.dp, modifier = Modifier.padding(start = 56.dp))
                        val (autoGif, setAutoGif) = rememberBooleanPreference("power_auto_gif", true)
                        PowerToggleRow(
                            icon = Lucide.Video,
                            title = "Autoplay GIFs",
                            subtitle = "",
                            isChecked = autoGif,
                            onCheckedChange = setAutoGif
                        )
                        HorizontalDivider(color = DividerColor, thickness = 0.5.dp, modifier = Modifier.padding(start = 56.dp))
                        val (animSticker, setAnimSticker) = rememberBooleanPreference("power_anim_stickers", true)
                        PowerToggleRow(
                            icon = Lucide.Video,
                            title = "Animated Stickers",
                            subtitle = "",
                            isChecked = animSticker,
                            onCheckedChange = setAnimSticker
                        )
                        HorizontalDivider(color = DividerColor, thickness = 0.5.dp, modifier = Modifier.padding(start = 56.dp))
                        val (animEmoji, setAnimEmoji) = rememberBooleanPreference("power_anim_emoji", true)
                        PowerToggleRow(
                            icon = Lucide.Video,
                            title = "Animated Emoji",
                            subtitle = "",
                            isChecked = animEmoji,
                            onCheckedChange = setAnimEmoji
                        )
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun PowerSectionHeader(text: String) {

    Text(
        text = text,
        color = TextGray,
        fontSize = 12.sp, lineHeight = 16.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
    )
}

@Composable
fun PowerItemRow(icon: ImageVector, rotateIcon: Boolean = false, title: String, subtitle: String, rightText: String) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon, 
            contentDescription = null, 
            tint = TextGray, 
            modifier = Modifier
                .size(24.dp)
                .then(if (rotateIcon) Modifier.rotate(90f) else Modifier)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = SettingsColors.textPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            if (subtitle.isNotEmpty()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(subtitle, color = TextGray, fontSize = 12.sp, lineHeight = 16.sp)
            }
        }
        if (rightText.isNotEmpty()) {
            Spacer(modifier = Modifier.width(14.dp))
            Text(rightText, color = TextGray, fontSize = 12.sp, lineHeight = 16.sp)
        }
    }
}

@Composable
fun PowerToggleRow(icon: ImageVector, title: String, subtitle: String, isChecked: Boolean, onCheckedChange: (Boolean) -> Unit = {}) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val t = com.example.ui.i18n.LocalTranslation.current
    val handleChange: (Boolean) -> Unit = { checked ->
        onCheckedChange(checked)
        showToggleToast(context, title, checked, t, icon)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { handleChange(!isChecked) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = TextGray, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = SettingsColors.textPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            if (subtitle.isNotEmpty()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(subtitle, color = TextGray, fontSize = 12.sp, lineHeight = 16.sp)
            }
        }
        Spacer(modifier = Modifier.width(14.dp))
        Switch(
            modifier = Modifier.scale(0.85f),
            checked = isChecked,
            onCheckedChange = handleChange,
            
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = SettingsColors.blueAccent,
                uncheckedThumbColor = SettingsColors.textSecondary,
                uncheckedTrackColor = SettingsColors.divider,
                uncheckedBorderColor = Color.Transparent
            )
        )
    }
}
