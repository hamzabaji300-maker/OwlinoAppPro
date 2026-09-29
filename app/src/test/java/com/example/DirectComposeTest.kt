package com.example

import android.os.Build
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.RobolectricTestRunner
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.O_MR1], manifest = "src/main/AndroidManifest.xml", packageName = "com.example")
class DirectComposeTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun testChatDetailScreenGroup() {
        composeTestRule.waitForIdle()
        try {
            composeTestRule.onNodeWithText("Continue").performClick()
            composeTestRule.waitForIdle()
        } catch (e: Exception) {}
        
        composeTestRule.onNodeWithText("محمد").performClick()
        composeTestRule.waitForIdle()
        
        val activity = composeTestRule.activity
        val crash = com.example.ui.CrashCatcher.getLastCrash(activity)
        if (crash != null) {
            throw RuntimeException("App Crashed! Trace:\n$crash")
        }
    }
}
