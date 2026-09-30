package com.example.ui

import com.example.ui.SettingsColors

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.icons.lucide.*
import com.composables.icons.lucide.Lucide
import com.example.supabase
import com.example.ui.i18n.LocalTranslation
import io.github.jan.supabase.auth.SignOutScope
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable














@Serializable
data class DeviceSessionModel(
    val user_id: String,
    val event_type: String,
    val device_name: String,
    val os_version: String? = null,
    val location: String? = null,
    val created_at: String? = null
)



@Composable
fun LinkedDevicesScreen(onBack: () -> Unit, onNavigateToQrScanner: () -> Unit = {}) {
    val bannerBg = Color(0xFF0F172A)
    val bannerContent = Color(0xFF3B82F6)
    var activeSessions by remember { mutableStateOf<List<DeviceSessionModel>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    
    val currentDeviceName = "${Build.MANUFACTURER} ${Build.MODEL}"

    // Dialog States
    var sessionToTerminate by remember { mutableStateOf<DeviceSessionModel?>(null) }
    var showTerminateAllDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val userId = supabase.auth.currentUserOrNull()?.id
        if (userId != null) {
            try {
                val sessions = supabase.postgrest["login_sessions"]
                    .select {
                        filter { eq("user_id", userId) }
                        order("created_at", io.github.jan.supabase.postgrest.query.Order.DESCENDING)
                    }.decodeList<DeviceSessionModel>()
                    
                val active = sessions.groupBy { it.device_name }
                    .map { (_, list) -> list.first() }
                    .filter { it.event_type == "login" }
                    .toMutableList()
                
                // Ensure current device is ALWAYS shown, as the current JWT is clearly active
                val currentIdx = active.indexOfFirst { it.device_name == currentDeviceName }
                val currentSession = if (currentIdx != -1) active.removeAt(currentIdx) else DeviceSessionModel(
                    user_id = userId,
                    event_type = "login",
                    device_name = currentDeviceName,
                    location = "Current Network",
                    created_at = "Now"
                )
                
                // Put current device at the very top
                active.add(0, currentSession)
                
                activeSessions = active
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        isLoading = false
    }

    if (sessionToTerminate != null) {
        AlertDialog(
            onDismissRequest = { sessionToTerminate = null },
            title = { Text("إنهاء جلسة الجهاز", fontWeight = FontWeight.Bold) },
            text = { Text("سيتم إخفاء هذا الجهاز من قائمتك. إذا لم يكن نشطاً فعلاً، ستنتهي جلسته تلقائياً خلال مدة صلاحية التوكن.") },
            confirmButton = {
                TextButton(onClick = {
                    val session = sessionToTerminate!!
                    sessionToTerminate = null
                    coroutineScope.launch {
                        try {
                            val userId = supabase.auth.currentUserOrNull()?.id ?: return@launch
                            // Optimistic UI update
                            activeSessions = activeSessions.filter { it.device_name != session.device_name }
                            
                            // Log 'logout' event for this specific device
                            supabase.postgrest["login_sessions"].insert(
                                mapOf(
                                    "user_id" to userId,
                                    "event_type" to "logout",
                                    "device_name" to session.device_name,
                                    "os_version" to (session.os_version ?: ""),
                                    "location" to (session.location ?: "")
                                )
                            )
                        } catch (e: Exception) {
                            withContext(Dispatchers.Main) {
                                android.widget.Toast.makeText(context, "حدث خطأ أثناء إنهاء الجلسة", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }) {
                    Text("تأكيد", color = SettingsColors.redAccent)
                }
            },
            dismissButton = {
                TextButton(onClick = { sessionToTerminate = null }) {
                    Text("إلغاء", color = SettingsColors.textPrimary)
                }
            },
            containerColor = SettingsColors.surface,
            titleContentColor = SettingsColors.textPrimary,
            textContentColor = SettingsColors.textSecondary
        )
    }

    if (showTerminateAllDialog) {
        AlertDialog(
            onDismissRequest = { showTerminateAllDialog = false },
            title = { Text("تسجيل الخروج من كل الأجهزة", fontWeight = FontWeight.Bold) },
            text = { Text("سيتم تسجيل الخروج فوراً من جميع أجهزتك الأخرى، وستبقى جلستك الحالية فقط نشطة.") },
            confirmButton = {
                TextButton(onClick = {
                    showTerminateAllDialog = false
                    coroutineScope.launch {
                        try {
                            val userId = supabase.auth.currentUserOrNull()?.id ?: return@launch
                            
                            // Safe authentic sign out from other devices
                            supabase.auth.signOut(SignOutScope.OTHERS)
                            
                            // Insert logout events for other devices to sync UI logic
                            val otherSessions = activeSessions.filter { it.device_name != currentDeviceName }
                            if (otherSessions.isNotEmpty()) {
                                val logData = otherSessions.map {
                                    mapOf(
                                        "user_id" to userId,
                                        "event_type" to "logout",
                                        "device_name" to it.device_name,
                                        "os_version" to (it.os_version ?: ""),
                                        "location" to (it.location ?: "")
                                    )
                                }
                                supabase.postgrest["login_sessions"].insert(logData)
                            }
                            
                            // Update UI to keep only current device
                            activeSessions = activeSessions.filter { it.device_name == currentDeviceName }
                        } catch (e: Exception) {
                            withContext(Dispatchers.Main) {
                                android.widget.Toast.makeText(context, "حدث خطأ: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                }) {
                    Text("تأكيد", color = SettingsColors.redAccent)
                }
            },
            dismissButton = {
                TextButton(onClick = { showTerminateAllDialog = false }) {
                    Text("إلغاء", color = SettingsColors.textPrimary)
                }
            },
            containerColor = SettingsColors.surface,
            titleContentColor = SettingsColors.textPrimary,
            textContentColor = SettingsColors.textSecondary
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .background(SettingsColors.background)
    ) {
        // Top Bar
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
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onBack
                    )
            )
            Spacer(modifier = Modifier.width(14.dp))
            Text(
                text = com.example.ui.i18n.LocalTranslation.current.linkedDevices,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = SettingsColors.textPrimary
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Banner Card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(SettingsColors.cardRadius))
                    .background(bannerBg)
                    .padding(vertical = 24.dp, horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Lucide.Computer,
                    contentDescription = null,
                    tint = bannerContent,
                    modifier = Modifier.size(40.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Link Cryptvora to your desktop or tablet to trade seamlessly across all your devices.",
                    color = bannerContent,
                    fontSize = 12.sp,
                    lineHeight = 20.sp,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Link New Device
            Text(
                text = "Link New Device",
                fontSize = 12.sp, lineHeight = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = SettingsColors.textSecondary,
                modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
            )

            // Single Item Card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(SettingsColors.cardRadius))
                    .background(SettingsColors.surface)
            ) {
                DeviceActionItem(
                    icon = Lucide.ScanLine,
                    title = "Link Desktop Device",
                    subtitle = "Scan a QR code to log in on Cryptvora Web",
                    isLast = false,
                    dividerColor = SettingsColors.divider
                )
                DeviceActionItem(
                    icon = Lucide.Scan,
                    title = "مسح رمز لتسجيل دخول جهاز آخر",
                    subtitle = "قم بمسح رمز QR من الجهاز الجديد للموافقة عليه",
                    isLast = true,
                    dividerColor = SettingsColors.divider,
                    onClick = onNavigateToQrScanner
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Active Sessions
            Text(
                text = com.example.ui.i18n.LocalTranslation.current.activeSessions,
                fontSize = 12.sp, lineHeight = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = SettingsColors.textSecondary,
                modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
            )

            // Multiple Items Card
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.padding(16.dp), color = SettingsColors.blueAccent)
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(SettingsColors.cardRadius))
                        .background(SettingsColors.surface)
                ) {
                    activeSessions.forEachIndexed { index, session ->
                        val isCurrent = session.device_name == currentDeviceName
                        // Basic parsing to pick icon based on device type (desktop vs mobile)
                        val isDesktop = session.device_name.contains("mac", ignoreCase = true) || session.device_name.contains("windows", ignoreCase = true)
                        
                        val locInfo = session.location.takeIf { !it.isNullOrBlank() } ?: "Unknown Location"
                        val timeInfo = session.created_at?.substringBefore("T") ?: "Unknown Date"
                        val subtitleText = "$locInfo · $timeInfo"
                        
                        ActiveSessionItem(
                            icon = if (isDesktop) Lucide.Monitor else Lucide.Smartphone,
                            title = session.device_name,
                            subtitle = subtitleText,
                            isCurrent = isCurrent,
                            iconTint = if (isCurrent) Color(0xFF4ADE80) else Color(0xFF60A5FA),
                            iconBg = if (isCurrent) Color(0xFF064E3B) else Color(0xFF1E3A8A),
                            isLast = index == activeSessions.size - 1 && activeSessions.size == 1,
                            dividerColor = SettingsColors.divider,
                            onTerminate = {
                                sessionToTerminate = session
                            }
                        )
                    }
                    if (activeSessions.size > 1) {
                        TerminateItem(
                            dividerColor = SettingsColors.divider,
                            onClick = { showTerminateAllDialog = true }
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))

            // Blocked Devices
            Text(
                text = "Blocked Devices",
                fontSize = 12.sp, lineHeight = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = SettingsColors.textSecondary,
                modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
            )

            // Single Item Card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(SettingsColors.cardRadius))
                    .background(SettingsColors.surface)
            ) {
                DeviceActionItem(
                    icon = Lucide.Ban,
                    title = "Manage Blocked Devices",
                    subtitle = "Devices that are banned from accessing yo...",
                    isLast = true,
                    dividerColor = SettingsColors.divider
                )
            }
            
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
fun DeviceActionItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    isLast: Boolean,
    dividerColor: Color,
    onClick: () -> Unit = {}
) {        
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() }
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = SettingsColors.textSecondary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = SettingsColors.textPrimary)
                Spacer(modifier = Modifier.height(2.dp))
                Text(subtitle, fontSize = 12.sp, lineHeight = 16.sp, color = SettingsColors.textSecondary)
            }
            Icon(
                imageVector = Lucide.ChevronRight,
                contentDescription = null,
                tint = SettingsColors.textSecondary,
                modifier = Modifier.size(20.dp)
            )
        }
        if (!isLast) {
            HorizontalDivider(color = SettingsColors.divider, thickness = 1.dp, modifier = Modifier.padding(start = 56.dp))
        }
    }
}

@Composable
fun ActiveSessionItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    isCurrent: Boolean,
    iconTint: Color,
    iconBg: Color,
    isLast: Boolean,
    dividerColor: Color,
    onTerminate: () -> Unit = {}
) {        
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { if (!isCurrent) onTerminate() }
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(title, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = SettingsColors.textPrimary)
                    if (isCurrent) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFF0F172A))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "CURRENT",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF3B82F6)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(subtitle, fontSize = 12.sp, lineHeight = 16.sp, color = SettingsColors.textSecondary)
            }
            if (!isCurrent) {
                IconButton(onClick = onTerminate, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = Lucide.LogOut,
                        contentDescription = "Terminate Session",
                        tint = SettingsColors.redAccent,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
        if (!isLast) {
            HorizontalDivider(color = SettingsColors.divider, thickness = 1.dp, modifier = Modifier.padding(start = 72.dp))
        }
    }
}

@Composable
fun TerminateItem(dividerColor: Color, onClick: () -> Unit = {}) {        
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color(0xFF450A0A)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Lucide.ShieldAlert,
                contentDescription = null,
                tint = Color(0xFFEF4444),
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Text(
            text = "تسجيل الخروج من كل الجلسات الأخرى",
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFFEF4444)
        )
    }
}
