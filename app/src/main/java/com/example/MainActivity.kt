package com.example

import android.os.Bundle
import io.github.jan.supabase.postgrest.postgrest
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*

import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat

import androidx.compose.material3.Icon
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import com.example.util.SecurityPreferences
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Button
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.dialog
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.NavType
import com.example.ui.ChatListScreen
import com.example.ui.ChatDetailScreen
import com.example.ui.SignInScreen
import com.example.ui.MainAppScreen
import com.example.ui.UserProfileScreen
import com.example.ui.UserListScreen
import com.example.ui.DiscoverUsersScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.i18n.TranslationManager
import com.example.ui.i18n.LocalTranslation
import com.example.ThemeManager
import androidx.activity.enableEdgeToEdge
import androidx.activity.SystemBarStyle
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.background
import androidx.compose.material3.Button
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import kotlinx.serialization.json.*
import kotlinx.coroutines.flow.first
import com.example.supabase
import io.github.jan.supabase.realtime.realtime
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.auth.handleDeeplinks
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.compose.ui.unit.dp
import com.example.ui.CrashCatcher

import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.auth.status.SessionStatus
import com.example.data.DatabaseProvider

class MainActivity : androidx.fragment.app.FragmentActivity() {

    private var currentIntent: android.content.Intent? = null

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        currentIntent = intent
        supabase.handleDeeplinks(intent)
    }
    override fun onStart() {

        super.onStart()

        lifecycleScope.launch {

            try {

                supabase.realtime.connect()

            } catch (e: Exception) {


            }

        }

    }



    override fun onStop() {
        super.onStop()
        // La connexion temps réel reste active en arrière-plan (ConnectionKeeper) : l'app est déjà à jour à l'ouverture.
    }



    override fun onResume() {
        super.onResume()
        // After the user grants "All files access" (or at any resume): create folders and refresh the media index
        Thread {
            try {
                com.example.util.MediaStorage.ensureFolders(this)
                com.example.ui.MediaIndex.scan(this)
            } catch (e: Exception) {
            }
        }.start()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        val splashStart = System.currentTimeMillis()
        // كلمة Owlino تبقى ظاهرة حتى تجهز الشاشة الأولى (أقصى مدة 1.5 ثانية)
        splashScreen.setKeepOnScreenCondition {
            !com.example.ui.GlobalAppState.startupReady && System.currentTimeMillis() - splashStart < 1500
        }
        
        try {
            app.rive.runtime.kotlin.core.Rive.init(applicationContext)
        } catch(e: Exception) {
        }
        
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }
        
        // Setup Background Sync Worker
        val syncRequest = androidx.work.PeriodicWorkRequestBuilder<com.example.worker.BackgroundSyncWorker>(15, java.util.concurrent.TimeUnit.MINUTES)
            .setConstraints(
                androidx.work.Constraints.Builder()
                    .setRequiredNetworkType(androidx.work.NetworkType.UNMETERED)
                    .build()
            )
            .build()
        androidx.work.WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
            "BackgroundSyncWorker",
            androidx.work.ExistingPeriodicWorkPolicy.KEEP,
            syncRequest
        )

        // Prefetch فوري عند فتح التطبيق (لا يتكدس: KEEP)
        com.example.worker.BackgroundSyncWorker.enqueueNow(applicationContext, delaySeconds = 20L)

                // Setup Storage Cleanup Worker
        val cleanupRequest = androidx.work.PeriodicWorkRequestBuilder<com.example.worker.StorageCleanupWorker>(7, java.util.concurrent.TimeUnit.DAYS)
            .setConstraints(
                androidx.work.Constraints.Builder()
                    .setRequiresDeviceIdle(true)
                    .setRequiresBatteryNotLow(true)
                    .build()
            )
            .build()
        androidx.work.WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
            "StorageCleanupWorker",
            androidx.work.ExistingPeriodicWorkPolicy.KEEP,
            cleanupRequest
        )

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        currentIntent = intent
        supabase.handleDeeplinks(intent)
        
        val batteryReceiver = object : android.content.BroadcastReceiver() {
            override fun onReceive(context: android.content.Context, intent: android.content.Intent) {
                if (intent.action == android.content.Intent.ACTION_BATTERY_LOW) {
                    com.example.ui.GlobalAppState.isBatterySaverEnabled = true
                } else if (intent.action == android.content.Intent.ACTION_BATTERY_OKAY) {
                    com.example.ui.GlobalAppState.isBatterySaverEnabled = false
                }
            }
        }
        val filter = android.content.IntentFilter().apply {
            addAction(android.content.Intent.ACTION_BATTERY_LOW)
            addAction(android.content.Intent.ACTION_BATTERY_OKAY)
        }
        registerReceiver(batteryReceiver, filter)
        
        lifecycleScope.launch {
            try {
                if (!supabase.realtime.status.value.name.contains("CONNECTED")) {
                    val myId = supabase.auth.currentSessionOrNull()?.user?.id
                    
                    val channel = supabase.realtime.channel("public:messages")
                    
                    val flow = channel.postgresChangeFlow<PostgresAction.Insert>(schema = "public") {
                        table = "messages"
                    }
                    
                    launch {
                        flow.collect { action ->
                            try {
                                val record = action.record
                                val senderId = record["sender_id"]?.jsonPrimitive?.content
                                val chatId = record["chat_id"]?.jsonPrimitive?.content
                                val text = record["text"]?.jsonPrimitive?.content ?: "New message"
                                
                                if (senderId != null && myId != null && senderId != myId) {
                                    try {
                                        var senderName = "User"
                                        var avatarUrl: String? = null
                                        
                                        try {
                                            val profileResult = com.example.supabase.postgrest["profiles"]
                                                .select { filter { eq("id", senderId) } }
                                                .decodeList<kotlinx.serialization.json.JsonObject>()
                                                
                                            if (profileResult.isNotEmpty()) {
                                                senderName = profileResult[0]["full_name"]?.jsonPrimitive?.content ?: senderName
                                                avatarUrl = profileResult[0]["avatar_url"]?.jsonPrimitive?.content
                                            }
                                        } catch (e: Exception) {
                                        }

                                        val actualChatId = chatId ?: senderId
                                        val db = com.example.data.DatabaseProvider.getDatabase(applicationContext)
                                        val chatEntity = db.chatDao().getChatById(actualChatId)
                                        if (chatEntity?.isMuted != true && chatEntity?.isBlocked != true) {
                                            com.example.NotificationHelper.showNotification(
                                                this@MainActivity,
                                                actualChatId,
                                                record["id"]?.jsonPrimitive?.content ?: System.currentTimeMillis().toString(),
                                                senderName,
                                                text,
                                                avatarUrl
                                            )
                                        }
                                    } catch (e: Exception) {
                                    }
                                } else {
                                }
                            } catch(e: Exception) {
                            }
                        }
                    }
                }
                try {
                    com.example.ui.BlockManager.loadCache(applicationContext)
                    com.example.ui.FollowGraphManager.loadCache(applicationContext)
                    com.example.ui.MessageDeletionManager.loadCache(applicationContext)
                    com.example.ui.SavedMessagesManager.loadCache(applicationContext)
                    supabase.realtime.connect()
                    com.example.ui.SyncCoordinator.start(applicationContext)
                } catch (e: Exception) {
                }
                
            } catch(e: Exception) {
            }
        }
        
        
        supabase.handleDeeplinks(intent)
        CrashCatcher.setup(this)
        val lastCrash = CrashCatcher.getLastCrash(this)
        
        ThemeManager.init(this)
        TranslationManager.init(this, lifecycleScope)
        
        setContent {
            val isDarkMode by ThemeManager.isDarkMode.collectAsState()
            val globalAccentColor by ThemeManager.accentColor.collectAsState()
            val currentTranslation by TranslationManager.currentTranslation.collectAsState()
            
            androidx.compose.runtime.CompositionLocalProvider(
                LocalTranslation provides currentTranslation
            ) {
                MyApplicationTheme(darkTheme = isDarkMode, accentColor = globalAccentColor) {

                val securityPrefs = remember { SecurityPreferences(this@MainActivity) }
                val isBiometricEnabled = securityPrefs.isBiometricEnabled
                val hasPin = !securityPrefs.pinCode.isNullOrBlank()
                
                // If neither is configured, app is unlocked
                var isAppUnlocked by remember { mutableStateOf(!isBiometricEnabled && !hasPin) }
                var pinInput by remember { mutableStateOf("") }
                
                LaunchedEffect(Unit) {
                    if (!isAppUnlocked && isBiometricEnabled) {
                        val executor = ContextCompat.getMainExecutor(this@MainActivity)
                        val biometricPrompt = BiometricPrompt(this@MainActivity, executor,
                            object : BiometricPrompt.AuthenticationCallback() {
                                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                                    super.onAuthenticationError(errorCode, errString)
                                    // If error or canceled, we fall back to PIN (which is shown by default behind this prompt)
                                }
                                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                                    super.onAuthenticationSucceeded(result)
                                    isAppUnlocked = true
                                }
                                override fun onAuthenticationFailed() {
                                    super.onAuthenticationFailed()
                                }
                            })

                        val promptInfo = BiometricPrompt.PromptInfo.Builder()
                            .setTitle("Unlock App")
                            .setSubtitle("Use your biometric credential")
                            .setNegativeButtonText("Use PIN")
                            .build()
                        
                        biometricPrompt.authenticate(promptInfo)
                    }
                }
                
                if (!isAppUnlocked) {
                    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Outlined.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.onBackground, modifier = Modifier.size(64.dp))
                            Spacer(modifier = Modifier.height(24.dp))
                            Text("App Locked", color = MaterialTheme.colorScheme.onBackground, fontSize = 24.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Please enter your PIN", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                            Spacer(modifier = Modifier.height(32.dp))
                            
                            OutlinedTextField(
                                value = pinInput,
                                onValueChange = { 
                                    pinInput = it
                                    if (it == securityPrefs.pinCode) {
                                        isAppUnlocked = true
                                    }
                                },
                                visualTransformation = PasswordVisualTransformation(),
                                singleLine = true,
                                colors = TextFieldDefaults.colors(
                                    focusedTextColor = MaterialTheme.colorScheme.onBackground,
                                    unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                                )
                            )
                        }
                    }
                } else if (lastCrash != null) {
                    Column(modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
                        Text("CRASH DETECTED", color = MaterialTheme.colorScheme.error)
                        Button(onClick = { CrashCatcher.clearLastCrash(this@MainActivity) }) { Text("Clear") }
                        Text(lastCrash ?: "")
                    }
                } else {
                    HooXApp(currentIntent)
                }
            }
        }
    }
}

