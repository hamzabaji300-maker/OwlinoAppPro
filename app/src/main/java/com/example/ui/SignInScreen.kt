package com.example.ui

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.QrCode
import com.composables.icons.lucide.ShieldCheck
import com.composables.icons.lucide.WifiOff
import com.composables.icons.lucide.X
import com.example.supabase
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

fun isOnline(context: Context): Boolean {
    val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val network = connectivityManager.activeNetwork ?: return false
    val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
    return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
}

@Composable
fun SignInScreen(onSignInSuccess: () -> Unit, onNavigateToQrGenerator: () -> Unit = {}) {
    val scope = rememberCoroutineScope()
    val sessionStatus by supabase.auth.sessionStatus.collectAsState(initial = SessionStatus.Initializing)
    var showSuccessScreen by remember { mutableStateOf(false) }
    var isLoggingIn by remember { mutableStateOf(false) }
    var hasHandledLogin by remember { mutableStateOf(false) }
    
    val context = LocalContext.current
    var showError by remember { mutableStateOf(false) }
    var introFinished by remember { mutableStateOf(false) }
    var showSplash by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        delay(3500)
        showSplash = false
    }

    LaunchedEffect(sessionStatus) {
        if (sessionStatus is SessionStatus.Authenticated && !hasHandledLogin) {
            val userId = supabase.auth.currentUserOrNull()?.id
            if (userId != null) {
                hasHandledLogin = true
                val locStr = com.example.util.SessionLogger.fetchLocation()
                try {
                    com.example.util.SessionLogger.logSession(userId, "login", locStr)
                } catch(e: Throwable) {}
                
                isLoggingIn = false
                onSignInSuccess()
            } else {
                isLoggingIn = false
                onSignInSuccess()
            }
        } else if (sessionStatus is SessionStatus.NotAuthenticated) {
            try {
                scope.launch {
                    try { supabase.auth.signOut() } catch (e: Exception) {}
                }
            } catch (e: Exception) {}
            isLoggingIn = false
        } else if (sessionStatus is SessionStatus.RefreshFailure) {
            isLoggingIn = false
        }
    }

    val handleLogin = {
        if (!isOnline(context)) {
            showError = true
        } else {
            isLoggingIn = true
            scope.launch {
                try {
                    supabase.auth.signInWith(Google)
                } catch (e: Exception) {
                    isLoggingIn = false
                    showError = true
                }
            }
        }
    }

    Scaffold(
        containerColor = Color.White,
        contentWindowInsets = WindowInsets.systemBars
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            AnimatedContent(
                targetState = if (showSplash) 0 else 1,
                transitionSpec = {
                    fadeIn(animationSpec = tween(500)) togetherWith fadeOut(animationSpec = tween(500))
                },
                label = "SignInTransition"
            ) { state ->
                when (state) {
                    0 -> SplashScreenContent()
                    1 -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        TradingBackground()

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .fillMaxWidth()
                                .widthIn(max = 384.dp) // max-w-sm
                                .padding(horizontal = 24.dp)
                                .padding(top = 48.dp, bottom = 48.dp)
                        ) {
                            AnimatedTitle(onAnimationComplete = { introFinished = true })

                            AnimatedVisibility(
                                visible = introFinished,
                                enter = fadeIn(tween(1000)) + slideInVertically(tween(1000), initialOffsetY = { 40 })
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Spacer(modifier = Modifier.height(40.dp))
                                    
                                    // Welcome Text
                                    Text(
                                        text = "Professional Trading Community",
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF111827),
                                        letterSpacing = (-0.5).sp,
                                        textAlign = TextAlign.Center,
                                        lineHeight = 28.sp
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = "Connect with traders, discover market insights, share strategies and grow together.",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFF6B7280),
                                        textAlign = TextAlign.Center,
                                        lineHeight = 24.sp,
                                        modifier = Modifier.widthIn(max = 300.dp)
                                    )

                                    Spacer(modifier = Modifier.height(40.dp))

                                    // Buttons
                                    GoogleButton(
                                        isLoading = isLoggingIn,
                                        onClick = { handleLogin() }
                                    )
                                    
                                    Spacer(modifier = Modifier.height(16.dp))

                                    QrLoginButton(onClick = onNavigateToQrGenerator)
                                    
                                    Spacer(modifier = Modifier.height(16.dp))
                                    
                                    Text(
                                        text = "We only use your Google account for secure authentication.",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFF9CA3AF),
                                        textAlign = TextAlign.Center,
                                        lineHeight = 18.sp,
                                        modifier = Modifier.widthIn(max = 240.dp)
                                    )
                                }
                            }
                        }
                    }
                    }
                }
            }

            // Error Toast
            AnimatedVisibility(
                visible = showError,
                enter = slideInVertically(spring(dampingRatio = 0.6f, stiffness = 400f), initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(spring(dampingRatio = 0.6f, stiffness = 400f), targetOffsetY = { it / 2 }) + fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 40.dp, start = 16.dp, end = 16.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xE6111827), // bg-gray-900/90
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                    shadowElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth().widthIn(max = 384.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(Color(0xFFEF4444).copy(alpha = 0.1f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Lucide.WifiOff,
                                    contentDescription = null,
                                    tint = Color(0xFFF87171),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(
                                    text = "No Internet Connection",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                                Text(
                                    text = "Please check your network settings.",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF9CA3AF)
                                )
                            }
                        }
                        IconButton(onClick = { showError = false }) {
                            Icon(imageVector = Lucide.X, contentDescription = "Dismiss", tint = Color(0xFF9CA3AF))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GoogleButton(isLoading: Boolean, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(stiffness = 400f, dampingRatio = 0.6f)
    )

    Surface(
        shape = RoundedCornerShape(50),
        color = if (isPressed) Color(0xFFF3F4F6) else Color(0xFFF9FAFB), // bg-gray-50
        border = BorderStroke(1.dp, Color(0xFFF3F4F6)),
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 280.dp)
            .height(56.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
    ) {
        // Inner shadow simulation via very subtle background gradient
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind {
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color.Black.copy(alpha = 0.03f), Color.Transparent),
                            startY = 0f,
                            endY = size.height * 0.2f
                        )
                    )
                }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {

                // Google Icon Container
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color.White, CircleShape)
                        .border(1.dp, Color(0xFFF3F4F6), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.foundation.Image(
                        painter = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.ic_google),
                        contentDescription = "Google Logo",
                        modifier = Modifier.size(20.dp)
                    )
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Continue with Google",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF111827),
                        letterSpacing = (-0.5).sp
                    )
                    Text(
                        text = "Secure authentication",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF6B7280)
                    )
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Box(
                    modifier = Modifier.size(40.dp).padding(end = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    AnimatedContent(
                        targetState = isLoading,
                        transitionSpec = {
                            (fadeIn(tween(200)) + scaleIn(tween(200), initialScale = 0.5f)).togetherWith(
                                fadeOut(tween(200)) + scaleOut(tween(200), targetScale = 0.5f)
                            )
                        }
                    ) { loading ->
                        if (loading) {
                            val infiniteTransition = rememberInfiniteTransition()
                            val angle by infiniteTransition.animateFloat(
                                initialValue = 0f,
                                targetValue = 360f,
                                animationSpec = infiniteRepeatable(tween(1000, easing = LinearEasing))
                            )
                            Icon(
                                imageVector = Lucide.ShieldCheck, // Replace with Loader if available, but shield is fine as fallback
                                contentDescription = null,
                                tint = Color(0xFF3B82F6),
                                modifier = Modifier.size(20.dp).rotate(angle)
                            )
                        } else {
                            Icon(
                                imageVector = Lucide.ShieldCheck,
                                contentDescription = null,
                                tint = Color(0xFF9CA3AF),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun QrLoginButton(onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(stiffness = 400f, dampingRatio = 0.6f)
    )

    Surface(
        shape = RoundedCornerShape(50),
        color = if (isPressed) Color(0xFFF3F4F6) else Color(0xFFF9FAFB),
        border = BorderStroke(1.dp, Color(0xFFF3F4F6)),
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 280.dp)
            .height(56.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind {
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color.Black.copy(alpha = 0.03f), Color.Transparent),
                            startY = 0f,
                            endY = size.height * 0.2f
                        )
                    )
                }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // QR Icon Container
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color.White, CircleShape)
                        .border(1.dp, Color(0xFFF3F4F6), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Lucide.QrCode,
                        contentDescription = null,
                        tint = Color(0xFF111827),
                        modifier = Modifier.size(20.dp)
                    )
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Log in with QR Code",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF111827),
                        letterSpacing = (-0.5).sp
                    )
                    Text(
                        text = "Scan with Owlino mobile app",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF6B7280),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun AnimatedTitle(onAnimationComplete: () -> Unit) {
    var glow by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        delay(1600)
        glow = true
        delay(1200)
        onAnimationComplete()
    }

    data class LetterAnim(val char: String, val startX: Float, val startY: Float, val startRot: Float, val startScale: Float, val isPurple: Boolean = false)
    val lettersData = listOf(
        LetterAnim("O", -500f, -300f, -25f, 1.5f),
        LetterAnim("w", 300f, -500f, 45f, 0.8f),
        LetterAnim("l", 500f, 400f, -45f, 1.2f),
        LetterAnim("i", 0f, 600f, 90f, 1.8f),
        LetterAnim("n", -600f, 200f, -90f, 0.9f),
        LetterAnim("o", 400f, -100f, 60f, 1.1f, true)
    )
    
    val animationStates = lettersData.map { remember { Animatable(0f) } }
    
    LaunchedEffect(Unit) {
        lettersData.forEachIndexed { index, _ ->
            launch {
                delay(index * 120L)
                animationStates[index].animateTo(
                    targetValue = 1f,
                    animationSpec = spring(
                        dampingRatio = 0.6f,
                        stiffness = 100f // custom spring like [0.16, 1, 0.3, 1]
                    )
                )
            }
        }
    }
    
    val glowAlpha by animateFloatAsState(
        targetValue = if (glow) 1f else 0f,
        animationSpec = tween(2000, easing = LinearOutSlowInEasing)
    )

    Box(contentAlignment = Alignment.Center) {
        // Purple Glow
        if (glowAlpha > 0f) {
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .graphicsLayer { alpha = glowAlpha; scaleX = 1.5f; scaleY = 1.5f }
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0xFF9333EA).copy(alpha = 0.2f), Color.Transparent)
                        )
                    )
            )
        }
        
        Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            lettersData.forEachIndexed { index, letter ->
                val progress = animationStates[index].value
                
                val currentX = letter.startX * (1f - progress)
                val currentY = letter.startY * (1f - progress)
                val currentRot = letter.startRot * (1f - progress)
                val currentScale = letter.startScale + (1f - letter.startScale) * progress
                
                Text(
                    text = letter.char,
                    fontSize = 72.sp, // 4.5rem = 72px
                    fontWeight = FontWeight.Black,
                    color = if (letter.isPurple) Color(0xFF7E22CE) else Color(0xFF111827),
                    letterSpacing = (-2).sp,
                    modifier = Modifier.graphicsLayer {
                        translationX = currentX
                        translationY = currentY
                        rotationZ = currentRot
                        scaleX = currentScale
                        scaleY = currentScale
                        alpha = progress.coerceIn(0f, 1f)
                    }
                )
            }
        }
    }
}

