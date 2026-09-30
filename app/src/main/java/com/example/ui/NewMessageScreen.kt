package com.example.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.supabase
import com.example.ui.i18n.LocalTranslation
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Hash
import com.composables.icons.lucide.Users
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.launch

private enum class NewMessageMode { BROWSE, SELECT_GROUP, SELECT_CHANNEL, GROUP_DETAILS, CHANNEL_DETAILS }

@Composable
fun NewMessageScreen(
    onBack: () -> Unit,
    onChatOpen: (String, String, Boolean) -> Unit
) {
    val t = LocalTranslation.current
    val __themeConfig = com.example.ui.LocalSettingsTheme.current
    val __theme = __themeConfig.theme
    val __bgColor = __theme.bgColor
    val __textPrimary = __theme.textPrimary
    val __textSecondary = __theme.textSecondary
    val __dividerColor = __theme.dividerColor
    val __accent = __themeConfig.accent

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val currentUserId = supabase.auth.currentUserOrNull()?.id

    var mode by remember { mutableStateOf(NewMessageMode.BROWSE) }
    var searchQuery by remember { mutableStateOf("") }
    var contacts by remember { mutableStateOf<List<Profile>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedIds by remember { mutableStateOf(setOf<String>()) }
    var isBusy by remember { mutableStateOf(false) }
    var groupName by remember { mutableStateOf("") }
    var avatarBytes by remember { mutableStateOf<ByteArray?>(null) }
    var avatarPreviewBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var errorText by remember { mutableStateOf<String?>(null) }
    var showGallerySheet by remember { mutableStateOf(false) }
    var cropUri by remember { mutableStateOf<android.net.Uri?>(null) }
    val listState = rememberLazyListState()

    LaunchedEffect(Unit) {
        try {
            val res = supabase.postgrest["profiles"].select {
                filter {
                    filterNot("id", io.github.jan.supabase.postgrest.query.filter.FilterOperator.EQ, "00000000-0000-0000-0000-000000000000")
                    if (currentUserId != null) neq("id", currentUserId)
                }
                order("full_name", io.github.jan.supabase.postgrest.query.Order.ASCENDING)
                limit(500)
            }.decodeList<Profile>()
            contacts = res
        } catch (e: Exception) {
            e.printStackTrace()
        }
        isLoading = false
    }

    val filteredContacts = remember(contacts, searchQuery) {
        val base = if (searchQuery.isBlank()) contacts else contacts.filter {
            it.displayName.contains(searchQuery, ignoreCase = true) || it.displayUsername.contains(searchQuery, ignoreCase = true)
        }
        base.sortedBy { it.displayName.lowercase() }
    }

    val grouped = remember(filteredContacts) {
        filteredContacts.groupBy { it.displayName.take(1).uppercase() }
    }

    val selectedProfiles = remember(contacts, selectedIds) {
        contacts.filter { it.id in selectedIds }
    }

    fun goBack() {
        mode = when (mode) {
            NewMessageMode.GROUP_DETAILS -> NewMessageMode.SELECT_GROUP
            NewMessageMode.CHANNEL_DETAILS -> NewMessageMode.SELECT_CHANNEL
            NewMessageMode.SELECT_GROUP, NewMessageMode.SELECT_CHANNEL -> {
                selectedIds = emptySet()
                NewMessageMode.BROWSE
            }
            NewMessageMode.BROWSE -> {
                onBack()
                NewMessageMode.BROWSE
            }
        }
    }

    fun startDirectChat(userId: String, name: String) {
        val myId = currentUserId
        if (myId == null || isBusy) return
        isBusy = true
        errorText = null
        coroutineScope.launch {
            try {
                val myChats = supabase.postgrest["chat_members"].select(Columns.list("chat_id")) {
                    filter { eq("user_id", myId) }
                }.decodeList<ChatMemberRow>().map { it.chat_id }

                var foundChatId: String? = null
                if (myChats.isNotEmpty()) {
                    val otherChats = supabase.postgrest["chat_members"].select(Columns.list("chat_id")) {
                        filter { eq("user_id", userId); isIn("chat_id", myChats) }
                    }.decodeList<ChatMemberRow>().map { it.chat_id }
                    if (otherChats.isNotEmpty()) {
                        val directChats = supabase.postgrest["chats"].select(Columns.list("id")) {
                            filter { isIn("id", otherChats); eq("type", "direct") }
                        }.decodeList<ChatRow>()
                        if (directChats.isNotEmpty()) foundChatId = directChats.first().id
                    }
                }

                if (foundChatId != null) {
                    onChatOpen(foundChatId, name, false)
                } else {
                    val newChat = supabase.postgrest["chats"].insert(ChatInsert(type = "direct", created_by = myId)) {
                        select()
                    }.decodeSingle<ChatRow>()
                    supabase.postgrest["chat_members"].insert(listOf(
                        ChatMemberRow(chat_id = newChat.id, user_id = myId, role = "admin"),
                        ChatMemberRow(chat_id = newChat.id, user_id = userId, role = "member")
                    ))
                    onChatOpen(newChat.id, name, false)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                errorText = "${t.errorGeneric}: ${e.message ?: e.javaClass.simpleName}"
            } finally {
                isBusy = false
            }
        }
    }

    fun createGroupOrChannel() {
        val myId = currentUserId
        if (myId == null || isBusy) return
        val isChannel = mode == NewMessageMode.CHANNEL_DETAILS
        val fallbackName = if (isChannel) t.newChannelTitle else t.newGroupTitle
        val finalName = groupName.trim().ifBlank { fallbackName }
        isBusy = true
        errorText = null
        coroutineScope.launch {
            try {
                var uploadedAvatarUrl: String? = null
                val bytes = avatarBytes
                if (bytes != null) {
                    try {
                        val filePath = "${myId}/group-${java.util.UUID.randomUUID()}.jpg"
                        supabase.storage["avatars"].upload(filePath, bytes)
                        uploadedAvatarUrl = supabase.storage["avatars"].publicUrl(filePath)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                val chatType = if (isChannel) "channel" else "group"
                val newChat = try {
                    supabase.postgrest["chats"].insert(
                        ChatInsert(type = chatType, created_by = myId, title = finalName, avatar_url = uploadedAvatarUrl)
                    ) { select() }.decodeSingle<ChatRow>()
                } catch (e: Exception) {
                    // fallback: قاعدة البيانات وقتها ماعندهاش أعمدة title/avatar_url
                    e.printStackTrace()
                    supabase.postgrest["chats"].insert(
                        ChatInsert(type = chatType, created_by = myId)
                    ) { select() }.decodeSingle<ChatRow>()
                }

                val members = mutableListOf(ChatMemberRow(chat_id = newChat.id, user_id = myId, role = "admin"))
                members += selectedIds.map { ChatMemberRow(chat_id = newChat.id, user_id = it, role = "member") }
                supabase.postgrest["chat_members"].insert(members)

                onChatOpen(newChat.id, finalName, isChannel)
            } catch (e: Exception) {
                e.printStackTrace()
                errorText = "${t.errorGeneric}: ${e.message ?: e.javaClass.simpleName}"
            } finally {
                isBusy = false
            }
        }
    }

    val pendingCropUri = cropUri
    if (pendingCropUri != null) {
        CircularAvatarCropScreen(
            imageUri = pendingCropUri,
            onCancel = { cropUri = null },
            onConfirm = { bytes ->
                avatarBytes = bytes
                avatarPreviewBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                cropUri = null
            }
        )
        return
    }

    Scaffold(
        containerColor = __bgColor,
        contentWindowInsets = WindowInsets.systemBars,
        floatingActionButtonPosition = FabPosition.End,
        floatingActionButton = {
            when (mode) {
                NewMessageMode.SELECT_GROUP, NewMessageMode.SELECT_CHANNEL -> {
                    if (selectedIds.isNotEmpty()) {
                        Surface(
                            shape = CircleShape,
                            color = __accent,
                            shadowElevation = 6.dp,
                            modifier = Modifier.size(56.dp).clickable {
                                mode = if (mode == NewMessageMode.SELECT_GROUP) NewMessageMode.GROUP_DETAILS else NewMessageMode.CHANNEL_DETAILS
                            }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(28.dp).graphicsLayer { rotationZ = 180f }
                                )
                            }
                        }
                    }
                }
                NewMessageMode.GROUP_DETAILS, NewMessageMode.CHANNEL_DETAILS -> {
                    Surface(
                        shape = CircleShape,
                        color = __accent,
                        shadowElevation = 6.dp,
                        modifier = Modifier.size(56.dp).clickable(enabled = !isBusy) { createGroupOrChannel() }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (isBusy) {
                                CircularProgressIndicator(modifier = Modifier.size(22.dp), color = Color.White, strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                            }
                        }
                    }
                }
                else -> {}
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            // شريط علوي: رجوع + عنوان
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                    contentDescription = t.back,
                    tint = __textSecondary,
                    modifier = Modifier
                        .size(24.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { goBack() }
                        .graphicsLayer { rotationZ = 180f }
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = when (mode) {
                        NewMessageMode.BROWSE -> t.newMessage
                        NewMessageMode.SELECT_GROUP -> t.groupMembers
                        NewMessageMode.SELECT_CHANNEL -> t.channelMembers
                        NewMessageMode.GROUP_DETAILS -> t.newGroupTitle
                        NewMessageMode.CHANNEL_DETAILS -> t.newChannelTitle
                    },
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = __textPrimary
                )
            }

            if (mode == NewMessageMode.GROUP_DETAILS || mode == NewMessageMode.CHANNEL_DETAILS) {
                if (errorText != null) {
                    Text(
                        text = errorText ?: "",
                        color = Color(0xFFE53935),
                        fontSize = 13.sp,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }
                GroupDetailsContent(
                    groupName = groupName,
                    onGroupNameChange = { groupName = it },
                    namePlaceholder = if (mode == NewMessageMode.GROUP_DETAILS) t.groupNamePlaceholder else t.channelNamePlaceholder,
                    avatarPreviewBitmap = avatarPreviewBitmap,
                    onPickAvatar = { showGallerySheet = true },
                    members = selectedProfiles,
                    t = t,
                    textColor = __textPrimary,
                    secondaryColor = __textSecondary,
                    dividerColor = __dividerColor,
                    accent = __accent,
                    bgColor = __bgColor
                )
            } else {
                // شريط البحث
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = __dividerColor,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(40.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 10.dp)) {
                        Icon(Icons.Outlined.Search, contentDescription = null, tint = __textSecondary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(modifier = Modifier.weight(1f)) {
                            if (searchQuery.isEmpty()) {
                                Text(
                                    text = if (mode == NewMessageMode.BROWSE) t.searchContacts else t.whoToAdd,
                                    fontSize = 15.sp,
                                    color = __textSecondary
                                )
                            }
                            BasicTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                textStyle = TextStyle(fontSize = 15.sp, color = __textPrimary),
                                cursorBrush = SolidColor(__accent),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Box(modifier = Modifier.weight(1f)) {
                    LazyColumn(state = listState, modifier = Modifier.fillMaxSize().padding(end = 14.dp)) {
                        if (mode == NewMessageMode.BROWSE && searchQuery.isBlank()) {
                            item {
                                Column {
                                    NewMessagePinnedRow(
                                        title = t.newGroup,
                                        icon = Lucide.Users,
                                        bg = Color(0xFF2196F3),
                                        textColor = __textPrimary,
                                        dividerColor = __dividerColor
                                    ) { mode = NewMessageMode.SELECT_GROUP }
                                    NewMessagePinnedRow(
                                        title = t.newChannel,
                                        icon = Lucide.Hash,
                                        bg = Color(0xFF4CAF50),
                                        textColor = __textPrimary,
                                        dividerColor = __dividerColor
                                    ) { mode = NewMessageMode.SELECT_CHANNEL }
                                    Text(
                                        text = t.sortedByName,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = __accent,
                                        modifier = Modifier.padding(start = 16.dp, top = 10.dp, bottom = 4.dp)
                                    )
                                }
                            }
                        }

                        if (isLoading) {
                            item {
                                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(color = __accent)
                                }
                            }
                        } else if (filteredContacts.isEmpty()) {
                            item {
                                Text(
                                    text = t.noContactsYet,
                                    fontSize = 14.sp,
                                    color = __textSecondary,
                                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                                    textAlign = TextAlign.Center
                                )
                            }
                        } else {
                            grouped.forEach { (letter, people) ->
                                item {
                                    Text(
                                        text = letter,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = __textSecondary,
                                        modifier = Modifier.padding(start = 16.dp, top = 10.dp, bottom = 2.dp)
                                    )
                                }
                                items(people, key = { it.id }) { person ->
                                    NewMessageContactRow(
                                        person = person,
                                        selectable = mode != NewMessageMode.BROWSE,
                                        isSelected = person.id in selectedIds,
                                        textColor = __textPrimary,
                                        secondaryColor = __textSecondary,
                                        dividerColor = __dividerColor,
                                        bgColor = __bgColor
                                    ) {
                                        if (mode == NewMessageMode.BROWSE) {
                                            startDirectChat(person.id, person.displayName)
                                        } else {
                                            selectedIds = if (person.id in selectedIds) selectedIds - person.id else selectedIds + person.id
                                        }
                                    }
                                }
                            }
                        }
                    }
                    FastScroll(listState = listState, modifier = Modifier.align(Alignment.CenterEnd))
                }
            }
        }
    }

    if (showGallerySheet) {
        AvatarGalleryBottomSheet(
            onDismiss = { showGallerySheet = false },
            onImagePicked = { uri ->
                showGallerySheet = false
                cropUri = uri
            }
        )
    }
}

@Composable
private fun GroupDetailsContent(
    groupName: String,
    onGroupNameChange: (String) -> Unit,
    namePlaceholder: String,
    avatarPreviewBitmap: Bitmap?,
    onPickAvatar: () -> Unit,
    members: List<Profile>,
    t: com.example.ui.i18n.Translation,
    textColor: Color,
    secondaryColor: Color,
    dividerColor: Color,
    accent: Color,
    bgColor: Color
) {
    val memberCountLabel = when (members.size) {
        0 -> t.members
        1 -> t.membersSectionOne
        2 -> t.membersSectionTwo
        else -> t.membersSectionManyFmt.replace("%d", members.size.toString())
    }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            // بطاقة اسم المجموعة/القناة + الصورة
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = dividerColor.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        if (groupName.isEmpty()) {
                            Text(namePlaceholder, fontSize = 16.sp, color = secondaryColor)
                        }
                        BasicTextField(
                            value = groupName,
                            onValueChange = onGroupNameChange,
                            textStyle = TextStyle(fontSize = 16.sp, color = textColor, fontWeight = FontWeight.Medium),
                            cursorBrush = SolidColor(accent),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Box(
                        modifier = Modifier.size(56.dp).clip(CircleShape).background(accent).clickable { onPickAvatar() },
                        contentAlignment = Alignment.Center
                    ) {
                        if (avatarPreviewBitmap != null) {
                            Image(
                                bitmap = avatarPreviewBitmap.asImageBitmap(),
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize().clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(Icons.Outlined.PhotoCamera, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                        }
                    }
                }
            }
        }

        item {
            Text(
                text = memberCountLabel,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = accent,
                modifier = Modifier.padding(start = 16.dp, top = 20.dp, bottom = 4.dp)
            )
        }

        item {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = dividerColor.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            ) {
                Column {
                    members.forEachIndexed { index, person ->
                        NewMessageContactRow(
                            person = person,
                            selectable = false,
                            isSelected = false,
                            textColor = textColor,
                            secondaryColor = secondaryColor,
                            dividerColor = if (index == members.lastIndex) Color.Transparent else dividerColor,
                            bgColor = bgColor
                        ) {}
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}

@Composable
private fun NewMessagePinnedRow(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    bg: Color,
    textColor: Color,
    dividerColor: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(44.dp).clip(CircleShape).background(bg),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
        }
        Spacer(modifier = Modifier.width(14.dp))
        Text(title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = textColor)
    }
    HorizontalDivider(modifier = Modifier.padding(start = 74.dp), color = dividerColor, thickness = 0.5.dp)
}

@Composable
private fun NewMessageContactRow(
    person: Profile,
    selectable: Boolean,
    isSelected: Boolean,
    textColor: Color,
    secondaryColor: Color,
    dividerColor: Color,
    bgColor: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(46.dp)) {
            if (person.avatarUrl != null) {
                AsyncImage(
                    model = person.avatarUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize().clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxSize().clip(CircleShape).background(person.avatarColor),
                    contentAlignment = Alignment.Center
                ) {
                    Text(person.displayName.take(1).uppercase(), color = bgColor, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }
            if (selectable && isSelected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = 2.dp, y = 2.dp)
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(bgColor),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier.size(15.dp).clip(CircleShape).background(Color(0xFF34C759)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(11.dp))
                    }
                }
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    person.displayName,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = textColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (person.isVerified == true) {
                    Spacer(modifier = Modifier.width(4.dp))
                    VerifiedBadge(isVerified = true, iconSize = 15.dp)
                }
            }
            Text(
                text = if (person.displayUsername.isNotBlank()) "@${person.displayUsername}" else "",
                fontSize = 13.sp,
                color = secondaryColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
    HorizontalDivider(modifier = Modifier.padding(start = 74.dp, end = 16.dp), color = dividerColor, thickness = 0.5.dp)
}