@Composable
fun HooXApp(intent: android.content.Intent?) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = context.getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
    val hasLoggedIn = prefs.getBoolean("has_logged_in", false)
    
    val sessionStatus by supabase.auth.sessionStatus.collectAsState(initial = SessionStatus.Initializing)
    val hasSessionInitially = supabase.auth.currentSessionOrNull() != null
    
    val startDestination = if (hasLoggedIn || sessionStatus is SessionStatus.Authenticated || hasSessionInitially) "chatList" else "signIn"
    val navController = rememberNavController()
    val mentionScope = rememberCoroutineScope()
    LaunchedEffect(navController) {
        com.example.ui.RichTextActions.onMention = { mention ->
            mentionScope.launch {
                com.example.ui.MentionRouter.open(context, mention) { chatId, name ->
                    navController.navigate("chatDetail/${chatId}?name=${android.net.Uri.encode(name)}&isChannel=false")
                }
            }
        }
    }
    LaunchedEffect(startDestination) {
        if (startDestination == "signIn") com.example.ui.GlobalAppState.startupReady = true
    }
    val settingsTheme = remember { mutableStateOf(com.example.ThemeManager.loadSettingsTheme(context)) }
    var profileUserIdToShow by remember { mutableStateOf<String?>(null) }
    // true عندما يُفتح البروفايل من اسم الحساب داخل المحادثة (ينزل من الأعلى إلى نصف الشاشة)
    var profileFromTop by remember { mutableStateOf(false) }
    // true فقط عند الفتح من شاشة البحث/الاكتشاف: بعد نجاح المتابعة يتحول الزر إلى "Message"
    var profileShowMessageAfterFollow by remember { mutableStateOf(false) }
    LaunchedEffect(profileUserIdToShow) {
        if (profileUserIdToShow == null) {
            profileFromTop = false
            profileShowMessageAfterFollow = false
        }
    }
    
    val intentChatId = intent?.getStringExtra("chatId")
    val intentSenderName = intent?.getStringExtra("senderName") ?: "Chat"
    val intentProfileId = intent?.getStringExtra("profileId")
    
    LaunchedEffect(intentChatId, sessionStatus) {
        if (intentChatId != null && sessionStatus is SessionStatus.Authenticated) {
            navController.navigate("chatDetail/${intentChatId}?name=${android.net.Uri.encode(intentSenderName)}&isChannel=false")
            intent?.removeExtra("chatId")
            intent?.removeExtra("senderName")
        }
    }

    LaunchedEffect(intentProfileId, sessionStatus) {
        if (intentProfileId != null && sessionStatus is SessionStatus.Authenticated) {
            profileUserIdToShow = intentProfileId
            intent?.removeExtra("profileId")
        }
    }
    
    LaunchedEffect(sessionStatus) {
        if (sessionStatus is SessionStatus.Authenticated) {
            com.google.firebase.messaging.FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val token = task.result
                    kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                        try {
                            val currentUserId = supabase.auth.currentUserOrNull()?.id
                            if (currentUserId != null) {
                                supabase.postgrest["profiles"]
                                    .update(mapOf("fcm_token" to token)) {
                                        filter { eq("id", currentUserId) }
                                    }
                            }
                        } catch (e: Exception) {
                        }
                    }
                }
            }
        }
    }
    
    LaunchedEffect(sessionStatus) {
        if ((sessionStatus is SessionStatus.NotAuthenticated || sessionStatus is SessionStatus.RefreshFailure) && hasLoggedIn) {
            com.example.ui.BlockManager.clear()
            com.example.ui.ChatStateManager.clear()
            com.example.ui.FollowGraphManager.clear()
            com.example.ui.MessageDeletionManager.clear()
            com.example.ui.SavedMessagesManager.clear()
            prefs.edit().putBoolean("has_logged_in", false).apply()
            navController.navigate("signIn") { popUpTo(0) }
        } else if (sessionStatus is SessionStatus.Authenticated && !hasLoggedIn) {
            prefs.edit().putBoolean("has_logged_in", true).apply()
        }
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route?.substringBefore("/") ?: "chatList"
    
    val bottomBarHeight = 100.dp
    val bottomBarHeightPx = with(androidx.compose.ui.platform.LocalDensity.current) { bottomBarHeight.roundToPx().toFloat() }
    val bottomBarOffsetHeightPx = remember { mutableFloatStateOf(0f) }
    val isDarkMode by ThemeManager.isDarkMode.collectAsState()
    val view = androidx.compose.ui.platform.LocalView.current
    if (!view.isInEditMode) {
        val activity = view.context as? androidx.activity.ComponentActivity
        androidx.compose.runtime.DisposableEffect(isDarkMode) {
            val isDark = isDarkMode
            activity?.enableEdgeToEdge(
                statusBarStyle = if (isDark) androidx.activity.SystemBarStyle.dark(android.graphics.Color.TRANSPARENT) else androidx.activity.SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
                navigationBarStyle = if (isDark) androidx.activity.SystemBarStyle.dark(android.graphics.Color.TRANSPARENT) else androidx.activity.SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
            )
            onDispose {}
        }
    }

    val nestedScrollConnection = remember {
        object : androidx.compose.ui.input.nestedscroll.NestedScrollConnection {
            override fun onPreScroll(available: androidx.compose.ui.geometry.Offset, source: androidx.compose.ui.input.nestedscroll.NestedScrollSource): androidx.compose.ui.geometry.Offset {
                val delta = available.y
                val newOffset = bottomBarOffsetHeightPx.floatValue + delta
                bottomBarOffsetHeightPx.floatValue = newOffset.coerceIn(-bottomBarHeightPx, 0f)
                return androidx.compose.ui.geometry.Offset.Zero
            }
        }
    }
    
    CompositionLocalProvider(com.example.ui.LocalSettingsTheme provides settingsTheme.value, com.example.ui.LocalSettingsThemeUpdater provides { newConfig -> settingsTheme.value = newConfig; com.example.ThemeManager.saveSettingsTheme(context, newConfig) }) {
        Box(modifier = Modifier.fillMaxSize().nestedScroll(nestedScrollConnection).background(settingsTheme.value.theme.bgColor)) {
        NavHost(
            navController = navController, 
            startDestination = startDestination,
            enterTransition = {
                androidx.compose.animation.slideInHorizontally(
                    initialOffsetX = { it },
                    animationSpec = androidx.compose.animation.core.tween(280, easing = com.example.ui.redesign.RdDecelerateEasing(1.5f))
                )
            },
            exitTransition = {
                androidx.compose.animation.slideOutHorizontally(
                    targetOffsetX = { -it / 3 },
                    animationSpec = androidx.compose.animation.core.tween(280, easing = com.example.ui.redesign.RdDecelerateEasing(1.5f))
                )
            },
            popEnterTransition = {
                androidx.compose.animation.slideInHorizontally(
                    initialOffsetX = { -it / 3 },
                    animationSpec = androidx.compose.animation.core.tween(280, easing = com.example.ui.redesign.RdDecelerateEasing(1.5f))
                )
            },
            popExitTransition = {
                androidx.compose.animation.slideOutHorizontally(
                    targetOffsetX = { it },
                    animationSpec = androidx.compose.animation.core.tween(280, easing = com.example.ui.redesign.RdDecelerateEasing(1.5f))
                )
            }
        ) {
            
            composable("mainApp") {
                MainAppScreen(
                    onNavigateNext = {
                        navController.navigate("chatList") {
                            popUpTo("mainApp") { inclusive = true }
                        }
                    }
                )
            }
            composable("signIn") { 
                SignInScreen(
                    onNavigateToQrGenerator = { navController.navigate("qrLoginGenerator") }, 
                    onSignInSuccess = { navController.navigate("mainApp") { popUpTo("signIn") { inclusive = true } } }
                ) 
            }
            
            composable("qrLoginScanner",
                enterTransition = {
                    androidx.compose.animation.slideInHorizontally(
                        initialOffsetX = { it },
                        animationSpec = androidx.compose.animation.core.tween(120, easing = androidx.compose.animation.core.LinearOutSlowInEasing)
                    )
                },
                exitTransition = {
                    androidx.compose.animation.slideOutHorizontally(
                        targetOffsetX = { -it / 5 },
                        animationSpec = androidx.compose.animation.core.tween(120, easing = androidx.compose.animation.core.LinearOutSlowInEasing)
                    )
                },
                popEnterTransition = {
                    androidx.compose.animation.slideInHorizontally(
                        initialOffsetX = { -it / 5 },
                        animationSpec = androidx.compose.animation.core.tween(120, easing = androidx.compose.animation.core.LinearOutSlowInEasing)
                    )
                },
                popExitTransition = {
                    androidx.compose.animation.slideOutHorizontally(
                        targetOffsetX = { it },
                        animationSpec = androidx.compose.animation.core.tween(120, easing = androidx.compose.animation.core.LinearOutSlowInEasing)
                    )
                }
            ) { 
                com.example.ui.QrLoginScannerScreen(
                    onBack = { navController.popBackStack() }, 
                    onSignInSuccess = {
                        navController.navigate("chatList") {
                            popUpTo("signIn") { inclusive = true }
                        }
                    }
                ) 
            }
            // Force refresh
            composable("chatList") { ChatListScreen(onSettingsClick = { navController.navigate("settings") }, onProfileSwipe = { profileFromTop = true; profileUserIdToShow = com.example.supabase.auth.currentUserOrNull()?.id ?: "" }, onChatClick = { chatId, chatName, isChannel -> navController.navigate("chatDetail/${chatId}?name=${android.net.Uri.encode(chatName)}&isChannel=${isChannel}") }, onDiscoverUsers = { navController.navigate("discoverUsers") }, onUserProfileClick = { userId -> profileUserIdToShow = userId }, onNotificationsClick = { navController.navigate("notificationsFeed") }, onEditProfileClick = { navController.navigate("editProfile") }, onFollowersIconClick = { navController.navigate("followers/followers") }, onNewMessage = { navController.navigate("newMessage") }, onSearchUserClick = { userId -> profileFromTop = true; profileShowMessageAfterFollow = true; profileUserIdToShow = userId }, onArchivedClick = { navController.navigate("archived_chats") }) }
            composable("archived_chats") {
                com.example.ui.ArchivedChatsScreen(
                    navController = navController,
                    onChatClick = { chatId, chatName, isChannel -> navController.navigate("chatDetail/${chatId}?name=${android.net.Uri.encode(chatName)}&isChannel=${isChannel}") }
                )
            }
            composable("newMessage") {
                com.example.ui.NewMessageScreen(
                    onBack = { navController.popBackStack() },
                    onChatOpen = { chatId, chatName, isChannel ->
                        navController.navigate("chatDetail/${chatId}?name=${android.net.Uri.encode(chatName)}&isChannel=${isChannel}") {
                            popUpTo("newMessage") { inclusive = true }
                        }
                    }
                )
            }
            composable("notificationsFeed") { com.example.ui.NotificationsFeedScreen(onBack = { navController.popBackStack() }) }
            composable("discoverUsers") { val botCtx = androidx.compose.ui.platform.LocalContext.current; DiscoverUsersScreen(onBack = { navController.popBackStack() }, onChannelClick = { chId, chName -> kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch { try { kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { com.example.supabase.postgrest.rpc("join_channel", kotlinx.serialization.json.buildJsonObject { put("p_chat_id", kotlinx.serialization.json.JsonPrimitive(chId)) }) } } catch (e: Exception) { e.printStackTrace() }; navController.navigate("chatDetail/" + chId + "?name=" + android.net.Uri.encode(chName) + "&isChannel=true") } }, onUserClick = { userId -> profileFromTop = true; profileShowMessageAfterFollow = true; profileUserIdToShow = userId }, onBotClick = { botId, botName -> if (botId == com.example.bot.BotManager.OFFICIAL_ID) { kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch { com.example.bot.BotManager.ensureChat(botCtx); navController.navigate("chatDetail/" + com.example.bot.BotManager.CHAT_ID + "?name=" + android.net.Uri.encode(botName)) } } }) }
            composable("settings",
    enterTransition = {
        androidx.compose.animation.slideInHorizontally(
            initialOffsetX = { -it },
            animationSpec = androidx.compose.animation.core.tween(120, easing = androidx.compose.animation.core.LinearOutSlowInEasing)
        )
    },
    exitTransition = {
        androidx.compose.animation.slideOutHorizontally(
            targetOffsetX = { it / 5 },
            animationSpec = androidx.compose.animation.core.tween(120, easing = androidx.compose.animation.core.LinearOutSlowInEasing)
        )
    },
    popEnterTransition = {
        androidx.compose.animation.slideInHorizontally(
            initialOffsetX = { it / 5 },
            animationSpec = androidx.compose.animation.core.tween(120, easing = androidx.compose.animation.core.LinearOutSlowInEasing)
        )
    },
    popExitTransition = {
        androidx.compose.animation.slideOutHorizontally(
            targetOffsetX = { -it },
            animationSpec = androidx.compose.animation.core.tween(120, easing = androidx.compose.animation.core.LinearOutSlowInEasing)
        )
    }
) { 
                com.example.ui.SettingsScreen(
                    onBack = { navController.popBackStack() }, 
                    onEditProfileClick = { navController.navigate("editProfile") }, 
                    onAccountClick = { navController.navigate("account") }, 
                    onPrivacyClick = { navController.navigate("privacy") },
                    onHelpClick = { navController.navigate("helpSupport") },
                    onAboutClick = { navController.navigate("about") },
                    onLanguageClick = { navController.navigate("language") },
                    onLinkedDevicesClick = { navController.navigate("linkedDevices") },
                    onChatWallpapersClick = { navController.navigate("chatWallpapers") },
                    onStorageClick = { navController.navigate("storageSettings") },
                    onSecurityClick = { navController.navigate("securitySettings") },
                    onNotificationsClick = { navController.navigate("notificationsSettings") },
                    onPowerUsageClick = { navController.navigate("powerUsage") },
                    onOwlinoFeatherClick = { navController.navigate("owlinoFeather") },
                    onOwlinoPlusClick = { navController.navigate("owlinoPlus") }
                ) 
            }
            composable("owlinoFeather") { com.example.ui.redesign.RdFeatherScreen(onBack = { navController.popBackStack() }) }
            composable("owlinoPlus") { com.example.ui.redesign.RdPlusScreen(onBack = { navController.popBackStack() }) }
            composable("helpSupport",
    enterTransition = {
        androidx.compose.animation.slideInHorizontally(
            initialOffsetX = { it },
            animationSpec = androidx.compose.animation.core.tween(120, easing = androidx.compose.animation.core.LinearOutSlowInEasing)
        )
    },
    exitTransition = {
        androidx.compose.animation.slideOutHorizontally(
            targetOffsetX = { -it / 5 },
            animationSpec = androidx.compose.animation.core.tween(120, easing = androidx.compose.animation.core.LinearOutSlowInEasing)
        )
    },
    popEnterTransition = {
        androidx.compose.animation.slideInHorizontally(
            initialOffsetX = { -it / 5 },
            animationSpec = androidx.compose.animation.core.tween(120, easing = androidx.compose.animation.core.LinearOutSlowInEasing)
        )
    },
    popExitTransition = {
        androidx.compose.animation.slideOutHorizontally(
            targetOffsetX = { it },
            animationSpec = androidx.compose.animation.core.tween(120, easing = androidx.compose.animation.core.LinearOutSlowInEasing)
        )
    }
) { com.example.ui.HelpSupportScreen(onBack = { navController.popBackStack() }) }
            composable("about",
    enterTransition = {
        androidx.compose.animation.slideInHorizontally(
            initialOffsetX = { it },
            animationSpec = androidx.compose.animation.core.tween(120, easing = androidx.compose.animation.core.LinearOutSlowInEasing)
        )
    },
    exitTransition = {
        androidx.compose.animation.slideOutHorizontally(
            targetOffsetX = { -it / 5 },
            animationSpec = androidx.compose.animation.core.tween(120, easing = androidx.compose.animation.core.LinearOutSlowInEasing)
        )
    },
    popEnterTransition = {
        androidx.compose.animation.slideInHorizontally(
            initialOffsetX = { -it / 5 },
            animationSpec = androidx.compose.animation.core.tween(120, easing = androidx.compose.animation.core.LinearOutSlowInEasing)
        )
    },
    popExitTransition = {
        androidx.compose.animation.slideOutHorizontally(
            targetOffsetX = { it },
            animationSpec = androidx.compose.animation.core.tween(120, easing = androidx.compose.animation.core.LinearOutSlowInEasing)
        )
    }
) { com.example.ui.AboutScreen(onBack = { navController.popBackStack() }) }
            composable("language",
    enterTransition = {
        androidx.compose.animation.slideInHorizontally(
            initialOffsetX = { it },
            animationSpec = androidx.compose.animation.core.tween(120, easing = androidx.compose.animation.core.LinearOutSlowInEasing)
        )
    },
    exitTransition = {
        androidx.compose.animation.slideOutHorizontally(
            targetOffsetX = { -it / 5 },
            animationSpec = androidx.compose.animation.core.tween(120, easing = androidx.compose.animation.core.LinearOutSlowInEasing)
        )
    },
    popEnterTransition = {
        androidx.compose.animation.slideInHorizontally(
            initialOffsetX = { -it / 5 },
            animationSpec = androidx.compose.animation.core.tween(120, easing = androidx.compose.animation.core.LinearOutSlowInEasing)
        )
    },
    popExitTransition = {
        androidx.compose.animation.slideOutHorizontally(
            targetOffsetX = { it },
            animationSpec = androidx.compose.animation.core.tween(120, easing = androidx.compose.animation.core.LinearOutSlowInEasing)
        )
    }
) { com.example.ui.LanguageScreen(onBack = { navController.popBackStack() }) }
            composable("linkedDevices",
    enterTransition = {
        androidx.compose.animation.slideInHorizontally(
            initialOffsetX = { it },
            animationSpec = androidx.compose.animation.core.tween(120, easing = androidx.compose.animation.core.LinearOutSlowInEasing)
        )
    },
    exitTransition = {
        androidx.compose.animation.slideOutHorizontally(
            targetOffsetX = { -it / 5 },
            animationSpec = androidx.compose.animation.core.tween(120, easing = androidx.compose.animation.core.LinearOutSlowInEasing)
        )
    },
    popEnterTransition = {
        androidx.compose.animation.slideInHorizontally(
            initialOffsetX = { -it / 5 },
            animationSpec = androidx.compose.animation.core.tween(120, easing = androidx.compose.animation.core.LinearOutSlowInEasing)
        )
    },
    popExitTransition = {
        androidx.compose.animation.slideOutHorizontally(
            targetOffsetX = { it },
            animationSpec = androidx.compose.animation.core.tween(120, easing = androidx.compose.animation.core.LinearOutSlowInEasing)
        )
    }
) { com.example.ui.LinkedDevicesScreen(onBack = { navController.popBackStack() }, onNavigateToQrScanner = { navController.navigate("qrLoginScanner") }) }
            composable("qrLoginGenerator",
                enterTransition = {
                    androidx.compose.animation.slideInHorizontally(
                        initialOffsetX = { it },
                        animationSpec = androidx.compose.animation.core.tween(120, easing = androidx.compose.animation.core.LinearOutSlowInEasing)
                    )
                },
                exitTransition = {
                    androidx.compose.animation.slideOutHorizontally(
                        targetOffsetX = { -it / 5 },
                        animationSpec = androidx.compose.animation.core.tween(120, easing = androidx.compose.animation.core.LinearOutSlowInEasing)
                    )
                },
                popEnterTransition = {
                    androidx.compose.animation.slideInHorizontally(
                        initialOffsetX = { -it / 5 },
                        animationSpec = androidx.compose.animation.core.tween(120, easing = androidx.compose.animation.core.LinearOutSlowInEasing)
                    )
                },
                popExitTransition = {
                    androidx.compose.animation.slideOutHorizontally(
                        targetOffsetX = { it },
                        animationSpec = androidx.compose.animation.core.tween(120, easing = androidx.compose.animation.core.LinearOutSlowInEasing)
                    )
                }
            ) { com.example.ui.QrLoginGeneratorScreen(onBack = { navController.popBackStack() }, onSignInSuccess = { navController.navigate("chatList") { popUpTo("signIn") { inclusive = true } } }) }
            composable("chatWallpapers",
    enterTransition = {
        androidx.compose.animation.slideInHorizontally(
            initialOffsetX = { it },
            animationSpec = androidx.compose.animation.core.tween(120, easing = androidx.compose.animation.core.LinearOutSlowInEasing)
        )
    },
    exitTransition = {
        androidx.compose.animation.slideOutHorizontally(
            targetOffsetX = { -it / 5 },
            animationSpec = androidx.compose.animation.core.tween(120, easing = androidx.compose.animation.core.LinearOutSlowInEasing)
        )
    },
    popEnterTransition = {
        androidx.compose.animation.slideInHorizontally(
            initialOffsetX = { -it / 5 },
            animationSpec = androidx.compose.animation.core.tween(120, easing = androidx.compose.animation.core.LinearOutSlowInEasing)
        )
    },
    popExitTransition = {
        androidx.compose.animation.slideOutHorizontally(
            targetOffsetX = { it },
            animationSpec = androidx.compose.animation.core.tween(120, easing = androidx.compose.animation.core.LinearOutSlowInEasing)
        )
    }
) { com.example.ui.ChatWallpaperScreen(onBack = { navController.popBackStack() }) }
            composable("storageSettings",
    enterTransition = {
        androidx.compose.animation.slideInHorizontally(
            initialOffsetX = { it },
            animationSpec = androidx.compose.animation.core.tween(120, easing = androidx.compose.animation.core.LinearOutSlowInEasing)
        )
    },
    exitTransition = {
        androidx.compose.animation.slideOutHorizontally(
            targetOffsetX = { -it / 5 },
            animationSpec = androidx.compose.animation.core.tween(120, easing = androidx.compose.animation.core.LinearOutSlowInEasing)
        )
    },
    popEnterTransition = {
        androidx.compose.animation.slideInHorizontally(
            initialOffsetX = { -it / 5 },
            animationSpec = androidx.compose.animation.core.tween(120, easing = androidx.compose.animation.core.LinearOutSlowInEasing)
        )
    },
    popExitTransition = {
        androidx.compose.animation.slideOutHorizontally(
            targetOffsetX = { it },
            animationSpec = androidx.compose.animation.core.tween(120, easing = androidx.compose.animation.core.LinearOutSlowInEasing)
        )
    }
) { com.example.ui.StorageSettingsScreen(onBack = { navController.popBackStack() }) }
            composable("securitySettings",
    enterTransition = {
        androidx.compose.animation.slideInHorizontally(
            initialOffsetX = { it },
            animationSpec = androidx.compose.animation.core.tween(120, easing = androidx.compose.animation.core.LinearOutSlowInEasing)
        )
    },
    exitTransition = {
        androidx.compose.animation.slideOutHorizontally(
            targetOffsetX = { -it / 5 },
            animationSpec = androidx.compose.animation.core.tween(120, easing = androidx.compose.animation.core.LinearOutSlowInEasing)
        )
    },
    popEnterTransition = {
        androidx.compose.animation.slideInHorizontally(
            initialOffsetX = { -it / 5 },
            animationSpec = androidx.compose.animation.core.tween(120, easing = androidx.compose.animation.core.LinearOutSlowInEasing)
        )
    },
    popExitTransition = {
        androidx.compose.animation.slideOutHorizontally(
            targetOffsetX = { it },
            animationSpec = androidx.compose.animation.core.tween(120, easing = androidx.compose.animation.core.LinearOutSlowInEasing)
        )
    }
) { com.example.ui.SecurityScreen(onBack = { navController.popBackStack() }) }
            composable("storageSettings",
    enterTransition = {
        androidx.compose.animation.slideInHorizontally(
            initialOffsetX = { it },
            animationSpec = androidx.compose.animation.core.tween(300)
        )
    },
    exitTransition = {
        androidx.compose.animation.slideOutHorizontally(
            targetOffsetX = { -it / 3 },
            animationSpec = androidx.compose.animation.core.tween(300)
        )
    },
    popEnterTransition = {
        androidx.compose.animation.slideInHorizontally(
            initialOffsetX = { -it / 3 },
            animationSpec = androidx.compose.animation.core.tween(300)
        )
    },
    popExitTransition = {
        androidx.compose.animation.slideOutHorizontally(
            targetOffsetX = { it },
            animationSpec = androidx.compose.animation.core.tween(300)
        )
    }
) { com.example.ui.DataAndStorageScreen(onBack = { navController.popBackStack() }) }
            composable("notificationsSettings",
    enterTransition = {
        androidx.compose.animation.slideInHorizontally(
            initialOffsetX = { it },
            animationSpec = androidx.compose.animation.core.tween(120, easing = androidx.compose.animation.core.LinearOutSlowInEasing)
        )
    },
    exitTransition = {
        androidx.compose.animation.slideOutHorizontally(
            targetOffsetX = { -it / 5 },
            animationSpec = androidx.compose.animation.core.tween(120, easing = androidx.compose.animation.core.LinearOutSlowInEasing)
        )
    },
    popEnterTransition = {
        androidx.compose.animation.slideInHorizontally(
            initialOffsetX = { -it / 5 },
            animationSpec = androidx.compose.animation.core.tween(120, easing = androidx.compose.animation.core.LinearOutSlowInEasing)
        )
    },
    popExitTransition = {
        androidx.compose.animation.slideOutHorizontally(
            targetOffsetX = { it },
            animationSpec = androidx.compose.animation.core.tween(120, easing = androidx.compose.animation.core.LinearOutSlowInEasing)
        )
    }
) { com.example.ui.NotificationsScreen(onBack = { navController.popBackStack() }, onMutedChatsClick = { navController.navigate("muted_chats") }) }
            composable("powerUsage",
    enterTransition = {
        androidx.compose.animation.slideInHorizontally(
            initialOffsetX = { it },
            animationSpec = androidx.compose.animation.core.tween(120, easing = androidx.compose.animation.core.LinearOutSlowInEasing)
        )
    },
    exitTransition = {
        androidx.compose.animation.slideOutHorizontally(
            targetOffsetX = { -it / 5 },
            animationSpec = androidx.compose.animation.core.tween(120, easing = androidx.compose.animation.core.LinearOutSlowInEasing)
        )
    },
    popEnterTransition = {
        androidx.compose.animation.slideInHorizontally(
            initialOffsetX = { -it / 5 },
            animationSpec = androidx.compose.animation.core.tween(120, easing = androidx.compose.animation.core.LinearOutSlowInEasing)
        )
    },
    popExitTransition = {
        androidx.compose.animation.slideOutHorizontally(
            targetOffsetX = { it },
            animationSpec = androidx.compose.animation.core.tween(120, easing = androidx.compose.animation.core.LinearOutSlowInEasing)
        )
    }
) { com.example.ui.PowerUsageScreen(onBack = { navController.popBackStack() }) }
            composable("account",
    enterTransition = {
        androidx.compose.animation.slideInHorizontally(
            initialOffsetX = { it },
            animationSpec = androidx.compose.animation.core.tween(120, easing = androidx.compose.animation.core.LinearOutSlowInEasing)
        )
    },
    exitTransition = {
        androidx.compose.animation.slideOutHorizontally(
            targetOffsetX = { -it / 5 },
            animationSpec = androidx.compose.animation.core.tween(120, easing = androidx.compose.animation.core.LinearOutSlowInEasing)
        )
    },
    popEnterTransition = {
        androidx.compose.animation.slideInHorizontally(
            initialOffsetX = { -it / 5 },
            animationSpec = androidx.compose.animation.core.tween(120, easing = androidx.compose.animation.core.LinearOutSlowInEasing)
        )
    },
    popExitTransition = {
        androidx.compose.animation.slideOutHorizontally(
            targetOffsetX = { it },
            animationSpec = androidx.compose.animation.core.tween(120, easing = androidx.compose.animation.core.LinearOutSlowInEasing)
        )
    }
) { com.example.ui.AccountScreen(onBack = { navController.popBackStack() }, onEditProfileClick = { navController.navigate("editProfile") }) }
            composable(
    "blocked_users",
    enterTransition = {
        androidx.compose.animation.slideInHorizontally(
            initialOffsetX = { it },
            animationSpec = androidx.compose.animation.core.tween(120, easing = androidx.compose.animation.core.LinearOutSlowInEasing)
        )
    },
    exitTransition = {
        androidx.compose.animation.slideOutHorizontally(
            targetOffsetX = { -it / 5 },
            animationSpec = androidx.compose.animation.core.tween(120, easing = androidx.compose.animation.core.LinearOutSlowInEasing)
        )
    },
    popEnterTransition = {
        androidx.compose.animation.slideInHorizontally(
            initialOffsetX = { -it / 5 },
            animationSpec = androidx.compose.animation.core.tween(120, easing = androidx.compose.animation.core.LinearOutSlowInEasing)
        )
    },
    popExitTransition = {
        androidx.compose.animation.slideOutHorizontally(
            targetOffsetX = { it },
            animationSpec = androidx.compose.animation.core.tween(120, easing = androidx.compose.animation.core.LinearOutSlowInEasing)
        )
    }
) { com.example.ui.BlockedUsersScreen(onBack = { navController.popBackStack() }, onUserProfileClick = { userId -> profileUserIdToShow = userId }) }
            composable(
    "muted_chats",
    enterTransition = {
        androidx.compose.animation.slideInHorizontally(
            initialOffsetX = { it },
            animationSpec = androidx.compose.animation.core.tween(120, easing = androidx.compose.animation.core.LinearOutSlowInEasing)
        )
    },
    exitTransition = {
        androidx.compose.animation.slideOutHorizontally(
            targetOffsetX = { -it / 5 },
            animationSpec = androidx.compose.animation.core.tween(120, easing = androidx.compose.animation.core.LinearOutSlowInEasing)
        )
    },
    popEnterTransition = {
        androidx.compose.animation.slideInHorizontally(
            initialOffsetX = { -it / 5 },
            animationSpec = androidx.compose.animation.core.tween(120, easing = androidx.compose.animation.core.LinearOutSlowInEasing)
        )
    },
    popExitTransition = {
        androidx.compose.animation.slideOutHorizontally(
            targetOffsetX = { it },
            animationSpec = androidx.compose.animation.core.tween(120, easing = androidx.compose.animation.core.LinearOutSlowInEasing)
        )
    }
) { com.example.ui.MutedChatsScreen(onBack = { navController.popBackStack() }) }
            composable("privacy",
    enterTransition = {
        androidx.compose.animation.slideInHorizontally(
            initialOffsetX = { it },
            animationSpec = androidx.compose.animation.core.tween(120, easing = androidx.compose.animation.core.LinearOutSlowInEasing)
        )
    },
    exitTransition = {
        androidx.compose.animation.slideOutHorizontally(
            targetOffsetX = { -it / 5 },
            animationSpec = androidx.compose.animation.core.tween(120, easing = androidx.compose.animation.core.LinearOutSlowInEasing)
        )
    },
    popEnterTransition = {
        androidx.compose.animation.slideInHorizontally(
            initialOffsetX = { -it / 5 },
            animationSpec = androidx.compose.animation.core.tween(120, easing = androidx.compose.animation.core.LinearOutSlowInEasing)
        )
    },
    popExitTransition = {
        androidx.compose.animation.slideOutHorizontally(
            targetOffsetX = { it },
            animationSpec = androidx.compose.animation.core.tween(120, easing = androidx.compose.animation.core.LinearOutSlowInEasing)
        )
    }
) { com.example.ui.PrivacyScreen(onBack = { navController.popBackStack() }, onBlockedUsersClick = { navController.navigate("blocked_users") }) }
            composable("editProfile",
    enterTransition = {
        androidx.compose.animation.slideInHorizontally(
            initialOffsetX = { it },
            animationSpec = androidx.compose.animation.core.tween(120, easing = androidx.compose.animation.core.LinearOutSlowInEasing)
        )
    },
    exitTransition = {
        androidx.compose.animation.slideOutHorizontally(
            targetOffsetX = { -it / 5 },
            animationSpec = androidx.compose.animation.core.tween(120, easing = androidx.compose.animation.core.LinearOutSlowInEasing)
        )
    },
    popEnterTransition = {
        androidx.compose.animation.slideInHorizontally(
            initialOffsetX = { -it / 5 },
            animationSpec = androidx.compose.animation.core.tween(120, easing = androidx.compose.animation.core.LinearOutSlowInEasing)
        )
    },
    popExitTransition = {
        androidx.compose.animation.slideOutHorizontally(
            targetOffsetX = { it },
            animationSpec = androidx.compose.animation.core.tween(120, easing = androidx.compose.animation.core.LinearOutSlowInEasing)
        )
    }
) { com.example.ui.EditProfileScreen(onBack = { navController.popBackStack() }) }
            composable("followers/{type}", arguments = listOf(androidx.navigation.navArgument("type") { type = androidx.navigation.NavType.StringType })) { backStackEntry ->
                val listType = backStackEntry.arguments?.getString("type") ?: "followers"
                UserListScreen(
                    initialType = listType,
                    onBack = { navController.popBackStack() },
                    onUserClick = { user -> 
                        navController.popBackStack() 
                    },
                    onUpdateFollowers = {},
                    onUpdateFollowing = {}
                ) 
            }
            composable("chatDetail/{chatId}?name={name}&isChannel={isChannel}", arguments = listOf(navArgument("chatId") { type = NavType.StringType }, navArgument("name") { type = NavType.StringType; nullable = true }, navArgument("isChannel") { type = NavType.BoolType; defaultValue = false })) { backStackEntry -> val chatId = backStackEntry.arguments?.getString("chatId") ?: ""; val name = backStackEntry.arguments?.getString("name") ?: "Chat"; val isChannel = backStackEntry.arguments?.getBoolean("isChannel") ?: false; ChatDetailScreen(chatId = chatId, name = name, isChannel = isChannel, onBack = { navController.popBackStack() }, onDiscoverUsers = { navController.navigate("discoverUsers") }, onProfileClick = { userId -> if (userId.isNotBlank()) { profileFromTop = true; profileUserIdToShow = userId } }) }
        }
        
        
        if (profileUserIdToShow != null) {
            com.example.ui.UserProfileScreen(
                userId = profileUserIdToShow!!,
                fromTop = profileFromTop,
                showMessageAfterFollow = profileShowMessageAfterFollow,
                onBack = { profileUserIdToShow = null },
                onMessageClick = { chatId, name -> 
                    profileUserIdToShow = null
                    navController.navigate("chatDetail/${chatId}?name=${android.net.Uri.encode(name)}&isChannel=false") 
                },
                onFollowersClick = { profileUserIdToShow = null; navController.navigate("followers/followers") },
                onFollowingClick = { profileUserIdToShow = null; navController.navigate("followers/following") },
                onEditProfileClick = { profileUserIdToShow = null; navController.navigate("editProfile") }
            )
        }
        
        val showBottomNav = false

        androidx.compose.animation.AnimatedVisibility(
            visible = showBottomNav,
            enter = androidx.compose.animation.slideInVertically(initialOffsetY = { it }) + androidx.compose.animation.fadeIn(),
            exit = androidx.compose.animation.slideOutVertically(targetOffsetY = { it }) + androidx.compose.animation.fadeOut(),
            modifier = Modifier
                .align(androidx.compose.ui.Alignment.BottomCenter)
                .offset { androidx.compose.ui.unit.IntOffset(x = 0, y = -bottomBarOffsetHeightPx.floatValue.toInt()) }
        ) {
            val activeTab = when {
                currentRoute == "settings" || currentRoute == "account" || currentRoute == "storage_settings" || currentRoute == "linked_devices" || currentRoute == "privacy" -> "Settings"
                currentRoute == "editProfile" || profileUserIdToShow != null -> "Library"
                else -> "Chat"
            }
            
            com.example.ui.CustomBottomNavigation(
                activeTab = activeTab,
                onTabSelected = { tab ->
                    when (tab) {
                        "Settings" -> navController.navigate("settings") { launchSingleTop = true; restoreState = true }
                        "Library" -> {
                            val myId = com.example.supabase.auth.currentUserOrNull()?.id
                            if (myId != null) {
                                profileUserIdToShow = myId
                            }
                        }
                        "Chat" -> {
                            profileUserIdToShow = null
                            navController.navigate("chatList") { launchSingleTop = true; restoreState = true; popUpTo("chatList") { inclusive = false } }
                        }
                    }
                },
                onSearchClick = { navController.navigate("discoverUsers") },
                modifier = Modifier.fillMaxWidth()
            )
        }
        
        // Removed MainBottomNavBar to prevent overlapping with FAB and ensure cleaner layout

        com.example.ui.OwlinoToastOverlay()
    }
    }
    }
}
