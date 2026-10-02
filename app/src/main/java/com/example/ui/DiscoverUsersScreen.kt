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
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.collectLatest
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(kotlinx.coroutines.FlowPreview::class)
@Composable
fun DiscoverUsersScreen(
    onBack: () -> Unit,
    onUserClick: (String) -> Unit,
    onBotClick: (String, String) -> Unit = { _, _ -> }
) {
    var botIdSet by remember { mutableStateOf(setOf<String>()) }
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

    val queryFlow = remember { kotlinx.coroutines.flow.MutableStateFlow(searchQuery) }
    val tabFlow = remember { kotlinx.coroutines.flow.MutableStateFlow(0) }
    
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("All", "People", "Channels", "Bots")

    LaunchedEffect(searchQuery) { queryFlow.value = searchQuery }
    LaunchedEffect(selectedTab) { tabFlow.value = selectedTab }

    LaunchedEffect(Unit) {
        kotlinx.coroutines.flow.combine(queryFlow, tabFlow) { q, tab -> Pair(q, tab) }
            .debounce(300)
            .collectLatest { (query, tab) ->
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    try {
                        if (query.isBlank()) {
                            val res = supabase.postgrest["profiles"].select {
                                filter {
                                    filterNot("id", io.github.jan.supabase.postgrest.query.filter.FilterOperator.EQ, "00000000-0000-0000-0000-000000000000")
                                    if (currentUser != null) neq("id", currentUser.id)
                                }
                                limit(20)
                            }.decodeList<Profile>()
                            searchResults = res
                        } else {
                            val profiles = mutableListOf<Profile>()
                            
                            // People
                            if (tab == 0 || tab == 1) {
                                try {
                                    val res = supabase.postgrest["profiles"].select {
                                        filter {
                                            filterNot("id", io.github.jan.supabase.postgrest.query.filter.FilterOperator.EQ, "00000000-0000-0000-0000-000000000000")
                                            ilike("full_name", "%${query}%")
                                            if (currentUser != null) neq("id", currentUser.id)
                                        }
                                        limit(20)
                                    }.decodeList<Profile>()
                                    profiles.addAll(res)
                                } catch (e: Exception) { e.printStackTrace() }
                            }
                            
                            // Channels
                            if (tab == 0 || tab == 2) {
                                try {
                                    val res = supabase.postgrest["channels"].select {
                                        filter { ilike("name", "%${query}%") }
                                        limit(20)
                                    }.decodeList<kotlinx.serialization.json.JsonObject>()
                                    
                                    res.forEach { obj ->
                                        val id = obj["id"]?.jsonPrimitive?.content ?: ""
                                        val name = obj["name"]?.jsonPrimitive?.content ?: ""
                                        val avatarUrl = obj["avatar_url"]?.jsonPrimitive?.content
                                        val username = obj["username"]?.jsonPrimitive?.content
                                        if (id.isNotBlank()) {
                                            profiles.add(Profile(id = id, fullName = name, avatarUrl = avatarUrl, username = username))
                                        }
                                    }
                                } catch (e: Exception) { e.printStackTrace() }
                            }
                            
                            // Bots
                            if (tab == 0 || tab == 3) {
                                try {
                                    // بحث آمن عبر دالة السيرفر (لا نكشف جدول bots ولا token_hash)
                                    val res = supabase.postgrest.rpc(
                                        "search_bots",
                                        buildJsonObject { put("q", query) }
                                    ).decodeList<kotlinx.serialization.json.JsonObject>()
                                    
                                    res.forEach { obj ->
                                        val id = obj["id"]?.jsonPrimitive?.content ?: ""
                                        val name = obj["name"]?.jsonPrimitive?.content ?: ""
                                        val avatarUrl = obj["avatar_url"]?.jsonPrimitive?.content
                                        val username = obj["username"]?.jsonPrimitive?.content
                                        if (id.isNotBlank()) {
                                            botIdSet = botIdSet + id
                                            profiles.add(Profile(id = id, fullName = name, avatarUrl = avatarUrl, username = username))
                                        }
                                    }
                                } catch (e: Exception) { e.printStackTrace() }
                            }
                            
                            searchResults = profiles
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
    }

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
                if (searchResults.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Text(
                                text = if (searchQuery.isEmpty()) "No users found" else "${com.example.ui.i18n.LocalTranslation.current.noResultsFoundFor} '$searchQuery'",
                                color = __textSecondary
                            )
                        }
                    }
                } else {
                    items(searchResults, key = { it.id }) { user ->
                        RealUserListItemClone(user = user, onClick = {
                            keyboardController?.hide()
                            focusManager.clearFocus()
                            if (user.id in botIdSet) onBotClick(user.id, user.fullName ?: "Bot") else onUserClick(user.id)
                        })
                    }
                }
            }
        }
    }
}

// Removed mock rendering functions
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