@Composable
fun TradingBackground() {
    val ICONS = listOf("XAU/USD", "EUR/USD", "GBP/USD", "USD/JPY", "BTC/USD", "ETH/USD", "NASDAQ", "S&P 500", "DOW JONES", "NIKKEI", "DAX", "FOREX", "CRYPTO")
    
    val patternCells = remember {
        val items = mutableListOf<Map<String, Any>>()
        var seed = 42L
        fun random(): Float {
            seed = (seed * 16807) % 2147483647
            return (seed - 1).toFloat() / 2147483646f
        }
        
        fun isOverlapping(x: Float, y: Float): Boolean {
            for (item in items) {
                val dx = Math.abs(x - (item["left"] as Float))
                val dy = Math.abs(y - (item["top"] as Float))
                if (dx < 22f && dy < 10f) return true
            }
            return false
        }
        
        fun isCenterSafe(x: Float, y: Float): Boolean {
            return !(x > 15f && x < 85f && y > 35f && y < 65f)
        }
        
        var attempts = 0
        var lastIconIndex = -1
        
        while (items.size < 18 && attempts < 2000) {
            attempts++
            val left = 2f + random() * 96f
            val top = 2f + random() * 96f
            
            if (!isOverlapping(left, top) && isCenterSafe(left, top)) {
                var iconIndex = (random() * ICONS.size).toInt()
                if (iconIndex == lastIconIndex) {
                    iconIndex = (iconIndex + 1) % ICONS.size
                }
                lastIconIndex = iconIndex
                
                val text = ICONS[iconIndex]
                val rotate = (random() * 91f).toInt() - 45f
                val size = 16f + (random() * 20f).toInt()
                val opacity = 0.12f + random() * 0.06f
                val color = if (random() > 0.6f) Color(0xFF60A5FA) else Color(0xFF94A3B8)
                
                items.add(
                    mapOf(
                        "id" to items.size,
                        "text" to text,
                        "rotate" to rotate,
                        "size" to size,
                        "opacity" to opacity,
                        "color" to color,
                        "left" to left,
                        "top" to top
                    )
                )
            }
        }
        items
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // Subtle gradient background
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0xFFEFF6FF).copy(alpha = 0.5f), Color.Transparent),
                        radius = 1200f,
                        center = Offset(0f, 0f)
                    )
                )
        )
        
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val width = constraints.maxWidth.toFloat()
            val height = constraints.maxHeight.toFloat()
            
            patternCells.forEach { cell ->
                val id = cell["id"] as Int
                val text = cell["text"] as String
                val rotate = cell["rotate"] as Float
                val size = cell["size"] as Float
                val opacity = cell["opacity"] as Float
                val color = cell["color"] as Color
                val left = cell["left"] as Float
                val top = cell["top"] as Float
                
                val posX = (left / 100f) * width
                val posY = (top / 100f) * height
                
                var visible by remember { mutableStateOf(false) }
                LaunchedEffect(Unit) {
                    delay((id % 10) * 100L)
                    visible = true
                }
                
                val alpha by animateFloatAsState(
                    targetValue = if (visible) opacity else 0f,
                    animationSpec = tween(2000)
                )
                
                Box(
                    modifier = Modifier
                        .offset(
                            x = with(androidx.compose.ui.platform.LocalDensity.current) { posX.toDp() - 40.dp },
                            y = with(androidx.compose.ui.platform.LocalDensity.current) { posY.toDp() - 20.dp }
                        )
                        .graphicsLayer {
                            rotationZ = rotate
                            this.alpha = alpha
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = text,
                        fontSize = size.sp,
                        fontWeight = FontWeight.Bold,
                        color = color,
                        letterSpacing = 1.sp,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }
    }
}


