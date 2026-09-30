package com.example.ui

import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import com.example.ui.i18n.LocalTranslation
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.composables.icons.lucide.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.ui.draw.shadow

import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.example.supabase
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.serialization.Serializable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Serializable
private data class FollowerIdRow(val follower_id: String)
@Serializable
private data class UserListFollowingIdRow(val following_id: String)
@Serializable
private data class FollowRow(val follower_id: String, val following_id: String)

data class UserListModel(
    val id: String,
    val displayName: String,
    val username: String,
    val avatarUrl: String,
    val isVerified: Boolean,
    val isOnline: Boolean,
    val isFollowing: Boolean,
    val category: String, // "mutual", "suggested", "none"
    val isBlocked: Boolean = false,
    val isMuted: Boolean = false,
    val isRemoved: Boolean = false
)

private fun generateMockUsers(type: String): List<UserListModel> {
    val users = listOf(
        UserListModel("1", "Elena Rostova", "elena_r", "https://i.pravatar.cc/150?u=1", true, true, type == "following", "none"),
        UserListModel("2", "Marcus Chen", "marcus.c", "https://i.pravatar.cc/150?u=2", false, false, true, "mutual"),
        UserListModel("3", "Sarah Jenkins", "sarahj", "https://i.pravatar.cc/150?u=3", false, true, true, "mutual"),
        UserListModel("4", "Alex Rivera", "arivera99", "https://i.pravatar.cc/150?u=4", true, false, false, "suggested"),
        UserListModel("5", "David Kim", "dkim_dev", "https://i.pravatar.cc/150?u=5", false, true, false, "none"),
        UserListModel("6", "Emma Watson", "emma.w", "https://i.pravatar.cc/150?u=6", true, false, true, "mutual"),
        UserListModel("7", "Lucas Silva", "lucass", "https://i.pravatar.cc/150?u=7", false, false, type == "following", "none"),
        UserListModel("8", "Tech Insider", "techinsider", "https://i.pravatar.cc/150?u=8", true, true, true, "mutual"),
        UserListModel("9", "Design UI", "design_ui", "https://i.pravatar.cc/150?u=9", false, false, type == "following", "suggested"),
        UserListModel("10", "Crypto King", "cryptoking", "https://i.pravatar.cc/150?u=10", true, true, false, "none")
    )
    return if (type == "followers") users.take(8) else users.drop(1).take(8)
}

