package com.example.ui
import com.example.supabase
import io.github.jan.supabase.postgrest.postgrest
import com.example.ui.StoryRow

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.delay

data class StoryMediaItem(val type: String, val url: String)




@kotlinx.serialization.Serializable
data class StoryViewWithProfile(
    val id: String = "",
    val viewer_id: String = "",
    val story_id: String = "",
    val viewed_at: String? = null,
    val profiles: Profile? = null
)

@Composable
fun MyStoryViewerOverlay(userId: String, onClose: () -> Unit) {
    var userStories by remember { mutableStateOf<List<StoryRow>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(userId) {
        try {
            val format = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", java.util.Locale.US)
            format.timeZone = java.util.TimeZone.getTimeZone("UTC")
            val currentTimeStr = format.format(java.util.Date())
            userStories = supabase.postgrest["stories"]
                .select(io.github.jan.supabase.postgrest.query.Columns.raw("*, profiles(*)")) {
                    filter {
                        eq("user_id", userId)
                        gte("expires_at", currentTimeStr)
                    }
                    order("created_at", io.github.jan.supabase.postgrest.query.Order.ASCENDING)
                }
                .decodeList<StoryRow>()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            isLoading = false
        }
    }

    if (!isLoading && userStories.isNotEmpty()) {
        MyStoryViewerScreen(stories = userStories, onBack = onClose)
    } else if (!isLoading) {
        onClose()
    }
}

@Composable
fun MyStoryViewerScreen(
    stories: List<StoryRow>,
    onBack: () -> Unit
) {

    var showViewers by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf(0f) }
    var isPaused by remember { mutableStateOf(false) }
    var currentIndex by remember { mutableStateOf(0) }
    
    var currentStoryViewers by remember { mutableStateOf<List<StoryViewWithProfile>>(emptyList()) }
    var viewersCount by remember { mutableStateOf(0) }


    val avatarUrl = stories.firstOrNull()?.profiles?.avatarUrl ?: "https://images.unsplash.com/photo-1524504388940-b1c1722653e1?q=80&w=200&auto=format&fit=crop"
    val firstViewerAvatarUrl = currentStoryViewers.firstOrNull()?.profiles?.avatarUrl ?: "https://images.unsplash.com/photo-1534528741775-53994a69daeb?q=80&w=200&auto=format&fit=crop"

    // val defaultDummyMedia = StoryMediaItem("image", "https://images.unsplash.com/photo-1517841905240-472988babdf9?q=80&w=1000&auto=format&fit=crop")
    
    val actualMedia = stories

    LaunchedEffect(showViewers, isPaused, currentIndex, actualMedia) {
        if (showViewers || isPaused) return@LaunchedEffect
        
        val interval = 50L
        val step = (interval.toFloat() / 5000f) * 100f

        while (progress < 100f) {
            delay(interval)
            progress += step
        }
        
        if (currentIndex < actualMedia.size - 1) {
            currentIndex++
            progress = 0f
        } else {
            onBack()
        }
    }

        val currentMedia = actualMedia.getOrNull(currentIndex)

    LaunchedEffect(currentMedia?.id) {
        if (currentMedia?.id != null) {
            try {
                val basicViewers = com.example.supabase.postgrest["story_views"]
                    .select(io.github.jan.supabase.postgrest.query.Columns.raw("*")) {
                        filter { eq("story_id", currentMedia.id) }
                        order("viewed_at", io.github.jan.supabase.postgrest.query.Order.DESCENDING)
                    }
                    .decodeList<StoryViewRow>()
                
                val viewerIds = basicViewers.map { it.viewer_id }.distinct()
                val profilesMap = if (viewerIds.isNotEmpty()) {
                    com.example.supabase.postgrest["profiles"]
                        .select(io.github.jan.supabase.postgrest.query.Columns.raw("*")) {
                            filter { isIn("id", viewerIds) }
                        }.decodeList<Profile>().associateBy { it.id }
                } else {
                    emptyMap()
                }

                val fetchedViewers = basicViewers.map { 
                    StoryViewWithProfile(
                        id = it.id, 
                        viewer_id = it.viewer_id, 
                        story_id = it.story_id, 
                        viewed_at = it.viewed_at, 
                        profiles = profilesMap[it.viewer_id]
                    ) 
                }.distinctBy { it.viewer_id }
                currentStoryViewers = fetchedViewers
                viewersCount = fetchedViewers.size
            } catch (e: Exception) {
                e.printStackTrace()
                currentStoryViewers = emptyList()
                viewersCount = 0
            }
        }
    }


    if (showViewers) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF121212))
        ) {
            Column(modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
                // Top Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "owlino",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(
                        onClick = { showViewers = false },
                        modifier = Modifier.offset(x = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Close",
                            tint = Color.White
                        )
                    }
                }

                // Story Thumbnails
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp)
                        .border(1.dp, Color.White.copy(alpha = 0.1f)),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Spacer(modifier = Modifier.height(180.dp)) // Ensures row height
                    
                    // Thumbnail 1
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(end = 16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(100.dp)
                                .height(160.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF262626))
                                .border(2.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                        ) {
                            if (true) {
                                AsyncImage(
                                    model = currentMedia?.media_url,
                                    contentDescription = "Story Thumbnail",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.Visibility,
                                contentDescription = "Views",
                                tint = Color.White.copy(alpha = 0.8f),
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${viewersCount}",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    // Empty Thumbnail
                    Box(
                        modifier = Modifier
                            .width(100.dp)
                            .height(160.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF1A1A1A))
                            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CameraAlt,
                            contentDescription = "Add Story",
                            tint = Color.White.copy(alpha = 0.5f),
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                // Action Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color.White.copy(alpha = 0.1f))
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.Visibility,
                            contentDescription = "Views",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${viewersCount}",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = "Star",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(20.dp))
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = "Delete",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                // Viewers List Header
                Text(
                    text = "Qui a vu votre story",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
                )

                androidx.compose.foundation.lazy.LazyColumn {
                    items(currentStoryViewers.size) { i ->
                        val viewer = currentStoryViewers[i]
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { }
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box {
                                AsyncImage(
                                    model = viewer.profiles?.avatarUrl ?: "https://images.unsplash.com/photo-1534528741775-53994a69daeb?q=80&w=200&auto=format&fit=crop",
                                    contentDescription = "Viewer Avatar",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = viewer.profiles?.username ?: viewer.profiles?.fullName ?: "Unknown User",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
        return
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
    ) {
        // Background with blur
        if (true) {
            AsyncImage(
                model = currentMedia?.media_url,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .scale(1.1f)
                    .blur(48.dp)
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f))
            )
        }

        // Top Gradient
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(96.dp)
                .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.6f), Color.Transparent)))
        )

        Column(modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            // Progress Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                actualMedia.forEachIndexed { index, _ ->
                    val segmentProgress = when {
                        index < currentIndex -> 100f
                        index == currentIndex -> progress
                        else -> 0f
                    }
                    val animatedProgress by animateFloatAsState(
                        targetValue = segmentProgress,
                        label = "progress"
                    )
                    
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(2.dp)
                            .clip(RoundedCornerShape(50))
                            .background(Color.White.copy(alpha = 0.3f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(animatedProgress / 100f)
                                .background(Color.White)
                        )
                    }
                }
            }

            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box {
                        AsyncImage(
                            model = avatarUrl,
                            contentDescription = "Profile",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .offset(x = 4.dp, y = 4.dp)
                                .background(Color(0xFF121212), CircleShape)
                                .padding(2.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(14.dp)
                                    .border(1.5.dp, Color.White, CircleShape)
                                    .background(Color(0xFF121212), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Add,
                                    contentDescription = "Add",
                                    tint = Color.White,
                                    modifier = Modifier.size(10.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = ""+ (stories.firstOrNull()?.profiles?.username ?: "User") +"",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "2 h",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 14.sp
                    )
                }
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.offset(x = 8.dp, y = (-8).dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Close",
                        tint = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // Main Content Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp)
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            val down = awaitFirstDown()
                            isPaused = true
                            
                            val up = waitForUpOrCancellation()
                            isPaused = false
                            
                            if (up != null) {
                                val width = size.width
                                val x = up.position.x
                                if (x < width * 0.3f) {
                                    if (currentIndex > 0) {
                                        currentIndex--
                                        progress = 0f
                                    }
                                } else if (x > width * 0.7f) {
                                    if (currentIndex < actualMedia.size - 1) {
                                        currentIndex++
                                        progress = 0f
                                    } else {
                                        onBack()
                                    }
                                }
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.8f)
                        .shadow(24.dp, RoundedCornerShape(24.dp))
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color.Black)
                ) {
                    if (true) {
                        AsyncImage(
                            model = currentMedia?.media_url,
                            contentDescription = "Story content",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Brush.verticalGradient(listOf(Color(0xFF262626), Color(0xFF171717)))),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text("No media captured", color = Color.White.copy(alpha = 0.5f))
                        }
                    }
                }
            }

            // Bottom Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))))
                    .padding(horizontal = 16.dp, vertical = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Viewer Count Button
                Row(
                    modifier = Modifier
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { showViewers = true },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.size(32.dp)) {
                        AsyncImage(
                            model = firstViewerAvatarUrl,
                            contentDescription = "Viewer",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .border(1.dp, Color(0xFF121212), CircleShape)
                                .align(Alignment.BottomStart)
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${if (viewersCount > 0) viewersCount.toString() else "0"} personne${if (viewersCount > 1) "s" else ""}...",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.widthIn(max = 100.dp)
                    )
                }

                // Share Button
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {}
                ) {
                    Icon(
                        imageVector = Icons.Filled.Share,
                        contentDescription = "Partager",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Partager",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
