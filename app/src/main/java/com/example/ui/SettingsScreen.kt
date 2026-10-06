package com.example.ui

import com.example.ui.SettingsColors

import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import com.example.ui.i18n.LocalTranslation
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.supabase
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest

// ---- Cryptvora "dark" theme + "violet" accent, mirrored 1:1 from the web app's themes.ts + styles.css ----

// True web border color: oklch(1 0 0 / 0.07) = white at 7% opacity — a near-invisible hairline
// True web --radius-2xl value (defined in styles.css), used for every "rounded-2xl" card/group

data class SItem(val icon: ImageVector, val title: String, val subtitle: String, val tintColor: Color, val onClick: () -> Unit = {})

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onEditProfileClick: () -> Unit = {},
    onAccountClick: () -> Unit = {},
    onPrivacyClick: () -> Unit = {},
    onHelpClick: () -> Unit = {},
    onAboutClick: () -> Unit = {},
    onLanguageClick: () -> Unit = {},
    onLinkedDevicesClick: () -> Unit = {},
    onChatWallpapersClick: () -> Unit = {},
    onStorageClick: () -> Unit = {},
    onSecurityClick: () -> Unit = {},
    onNotificationsClick: () -> Unit = {},
    onPowerUsageClick: () -> Unit = {},
    onAppearanceClick: () -> Unit = {},
    onSocialLinksClick: () -> Unit = {},
    onOwlinoFeatherClick: () -> Unit = {},
    onOwlinoPlusClick: () -> Unit = {}
) {
    
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    var isLoggingOut by remember { mutableStateOf(false) }
    var showLogoutConfirm by remember { mutableStateOf(false) }
    var networkErrorMsg by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(networkErrorMsg) {
        if (networkErrorMsg != null) {
            android.widget.Toast.makeText(context, networkErrorMsg, android.widget.Toast.LENGTH_SHORT).show()
            kotlinx.coroutines.delay(3000)
            networkErrorMsg = null
        }
    }
    val cachePrefs = remember { context.getSharedPreferences("user_profile_prefs", android.content.Context.MODE_PRIVATE) }
    val cachedUid = remember { supabase.auth.currentUserOrNull()?.id ?: "" }
    var realName by remember { mutableStateOf(cachePrefs.getString("cache_name_$cachedUid", null) ?: "Loading...") }
    var username by remember { mutableStateOf(cachePrefs.getString("cache_username_$cachedUid", null) ?: "...") }
    var avatarUrl by remember { mutableStateOf<String?>(cachePrefs.getString("local_avatar_$cachedUid", null) ?: cachePrefs.getString("cache_avatar_$cachedUid", null)) }
    var isVerified by remember { mutableStateOf(cachePrefs.getBoolean("cache_verified_$cachedUid", false)) }
    var searchQuery by remember { mutableStateOf("") }
    var activeSessionCount by remember { mutableStateOf(cachePrefs.getInt("cache_sessions_$cachedUid", 1)) }
    val prefs = remember { context.getSharedPreferences("user_profile_prefs", android.content.Context.MODE_PRIVATE) }

    val q = searchQuery.trim().lowercase()
    val matches: (String, String) -> Boolean = { t, s ->
        q.isEmpty() || t.lowercase().contains(q) || s.lowercase().contains(q)
    }

    LaunchedEffect(Unit) {
        val userId = supabase.auth.currentUserOrNull()?.id
        if (userId != null) {
            try {
                val currentDeviceName = "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}"
                val sessions = supabase.postgrest["login_sessions"]
                    .select {
                        filter { eq("user_id", userId) }
                        order("created_at", io.github.jan.supabase.postgrest.query.Order.DESCENDING)
                    }.decodeList<DeviceSessionModel>()

                val active = sessions.groupBy { it.device_name }
                    .map { (_, list) -> list.first() }
                    .count { it.event_type == "login" }

                // ضمان احتساب الجهاز الحالي حتى لو ما كانش عندو سجل دخول
                val hasCurrent = sessions.any { it.device_name == currentDeviceName && it.event_type == "login" }
                activeSessionCount = if (hasCurrent) active else active + 1
                cachePrefs.edit().putInt("cache_sessions_$userId", activeSessionCount).apply()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    LaunchedEffect(Unit) {
        val currentUser = supabase.auth.currentUserOrNull()
        if (currentUser != null) {
            val emailPrefix = currentUser.email?.substringBefore("@") ?: "user"
            if (realName == "Loading...") realName = emailPrefix
            if (username == "...") username = emailPrefix

            try {
                val profile = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { supabase.postgrest["profiles"]
                    .select { filter { eq("id", currentUser.id) } }
                    .decodeSingleOrNull<kotlinx.serialization.json.JsonObject>() }

                if (profile != null) {
                    val pName = profile["full_name"]?.takeIf { it !is kotlinx.serialization.json.JsonNull }?.toString()?.removeSurrounding("\"")
                    if (!pName.isNullOrBlank()) realName = pName

                    val pUser = profile["username"]?.takeIf { it !is kotlinx.serialization.json.JsonNull }?.toString()?.removeSurrounding("\"")
                    if (!pUser.isNullOrBlank()) username = pUser

                    val pAvatar = profile["avatar_url"]?.takeIf { it !is kotlinx.serialization.json.JsonNull }?.toString()?.removeSurrounding("\"")

                    val localAvatar = prefs.getString("local_avatar_${currentUser.id}", null)

                    if (localAvatar != null) {
                        avatarUrl = localAvatar
                    } else if (!pAvatar.isNullOrBlank()) {
                        avatarUrl = pAvatar
                    }

                    val pBadge = profile["is_verified"]?.takeIf { it !is kotlinx.serialization.json.JsonNull }?.toString()?.toBooleanStrictOrNull()
                    if (pBadge == true) {
                        isVerified = true
                    }
                    cachePrefs.edit()
                        .putString("cache_name_${currentUser.id}", realName)
                        .putString("cache_username_${currentUser.id}", username)
                        .putString("cache_avatar_${currentUser.id}", pAvatar)
                        .putBoolean("cache_verified_${currentUser.id}", pBadge == true)
                        .apply()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .background(SettingsColors.background)
    ) {
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
                text = com.example.ui.i18n.LocalTranslation.current.settings,
                fontSize = 17.sp,
                fontWeight = FontWeight.Medium,
                color = SettingsColors.textPrimary
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 12.dp)
        ) {
            // Profile Card — corner radius now matches CSS --radius-2xl (28px)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(SettingsColors.cardRadius))
                    .background(SettingsColors.surface)
                    .clickable { onEditProfileClick() }
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(SettingsColors.surface),
                            contentAlignment = Alignment.Center
                        ) {
                            if (!avatarUrl.isNullOrBlank()) {
                                AsyncImage(
                                    model = avatarUrl,
                                    contentDescription = "Profile Picture",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Text(
                                    text = realName.take(2).uppercase(),
                                    color = SettingsColors.textPrimary,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                realName, 
                                fontSize = 17.sp, 
                                fontWeight = FontWeight.SemiBold, 
                                color = SettingsColors.textPrimary,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            if (isVerified) {
                                Spacer(modifier = Modifier.width(4.dp))
                                com.example.ui.VerifiedBadge(isVerified = true, iconSize = 16.dp)
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text("@$username", fontSize = 12.sp, lineHeight = 16.sp, color = SettingsColors.textSecondary)
                    }

                    Column(
                        verticalArrangement = Arrangement.SpaceBetween,
                        horizontalAlignment = Alignment.End,
                        modifier = Modifier.height(64.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(SettingsColors.surface)
                                .clickable { onEditProfileClick() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Lucide.Pencil, contentDescription = com.example.ui.i18n.LocalTranslation.current.edit, tint = SettingsColors.textPrimary, modifier = Modifier.size(15.dp))
                        }
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(SettingsColors.surface)
                                .clickable { },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Lucide.QrCode, contentDescription = "QR Code", tint = SettingsColors.textPrimary, modifier = Modifier.size(15.dp))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .clip(RoundedCornerShape(23.dp))
                    .background(SettingsColors.divider)
                    .clickable { }
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Lucide.Search, contentDescription = "Search", tint = SettingsColors.textSecondary, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    androidx.compose.foundation.text.BasicTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        textStyle = androidx.compose.ui.text.TextStyle(color = SettingsColors.textPrimary, fontSize = 14.sp, lineHeight = 18.sp),
                        singleLine = true,
                        cursorBrush = androidx.compose.ui.graphics.SolidColor(SettingsColors.blueAccent),
                        modifier = Modifier.weight(1f),
                        decorationBox = { innerTextField ->
                            if (searchQuery.isEmpty()) {
                                Text(com.example.ui.i18n.LocalTranslation.current.searchSettings, color = SettingsColors.textSecondary, fontSize = 14.sp, lineHeight = 18.sp)
                            }
                            innerTextField()
                        }
                    )
                    if (searchQuery.isNotEmpty()) {
                        Icon(
                            imageVector = Lucide.X,
                            contentDescription = "Clear",
                            tint = SettingsColors.textSecondary,
                            modifier = Modifier
                                .size(16.dp)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) { searchQuery = "" }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            val allSections = listOf(
                com.example.ui.i18n.LocalTranslation.current.personal to listOf(
                    SItem(Lucide.User, com.example.ui.i18n.LocalTranslation.current.account, com.example.ui.i18n.LocalTranslation.current.googleAccountUsername, SettingsColors.textSecondary, onClick = onAccountClick),
                    SItem(Lucide.Shield, com.example.ui.i18n.LocalTranslation.current.privacy, com.example.ui.i18n.LocalTranslation.current.whoCanSeeActivity, SettingsColors.textSecondary, onClick = onPrivacyClick),
                    SItem(Lucide.Lock, com.example.ui.i18n.LocalTranslation.current.security, com.example.ui.i18n.LocalTranslation.current.appPasswordPinBiometrics, SettingsColors.greenAccent, onClick = onSecurityClick),
                    SItem(Lucide.Link2, com.example.ui.i18n.LocalTranslation.current.socialLinks, com.example.ui.i18n.LocalTranslation.current.telegramXDiscord, SettingsColors.textSecondary, onClick = onSocialLinksClick)
                ),
                com.example.ui.i18n.LocalTranslation.current.experience to listOf(
                    SItem(Lucide.Bell, com.example.ui.i18n.LocalTranslation.current.notifications, com.example.ui.i18n.LocalTranslation.current.alertsSoundsMentions, SettingsColors.textSecondary, onClick = onNotificationsClick),
                    SItem(Lucide.MessageSquare, com.example.ui.i18n.LocalTranslation.current.chats, com.example.ui.i18n.LocalTranslation.current.autoDownloadComposer, SettingsColors.textSecondary, onClick = onChatWallpapersClick),
                    SItem(Lucide.Palette, com.example.ui.i18n.LocalTranslation.current.appearance, com.example.ui.i18n.LocalTranslation.current.changeColorsUiThemes, SettingsColors.textSecondary, onClick = onAppearanceClick),
                    SItem(Lucide.Database, com.example.ui.i18n.LocalTranslation.current.storageAndData, com.example.ui.i18n.LocalTranslation.current.storageUsage, SettingsColors.textSecondary, onClick = onStorageClick),
                    SItem(Lucide.BatteryCharging, com.example.ui.i18n.LocalTranslation.current.powerUsage, com.example.ui.i18n.LocalTranslation.current.batteryAnimations, SettingsColors.textSecondary, onClick = onPowerUsageClick)
                ),
                com.example.ui.i18n.LocalTranslation.current.system to listOf(
                    SItem(Lucide.Smartphone, com.example.ui.i18n.LocalTranslation.current.linkedDevices, com.example.ui.i18n.LocalTranslation.current.threeActiveSessions.replaceFirst("3", activeSessionCount.toString()), SettingsColors.textSecondary, onClick = onLinkedDevicesClick),
                    SItem(Lucide.Globe, com.example.ui.i18n.LocalTranslation.current.language, com.example.ui.i18n.LocalTranslation.current.interfaceAndRegion, SettingsColors.textSecondary, onClick = onLanguageClick)
                ),
                com.example.ui.i18n.LocalTranslation.current.support to listOf(
                    SItem(Lucide.CircleHelp, com.example.ui.i18n.LocalTranslation.current.helpAndSupport, com.example.ui.i18n.LocalTranslation.current.faqContactUs, SettingsColors.textSecondary, onClick = onHelpClick)
                )
            )

            if (searchQuery.isNotEmpty()) {
                val flatItems = allSections.flatMap { it.second }.filter { matches(it.title, it.subtitle) }
                if (flatItems.isNotEmpty()) {
                    Text(com.example.ui.i18n.LocalTranslation.current.suggestions, color = SettingsColors.textSecondary, fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(start = 8.dp, bottom = 8.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(SettingsColors.cardRadius))
                            .background(SettingsColors.surface)
                    ) {
                        flatItems.forEachIndexed { index, item ->
                            SettingsItem(
                                icon = item.icon,
                                title = item.title,
                                subtitle = item.subtitle,
                                iconColor = item.tintColor,
                                titleColor = SettingsColors.textPrimary,
                                subtitleColor = SettingsColors.textSecondary,
                                dividerColor = SettingsColors.divider,
                                isLast = index == flatItems.size - 1,
                                onClick = {
                                    searchQuery = ""
                                    item.onClick()
                                }
                            )
                        }
                    }
                } else {
                    Box(modifier = Modifier.fillMaxWidth().padding(top = 32.dp), contentAlignment = Alignment.Center) {
                        Text(com.example.ui.i18n.LocalTranslation.current.noSuggestionsFound, color = SettingsColors.textSecondary, fontSize = 15.sp)
                    }
                }
            } else {
                allSections.forEach { (sectionTitle, items) ->
                    if (items.isNotEmpty()) {
                        SettingsSection(sectionTitle, SettingsColors.textSecondary, SettingsColors.surface) {
                            items.forEachIndexed { index, item ->
                                SettingsItem(
                                    icon = item.icon,
                                    title = item.title,
                                    subtitle = item.subtitle,
                                    iconColor = item.tintColor,
                                    titleColor = SettingsColors.textPrimary,
                                    subtitleColor = SettingsColors.textSecondary,
                                    dividerColor = SettingsColors.divider,
                                    isLast = index == items.size - 1,
                                    onClick = item.onClick
                                )
                            }
                        }
                    }
                }

            // Owlino Feather + Owlino Plus cards (UI only) — juste au-dessus de « تسجيل الخروج »
            val rdExtra = com.example.ui.i18n.rememberExtraStrings()
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp)
                    .clip(RoundedCornerShape(SettingsColors.cardRadius))
                    .background(SettingsColors.surface)
            ) {
                SettingsItem(
                    icon = Lucide.Feather,
                    title = rdExtra.owlinoFeatherTitle,
                    subtitle = rdExtra.owlinoFeatherSub,
                    iconColor = Color(0xFF19BD6B),
                    titleColor = SettingsColors.textPrimary,
                    subtitleColor = SettingsColors.textSecondary,
                    dividerColor = SettingsColors.divider,
                    isLast = true,
                    onClick = onOwlinoFeatherClick
                )
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp)
                    .clip(RoundedCornerShape(SettingsColors.cardRadius))
                    .background(SettingsColors.surface)
            ) {
                SettingsItem(
                    icon = Lucide.Crown,
                    title = rdExtra.owlinoPlusTitle,
                    subtitle = rdExtra.owlinoPlusSub,
                    iconColor = Color(0xFF7B5CF0),
                    titleColor = SettingsColors.textPrimary,
                    subtitleColor = SettingsColors.textSecondary,
                    dividerColor = SettingsColors.divider,
                    isLast = true,
                    onClick = onOwlinoPlusClick
                )
            }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 20.dp)
                        .clip(RoundedCornerShape(SettingsColors.cardRadius))
                        .background(SettingsColors.surface)
                ) {
                    SettingsItem(
                        icon = Lucide.LogOut,
                        title = com.example.ui.i18n.LocalTranslation.current.signOut,
                        subtitle = "",
                        iconColor = SettingsColors.redAccent,
                        titleColor = SettingsColors.redAccent,
                        subtitleColor = Color.Transparent,
                        dividerColor = SettingsColors.divider,
                        isLast = true,
                        isDanger = true,
                        showArrow = false,
                        onClick = {
                            if (isLoggingOut) return@SettingsItem
                            showLogoutConfirm = true
                        }
                    )
                }
            }

            if (showLogoutConfirm) {
                androidx.compose.material3.AlertDialog(
                    onDismissRequest = { showLogoutConfirm = false },
                    title = { Text("تسجيل الخروج") },
                    text = { Text("هل تريد فعلاً تسجيل الخروج من حسابك؟") },
                    confirmButton = {
                        androidx.compose.material3.TextButton(onClick = {
                            showLogoutConfirm = false
                            isLoggingOut = true
                            scope.launch {
                                try {
                                    val userId = supabase.auth.currentUserOrNull()?.id
                                    if (userId != null) {
                                        val locStr = com.example.util.SessionLogger.fetchLocation()
                                        com.example.util.SessionLogger.logSession(userId, "logout", locStr)
                                        try {
                                            // 1. Clear FCM token from database
                                            com.example.supabase.postgrest["profiles"]
                                                .update(mapOf("fcm_token" to null)) {
                                                    filter { eq("id", userId) }
                                                }
                                            // 2. Delete token locally to force a new one for the next login
                                            com.google.firebase.messaging.FirebaseMessaging.getInstance().deleteToken().await()
                                        } catch (e: Exception) {
                                        }
                                    }
                                    
                                    // 3. Clear all local databases and caches synchronously
                                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                        // Clear Main Database (chats, profiles)
                                        com.example.data.DatabaseProvider.getDatabase(context).clearAllTables()
                                        // Clear Cache Database (cached_chats, cached_messages)
                                        com.example.cache.AppDatabase.getDatabase(context).clearAllTables()
                                        
                                        // Clear User SharedPreferences
                                        context.getSharedPreferences("user_profile_prefs", android.content.Context.MODE_PRIVATE).edit().clear().apply()
                                        context.getSharedPreferences("app_messages_cache", android.content.Context.MODE_PRIVATE).edit().clear().apply()
                                        context.getSharedPreferences("chat_scroll_prefs", android.content.Context.MODE_PRIVATE).edit().clear().apply()
                                        context.getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE).edit().clear().apply()
                                    }
                                    
                                    // 4. Clear Global In-Memory State
                                    com.example.ui.GlobalAppState.roomChats = null
                                    com.example.ui.GlobalAppState.cachedChats = emptyList()
                                    com.example.ui.GlobalAppState.liveMyProfile = null
                                    com.example.ui.GlobalAppState.hasCompletedInitialChatSync = false
                                    // إصلاح أمني مهم: نمسح كل كاش الرسائل بالذاكرة (كان ينسى يتمسح،
                                    // وهذا كان يخلي رسائل الحساب القديم قابلة للقراءة بعد تبديل الحساب)
                                    com.example.AppState.chatMessagesCache.clear()

                                    supabase.auth.signOut()

                                    // إصلاح أمني جذري: نعيد تشغيل التطبيق بالكامل (يقتل العملية كاملة)
                                    // بدل ما نتنقل بس لشاشة الدخول داخل نفس الجلسة. هذا يضمن انقطاع أي
                                    // عملية شبكة معلقة من الحساب القديم، ومسح كل شيء بالذاكرة بلا استثناء —
                                    // وهذا هو الحل الوحيد المضمون 100% لمنع اختلاط بيانات الحسابات.
                                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                        val packageManager = context.packageManager
                                        val restartIntent = packageManager.getLaunchIntentForPackage(context.packageName)
                                        val componentName = restartIntent?.component
                                        val mainIntent = android.content.Intent.makeRestartActivityTask(componentName)
                                        context.startActivity(mainIntent)
                                        // مهم: نستنى شوية قبل ما نقتل العملية، باش نظام أندرويد
                                        // يكمل بدء تشغيل الشاشة الجديدة أول (هذا كان سبب رجوعك
                                        // للشاشة الرئيسية بدل الخروج فعلياً)
                                        kotlinx.coroutines.delay(300)
                                        Runtime.getRuntime().exit(0)
                                    }
                                } catch (e: Throwable) {
                                } finally {
                                    isLoggingOut = false
                                }
                            }
                        }) {
                            Text("تسجيل الخروج", color = SettingsColors.redAccent)
                        }
                    },
                    dismissButton = {
                        androidx.compose.material3.TextButton(onClick = { showLogoutConfirm = false }) {
                            Text("إلغاء")
                        }
                    }
                )
            }

            Text(
                text = "Cryptvora · Built for traders",
                color = SettingsColors.textSecondary,
                fontSize = 11.5.sp,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun SettingsSection(title: String, titleColor: Color, surfaceColor: Color, content: @Composable () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp)) {
        Text(
            text = title,
            color = titleColor,
            fontSize = 12.sp, lineHeight = 16.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(SettingsColors.cardRadius))
                .background(SettingsColors.surface)
        ) {
            content()
        }
    }
}

@Composable
fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    iconColor: Color,
    titleColor: Color,
    subtitleColor: Color,
    dividerColor: Color,
    isLast: Boolean = false,
    isDanger: Boolean = false,
    showArrow: Boolean = true,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isDanger) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(iconColor.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(20.dp)
                )
            }
        } else {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = titleColor)
            if (subtitle.isNotEmpty()) {
                Spacer(modifier = Modifier.height(3.dp))
                Text(subtitle, fontSize = 12.sp, lineHeight = 16.sp, color = subtitleColor)
            }
        }
        if (showArrow) {
            Icon(
                imageVector = Lucide.ChevronRight,
                contentDescription = null,
                tint = subtitleColor.copy(alpha = 0.6f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
    // True web hairline: white at 7% opacity, matching --border in styles.css exactly
    if (!isLast) {
        HorizontalDivider(
            color = SettingsColors.divider,
            thickness = 1.dp,
            modifier = Modifier.padding(start = 54.dp)
        )
    }
}
