package com.example

import android.os.Build
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.performClick

@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.O_MR1])
class DebugCrashTest2 {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun testCrash() {
        try {
            composeTestRule.waitForIdle()
            // sign in
            val nodes = composeTestRule.onAllNodes(hasClickAction())
            nodes[0].performClick()
            composeTestRule.waitForIdle()
            
            // click chat
            val listNodes = composeTestRule.onAllNodes(hasClickAction())
            if(listNodes.fetchSemanticsNodes().size > 2) {
                listNodes[2].performClick()
                composeTestRule.waitForIdle()
            }
        } catch (e: Throwable) {
            e.printStackTrace()
            throw e
        }
    }
}
