package br.edu.ufmt.mothercare.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val MotherCareColorScheme = lightColorScheme(
    primary = BluePrimary,
    onPrimary = White,
    primaryContainer = BlueSurfaceTint,
    onPrimaryContainer = BluePrimaryDark,
    secondary = BlueLight,
    onSecondary = White,
    background = OffWhite,
    onBackground = TextDark,
    surface = White,
    onSurface = TextDark,
    error = ErrorRed
)

@Composable
fun MotherCareTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = MotherCareColorScheme,
        typography = MotherCareTypography,
        content = content
    )
}
