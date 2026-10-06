package com.example.ui

import com.example.ui.SettingsColors

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.os.Build
import android.view.ViewGroup
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.ArrowLeft
import com.composables.icons.lucide.CircleCheck
import com.composables.icons.lucide.X
import com.example.supabase
import com.google.zxing.BinaryBitmap
import com.google.zxing.MultiFormatReader
import com.google.zxing.NotFoundException
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.common.HybridBinarizer
import io.github.jan.supabase.functions.functions
import io.github.jan.supabase.auth.auth
import io.ktor.client.request.header
import io.ktor.client.request.setBody
import io.ktor.http.contentType
import io.ktor.http.ContentType
import kotlinx.serialization.json.put
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.JsonPrimitive
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.decodeRecord
import io.github.jan.supabase.realtime.postgresChangeFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID
import java.util.concurrent.Executors


class QrCodeAnalyzer(private val onQrCodeScanned: (String) -> Unit) : ImageAnalysis.Analyzer {
    private val reader = MultiFormatReader().apply {
        val hints = mapOf(
            com.google.zxing.DecodeHintType.POSSIBLE_FORMATS to listOf(com.google.zxing.BarcodeFormat.QR_CODE),
            com.google.zxing.DecodeHintType.TRY_HARDER to true
        )
        setHints(hints)
    }

    private var lastLogTime = 0L

    override fun analyze(image: ImageProxy) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastLogTime > 1000) {
            lastLogTime = currentTime
        }
        
        if (image.format == ImageFormat.YUV_420_888) {
            val buffer = image.planes[0].buffer
            val data = ByteArray(buffer.remaining())
            buffer.get(data)
            
            val width = image.width
            val height = image.height
            val rowStride = image.planes[0].rowStride
            val rotationDegrees = image.imageInfo.rotationDegrees
            
            val source = if (rotationDegrees == 90 || rotationDegrees == 270) {
                val rotatedData = ByteArray(width * height)
                var i = 0
                if (rotationDegrees == 90) {
                    for (x in 0 until width) {
                        for (y in height - 1 downTo 0) {
                            rotatedData[i++] = data[y * rowStride + x]
                        }
                    }
                } else {
                    for (x in width - 1 downTo 0) {
                        for (y in 0 until height) {
                            rotatedData[i++] = data[y * rowStride + x]
                        }
                    }
                }
                PlanarYUVLuminanceSource(
                    rotatedData,
                    height,
                    width,
                    0,
                    0,
                    height,
                    width,
                    false
                )
            } else {
                PlanarYUVLuminanceSource(
                    data,
                    rowStride,
                    height,
                    0,
                    0,
                    width,
                    height,
                    false
                )
            }
            
            val binaryBitmap = BinaryBitmap(HybridBinarizer(source))
            try {
                val result = reader.decode(binaryBitmap)
                onQrCodeScanned(result.text)
            } catch (e: NotFoundException) {
                // No QR code found
            } catch (e: Exception) {
            } finally {
                image.close()
            }
        } else {
            image.close()
        }
    }
}

@kotlinx.serialization.Serializable
data class ApproveQrRequest(val request_id: String)

enum class ScannerState {
    Scanning,
    Processing,
    WaitingForApproval,
    Approved,
    RejectedOrExpired
}

