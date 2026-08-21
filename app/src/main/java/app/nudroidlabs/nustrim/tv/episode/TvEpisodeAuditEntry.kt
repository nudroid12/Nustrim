package app.nudroidlabs.nustrim.tv.episode

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
import app.nudroidlabs.nustrim.tv.navigation.TvRoute

@Composable
fun TvEpisodeAuditEntry(
    route: TvRoute.Details,
    focusRegistry: TvFocusRegistry,
    focusRequestToken: Int,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val repository = remember(context) { TvEpisodeRepository(context) }
    var reloadToken by remember(route.stableKey) { mutableIntStateOf(0) }
    var state by remember(route.stableKey) { mutableStateOf<TvEpisodeUiState>(TvEpisodeUiState.Loading) }

    LaunchedEffect(route.stableKey, reloadToken) {
        state = TvEpisodeUiState.Loading
        val snapshot = runCatching { repository.load(route) }
            .getOrElse { error ->
                state = TvEpisodeUiState.Error(
                    error.message?.takeIf { it.isNotBlank() }
                        ?: "Episode metadata could not be loaded.",
                )
                return@LaunchedEffect
            }
        state = if (snapshot.catalogue.episodes.isEmpty()) {
            TvEpisodeUiState.Empty(
                snapshot = snapshot,
                message = if (snapshot.item.type.name in setOf("MOVIE", "LIVE")) {
                    "This title does not expose an episode catalogue."
                } else {
                    "The provider returned no episodes for this title."
                },
            )
        } else {
            TvEpisodeUiState.Ready(snapshot)
        }
    }

    TvEpisodeAuditScreen(
        state = state,
        scopeKey = route.focusScope,
        focusRegistry = focusRegistry,
        focusRequestToken = focusRequestToken,
        onRetry = { reloadToken += 1 },
        modifier = modifier,
    )
}
