package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@Composable
fun MutedChatsScreen(onBack: () -> Unit) {
    val t = LocalTranslation.current
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val db = com.example.data.DatabaseProvider.getDatabase(context)
    val allChatsState by db.chatDao().getAllChats().collectAsState(initial = emptyList())
    val mutedChats = allChatsState.map { it.toModel() }.filter { it.isMuted }
    val isLoading = false
    var menuChatId by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .background(SettingsColors.background)
    ) {
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
                text = t.mutedChatsTitle,
                fontSize = 17.sp,
                fontWeight = FontWeight.Medium,
                color = SettingsColors.textPrimary
            )
        }
        HorizontalDivider(color = SettingsColors.divider, thickness = 1.dp)

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp)
        ) {
            if (isLoading) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = SettingsColors.blueAccent)
                    }
                }
            } else if (mutedChats.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(SettingsColors.cardRadius))
                            .background(SettingsColors.surface)
                            .padding(vertical = 28.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = t.mutedChatsTitle,
                            color = SettingsColors.textSecondary,
                            fontSize = 15.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(SettingsColors.cardRadius))
                            .background(SettingsColors.surface)
                    ) {
                        mutedChats.forEachIndexed { index, chat ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 16.dp, end = 4.dp, top = 10.dp, bottom = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val avatarUrl = chat.avatarUrl
                                if (!avatarUrl.isNullOrBlank()) {
                                    AsyncImage(
                                        model = avatarUrl,
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.size(50.dp).clip(CircleShape).background(SettingsColors.divider)
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier.size(50.dp).clip(CircleShape).background(Color(0xFF3B82F6)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = chat.name.take(1).uppercase(),
                                            color = Color.White,
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(14.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = chat.name,
                                        color = SettingsColors.textPrimary,
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Box {
                                    IconButton(onClick = { menuChatId = chat.id }) {
                                        Icon(
                                            imageVector = Icons.Filled.MoreVert,
                                            contentDescription = null,
                                            tint = SettingsColors.textSecondary
                                        )
                                    }
                                    DropdownMenu(
                                        expanded = menuChatId == chat.id,
                                        onDismissRequest = { menuChatId = null },
                                        modifier = Modifier.background(SettingsColors.surface)
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text(t.unmuteChat, color = SettingsColors.textPrimary) },
                                            onClick = {
                                                menuChatId = null
                                                scope.launch {
                                                    val myId = supabase.auth.currentUserOrNull()?.id
                                                    if (myId != null) {
                                                        val db = com.example.data.DatabaseProvider.getDatabase(context)
                                                        val success = com.example.ui.ChatStateManager.setMuted(myId, listOf(chat.id), false, db.chatDao())
                                                        if (!success) {
                                                            android.widget.Toast.makeText(context, "خطأ في الاتصال، يرجى المحاولة لاحقاً", android.widget.Toast.LENGTH_LONG).show()
                                                        }
                                                    }
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                            if (index < mutedChats.lastIndex) {
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