@Composable
fun QrLoginScannerScreen(onBack: () -> Unit, onSignInSuccess: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()
    
    var state by remember { mutableStateOf(ScannerState.Scanning) }
    var scannedCode by remember { mutableStateOf<String?>(null) }
    var message by remember { mutableStateOf("") }
    var showApprovalDialog by remember { mutableStateOf(false) }
    var requesterDevice by remember { mutableStateOf("") }
    var requesterLocation by remember { mutableStateOf("") }
    
    var hasCameraPermission by remember { 
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) 
    }
    
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted -> 
            hasCameraPermission = granted 
        }
    )
    
    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            launcher.launch(Manifest.permission.CAMERA)
        } else {
        }
    }



    val processScannedCode = { code: String ->
        if (state == ScannerState.Scanning) {
            // Validate UUID
            val isValidUuid = try {
                UUID.fromString(code)
                true
            } catch (e: Exception) {
                false
            }

            if (isValidUuid) {
                state = ScannerState.Processing
                scannedCode = code
                coroutineScope.launch {
                    try {
                        val sessions = supabase.postgrest["qr_login_requests"]
                            .select { filter { eq("code", code) } }
                            .decodeList<QrLoginRequestModel>()
                            
                        val session = sessions.firstOrNull()
                        if (session != null && session.status == "pending") {
                            // We found a valid pending request, we show the dialog
                            requesterDevice = session.requester_device_name ?: "جهاز غير معروف"
                            requesterLocation = session.requester_location ?: "موقع غير معروف"
                            showApprovalDialog = true
                            state = ScannerState.WaitingForApproval // Just to pause scanning
                        } else {
                            state = ScannerState.RejectedOrExpired
                            message = "هذا الرمز غير صالح أو منتهي."
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        state = ScannerState.RejectedOrExpired
                        message = "حدث خطأ أثناء الاتصال بالخادم."
                    }
                }
            }
        }
    }

    if (showApprovalDialog) {
        AlertDialog(
            onDismissRequest = { /* Must actively choose */ },
            title = { Text("طلب تسجيل دخول جديد", fontWeight = FontWeight.Bold) },
            text = { Text("جهاز $requesterDevice من $requesterLocation يريد تسجيل الدخول بحسابك. هل توافق؟") },
            confirmButton = {
                TextButton(onClick = { 
                    showApprovalDialog = false
                    state = ScannerState.Processing
                    coroutineScope.launch {
                        try {
                            supabase.functions.invoke("approve-qr-login") {
                                contentType(ContentType.Application.Json)
                                setBody(ApproveQrRequest(scannedCode!!))
                            }
                            state = ScannerState.Approved
                            message = "تمت الموافقة بنجاح!"
                        } catch (e: Exception) { 
                            e.printStackTrace() 
                            state = ScannerState.RejectedOrExpired
                            message = "حدث خطأ: ${e.message}"
                        }
                    }
                }) {
                    Text("موافقة", color = SettingsColors.greenAccent)
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showApprovalDialog = false
                    coroutineScope.launch {
                        try {
                            supabase.postgrest["qr_login_requests"].update(mapOf("status" to "denied")) { filter { eq("code", scannedCode!!) } }
                            state = ScannerState.RejectedOrExpired
                            message = "تم رفض الطلب."
                        } catch (e: Exception) { e.printStackTrace() }
                    }
                }) {
                    Text("رفض", color = SettingsColors.redAccent)
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
            .background(SettingsColors.background)
            .statusBarsPadding()
    ) {
        // Header
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
                text = "مسح رمز QR",
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = SettingsColors.textPrimary
            )
        }
        
        Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            if (!hasCameraPermission) {
                Text("نحتاج لصلاحية الكاميرا لمسح الرمز.", color = SettingsColors.textPrimary)
            } else if (state == ScannerState.Scanning) {
                AndroidView(
                    factory = { ctx ->
                        PreviewView(ctx).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            scaleType = PreviewView.ScaleType.FILL_CENTER
                        }
                    },
                    update = { previewView ->
                        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
                        cameraProviderFuture.addListener({
                            val cameraProvider = cameraProviderFuture.get()
                            val preview = Preview.Builder().build().also {
                                it.setSurfaceProvider(previewView.surfaceProvider)
                            }
                            
                            val imageAnalysis = ImageAnalysis.Builder()
                                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                .build()
                                .also {
                                    it.setAnalyzer(Executors.newSingleThreadExecutor(), QrCodeAnalyzer { code ->
                                        previewView.post { processScannedCode(code) }
                                    })
                                }
                            
                            try {
                                cameraProvider.unbindAll()
                                cameraProvider.bindToLifecycle(
                                    lifecycleOwner,
                                    CameraSelector.DEFAULT_BACK_CAMERA,
                                    preview,
                                    imageAnalysis
                                )
                            } catch (exc: Exception) {
                                exc.printStackTrace()
                                android.widget.Toast.makeText(context, "فشل تهيئة الكاميرا", android.widget.Toast.LENGTH_LONG).show()
                            }
                        }, ContextCompat.getMainExecutor(context))
                    },
                    modifier = Modifier.fillMaxSize()
                )
                
                // Target overlay
                Box(
                    modifier = Modifier
                        .size(250.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color.Black.copy(alpha = 0.3f))
                )
                
                Text(
                    text = "وجّه الكاميرا نحو رمز QR على الجهاز الآخر",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 60.dp)
                )
            } else {
                // Processing or Result State
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(32.dp)
                ) {
                    when (state) {
                        ScannerState.Processing -> {
                            CircularProgressIndicator(color = SettingsColors.blueAccent)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("جاري التحقق من الرمز...", color = SettingsColors.textPrimary)
                        }
                        ScannerState.WaitingForApproval -> {
                            // Dialog is showing, just a small indicator here
                            CircularProgressIndicator(color = SettingsColors.blueAccent)
                        }
                        ScannerState.Approved -> {
                            Icon(Lucide.CircleCheck, contentDescription = null, tint = SettingsColors.greenAccent, modifier = Modifier.size(64.dp))
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(message, color = SettingsColors.greenAccent, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                        ScannerState.RejectedOrExpired -> {
                            Icon(Lucide.X, contentDescription = null, tint = SettingsColors.redAccent, modifier = Modifier.size(64.dp))
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(message, color = SettingsColors.redAccent, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(24.dp))
                            Button(
                                onClick = { state = ScannerState.Scanning },
                                colors = ButtonDefaults.buttonColors(containerColor = SettingsColors.blueAccent)
                            ) {
                                Text("حاول مرة أخرى", color = Color.White)
                            }
                        }
                        else -> {}
                    }
                }
            }
        }
    }
}
