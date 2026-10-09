package com.example

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Garde l'écoute temps réel TOUJOURS active tant que le processus de l'application vit
 * (comme Telegram / WhatsApp / Viber) : à l'ouverture tout est déjà à jour, sans attente.
 *
 *  - ne coupe plus la connexion quand l'application passe en arrière-plan ;
 *  - se reconnecte immédiatement quand le réseau revient ;
 *  - un petit contrôle toutes les 20 s relance la connexion si elle est tombée ;
 *  - au retour au premier plan : reconnexion si besoin + rafraîchissement silencieux (sans écran de chargement).
 *
 * Aucune requête, aucun schéma, aucune logique métier n'est modifiée : seulement le maintien de la connexion existante.
 */
object ConnectionKeeper {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var started = false
    private var watchdog: Job? = null

    fun start(context: Context) {
        if (started) return
        started = true
        val app = context.applicationContext

        // 1) Réseau retrouvé -> reconnexion immédiate
        try {
            val cm = app.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            cm.registerDefaultNetworkCallback(object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    ensureConnected()
                }
            })
        } catch (e: Throwable) {
        }

        // 2) Retour au premier plan -> reconnexion + rafraîchissement silencieux
        try {
            ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
                override fun onStart(owner: LifecycleOwner) {
                    ensureConnected()
                    try {
                        com.example.ui.SyncCoordinator.refreshSilently(app)
                    } catch (e: Throwable) {
                    }
                }
            })
        } catch (e: Throwable) {
        }

        // 3) Contrôle périodique léger : relance seulement si la connexion est réellement tombée
        watchdog = scope.launch {
            while (isActive) {
                delay(20_000)
                ensureConnected()
            }
        }
    }

    private fun ensureConnected() {
        scope.launch {
            try {
                if (supabase.realtime.status.value.name == "DISCONNECTED") {
                    supabase.realtime.connect()
                }
            } catch (e: Throwable) {
            }
        }
    }
}
