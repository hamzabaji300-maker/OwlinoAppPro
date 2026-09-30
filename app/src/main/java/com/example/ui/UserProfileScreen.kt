package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import com.example.ui.SettingsColors
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.composables.icons.lucide.*
import com.example.supabase
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.launch

@Composable
fun UserProfileScreen(
    userId: String = "",
    onNavigateToStories: (() -> Unit)? = null,
    onAddStory: () -> Unit = {},
    onBack: () -> Unit = {},
    onMessageClick: (String, String) -> Unit = { _, _ -> },
    onFollowersClick: () -> Unit = {},
    onFollowingClick: () -> Unit = {},
    onEditProfileClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val isOwnProfile = userId.isNotEmpty() && userId == supabase.auth.currentUserOrNull()?.id

    // States
    var isFollowing by remember { mutableStateOf(false) }
    var followersCount by remember { mutableStateOf(0L) }
    var followingCount by remember { mutableStateOf(0L) }
    var isMenuOpen by remember { mutableStateOf(false) }
    var isFavorite by remember { mutableStateOf(false) }
    var isQrModalOpen by remember { mutableStateOf(false) }
    
    var isVisible by remember { mutableStateOf(false) }
    var realName by remember { mutableStateOf("User") }
    var username by remember { mutableStateOf("user") }
    var bio by remember { mutableStateOf("Product Designer who focuses\non simplicity & usability.") }
    var location by remember { mutableStateOf("Algeria") }
    var avatarUrl by remember { mutableStateOf<String?>(null) }
    var isVerified by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    var existingChatId by remember { mutableStateOf<String?>(null) }

    // Fetch logic
    LaunchedEffect(userId) {
        isVisible = true
        if (userId.isNotBlank()) {
            try {
                val profile = supabase.postgrest["profiles"]
                    .select { filter { eq("id", userId) } }
                    .decodeSingleOrNull<Profile>()
                if (profile != null) {
                    realName = profile.displayName
                    username = profile.displayUsername
                    if (!profile.bio.isNullOrBlank()) {
                        // Clean up multiple empty lines into a single newline
                        bio = profile.bio!!.replace(Regex("\\n{2,}"), "\n").trim()
                    }
                    avatarUrl = profile.avatarUrl
                    isVerified = profile.isVerified == true
                }
                
                followersCount = supabase.postgrest["followers"].select(Columns.list("id")) {
                    filter { eq("following_id", userId) }
                    count(io.github.jan.supabase.postgrest.query.Count.EXACT)
                }.countOrNull() ?: 0L
                
                followingCount = supabase.postgrest["followers"].select(Columns.list("id")) {
                    filter { eq("follower_id", userId) }
                    count(io.github.jan.supabase.postgrest.query.Count.EXACT)
                }.countOrNull() ?: 0L

                val myId = supabase.auth.currentUserOrNull()?.id
                if (myId != null) {
                    val followCheck = supabase.postgrest["followers"].select(Columns.list("id")) {
                        filter {
                            eq("follower_id", myId)
                            eq("following_id", userId)
                        }
                        count(io.github.jan.supabase.postgrest.query.Count.EXACT)
                    }.countOrNull() ?: 0L
                    isFollowing = followCheck > 0

                    // نجيب الشات الموجود مسبقاً (إذا كاين) وقت تحميل البروفايل،
                    // باش زر Message يفتح المحادثة مباشرة بلا انتظار وقتاش نضغط عليه
                    if (myId != userId) {
                        val myChats = supabase.postgrest["chat_members"].select(Columns.list("chat_id")) {
                            filter { eq("user_id", myId) }
                        }.decodeList<ChatMemberRow>().map { it.chat_id }

                        if (myChats.isNotEmpty()) {
                            val otherChats = supabase.postgrest["chat_members"].select(Columns.list("chat_id")) {
                                filter {
                                    eq("user_id", userId)
                                    isIn("chat_id", myChats)
                                }
                            }.decodeList<ChatMemberRow>().map { it.chat_id }

                            if (otherChats.isNotEmpty()) {
                                val directChats = supabase.postgrest["chats"].select(Columns.list("id")) {
                                    filter {
                                        isIn("id", otherChats)
                                        eq("type", "direct")
                                    }
                                }.decodeList<ChatRow>()

                                if (directChats.isNotEmpty()) {
                                    existingChatId = directChats.first().id
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        isLoading = false
    }

    var isMessageLoading by remember { mutableStateOf(false) }

    val handleMessageClick = {
        if (!isMessageLoading) {
            if (existingChatId != null) {
                // محادثة موجودة مسبقاً وتحملت وقت فتح البروفايل -> فتح فوري بلا انتظار
                onMessageClick(existingChatId!!, realName)
            } else {
            isMessageLoading = true
            coroutineScope.launch {
                try {
                    val myId = supabase.auth.currentUserOrNull()?.id
                    if (myId != null && myId != userId) {
                        val myChats = supabase.postgrest["chat_members"].select(Columns.list("chat_id")) {
                            filter { eq("user_id", myId) }
                        }.decodeList<ChatMemberRow>().map { it.chat_id }

                        var foundChatId: String? = null
                        if (myChats.isNotEmpty()) {
                            val otherChats = supabase.postgrest["chat_members"].select(Columns.list("chat_id")) {
                                filter { 
                                    eq("user_id", userId)
                                    isIn("chat_id", myChats)
                                }
                            }.decodeList<ChatMemberRow>().map { it.chat_id }
                            
                            if (otherChats.isNotEmpty()) {
                                val directChats = supabase.postgrest["chats"].select(Columns.list("id")) {
                                    filter {
                                        isIn("id", otherChats)
                                        eq("type", "direct")
                                    }
                                }.decodeList<ChatRow>()
                                
                                if (directChats.isNotEmpty()) {
                                    foundChatId = directChats.first().id
                                }
                            }
                        }

                        if (foundChatId != null) {
                            onMessageClick(foundChatId, realName)
                        } else {
                            val newChat = supabase.postgrest["chats"].insert(ChatInsert(type = "direct", created_by = myId)) {
                                select()
                            }.decodeSingle<ChatRow>()
                            
                            supabase.postgrest["chat_members"].insert(listOf(
                                ChatMemberRow(chat_id = newChat.id, user_id = myId, role = "admin"),
                                ChatMemberRow(chat_id = newChat.id, user_id = userId, role = "member")
                            ))
                            onMessageClick(newChat.id, realName)
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    isMessageLoading = false
                }
            }
            }
        }
    }

    val handleFollowToggle = {
        val previousState = isFollowing
        isFollowing = !isFollowing
        followersCount = if (isFollowing) followersCount + 1 else followersCount - 1

        coroutineScope.launch {
            try {
                val myId = supabase.auth.currentUserOrNull()?.id ?: return@launch
                if (isFollowing) {
                    supabase.postgrest["followers"].insert(FollowerInsertRow(myId, userId))
                } else {
                    supabase.postgrest["followers"].delete {
                        filter {
                            eq("follower_id", myId)
                            eq("following_id", userId)
                        }
                    }
                }
            } catch (e: Exception) {
                isFollowing = previousState
                followersCount = if (isFollowing) followersCount + 1 else followersCount - 1
            }
        }
    }
    
    val dismiss = {
        isVisible = false
        coroutineScope.launch {
            kotlinx.coroutines.delay(250) // wait for exit anim
            onBack()
        }
    }

    // Scrim overlay
    val scrimAlpha by animateFloatAsState(targetValue = if (isVisible) 1f else 0f, animationSpec = tween(300))

    // Main Wrapper
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent) // Removed darkening effect
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { dismiss() },
        contentAlignment = Alignment.BottomCenter // Aligned to bottom
    ) {
        
        AnimatedVisibility(
            visible = isVisible,
            enter = slideInVertically(initialOffsetY = { it }, animationSpec = tween(400, easing = FastOutSlowInEasing)) + fadeIn(tween(400)),
            exit = slideOutVertically(targetOffsetY = { it }, animationSpec = tween(300, easing = FastOutLinearInEasing)) + fadeOut(tween(300))
        ) {
            // The Main Card (Bottom Sheet style)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SettingsColors.surface, RoundedCornerShape(topStart = 40.dp, topEnd = 40.dp)) // Dark theme background
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { isMenuOpen = false } // Consume clicks to avoid dismissing modal
            ) {
                Column(
                    horizontalAlignment = Alignment.Start,
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    // Image Wrapper (aspect-square, full width, top 40dp radius, bottom 32dp radius)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .background(SettingsColors.background, RoundedCornerShape(topStart = 40.dp, topEnd = 40.dp, bottomStart = 32.dp, bottomEnd = 32.dp))
                            .clip(RoundedCornerShape(topStart = 40.dp, topEnd = 40.dp, bottomStart = 32.dp, bottomEnd = 32.dp))
                    ) {
                        if (avatarUrl != null) {
                            AsyncImage(
                                model = avatarUrl,
                                contentDescription = "Profile Picture",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Box(
                                modifier = Modifier.fillMaxSize().background(SettingsColors.background),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Lucide.User,
                                    contentDescription = "Profile Picture",
                                    tint = SettingsColors.textSecondary,
                                    modifier = Modifier.size(80.dp)
                                )
                            }
                        }

                        // Gradient Overlay (Bottom 35%)
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                                        0.65f to Color.Transparent,
                                        1.0f to Color.Black.copy(alpha = 0.55f)
                                    )
                                )
                        )

                        // Message Button (Top Left)
                        if (!isOwnProfile) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(16.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .height(36.dp)
                                    .background(Color(0x33000000), RoundedCornerShape(50))
                                    .clip(RoundedCornerShape(50))
                                    .clickable { handleMessageClick() },
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    if (isMessageLoading) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            color = Color.White,
                                            strokeWidth = 2.dp
                                        )
                                    }
                                    Text(
                                        text = com.example.ui.i18n.LocalTranslation.current.messageInputPlaceholder,
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                        }

                        // Options Button (Top Right)
                        if (!isOwnProfile) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(16.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(Color(0x33000000), CircleShape)
                                    .clip(CircleShape)
                                    .clickable { isMenuOpen = !isMenuOpen },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.MoreHoriz,
                                    contentDescription = "More options",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            // Dropdown Menu
                            Box(
                                modifier = Modifier
                                    .padding(top = 44.dp)
                                    .align(Alignment.TopEnd)
                            ) {
                                androidx.compose.animation.AnimatedVisibility(
                                    visible = isMenuOpen,
                                    enter = fadeIn(tween(150)) + scaleIn(tween(150), initialScale = 0.95f),
                                    exit = fadeOut(tween(150)) + scaleOut(tween(150), targetScale = 0.95f)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(16.dp),
                                        color = SettingsColors.surface,
                                        shadowElevation = 8.dp,
                                        modifier = Modifier.width(192.dp)
                                    ) {
                                        Column(modifier = Modifier.padding(vertical = 8.dp)) {
                                            // Add to Favorites
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable {
                                                        isFavorite = !isFavorite
                                                        coroutineScope.launch { kotlinx.coroutines.delay(600); isMenuOpen = false }
                                                    }
                                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                                            ) {
                                                Box(modifier = Modifier.size(18.dp), contentAlignment = Alignment.Center) {
                                                    AnimatedContent(
                                                        targetState = isFavorite,
                                                        transitionSpec = {
                                                            if (targetState) {
                                                                (scaleIn(tween(300)) + fadeIn()).togetherWith(scaleOut(tween(300)) + fadeOut())
                                                            } else {
                                                                (scaleIn(tween(200)) + fadeIn()).togetherWith(scaleOut(tween(200)) + fadeOut())
                                                            }
                                                        },
                                                        label = "FavoriteStar"
                                                    ) { fav ->
                                                        if (fav) {
                                                            Icon(Icons.Filled.Star, contentDescription = null, tint = Color(0xFFFACC15), modifier = Modifier.size(18.dp))
                                                        } else {
                                                            Icon(Lucide.Star, contentDescription = null, tint = SettingsColors.textSecondary, modifier = Modifier.size(18.dp))
                                                        }
                                                    }
                                                }
                                                Text(if (isFavorite) "Favorited" else "Add to Favorites", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = SettingsColors.textPrimary)
                                            }
        
                                            // QR Code
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable { isMenuOpen = false; isQrModalOpen = true }
                                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                                            ) {
                                                Icon(Lucide.QrCode, contentDescription = null, tint = SettingsColors.textSecondary, modifier = Modifier.size(18.dp))
                                                Text("QR Code", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = SettingsColors.textPrimary)
                                            }
        
                                            Box(modifier = Modifier.fillMaxWidth().height(1.dp).padding(horizontal = 12.dp).background(SettingsColors.divider))
        
                                            // Report
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable { /* Report */ }
                                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                                            ) {
                                                Icon(Lucide.Flag, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(18.dp))
                                                Text("Report User", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color(0xFFDC2626))
                                            }
        
                                            // Block
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable { /* Block */ }
                                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                                            ) {
                                                Icon(Lucide.Ban, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(18.dp))
                                                Text("Block User", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color(0xFFDC2626))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Column(modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 8.dp)) {
                        // Header
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(realName, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = SettingsColors.textPrimary, letterSpacing = (-0.5).sp, modifier = Modifier.weight(1f, fill = false), maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                            if (isVerified) {
                                com.example.ui.VerifiedBadge(isVerified = true, iconSize = 20.dp)
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Bio
                        Text(
                            text = bio,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = SettingsColors.textPrimary,
                            lineHeight = 20.sp,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        // Footer / Stats
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(20.dp), verticalAlignment = Alignment.CenterVertically) {
                                // Followers
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Lucide.Users, contentDescription = "Followers", tint = SettingsColors.textPrimary, modifier = Modifier.size(18.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.Bottom) {
                                        Text(followersCount.toString(), fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = SettingsColors.textPrimary)
                                        Text("Followers", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = SettingsColors.textPrimary, modifier = Modifier.padding(bottom = 1.dp))
                                    }
                                }
                                
                                // Following
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Lucide.UserCheck, contentDescription = "Following", tint = SettingsColors.textPrimary, modifier = Modifier.size(18.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.Bottom) {
                                        Text(followingCount.toString(), fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = SettingsColors.textPrimary)
                                        Text("Following", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = SettingsColors.textPrimary, modifier = Modifier.padding(bottom = 1.dp))
                                    }
                                }
                            }
                            
                            // Follow Button Row / Edit Profile Button (own profile)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                if (isOwnProfile) {
                                    Box(
                                        modifier = Modifier
                                            .background(SettingsColors.textPrimary.copy(alpha = 0.08f), RoundedCornerShape(50))
                                            .border(1.dp, SettingsColors.divider, RoundedCornerShape(50))
                                            .clip(RoundedCornerShape(50))
                                            .clickable { onEditProfileClick() }
                                            .padding(horizontal = 20.dp, vertical = 10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("Edit Profile", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = SettingsColors.textPrimary)
                                    }
                                } else {
                                // Follow Button
                                val followBg = Color(0xFF3B82F6)
                                val followBorder = Color.Transparent
                                val followText = Color.White
                                Box(
                                    modifier = Modifier
                                        .background(followBg, RoundedCornerShape(50))
                                        .border(1.dp, followBorder, RoundedCornerShape(50))
                                        .clip(RoundedCornerShape(50))
                                        .clickable { handleFollowToggle() }
                                        .padding(horizontal = 20.dp, vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(if (isFollowing) "Following" else "Follow", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = followText)
                                        if (!isFollowing) {
                                            Text("+", fontSize = 18.sp, fontWeight = FontWeight.Normal, color = followText)
                                        }
                                    }
                                }
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(32.dp))
                    Spacer(modifier = Modifier.navigationBarsPadding())
                }
            }
        }

        // QR Modal Overlay
        androidx.compose.animation.AnimatedVisibility(
            visible = isQrModalOpen,
            enter = fadeIn(tween(300)),
            exit = fadeOut(tween(300))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x99000000)) // darker for modal
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { isQrModalOpen = false },
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.animation.AnimatedVisibility(
                    visible = isQrModalOpen,
                    enter = scaleIn(spring(dampingRatio = 0.6f, stiffness = 400f), initialScale = 0.9f) + slideInVertically(initialOffsetY = { 50 }),
                    exit = scaleOut(tween(200), targetScale = 0.9f) + slideOutVertically(targetOffsetY = { 50 })
                ) {
                    Box(
                        modifier = Modifier
                            .width(300.dp)
                            .background(SettingsColors.surface, RoundedCornerShape(32.dp))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { /* Consume click */ }
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        // Close btn
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(top = 16.dp, end = 16.dp)
                                .size(36.dp)
                                .background(SettingsColors.background, CircleShape)
                                .clip(CircleShape)
                                .clickable { isQrModalOpen = false },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Lucide.X, contentDescription = "Close", tint = SettingsColors.textSecondary, modifier = Modifier.size(18.dp))
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            // Mini avatar
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .background(SettingsColors.background, RoundedCornerShape(16.dp))
                                    .clip(RoundedCornerShape(16.dp))
                            ) {
                                if (avatarUrl != null) {
                                    AsyncImage(
                                        model = avatarUrl,
                                        contentDescription = "Profile",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Icon(Lucide.User, contentDescription = null, tint = SettingsColors.textSecondary, modifier = Modifier.align(Alignment.Center).size(36.dp))
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            Text(realName, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = SettingsColors.textPrimary, letterSpacing = (-0.5).sp)
                            Text("Scan to view profile", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = SettingsColors.textSecondary)

                            Spacer(modifier = Modifier.height(24.dp))

                            // QR Code container
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(SettingsColors.background, RoundedCornerShape(24.dp))
                                    .border(1.dp, SettingsColors.background, RoundedCornerShape(24.dp))
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Lucide.QrCode, contentDescription = "QR Code", tint = SettingsColors.textPrimary, modifier = Modifier.size(160.dp))
                                    Box(
                                        modifier = Modifier
                                            .background(SettingsColors.surface, RoundedCornerShape(8.dp))
                                            .shadow(2.dp, RoundedCornerShape(8.dp))
                                            .padding(6.dp)
                                    ) {
                                        Icon(Icons.Filled.Star, contentDescription = null, tint = Color(0xFF3B82F6), modifier = Modifier.size(24.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@kotlinx.serialization.Serializable
data class FollowerInsertRow(val follower_id: String, val following_id: String)