val DarkSlate = Color(0xFF111827)
val BrandBlue = Color(0xFF3B82F6)
val BgGray = Color(0xFFF3F4F6)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserListScreen(
    initialType: String,
    onBack: () -> Unit,
    onUserClick: (UserListModel) -> Unit,
    onUpdateFollowers: (Int) -> Unit,
    onUpdateFollowing: (Int) -> Unit,
    embedded: Boolean = false
) {
        var activeTab by remember { mutableStateOf(if (initialType == "followers") "followers" else "following") }
    var users by remember { mutableStateOf(emptyList<UserListModel>()) }
    var loading by remember { mutableStateOf(true) }
    var refreshing by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var sortOrder by remember { mutableStateOf("default") }
    
    var isSortMenuOpen by remember { mutableStateOf(false) }
    var reportDialogUserId by remember { mutableStateOf<String?>(null) }
    var reportReason by remember { mutableStateOf<String?>(null) }
    
    var confirmDialogType by remember { mutableStateOf<String?>(null) }
    var confirmDialogUserId by remember { mutableStateOf<String?>(null) }
    
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    LaunchedEffect(activeTab) {
        loading = true
        val myId = supabase.auth.currentSessionOrNull()?.user?.id
        if (myId != null) {
            try {
                val db = com.example.data.DatabaseProvider.getDatabase(context)
                
                val followersList = supabase.postgrest["followers"].select(Columns.list("follower_id")) {
                    filter { eq("following_id", myId) }
                }.decodeList<FollowerIdRow>().map { it.follower_id }.toSet()
                
                val followingList = supabase.postgrest["followers"].select(Columns.list("following_id")) {
                    filter { eq("follower_id", myId) }
                }.decodeList<UserListFollowingIdRow>().map { it.following_id }.toSet()
                
                val targetIds = when (activeTab) {
                    "followers" -> followersList
                    "following" -> followingList
                    "mutual" -> followersList.intersect(followingList)
                    else -> emptySet()
                }
                
                if (targetIds.isNotEmpty()) {
                    val profiles = supabase.postgrest["profiles"].select {
                        filter { isIn("id", targetIds.toList()) }
                    }.decodeList<com.example.ui.Profile>()
                    
                    val blockedIds = BlockManager.getBlockedByMe(myId).toSet()

                    users = profiles.map { p ->
                        // Update Room Cache
                        coroutineScope.launch(Dispatchers.IO) {
                            val cached = db.profileDao().getProfileById(p.id)
                            if (cached != null) {
                                db.profileDao().insertProfile(cached.copy(isFollowing = followingList.contains(p.id)))
                            } else {
                                db.profileDao().insertProfile(com.example.data.ProfileEntity(
                                    id = p.id,
                                    username = p.username,
                                    fullName = p.fullName,
                                    avatarUrl = p.avatarUrl,
                                    isFollowing = followingList.contains(p.id)
                                ))
                            }
                        }
                        
                        UserListModel(
                            id = p.id,
                            displayName = p.displayName,
                            username = p.displayUsername,
                            avatarUrl = p.avatarUrl ?: "",
                            isVerified = p.isVerified == true,
                            isOnline = p.isOnlineNow,
                            isFollowing = followingList.contains(p.id),
                            category = if (followersList.contains(p.id) && followingList.contains(p.id)) "mutual" else "none",
                            isBlocked = blockedIds.contains(p.id)
                        )
                    }
                } else {
                    users = emptyList()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        loading = false
    }

    val handleRefresh = {
        coroutineScope.launch {
            refreshing = true
            // Can reload here if needed, for now just delay
            delay(800)
            refreshing = false
        }
    }

    val updateUserState = { userId: String, updates: (UserListModel) -> UserListModel ->
        users = users.map { 
            if (it.id == userId) {
                val updated = updates(it)
                updated
            } else {
                it
            }
        }
    }

    val filteredUsers = users
        .filter { !it.isRemoved }
        .filter { 
            it.username.contains(searchQuery, ignoreCase = true) || 
            it.displayName.contains(searchQuery, ignoreCase = true) 
        }
        .sortedWith { a, b ->
            when (sortOrder) {
                "alphabetical" -> a.displayName.compareTo(b.displayName, ignoreCase = true)
                "recent" -> b.id.toInt().compareTo(a.id.toInt())
                else -> 0
            }
        }

    fun showToast(msg: String, actionLabel: String? = null, onUndo: (() -> Unit)? = null) {
        coroutineScope.launch {
            val result = snackbarHostState.showSnackbar(
                message = msg,
                actionLabel = actionLabel,
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed && onUndo != null) {
                onUndo()
            }
        }
    }

    val embeddedBg = LocalSettingsTheme.current.theme.bgColor
    Scaffold(
        containerColor = if (embedded) embeddedBg else Color.White,
        contentWindowInsets = if (embedded) WindowInsets(0) else ScaffoldDefaults.contentWindowInsets,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Search and Filters
            Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 16.dp)) {
                if (!embedded) Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    // Back button (round card)
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .shadow(3.dp, CircleShape)
                            .clip(CircleShape)
                            .background(Color.White)
                            .border(1.dp, Color.Black.copy(alpha = 0.05f), CircleShape)
                            .clickable { onBack() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Lucide.ArrowLeft, contentDescription = "Back", tint = DarkSlate, modifier = Modifier.size(22.dp))
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    // Search bar (rounded card)
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text(com.example.ui.i18n.LocalTranslation.current.searchPlaceholder, color = Color.Gray) },
                        leadingIcon = { Icon(Lucide.Search, contentDescription = null, modifier = Modifier.size(20.dp), tint = Color.Gray) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(24.dp)) {
                                    Icon(Lucide.X, contentDescription = "Clear", modifier = Modifier.size(16.dp), tint = Color.Gray)
                                }
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .shadow(3.dp, RoundedCornerShape(26.dp)),
                        shape = RoundedCornerShape(26.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            focusedBorderColor = Color.Black.copy(alpha = 0.05f),
                            unfocusedBorderColor = Color.Black.copy(alpha = 0.05f)
                        ),
                        singleLine = true
                    )
                }

                if (!embedded) Spacer(modifier = Modifier.height(12.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("followers", "following", "mutual").forEach { tab ->
                            val isSelected = activeTab == tab
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(if (isSelected) DarkSlate else BgGray)
                                    .clickable { activeTab = tab }
                                    .padding(horizontal = 16.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = tab.replaceFirstChar { it.uppercase() },
                                    color = if (isSelected) Color.White else DarkSlate,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                    
                    Box {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { isSortMenuOpen = true }
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Lucide.ChevronDown, contentDescription = "Sort", modifier = Modifier.size(16.dp), tint = DarkSlate)
                        }
                        
                        DropdownMenu(
                            expanded = isSortMenuOpen,
                            onDismissRequest = { isSortMenuOpen = false },
                            modifier = Modifier.background(Color.White)
                        ) {
                            listOf(
                                "default" to "Default",
                                "recent" to "Date followed",
                                "alphabetical" to "Alphabetical"
                            ).forEach { (id, label) ->
                                DropdownMenuItem(
                                    text = { 
                                        Text(
                                            label, 
                                            fontWeight = if (sortOrder == id) FontWeight.Bold else FontWeight.Medium,
                                            color = if (sortOrder == id) DarkSlate else Color.Gray
                                        ) 
                                    },
                                    onClick = { sortOrder = id; isSortMenuOpen = false }
                                )
                            }
                        }
                    }
                }
            }
            
            // List Content
            Box(modifier = Modifier.fillMaxSize()) {
                if (loading) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        repeat(6) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(52.dp).clip(CircleShape).background(BgGray))
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Box(modifier = Modifier.width(100.dp).height(16.dp).background(BgGray))
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Box(modifier = Modifier.width(140.dp).height(12.dp).background(BgGray))
                                }
                                Box(modifier = Modifier.width(80.dp).height(32.dp).clip(RoundedCornerShape(8.dp)).background(BgGray))
                            }
                        }
                    }
                } else if (filteredUsers.isEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(bottom = 80.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier.size(80.dp).clip(CircleShape).background(BgGray),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Lucide.UserPlus, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(40.dp))
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(com.example.ui.i18n.LocalTranslation.current.usersNotFound, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = DarkSlate)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(com.example.ui.i18n.LocalTranslation.current.noMatchingSearch, color = Color.Gray, fontSize = 15.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center, modifier = Modifier.width(250.dp))
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 24.dp)
                    ) {
                        items(filteredUsers, key = { it.id }) { user ->
                            var isMenuOpen by remember { mutableStateOf(false) }
                            
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onUserClick(user) }
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(modifier = Modifier.size(52.dp)) {
                                    AsyncImage(
                                        model = user.avatarUrl,
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize().clip(CircleShape).border(1.dp, BgGray, CircleShape),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                                
                                Spacer(modifier = Modifier.width(12.dp))
                                
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = highlightText(user.username, searchQuery),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.5.sp,
                                            color = DarkSlate,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f, fill = false)
                                        )
                                        if (user.isVerified) {
                                            Spacer(modifier = Modifier.width(4.dp))
                                            VerifiedBadge(isVerified = true, iconSize = 16.dp)
                                        }
                                    }
                                    Text(
                                        text = highlightText(user.displayName, searchQuery),
                                        fontSize = 13.5.sp,
                                        color = Color.Gray,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                
                                Spacer(modifier = Modifier.width(8.dp))
                                
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (user.isBlocked) {
                                        Button(
                                            onClick = {
                                                updateUserState(user.id) { it.copy(isBlocked = false) }
                                                showToast("Unblocked ${user.username}")
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFE4E6), contentColor = Color(0xFFE11D48)),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 6.dp),
                                            modifier = Modifier.height(32.dp)
                                        ) {
                                            Text(com.example.ui.i18n.LocalTranslation.current.unblock, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                        }
                                    } else {
                                        Button(
                                            onClick = {
                                                coroutineScope.launch {
                                                    val myId = supabase.auth.currentSessionOrNull()?.user?.id
                                                    if (myId != null) {
                                                        try {
                                                            val newState = !user.isFollowing
                                                            if (newState) {
                                                                supabase.postgrest["followers"].insert(FollowRow(follower_id = myId, following_id = user.id))
                                                            } else {
                                                                supabase.postgrest["followers"].delete {
                                                                    filter {
                                                                        eq("follower_id", myId)
                                                                        eq("following_id", user.id)
                                                                    }
                                                                }
                                                            }
                                                            
                                                            val db = com.example.data.DatabaseProvider.getDatabase(context)
                                                            val profile = db.profileDao().getProfileById(user.id)
                                                            if (profile != null) {
                                                                db.profileDao().insertProfile(profile.copy(isFollowing = newState))
                                                            }
                                                            
                                                            updateUserState(user.id) { it.copy(isFollowing = newState) }
                                                            onUpdateFollowing(if (newState) 1 else -1)
                                                        } catch (e: Exception) {
                                                            e.printStackTrace()
                                                        }
                                                    }
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (user.isFollowing) BgGray else BrandBlue,
                                                contentColor = if (user.isFollowing) DarkSlate else Color.White
                                            ),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 6.dp),
                                            modifier = Modifier.height(32.dp)
                                        ) {
                                            Text(if (user.isFollowing) com.example.ui.i18n.LocalTranslation.current.following else com.example.ui.i18n.LocalTranslation.current.follow, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                        }
                                    }
                                    
                                    Box {
                                        IconButton(onClick = { isMenuOpen = true }, modifier = Modifier.size(36.dp)) {
                                            Icon(Icons.Filled.MoreVert, contentDescription = "More", tint = Color.Gray)
                                        }
                                        DropdownMenu(
                                            expanded = isMenuOpen,
                                            onDismissRequest = { isMenuOpen = false },
                                            modifier = Modifier.background(Color.White)
                                        ) {
                                            if (activeTab == "followers") {
                                                DropdownMenuItem(
                                                    text = { Text(com.example.ui.i18n.LocalTranslation.current.removeFollower, color = Color.Red) },
                                                    leadingIcon = { Icon(Lucide.X, contentDescription = null, tint = Color.Red) },
                                                    onClick = {
                                                        isMenuOpen = false
                                                        confirmDialogUserId = user.id
                                                        confirmDialogType = "remove"
                                                    }
                                                )
                                            }
                                            if (activeTab == "following" && user.isFollowing) {
                                                DropdownMenuItem(
                                                    text = { Text(com.example.ui.i18n.LocalTranslation.current.unfollow, color = Color.Red) },
                                                    leadingIcon = { Icon(Lucide.UserPlus, contentDescription = null, tint = Color.Red) },
                                                    onClick = {
                                                        isMenuOpen = false
                                                        coroutineScope.launch {
                                                            val myId = supabase.auth.currentSessionOrNull()?.user?.id
                                                            if (myId != null) {
                                                                try {
                                                                    supabase.postgrest["followers"].delete {
                                                                        filter {
                                                                            eq("follower_id", myId)
                                                                            eq("following_id", user.id)
                                                                        }
                                                                    }
                                                                    val db = com.example.data.DatabaseProvider.getDatabase(context)
                                                                    val profile = db.profileDao().getProfileById(user.id)
                                                                    if (profile != null) {
                                                                        db.profileDao().insertProfile(profile.copy(isFollowing = false))
                                                                    }
                                                                    updateUserState(user.id) { it.copy(isFollowing = false) }
                                                                    onUpdateFollowing(-1)
                                                                } catch (e: Exception) {
                                                                    e.printStackTrace()
                                                                }
                                                            }
                                                        }
                                                    }
                                                )
                                            }
                                            DropdownMenuItem(
                                                text = { Text(if (user.isBlocked) com.example.ui.i18n.LocalTranslation.current.unblock else com.example.ui.i18n.LocalTranslation.current.block) },
                                                leadingIcon = { Icon(Lucide.ShieldAlert, contentDescription = null) },
                                                onClick = {
                                                    isMenuOpen = false
                                                    if (user.isBlocked) {
                                                        updateUserState(user.id) { it.copy(isBlocked = false) }
                                                        showToast("Unblocked ${user.username}")
                                                    } else {
                                                        confirmDialogUserId = user.id
                                                        confirmDialogType = "block"
                                                    }
                                                }
                                            )
                                            if (!user.isBlocked) {
                                                DropdownMenuItem(
                                                    text = { Text(com.example.ui.i18n.LocalTranslation.current.report) },
                                                    leadingIcon = { Icon(Lucide.Flag, contentDescription = null) },
                                                    onClick = {
                                                        isMenuOpen = false
                                                        reportDialogUserId = user.id
                                                    }
                                                )
                                            }
                                            HorizontalDivider()
                                            DropdownMenuItem(
                                                text = { Text(com.example.ui.i18n.LocalTranslation.current.copyProfileLink) },
                                                onClick = {
                                                    isMenuOpen = false
                                                    val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                                    val clip = android.content.ClipData.newPlainText("Profile Link", "https://nightowl.app/${user.username}")
                                                    clipboard.setPrimaryClip(clip)
                                                    showToast("Link copied to clipboard")
                                                }
                                            )
                                            DropdownMenuItem(
                                                text = { Text(com.example.ui.i18n.LocalTranslation.current.shareProfile) },
                                                onClick = {
                                                    isMenuOpen = false
                                                    val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                                        putExtra(Intent.EXTRA_TEXT, "Check out ${user.displayName} on nightowl! https://nightowl.app/${user.username}")
                                                        type = "text/plain"
                                                    }
                                                    context.startActivity(Intent.createChooser(sendIntent, "Share Profile"))
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                
                // Refresh indicator overlay
                if (refreshing) {
                    Box(
                        modifier = Modifier.align(Alignment.TopCenter).padding(top = 8.dp).shadow(4.dp, CircleShape).clip(CircleShape).background(Color.White).padding(8.dp)
                    ) {
                        Icon(Lucide.RefreshCw, contentDescription = null, tint = DarkSlate, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }

    // Confirmation Dialog
    if (confirmDialogType != null) {
        val user = users.find { it.id == confirmDialogUserId }
        Dialog(onDismissRequest = { confirmDialogType = null }) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (confirmDialogType == "block") "Block @${user?.username}?" else "Remove follower?",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = DarkSlate,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (confirmDialogType == "block") "They won't be able to find your profile, follow you, or message you." else "This person will no longer follow you. They won't be notified.",
                        color = Color.Gray,
                        fontSize = 15.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Button(
                        onClick = {
                            if (confirmDialogType == "block") {
                                coroutineScope.launch {
                                    val myId = supabase.auth.currentSessionOrNull()?.user?.id
                                    if (myId != null) {
                                        val wasFollowing = user?.isFollowing == true
                                        try {
                                            BlockManager.blockUser(myId, user!!.id)
                                            updateUserState(user!!.id) { it.copy(isBlocked = true, isFollowing = false) }
                                            if (wasFollowing) onUpdateFollowing(-1)
                                            showToast("User blocked successfully.")
                                        } catch(e:Exception) {}
                                    }
                                }
                            } else {
                                coroutineScope.launch {
                                    val myId = supabase.auth.currentSessionOrNull()?.user?.id
                                    if (myId != null) {
                                        try {
                                            supabase.postgrest["followers"].delete {
                                                filter {
                                                    eq("follower_id", user!!.id)
                                                    eq("following_id", myId)
                                                }
                                            }
                                            updateUserState(user!!.id) { it.copy(isRemoved = true) }
                                            onUpdateFollowers(-1)
                                            showToast("Follower removed.")
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                        }
                                    }
                                }
                            }
                            confirmDialogType = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = if (confirmDialogType == "block") Color.Red else DarkSlate),
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(if (confirmDialogType == "block") "Block" else "Remove", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { confirmDialogType = null },
                        colors = ButtonDefaults.buttonColors(containerColor = BgGray, contentColor = DarkSlate),
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(com.example.ui.i18n.LocalTranslation.current.cancel, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }
        }
    }

    // Report Dialog
    if (reportDialogUserId != null) {
        val user = users.find { it.id == reportDialogUserId }
        Dialog(onDismissRequest = { reportDialogUserId = null; reportReason = null }) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("${com.example.ui.i18n.LocalTranslation.current.reportUserTitle} @${user?.username}", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = DarkSlate)
                        IconButton(onClick = { reportDialogUserId = null; reportReason = null }, modifier = Modifier.size(24.dp)) {
                            Icon(Lucide.X, contentDescription = "Close", tint = Color.Gray)
                        }
                    }
                    HorizontalDivider(color = Color.Black.copy(alpha = 0.05f))
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(com.example.ui.i18n.LocalTranslation.current.reportUserDesc, color = Color.Gray, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        LazyColumn(modifier = Modifier.heightIn(max = 250.dp)) {
                            items(listOf("Spam", "Harassment or bullying", "Inappropriate content", "Hate speech or symbols", "False information", "Scam or fraud")) { reason ->
                                val isSelected = reportReason == reason
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isSelected) BrandBlue.copy(alpha = 0.1f) else Color.Transparent)
                                        .border(1.dp, if (isSelected) BrandBlue else Color.LightGray, RoundedCornerShape(12.dp))
                                        .clickable { reportReason = reason }
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(reason, color = if (isSelected) Color(0xFF1D4ED8) else DarkSlate, fontWeight = FontWeight.Medium, fontSize = 15.sp)
                                    if (isSelected) {
                                        Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = BrandBlue, modifier = Modifier.size(20.dp))
                                    }
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Button(
                                onClick = { reportDialogUserId = null; reportReason = null },
                                colors = ButtonDefaults.buttonColors(containerColor = BgGray, contentColor = DarkSlate),
                                modifier = Modifier.weight(1f).height(48.dp)
                            ) {
                                Text(com.example.ui.i18n.LocalTranslation.current.cancel, fontWeight = FontWeight.SemiBold)
                            }
                            Button(
                                onClick = {
                                    reportDialogUserId = null
                                    reportReason = null
                                    showToast("Report submitted.")
                                },
                                enabled = reportReason != null,
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Red, disabledContainerColor = Color(0xFFFCA5A5)),
                                modifier = Modifier.weight(1f).height(48.dp)
                            ) {
                                Text(com.example.ui.i18n.LocalTranslation.current.report, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }
    }
}

fun highlightText(text: String, query: String): AnnotatedString {
    if (query.isBlank()) return AnnotatedString(text)
    return buildAnnotatedString {
        val matches = Regex(query, RegexOption.IGNORE_CASE).findAll(text)
        var lastIndex = 0
        for (match in matches) {
            append(text.substring(lastIndex, match.range.first))
            withStyle(style = SpanStyle(color = BrandBlue, background = BrandBlue.copy(alpha = 0.1f))) {
                append(match.value)
            }
            lastIndex = match.range.last + 1
        }
        append(text.substring(lastIndex))
    }
}
