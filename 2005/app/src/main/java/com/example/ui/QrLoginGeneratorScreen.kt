package com.example.ui

import android.graphics.Bitmap
import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.ArrowLeft
import com.composables.icons.lucide.CircleCheck
import com.composables.icons.lucide.X
import com.composables.icons.lucide.RefreshCw
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import com.example.supabase
import com.example.util.SessionLogger
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.decodeRecord
import io.github.jan.supabase.realtime.PostgresAction
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.util.Log
import android.widget.Toast
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class QrLoginRequestModel(
    val id: String,
    val code: String,
    val status: String,
    val requester_device_name: String? = null,
    val requester_location: String? = null,
    val login_token_hash: String? = null,
    val login_email: String? = null
)


fun generateQrCodeBitmap(text: String, size: Int): Bitmap? {
    try {
        val bitMatrix: BitMatrix = MultiFormatWriter().encode(
            text,
            BarcodeFormat.QR_CODE,
            size,
            size,
            null
        )
        val width = bitMatrix.width
        val height = bitMatrix.height
        val pixels = IntArray(width * height)
        for (y in 0 until height) {
            val offset = y * width
            for (x in 0 until width) {
                pixels[offset + x] = if (bitMatrix.get(x, y)) android.graphics.Color.BLACK else android.graphics.Color.WHITE
            }
        }
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        return bitmap
    } catch (e: Exception) {
        e.printStackTrace()
        return null
    }
}

