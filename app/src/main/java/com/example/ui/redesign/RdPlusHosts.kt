package com.example.ui.redesign

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.ViewCompat
import com.example.ui.LocalSettingsTheme
import com.example.ui.plusui.FeatherActivity
import com.example.ui.plusui.Pal
import com.example.ui.plusui.PlusActivity

private fun Context.rdFindActivity(): Activity {
    var c: Context = this
    while (c is ContextWrapper) {
        if (c is Activity) return c
        c = c.baseContext
    }
    error("No Activity found in context chain")
}

/**
 * Route `owlinoFeather` : l'écran « Owlino Feather » copié tel quel (Views/Canvas) et hébergé via AndroidView.
 * UI uniquement : les boutons affichent des Toast, le solde est fixé à 0.
 */
@Composable
fun RdFeatherScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val dark = LocalSettingsTheme.current.theme.isDark
    Pal.appDark = dark
    val host = remember(dark) { FeatherActivity(ctx.rdFindActivity(), onBack) }
    androidx.compose.runtime.key(dark) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { host.build().also { ViewCompat.requestApplyInsets(it) } }
        )
    }
}

/**
 * Route `owlinoPlus` : l'écran « Owlino Plus » copié tel quel (Views/Canvas) et hébergé via AndroidView.
 * UI uniquement : aucun paiement réel.
 */
@Composable
fun RdPlusScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val dark = LocalSettingsTheme.current.theme.isDark
    Pal.appDark = dark
    val host = remember(dark) { PlusActivity(ctx.rdFindActivity(), onBack) }
    androidx.compose.runtime.key(dark) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { host.build().also { ViewCompat.requestApplyInsets(it) } }
        )
    }
}
