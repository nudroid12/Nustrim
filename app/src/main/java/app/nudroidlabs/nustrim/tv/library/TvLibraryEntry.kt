package app.nudroidlabs.nustrim.tv.library

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.nudroidlabs.nustrim.tv.common.TvFoundationScreen
import app.nudroidlabs.nustrim.tv.focus.TvFocusRegistry

@Composable
fun TvLibraryEntry(
    scopeKey: String,
    focusRegistry: TvFocusRegistry,
    focusRequestToken: Int,
    modifier: Modifier = Modifier,
) {
    TvFoundationScreen(
        title = "Library",
        scopeKey = scopeKey,
        focusRegistry = focusRegistry,
        focusRequestToken = focusRequestToken,
        modifier = modifier,
    )
}
