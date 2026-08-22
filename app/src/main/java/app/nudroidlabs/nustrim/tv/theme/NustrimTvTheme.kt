package app.nudroidlabs.nustrim.tv.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

object TvColors {
    val Background = Color(0xFF08090B)
    val Surface = Color(0xFF111216)
    val SurfaceRaised = Color(0xFF1A1C22)
    val SurfaceSelected = Color(0xFF292B31)
    val FocusSurface = Color(0xFFF1F1F3)
    val TextPrimary = Color(0xFFF5F5F6)
    val TextSecondary = Color(0xFFB6B8C0)
    val TextMuted = Color(0xFF858891)
    val TextInverse = Color(0xFF101114)
    val Border = Color(0xFF30333A)
    val BorderMuted = Color(0xFF272A30)
    val SidebarCollapsed = Color(0xF20A0B0E)
    val SidebarExpanded = Color(0xFA0A0B0E)
}

private val TvColourScheme = darkColorScheme(
    primary = TvColors.FocusSurface,
    onPrimary = TvColors.TextInverse,
    background = TvColors.Background,
    onBackground = TvColors.TextPrimary,
    surface = TvColors.Surface,
    onSurface = TvColors.TextPrimary,
    surfaceVariant = TvColors.SurfaceRaised,
    onSurfaceVariant = TvColors.TextSecondary,
    outline = TvColors.Border,
    error = Color(0xFFFFB4AB),
)

private val TvTypography = Typography(
    headlineLarge = TextStyle(
        fontSize = 36.sp,
        lineHeight = 42.sp,
        fontWeight = FontWeight.SemiBold,
    ),
    titleLarge = TextStyle(
        fontSize = 26.sp,
        lineHeight = 32.sp,
        fontWeight = FontWeight.SemiBold,
    ),
    bodyLarge = TextStyle(
        fontSize = 16.sp,
        lineHeight = 23.sp,
        fontWeight = FontWeight.Normal,
    ),
    labelLarge = TextStyle(
        fontSize = 14.sp,
        lineHeight = 18.sp,
        fontWeight = FontWeight.Medium,
    ),
)

@Composable
fun NustrimTvTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = TvColourScheme,
        typography = TvTypography,
        shapes = Shapes(),
        content = content,
    )
}
