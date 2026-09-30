package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.ArrowLeft
import com.composables.icons.lucide.UserPlus
import com.example.data.toModel
import com.example.supabase
import com.example.ui.i18n.LocalTranslation
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@kotlinx.serialization.Serializable
private data class BlockedFollowingIdRow(val following_id: String)

/**
 * شاشة "المستخدمون المحظورون" على طريقة تيليجرام:
 *  - كارد "حظر المستخدم" يفتح قائمة اختيار (محادثات / جهات اتصال + بحث)
 *  - النقر على شخص من القائمة => رسالة تأكيد ثم الحظر
 *  - قائمة المحظورين: النقر على المستخدم يفتح بروفايله، وثلاث نقاط => إلغاء الحظر
 *  - كل النصوص حسب لغة التطبيق المختارة
 * (منطق الحظر/إلغاء الحظر هو نفسه BlockManager الموجود، ما تغيّر شيء فيه)
 */
@Composable
fun BlockedUsersScreen(onBack: () -> Unit, onUserProfileClick: (String) -> Unit = {}) {
    val t = LocalTranslation.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val blockedIds by BlockManager.blockedByMe.collectAsState()
    var blockedProfiles by remember { mutableStateOf<List<Profile>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var showPicker by remember { mutableStateOf(false) }
    var menuUserId by remember { mutableStateOf<String?>(null) }
    var pendingBlock by remember { mutableStateOf<Profile?>(null) }

    LaunchedEffect(blockedIds) {
        if (blockedIds.isNotEmpty()) {
            try {
                val currentIds = blockedProfiles.map { it.id }.toSet()
                val missingIds = blockedIds - currentIds
                val newProfiles = if (missingIds.isNotEmpty()) {
                    supabase.postgrest["profiles"].select {
                        filter { isIn("id", missingIds) }
                    }.decodeList<Profile>()
                } else emptyList()
                blockedProfiles = (blockedProfiles + newProfiles).filter { it.id in blockedIds }
            } catch (e: Exception) {
            }
        } else {
            blockedProfiles = emptyList()
        }
        isLoading = false
    }

    if (showPicker) {
        BlockUserPicker(
            alreadyBlockedIds = blockedIds,
            onBack = { showPicker = false },
            onPick = { pendingBlock = it }
        )
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .background(SettingsColors.background)
        ) {
            BlockUiTopBar(title = t.blockedUsersTitle, onBack = onBack)
            HorizontalDivider(color = SettingsColors.divider, thickness = 1.dp)

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp)
            ) {
                // كارد "حظر المستخدم"
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(SettingsColors.cardRadius))
                            .background(SettingsColors.surface)
                            .clickable { showPicker = true }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Lucide.UserPlus,
                            contentDescription = null,
                            tint = SettingsColors.textPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Text(
                            text = t.blockUser,
                            color = SettingsColors.textPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                item {
                    Text(
                        text = t.blockedUsersInfo,
                        color = SettingsColors.textSecondary,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 12.dp)
                    )
                }

                if (isLoading) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = SettingsColors.blueAccent)
                        }
                    }
                } else if (blockedProfiles.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(SettingsColors.cardRadius))
                                .background(SettingsColors.surface)
                                .padding(vertical = 28.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = t.noBlockedUsers,
                                color = SettingsColors.textSecondary,
                                fontSize = 15.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(SettingsColors.cardRadius))
                                .background(SettingsColors.surface)
                        ) {
                            Text(
                                text = String.format(t.blockedUsersCountFmt, blockedProfiles.size),
                                color = SettingsColors.blueAccent,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(start = 18.dp, end = 18.dp, top = 16.dp, bottom = 6.dp)
                            )
                            blockedProfiles.forEachIndexed { index, profile ->
                                BlockUiUserRow(
                                    profile = profile,
                                    onClick = { onUserProfileClick(profile.id) },
                                    trailing = {
                                        Box {
                                            IconButton(onClick = { menuUserId = profile.id }) {
                                                Icon(
                                                    imageVector = Icons.Filled.MoreVert,
                                                    contentDescription = null,
                                                    tint = SettingsColors.textSecondary
                                                )
                                            }
                                            DropdownMenu(
                                                expanded = menuUserId == profile.id,
                                                onDismissRequest = { menuUserId = null },
                                                modifier = Modifier.background(SettingsColors.surface)
                                            ) {
                                                DropdownMenuItem(
                                                    text = { Text(t.unblockUserAction, color = SettingsColors.textPrimary) },
                                                    onClick = {
                                                        menuUserId = null
                                                        scope.launch {
                                                            val myId = supabase.auth.currentSessionOrNull()?.user?.id
                                                            if (myId != null) {
                                                                val db = com.example.data.DatabaseProvider.getDatabase(context)
                                                                val success = BlockManager.unblock(myId, profile.id, db.chatDao())
                                                                if (!success) {
                                                                    android.widget.Toast.makeText(context, "Error unblocking user", android.widget.Toast.LENGTH_LONG).show()
                                                                }
                                                            }
                                                        }
                                                    }
                                                )
                                            }
                                        }
                                    }
                                )
                                if (index < blockedProfiles.lastIndex) {
                                    HorizontalDivider(
                                        color = SettingsColors.divider,
                                        thickness = 1.dp,
                                        modifier = Modifier.padding(start = 78.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // رسالة التأكيد (تظهر فوق قائمة الاختيار)
    val target = pendingBlock
    if (target != null) {
        AlertDialog(
            onDismissRequest = { pendingBlock = null },
            containerColor = SettingsColors.surface,
            title = { Text(t.blockUser, color = SettingsColors.textPrimary, fontWeight = FontWeight.Medium) },
            text = {
                Text(
                    text = String.format(t.blockUserQuestionFmt, target.displayName),
                    color = SettingsColors.textPrimary,
                    fontSize = 16.sp
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val profile = target
                    pendingBlock = null
                    scope.launch {
                        val myId = supabase.auth.currentSessionOrNull()?.user?.id
                        if (myId != null) {
                            val db = com.example.data.DatabaseProvider.getDatabase(context)
                            BlockManager.block(myId, profile.id, db.chatDao())
                            // Optimistically add to avoid loading flash; LaunchedEffect will reconcile
                            blockedProfiles = listOf(profile) + blockedProfiles.filter { it.id != profile.id }
                        }
                        showPicker = false
                    }
                }) {
                    Text(t.blockUser, color = SettingsColors.redAccent)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingBlock = null }) {
                    Text(t.cancel, color = SettingsColors.blueAccent)
                }
            }
        )
    }
}

@Composable
private fun BlockUserPicker(
    alreadyBlockedIds: Set<String>,
    onBack: () -> Unit,
    onPick: (Profile) -> Unit
) {
    val t = LocalTranslation.current
    val context = LocalContext.current

    var query by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var allUsers by remember { mutableStateOf<List<Profile>>(emptyList()) }

    BackHandler(enabled = true) { onBack() }

    LaunchedEffect(Unit) {
        val myId = supabase.auth.currentSessionOrNull()?.user?.id
        if (myId != null) {
            val merged = LinkedHashMap<String, Profile>()
            // المحادثات (فردية فقط) من الكاش المحلي
            try {
                val db = com.example.data.DatabaseProvider.getDatabase(context)
                val chats = db.chatDao().getAllChats().first()
                val ids = chats
                    .filter { !it.isGroup && !it.isChannel && !it.isNotes }
                    .mapNotNull { it.toModel().participantIds.firstOrNull() }
                    .filter { it != myId }
                    .distinct()
                if (ids.isNotEmpty()) {
                    val profiles = supabase.postgrest["profiles"].select {
                        filter { isIn("id", ids) }
                    }.decodeList<Profile>()
                    profiles.forEach { merged[it.id] = it }
                }
            } catch (e: Exception) {
            }
            // جهات الاتصال = الأشخاص اللي أتابعهم
            try {
                val ids = supabase.postgrest["followers"]
                    .select(Columns.list("following_id")) {
                        filter { eq("follower_id", myId) }
                    }.decodeList<BlockedFollowingIdRow>().map { it.following_id }.filter { it != myId }.distinct()
                if (ids.isNotEmpty()) {
                    val profiles = supabase.postgrest["profiles"].select {
                        filter { isIn("id", ids) }
                    }.decodeList<Profile>()
                    profiles.forEach { merged[it.id] = it }
                }
            } catch (e: Exception) {
            }
            allUsers = merged.values.toList()
        }
        isLoading = false
    }

    val visible = allUsers.filter { p ->
        p.id !in alreadyBlockedIds &&
            (query.isBlank() ||
                p.displayName.contains(query, ignoreCase = true) ||
                (p.username ?: "").contains(query, ignoreCase = true))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .background(SettingsColors.background)
    ) {
        // الشريط العلوي: رجوع + عنوان — على طراز تيليجرام
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Lucide.ArrowLeft,
                contentDescription = "Back",
                tint = SettingsColors.textPrimary,
                modifier = Modifier
                    .size(24.dp)
                    .clickable { onBack() }
            )
            Spacer(modifier = Modifier.width(14.dp))
            Text(
                text = t.blockUser,
                fontSize = 17.sp,
                fontWeight = FontWeight.Medium,
                color = SettingsColors.textPrimary
            )
        }

        // كارد بحث دائم — بنفس تصميم شاشة البحث فالتطبيق (بدل تبويبات Chats/Contacts)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            androidx.compose.material3.Surface(
                shape = RoundedCornerShape(12.dp),
                color = SettingsColors.divider,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = t.search,
                        tint = SettingsColors.textSecondary,
                        modifier = Modifier.size(19.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    BasicTextField(
                        value = query,
                        onValueChange = { query = it },
                        singleLine = true,
                        textStyle = TextStyle(color = SettingsColors.textPrimary, fontSize = 16.sp),
                        cursorBrush = SolidColor(SettingsColors.blueAccent),
                        modifier = Modifier.weight(1f),
                        decorationBox = { inner ->
                            Box(contentAlignment = Alignment.CenterStart) {
                                if (query.isEmpty()) {
                                    Text(t.search, color = SettingsColors.textSecondary, fontSize = 16.sp)
                                }
                                inner()
                            }
                        }
                    )
                    if (query.isNotEmpty()) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Clear",
                            tint = SettingsColors.textSecondary,
                            modifier = Modifier
                                .size(16.dp)
                                .clickable { query = "" }
                        )
                    }
                }
            }
        }
        HorizontalDivider(color = SettingsColors.divider, thickness = 1.dp, modifier = Modifier.padding(top = 4.dp))

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = SettingsColors.blueAccent)
            }
        } else if (visible.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(t.noUsersFound, color = SettingsColors.textSecondary, fontSize = 15.sp)
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(SettingsColors.cardRadius))
                    .background(SettingsColors.surface)
            ) {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(visible, key = { it.id }) { profile ->
                        BlockUiUserRow(profile = profile, onClick = { onPick(profile) })
                    }
                }
            }
        }
    }
}