@Composable
fun SplashScreenContent() {
    // 3 second timer handled by the parent AnimatedContent state in SignInScreen
    // This is just the visual part of the SplashScreen.
    
    val animationStates = List(6) { remember { Animatable(0f) } }
    val glowAlpha = remember { Animatable(0f) }
    val glowScale = remember { Animatable(0.5f) }
    val containerScale = remember { Animatable(1f) }
    
    LaunchedEffect(Unit) {
        // Individual letter animations with delays
        val delays = listOf(0L, 100L, 200L, 300L, 400L, 1000L)
        animationStates.forEachIndexed { index, animatable ->
            launch {
                delay(delays[index])
                if (index == 5) { // 'o' uses backOut (spring)
                    animatable.animateTo(
                        targetValue = 1f,
                        animationSpec = spring(dampingRatio = 0.5f, stiffness = 200f)
                    )
                } else {
                    animatable.animateTo(
                        targetValue = 1f,
                        animationSpec = tween(durationMillis = 1200, easing = CubicBezierEasing(0.2f, 0.65f, 0.3f, 0.9f))
                    )
                }
            }
        }
        
        // Glow animation
        launch {
            delay(1200)
            launch { glowAlpha.animateTo(1f, tween(1000)) }
            launch { glowScale.animateTo(1f, tween(1000)) }
        }
        
        // Container scale down at the end (from 2.4s to 3s)
        launch {
            delay(2400)
            containerScale.animateTo(0.85f, tween(600, easing = EaseInOut))
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier.graphicsLayer {
                scaleX = containerScale.value
                scaleY = containerScale.value
            },
            contentAlignment = Alignment.Center
        ) {
            // Purple Glow
            Box(
                modifier = Modifier
                    .size(192.dp) // w-48 h-48
                    .graphicsLayer { 
                        alpha = glowAlpha.value
                        scaleX = glowScale.value
                        scaleY = glowScale.value
                    }
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0xFF9333EA).copy(alpha = 0.2f), Color.Transparent)
                        )
                    )
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                // O
                Text(
                    text = "O",
                    fontSize = 60.sp, // text-6xl
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF111827),
                    letterSpacing = (-2).sp,
                    modifier = Modifier.graphicsLayer {
                        translationX = -80f * (1f - animationStates[0].value)
                        alpha = animationStates[0].value
                    }
                )
                // w
                Text(
                    text = "w",
                    fontSize = 60.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF111827),
                    letterSpacing = (-2).sp,
                    modifier = Modifier.graphicsLayer {
                        translationY = -80f * (1f - animationStates[1].value)
                        alpha = animationStates[1].value
                    }
                )
                // l
                Text(
                    text = "l",
                    fontSize = 60.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF111827),
                    letterSpacing = (-2).sp,
                    modifier = Modifier.graphicsLayer {
                        translationX = 80f * (1f - animationStates[2].value)
                        alpha = animationStates[2].value
                    }
                )
                // i
                Text(
                    text = "i",
                    fontSize = 60.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF111827),
                    letterSpacing = (-2).sp,
                    modifier = Modifier.graphicsLayer {
                        translationY = 80f * (1f - animationStates[3].value)
                        alpha = animationStates[3].value
                    }
                )
                // n
                Text(
                    text = "n",
                    fontSize = 60.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF111827),
                    letterSpacing = (-2).sp,
                    modifier = Modifier.graphicsLayer {
                        translationX = -80f * (1f - animationStates[4].value)
                        alpha = animationStates[4].value
                    }
                )
                // o
                Text(
                    text = "o",
                    fontSize = 60.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF7E22CE), // text-purple-700
                    letterSpacing = (-2).sp,
                    modifier = Modifier.graphicsLayer {
                        scaleX = animationStates[5].value
                        scaleY = animationStates[5].value
                        alpha = animationStates[5].value
                    }
                )
            }
        }
    }
}


