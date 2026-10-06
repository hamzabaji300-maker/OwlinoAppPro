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
import androidx.compose.ui.graphics.vector.ImageVector
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

// Warm accent used only for the avatar edit badge, to match the reference design
private val EditBadgeColor = Color(0xFFFF7A45)

// Purple used for the primary Save Changes card-button
private val SaveButtonPurple = Color(0xFF7C5CFC)

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

    fun doSave() {
        if (currentUser == null) return
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
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .background(SettingsColors.background)
    ) {
        // Top Bar — back chevron on the left, centered bold title
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 8.dp)
        ) {
            IconButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterStart)) {
                Icon(Lucide.ArrowLeft, contentDescription = "Back", tint = SettingsColors.textPrimary)
            }
            Text(
                "Edit Profile",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = SettingsColors.textPrimary,
                modifier = Modifier.align(Alignment.Center)
            )
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
                    .padding(horizontal = 24.dp)
            ) {
                Spacer(modifier = Modifier.height(20.dp))

                // Avatar Section — centered circle with a small round edit badge on the corner
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Box(contentAlignment = Alignment.BottomEnd) {
                        Box(
                            modifier = Modifier
                                .size(120.dp)
                                .clip(CircleShape)
                                .background(cardColor)
                                .border(1.dp, borderColor, CircleShape)
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
                                Text(if (initials.isNotBlank()) initials else "AM", color = SettingsColors.textPrimary, fontSize = 36.sp, fontWeight = FontWeight.SemiBold)
                            }

                            if (isUploading) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(textColor.copy(alpha = 0.3f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = SettingsColors.background, strokeWidth = 2.dp)
                                }
                            }
                        }

                        // Small round edit badge, matching the reference screen
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(EditBadgeColor)
                                .border(3.dp, SettingsColors.background, CircleShape)
                                .clickable {
                                    imagePickerLauncher.launch(
                                        androidx.activity.result.PickVisualMediaRequest(
                                            ActivityResultContracts.PickVisualMedia.ImageOnly
                                        )
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Lucide.Pencil, contentDescription = "Change photo", tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Full Name
                ProfileFormField(
                    label = "Full Name",
                    value = fullName,
                    onValueChange = { fullName = it },
                    cardColor = cardColor,
                    borderColor = borderColor
                )
                Spacer(modifier = Modifier.height(20.dp))

                // Username
                ProfileFormField(
                    label = "Username",
                    value = username,
                    onValueChange = { username = it },
                    cardColor = cardColor,
                    borderColor = borderColor
                )
                Spacer(modifier = Modifier.height(20.dp))

                // Bio
                ProfileFormField(
                    label = "Bio",
                    value = bio,
                    onValueChange = { if (it.length <= 160) bio = it },
                    cardColor = cardColor,
                    borderColor = borderColor,
                    placeholder = "Tell people a bit about yourself",
                    minLines = 2
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Text("${bio.length}/160", fontSize = 12.sp, color = SettingsColors.textSecondary)
                }

                Spacer(modifier = Modifier.height(32.dp))
                Text("CRYPTO IDENTITY", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SettingsColors.textSecondary, letterSpacing = 1.5.sp)
                Spacer(modifier = Modifier.height(16.dp))

                ProfileFormField(
                    label = "Cryptvora ID",
                    value = cryptvoraId,
                    onValueChange = { cryptvoraId = it },
                    cardColor = cardColor,
                    borderColor = borderColor,
                    placeholder = "Not assigned yet",
                    readOnly = true,
                    trailingIcon = Lucide.Lock
                )
                Spacer(modifier = Modifier.height(20.dp))

                ProfileFormField(
                    label = "Wallet address",
                    value = walletAddress,
                    onValueChange = { walletAddress = it },
                    cardColor = cardColor,
                    borderColor = borderColor,
                    trailingIcon = Lucide.Hash
                )
                Spacer(modifier = Modifier.height(20.dp))

                ProfileFormField(
                    label = "Chain",
                    value = chain,
                    onValueChange = { chain = it },
                    cardColor = cardColor,
                    borderColor = borderColor,
                    trailingIcon = Lucide.Link2
                )

                Spacer(modifier = Modifier.height(32.dp))
                Text("CONTACT · PRIVATE", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SettingsColors.textSecondary, letterSpacing = 1.5.sp)
                Spacer(modifier = Modifier.height(16.dp))

                ProfileFormField(
                    label = "Email",
                    value = email,
                    onValueChange = { email = it },
                    cardColor = cardColor,
                    borderColor = borderColor,
                    trailingIcon = Lucide.Mail
                )

                Spacer(modifier = Modifier.height(36.dp))

                // Single full-width save button — purple card style, soft rounded corners
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(SaveButtonPurple)
                        .clickable { doSave() },
                    contentAlignment = Alignment.Center
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            "SAVE CHANGES",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}

@Composable
fun ProfileFormField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    cardColor: Color,
    borderColor: Color,
    placeholder: String = "",
    readOnly: Boolean = false,
    trailingIcon: ImageVector? = null,
    minLines: Int = 1
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(label, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = SettingsColors.textPrimary)
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(cardColor)
                .border(1.dp, borderColor, RoundedCornerShape(16.dp))
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.weight(1f)) {
                if (value.isEmpty() && placeholder.isNotEmpty()) {
                    Text(placeholder, fontSize = 15.sp, color = SettingsColors.textSecondary)
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    textStyle = TextStyle(fontSize = 15.sp, color = if (readOnly) SettingsColors.textSecondary else SettingsColors.textPrimary, lineHeight = 20.sp),
                    cursorBrush = SolidColor(SettingsColors.blueAccent),
                    modifier = Modifier.fillMaxWidth(),
                    readOnly = readOnly,
                    minLines = minLines
                )
            }
            if (trailingIcon != null) {
                Spacer(modifier = Modifier.width(8.dp))
                Icon(trailingIcon, contentDescription = null, tint = SettingsColors.textSecondary, modifier = Modifier.size(20.dp))
            }
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
