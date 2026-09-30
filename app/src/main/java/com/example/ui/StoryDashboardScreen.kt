package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.PlayCircleOutline
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import com.example.ui.i18n.LocalTranslation
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun StoryDashboardScreen(
    onNavigateToCreator: () -> Unit,
    onNavigateToViewer: () -> Unit,
    onNavigateToMyStory: () -> Unit,
    onBack: () -> Unit
) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Column(
            modifier = Modifier.fillMaxSize().background(Color(0xFFFAFAFA)).statusBarsPadding()
        ) {
            Row(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Outlined.PhotoCamera, contentDescription = "Back", tint = Color.Black)
                }
            }
            
            Column(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 32.dp, start = 24.dp, end = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier.size(64.dp).clip(RoundedCornerShape(24.dp)).background(
                        Brush.linearGradient(
                            listOf(Color(0xFFF58529), Color(0xFFDD2A7B), Color(0xFF8134AF), Color(0xFF515BD4))
                        )
                    ).shadow(8.dp, RoundedCornerShape(24.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.CameraAlt, contentDescription = null, tint = Color.White, modifier = Modifier.size(32.dp))
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(com.example.ui.i18n.LocalTranslation.current.storyStudio, fontSize = 24.sp, fontWeight = FontWeight.Black, color = Color(0xFF111827))
                Spacer(modifier = Modifier.height(8.dp))
                Text(com.example.ui.i18n.LocalTranslation.current.chooseExperience, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color(0xFF6B7280))
            }
            
            Column(
                modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Create Story
                Row(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(Color.White).border(1.dp, Color(0xFFE5E7EB), RoundedCornerShape(24.dp)).clickable { onNavigateToCreator() }.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.size(64.dp).clip(RoundedCornerShape(16.dp)).background(Color(0xFFFDF2F8)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.PhotoCamera, contentDescription = null, tint = Color(0xFFDB2777), modifier = Modifier.size(28.dp))
                    }
                    Spacer(modifier = Modifier.width(20.dp))
                    Column {
                        Text(com.example.ui.i18n.LocalTranslation.current.createStory, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF111827))
                        Text(com.example.ui.i18n.LocalTranslation.current.advancedStoryEditor, fontSize = 14.sp, color = Color(0xFF6B7280))
                    }
                }
                
                // View Story
                Row(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(Color.White).border(1.dp, Color(0xFFE5E7EB), RoundedCornerShape(24.dp)).clickable { onNavigateToViewer() }.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.size(64.dp).clip(RoundedCornerShape(16.dp)).background(Color(0xFFEEF2FF)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.Visibility, contentDescription = null, tint = Color(0xFF4F46E5), modifier = Modifier.size(28.dp))
                    }
                    Spacer(modifier = Modifier.width(20.dp))
                    Column {
                        Text(com.example.ui.i18n.LocalTranslation.current.myStoryViewer, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF111827))
                        Text(com.example.ui.i18n.LocalTranslation.current.myStoryViewer, fontSize = 14.sp, color = Color(0xFF6B7280))
                    }
                }
                
                // My Story
                Row(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(Color.White).border(1.dp, Color(0xFFE5E7EB), RoundedCornerShape(24.dp)).clickable { onNavigateToMyStory() }.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.size(64.dp).clip(RoundedCornerShape(16.dp)).background(Color(0xFFD1FAE5)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.PlayCircleOutline, contentDescription = null, tint = Color(0xFF059669), modifier = Modifier.size(28.dp))
                    }
                    Spacer(modifier = Modifier.width(20.dp))
                    Column {
                        Text(com.example.ui.i18n.LocalTranslation.current.yourStory, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF111827))
                        Text(com.example.ui.i18n.LocalTranslation.current.myStoryViewer, fontSize = 14.sp, color = Color(0xFF6B7280))
                    }
                }
            }
        }
    }
}
