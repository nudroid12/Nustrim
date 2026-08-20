package app.nudroidlabs.nustrim.tv.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val TvColourScheme = darkColorScheme(
    primary = Color(0xFFF4F4F5),
    onPrimary = Color(0xFF111113),
    background = Color(0xFF08090B),
    onBackground = Color(0xFFF5F5F6),
    surface = Color(0xFF111216),
    onSurface = Color(0xFFF5F5F6),
    surfaceVariant = Color(0xFF1A1C22),
    onSurfaceVariant = Color(0xFFB6B8C0),
    outline = Color(0xFF777A84),
    error = Color(0xFFFFB4AB),
)

@Composable
fun NustrimTvTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = TvColourScheme,
        typography = Typography(),
        content = content,
    )
}
