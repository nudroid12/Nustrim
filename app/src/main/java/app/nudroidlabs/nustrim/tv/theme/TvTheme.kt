package app.nudroidlabs.nustrim.tv.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

object TvColors {
    val Background = Color(0xFF090A0C)
    val BackgroundElevated = Color(0xFF111317)
    val Surface = Color(0xFF181A1F)
    val SurfaceVariant = Color(0xFF22252B)
    val TextPrimary = Color(0xFFF3F3F5)
    val TextSecondary = Color(0xFF9A9DA6)
    val FocusBackground = Color(0xFF30333A)
    val FocusRing = Color(0xFFE9E9EC)
    val Accent = Color(0xFF7B6CF6)
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
