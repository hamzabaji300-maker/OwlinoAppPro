package com.example.ui
import com.example.supabase
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.json.JsonObject

suspend fun checkDb() {
    try {
        val user = supabase.auth.currentUserOrNull()
        if (user != null) {
            val res = supabase.postgrest["profiles"].select { filter { eq("id", user.id) } }.decodeSingle<JsonObject>()
            println("DB_SCHEMA: $res")
        }
    } catch (e: Exception) {
        println("DB_SCHEMA_ERROR: ${e.message}")
    }
}