@Composable
fun QrLoginGeneratorScreen(onBack: () -> Unit, onSignInSuccess: () -> Unit = {}) {
    val coroutineScope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current
    var currentQrCode by remember { mutableStateOf<String?>(null) }
    var qrBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var timeLeft by remember { mutableStateOf(90) }
    var qrStatus by remember { mutableStateOf("loading") } // loading, active, expired
    
    // Approval Dialog State
    var showApprovalDialog by remember { mutableStateOf(false) }
    var requesterDevice by remember { mutableStateOf("") }
    
    var showSplash by remember { mutableStateOf(false) }
    var requesterLocation by remember { mutableStateOf("") }

    val generateNewCode = {
        coroutineScope.launch {
            qrStatus = "loading"
            val newCode = UUID.randomUUID().toString()
                try {
                    // Update previous if any
                    if (currentQrCode != null) {
                        supabase.postgrest["qr_login_requests"]
                            .update(mapOf("status" to "expired")) {
                                filter { eq("code", currentQrCode!!) }
                            }
                    }
                    
                    val location = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { SessionLogger.fetchLocation() }
                    val deviceName = "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}"
                    val osVersion = "Android ${android.os.Build.VERSION.RELEASE}"
                    
                    supabase.postgrest["qr_login_requests"].insert(
                        mapOf(
                            "code" to newCode,
                            "status" to "pending",
                            "requester_device_name" to deviceName,
                            "requester_os_version" to osVersion,
                            "requester_location" to location
                        )
                    )
                    currentQrCode = newCode
                    qrBitmap = generateQrCodeBitmap(newCode, 512)
                    timeLeft = 90
                    qrStatus = "active"
                } catch (e: Exception) {
                    e.printStackTrace()
                }
        }
    }

    LaunchedEffect(Unit) {
        generateNewCode()
    }

    // Timer effect
    LaunchedEffect(qrStatus, timeLeft) {
        if (qrStatus == "active" && timeLeft > 0) {
            delay(1000)
            timeLeft -= 1
            if (timeLeft == 0) {
                qrStatus = "expired"
                currentQrCode?.let {
                    try {
                        supabase.postgrest["qr_login_requests"]
                            .update(mapOf("status" to "expired")) {
                                filter { eq("code", it) }
                            }
                    } catch (e: Exception) { e.printStackTrace() }
                }
            }
        }
    }

    // Realtime Listener
    LaunchedEffect(currentQrCode) {
        if (currentQrCode != null) {
            val channel = supabase.channel("qr_requests_${currentQrCode}")
            val flow = channel.postgresChangeFlow<PostgresAction.Update>(schema = "public") {
                table = "qr_login_requests"
            }
            
            val job = flow.onEach { action ->
                val record = action.decodeRecord<QrLoginRequestModel>()
                if (record.code == currentQrCode) {
                    if (record.status == "approved") {
                        qrStatus = "approved"
                        if (record.login_token_hash != null && record.login_email != null) {
                            try {
                                supabase.auth.verifyEmailOtp(
                                    type = io.github.jan.supabase.auth.OtpType.Email.MAGIC_LINK,
                                    email = record.login_email,
                                    token = record.login_token_hash
                                )
                                
                                val userId = supabase.auth.currentUserOrNull()?.id
                                if (userId != null) {
                                    com.example.util.SessionLogger.logSession(userId, "login")
                                    supabase.postgrest["qr_login_requests"].update(mapOf("status" to "consumed")) {
                                        filter { eq("code", currentQrCode!!) }
                                    }
                                    withContext(Dispatchers.Main) {
                                        showSplash = true
                                    }
                                    kotlinx.coroutines.delay(3500)
                                    withContext(Dispatchers.Main) {
                                        onSignInSuccess()
                                    }
                                } else {
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(context, "المصادقة نجحت لكن معرّف المستخدم فارغ!", Toast.LENGTH_LONG).show()
                                        qrStatus = "error"
                                    }
                                }
                            } catch (e: Exception) {
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(context, "فشل تسجيل الدخول: ${e.message}", Toast.LENGTH_LONG).show()
                                    qrStatus = "error"
                                }
                            }
                        } else {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(context, "خطأ: بيانات الدخول ناقصة من الخادم", Toast.LENGTH_LONG).show()
                                qrStatus = "error"
                            }
                        }
                    } else if (record.status == "denied") {
                        qrStatus = "denied"
                    }
                }
            }.launchIn(coroutineScope)
            
            channel.subscribe()
            
            // Cleanup on dispose or when currentQrCode changes
            // wait, LaunchedEffect will automatically cancel the block and job on change/dispose
        }
    }
    
    DisposableEffect(Unit) {
        onDispose {
            if (qrStatus == "active" && currentQrCode != null) {
                kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                    try {
                        supabase.postgrest["qr_login_requests"]
                            .update(mapOf("status" to "expired")) {
                                filter { eq("code", currentQrCode!!) }
                            }
                    } catch (e: Exception) { e.printStackTrace() }
                }
            }
        }
    }



    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SettingsColors.background)
            .statusBarsPadding()
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
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onBack
                    )
            )
            Spacer(modifier = Modifier.width(14.dp))
            Text(
                text = "تسجيل الدخول بـ QR",
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = SettingsColors.textPrimary
            )
        }
        
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "استخدم الجهاز الآخر لمسح الرمز أدناه",
                color = SettingsColors.textPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "سيسمح ذلك للجهاز الآخر بالدخول إلى حسابك دون الحاجة لكلمة مرور.",
                color = SettingsColors.textSecondary,
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(40.dp))
            
            Box(
                modifier = Modifier
                    .size(280.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.White)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                if (qrStatus == "loading") {
                    CircularProgressIndicator(color = SettingsColors.blueAccent)
                } else if (qrStatus == "active" && qrBitmap != null) {
                    Image(
                        bitmap = qrBitmap!!.asImageBitmap(),
                        contentDescription = "QR Code",
                        modifier = Modifier.fillMaxSize()
                    )
                } else if (qrStatus == "expired") {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Lucide.RefreshCw, contentDescription = null, tint = SettingsColors.textSecondary, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("انتهت صلاحية الرمز", color = SettingsColors.textPrimary, fontWeight = FontWeight.Bold)
                    }
                } else if (qrStatus == "approved") {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(com.composables.icons.lucide.Lucide.CircleCheck, contentDescription = null, tint = SettingsColors.greenAccent, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("تمت الموافقة! جاري الدخول...", color = SettingsColors.greenAccent, fontWeight = FontWeight.Bold)
                    }
                } else if (qrStatus == "denied") {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(com.composables.icons.lucide.Lucide.X, contentDescription = null, tint = SettingsColors.redAccent, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("تم رفض الطلب.", color = SettingsColors.redAccent, fontWeight = FontWeight.Bold)
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(40.dp))
            
            if (qrStatus == "active") {
                Text(
                    text = "ينتهي في: $timeLeft ثانية",
                    color = SettingsColors.blueAccent,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            } else if (qrStatus == "expired") {
                Button(
                    onClick = { generateNewCode() },
                    colors = ButtonDefaults.buttonColors(containerColor = SettingsColors.blueAccent)
                ) {
                    Text("توليد رمز جديد", color = Color.White)
                }
            }
        }
    }
}
