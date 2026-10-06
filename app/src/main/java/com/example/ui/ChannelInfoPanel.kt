package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.outlined.Logout
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Report
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// عنصر وسائط واحد داخل شبكة "Médias, liens et fichiers"
data class ChannelMediaItem(
    val messageId: String,
    val url: String,
    val isVideo: Boolean = false
)

// عضو من "ADMINS DE LA CHAÎNE"
data class ChannelAdminEntry(
    val name: String,
    val avatarUrl: String? = null,
    val role: String = "ADMIN" // "SUPERADMIN" أو "ADMIN"
)

// أزرق موحّد لأزرار التشغيل/الإيقاف داخل هذه اللوحة (بدل البنفسجي العام للتطبيق)
// حتى تشبه ستايل Telegram/Viber العالمي المطلوب.
private val ChannelSwitchBlue = Color(0xFF2AABEE)

/**
 * لوحة معلومات القناة/المجموعة: تنزلق بسلاسة من الجانب الأيمن للشاشة (وليس بشاشة كاملة)،
 * فوق محتوى المحادثة، بنفس روح شاشة "Médias, liens et fichiers" في Viber.
 */
@Composable
fun ChannelInfoPanel(
    name: String,
    avatarUrl: String? = null,
    isVerified: Boolean = false,
    verifiedTint: Color = Color(0xFF3B82F6),
    subscribersLabel: String? = null,
    description: String? = null,
    isPublic: Boolean = true,
    mediaItems: List<ChannelMediaItem> = emptyList(),
    admins: List<ChannelAdminEntry> = emptyList(),
    isMuted: Boolean = false,
    onMuteToggle: () -> Unit = {},
    onMediaClick: (String) -> Unit = {},
    onInvite: () -> Unit = {},
    onReport: () -> Unit = {},
    onLeave: () -> Unit = {},
    onDismiss: () -> Unit
) {
    var isVisible by remember { mutableStateOf(false) }
    var allowAdminContact by remember { mutableStateOf(true) }
    var saveToGallery by remember { mutableStateOf(false) }
    var snoozed30Days by remember { mutableStateOf(false) }
    var bioExpanded by remember { mutableStateOf(false) }
    var bioOverflows by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) { isVisible = true }

    val dismiss: () -> Unit = {
        isVisible = false
        coroutineScope.launch {
            delay(260)
            onDismiss()
        }
    }

    val scrimAlpha by animateFloatAsState(
        targetValue = if (isVisible) 0.45f else 0f,
        animationSpec = tween(300),
        label = "channelInfoScrim"
    )

    // نجبر اتجاه من اليسار لليمين لهذه اللوحة تحديداً حتى تبقى ملتصقة بيمين الشاشة
    // فعلياً دائماً، بغض النظر عن اتجاه لغة الواجهة (عربي/فرنسي).
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = scrimAlpha))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { dismiss() }
        ) {
            AnimatedVisibility(
                visible = isVisible,
                enter = slideInHorizontally(
                    initialOffsetX = { fullWidth -> fullWidth },
                    animationSpec = tween(320, easing = FastOutSlowInEasing)
                ),
                exit = slideOutHorizontally(
                    targetOffsetX = { fullWidth -> fullWidth },
                    animationSpec = tween(260, easing = FastOutLinearInEasing)
                ),
                modifier = Modifier.align(Alignment.CenterEnd)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth(0.86f)
                        .fillMaxHeight()
                        .background(SettingsColors.background)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { /* استهلاك الضغطات حتى لا تغلق اللوحة */ }
                        .verticalScroll(rememberScrollState())
                ) {
                    // ===== الرأس =====
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Retour",
                            tint = SettingsColors.textPrimary,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .clickable { dismiss() }
                                .padding(8.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = name,
                                color = SettingsColors.textPrimary,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (!subscribersLabel.isNullOrBlank()) {
                                Text(
                                    text = subscribersLabel,
                                    color = SettingsColors.textSecondary,
                                    fontSize = 13.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    // ===== الصورة الرمزية الكبيرة =====
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1.05f)
                            .background(SettingsColors.surface),
                        contentAlignment = Alignment.Center
                    ) {
                        if (avatarUrl != null) {
                            AsyncImage(
                                model = avatarUrl,
                                contentDescription = name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(96.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = name.take(1).uppercase(),
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 34.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        if (isVerified) {
                            Icon(
                                imageVector = Icons.Filled.Verified,
                                contentDescription = "Vérifié",
                                tint = verifiedTint,
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(12.dp)
                                    .size(24.dp)
                                    .background(SettingsColors.background, CircleShape)
                                    .padding(2.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    Column(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (!description.isNullOrBlank()) {
                            Text(
                                text = description,
                                color = SettingsColors.textSecondary,
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center,
                                maxLines = if (bioExpanded) Int.MAX_VALUE else 2,
                                overflow = TextOverflow.Ellipsis,
                                onTextLayout = { result -> bioOverflows = result.hasVisualOverflow }
                            )
                            if (bioOverflows || bioExpanded) {
                                Text(
                                    text = "Plus d'infos",
                                    color = SettingsColors.blueAccent,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier
                                        .padding(top = 2.dp)
                                        .clickable { bioExpanded = !bioExpanded }
                                )
                            }
                            Spacer(Modifier.height(10.dp))
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(SettingsColors.surface)
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = if (isPublic) "Chaîne publique" else "Chaîne privée",
                                color = SettingsColors.textSecondary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    // ===== كارت: Médias, liens et fichiers =====
                    SectionCard {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = mediaItems.isNotEmpty()) {
                                    mediaItems.firstOrNull()?.let { onMediaClick(it.messageId) }
                                }
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF29B6D8)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.PhotoLibrary,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(Modifier.width(14.dp))
                            Text(
                                text = "Médias, liens et fichiers",
                                color = SettingsColors.textPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "${mediaItems.size}",
                                color = SettingsColors.textSecondary,
                                fontSize = 14.sp
                            )
                        }

                        if (mediaItems.isNotEmpty()) {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(5),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(64.dp)
                                    .padding(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                                userScrollEnabled = false
                            ) {
                                items(mediaItems.take(5)) { item ->
                                    Box(
                                        modifier = Modifier
                                            .aspectRatio(1f)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(SettingsColors.background)
                                            .clickable { onMediaClick(item.messageId) }
                                    ) {
                                        AsyncImage(
                                            model = item.url,
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                        if (item.isVideo) {
                                            Icon(
                                                imageVector = Icons.Filled.PlayArrow,
                                                contentDescription = "Vidéo",
                                                tint = Color.White,
                                                modifier = Modifier.align(Alignment.Center).size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                            Spacer(Modifier.height(14.dp))
                        } else {
                            Spacer(Modifier.height(6.dp))
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    // ===== كارت: Notifications + Inviter + Admins =====
                    SectionCard {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Notifications",
                                color = SettingsColors.textPrimary,
                                fontSize = 15.sp,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "Éléments mis en évidence",
                                color = SettingsColors.textSecondary,
                                fontSize = 12.sp,
                                textAlign = TextAlign.End,
                                modifier = Modifier.widthIn(max = 90.dp).padding(end = 8.dp)
                            )
                            Switch(
                                checked = !isMuted,
                                onCheckedChange = { onMuteToggle() },
                                colors = SwitchDefaults.colors(checkedTrackColor = ChannelSwitchBlue, checkedThumbColor = Color.White, uncheckedThumbColor = Color.White),
                                modifier = Modifier.scale(0.8f)
                            )
                        }

                        ChannelInfoDivider()

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onInvite() }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(SettingsColors.background),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.PersonAdd,
                                    contentDescription = "Inviter",
                                    tint = SettingsColors.blueAccent,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(Modifier.width(14.dp))
                            Text(text = "Inviter", color = SettingsColors.textPrimary, fontSize = 15.sp)
                        }

                        if (admins.isNotEmpty()) {
                            ChannelInfoDivider()

                            Text(
                                text = "ADMINS DE LA CHAÎNE",
                                color = SettingsColors.textSecondary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                            )

                            admins.forEachIndexed { index, admin ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (admin.avatarUrl != null) {
                                            AsyncImage(
                                                model = admin.avatarUrl,
                                                contentDescription = admin.name,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize().clip(CircleShape)
                                            )
                                        } else {
                                            Text(
                                                text = admin.name.take(1).uppercase(),
                                                color = MaterialTheme.colorScheme.primary,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                    Spacer(Modifier.width(12.dp))
                                    Text(
                                        text = admin.name,
                                        color = SettingsColors.textPrimary,
                                        fontSize = 14.sp,
                                        modifier = Modifier.weight(1f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(50))
                                            .background(SettingsColors.background)
                                            .padding(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = admin.role,
                                            color = SettingsColors.textSecondary,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                                if (index != admins.lastIndex) ChannelInfoDivider()
                            }
                            Spacer(Modifier.height(6.dp))
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    // ===== كارت: التبديلات (بدون فواصل، بدون أيقونات) =====
                    SectionCard {
                        ToggleRow(
                            label = "Autoriser les admins à vous contacter",
                            checked = allowAdminContact,
                            onCheckedChange = { allowAdminContact = it }
                        )
                        ToggleRow(
                            label = "Enregistrer dans la galerie",
                            checked = saveToGallery,
                            onCheckedChange = { saveToGallery = it }
                        )
                        ToggleRow(
                            label = "Mettre en suspens pour 30 jours",
                            checked = snoozed30Days,
                            onCheckedChange = { snoozed30Days = it }
                        )
                    }

                    Spacer(Modifier.height(10.dp))

                    // ===== كارت: Signaler / Quitter (بدون أيقونات) =====
                    SectionCard {
                        Text(
                            text = "Signaler la chaîne",
                            color = SettingsColors.textPrimary,
                            fontSize = 15.sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onReport() }
                                .padding(horizontal = 16.dp, vertical = 14.dp)
                        )
                        Text(
                            text = "Quitter et supprimer",
                            color = SettingsColors.redAccent,
                            fontSize = 15.sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onLeave() }
                                .padding(horizontal = 16.dp, vertical = 14.dp)
                        )
                    }

                    Spacer(Modifier.height(24.dp))
                    Spacer(Modifier.navigationBarsPadding())
                }
            }
        }
    }
}

@Composable
private fun SectionCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White),
        content = content
    )
}

@Composable
private fun ToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = SettingsColors.textPrimary,
            fontSize = 14.sp,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedTrackColor = ChannelSwitchBlue, checkedThumbColor = Color.White, uncheckedThumbColor = Color.White),
            modifier = Modifier.scale(0.8f)
        )
    }
}

@Composable
private fun ChannelInfoDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp)
            .height(1.dp)
            .background(SettingsColors.divider)
    )
}
