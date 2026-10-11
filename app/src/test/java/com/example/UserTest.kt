package com.example

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith
import kotlinx.coroutines.runBlocking
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import com.example.ui.Profile
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest

@RunWith(AndroidJUnit4::class)
class UserTest {
    @Test
    fun testProfileQuery() = runBlocking {
        val supabase = createSupabaseClient(
            supabaseUrl = "https://tvleocnrlwifptaohbkh.supabase.co",
            supabaseKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InR2bGVvY25ybHdpZnB0YW9oYmtoIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODUwNzYxOTMsImV4cCI6MjEwMDY1MjE5M30.Ugv21lSGKSSGzFiOfNklfmhKFI0-d_fRwc9PE-JgrGA"
        ) {
            install(Postgrest)
            install(Auth)
        }
        
        try {
            val res = supabase.postgrest["profiles"]
                .select(Columns.list("id", "username", "full_name", "avatar_url", "is_online")) {
                    filter {
                        eq("username", "testuser")
                    }
                    limit(1)
                }
                .decodeSingleOrNull<Profile>()
            println("Result was: $res")
        } catch (e: Exception) {
            println("EXCEPTION CAUGHT IN TEST:")
            e.printStackTrace()
            throw e
        }
    }
}
