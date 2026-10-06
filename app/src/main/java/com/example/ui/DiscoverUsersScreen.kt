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
import kotlinx.serialization.json.contentOrNull
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(kotlinx.coroutines.FlowPreview::class)
@Composable
fun DiscoverUsersScreen(
    onBack: () -> Unit,
    onUserClick: (String) -> Unit,
    onBotClick: (String, String) -> Unit = { _, _ -> },
    onChannelClick: (String, String) -> Unit = { _, _ -> }
) {
    var channelIdSet by remember { mutableStateOf(setOf<String>()) }
    var memberChannelIds by remember { mutableStateOf(setOf<String>()) }
    var pendingChannel by remember { mutableStateOf<Profile?>(null) }
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
                        if (query.isBlank() && tab == 2) {
                            val chs = mutableListOf<Profile>()
                            try {
                                val res = supabase.postgrest.rpc(
                                    "search_channels",
                                    buildJsonObject { put("q", "") }
                                ).decodeList<kotlinx.serialization.json.JsonObject>()
                                res.forEach { obj ->
                                    val id = obj["id"]?.jsonPrimitive?.content ?: ""
                                    if (id.isNotBlank()) {
                                        channelIdSet = channelIdSet + id
                                        if (obj["is_member"]?.jsonPrimitive?.content == "true") memberChannelIds = memberChannelIds + id
                                        chs.add(Profile(id = id, fullName = obj["title"]?.jsonPrimitive?.content ?: "", avatarUrl = obj["avatar_url"]?.jsonPrimitive?.contentOrNull, username = obj["username"]?.jsonPrimitive?.contentOrNull))
                                    }
                                }
                            } catch (e: Exception) { e.printStackTrace() }
                            searchResults = chs
                        } else if (query.isBlank()) {
                            val res = supabase.postgrest["profiles"].select {
                                filter {
                                    filterNot("id", io.github.jan.supabase.postgrest.query.filter.FilterOperator.EQ, "00000000-0000-0000-0000-000000000000")
                                    if (currentUser != null) neq("id", currentUser.id)
                                }
                                limit(20)
                            }.decodeList<Profile>()
                            // تبويب "All" بدون كتابة: نضيف أشهر القنوات أيضاً حتى يراها الناس مباشرة
                            val topChannels = mutableListOf<Profile>()
                            if (tab == 0) {
                                try {
                                    val chRes = supabase.postgrest.rpc(
                                        "search_channels",
                                        buildJsonObject { put("q", "") }
                                    ).decodeList<kotlinx.serialization.json.JsonObject>()
                                    chRes.take(8).forEach { obj ->
                                        val id = obj["id"]?.jsonPrimitive?.content ?: ""
                                        if (id.isNotBlank()) {
                                            channelIdSet = channelIdSet + id
                                            if (obj["is_member"]?.jsonPrimitive?.content == "true") memberChannelIds = memberChannelIds + id
                                            topChannels.add(Profile(id = id, fullName = obj["title"]?.jsonPrimitive?.content ?: "", avatarUrl = obj["avatar_url"]?.jsonPrimitive?.contentOrNull, username = obj["username"]?.jsonPrimitive?.contentOrNull))
                                        }
                                    }
                                } catch (e: Exception) { e.printStackTrace() }
                            }
                            searchResults = topChannels + res
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
                                    // قنوات حقيقية عبر دالة السيرفر (عامة، ويمكن لأي مستخدم الانضمام إليها)
                                    val res = supabase.postgrest.rpc(
                                        "search_channels",
                                        buildJsonObject { put("q", query) }
                                    ).decodeList<kotlinx.serialization.json.JsonObject>()
                                    res.forEach { obj ->
                                        val id = obj["id"]?.jsonPrimitive?.content ?: ""
                                        val name = obj["title"]?.jsonPrimitive?.content ?: ""
                                        val avatarUrl = obj["avatar_url"]?.jsonPrimitive?.contentOrNull
                                        val username = obj["username"]?.jsonPrimitive?.contentOrNull
                                        val isMember = obj["is_member"]?.jsonPrimitive?.content == "true"
                                        if (id.isNotBlank()) {
                                            channelIdSet = channelIdSet + id
                                            if (isMember) memberChannelIds = memberChannelIds + id
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

    pendingChannel?.let { ch ->
        AlertDialog(
            onDismissRequest = { pendingChannel = null },
            containerColor = __surfaceColor,
            title = { Text(ch.fullName ?: "Channel", color = __textPrimary) },
            text = { Text("الانضمام إلى هذه القناة ومتابعة منشوراتها؟", color = __textSecondary) },
            confirmButton = {
                TextButton(onClick = {
                    memberChannelIds = memberChannelIds + ch.id
                    pendingChannel = null
                    onChannelClick(ch.id, ch.fullName ?: "Channel")
                }) { Text("انضمام", color = __accent) }
            },
            dismissButton = { TextButton(onClick = { pendingChannel = null }) { Text("إلغاء", color = __textSecondary) } }
        )
    }

    // ---- Redesign (UI seulement) : barre du haut identique à l'accueil, onglets à pastille glissante,
    // ---- pages à balayer. La logique de recherche (queryFlow / tabFlow / requêtes) reste inchangée.
    val rdDark = __theme.isDark
    val rdX = com.example.ui.i18n.rememberExtraStrings()
    val rdPageLabels = listOf("All", "People", "Groups", "Channels", "Bots")
    // page -> ancien onglet (tabFlow) ; « Groups » n'a pas de requête existante : -1 (aucune requête lancée)
    val rdLegacyTab = intArrayOf(0, 1, -1, 2, 3)
    val rdPager = androidx.compose.foundation.pager.rememberPagerState(pageCount = { rdPageLabels.size })
    LaunchedEffect(rdPager.currentPage) {
        val lt = rdLegacyTab[rdPager.currentPage]
        if (lt >= 0) selectedTab = lt
    }
    LaunchedEffect(rdPager.isScrollInProgress) {
        if (rdPager.isScrollInProgress) keyboardController?.hide()
    }
    val rdMe = com.example.ui.GlobalAppState.liveMyProfile

    Scaffold(
        containerColor = com.example.ui.redesign.RdColors.bg(rdDark),
        contentWindowInsets = WindowInsets.systemBars
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            HomeTopBar(
                hint = com.example.ui.i18n.LocalTranslation.current.searchPlaceholder,
                avatarUrl = rdMe?.avatarUrl,
                initial = ((rdMe?.fullName?.takeIf { it.isNotBlank() } ?: rdMe?.username) ?: "").trim().take(1).uppercase().ifBlank { "H" },
                isSearching = true,
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                onSearchActivate = {},
                onCloseSearch = onBack,
                onMenuClick = {},
                onAvatarClick = {}
            )
            com.example.ui.redesign.RdTabStrip(
                labels = rdPageLabels,
                isDark = rdDark,
                progress = { rdPager.currentPage + rdPager.currentPageOffsetFraction },
                onSelect = { i ->
                    coroutineScope.launch {
                        rdPager.animateScrollToPage(
                            i,
                            animationSpec = androidx.compose.animation.core.tween(280, easing = com.example.ui.redesign.RdDecelerateEasing(1.5f))
                        )
                    }
                }
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(com.example.ui.redesign.RdColors.divider(rdDark))
            )
            androidx.compose.foundation.pager.HorizontalPager(
                state = rdPager,
                modifier = Modifier.weight(1f).fillMaxWidth()
            ) { page ->
                // Les résultats déjà chargés sont filtrés par type pour chaque page (pas de nouvelle requête)
                val pageResults = when (page) {
                    0 -> searchResults
                    1 -> searchResults.filter { it.id !in channelIdSet && it.id !in botIdSet }
                    2 -> emptyList()
                    3 -> searchResults.filter { it.id in channelIdSet }
                    else -> searchResults.filter { it.id in botIdSet }
                }
                val listState = androidx.compose.foundation.lazy.rememberLazyListState()
                LaunchedEffect(listState.isScrollInProgress) {
                    if (listState.isScrollInProgress) keyboardController?.hide()
                }
                LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
                    if (pageResults.isEmpty()) {
                        item {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(top = 90.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(80.dp)
                                        .clip(CircleShape)
                                        .background(com.example.ui.redesign.RdColors.chip(rdDark))
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(rdX.rdNoResults, color = __textPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = if (searchQuery.isEmpty()) rdX.rdTryDifferent else "${com.example.ui.i18n.LocalTranslation.current.noResultsFoundFor} '$searchQuery'",
                                    color = __textSecondary,
                                    fontSize = 15.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    } else {
                        item {
                            Text(
                                text = if (searchQuery.isEmpty()) rdX.rdSearchRemnants else rdX.rdSearchResults,
                                fontSize = 14.sp,
                                color = __textSecondary,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 8.dp)
                            )
                        }
                        items(pageResults, key = { it.id }) { user ->
                            RealUserListItemClone(
                                user = user,
                                kindLabel = when {
                                    user.id in botIdSet -> rdX.rdKindBot
                                    user.id in channelIdSet -> rdX.rdKindChannel
                                    else -> null
                                },
                                onClick = {
                                    keyboardController?.hide()
                                    focusManager.clearFocus()
                                    if (user.id in botIdSet) onBotClick(user.id, user.fullName ?: "Bot")
                                    else if (user.id in channelIdSet) {
                                        if (user.id in memberChannelIds) onChannelClick(user.id, user.fullName ?: "Channel") else pendingChannel = user
                                    } else onUserClick(user.id)
                                }
                            )
                        }
                        item { Spacer(modifier = Modifier.height(24.dp)) }
                    }
                }
            }
        }
    }
}

// Removed mock rendering functions
@Composable
fun RealUserListItemClone(user: Profile, onClick: () -> Unit, kindLabel: String? = null) {
    val __themeConfig = com.example.ui.LocalSettingsTheme.current
    val __theme = __themeConfig.theme
    val __bgColor = __theme.bgColor
    val __textPrimary = __theme.textPrimary
    val __textSecondary = __theme.textSecondary
    val rdDark = __theme.isDark

    // Séparateur inséré à 72dp comme dans l'écran de référence
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 72.dp, end = 16.dp)
            .height(1.dp)
            .background(com.example.ui.redesign.RdColors.divider(rdDark))
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(46.dp)) {
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
                    Text(user.displayName.take(1), color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = user.displayName,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Medium,
                    color = __textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (user.isVerified == true) {
                    Spacer(modifier = Modifier.width(6.dp))
                    com.example.ui.VerifiedBadge(isVerified = true, iconSize = 16.dp)
                }
            }
            Spacer(modifier = Modifier.height(3.dp))
            val handle = if (user.displayUsername.isNotBlank()) "@${user.displayUsername}" else "User..."
            Text(
                text = if (kindLabel != null) "$handle · $kindLabel" else handle,
                fontSize = 14.5.sp,
                color = __textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        com.example.ui.redesign.RdGlyph(
            kind = com.example.ui.redesign.RdGlyphKind.CHEV_RIGHT,
            color = Color(0xFF8E8E93),
            modifier = Modifier.size(20.dp)
        )
    }
}
