package app.nudroidlabs.nustrim.tv.focus

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.withFrameNanos

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

        withFrameNanos { }
        withFrameNanos { }
        registry.requestFocus(scopeKey, fallbackAnchorKey)
    }
}
