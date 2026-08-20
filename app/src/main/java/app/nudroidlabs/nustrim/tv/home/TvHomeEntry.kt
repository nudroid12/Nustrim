package app.nudroidlabs.nustrim.tv.home

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.nudroidlabs.nustrim.tv.common.TvFoundationScreen
import app.nudroidlabs.nustrim.tv.focus.TvFocusRegistry

@Composable
fun TvHomeEntry(
    scopeKey: String,
    focusRegistry: TvFocusRegistry,
    focusRequestToken: Int,
    modifier: Modifier = Modifier,
) {
    TvFoundationScreen(
        title = "Home",
        scopeKey = scopeKey,
        focusRegistry = focusRegistry,
        focusRequestToken = focusRequestToken,
        modifier = modifier,
    )
}
