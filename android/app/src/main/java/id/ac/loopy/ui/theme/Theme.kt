package id.ac.loopy.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Palet fondasi sementara. TODO: cocokkan token, font Plus Jakarta Sans, spacing dengan UI kit Figma.
private val LightColors = lightColorScheme(primary = Color(0xFF25634B), secondary = Color(0xFF6D7855), background = Color(0xFFFAF8F2))
private val DarkColors = darkColorScheme(primary = Color(0xFFA3D4B5), secondary = Color(0xFFC2CBA7))

@Composable
fun LoopyTheme(darkTheme: Boolean = false, content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (darkTheme) DarkColors else LightColors, content = content)
}
