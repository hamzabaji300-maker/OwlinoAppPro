@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.example.ui

import androidx.compose.animation.*
import androidx.compose.material.icons.outlined.SentimentSatisfiedAlt
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.*
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.outlined.*
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.DoneAll
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SentimentSatisfied
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material.icons.outlined.Wallpaper
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.composables.icons.lucide.*
import com.composables.icons.lucide.Lucide
import com.example.ui.i18n.LocalTranslation
import com.example.ui.theme.*
import kotlin.math.roundToInt
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

// ===== مقتطفات حرفية من ChatDetailScreen.kt الأصلي (التصميم كما هو) =====

@Composable
fun ChatHeaderMenu(
    chatType: String?,
    isMuted: Boolean,
    isPinned: Boolean,
    isArchived: Boolean,
    expanded: Boolean,
    onAction: (String) -> Unit,
    onClose: () -> Unit,
    isBlocked: Boolean = false
) {
    val menuTr = LocalTranslation.current
    var menuState by remember(expanded) { mutableStateOf(if (chatType == "channel") "more" else "main") }
    var previousState by remember(expanded) { mutableStateOf(menuState) }

    MaterialTheme(
        shapes = MaterialTheme.shapes.copy(extraSmall = RoundedCornerShape(14.dp)),
        colorScheme = MaterialTheme.colorScheme.copy(surfaceTint = Color.Transparent, surface = com.example.ui.SettingsColors.surface)
    ) {
        androidx.compose.material3.DropdownMenu(
            expanded = expanded,
            onDismissRequest = onClose,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .background(com.example.ui.SettingsColors.surface)
                .width(180.dp)
        ) {
            androidx.compose.animation.AnimatedContent(
                targetState = menuState,
                transitionSpec = {
                    if (targetState != "main" && initialState == "main") {
                        (androidx.compose.animation.slideInHorizontally(animationSpec = tween(150)) { it } + androidx.compose.animation.fadeIn(animationSpec = tween(150))) .togetherWith(
                        androidx.compose.animation.slideOutHorizontally(animationSpec = tween(150)) { -it } + androidx.compose.animation.fadeOut(animationSpec = tween(150)))
                    } else {
                        (androidx.compose.animation.slideInHorizontally(animationSpec = tween(150)) { -it } + androidx.compose.animation.fadeIn(animationSpec = tween(150))) .togetherWith(
                        androidx.compose.animation.slideOutHorizontally(animationSpec = tween(150)) { it } + androidx.compose.animation.fadeOut(animationSpec = tween(150)))
                    }
                }
            ) { state ->
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    when (state) {
                        "main" -> {
                            if (chatType != "channel") {
                                MenuRowItem(if (isMuted) "Unmute" else "Mute", if (isMuted) com.composables.icons.lucide.Lucide.BellOff else com.composables.icons.lucide.Lucide.Bell, { 
                                    if (isMuted) {
                                        onAction("mute")
                                    } else {
                                        previousState = "main"
                                        menuState = "mute"
                                    }
                                })
                                MenuRowItem(if (isPinned) "Unpin Chat" else "Pin Chat", com.composables.icons.lucide.Lucide.Pin, { onAction("pin") })
                                MenuRowItem("Favorites", com.composables.icons.lucide.Lucide.Star, { onAction("favorites") })
                                MenuRowItem(if (isArchived) "Unarchive" else "Archive", com.composables.icons.lucide.Lucide.Archive, { onAction("archive") })
                                MenuRowItem("Clear History", com.composables.icons.lucide.Lucide.Eraser, { onAction("clear_history") }, tint = Color(0xFFF59E0B))
                                MenuRowItem("Delete Chat", com.composables.icons.lucide.Lucide.Trash2, { onAction("delete_chat") }, tint = Color(0xFFEF4444))
                                MenuRowItem("More", androidx.compose.material.icons.Icons.Default.MoreHoriz, { 
                                    previousState = "main"
                                    menuState = "more" 
                                })
                                MenuRowItem(
                                    text = if (isBlocked) menuTr.unblock else menuTr.block,
                                    icon = if (isBlocked) com.composables.icons.lucide.Lucide.UserCheck else com.composables.icons.lucide.Lucide.Ban,
                                    onClick = { onAction("block") },
                                    tint = if (isBlocked) null else Color(0xFFEF4444)
                                )
                            }
                        }
                        "more" -> {
                            MenuRowItem("Photos", com.composables.icons.lucide.Lucide.Image, { onAction("photos") })
                            MenuRowItem("Videos", com.composables.icons.lucide.Lucide.Video, { onAction("videos") })
                            MenuRowItem("Documents", com.composables.icons.lucide.Lucide.FileText, { onAction("documents") })
                            MenuRowItem("Voice", com.composables.icons.lucide.Lucide.Mic, { onAction("voice") })
                            MenuRowItem("Links", com.composables.icons.lucide.Lucide.Link2, { onAction("links") })
                            MenuRowItem("Shared Files", com.composables.icons.lucide.Lucide.Folder, { onAction("files") })
                            MenuRowItem("Wallpaper", com.composables.icons.lucide.Lucide.Image, { onAction("wallpaper") })
                            
                            if (chatType != "channel") {
                                MenuRowItem("Back", com.composables.icons.lucide.Lucide.ArrowLeft, { 
                                    previousState = "more"
                                    menuState = "main" 
                                })
                            }
                        }
                        "mute" -> {
                            MenuRowItem("1 hour", com.composables.icons.lucide.Lucide.BellOff, { onAction("mute_1h") })
                            MenuRowItem("8 hours", com.composables.icons.lucide.Lucide.BellOff, { onAction("mute_8h") })
                            MenuRowItem("24 hours", com.composables.icons.lucide.Lucide.BellOff, { onAction("mute_24h") })
                            MenuRowItem("Always", com.composables.icons.lucide.Lucide.BellOff, { onAction("mute_forever") })
                            
                            MenuRowItem("Back", com.composables.icons.lucide.Lucide.ArrowLeft, { 
                                previousState = "mute"
                                menuState = "main" 
                            })
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun MenuRowItem(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit, tint: Color? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = text, tint = tint ?: MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Text(text, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = tint ?: MaterialTheme.colorScheme.onSurface)
    }
}


@Composable
fun FloatingTopBar(
    name: String, avatarUrl: String? = null, isChannel: Boolean = false,
    subscriberCount: Int? = null,
    isTyping: Boolean = false, isOnline: Boolean = false, lastSeen: String? = null, statusOverride: String? = null, isBlocked: Boolean = false, onBack: () -> Unit,
    activeFilter: String? = null, onFilterChange: (String?) -> Unit = {}, onSearch: () -> Unit = {},
    menuExpanded: Boolean, onMenuExpandedChange: (Boolean) -> Unit,
    onProfileClick: () -> Unit = {},
    isMuted: Boolean = false,
    isPinned: Boolean = false,
    isVerified: Boolean = false,
    verifyType: VerifyType = VerifyType.NONE,
    onMuteToggle: () -> Unit = {},
    onPinToggle: () -> Unit = {},
    isSearchMode: Boolean = false,
    onSearchModeChange: (Boolean) -> Unit = {},
    searchQuery: String = "",
    onSearchQueryChange: (String) -> Unit = {},
    onBlockToggle: () -> Unit = {},
    blockedByThem: Boolean = false,
    isArchived: Boolean = false,
    onArchiveToggle: () -> Unit = {},
    onDeleteChat: () -> Unit = {},
    onClearHistory: () -> Unit = {}
) {
    val isDark by com.example.ThemeManager.isDarkMode.collectAsState()
    val glassBg = com.example.ui.SettingsColors.surface
    val glassBorder = if (isDark) androidx.compose.ui.graphics.Color.White.copy(alpha = 0.05f) else androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.05f)
    val textColor = if (isDark) androidx.compose.ui.graphics.Color.White else androidx.compose.ui.graphics.Color(0xFF111827)
    val topBarIconColor = if (isDark) androidx.compose.ui.graphics.Color(0xFFE5E7EB) else androidx.compose.ui.graphics.Color(0xFF374151)
    val statusColor = if (isTyping || isOnline) androidx.compose.ui.graphics.Color(0xFF007AFF) else androidx.compose.ui.graphics.Color(0xFF6B7280)
    val context = androidx.compose.ui.platform.LocalContext.current

  if (isSearchMode) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .background(androidx.compose.ui.graphics.Color.Transparent)
        .padding(top = 8.dp, start = 8.dp, end = 8.dp, bottom = 8.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = glassBg,
            border = androidx.compose.foundation.BorderStroke(1.dp, glassBorder),
            modifier = Modifier.fillMaxWidth().height(46.dp),
            shadowElevation = 0.dp,
            tonalElevation = 0.dp
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 12.dp)) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Close Search", modifier = Modifier.clickable { onSearchModeChange(false); onSearchQueryChange("") }, tint = topBarIconColor)
                Spacer(modifier = Modifier.width(8.dp))
                androidx.compose.foundation.text.BasicTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    textStyle = androidx.compose.ui.text.TextStyle(color = textColor, fontSize = 16.sp),
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    decorationBox = { innerTextField ->
                        if (searchQuery.isEmpty()) {
                            Text(com.example.ui.i18n.LocalTranslation.current.searchMessages, color = textColor.copy(alpha = 0.5f), fontSize = 16.sp)
                        }
                        innerTextField()
                    }
                )
                if (searchQuery.isNotEmpty()) {
                    Icon(Icons.Outlined.Close, contentDescription = "Clear", modifier = Modifier.clickable { onSearchQueryChange("") }, tint = topBarIconColor)
                }
            }
        }
    }
  } else {

  Row(
    modifier = Modifier
      .fillMaxWidth()
      .background(androidx.compose.ui.graphics.Color.Transparent)
      .padding(top = 8.dp, start = 8.dp, end = 8.dp, bottom = 8.dp),
      verticalAlignment = Alignment.CenterVertically
  ){
       // Independent Back Button
       Surface(
          shape = CircleShape,
          color = glassBg,
          border = androidx.compose.foundation.BorderStroke(1.dp, glassBorder),
          modifier = Modifier.size(42.dp).clickable { onBack() },
          shadowElevation = 0.dp,
          tonalElevation = 0.dp
       ){
          Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Icon(
               imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowLeft,
               contentDescription = "Back",
               tint = topBarIconColor,
               modifier = Modifier.size(22.dp)
            )
          }
       }
       
       Spacer(modifier = Modifier.width(8.dp))
       
       // Independent Profile Area
       Surface(
          shape = RoundedCornerShape(24.dp),
          color = glassBg,
          border = androidx.compose.foundation.BorderStroke(1.dp, glassBorder),
          modifier = Modifier.weight(1f).height(42.dp).clickable { onProfileClick() },
          shadowElevation = 0.dp,
          tonalElevation = 0.dp
       ){
          Row(modifier = Modifier.fillMaxSize().padding(start = 4.dp, end = 16.dp), verticalAlignment = Alignment.CenterVertically) {
              // Avatar
               Box(contentAlignment = Alignment.Center, modifier = Modifier.size(34.dp)) {
                   Box(modifier = Modifier.size(34.dp).clip(CircleShape).background(androidx.compose.ui.graphics.Color(0xFFE5E7EB))) {
                      if (avatarUrl != null) {
                          coil.compose.AsyncImage(
                            model = coil.request.ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current).data(avatarUrl).crossfade(false).build(),
                            contentDescription = "Avatar",
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                     } else {
                         Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                             Text(name.take(1).uppercase(), color = androidx.compose.ui.graphics.Color.Gray, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                         }
                     }
                   }
                   if (!isChannel && isOnline) {
                       Box(
                          modifier = Modifier
                             .align(Alignment.BottomEnd)
                             .size(11.dp)
                             .clip(CircleShape)
                             .background(if (isDark) androidx.compose.ui.graphics.Color(0xFF1C1C1D) else androidx.compose.ui.graphics.Color.White)
                       ){
                          Box(
                             modifier = Modifier
                                .padding(1.5.dp)
                                .fillMaxSize()
                                .clip(CircleShape)
                                .background(androidx.compose.ui.graphics.Color(0xFF22C55E))
                          )
                       }
                   }
               }
               
               Spacer(modifier = Modifier.width(10.dp))
               
               // Name and Status
               Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
                   Row(verticalAlignment = Alignment.CenterVertically) {
                       Text(if (blockedByThem) "-" else name, color = textColor, fontSize = 15.sp, lineHeight = 18.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                       if ((verifyType != VerifyType.NONE || isVerified) && !blockedByThem) {
                           Spacer(modifier = Modifier.width(4.dp))
                           if (verifyType != VerifyType.NONE) {
                               com.example.ui.VerifiedBadge(type = verifyType, iconSize = 16.dp)
                           } else {
                               com.example.ui.VerifiedBadge(isVerified = true, iconSize = 16.dp)
                           }
                       }
                       if (isMuted) {
                           Spacer(modifier = Modifier.width(4.dp))
                           Icon(com.composables.icons.lucide.Lucide.BellOff, contentDescription = "Muted", tint = androidx.compose.ui.graphics.Color.Gray, modifier = Modifier.size(12.dp))
                       }
                       if (isPinned) {
                           Spacer(modifier = Modifier.width(4.dp))
                           Icon(com.composables.icons.lucide.Lucide.Pin, contentDescription = "Pinned", tint = androidx.compose.ui.graphics.Color.Gray, modifier = Modifier.size(12.dp))
                       }
                   }
                   
                   val statusText = if (statusOverride != null) {
                       statusOverride
                   } else if (isChannel) {
                       val c = subscriberCount ?: 0
                       if (c == 1) "1 Subscriber" else "$c Subscribers"
                   } else if (isTyping) {
                       "typing..."
                   } else if (isOnline) {
                       "online"
                   } else {
                       "last seen recently"
                   }
                   if (!blockedByThem) {
                       Text(statusText, color = statusColor, fontSize = 12.sp, lineHeight = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                   }
               }
          }
       }
       
       Spacer(modifier = Modifier.width(8.dp))
       
       // Actions
       Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
           if (!isChannel) {
               Surface(
                  shape = CircleShape,
                  color = glassBg,
                  border = androidx.compose.foundation.BorderStroke(1.dp, glassBorder),
                  modifier = Modifier.size(42.dp).clickable { onSearch() },
                  shadowElevation = 0.dp,
                  tonalElevation = 0.dp
               ){
                  Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                      Icon(com.composables.icons.lucide.Lucide.Search, contentDescription = "Search", tint = topBarIconColor, modifier = Modifier.size(18.dp))
                  }
               }
           }
           
           Surface(
              shape = CircleShape,
              color = glassBg,
              border = androidx.compose.foundation.BorderStroke(1.dp, glassBorder),
              modifier = Modifier.size(42.dp).clickable { onMenuExpandedChange(true) },
              shadowElevation = 0.dp,
              tonalElevation = 0.dp
           ){
              Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                  Icon(imageVector = Icons.Default.MoreVert, contentDescription = "More", tint = topBarIconColor, modifier = Modifier.size(20.dp))
                  ChatHeaderMenu(
                      chatType = if (isChannel) "channel" else "chat",
                      isMuted = isMuted,
                      isPinned = isPinned,
                      isArchived = isArchived,
                      isBlocked = isBlocked,
                      expanded = menuExpanded,
                      onAction = { action ->
                          onMenuExpandedChange(false)
                          if (action == "mute" || action.startsWith("mute_")) onMuteToggle()
                          if (action == "pin") onPinToggle()
                          if (action == "block") onBlockToggle()
                          if (action == "archive") onArchiveToggle()
                          if (action == "delete_chat") onDeleteChat()
                          if (action == "clear_history") onClearHistory()
                          if (action == "favorites") onFilterChange("Favorites")
                          if (action == "photos") onFilterChange("Photos")
                          if (action == "videos") onFilterChange("Videos")
                          if (action == "documents") onFilterChange("Documents")
                          if (action == "voice") onFilterChange("Voice Messages")
                          if (action == "links") onFilterChange("Links")
                          if (action == "files") onFilterChange("Shared Files")
                      },
                      onClose = { onMenuExpandedChange(false) }
                  )
              }
           }
       }
  }

}
}

