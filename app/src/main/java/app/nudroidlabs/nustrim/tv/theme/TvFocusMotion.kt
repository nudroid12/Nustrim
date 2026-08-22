package app.nudroidlabs.nustrim.tv.theme

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue

@Composable
fun animateTvFocusScale(
    focused: Boolean,
    focusedScale: Float = TvTokens.FocusScale,
    label: String,
): Float {
    val scale by animateFloatAsState(
        targetValue = if (focused) focusedScale else 1f,
        animationSpec = tween(
            durationMillis = TvTokens.FastMotionMillis,
            easing = FastOutSlowInEasing,
        ),
        label = label,
    )
    return scale
}
