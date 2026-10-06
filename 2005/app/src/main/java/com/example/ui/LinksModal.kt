package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.*
import java.util.UUID

data class PlatformDef(val id: String, val icon: ImageVector, val label: String)

val PLATFORMS = listOf(
    PlatformDef("website", Lucide.Globe, "Website"),
    PlatformDef("twitter", Lucide.Twitter, "Lucide.Twitter / Lucide.X"),
    PlatformDef("instagram", Lucide.Instagram, "Lucide.Instagram"),
    PlatformDef("youtube", Lucide.Youtube, "YouTube"),
    PlatformDef("telegram", Lucide.Send, "Telegram"),
    PlatformDef("discord", Lucide.MessageSquare, "Discord"),
    PlatformDef("email", Lucide.Mail, "Email"),
    PlatformDef("custom", Lucide.Link, "Custom Lucide.Link")
)

fun getPlatformIcon(platformId: String): ImageVector {
    return PLATFORMS.find { it.id == platformId }?.icon ?: Lucide.Link
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LinksModal(
    isOpen: Boolean,
    onClose: () -> Unit,
    links: List<ProfileLink>,
    onLinksChange: (List<ProfileLink>) -> Unit,
    isOwner: Boolean
) {
    if (!isOpen) return
    
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
    var isEditing by remember { mutableStateOf(false) }
    var editingLink by remember { mutableStateOf<ProfileLink?>(null) }
    
    ModalBottomSheet(
        onDismissRequest = onClose,
        sheetState = sheetState,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = null,
        modifier = Modifier.padding(top = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (editingLink != null) {
                        if (editingLink!!.id.startsWith("link-") && links.none { it.id == editingLink!!.id }) "Add Lucide.Link" else "Edit Lucide.Link"
                    } else if (isEditing) "Manage Links" else "Links",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF111827)
                )
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (editingLink == null && isOwner) {
                        Text(
                            text = if (isEditing) "Done" else "Edit",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF3B82F6),
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { isEditing = !isEditing }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color(0xFFF3F4F6), CircleShape)
                            .clickable {
                                if (editingLink != null) {
                                    editingLink = null
                                } else {
                                    onClose()
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Lucide.X, contentDescription = "Close", tint = Color(0xFF374151), modifier = Modifier.size(20.dp))
                    }
                }
            }
            
            HorizontalDivider(color = Color(0xFFF3F4F6))
            
            // Content
            Box(modifier = Modifier.weight(1f)) {
                if (editingLink != null) {
                    val link = editingLink!!
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        Text("PLATFORM", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.Gray, modifier = Modifier.padding(bottom = 6.dp))
                        
                        // Platforms grid
                        @Composable
                        fun PlatformGrid() {
                            Column {
                                for (i in PLATFORMS.indices step 4) {
                                    Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        for (j in 0 until 4) {
                                            if (i + j < PLATFORMS.size) {
                                                val p = PLATFORMS[i + j]
                                                val isSelected = link.platform == p.id
                                                Box(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .aspectRatio(1f)
                                                        .clip(RoundedCornerShape(16.dp))
                                                        .border(1.dp, if (isSelected) Color(0xFF3B82F6) else Color(0xFFE5E7EB), RoundedCornerShape(16.dp))
                                                        .background(if (isSelected) Color(0xFFEFF6FF) else Color.Transparent)
                                                        .clickable {
                                                            editingLink = link.copy(platform = p.id)
                                                        },
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                        Icon(p.icon, contentDescription = p.label, modifier = Modifier.size(24.dp).padding(bottom = 6.dp), tint = if (isSelected) Color(0xFF3B82F6) else Color.Gray)
                                                        Text(p.label, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (isSelected) Color(0xFF3B82F6) else Color.Gray)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        PlatformGrid()
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("TITLE", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.Gray, modifier = Modifier.padding(bottom = 6.dp))
                        OutlinedTextField(
                            value = link.title,
                            onValueChange = { editingLink = link.copy(title = it) },
                            placeholder = { Text("e.g. My Portfolio") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color(0xFFF9FAFB),
                                focusedBorderColor = Color(0xFF3B82F6).copy(alpha = 0.5f),
                                unfocusedBorderColor = Color(0xFFE5E7EB)
                            )
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("URL", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.Gray, modifier = Modifier.padding(bottom = 6.dp))
                        OutlinedTextField(
                            value = link.url,
                            onValueChange = { editingLink = link.copy(url = it) },
                            placeholder = { Text("https://") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color(0xFFF9FAFB),
                                focusedBorderColor = Color(0xFF3B82F6).copy(alpha = 0.5f),
                                unfocusedBorderColor = Color(0xFFE5E7EB)
                            )
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("DESCRIPTION (OPTIONAL)", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.Gray, modifier = Modifier.padding(bottom = 6.dp))
                        OutlinedTextField(
                            value = link.description,
                            onValueChange = { editingLink = link.copy(description = it) },
                            placeholder = { Text("Short description") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color(0xFFF9FAFB),
                                focusedBorderColor = Color(0xFF3B82F6).copy(alpha = 0.5f),
                                unfocusedBorderColor = Color(0xFFE5E7EB)
                            )
                        )
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = {
                                if (link.title.isNotBlank() && link.url.isNotBlank()) {
                                    val newLinks = links.toMutableList()
                                    val index = newLinks.indexOfFirst { it.id == link.id }
                                    if (index >= 0) {
                                        newLinks[index] = link
                                    } else {
                                        newLinks.add(link)
                                    }
                                    onLinksChange(newLinks)
                                    editingLink = null
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                            enabled = link.title.isNotBlank() && link.url.isNotBlank()
                        ) {
                            Icon(Lucide.Check, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Save Lucide.Link", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (links.isEmpty() && !isEditing) {
                            item {
                                Box(modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp), contentAlignment = Alignment.Center) {
                                    Text("No links added yet.", color = Color.Gray, fontSize = 15.sp)
                                }
                            }
                        } else {
                            itemsIndexed(links) { index, link ->
                                val icon = getPlatformIcon(link.platform)
                                if (isEditing) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(Color(0xFFF9FAFB), RoundedCornerShape(16.dp))
                                            .border(1.dp, Color(0xFFF3F4F6), RoundedCornerShape(16.dp))
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.padding(end = 12.dp)) {
                                            Icon(
                                                Lucide.ArrowUp, contentDescription = "Up",
                                                modifier = Modifier.size(16.dp).clickable(enabled = index > 0) {
                                                    val newLinks = links.toMutableList()
                                                    val temp = newLinks[index - 1]
                                                    newLinks[index - 1] = newLinks[index]
                                                    newLinks[index] = temp
                                                    onLinksChange(newLinks)
                                                },
                                                tint = if (index > 0) Color.Gray else Color.LightGray
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Icon(
                                                Lucide.ArrowDown, contentDescription = "Down",
                                                modifier = Modifier.size(16.dp).clickable(enabled = index < links.size - 1) {
                                                    val newLinks = links.toMutableList()
                                                    val temp = newLinks[index + 1]
                                                    newLinks[index + 1] = newLinks[index]
                                                    newLinks[index] = temp
                                                    onLinksChange(newLinks)
                                                },
                                                tint = if (index < links.size - 1) Color.Gray else Color.LightGray
                                            )
                                        }
                                        
                                        Box(
                                            modifier = Modifier.size(48.dp).background(Color.White, RoundedCornerShape(12.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(icon, contentDescription = null, tint = Color(0xFF111827), modifier = Modifier.size(24.dp))
                                        }
                                        
                                        Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                                            Text(link.title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF111827), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            Text(link.url.replace(Regex("^https?://"), ""), fontSize = 13.sp, color = Color.Gray, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        }
                                        
                                        Row(modifier = Modifier.padding(start = 8.dp)) {
                                            Box(modifier = Modifier.size(36.dp).clip(CircleShape).clickable { editingLink = link }.padding(8.dp)) {
                                                Icon(Lucide.Pencil, contentDescription = "Edit", tint = Color.Gray, modifier = Modifier.fillMaxSize())
                                            }
                                            Box(modifier = Modifier.size(36.dp).clip(CircleShape).clickable {
                                                onLinksChange(links.filter { it.id != link.id })
                                            }.padding(8.dp)) {
                                                Icon(Lucide.Trash2, contentDescription = "Delete", tint = Color.Red, modifier = Modifier.fillMaxSize())
                                            }
                                        }
                                    }
                                } else {
                                    val context = LocalContext.current
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(Color(0xFFF9FAFB), RoundedCornerShape(16.dp))
                                            .clickable {
                                                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(link.url))
                                                context.startActivity(intent)
                                            }
                                            .padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier.size(48.dp).background(Color.White, RoundedCornerShape(12.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(icon, contentDescription = null, tint = Color(0xFF111827), modifier = Modifier.size(24.dp))
                                        }
                                        Column(modifier = Modifier.weight(1f).padding(start = 16.dp)) {
                                            Text(link.title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF111827), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            Text(if (link.description.isNotBlank()) link.description else link.url.replace(Regex("^https?://"), "").removeSuffix("/"), fontSize = 13.sp, color = Color.Gray, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 2.dp))
                                        }
                                    }
                                }
                            }
                        }
                        
                        if (isEditing && editingLink == null) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 8.dp)
                                        .border(2.dp, Color(0xFFE5E7EB), RoundedCornerShape(16.dp))
                                        .clickable {
                                            editingLink = ProfileLink(id = "link-${UUID.randomUUID()}", platform = "website", title = "", url = "")
                                        }
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Lucide.Plus, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Add New Lucide.Link", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
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
