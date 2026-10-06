package com.example

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.functions.Functions

val supabase by lazy {
    createSupabaseClient(
    supabaseUrl = "https://tvleocnrlwifptaohbkh.supabase.co",
    supabaseKey = "sb_publishable_aW224YQZFDWwcrdFqB00IQ_qwXuSr9r") {
    install(Postgrest)
    install(Realtime)
    install(Storage)
    install(Functions)
    install(Auth) {
        scheme = "owlinoapp"
        host = "login-callback"
        alwaysAutoRefresh = true
        autoLoadFromStorage = true
    }
}
}
