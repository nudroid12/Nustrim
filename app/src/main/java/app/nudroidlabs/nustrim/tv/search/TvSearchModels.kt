package app.nudroidlabs.nustrim.tv.search

import app.nudroidlabs.nustrim.core.model.MediaItem

data class TvSearchMedia(
    val sourceUrl: String,
    val sourceName: String,
    val item: MediaItem,
) {
    val stableKey: String = buildString {
        append(sourceUrl)
        append('|')
        append(item.ref?.mediaType ?: item.type.name)
        append('|')
        append(item.ref?.metaId?.takeIf { it.isNotBlank() } ?: item.id)
        item.ref?.providerLocator?.takeIf { it.isNotBlank() }?.let { locator ->
            append('|')
            append(locator.hashCode().toString(16))
        }
    }
}

data class TvSearchRow(
    val key: String,
    val title: String,
    val sourceName: String,
    val items: List<TvSearchMedia>,
)

data class TvSearchSnapshot(
    val rows: List<TvSearchRow>,
    val failedSources: Int,
    val searchableSources: Int,
    val totalSources: Int,
)

sealed interface TvSearchUiState {
    data object DiscoverLoading : TvSearchUiState
    data class Discover(val snapshot: TvSearchSnapshot) : TvSearchUiState
    data class Searching(
        val query: String,
        val previous: TvSearchSnapshot? = null,
    ) : TvSearchUiState
    data class Results(
        val query: String,
        val snapshot: TvSearchSnapshot,
    ) : TvSearchUiState
    data class Empty(
        val query: String,
        val message: String,
    ) : TvSearchUiState
    data class Error(
        val query: String,
        val message: String,
    ) : TvSearchUiState
}
