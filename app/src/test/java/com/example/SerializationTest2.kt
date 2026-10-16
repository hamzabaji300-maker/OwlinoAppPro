package com.example

import org.junit.Test
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString
import com.example.ui.Profile

class SerializationTest2 {
    @Test
    fun testDecodeNull() {
        val jsonStr = """{"id":"123","username":"test","full_name":null,"avatar_url":null,"is_online":null}"""
        try {
            val p = Json { ignoreUnknownKeys = true }.decodeFromString<Profile>(jsonStr)
            println("Decoded: $p")
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
    }
}
