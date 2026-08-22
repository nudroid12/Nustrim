package app.nudroidlabs.nustrim.tv.search

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
import kotlinx.coroutines.delay

@Composable
fun TvSearchEntry(
    scopeKey: String,
    focusRegistry: TvFocusRegistry,
    focusRequestToken: Int,
    onOpen: (TvSearchMedia, Int, Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val repository = remember(context) { TvSearchRepository(context) }
    val historyStore = remember(context) { TvSearchHistoryStore(context) }
    val memory = remember(scopeKey) { TvSearchSessionStore.memory(scopeKey) }

    var query by remember(memory) { mutableStateOf(memory.query) }
    var state by remember(memory) {
        mutableStateOf<TvSearchUiState>(
            when {
                memory.query.trim().length >= MIN_SEARCH_QUERY_LENGTH && memory.searchSnapshot != null -> {
                    TvSearchUiState.Results(memory.query.trim(), memory.searchSnapshot!!)
                }
                memory.discoverSnapshot != null -> TvSearchUiState.Discover(memory.discoverSnapshot!!)
                else -> TvSearchUiState.DiscoverLoading
            },
        )
    }
    var recentSearches by remember { mutableStateOf(historyStore.recent()) }
    var retryToken by remember { mutableIntStateOf(0) }

    fun rememberConfirmedSearch() {
        val clean = query.trim()
        if (clean.length < MIN_SEARCH_QUERY_LENGTH) return
        historyStore.remember(clean)
        recentSearches = historyStore.recent()
    }

    LaunchedEffect(query, retryToken) {
        memory.query = query
        val clean = query.trim()

        if (clean.length < MIN_SEARCH_QUERY_LENGTH) {
            val cached = memory.discoverSnapshot
            if (cached != null && retryToken == 0) {
                state = TvSearchUiState.Discover(cached)
                return@LaunchedEffect
            }

            state = TvSearchUiState.DiscoverLoading
            val snapshot = runCatching {
                repository.discover(forceRefresh = retryToken > 0)
            }.getOrElse {
                state = TvSearchUiState.Error(
                    query = "",
                    message = "Discover could not be loaded. Check your enabled sources and try again.",
                )
                return@LaunchedEffect
            }
            memory.discoverSnapshot = snapshot
            state = when {
                snapshot.rows.isNotEmpty() -> TvSearchUiState.Discover(snapshot)
                snapshot.totalSources == 0 -> TvSearchUiState.Empty(
                    query = "",
                    message = "No enabled catalogue source is installed.",
                )
                snapshot.failedSources >= snapshot.totalSources -> TvSearchUiState.Error(
                    query = "",
                    message = "All enabled catalogue sources failed to load.",
                )
                else -> TvSearchUiState.Empty(
                    query = "",
                    message = "The enabled sources returned no Discover catalogues.",
                )
            }
            return@LaunchedEffect
        }

        state = TvSearchUiState.Searching(
            query = clean,
            previous = memory.searchSnapshot?.takeIf { memory.searchQuery == clean },
        )
        if (retryToken == 0) delay(SEARCH_DEBOUNCE_MS)

        val snapshot = runCatching { repository.search(clean) }
            .getOrElse {
                state = TvSearchUiState.Error(
                    query = clean,
                    message = "Search could not be completed. Check your connection and try again.",
                )
                return@LaunchedEffect
            }

        memory.searchQuery = clean
        memory.searchSnapshot = snapshot
        state = when {
            snapshot.rows.isNotEmpty() -> TvSearchUiState.Results(clean, snapshot)
            snapshot.totalSources == 0 -> TvSearchUiState.Empty(
                query = clean,
                message = "No enabled source is installed.",
            )
            snapshot.searchableSources == 0 -> TvSearchUiState.Empty(
                query = clean,
                message = "None of the enabled sources supports search.",
            )
            snapshot.failedSources >= snapshot.searchableSources -> TvSearchUiState.Error(
                query = clean,
                message = "Every searchable source failed. Try again.",
            )
            else -> TvSearchUiState.Empty(
                query = clean,
                message = "No result matched \"$clean\".",
            )
        }
    }

    TvSearchScreen(
        query = query,
        state = state,
        recentSearches = recentSearches,
        memory = memory,
        scopeKey = scopeKey,
        focusRegistry = focusRegistry,
        focusRequestToken = focusRequestToken,
        onQueryChanged = {
            retryToken = 0
            query = it
        },
        onConfirmSearch = ::rememberConfirmedSearch,
        onRecentSelected = {
            retryToken = 0
            query = it
        },
        onClearHistory = {
            historyStore.clear()
            recentSearches = emptyList()
        },
        onRetry = { retryToken += 1 },
        onOpen = { media, rowIndex, itemIndex ->
            rememberConfirmedSearch()
            onOpen(media, rowIndex, itemIndex)
        },
        modifier = modifier,
    )
}

private const val SEARCH_DEBOUNCE_MS = 350L
const val MIN_SEARCH_QUERY_LENGTH = 2
