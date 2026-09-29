package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.ShowChart
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import com.example.ui.i18n.LocalTranslation
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.supabase
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class MockChatItem(
    val title: String,
    val subtitle: String,
    val avatarBg: Color,
    val avatarText: String? = null,
    val avatarIcon: ImageVector? = null,
    val hasDot: Boolean,
    val rightText: String? = null,
    val rightTextBlue: Boolean = false,
    val isChevron: Boolean = true,
    val isPlayIcon: Boolean = false
)

val mockChats = listOf(
    MockChatItem("Trading chat", "đeo Cmmocanopod torr kudin aov fuwih...", Color(0xFFD1D1D6), avatarText = "TC", hasDot = true, rightText = "08:00", rightTextBlue = true, isChevron = false, isPlayIcon = true),
    MockChatItem("Channee chats", "iui aooeco rnitry to atcle to hev da...", Color(0xFFE0F7FA), avatarText = "BTC", hasDot = false, rightText = "08:00", rightTextBlue = false, isChevron = false, isPlayIcon = false),
    MockChatItem("Goldtine chats", "Get orbnat1s youram to coocirnoo'de...", Color(0xFF2196F3), avatarIcon = Icons.Outlined.Group, hasDot = true),
    MockChatItem("Braditing chats", "Fhome|", Color(0xFF000000), avatarText = "BTC", hasDot = true),
    MockChatItem("Trading chats", "Candteadinc onined couues", Color(0xFF1976D2), avatarIcon = Icons.Outlined.ShowChart, hasDot = false),
    MockChatItem("Gendineance chats", "Sentire & oloines", Color(0xFFFF9800), avatarText = "ATS", hasDot = false),
    MockChatItem("Socialy chats", "Get couip to couts", Color(0xFF4CAF50), avatarIcon = Icons.Outlined.Group, hasDot = false),
    MockChatItem("Soetings chats", "Awor opderts", Color(0xFF2196F3), avatarIcon = Icons.Outlined.TrendingUp, hasDot = false),
    MockChatItem("Scating chats", "...", Color(0xFF9E9E9E), avatarText = "SC", hasDot = false)
)

