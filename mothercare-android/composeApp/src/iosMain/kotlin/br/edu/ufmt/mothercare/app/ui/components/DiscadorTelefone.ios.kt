package br.edu.ufmt.mothercare.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import platform.Foundation.NSURL
import platform.UIKit.UIApplication

@Composable
actual fun rememberAbrirDiscador(): (numero: String) -> Unit = remember {
    { numero ->
        NSURL.URLWithString("tel:$numero")?.let { UIApplication.sharedApplication.openURL(it) }
    }
}
