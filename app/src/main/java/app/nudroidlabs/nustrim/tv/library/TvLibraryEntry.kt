package app.nudroidlabs.nustrim.tv.library

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import app.nudroidlabs.nustrim.core.library.LocalMediaStore
import app.nudroidlabs.nustrim.core.model.MediaType
import app.nudroidlabs.nustrim.tv.focus.TvFocusRegistry

@Composable
fun TvLibraryEntry(
    scopeKey: String,
    focusRegistry: TvFocusRegistry,
    focusRequestToken: Int,
    onOpen: (TvLibraryMedia) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val store = remember(context) { LocalMediaStore(context) }
    val memory = remember(scopeKey) { TvLibrarySessionStore.memory(scopeKey) }

    TvLibraryScreen(
        scopeKey = scopeKey,
        focusRegistry = focusRegistry,
        focusRequestToken = focusRequestToken,
        memory = memory,
        loadEntries = { section ->
            val entries = when (section) {
                TvLibrarySection.SAVED -> store.saved()
                TvLibrarySection.CONTINUE_WATCHING -> store.continueWatching()
            }
            entries.map { entry ->
                TvLibraryMedia(
                    entry = entry,
                    watched = store.isWatched(entry.sourceUrl, entry.toMediaItem()),
                )
            }
        },
        onOpen = onOpen,
        modifier = modifier,
    )
}

internal fun List<TvLibraryMedia>.filteredAndSorted(
    typeFilter: TvLibraryTypeFilter,
    watchedFilter: TvLibraryWatchedFilter,
    sort: TvLibrarySort,
): List<TvLibraryMedia> {
    val filtered = filter { media ->
        val typeMatches = when (typeFilter) {
            TvLibraryTypeFilter.ALL -> true
            TvLibraryTypeFilter.MOVIES -> media.entry.type == MediaType.MOVIE
            TvLibraryTypeFilter.SERIES -> media.entry.type == MediaType.SERIES || media.entry.type == MediaType.TV
        }
        val watchedMatches = when (watchedFilter) {
            TvLibraryWatchedFilter.ALL -> true
            TvLibraryWatchedFilter.WATCHED -> media.watched
            TvLibraryWatchedFilter.UNWATCHED -> !media.watched
        }
        typeMatches && watchedMatches
    }
    return when (sort) {
        TvLibrarySort.RECENT -> filtered.sortedByDescending { it.entry.updatedAt }
        TvLibrarySort.OLDEST -> filtered.sortedBy { it.entry.updatedAt }
        TvLibrarySort.TITLE_ASC -> filtered.sortedBy { it.entry.title.lowercase() }
        TvLibrarySort.TITLE_DESC -> filtered.sortedByDescending { it.entry.title.lowercase() }
    }
}
