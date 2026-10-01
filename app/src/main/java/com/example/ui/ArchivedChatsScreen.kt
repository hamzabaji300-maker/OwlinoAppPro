package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.*
import com.example.data.DatabaseProvider
import com.example.data.toModel
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import io.github.jan.supabase.auth.auth
import com.example.supabase

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArchivedChatsScreen(
    navController: NavController,
    onChatClick: (String, String, Boolean) -> Unit
) {
    val context = LocalContext.current
    val db = remember { DatabaseProvider.getDatabase(context) }
    
    val archivedChats by db.chatDao().getAllChats()
        .map { list -> list.filter { it.isArchived }.map { it.toModel() } }
        .collectAsState(initial = emptyList())

    val __themeConfig = com.example.ui.LocalSettingsTheme.current
    val __theme = __themeConfig.theme
    val isDark = __theme.isDark
    
    var selectedChats by remember { mutableStateOf(emptySet<String>()) }
    val coroutineScope = rememberCoroutineScope()
    var moreMenuExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            if (selectedChats.isEmpty()) {
                TopAppBar(
                    title = { Text("Archived chats", fontWeight = FontWeight.SemiBold) },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(imageVector = Lucide.ArrowLeft, contentDescription = "Back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = __theme.bgColor,
                        titleContentColor = __theme.textPrimary,
                        navigationIconContentColor = __theme.textPrimary
                    )
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth().height(56.dp).background(__theme.bgColor).padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { selectedChats = emptySet() }) {
                            Icon(imageVector = Lucide.X, contentDescription = "Close", tint = __theme.textPrimary)
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = selectedChats.size.toString(),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = __theme.textPrimary
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { 
                            val chatsToUnarchive = selectedChats.toList()
                            selectedChats = emptySet()
                            coroutineScope.launch {
                                val myId = supabase.auth.currentUserOrNull()?.id
                                if (myId != null) {
                                    ChatLifecycleManager.setArchived(context, myId, chatsToUnarchive, false, db.chatDao())
                                }
                            }
                        }) {
                            Icon(imageVector = Lucide.Archive, contentDescription = "Unarchive", tint = __theme.textPrimary)
                        }
                        
                        IconButton(onClick = { 
                            val chatsToDelete = selectedChats.toList()
                            selectedChats = emptySet()
                            coroutineScope.launch {
                                val myId = supabase.auth.currentUserOrNull()?.id
                                if (myId != null) {
                                    ChatLifecycleManager.deleteChats(context, myId, chatsToDelete, false, db.chatDao())
                                }
                            }
                        }) {
                            Icon(imageVector = Lucide.Trash2, contentDescription = "Delete", tint = __theme.textPrimary)
                        }
                    }
                }
            }
        },
        containerColor = __theme.bgColor
    ) { padding ->
        ChatList(
            chats = archivedChats,
            onChatClick = { chatId, chatName, isChannel ->
                if (selectedChats.isNotEmpty()) {
                    selectedChats = if (chatId in selectedChats) selectedChats - chatId else selectedChats + chatId
                } else {
                    onChatClick(chatId, chatName, isChannel)
                }
            },
            contentPadding = padding,
            selectedChats = selectedChats,
            onChatLongClick = { chat, rect ->
                if (selectedChats.isEmpty()) {
                    selectedChats = selectedChats + chat.id
                } else {
                    selectedChats = if (chat.id in selectedChats) selectedChats - chat.id else selectedChats + chat.id
                }
            },
            emptyMessage = "No archived chats"
        )
    }
}
