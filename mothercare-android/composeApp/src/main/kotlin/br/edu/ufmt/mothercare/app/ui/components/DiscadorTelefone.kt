package br.edu.ufmt.mothercare.app.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun rememberAbrirDiscador(): (numero: String) -> Unit {
    val context = LocalContext.current
    return remember(context) {
        { numero -> context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$numero"))) }
    }
}