fun formatChatDateHeader(timestampMillis: Long, languageCode: String): String {
   val cal = java.util.Calendar.getInstance()
   cal.timeInMillis = timestampMillis
   val today = java.util.Calendar.getInstance()
   val yesterday = java.util.Calendar.getInstance().apply { add(java.util.Calendar.DAY_OF_YEAR, -1) }
   fun sameDay(a: java.util.Calendar, b: java.util.Calendar) =
      a.get(java.util.Calendar.YEAR) == b.get(java.util.Calendar.YEAR) &&
      a.get(java.util.Calendar.DAY_OF_YEAR) == b.get(java.util.Calendar.DAY_OF_YEAR)
   val locale = when (languageCode) {
      "ar" -> java.util.Locale("ar")
      "fr" -> java.util.Locale.FRENCH
      else -> java.util.Locale.ENGLISH
   }
   return when {
      sameDay(cal, today) -> when (languageCode) {
         "ar" -> "اليوم"
         "fr" -> "Aujourd'hui"
         else -> "Today"
      }
      sameDay(cal, yesterday) -> when (languageCode) {
         "ar" -> "أمس"
         "fr" -> "Hier"
         else -> "Yesterday"
      }
      else -> java.text.SimpleDateFormat("d MMMM", locale).format(java.util.Date(timestampMillis))
   }
}


