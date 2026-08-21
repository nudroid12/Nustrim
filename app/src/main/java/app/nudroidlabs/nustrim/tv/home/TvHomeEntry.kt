package app.nudroidlabs.nustrim.tv.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import app.nudroidlabs.nustrim.tv.focus.TvFocusRegistry

@Composable
fun TvHomeEntry(
    scopeKey: String,
    focusRegistry: TvFocusRegistry,
    focusRequestToken: Int,
    onOpen: (TvHomeMedia, Int, Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val repository = remember(context) { TvHomeRepository(context) }
    var reloadToken by remember { mutableIntStateOf(0) }
    var state by remember { mutableStateOf<TvHomeUiState>(TvHomeUiState.Loading) }

    LaunchedEffect(reloadToken) {
        state = TvHomeUiState.Loading
        val snapshot = runCatching { repository.load(forceRefresh = reloadToken > 0) }
            .getOrElse {
                state = TvHomeUiState.Error("Catalogues could not be loaded. Check your installed sources and try again.")
                return@LaunchedEffect
            }

        state = when {
            snapshot.rows.isNotEmpty() -> TvHomeUiState.Ready(snapshot)
            snapshot.totalSources == 0 -> TvHomeUiState.Empty("No enabled catalogue source is installed.")
            snapshot.failedSources >= snapshot.totalSources -> TvHomeUiState.Error("All enabled catalogue sources failed to load.")
            else -> TvHomeUiState.Empty("The enabled sources returned no Home catalogues.")
        }
    }

    TvHomeScreen(
        state = state,
        scopeKey = scopeKey,
        focusRegistry = focusRegistry,
        focusRequestToken = focusRequestToken,
        onRetry = { reloadToken += 1 },
        onOpen = onOpen,
        modifier = modifier,
    )
}
