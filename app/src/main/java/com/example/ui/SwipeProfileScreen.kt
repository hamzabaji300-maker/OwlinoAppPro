package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import com.example.ui.i18n.LocalTranslation
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.example.supabase
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.launch

private fun formatStat(count: Long?): String {
    if (count == null) return "..."
    return if (count >= 1000) {
        val kValue = count / 1000.0
        val formatted = ((kValue * 10.0).toLong() / 10.0).toString()
        if (formatted.endsWith(".0")) "${formatted.substringBefore(".")}k" else "${formatted}k"
    } else {
        count.toString()
    }
}

@Composable
fun SwipeProfileScreen(userId: String = "", onBack: () -> Unit = {
    }, onMessageClick: (String, String) -> Unit = { _, _ -> }, onFollowersClick: () -> Unit = {}, onEditProfileClick: () -> Unit = {}) {
        val __themeConfig = com.example.ui.LocalSettingsTheme.current
    val __theme = __themeConfig.theme
    val __bgColor = __theme.bgColor
    val __surfaceColor = __theme.surfaceColor
    val __textPrimary = __theme.textPrimary
    val __textSecondary = __theme.textSecondary
    val __dividerColor = __theme.dividerColor
    val __accent = __themeConfig.accent

    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = remember { context.getSharedPreferences("user_profile_prefs", android.content.Context.MODE_PRIVATE) }
    var menuExpanded by remember { mutableStateOf(false) }

    var postsCount by remember { mutableStateOf<Long?>(null) }
    var followersCount by remember { mutableStateOf<Long?>(null) }
    var followingCount by remember { mutableStateOf<Long?>(null) }
    var userPosts by remember { mutableStateOf<List<kotlinx.serialization.json.JsonObject>?>(null) }

    val currentUser = supabase.auth.currentUserOrNull()
    var realName by remember { mutableStateOf("Nightowl") }
    var username by remember { mutableStateOf(currentUser?.email?.substringBefore("@") ?: "user") }
    var bio by remember { mutableStateOf("Night-shift crypto trader. Order flow, structure, calm risk. Building OwlinoPlus — a private members' lounge for traders who prefer craft over noise.") }
    var location by remember { mutableStateOf("Lisbon") }
    var website by remember { mutableStateOf("owlino.com & 1 more") }
    var badge by remember { mutableStateOf("Trader · Order flow & structure") }
    var avatarUrl by remember { mutableStateOf("") }

    LaunchedEffect(currentUser) {
        if (currentUser != null) {
            val currUserId = currentUser.id
            bio = prefs.getString("bio_$currUserId", bio) ?: bio
            location = prefs.getString("location_$currUserId", location) ?: location
            website = prefs.getString("website_$currUserId", website) ?: website
            val storedBadge = prefs.getString("badge_$currUserId", null)
            if (storedBadge != null && storedBadge != "None") {
                badge = storedBadge
            }
            try {
                val profile = supabase.postgrest["profiles"]
                    .select() { filter { eq("id", currentUser.id) } }
                    .decodeSingleOrNull<com.example.ui.Profile>()
                if (profile != null) {
                    val defaultName = currentUser.email?.substringBefore("@") ?: "user"
                    realName = profile.fullName.takeIf { !it.isNullOrBlank() } ?: defaultName.replaceFirstChar { it.uppercase() }
                    username = profile.username.takeIf { !it.isNullOrBlank() } ?: defaultName
                    avatarUrl = profile.avatarUrl ?: ""
                } else {
                    username = currentUser.email?.substringBefore("@") ?: "user"
                }

                postsCount = supabase.postgrest["posts"].select(io.github.jan.supabase.postgrest.query.Columns.list("id")) {
                    filter { eq("user_id", currentUser.id) }
                    count(io.github.jan.supabase.postgrest.query.Count.EXACT)
                }.countOrNull() ?: 0L

                followersCount = supabase.postgrest["followers"].select(io.github.jan.supabase.postgrest.query.Columns.list("id")) {
                    filter { eq("following_id", currentUser.id) }
                    count(io.github.jan.supabase.postgrest.query.Count.EXACT)
                }.countOrNull() ?: 0L

                followingCount = supabase.postgrest["followers"].select(io.github.jan.supabase.postgrest.query.Columns.list("id")) {
                    filter { eq("follower_id", currentUser.id) }
                    count(io.github.jan.supabase.postgrest.query.Count.EXACT)
                }.countOrNull() ?: 0L

                userPosts = supabase.postgrest["posts"]
                    .select {
                        filter { eq("user_id", currentUser.id) }
                        order("created_at", io.github.jan.supabase.postgrest.query.Order.DESCENDING)
                    }
                    .decodeList<kotlinx.serialization.json.JsonObject>()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .statusBarsPadding(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = "Back",
                        tint = __textPrimary,
                        modifier = Modifier
                            .size(24.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = onBack
                            )
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "@$username",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = __textPrimary
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Spacer(modifier = Modifier.width(16.dp))
                    Box {
                        Icon(
                            imageVector = Icons.Outlined.MoreVert,
                            contentDescription = "More",
                            tint = __textPrimary,
                            modifier = Modifier
                                .size(24.dp)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) { menuExpanded = true }
                        )
                        val context = androidx.compose.ui.platform.LocalContext.current
                        ProfileDropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false },
                            isMyProfile = true,
                            onShare = {
                                val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(android.content.Intent.EXTRA_TEXT, "Profile: $realName (https://owlino.app/u/${username})")
                                }
                                context.startActivity(android.content.Intent.createChooser(shareIntent, "Share Profile"))
                            },
                            onCopyLink = {
                                val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                val clip = android.content.ClipData.newPlainText("Profile Link", "https://owlino.app/u/${username}")
                                clipboard.setPrimaryClip(clip)
                                android.widget.Toast.makeText(context, "Link copied", android.widget.Toast.LENGTH_SHORT).show()
                            },
                            onEditProfile = onEditProfileClick
                        )
                    }
                }
            }
        },

        containerColor = __bgColor
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            // Profile Header Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar
                Box(modifier = Modifier.size(100.dp)) {
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .border(3.dp, androidx.compose.ui.graphics.Brush.linearGradient(
                                listOf(Color(0xFFFEDA75), Color(0xFFFA7E1E), Color(0xFFD62976), Color(0xFF962FBF), Color(0xFF4F5BD5))
                            ), CircleShape)
                            .padding(4.dp)
                            .clip(CircleShape)
                            .background(__surfaceColor),
                        contentAlignment = Alignment.Center
                    ) {
                        if (avatarUrl.isNotBlank()) {
                            AsyncImage(
                                model = avatarUrl,
                                contentDescription = "Avatar",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(if (realName.isNotBlank()) realName.take(1).uppercase() else "U", color = __accent, fontSize = 48.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF4CAF50))
                            .border(3.dp, __bgColor, CircleShape)
                            .align(Alignment.BottomEnd)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Stats Card
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(32.dp))
                        .border(1.dp, __dividerColor, RoundedCornerShape(32.dp))
                        .padding(vertical = 12.dp, horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StatItem(value = formatStat(postsCount), label = "Posts")
                    Divider(modifier = Modifier.height(24.dp).width(1.dp), color = __dividerColor)
                    StatItem(value = formatStat(followersCount), label = "Followers", modifier = Modifier.clickable { onFollowersClick() })
                    Divider(modifier = Modifier.height(24.dp).width(1.dp), color = __dividerColor)
                    StatItem(value = formatStat(followingCount), label = "Following", modifier = Modifier.clickable { onFollowersClick() })
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Name and Subtitle
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(realName, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = __textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = "Verified",
                        tint = __accent,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(badge, fontSize = 16.sp, color = __textSecondary)

                Spacer(modifier = Modifier.height(12.dp))

                // Bio
                Text(
                    text = bio,
                    fontSize = 15.sp,
                    lineHeight = 22.sp,
                    color = __textPrimary
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Location and links
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Place, contentDescription = null, tint = __textSecondary, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(location, fontSize = 14.sp, color = __textSecondary)

                    Spacer(modifier = Modifier.width(16.dp))

                    Icon(Icons.Outlined.Link, contentDescription = null, tint = __accent, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(website, fontSize = 14.sp, color = __accent, fontWeight = FontWeight.Medium)

                    Spacer(modifier = Modifier.width(16.dp))

                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF4CAF50)))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(com.example.ui.i18n.LocalTranslation.current.online, fontSize = 14.sp, color = __textSecondary)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Action Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(__surfaceColor)
                        .clickable { /* Share Profile Action */ },
                    contentAlignment = Alignment.Center
                ) {
                    Text(com.example.ui.i18n.LocalTranslation.current.shareProfile, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = __textPrimary)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(__surfaceColor)
                        .clickable { onEditProfileClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Text("Edit Profile", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = __textPrimary)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Tabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TabItem(title = "POSTS", icon = Icons.Outlined.GridView, isSelected = true)
                TabItem(title = "VIDEOS", icon = Icons.Outlined.SmartDisplay, isSelected = false)
                TabItem(title = "MEDIA", icon = Icons.Outlined.PhotoLibrary, isSelected = false)
                TabItem(title = "COURSES", icon = Icons.Outlined.School, isSelected = false)
            }
            Divider(color = __dividerColor, thickness = 1.dp)

            Spacer(modifier = Modifier.height(16.dp))

            // Search Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(44.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .border(1.dp, __dividerColor, RoundedCornerShape(22.dp))
                    .background(__bgColor)
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Search, contentDescription = null, tint = __textSecondary, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Search posts by keyword or dat", color = __textSecondary, fontSize = 14.sp)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // User Posts Grid
            if (userPosts == null) {
                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = __accent, modifier = Modifier.size(24.dp))
                }
            } else if (userPosts!!.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text("لا توجد منشورات بعد", color = __textSecondary, fontSize = 16.sp)
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val chunkedPosts = userPosts!!.chunked(3)
                    for (rowPosts in chunkedPosts) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            for (post in rowPosts) {
                                val imageUrl = post["media_url"]?.takeIf { it !is kotlinx.serialization.json.JsonNull }?.toString()?.removeSurrounding("\"")
                                    ?: post["image_url"]?.takeIf { it !is kotlinx.serialization.json.JsonNull }?.toString()?.removeSurrounding("\"")
                                
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(__dividerColor),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (imageUrl != null && imageUrl.isNotBlank()) {
                                        AsyncImage(
                                            model = imageUrl,
                                            contentDescription = "Post Image",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Outlined.FormatQuote,
                                            contentDescription = "Text Post",
                                            tint = __textSecondary,
                                            modifier = Modifier.size(32.dp)
                                        )
                                    }
                                }
                            }
                            // Fill remaining spaces in the last row to keep the grid aligned
                            val emptySpaces = 3 - rowPosts.size
                            for (i in 0 until emptySpaces) {
                                Spacer(modifier = Modifier.weight(1f).aspectRatio(1f))
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}