@Composable
fun ChatMessages(
   isChannel: Boolean = false,
   isGroup: Boolean = false,
   messages: List<UiMessage>,
   hasPinnedBanner: Boolean = false,
   name: String,
   onImageClick: (String) -> Unit = {},
   onReactionSelected: (MessageModel, String) -> Unit,
   onMoreClick: () -> Unit,
   onLongClick: (MessageModel, androidx.compose.ui.geometry.Rect, Boolean) -> Unit,
   activeContextMenuMessageId: String? = null,
   onReply: (MessageModel) -> Unit,
   modifier: Modifier = Modifier,
   listState: LazyListState = rememberLazyListState(),
   selectedMessages: Set<String> = emptySet(),
   isSelectionMode: Boolean = false,
   onToggleSelect: (String) -> Unit = {},
   contentPadding: PaddingValues = PaddingValues(start = 12.dp, end = 12.dp, top = 80.dp, bottom = 90.dp),
   isTyping: Boolean = false,
   firstUnreadIndex: Int? = null,
   isNetworkFetchComplete: Boolean = false,
   deletingIds: Set<String> = emptySet(),
   savedIndex: Int = -1,
   savedOffset: Int = 0,
   highlightedMessageId: String? = null,
   onHighlightMessage: (String?) -> Unit = {},
   onInlineButtonClick: (MessageModel, InlineButton) -> Unit = { _, _ -> }
){
   val coroutineScope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current
  var initialScrollDone by remember { mutableStateOf(false) }
   // الكيبورد: نرجعو المنطق اللي كان خدام مزيان (تأكد المستخدم بلي فقاعاتو كترتفع مزيان بيه) —
   // بمجرد ما يظهر الكيبورد وكنا أصلاً فالأسفل أو آخر رسالة ديالي، كنمشيو لآخر رسالة بأنيميشن
   // سلسة. ما كنقفزوش لأول رسالة غير مقروءة إذا كان الكيبورد ظاهر (!imeVisible) باش ما
   // يبقاش يقفز للخلف فالمحادثات اللي فيها رسائل غير مقروءة.
   val imeVisible = WindowInsets.isImeVisible

  LaunchedEffect(messages.size, firstUnreadIndex, isNetworkFetchComplete, imeVisible) {
     if (messages.isNotEmpty()) {
         try {
            val target = messages.lastIndex
            if (!initialScrollDone) {
                // If we have a saved position, and the list is large enough to contain it, restore it.
                if (savedIndex != -1) {
                    if (messages.size > savedIndex) {
                        listState.scrollToItem(savedIndex, savedOffset)
                        initialScrollDone = true
                    } else if (isNetworkFetchComplete) {
                        // Fallback: network finished but list is still smaller than savedIndex (e.g. messages deleted)
                        listState.scrollToItem(target)
                        initialScrollDone = true
                    }
                    // If list is not large enough yet, we wait (do not set initialScrollDone to true).
                } else {
                    if (firstUnreadIndex != null && !imeVisible) {
                        listState.scrollToItem(firstUnreadIndex)
                    } else {
                        listState.scrollToItem(target)
                    }
                    initialScrollDone = true
                }
            } else {
                val atBottom = !listState.canScrollForward
                val isLastMessageMine = messages.lastOrNull()?.msg?.isMine == true
                if (atBottom || isLastMessageMine || imeVisible) {
                    // imeVisible مضافة: بمجرد ما يفتح الكيبورد، نرتفعو لآخر رسالة بأنيميشن.
                    listState.animateScrollToItem(target)
                }
            }
         } catch (e: Exception) {}
     }
  }

   if (messages.isEmpty()) {
       if (!isNetworkFetchComplete) {
           // Do not show empty state while loading to prevent flash
           Box(modifier = modifier.fillMaxSize().padding(contentPadding))
           return
       }
       Box(modifier = modifier.fillMaxSize().padding(contentPadding), contentAlignment = Alignment.Center) {
          Surface(
             shape = RoundedCornerShape(16.dp),
             color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f),
          ){
             Text(
               text = "لا توجد رسائل بعد",
               fontSize = 14.sp,
               color = MaterialTheme.colorScheme.onBackground,
               modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
             )
          }
       }
       return
   }

   // ==== شارة التاريخ العائمة عند التمرير (متل تيليجرام) ====
   // ملاحظة مهمة: msg.timestamp (Long) ما كيتصاوبش من البيانات الحقيقية عند تحميل الرسائل
   // القديمة من الكاش/السيرفر (كيبقى بالقيمة الافتراضية = وقت الإنشاء = "دابا")، لهذا كانت
   // الشارة كتبين "اليوم" لجميع الرسائل بلا استثناء. المصدر الصحيح والموثوق هو createdAtExact
   // (نص ISO كيتصاوب فعلياً من created_at فكل الحالات) عبر parseTimestampSafe.
   val currentLangCode by com.example.ui.i18n.TranslationManager.currentLanguageCode.collectAsState()
   val idToTimestamp = remember(messages) {
       messages.associate { it.msg.id to parseTimestampSafe(it.msg.createdAtExact) }
   }
   val topDateLabel by remember(idToTimestamp, currentLangCode) {
       derivedStateOf {
           val firstMsgItem = listState.layoutInfo.visibleItemsInfo.firstOrNull { info ->
               (info.key as? String)?.let { idToTimestamp.containsKey(it) } == true
           }
           val ts = (firstMsgItem?.key as? String)?.let { idToTimestamp[it] }
           ts?.let { formatChatDateHeader(it, currentLangCode) }
       }
   }
   var showDateBadge by remember { mutableStateOf(false) }
   LaunchedEffect(Unit) {
       snapshotFlow {
           Triple(listState.isScrollInProgress, listState.firstVisibleItemIndex, listState.firstVisibleItemScrollOffset)
       }.collectLatest {
           showDateBadge = true
           kotlinx.coroutines.delay(1200)
           showDateBadge = false
       }
   }

   Box(modifier = modifier) {
     LazyColumn(
        state = listState,
        contentPadding = contentPadding,
        verticalArrangement = if (isChannel) Arrangement.Bottom else Arrangement.Top,
        modifier = Modifier
            .fillMaxSize()
            .imeNestedScroll()
     ){
       itemsIndexed(messages, key = { _, it -> it.msg.id }, contentType = { _, _ -> "message" }) { index, uiMsg ->
          val message = uiMsg.msg
          SwipeToReplyWrapper(modifier = Modifier.animateItem().then(if (message.id in deletingIds) Modifier.dissolveOnDelete(true) else Modifier), onReply = { if (!isSelectionMode) onReply(message) }) {
             val bounds = remember { arrayOf(androidx.compose.ui.geometry.Rect.Zero) }
             val isSelected = message.id in selectedMessages
             val isHighlighted = message.id == highlightedMessageId || message.id == activeContextMenuMessageId
             val isFirstInCluster = uiMsg.isFirst
             val isLastInCluster = uiMsg.isLast
             val itemBgColor by androidx.compose.animation.animateColorAsState(
                targetValue = if (isSelected) primaryPurple.copy(alpha = 0.1f) else if (isHighlighted) primaryPurple.copy(alpha = 0.15f) else Color.Transparent,
                animationSpec = tween(durationMillis = if (isHighlighted) 300 else 1000)
             )
             Box(modifier = Modifier
                .fillMaxWidth()
                .onGloballyPositioned { coordinates -> bounds[0] = coordinates.boundsInWindow() }
                .background(itemBgColor)
                .clickable(
                     enabled = isSelectionMode,
                     onClick = { onToggleSelect(message.id) }
                )
             ){
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = if (isSelectionMode) 12.dp else 0.dp), verticalAlignment = Alignment.CenterVertically) {
                     if (isSelectionMode) {
                         Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) primaryPurple else Color.Transparent)
                                .border(1.5.dp, if (isSelected) primaryPurple else primaryPurple.copy(alpha = 0.5f), CircleShape),
                            contentAlignment = Alignment.Center
                         ){
                            if (isSelected) {
                                Icon(Icons.Outlined.Check, contentDescription = null, tint = MaterialTheme.colorScheme.onBackground, modifier = Modifier.size(16.dp))
                            }
                         }
                         Spacer(modifier = Modifier.width(12.dp))
                     }

                     Column(modifier = Modifier.weight(1f)) {
                       if (isChannel) {
                         ChannelPostCard(
                            msg = message,
                            channelName = name,
                            onImageClick = { clickedMsgId -> onImageClick(clickedMsgId) },
                            onLightLongPress = { _, _ ->
                               if (isSelectionMode) onToggleSelect(message.id)
                               else {
                                   onLongClick(message, bounds[0], false)
                               }
                            },
                            onHeavyLongPress = { _, _ ->
                               if (!isSelectionMode) {
                                   onLongClick(message, bounds[0], true)
                               }
                            }
                         )
                       } else {
                         MessageBubble(
                            msg = message,
                            showSender = isGroup,
                            onImageClick = { clickedMsgId -> onImageClick(clickedMsgId) },
                            isFirstInCluster = isFirstInCluster,
                            isLastInCluster = isLastInCluster,
                            onReplyClick = {
                               if (message.replyTo != null) {
                                   val replyIndex = messages.indexOfFirst { it.msg.id == message.replyTo?.id }
                                   if (replyIndex >= 0) {
                                       coroutineScope.launch {
                                          listState.animateScrollToItem(replyIndex)
                                          onHighlightMessage(message.replyTo.id)
                                          kotlinx.coroutines.delay(1500)
                                          onHighlightMessage(null)
                                       }
                                   }
                               }
                            },
                            onDoubleTap = {
                               onReactionSelected(message, "love")
                            },
                            onLightLongPress = { _, _ ->
                               if (isSelectionMode) onToggleSelect(message.id)
                               else {
                                   onLongClick(message, bounds[0], false)
                               }
                            },
                            onHeavyLongPress = { _, _ ->
                               if (!isSelectionMode) {
                                   onLongClick(message, bounds[0], true)
                               }
                            }
                         )
                         val inlineRows = remember(message.replyMarkup) { parseInlineKeyboard(message.replyMarkup) }
                         if (!message.isMine && inlineRows.isNotEmpty()) {
                             InlineKeyboardGrid(rows = inlineRows, onClick = { b -> onInlineButtonClick(message, b) })
                         }
                       }
                     }
                }
             }
          }
        }
        if (isTyping) {
            item {
               TypingIndicatorBubble()
            }
        }
     } 

     androidx.compose.animation.AnimatedVisibility(
        visible = showDateBadge && topDateLabel != null,
        modifier = Modifier.align(Alignment.TopCenter).padding(top = if (hasPinnedBanner) 100.dp else 64.dp),
        enter = androidx.compose.animation.fadeIn(),
        exit = androidx.compose.animation.fadeOut()
     ) {
        Surface(
           shape = RoundedCornerShape(50),
           color = Color(0xCC202020)
        ) {
           Text(
              text = topDateLabel ?: "",
              color = Color.White,
              fontSize = 10.sp,
              modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp)
           )
        }
     }
   }
}


