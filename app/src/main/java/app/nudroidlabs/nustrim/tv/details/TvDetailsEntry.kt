package app.nudroidlabs.nustrim.tv.details

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
import app.nudroidlabs.nustrim.tv.episode.TvCanonicalEpisode

@Composable
fun TvDetailsEntry(
    route: TvRoute.Details,
    focusRegistry: TvFocusRegistry,
    focusRequestToken: Int,
    onPlayMovie: (TvDetailsSnapshot) -> Unit,
    onPlayEpisode: (TvDetailsSnapshot, TvCanonicalEpisode) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val repository = remember(context) { TvDetailsRepository(context) }
    var reloadToken by remember(route.stableKey) { mutableIntStateOf(0) }
    var state by remember(route.stableKey) { mutableStateOf<TvDetailsUiState>(TvDetailsUiState.Loading) }

    LaunchedEffect(route.stableKey, reloadToken) {
        state = TvDetailsUiState.Loading
        state = runCatching { repository.load(route, forceRefresh = reloadToken > 0) }
            .fold(
                onSuccess = { TvDetailsUiState.Ready(it) },
                onFailure = {
                    TvDetailsUiState.Error(
                        it.message?.takeIf { message -> message.isNotBlank() }
                            ?: "Details could not be loaded from this source.",
                    )
                },
            )
    }

    TvDetailsScreen(
        state = state,
        contentKey = route.contentKey,
        scopeKey = route.focusScope,
        focusRegistry = focusRegistry,
        focusRequestToken = focusRequestToken,
        onRetry = { reloadToken += 1 },
        onPlayMovie = onPlayMovie,
        onPlayEpisode = onPlayEpisode,
        modifier = modifier,
    )
}