@Composable
fun StatItem(value: String, label: String, modifier: Modifier = Modifier) {
    val __themeConfig = com.example.ui.LocalSettingsTheme.current
    val __theme = __themeConfig.theme
    val __bgColor = __theme.bgColor
    val __surfaceColor = __theme.surfaceColor
    val __textPrimary = __theme.textPrimary
    val __textSecondary = __theme.textSecondary
    val __dividerColor = __theme.dividerColor
    val __accent = __themeConfig.accent




    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
        Text(value, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = __textPrimary)
        Text(label, fontSize = 12.sp, color = __textSecondary)
    }
}

@Composable
fun TabItem(title: String, icon: ImageVector, isSelected: Boolean, onClick: () -> Unit = {}) {
    val __themeConfig = com.example.ui.LocalSettingsTheme.current
    val __theme = __themeConfig.theme
    val __bgColor = __theme.bgColor
    val __surfaceColor = __theme.surfaceColor
    val __textPrimary = __theme.textPrimary
    val __textSecondary = __theme.textSecondary
    val __dividerColor = __theme.dividerColor
    val __accent = __themeConfig.accent

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) __accent else __textSecondary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) __accent else __textSecondary
            )
        }
        if (isSelected) {
            Box(modifier = Modifier.height(3.dp).width(60.dp).background(__accent, RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp)))
        } else {
            Box(modifier = Modifier.height(3.dp).width(60.dp).background(Color.Transparent))
        }
    }
}