@Composable
fun DiscoverUsersScreen(
    onBack: () -> Unit,
    onUserClick: (String) -> Unit
) {
        var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<Profile>>(emptyList()) }
    val coroutineScope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
    val __themeConfig = com.example.ui.LocalSettingsTheme.current
    val __theme = __themeConfig.theme
    val __bgColor = __theme.bgColor
    val __surfaceColor = __theme.surfaceColor
    val __textPrimary = __theme.textPrimary
    val __textSecondary = __theme.textSecondary
    val __dividerColor = __theme.dividerColor
    val __accent = __themeConfig.accent
    var searchJob by remember { mutableStateOf<Job?>(null) }
    
    val currentUser = supabase.auth.currentUserOrNull()

    LaunchedEffect(searchQuery) {
        searchJob?.cancel()
        if (searchQuery.isBlank()) {
            searchResults = emptyList()
            return@LaunchedEffect
        }
        searchJob = coroutineScope.launch {
            delay(300)
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                try {
                    val res = supabase.postgrest["profiles"]
                        .select() {
                            filter {
                                filterNot("id", io.github.jan.supabase.postgrest.query.filter.FilterOperator.EQ, "00000000-0000-0000-0000-000000000000")
                                ilike("full_name", "%${searchQuery}%")
                                if (currentUser != null) {
                                    neq("id", currentUser.id)
                                }
                            }
                            limit(20)
                        }
                        .decodeList<Profile>()
                    searchResults = res
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Chats", "Channels", "Apps", "Posts", "Media")

    Scaffold(
        containerColor = __bgColor,
        contentWindowInsets = WindowInsets.systemBars
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            
            // 1. Search Bar Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                    contentDescription = "Back",
                    tint = __textSecondary,
                    modifier = Modifier
                        .size(24.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onBack
                        )
                        .graphicsLayer { rotationZ = 180f }
                )
                Spacer(modifier = Modifier.width(12.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = __dividerColor, // Darker gray so it is clearly visible like iOS
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Search, 
                            contentDescription = "Search", 
                            tint = __textSecondary, 
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            textStyle = TextStyle(fontSize = 16.sp, color = __textPrimary),
                            cursorBrush = SolidColor(__accent),
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(
                                onSearch = { keyboardController?.hide() }
                            ),
                            decorationBox = { innerTextField ->
                                if (searchQuery.isEmpty()) {
                                    Text(com.example.ui.i18n.LocalTranslation.current.searchPlaceholder, color = __textSecondary, fontSize = 16.sp)
                                }
                                innerTextField()
                            }
                        )
                        if (searchQuery.isNotEmpty()) {
                            Box(
                                modifier = Modifier
                                    .size(18.dp)
                                    .clip(CircleShape)
                                    .background(__textSecondary) // Darker X button background
                                    .clickable { searchQuery = "" },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Close, 
                                    contentDescription = "Clear", 
                                    tint = __bgColor, 
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                    }
                }
            }
            
            // 2. Tabs Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                tabs.forEachIndexed { index, title ->
                    val isSelected = index == selectedTab
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .width(IntrinsicSize.Min)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { selectedTab = index }
                    ) {
                        Text(
                            text = title,
                            color = if (isSelected) __accent else __textSecondary,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                            fontSize = 15.sp,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .height(3.dp)
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                                    .background(__accent)
                            )
                        } else {
                            Spacer(modifier = Modifier.height(3.dp))
                        }
                    }
                }
            }
            HorizontalDivider(color = __dividerColor, thickness = 0.5.dp)
            
            // 3. Section Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Search remnants", 
                    fontSize = 13.sp, 
                    color = __textSecondary, 
                    fontWeight = FontWeight.Medium
                )
            }
            
            // 4. List Content
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                if (searchQuery.isEmpty()) {
                    if (selectedTab == 0) {
                        items(mockChats, key = { it.title }) { chat ->
                            ChatListItemClone(chat)
                        }
                    } else {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                Text("${com.example.ui.i18n.LocalTranslation.current.noResultsFoundFor} ${tabs[selectedTab].lowercase()}", color = __textSecondary)
                            }
                        }
                    }
                } else {
                    if (searchResults.isEmpty()) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                Text("${com.example.ui.i18n.LocalTranslation.current.noResultsFoundFor} '$searchQuery'", color = __textSecondary)
                            }
                        }
                    } else {
                        items(searchResults, key = { it.id }) { user ->
                            RealUserListItemClone(user = user, onClick = {
                                keyboardController?.hide()
                                focusManager.clearFocus()
                                onUserClick(user.id)
                            })
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChatListItemClone(chat: MockChatItem) {
        val __themeConfig = com.example.ui.LocalSettingsTheme.current
    val __theme = __themeConfig.theme
    val __bgColor = __theme.bgColor
    val __surfaceColor = __theme.surfaceColor
    val __textPrimary = __theme.textPrimary
    val __textSecondary = __theme.textSecondary
    val __dividerColor = __theme.dividerColor
    val __accent = __themeConfig.accent

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { /* mock click */ }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar
        Box(modifier = Modifier.size(50.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(chat.avatarBg),
                contentAlignment = Alignment.Center
            ) {
                if (chat.avatarIcon != null) {
                    Icon(chat.avatarIcon, contentDescription = null, tint = __bgColor, modifier = Modifier.size(24.dp))
                } else if (chat.avatarText != null) {
                    Text(
                        text = chat.avatarText, 
                        color = if (chat.avatarBg == Color(0xFFE0F7FA)) Color.Black else __bgColor, 
                        fontSize = 16.sp, 
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        // Text Content
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = chat.title, 
                fontSize = 16.sp, 
                fontWeight = FontWeight.SemiBold, 
                color = __textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = chat.subtitle, 
                fontSize = 14.sp, 
                color = __textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        
        Spacer(modifier = Modifier.width(8.dp))
        
        // Right Action
        if (chat.rightText != null || chat.isPlayIcon) {
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.Center) {
                if (chat.rightText != null) {
                    Text(
                        text = chat.rightText, 
                        fontSize = 12.sp, 
                        color = if (chat.rightTextBlue) __accent else __textSecondary,
                        fontWeight = FontWeight.Medium
                    )
                }
                if (chat.isPlayIcon) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier.size(22.dp).background(Color(0xFF673AB7), CircleShape), 
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = __bgColor, modifier = Modifier.size(14.dp))
                    }
                }
            }
        } else if (chat.isChevron) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = "Go",
                tint = __textSecondary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
    HorizontalDivider(
        modifier = Modifier.padding(start = 78.dp, end = 16.dp),
        color = __dividerColor,
        thickness = 0.5.dp
    )
}

@Composable
fun RealUserListItemClone(user: Profile, onClick: () -> Unit) {
    val __themeConfig = com.example.ui.LocalSettingsTheme.current
    val __theme = __themeConfig.theme
    val __bgColor = __theme.bgColor
    val __surfaceColor = __theme.surfaceColor
    val __textPrimary = __theme.textPrimary
    val __textSecondary = __theme.textSecondary
    val __dividerColor = __theme.dividerColor
    val __accent = __themeConfig.accent

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(50.dp)) {
            if (user.avatarUrl != null) {
                AsyncImage(
                    model = user.avatarUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize().clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxSize().clip(CircleShape).background(user.avatarColor),
                    contentAlignment = Alignment.Center
                ) {
                    Text(user.displayName.take(1), color = __bgColor, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = user.displayName, 
                    fontSize = 16.sp, 
                    fontWeight = FontWeight.SemiBold, 
                    color = __textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (user.isVerified == true) {
                    Spacer(modifier = Modifier.width(4.dp))
                    com.example.ui.VerifiedBadge(isVerified = true, iconSize = 16.dp)
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = if (user.displayUsername.isNotBlank()) "@${user.displayUsername}" else "User...", 
                fontSize = 14.sp, 
                color = __textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        
        Spacer(modifier = Modifier.width(8.dp))
        
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
            contentDescription = "Go",
            tint = __textSecondary,
            modifier = Modifier.size(20.dp)
        )
    }
    HorizontalDivider(
        modifier = Modifier.padding(start = 78.dp, end = 16.dp),
        color = __dividerColor,
        thickness = 0.5.dp
    )
}
