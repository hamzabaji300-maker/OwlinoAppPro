package com.example.ui

import com.example.ui.SettingsColors

import kotlinx.serialization.Serializable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.*

import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import com.example.ui.i18n.LocalTranslation
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.supabase
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.storage.storage
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import kotlinx.coroutines.launch








@Composable
fun EditProfileScreen(onBack: () -> Unit) {
        val coroutineScope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val textColor = SettingsColors.textPrimary
    val prefs = remember { context.getSharedPreferences("user_profile_prefs", android.content.Context.MODE_PRIVATE) }
    
    var fullName by remember { mutableStateOf("Alex Morgan") }
    var username by remember { mutableStateOf("alex.morgan") }
    var avatarUrl by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf("") }
    
    var cryptvoraId by remember { mutableStateOf("") }
    var walletAddress by remember { mutableStateOf("") }
    var chain by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }

    var isUploading by remember { mutableStateOf(false) }
    var selectedImageUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var selectedImageBytes by remember { mutableStateOf<ByteArray?>(null) }
    val imagePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            selectedImageUri = uri
            coroutineScope.launch {
                try {
                    selectedImageBytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
    val currentUser = supabase.auth.currentUserOrNull()

    LaunchedEffect(currentUser) {
        if (currentUser != null) {
    val userId = currentUser.id
    val localAvatar = prefs.getString("local_avatar_$userId", null)
            if (localAvatar != null) {
                avatarUrl = localAvatar
            }
            
            try {
    val profile = supabase.postgrest["profiles"]
                    .select { filter { eq("id", userId) } }
                    .decodeSingleOrNull<Profile>()
                if (profile != null) {
    val defaultName = currentUser?.email?.substringBefore("@") ?: "User"
                    fullName = profile.fullName.takeIf { !it.isNullOrBlank() } ?: defaultName.replaceFirstChar { it.uppercase() }
                    username = profile.username.takeIf { !it.isNullOrBlank() } ?: defaultName
                    if (localAvatar == null) {
                        avatarUrl = profile.avatarUrl ?: ""
                    }
                    bio = profile.bio ?: ""
                    cryptvoraId = profile.cryptvoraId ?: ""
                    walletAddress = profile.walletAddress ?: ""
                    chain = profile.chain ?: ""
                    email = profile.contactEmail ?: ""
                    phone = profile.contactPhone ?: ""
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isLoading = false
            }
        } else {
            isLoading = false
        }
    }
    val cardColor = SettingsColors.surface
        val borderColor = SettingsColors.divider

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
                .padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Lucide.ArrowLeft, contentDescription = "Back", tint = textColor)
            }
            Text("Edit Profile", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = SettingsColors.textPrimary)
        }
        
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = SettingsColors.blueAccent)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
            ) {
                Spacer(modifier = Modifier.height(16.dp))
                
                // Avatar Section
                Box(
                    modifier = Modifier.size(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(110.dp)
                            .clip(CircleShape)
                            .background(cardColor)
                            .clickable { 
                                imagePickerLauncher.launch(
                                    androidx.activity.result.PickVisualMediaRequest(
                                        ActivityResultContracts.PickVisualMedia.ImageOnly
                                    )
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (selectedImageUri != null) {
                            AsyncImage(
                                model = selectedImageUri,
                                contentDescription = "Avatar",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else if (avatarUrl.isNotBlank()) {
                            AsyncImage(
                                model = avatarUrl,
                                contentDescription = "Avatar",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
    val initials = fullName.split(" ").mapNotNull { it.firstOrNull()?.uppercase() }.take(2).joinToString("")
                            Text(if (initials.isNotBlank()) initials else "AM", color = SettingsColors.textPrimary, fontSize = 40.sp, fontWeight = FontWeight.SemiBold)
                        }
                        
                        // Dark overlay with Camera icon
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(textColor.copy(alpha = 0.3f)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isUploading) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = SettingsColors.background, strokeWidth = 2.dp)
                            } else {
                                Icon(Lucide.Camera, contentDescription = "Change photo", tint = SettingsColors.background, modifier = Modifier.size(32.dp))
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                
                // Name
                BasicTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    textStyle = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.Bold, color = SettingsColors.textPrimary),
                    cursorBrush = SolidColor(SettingsColors.blueAccent),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                // Username
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text("@ ", fontSize = 15.sp, color = SettingsColors.textSecondary)
                    BasicTextField(
                        value = username,
                        onValueChange = { username = it },
                        textStyle = TextStyle(fontSize = 15.sp, color = SettingsColors.textSecondary),
                        cursorBrush = SolidColor(SettingsColors.blueAccent),
                        modifier = Modifier.weight(1f)
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Action Buttons
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Save Button
                    Box(
                        modifier = Modifier
                            .weight(2f)
                            .height(52.dp)
                            .clip(RoundedCornerShape(26.dp))
                            .background(SettingsColors.blueAccent)
                            .clickable {
                                if (currentUser == null) return@clickable
                                isSaving = true
                                coroutineScope.launch {
                                    try {
    val userId = currentUser.id
                                        var finalAvatarUrl = avatarUrl
                                        if (selectedImageBytes != null) {
    val filePath = "${userId}/${java.util.UUID.randomUUID()}.jpg"
                                            try {
                                                supabase.storage["avatars"].upload(filePath, selectedImageBytes!!)
                                                finalAvatarUrl = supabase.storage["avatars"].publicUrl(filePath)
                                            } catch (e: Exception) {
                                                e.printStackTrace()
                                                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                                    android.widget.Toast.makeText(context, "Upload Error: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
                                                }
                                                isSaving = false
                                                return@launch
                                            }
                                        }



                                        if (selectedImageUri != null) {
                                            prefs.edit().putString("local_avatar_$userId", selectedImageUri.toString()).apply()
                                        }
    val updateData = ProfileUpdate(
                                            fullName = fullName.takeIf { it.isNotBlank() },
                                            username = username.takeIf { it.isNotBlank() },
                                            avatarUrl = finalAvatarUrl.takeIf { it.isNotBlank() },
                                            bio = bio,
                                            walletAddress = walletAddress,
                                            chain = chain,
                                            contactEmail = email
                                        )
                                        supabase.postgrest["profiles"].update(updateData) {
                                            filter { eq("id", userId) }
                                        }
                                        onBack()
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                        isSaving = false
                                    }
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = SettingsColors.background, strokeWidth = 2.dp)
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Lucide.Check, contentDescription = null, tint = SettingsColors.background, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Save profile", color = SettingsColors.background, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    
                    // Cancel Button
                    Box(
                        modifier = Modifier
                            .weight(1.2f)
                            .height(52.dp)
                            .clip(RoundedCornerShape(26.dp))
                            .background(SettingsColors.divider)
                            .clickable { onBack() },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Lucide.X, contentDescription = null, tint = textColor, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Cancel", color = SettingsColors.textPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Bio section
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(SettingsColors.cardRadius))
                        .background(cardColor)
                        .border(1.dp, borderColor, RoundedCornerShape(SettingsColors.cardRadius))
                        .padding(20.dp)
                ) {
                    Column {
                        BasicTextField(
                            value = bio,
                            onValueChange = { if (it.length <= 160) bio = it },
                            textStyle = TextStyle(fontSize = 15.sp, color = SettingsColors.textPrimary, lineHeight = 24.sp),
                            cursorBrush = SolidColor(SettingsColors.blueAccent),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(40.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Tap to edit", fontSize = 12.sp, lineHeight = 16.sp, color = SettingsColors.textSecondary)
                            Text("${bio.length}/160", fontSize = 12.sp, lineHeight = 16.sp, color = SettingsColors.textSecondary)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(40.dp))
                
                // Crypto Identity
                Text("CRYPTO IDENTITY", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SettingsColors.textSecondary, letterSpacing = 1.5.sp)
                Spacer(modifier = Modifier.height(16.dp))
                LightInfoField(label = "Cryptvora ID", value = cryptvoraId.takeIf { it.isNotBlank() } ?: "Not assigned yet", onValueChange = { cryptvoraId = it }, cardColor = cardColor, textColor = textColor, textSecondary = SettingsColors.textSecondary, accentColor = SettingsColors.blueAccent, readOnly = true)
                Spacer(modifier = Modifier.height(16.dp))
                LightInfoField(label = "Wallet address", value = walletAddress, onValueChange = { walletAddress = it }, cardColor = cardColor, textColor = textColor, textSecondary = SettingsColors.textSecondary, accentColor = SettingsColors.blueAccent)
                Spacer(modifier = Modifier.height(16.dp))
                LightInfoField(label = "Chain", value = chain, onValueChange = { chain = it }, cardColor = cardColor, textColor = textColor, textSecondary = SettingsColors.textSecondary, accentColor = SettingsColors.blueAccent)
                Spacer(modifier = Modifier.height(40.dp))
                
                // Contact Private
                Text("CONTACT · PRIVATE", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SettingsColors.textSecondary, letterSpacing = 1.5.sp)
                Spacer(modifier = Modifier.height(16.dp))
                LightInfoField(label = "Email", value = email, onValueChange = { email = it }, cardColor = cardColor, textColor = textColor, textSecondary = SettingsColors.textSecondary, accentColor = SettingsColors.blueAccent)
                Spacer(modifier = Modifier.height(60.dp))
            }
        }
    }
}

@Composable
fun LightInfoField(label: String, value: String, onValueChange: (String) -> Unit, cardColor: Color, textColor: Color, textSecondary: Color, accentColor: Color, readOnly: Boolean = false) {
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(SettingsColors.cardRadius))
            .background(cardColor)
            .border(1.dp, SettingsColors.divider, RoundedCornerShape(SettingsColors.cardRadius))
            .padding(horizontal = 24.dp, vertical = 20.dp)
    ) {
        Column {
            Text(label, fontSize = 12.sp, lineHeight = 16.sp, color = SettingsColors.textSecondary)
            Spacer(modifier = Modifier.height(8.dp))
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                textStyle = TextStyle(fontSize = 18.sp, color = if (readOnly) SettingsColors.textSecondary else SettingsColors.textPrimary),
                cursorBrush = SolidColor(accentColor),
                modifier = Modifier.fillMaxWidth(),
                readOnly = readOnly
            )
        }
    }
}

@Serializable
data class ProfileUpdate(
    @kotlinx.serialization.SerialName("full_name")
    val fullName: String?,
    val username: String?,
    @kotlinx.serialization.SerialName("avatar_url")
    val avatarUrl: String?,
    val bio: String?,
    @kotlinx.serialization.SerialName("wallet_address")
    val walletAddress: String?,
    val chain: String?,
    @kotlinx.serialization.SerialName("contact_email")
    val contactEmail: String?
)