@Composable
fun ProfileDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    isMyProfile: Boolean = true,
    isMuted: Boolean = false,
    isFavorite: Boolean = false,
    isBlocked: Boolean = false,
    onMuteToggle: () -> Unit = {

},
    onFavoriteToggle: () -> Unit = {},
    onBlockToggle: () -> Unit = {},
    onReport: () -> Unit = {},
    onShare: () -> Unit = {},
    onCopyLink: () -> Unit = {},
    onEditProfile: () -> Unit = {}
) {

    val __themeConfig = com.example.ui.LocalSettingsTheme.current
    val __theme = __themeConfig.theme
    val __bgColor = __theme.bgColor
    val __surfaceColor = __theme.surfaceColor
    val __textPrimary = __theme.textPrimary
    val __textSecondary = __theme.textSecondary
    val __dividerColor = __theme.dividerColor
    val __accent = __themeConfig.accent
    MaterialTheme(
        shapes = MaterialTheme.shapes.copy(extraSmall = RoundedCornerShape(16.dp))
    ) {
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = onDismissRequest,
            modifier = Modifier
                .width(240.dp)
                .background(__bgColor)
                .padding(vertical = 8.dp),
            offset = DpOffset(x = (-16).dp, y = 0.dp)
        ) {
            if (isMyProfile) {
                DropdownMenuItem(
                    text = { Text("Edit Profile", fontSize = 15.sp, fontWeight = FontWeight.Medium) },
                    leadingIcon = { Icon(Icons.Outlined.Edit, contentDescription = null, tint = __textPrimary) },
                    onClick = { onDismissRequest(); onEditProfile() },
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp)
                )
            }
            DropdownMenuItem(
                text = { Text(com.example.ui.i18n.LocalTranslation.current.shareProfile, fontSize = 15.sp, fontWeight = FontWeight.Medium) },
                leadingIcon = { Icon(Icons.Outlined.Share, contentDescription = null, tint = __textPrimary) },
                onClick = { onDismissRequest(); onShare() },
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp)
            )
            DropdownMenuItem(
                text = { Text("Copy Profile Link", fontSize = 15.sp, fontWeight = FontWeight.Medium) },
                leadingIcon = { Icon(Icons.Outlined.Link, contentDescription = null, tint = __textPrimary) },
                onClick = { onDismissRequest(); onCopyLink() },
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp)
            )
            if (isMyProfile) {
                DropdownMenuItem(
                    text = { Text(com.example.ui.i18n.LocalTranslation.current.qrCode, fontSize = 15.sp, fontWeight = FontWeight.Medium) },
                    leadingIcon = { Icon(Icons.Outlined.QrCode, contentDescription = null, tint = __textPrimary) },
                    onClick = onDismissRequest,
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp)
                )
            }
            if (!isMyProfile) {
                DropdownMenuItem(
                    text = { Text(if (isMuted) "Unmute Notifications" else "Mute Notifications", fontSize = 15.sp, fontWeight = FontWeight.Medium) },
                    leadingIcon = { Icon(if (isMuted) Icons.Outlined.NotificationsOff else Icons.Outlined.Notifications, contentDescription = null, tint = __textPrimary) },
                    onClick = { onDismissRequest(); onMuteToggle() },
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp)
                )
                DropdownMenuItem(
                    text = { Text(if (isFavorite) "Remove from\nFavorites" else "Add to\nFavorites", fontSize = 15.sp, fontWeight = FontWeight.Medium, lineHeight = 18.sp) },
                    leadingIcon = { Icon(if (isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder, contentDescription = null, tint = Color(0xFFFFC107)) },
                    onClick = { onDismissRequest(); onFavoriteToggle() },
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp)
                )
            }
            
            if (isMyProfile) {
                DropdownMenuItem(
                    text = { Text(com.example.ui.i18n.LocalTranslation.current.settings, fontSize = 15.sp, fontWeight = FontWeight.Medium) },
                    leadingIcon = { Icon(Icons.Outlined.Settings, contentDescription = null, tint = __textPrimary) },
                    onClick = onDismissRequest,
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp)
                )
            }
            
            if (!isMyProfile) {
                Divider(color = __dividerColor, thickness = 1.dp, modifier = Modifier.padding(vertical = 8.dp))
                DropdownMenuItem(
                    text = { Text(if (isBlocked) "Unblock User" else "Block User", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = Color.Red) },
                    leadingIcon = { Icon(Icons.Outlined.Block, contentDescription = null, tint = Color.Red) },
                    onClick = { onDismissRequest(); onBlockToggle() },
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp)
                )
                DropdownMenuItem(
                    text = { Text(com.example.ui.i18n.LocalTranslation.current.reportUser, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = Color.Red) },
                    leadingIcon = { Icon(Icons.Outlined.Flag, contentDescription = null, tint = Color.Red) },
                    onClick = { onDismissRequest(); onReport() },
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp)
                )
            }
        }
    }
}
