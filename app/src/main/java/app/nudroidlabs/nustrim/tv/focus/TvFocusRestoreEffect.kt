package app.nudroidlabs.nustrim.tv.focus

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.withFrameNanos
import app.nudroidlabs.nustrim.tv.theme.TvTokens

@Composable
fun TvFocusRestoreEffect(
    registry: TvFocusRegistry,
    scopeKey: String,
    fallbackAnchorKey: String,
    requestToken: Int,
    enabled: Boolean = true,
) {
    LaunchedEffect(registry, scopeKey, fallbackAnchorKey, requestToken, enabled) {
        if (!enabled) return@LaunchedEffect

        repeat(TvTokens.FocusRestoreAttempts) {
            withFrameNanos { }
            if (registry.requestFocus(scopeKey, fallbackAnchorKey)) return@LaunchedEffect
        }
    }
}