@Composable
fun SwipeToReplyWrapper(
    modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier,
    onReply: () -> Unit,
    content: @Composable () -> Unit
) {
    val offsetX = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val density = androidx.compose.ui.platform.LocalDensity.current
    val triggerOffset = with(density) { 60.dp.toPx() }
    val view = androidx.compose.ui.platform.LocalView.current
    
    Box(
        modifier = modifier.pointerInput(Unit) {
            detectHorizontalDragGestures(
                onDragEnd = {
                    if (offsetX.value >= triggerOffset || offsetX.value <= -triggerOffset) {
                        onReply()
                        view.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP)
                    }
                    scope.launch {
                        offsetX.animateTo(0f, animationSpec = tween(300))
                    }
                },
                onDragCancel = {
                    scope.launch {
                        offsetX.animateTo(0f, animationSpec = tween(300))
                    }
                },
                onHorizontalDrag = { change, dragAmount ->
                    val newVal = offsetX.value + (dragAmount * 0.5f) // add resistance
                    scope.launch {
                        offsetX.snapTo(newVal.coerceIn(-triggerOffset * 1.5f, triggerOffset * 1.5f))
                    }
                }
            )
        },
        contentAlignment = Alignment.Center
    ) {
        // Icon in background
        Row(
            modifier = Modifier.matchParentSize(),
            horizontalArrangement = if (offsetX.value > 0) Arrangement.Start else Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val iconAlpha = (Math.abs(offsetX.value) / triggerOffset).coerceIn(0f, 1f)
            val iconScale = 0.5f + (0.5f * iconAlpha)
            Icon(
                imageVector = Icons.Default.Reply,
                contentDescription = "Reply",
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .graphicsLayer(alpha = iconAlpha, scaleX = iconScale, scaleY = iconScale),
                tint = Color.Gray
            )
        }
        
        Box(modifier = Modifier.graphicsLayer(translationX = offsetX.value)) {
            content()
        }
    }
}


