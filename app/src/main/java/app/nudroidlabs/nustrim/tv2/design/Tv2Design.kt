package app.nudroidlabs.nustrim.tv2.design

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Nustrim TV2 design primitives.
 *
 * Independently implemented for Nustrim.
 * The organisation into explicit spacing, motion and shape tokens follows
 * the TV-first design-system approach used by the locked UX reference.
 */
object Tv2Spacing {
    val screenHorizontal = 48.dp
    val screenVertical = 32.dp

    val railCompactWidth = 72.dp
    val railExpandedWidth = 220.dp
    val railHorizontalPadding = 10.dp
    val railVerticalPadding = 24.dp
    val railItemGap = 8.dp

    val itemHorizontal = 14.dp
    val itemVertical = 12.dp
    val iconLabelGap = 12.dp
}

object Tv2Motion {
    const val focusDurationMs = 180
    const val railDurationMs = 180
    const val focusScale = 1.02f
}

object Tv2Shapes {
    val navItem = RoundedCornerShape(18.dp)
    val panel = RoundedCornerShape(20.dp)
}

object Tv2Colors {
    val background = Color(0xFF090A0C)
    val rail = Color(0xF20C0D10)
    val railBorder = Color(0x1FFFFFFF)

    val text = Color(0xFFF5F6F8)
    val textMuted = Color(0xFF9B9EA6)

    val active = Color(0xFF25282E)
    val focused = Color(0xFFF2F4F7)
    val focusedContent = Color(0xFF16181C)
}
