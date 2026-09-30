package com.example

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*

@RunWith(AndroidJUnit4::class)
class LivePreviewSimulatorTest {
    @Test
    fun testIcons() {
        println(Icons.Outlined.Create.name)
        println(Icons.Outlined.Email.name)
    }
}
