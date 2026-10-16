package com.example

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Email
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import org.junit.Test

@Serializable
data class PrivacySettingsTest(
    val screenshot_protection: Boolean = false
)

@Serializable
data class PrivacyUpdateTestObj(
    val privacy_settings: PrivacySettingsTest
)

class PrivacyUpdateTest {
    @Test
    fun testPrivacyUpdate() = runBlocking {
        val supabase = createSupabaseClient(
            supabaseUrl = "https://tvleocnrlwifptaohbkh.supabase.co",
            supabaseKey = "sb_publishable_aW224YQZFDWwcrdFqB00IQ_qwXuSr9r"
        ) {
            install(Auth)
            install(Postgrest)
        }

        val email = "test_privacy_${System.currentTimeMillis()}@example.com"
        val password = "Password123!"
        
        println("Registering user $email...")
        supabase.auth.signUpWith(Email) {
            this.email = email
            this.password = password
        }
        
        val user = supabase.auth.currentUserOrNull()
        if (user == null) {
            println("Failed to get user")
            return@runBlocking
        }
        println("Registered user ID: ${user.id}")
        
        val newSettings = PrivacySettingsTest(screenshot_protection = true)
        println("Updating privacy_settings...")
        
        supabase.postgrest["profiles"].update(PrivacyUpdateTestObj(newSettings)) {
            filter { eq("id", user.id) }
        }
        
        println("Update request sent. Verifying...")
        
        val res = supabase.postgrest["profiles"].select {
            filter { eq("id", user.id) }
        }.decodeSingleOrNull<Map<String, JsonElement>>()
        
        println("Row: $res")
    }
}