@Composable
fun MessageInputBar(
    text: String,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    onAttachmentSelected: (List<android.net.Uri>, AttachmentType) -> Unit,
    replyingTo: MessageModel?,
    onCancelReply: () -> Unit,
    editingMessage: MessageModel?,
    onCancelEdit: () -> Unit,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    onOpenGifPicker: () -> Unit = {},
    closeAttachmentSignal: Int = 0,
    allowPoll: Boolean = false,
    onPollClick: () -> Unit = {},
    onAttachmentPanelToggle: (Boolean) -> Unit = {}
) {
    var isRecording by remember { mutableStateOf(false) }
    var isLocked by remember { mutableStateOf(false) }
    var slideOffsetX by remember { mutableFloatStateOf(0f) }
    var slideOffsetY by remember { mutableFloatStateOf(0f) }
    var recordingDuration by remember { mutableIntStateOf(0) }

    // Telegram-style attachment panel: swaps in place of the keyboard instead of
    // opening a floating dialog.
    var showAttachmentPanel by remember { mutableStateOf(false) }
    // إغلاق لوحة المرفقات من الخارج (عند فتح لوحة الإيموجي) حتى لا تفتحا معًا
    LaunchedEffect(closeAttachmentSignal) { if (closeAttachmentSignal > 0) showAttachmentPanel = false }
    val screenHeightDp = androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp.dp
    val minPanelHeight = screenHeightDp * 0.75f
    var capturedKeyboardHeight by remember { mutableStateOf(minPanelHeight) }
    val density = androidx.compose.ui.platform.LocalDensity.current
    val keyboardController = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
    val imeVisible = WindowInsets.isImeVisible
    val imeInsetsForPanel = WindowInsets.ime
    LaunchedEffect(Unit) {
        snapshotFlow { imeInsetsForPanel.getBottom(density) }
            .collect { imeBottomPx ->
                if (imeBottomPx > 0) {
                    val heightDp = with(density) { imeBottomPx.toDp() }
                    if (heightDp > minPanelHeight) capturedKeyboardHeight = heightDp
                }
            }
    }
    val panelHeight = if (capturedKeyboardHeight > minPanelHeight) capturedKeyboardHeight else minPanelHeight

    
    // recordingDuration بالعُشر من الثانية (لعرض 0:02,6 مثل تيليجرام)
    LaunchedEffect(isRecording || isLocked) {
        if (isRecording || isLocked) {
            recordingDuration = 0
            while (true) {
                kotlinx.coroutines.delay(100)
                recordingDuration++
            }
        }
    }
    val recLangCode by com.example.ui.i18n.TranslationManager.currentLanguageCode.collectAsState()
    val recDecimalSep = if (recLangCode == "ar" || recLangCode == "fr") "," else "."
    val recHaptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    val recContext = androidx.compose.ui.platform.LocalContext.current
    var recMediaRecorder by remember { mutableStateOf<android.media.MediaRecorder?>(null) }
    var recFile by remember { mutableStateOf<java.io.File?>(null) }
    var recStartTimeMs by remember { mutableStateOf(0L) }
    var recHasPermission by remember {
        mutableStateOf(androidx.core.content.ContextCompat.checkSelfPermission(recContext, android.Manifest.permission.RECORD_AUDIO) == android.content.pm.PackageManager.PERMISSION_GRANTED)
    }
    val recPermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { granted -> recHasPermission = granted }
    DisposableEffect(Unit) {
        onDispose {
            // نتخلص من المسجّل إن غادر المستخدم الشاشة أثناء التسجيل، بدون إرسال ملف ناقص
            try { recMediaRecorder?.stop() } catch (e: Exception) {}
            try { recMediaRecorder?.release() } catch (e: Exception) {}
            try { recFile?.delete() } catch (e: Exception) {}
        }
    }

    fun startVoiceRecording() {
        try {
            val dir = java.io.File(recContext.cacheDir, "voice_msgs").apply { mkdirs() }
            val file = java.io.File(dir, "voice_${System.currentTimeMillis()}.m4a")
            @Suppress("DEPRECATION")
            val recorder = if (android.os.Build.VERSION.SDK_INT >= 31) android.media.MediaRecorder(recContext) else android.media.MediaRecorder()
            recorder.setAudioSource(android.media.MediaRecorder.AudioSource.MIC)
            recorder.setOutputFormat(android.media.MediaRecorder.OutputFormat.MPEG_4)
            recorder.setAudioEncoder(android.media.MediaRecorder.AudioEncoder.AAC)
            recorder.setAudioChannels(1)            // أحادي القناة: يكفي للصوت البشري
            recorder.setAudioEncodingBitRate(32000) // 32 كيلوبت/ث: واضح للكلام وحجمه صغير
            recorder.setAudioSamplingRate(22050)
            recorder.setOutputFile(file.absolutePath)
            recorder.prepare()
            recorder.start()
            recMediaRecorder = recorder
            recFile = file
            recStartTimeMs = System.currentTimeMillis()
        } catch (e: Exception) {
            recMediaRecorder = null
            recFile = null
            isRecording = false
        }
    }

    // send=true: يوقف التسجيل ويرسله كمرفق صوتي حقيقي عبر نفس مسار رفع المرفقات
    fun finishVoiceRecording(send: Boolean) {
        val recorder = recMediaRecorder
        val file = recFile
        val elapsedMs = System.currentTimeMillis() - recStartTimeMs
        recMediaRecorder = null
        recFile = null
        try { recorder?.stop() } catch (e: Exception) {}
        try { recorder?.release() } catch (e: Exception) {}
        if (send && file != null && elapsedMs >= 300) {
            // نضع مدة التسجيل بالمللي في اسم الملف لنستخرجها لاحقًا عند بناء المرفق
            val named = java.io.File(file.parent, "voice_dur${elapsedMs}_${file.name}")
            val finalFile = if (file.renameTo(named)) named else file
            onAttachmentSelected(listOf(android.net.Uri.fromFile(finalFile)), AttachmentType.VOICE)
        } else {
            try { file?.delete() } catch (e: Exception) {}
        }
    }
    
    val isTextMode = text.trim().isNotEmpty()
    
    Column(modifier = Modifier.fillMaxWidth().background(Color.Transparent).padding(start = 8.dp, end = 8.dp, top = 4.dp, bottom = 14.dp)) {
        Box(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(com.example.ui.SettingsColors.surface)
                .border(1.dp, Color.Black.copy(alpha = 0.05f), RoundedCornerShape(28.dp))
        ) {
            // Reply / Edit preview (Inside the rounded bubble!)
            AnimatedVisibility(visible = replyingTo != null || editingMessage != null) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 8.dp, top = 8.dp, end = 8.dp, bottom = 0.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (editingMessage != null) Icons.Default.Edit else Icons.AutoMirrored.Outlined.Reply,
                            contentDescription = null,
                            tint = Color(0xFF555555),
                            modifier = Modifier.padding(horizontal = 8.dp).size(20.dp)
                        )
                        Box(modifier = Modifier.width(3.dp).height(30.dp).background(Color(0xFF555555)).clip(RoundedCornerShape(1.5.dp)))
                        Spacer(Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (editingMessage != null) "Edit Message" else if (replyingTo?.isMine == true) "You" else replyingTo?.senderName?.takeIf { it.isNotBlank() } ?: "User",
                                color = Color(0xFF555555), fontSize = 13.sp, fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = (editingMessage ?: replyingTo)?.text ?: "",
                                color = Color.Gray, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis
                            )
                        }
                        IconButton(onClick = {
                            if (editingMessage != null) onCancelEdit() else onCancelReply()
                        }, modifier = Modifier.size(40.dp)) {
                            Icon(Icons.Default.Close, "Cancel", tint = Color.Gray, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = 4.dp).heightIn(min = 48.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isRecording || isLocked) {
                    // Recording UI (بشكل تيليجرام): نقطة حمراء نابضة + المؤقت بالعُشر + "اسحب للإلغاء"
                    val dotAlpha by rememberInfiniteTransition(label = "rec_dot").animateFloat(
                        initialValue = 1f,
                        targetValue = 0.25f,
                        animationSpec = infiniteRepeatable(animation = tween(700), repeatMode = RepeatMode.Reverse),
                        label = "rec_dot_alpha"
                    )
                    Row(
                        modifier = Modifier.weight(1f).height(44.dp).padding(start = 16.dp, end = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.size(10.dp).graphicsLayer { alpha = dotAlpha }.clip(CircleShape).background(Color(0xFFEF4444)))
                        Spacer(Modifier.width(10.dp))
                        val recTotalSec = recordingDuration / 10
                        Text(
                            String.format("%d:%02d%s%d", recTotalSec / 60, recTotalSec % 60, recDecimalSep, recordingDuration % 10),
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                            maxLines = 1
                        )
                        
                        if (!isLocked) {
                            Spacer(Modifier.weight(1f))
                            Icon(Icons.AutoMirrored.Outlined.KeyboardArrowLeft, null, tint = Color.Gray, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(2.dp))
                            Text(LocalTranslation.current.slideToCancel, color = Color.Gray, fontSize = 15.sp, maxLines = 1, modifier = Modifier.offset { androidx.compose.ui.unit.IntOffset(slideOffsetX.roundToInt(), 0) })
                            Spacer(Modifier.weight(1f))
                        } else {
                            Spacer(Modifier.weight(1f))
                            IconButton(onClick = { isLocked = false; isRecording = false; slideOffsetX = 0f; slideOffsetY = 0f; finishVoiceRecording(send = false) }) {
                                Icon(Icons.Outlined.Delete, "Delete", tint = Color.Red, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                } else {
                    // Text Input UI
                    leading?.invoke()
                    IconButton(
                        onClick = {
                            if (showAttachmentPanel) {
                                showAttachmentPanel = false
                                onAttachmentPanelToggle(false)
                            } else {
                                focusManager.clearFocus()
                                keyboardController?.hide()
                                showAttachmentPanel = true
                                onAttachmentPanelToggle(true)
                            }
                        },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(androidx.compose.material.icons.Icons.Default.Add, "Attach", tint = Color(0xFF007AFF), modifier = Modifier.size(28.dp))
                    }
                    
                    Box(
                        modifier = Modifier.weight(1f).padding(vertical = 10.dp)
                    ) {
                        if (text.isEmpty()) {
                            Text("Message", color = Color.Gray, fontSize = 16.sp)
                        }
                        androidx.compose.foundation.text.BasicTextField(
                            value = text,
                            onValueChange = onTextChange,
                            modifier = Modifier
                                .fillMaxWidth()
                                .onFocusEvent { if (it.isFocused) showAttachmentPanel = false },
                            textStyle = androidx.compose.ui.text.TextStyle(
                                fontSize = 16.sp, 
                                lineHeight = 22.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            cursorBrush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary),
                            maxLines = 5
                        )
                    }
                    trailing?.invoke()
                    // زر الإيموجي (بدل GIF): يفتح لوحة الإيموجي / الصور المتحركة
                    Icon(
                        androidx.compose.material.icons.Icons.Outlined.SentimentSatisfiedAlt,
                        contentDescription = "Emoji",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .clickable { onOpenGifPicker() }
                            .padding(5.dp)
                    )
                }
                
                // Send / Mic Button
                Box(
                    modifier = Modifier.size(40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.animation.AnimatedContent(
                        targetState = isTextMode || isLocked,
                        transitionSpec = {
                            androidx.compose.animation.scaleIn() + androidx.compose.animation.fadeIn() togetherWith
                            androidx.compose.animation.scaleOut() + androidx.compose.animation.fadeOut()
                        },
                        label = "send_mic_anim"
                    ) { showSend ->
                        if (showSend) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                                .clickable {
                                    if (isTextMode) {
                                        onSend()
                                    } else if (isLocked) {
                                        isLocked = false
                                        isRecording = false
                                        finishVoiceRecording(send = true)
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.ArrowUpward, "Send", tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .offset { androidx.compose.ui.unit.IntOffset(slideOffsetX.roundToInt(), slideOffsetY.roundToInt()) }
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(if (isRecording && !isLocked) Color.Transparent else MaterialTheme.colorScheme.primary)
                                .pointerInput(Unit) {
                                    detectDragGesturesAfterLongPress(
                                        onDragStart = {
                                            if (!recHasPermission) {
                                                recPermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
                                            } else {
                                                recHaptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                                isRecording = true
                                                slideOffsetX = 0f
                                                slideOffsetY = 0f
                                                startVoiceRecording()
                                            }
                                        },
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            if (isRecording && !isLocked) {
                                                if (slideOffsetX + dragAmount.x < 0) slideOffsetX += dragAmount.x
                                                if (slideOffsetY + dragAmount.y < 0) slideOffsetY += dragAmount.y
                                                
                                                if (slideOffsetX < -150) {
                                                    isRecording = false
                                                    slideOffsetX = 0f
                                                    slideOffsetY = 0f
                                                    finishVoiceRecording(send = false)
                                                } else if (slideOffsetY < -100) {
                                                    isLocked = true
                                                    isRecording = false
                                                    slideOffsetX = 0f
                                                    slideOffsetY = 0f
                                                }
                                            }
                                        },
                                        onDragEnd = {
                                            if (isRecording && !isLocked) {
                                                isRecording = false
                                                slideOffsetX = 0f
                                                slideOffsetY = 0f
                                                finishVoiceRecording(send = true)
                                            }
                                        },
                                        onDragCancel = {
                                            if (isRecording && !isLocked) {
                                                isRecording = false
                                                slideOffsetX = 0f
                                                slideOffsetY = 0f
                                                finishVoiceRecording(send = false)
                                            }
                                        }
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Outlined.Mic, "Mic", tint = if (isRecording && !isLocked) Color.Transparent else Color.White, modifier = Modifier.size(20.dp))
                        }
                    }
                    }
                }
            }
        }

        // ===== أثناء التسجيل: دائرة الميكروفون الكبيرة + قفل التسجيل (تُرسم فوق البطاقة دون تغيير حجمها) =====
        val recScale by animateFloatAsState(
            targetValue = if (isRecording && !isLocked) 1f else 0f,
            animationSpec = spring(dampingRatio = 0.6f, stiffness = 500f),
            label = "rec_scale"
        )
        val haloScale by rememberInfiniteTransition(label = "rec_halo").animateFloat(
            initialValue = 1f,
            targetValue = 1.22f,
            animationSpec = infiniteRepeatable(animation = tween(800), repeatMode = RepeatMode.Reverse),
            label = "rec_halo_scale"
        )
        if (recScale > 0.01f) {
            // قفل التسجيل (فوق الدائرة)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = (-4).dp, y = (-68).dp)
                    .size(width = 40.dp, height = 64.dp)
                    .graphicsLayer { scaleX = recScale; scaleY = recScale; alpha = recScale.coerceIn(0f, 1f) }
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .background(Color(0xFF007AFF).copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(com.composables.icons.lucide.Lucide.Lock, contentDescription = "Lock", tint = Color(0xFF007AFF).copy(alpha = 0.65f), modifier = Modifier.size(20.dp))
            }
            // الدائرة الزرقاء الكبيرة (تتبع الإصبع)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = (-4).dp, y = (-4).dp)
                    .size(40.dp)
                    .offset { androidx.compose.ui.unit.IntOffset(slideOffsetX.roundToInt(), slideOffsetY.roundToInt()) },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .requiredSize(72.dp)
                        .graphicsLayer { scaleX = recScale * haloScale; scaleY = recScale * haloScale; alpha = 0.18f * recScale.coerceIn(0f, 1f) }
                        .clip(CircleShape)
                        .background(Color(0xFF007AFF))
                )
                Box(
                    modifier = Modifier
                        .requiredSize(72.dp)
                        .graphicsLayer { scaleX = recScale; scaleY = recScale; alpha = recScale.coerceIn(0f, 1f) }
                        .clip(CircleShape)
                        .background(Color(0xFF007AFF)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.Mic, "Mic", tint = Color.White, modifier = Modifier.size(30.dp))
                }
            }
        }
    }

        AnimatedVisibility(
            visible = showAttachmentPanel,
            enter = expandVertically(animationSpec = tween(280, easing = LinearOutSlowInEasing)) +
                    fadeIn(animationSpec = tween(220)),
            exit = shrinkVertically(animationSpec = tween(220)) +
                    fadeOut(animationSpec = tween(160))
        ) {
            AttachmentPickerPanel(
                panelHeight = panelHeight,
                onAttachmentSelected = { uris, type ->
                    onAttachmentSelected(uris, type)
                    showAttachmentPanel = false
                },
                allowPoll = allowPoll,
                onPollClick = {
                    showAttachmentPanel = false
                    onAttachmentPanelToggle(false)
                    onPollClick()
                },
            )
        }
    }

}

