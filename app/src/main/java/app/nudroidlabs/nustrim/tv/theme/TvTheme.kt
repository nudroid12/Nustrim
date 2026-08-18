package app.nudroidlabs.nustrim.tv.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

object TvColors {
    val Background = Color(0xFF08090B)
    val BackgroundElevated = Color(0xFF101216)
    val Surface = Color(0xFF17191E)
    val SurfaceVariant = Color(0xFF22252B)
    val TextPrimary = Color(0xFFF3F3F5)
    val TextSecondary = Color(0xFFA2A5AE)
    val FocusBackground = Color(0xFF30333A)
    val FocusRing = Color(0xFFF1F2F4)
    val Accent = Color(0xFF7868E6)
}

private val TvColorScheme = darkColorScheme(
    primary = TvColors.Accent,
    onPrimary = Color.White,
    background = TvColors.Background,
    onBackground = TvColors.TextPrimary,
    surface = TvColors.Surface,
    onSurface = TvColors.TextPrimary,
    surfaceVariant = TvColors.SurfaceVariant,
    onSurfaceVariant = TvColors.TextSecondary,
    outline = Color(0xFF34373E)
)

@Composable
fun NustrimTvTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = TvColorScheme,
        content = content
    )
}
