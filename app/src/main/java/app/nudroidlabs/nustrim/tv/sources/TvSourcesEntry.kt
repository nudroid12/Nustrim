package app.nudroidlabs.nustrim.tv.sources

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
import app.nudroidlabs.nustrim.ui.UiPreferences
import kotlinx.coroutines.flow.collect

@Composable
fun TvSourcesEntry(
    route: TvRoute.Sources,
    focusRegistry: TvFocusRegistry,
    focusRequestToken: Int,
    onStreamSelected: (TvSourceStream) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val repository = remember(context.applicationContext) { TvSourcesRepository(context.applicationContext) }
    val autoplayFirstSource = remember(route.stableKey) {
        UiPreferences(context.applicationContext).autoplayFirstSource
    }
    var autoplayConsumed by remember(route.stableKey) { mutableStateOf(false) }
    var reloadToken by remember(route.stableKey) { mutableIntStateOf(0) }
    var state by remember(route.stableKey) { mutableStateOf<TvSourcesUiState>(TvSourcesUiState.Loading()) }

    LaunchedEffect(route.stableKey, reloadToken) {
        state = TvSourcesUiState.Loading()
        runCatching {
            repository.loadProgressively(route, forceRefresh = reloadToken > 0).collect { snapshot ->
                when {
                    snapshot.streams.isNotEmpty() -> state = TvSourcesUiState.Ready(snapshot)
                    snapshot.loadingProviderCount > 0 -> state = TvSourcesUiState.Loading(snapshot)
                    snapshot.attempts.isNotEmpty() &&
                        snapshot.attempts.all { it.status == TvSourceAttemptStatus.ERROR } -> {
                        val message = snapshot.attempts
                            .firstOrNull { it.status == TvSourceAttemptStatus.ERROR }
                            ?.let { attempt ->
                                buildString {
                                    append(attempt.sourceLabel)
                                    attempt.message.takeIf { it.isNotBlank() }?.let {
                                        append(": ")
                                        append(it)
                                    }
                                }
                            }
                            .orEmpty()
                            .ifBlank { "No source completed successfully." }
                        state = TvSourcesUiState.Error(message)
                    }
                    else -> state = TvSourcesUiState.Empty(snapshot)
                }
            }
        }.onFailure { error ->
            state = TvSourcesUiState.Error(
                error.message.orEmpty().ifBlank { "Unable to load sources." },
            )
        }
    }

    LaunchedEffect(state, autoplayFirstSource, autoplayConsumed) {
        if (!autoplayFirstSource || autoplayConsumed) return@LaunchedEffect
        val ready = state as? TvSourcesUiState.Ready ?: return@LaunchedEffect
        val firstPlayable = ready.snapshot.streams.firstOrNull { it.playable } ?: return@LaunchedEffect
        autoplayConsumed = true
        onStreamSelected(firstPlayable)
    }

    TvSourcesScreen(
        media = route.media,
        episode = route.episode,
        state = state,
        routeKey = route.stableKey,
        scopeKey = route.focusScope,
        focusRegistry = focusRegistry,
        focusRequestToken = focusRequestToken,
        onRefresh = { reloadToken += 1 },
        onStreamSelected = onStreamSelected,
        modifier = modifier,
    )
}
