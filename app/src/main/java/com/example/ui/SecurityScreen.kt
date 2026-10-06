package com.example.ui

import com.example.ui.SettingsColors

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.*

import androidx.compose.runtime.*

import kotlinx.serialization.Serializable
import io.github.jan.supabase.postgrest.postgrest
import com.example.supabase
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.launch

import androidx.compose.ui.platform.LocalContext
import com.example.util.SecurityPreferences
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.ui.Alignment
import com.example.ui.i18n.LocalTranslation
import androidx.compose.ui.Modifier
import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.scale

private val ScreenBg = SettingsColors.LightBackground
private val CardBg = SettingsColors.LightSurface
private val TextGray = SettingsColors.LightMutedForeground
private val DividerColor = SettingsColors.LightDivider
private val BlueToggle = Color(0xFF007AFF)
private val BannerBg = Color(0xFFEFF6FF)
private val BannerText = Color(0xFF2563EB)


@Serializable
data class LoginAlertsUpdate(
    @kotlinx.serialization.SerialName("login_alerts_enabled")
    val loginAlertsEnabled: Boolean
)




@OptIn(ExperimentalMaterial3Api::class)




@Composable
fun SecurityScreen(onBack: () -> Unit) {
        val context = LocalContext.current
    val securityPrefs = remember { SecurityPreferences(context) }
    
    var isBiometricEnabled by remember { mutableStateOf(securityPrefs.isBiometricEnabled) }
    var pinCode by remember { mutableStateOf(securityPrefs.pinCode) }
    var appPassword by remember { mutableStateOf(securityPrefs.appPassword) }
    var autoLockTime by remember { mutableStateOf(securityPrefs.autoLockTime) }
    
    var showPinDialog by remember { mutableStateOf(false) }
    var tempPin by remember { mutableStateOf("") }
    
    var showPasswordDialog by remember { mutableStateOf(false) }
    var tempPassword by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()
    var loginAlertsEnabled by remember { mutableStateOf(true) }
    var isLoadingProfile by remember { mutableStateOf(true) }
    
    LaunchedEffect(Unit) {
        try {
    val userId = supabase.auth.currentSessionOrNull()?.user?.id
            if (userId != null) {
    val profile = supabase.postgrest["profiles"].select { filter { eq("id", userId) } }.decodeSingleOrNull<com.example.ui.Profile>()
                if (profile?.loginAlertsEnabled != null) {
                    loginAlertsEnabled = profile.loginAlertsEnabled
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            isLoadingProfile = false
        }
    }

    if (showPinDialog) {
        AlertDialog(
            onDismissRequest = { showPinDialog = false },
            title = { Text(com.example.ui.i18n.LocalTranslation.current.setPinCode, color = SettingsColors.textPrimary) },
            text = {
                OutlinedTextField(
                    value = tempPin,
                    onValueChange = { tempPin = it },
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedTextColor = SettingsColors.textPrimary,
                        unfocusedTextColor = SettingsColors.textPrimary,
                        focusedContainerColor = CardBg,
                        unfocusedContainerColor = CardBg
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = {
    val newPin = tempPin.takeIf { it.isNotBlank() }
                    securityPrefs.pinCode = newPin
                    pinCode = newPin
                    showPinDialog = false
                }) { Text("Save", color = BlueToggle) }
            },
            dismissButton = {
                TextButton(onClick = { showPinDialog = false }) { Text("Cancel", color = TextGray) }
            },
            containerColor = CardBg
        )
    }

    if (showPasswordDialog) {
        AlertDialog(
            onDismissRequest = { showPasswordDialog = false },
            title = { Text(com.example.ui.i18n.LocalTranslation.current.setAppPassword, color = SettingsColors.textPrimary) },
            text = {
                OutlinedTextField(
                    value = tempPassword,
                    onValueChange = { tempPassword = it },
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedTextColor = SettingsColors.textPrimary,
                        unfocusedTextColor = SettingsColors.textPrimary,
                        focusedContainerColor = CardBg,
                        unfocusedContainerColor = CardBg
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = {
    val newPass = tempPassword.takeIf { it.isNotBlank() }
                    securityPrefs.appPassword = newPass
                    appPassword = newPass
                    showPasswordDialog = false
                }) { Text("Save", color = BlueToggle) }
            },
            dismissButton = {
                TextButton(onClick = { showPasswordDialog = false }) { Text("Cancel", color = TextGray) }
            },
            containerColor = CardBg
        )
    }

    Scaffold(
        containerColor = ScreenBg,
        topBar = {
            TopAppBar(
                title = { Text("Security", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Lucide.ArrowLeft, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ScreenBg,
                    titleContentColor = SettingsColors.textPrimary,
                    navigationIconContentColor = SettingsColors.textPrimary
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            // Section 1: Info Banner
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(SettingsColors.cardRadius)).background(BannerBg)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            Lucide.ShieldCheck,
                            contentDescription = null,
                            tint = BannerText,
                            modifier = Modifier.size(20.dp).padding(top = 2.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            "Local application security settings. Login and account recovery are securely managed by your Google account.",
                            color = BannerText,
                            fontSize = 12.sp,
                            lineHeight = 20.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
            }

            // Section 2: Screen Lock
            item {
                SectionHeader("Screen Lock")
                Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(SettingsColors.cardRadius)).background(CardBg)) {
                    Column {
                        SecurityToggleRow(
                            icon = Lucide.Fingerprint,
                            title = "Biometric Lock",
                            subtitle = if (isBiometricEnabled) "Enabled" else "Disabled",
                            isChecked = isBiometricEnabled,
                            onToggle = { 
                                isBiometricEnabled = it
                                securityPrefs.isBiometricEnabled = it
                            }
                        )
                        HorizontalDivider(color = DividerColor, thickness = 0.5.dp, modifier = Modifier.padding(start = 56.dp))
                        SecurityItemRow(
                            icon = Lucide.Hash,
                            title = "PIN Code",
                            subtitle = if (pinCode != null) "Configured" else "Not configured",
                            rightText = if (pinCode != null) "Change" else "Set up",
                            onClick = { 
                                tempPin = pinCode ?: ""
                                showPinDialog = true
                            }
                        )
                        HorizontalDivider(color = DividerColor, thickness = 0.5.dp, modifier = Modifier.padding(start = 56.dp))
                        SecurityItemRow(
                            icon = Lucide.Key,
                            title = "App Password",
                            subtitle = if (appPassword != null) "Configured" else "Not configured",
                            rightText = if (appPassword != null) "Change" else "Set up",
                            onClick = {
                                tempPassword = appPassword ?: ""
                                showPasswordDialog = true
                            }
                        )
                        HorizontalDivider(color = DividerColor, thickness = 0.5.dp, modifier = Modifier.padding(start = 56.dp))
                        SecurityItemRow(
                            icon = Lucide.Timer,
                            title = "Auto Lock",
                            subtitle = autoLockTime,
                            rightText = "",
                            onClick = {
    val next = if (autoLockTime == "Immediately") "1 Minute" else "Immediately"
                                autoLockTime = next
                                securityPrefs.autoLockTime = next
                            }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
            }

            // Section 3: Advanced Security
            item {
                SectionHeader("Advanced Security")
                Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(SettingsColors.cardRadius)).background(CardBg)) {
                    Column {
                        if (!isLoadingProfile) {
                            SecurityToggleRow(
                                icon = Lucide.BellRing,
                                title = "Login Alerts",
                                subtitle = "Notify me about new app logins",
                                isChecked = loginAlertsEnabled,
                                onToggle = { newVal ->
                                    loginAlertsEnabled = newVal
                                    coroutineScope.launch {
                                        try {
    val userId = supabase.auth.currentSessionOrNull()?.user?.id
                                            if (userId != null) {
                                                supabase.postgrest["profiles"].update(LoginAlertsUpdate(newVal)) {
                                                    filter { eq("id", userId) }
                                                }
                                            }
                                        } catch(e: Exception) {
                                            e.printStackTrace()
                                            loginAlertsEnabled = !newVal // revert on error
                                        }
                                    }
                                }
                            )
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                CircularProgressIndicator(color = BlueToggle, modifier = Modifier.size(24.dp))
                            }
                        }
                        HorizontalDivider(color = DividerColor, thickness = 0.5.dp, modifier = Modifier.padding(start = 56.dp))
                        SecurityItemRow(
                            icon = Lucide.Lock,
                            title = "Two-factor authentication",
                            subtitle = "Login verification is managed by your Google account.",
                            rightText = ""
                        )
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun SectionHeader(text: String) {

    Text(
        text = text,
        color = TextGray,
        fontSize = 12.sp, lineHeight = 16.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
    )
}

@Composable
fun SecurityItemRow(icon: ImageVector, title: String, subtitle: String, rightText: String, onClick: () -> Unit = {}) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = TextGray, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = SettingsColors.textPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            if (subtitle.isNotEmpty()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(subtitle, color = TextGray, fontSize = 13.sp, lineHeight = 18.sp)
            }
        }
        if (rightText.isNotEmpty()) {
            Spacer(modifier = Modifier.width(14.dp))
            Text(rightText, color = TextGray, fontSize = 12.sp, lineHeight = 16.sp)
        }
    }
}

@Composable
fun SecurityArrowRow(icon: ImageVector, title: String) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = TextGray, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(14.dp))
        Text(title, color = SettingsColors.textPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp, modifier = Modifier.weight(1f))
        Icon(Lucide.ChevronRight, contentDescription = null, tint = TextGray, modifier = Modifier.size(16.dp))
    }
}

@Composable
fun SecurityToggleRow(icon: ImageVector, title: String, subtitle: String, isChecked: Boolean, onToggle: ((Boolean) -> Unit)? = null) {
    var localCheckedState by remember(isChecked) { mutableStateOf(isChecked) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val t = com.example.ui.i18n.LocalTranslation.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
    val newVal = !localCheckedState
                localCheckedState = newVal
                onToggle?.invoke(newVal)
                showToggleToast(context, title, newVal, t, icon)
            }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = TextGray, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = SettingsColors.textPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            if (subtitle.isNotEmpty()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(subtitle, color = TextGray, fontSize = 13.sp, lineHeight = 18.sp)
            }
        }
        Spacer(modifier = Modifier.width(14.dp))
        Switch(
            modifier = Modifier.scale(0.85f),
            checked = localCheckedState,
            onCheckedChange = { 
                localCheckedState = it
                onToggle?.invoke(it)
                showToggleToast(context, title, it, t, icon)
            },
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = SettingsColors.blueAccent,
                uncheckedThumbColor = SettingsColors.textSecondary,
                uncheckedTrackColor = SettingsColors.divider,
                uncheckedBorderColor = Color.Transparent
            )
        )
    }
}
