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
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.PersonAdd
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
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
import com.example.data.toModel
import com.example.supabase
import com.example.ui.i18n.LocalTranslation
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@kotlinx.serialization.Serializable
private data class FollowingIdRow(val following_id: String)

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
    val scope = rememberCoroutineScope()

    var blockedUsers by remember { mutableStateOf<List<Profile>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var showPicker by remember { mutableStateOf(false) }
    var menuUserId by remember { mutableStateOf<String?>(null) }
    var pendingBlock by remember { mutableStateOf<Profile?>(null) }

    LaunchedEffect(Unit) {
        val myId = supabase.auth.currentSessionOrNull()?.user?.id
        if (myId != null) {
            val blockedIds = BlockManager.getBlockedByMe(myId)
            if (blockedIds.isNotEmpty()) {
                try {
                    val profiles = supabase.postgrest["profiles"].select {
                        filter { isIn("id", blockedIds) }
                    }.decodeList<Profile>()
                    blockedUsers = profiles
                } catch (e: Exception) {
                }
            }
        }
        isLoading = false
    }

    if (showPicker) {
        BlockUserPicker(
            alreadyBlockedIds = blockedUsers.map { it.id }.toSet(),
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
                            .clip(RoundedCornerShape(20.dp))
                            .background(SettingsColors.surface)
                            .clickable { showPicker = true }
                            .padding(horizontal = 18.dp, vertical = 18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.PersonAdd,
                            contentDescription = null,
                            tint = SettingsColors.blueAccent,
                            modifier = Modifier.size(26.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = t.blockUser,
                            color = SettingsColors.blueAccent,
                            fontSize = 17.sp,
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
                } else if (blockedUsers.isEmpty()) {
                    item {
                        Text(
                            text = t.noBlockedUsers,
                            color = SettingsColors.textSecondary,
                            fontSize = 15.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(top = 24.dp)
                        )
                    }
                } else {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(20.dp))
                                .background(SettingsColors.surface)
                        ) {
                            Text(
                                text = String.format(t.blockedUsersCountFmt, blockedUsers.size),
                                color = SettingsColors.blueAccent,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(start = 18.dp, end = 18.dp, top = 16.dp, bottom = 6.dp)
                            )
                            blockedUsers.forEachIndexed { index, profile ->
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
                                                                BlockManager.unblockUser(myId, profile.id)
                                                                blockedUsers = blockedUsers.filter { it.id != profile.id }
                                                            }
                                                        }
                                                    }
                                                )
                                            }
                                        }
                                    }
                                )
                                if (index < blockedUsers.lastIndex) {
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
                            BlockManager.blockUser(myId, profile.id)
                            blockedUsers = listOf(profile) + blockedUsers.filter { it.id != profile.id }
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

    var tab by remember { mutableStateOf(0) }
    var isSearching by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var chatUsers by remember { mutableStateOf<List<Profile>>(emptyList()) }
    var contactUsers by remember { mutableStateOf<List<Profile>>(emptyList()) }
    val focusRequester = remember { FocusRequester() }

    BackHandler(enabled = true) {
        if (isSearching) {
            isSearching = false
            query = ""
        } else {
            onBack()
        }
    }

    LaunchedEffect(isSearching) {
        if (isSearching) {
            try { focusRequester.requestFocus() } catch (e: Exception) {}
        }
    }

    LaunchedEffect(Unit) {
        val myId = supabase.auth.currentSessionOrNull()?.user?.id
        if (myId != null) {
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
                    chatUsers = ids.mapNotNull { id -> profiles.find { it.id == id } }
                }
            } catch (e: Exception) {
            }
            // جهات الاتصال = الأشخاص اللي أتابعهم
            try {
                val ids = supabase.postgrest["followers"]
                    .select(Columns.list("following_id")) {
                        filter { eq("follower_id", myId) }
                    }.decodeList<FollowingIdRow>().map { it.following_id }.filter { it != myId }.distinct()
                if (ids.isNotEmpty()) {
                    contactUsers = supabase.postgrest["profiles"].select {
                        filter { isIn("id", ids) }
                    }.decodeList<Profile>()
                }
            } catch (e: Exception) {
            }
        }
        isLoading = false
    }

    val source = if (tab == 0) chatUsers else contactUsers
    val visible = source.filter { p ->
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
        // الشريط العلوي: رجوع + عنوان/حقل بحث + أيقونة بحث
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
                    .clickable {
                        if (isSearching) {
                            isSearching = false
                            query = ""
                        } else onBack()
                    }
            )
            Spacer(modifier = Modifier.width(14.dp))
            if (isSearching) {
                BasicTextField(
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    textStyle = TextStyle(color = SettingsColors.textPrimary, fontSize = 17.sp),
                    cursorBrush = SolidColor(SettingsColors.blueAccent),
                    modifier = Modifier.weight(1f).focusRequester(focusRequester),
                    decorationBox = { inner ->
                        Box(contentAlignment = Alignment.CenterStart) {
                            if (query.isEmpty()) {
                                Text(t.search, color = SettingsColors.textSecondary, fontSize = 17.sp)
                            }
                            inner()
                        }
                    }
                )
            } else {
                Text(
                    text = t.blockUser,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Medium,
                    color = SettingsColors.textPrimary,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = Icons.Outlined.Search,
                    contentDescription = t.search,
                    tint = SettingsColors.textPrimary,
                    modifier = Modifier
                        .size(26.dp)
                        .clickable { isSearching = true }
                )
            }
        }

        // التبويبات: المحادثات | جهات الاتصال
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(t.chats, t.contactsTab).forEachIndexed { index, label ->
                val selected = tab == index
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(50))
                        .background(if (selected) SettingsColors.blueAccent.copy(alpha = 0.15f) else androidx.compose.ui.graphics.Color.Transparent)
                        .clickable { tab = index }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        color = if (selected) SettingsColors.blueAccent else SettingsColors.textSecondary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
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
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(visible, key = { it.id }) { profile ->
                    BlockUiUserRow(profile = profile, onClick = { onPick(profile) })
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