@Composable
private fun BlockUiTopBar(title: String, onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Lucide.ArrowLeft,
            contentDescription = "Back",
            tint = SettingsColors.textPrimary,
            modifier = Modifier
                .size(24.dp)
                .clickable { onBack() }
        )
        Spacer(modifier = Modifier.width(14.dp))
        Text(
            text = title,
            fontSize = 17.sp,
            fontWeight = FontWeight.Medium,
            color = SettingsColors.textPrimary
        )
    }
}

@Composable
private fun BlockUiUserRow(
    profile: Profile,
    onClick: () -> Unit,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(start = 16.dp, end = if (trailing != null) 4.dp else 16.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val avatarUrl = profile.avatarUrl
        if (!avatarUrl.isNullOrBlank()) {
            AsyncImage(
                model = avatarUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(50.dp).clip(CircleShape).background(SettingsColors.divider)
            )
        } else {
            Box(
                modifier = Modifier.size(50.dp).clip(CircleShape).background(profile.avatarColor),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = profile.displayName.take(1).uppercase(),
                    color = androidx.compose.ui.graphics.Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = profile.displayName,
                color = SettingsColors.textPrimary,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            val uname = profile.username
            if (!uname.isNullOrBlank()) {
                Text(
                    text = "@$uname",
                    color = SettingsColors.textSecondary,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (trailing != null) trailing()
    }
}