@Composable
fun TypingIndicatorBubble() {
  val dots = listOf(
     remember { Animatable(0f) },
      remember { Animatable(0f) },
      remember { Animatable(0f) }
  )

  LaunchedEffect(Unit) {
    dots.forEachIndexed { index, animatable ->
      launch {
         kotlinx.coroutines.delay(index * 200L)
         animatable.animateTo(
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
               animation = tween(durationMillis = 600, easing = LinearOutSlowInEasing),
               repeatMode = RepeatMode.Reverse
            )
         )
      }
    }
  }

  Row(
     modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 4.dp, horizontal = 12.dp),
     horizontalArrangement = Arrangement.Start
  ){
     Surface(
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart =
4.dp, bottomEnd = 16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shadowElevation = 1.dp
     ){
        Row(
             modifier = Modifier.padding(horizontal = 4.dp, vertical = 12.dp),
             horizontalArrangement = Arrangement.spacedBy(4.dp),
             verticalAlignment = Alignment.CenterVertically
        ){
             dots.forEach { animatable ->
               Box(
                   modifier = Modifier
                     .size(6.dp)
                     .graphicsLayer {
                        translationY = -animatable.value * 4.dp.toPx()
                     }
                     .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha =
0.5f + (animatable.value * 0.5f)), CircleShape)
               )
             }
        }
        }
    }
}


