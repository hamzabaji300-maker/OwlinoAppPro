package com.example.ui

import com.example.ui.SettingsColors

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.*

import androidx.compose.runtime.*
import com.example.supabase
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.contentOrNull
import androidx.compose.ui.Alignment
import com.example.ui.i18n.LocalTranslation
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp








@Composable
fun LanguageScreen(onBack: () -> Unit) {
    var showTranslateBtn by remember { mutableStateOf(false) }
    var translateChat by remember { mutableStateOf(false) }
    
    val languageCodes = remember {
        mapOf(
            "English" to "en",
            "Français" to "fr",
            "Español" to "es",
            "Deutsch" to "de",
            "Português" to "pt",
            "العربية" to "ar",
            "中文" to "zh",
            "日本語" to "ja"
        )
    }
    
    var selectedLanguageCode by remember { mutableStateOf("ar") }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        try {
            val userId = supabase.auth.currentUserOrNull()?.id
            if (userId != null) {
                val result = supabase.postgrest["profiles"]
                    .select {
                        filter { eq("id", userId) }
                    }.decodeSingleOrNull<JsonObject>()
                
                val code = result?.get("language_code")?.jsonPrimitive?.contentOrNull
                if (code != null) {
                    selectedLanguageCode = code
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .background(SettingsColors.background)
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Lucide.ArrowLeft,
                contentDescription = com.example.ui.i18n.LocalTranslation.current.back,
                tint = SettingsColors.textPrimary,
                modifier = Modifier
                    .size(24.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onBack
                    )
            )
            Spacer(modifier = Modifier.width(14.dp))
            Text(
                text = com.example.ui.i18n.LocalTranslation.current.language,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = SettingsColors.textPrimary
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Section: Translate messages
            Text(
                text = com.example.ui.i18n.LocalTranslation.current.translateMessages,
                fontSize = 12.sp, lineHeight = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = SettingsColors.textSecondary,
                modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
            )

            // Card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(SettingsColors.cardRadius))
                    .background(SettingsColors.surface)
            ) {
                LanguageToggleItem(
                    icon = Lucide.ArrowDownAZ,
                    title = com.example.ui.i18n.LocalTranslation.current.showTranslateButton,
                    subtitle = com.example.ui.i18n.LocalTranslation.current.appearsWhenTapMessage,
                    isChecked = showTranslateBtn,
                    onToggle = { showTranslateBtn = it },
                    isLast = false,
                    dividerColor = SettingsColors.divider
                )
                LanguageToggleItem(
                    icon = Lucide.MessageSquare,
                    title = com.example.ui.i18n.LocalTranslation.current.translateEntireChat,
                    subtitle = com.example.ui.i18n.LocalTranslation.current.autoTranslateIncoming,
                    isChecked = translateChat,
                    onToggle = { translateChat = it },
                    isLast = true,
                    dividerColor = SettingsColors.divider
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Section: Interface language
            Text(
                text = com.example.ui.i18n.LocalTranslation.current.interfaceLanguage,
                fontSize = 12.sp, lineHeight = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = SettingsColors.textSecondary,
                modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
            )
    val languages = listOf(
                "English" to "English (US)",
                "Français" to "French",
                "Español" to "Spanish",
                "Deutsch" to "German",
                "Português" to "Portuguese",
                "العربية" to "Arabic",
                "中文" to "Chinese (Simplified)",
                "日本語" to "Japanese"
            )

            // Card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(SettingsColors.cardRadius))
                    .background(SettingsColors.surface)
            ) {
                languages.forEachIndexed { index, (lang, sub) ->
                    LanguageRadioItem(
                        title = lang,
                        subtitle = sub,
                        isSelected = selectedLanguageCode == languageCodes[lang],
                        onClick = { 
                            val newCode = languageCodes[lang] ?: "ar"
                            selectedLanguageCode = newCode
                            com.example.ui.i18n.TranslationManager.setLanguage(newCode)
                            scope.launch {
                                try {
                                    val userId = supabase.auth.currentUserOrNull()?.id
                                    if (userId != null) {
                                        supabase.postgrest["profiles"].update(
                                            mapOf("language_code" to newCode)
                                        ) {
                                            filter { eq("id", userId) }
                                        }
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                        },
                        isLast = index == languages.size - 1,
                        dividerColor = SettingsColors.divider
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))

            // Section: Region & format
            Text(
                text = com.example.ui.i18n.LocalTranslation.current.regionAndFormat,
                fontSize = 12.sp, lineHeight = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = SettingsColors.textSecondary,
                modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
            )

            // Card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(SettingsColors.cardRadius))
                    .background(SettingsColors.surface)
            ) {
                LanguageActionItem(
                    icon = Lucide.MapPin,
                    title = com.example.ui.i18n.LocalTranslation.current.region,
                    trailingText = "Portugal",
                    isLast = false,
                    dividerColor = SettingsColors.divider
                )
                LanguageActionItem(
                    icon = Lucide.CalendarClock,
                    title = com.example.ui.i18n.LocalTranslation.current.timeFormat,
                    trailingText = com.example.ui.i18n.LocalTranslation.current.twentyFourHour,
                    isLast = true,
                    dividerColor = SettingsColors.divider
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            
            // Footer text
            Row(
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = Lucide.Globe,
                    contentDescription = null,
                    tint = SettingsColors.textSecondary,
                    modifier = Modifier.size(18.dp).padding(top = 2.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = com.example.ui.i18n.LocalTranslation.current.helpTranslate,
                    color = SettingsColors.textSecondary,
                    fontSize = 12.sp,
                            lineHeight = 20.sp
                )
            }
            
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
fun LanguageToggleItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    isChecked: Boolean,
    onToggle: (Boolean) -> Unit,
    isLast: Boolean,
    dividerColor: Color
) {
        
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggle(!isChecked) }
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = SettingsColors.textSecondary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = SettingsColors.textPrimary)
                Spacer(modifier = Modifier.height(2.dp))
                Text(subtitle, fontSize = 12.sp, lineHeight = 16.sp, color = SettingsColors.textSecondary)
            }
            Switch(
            modifier = Modifier.scale(0.85f),
                checked = isChecked,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = SettingsColors.textPrimary,
                    checkedTrackColor = Color(0xFF3B82F6),
                    uncheckedThumbColor = SettingsColors.textSecondary,
                    uncheckedTrackColor = SettingsColors.divider,
                    uncheckedBorderColor = Color.Transparent
                )
            )
        }
        if (!isLast) {
            HorizontalDivider(color = SettingsColors.divider, thickness = 1.dp, modifier = Modifier.padding(start = 56.dp))
        }
    }
}

@Composable
fun LanguageRadioItem(
    title: String,
    subtitle: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    isLast: Boolean,
    dividerColor: Color
) {
        
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() }
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = SettingsColors.textPrimary)
                Spacer(modifier = Modifier.height(2.dp))
                Text(subtitle, fontSize = 12.sp, lineHeight = 16.sp, color = SettingsColors.textSecondary)
            }
            
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF3B82F6)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Lucide.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                }
            } else {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .border(2.dp, SettingsColors.divider, CircleShape)
                )
            }
        }
        if (!isLast) {
            HorizontalDivider(color = SettingsColors.divider, thickness = 1.dp, modifier = Modifier.padding(start = 16.dp))
        }
    }
}

@Composable
fun LanguageActionItem(
    icon: ImageVector,
    title: String,
    trailingText: String,
    isLast: Boolean,
    dividerColor: Color
) {
        
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { }
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = SettingsColors.textSecondary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = SettingsColors.textPrimary, modifier = Modifier.weight(1f))
            Text(trailingText, fontSize = 15.sp, color = SettingsColors.textSecondary)
        }
        if (!isLast) {
            HorizontalDivider(color = SettingsColors.divider, thickness = 1.dp, modifier = Modifier.padding(start = 56.dp))
        }
    }
}