@Composable
fun ChannelBottomBar() {
    Box(modifier = androidx.compose.ui.Modifier.fillMaxWidth().padding(16.dp), contentAlignment = androidx.compose.ui.Alignment.Center) {
        Text(com.example.ui.i18n.LocalTranslation.current.muteChannel, color = androidx.compose.ui.graphics.Color.Gray)
    }
}


@Composable
fun ChatWallpaper() {
    val themeConfig = LocalSettingsTheme.current
    val chatBgColor = themeConfig.chatBackground
    val isDarkBg = chatBgColor.luminance() < 0.4f
    Box(modifier = androidx.compose.ui.Modifier.fillMaxSize().background(chatBgColor)) {
        if (isDarkBg) {
            // خلفية داكنة: اللون المختار يظهر كما هو (أسود حقيقي) والرسومات تظهر كخطوط فاتحة خافتة فوقه
            val doodleMatrix = androidx.compose.ui.graphics.ColorMatrix(
                floatArrayOf(
                    0f, 0f, 0f, 0f, 255f,
                    0f, 0f, 0f, 0f, 255f,
                    0f, 0f, 0f, 0f, 255f,
                    -0.2126f * 1.2f, -0.7152f * 1.2f, -0.0722f * 1.2f, 0f, 245f * 1.2f
                )
            )
            androidx.compose.foundation.Image(
                painter = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.chat_wallpaper_bg),
                contentDescription = "Chat Background",
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                modifier = androidx.compose.ui.Modifier.fillMaxSize(),
                colorFilter = androidx.compose.ui.graphics.ColorFilter.colorMatrix(doodleMatrix)
            )
        } else {
            androidx.compose.foundation.Image(
                painter = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.chat_wallpaper_bg),
                contentDescription = "Chat Background",
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                modifier = androidx.compose.ui.Modifier.fillMaxSize(),
                colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(
                    color = chatBgColor,
                    blendMode = androidx.compose.ui.graphics.BlendMode.Color
                )
            )
            // طبقة لون المستخدم المختار تُطبَّق فوق صورة الخلفية بشفافية أقل لتظهر الرسومات
            Box(
                modifier = androidx.compose.ui.Modifier
                    .fillMaxSize()
                    .background(chatBgColor.copy(alpha = 0.3f))
            )
        }
    }
}


fun sameSenderCluster(a: MessageModel, b: MessageModel, isGroup: Boolean): Boolean =
    a.isMine == b.isMine && (!isGroup || a.senderId == b.senderId)

/** يضيف اسم وصورة المرسل الحقيقيين لرسائل المجموعة. */
fun withSenderInfo(msg: MessageModel, isGroup: Boolean, members: Map<String, Profile>): MessageModel {
    if (!isGroup || msg.isMine) return msg
    val p = members[msg.senderId] ?: return msg.copy(senderName = "")
    val n = p.fullName?.takeIf { it.isNotBlank() } ?: p.username ?: msg.senderName
    return msg.copy(senderName = n, senderAvatarUrl = p.avatarUrl)
}

@androidx.compose.runtime.Stable
data class UiMessage(
    val msg: MessageModel,
    val isFirst: Boolean,
    val isLast: Boolean
)




