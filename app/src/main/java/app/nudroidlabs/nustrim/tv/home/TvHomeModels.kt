package app.nudroidlabs.nustrim.tv.home

import app.nudroidlabs.nustrim.core.library.LocalMediaEntry
import app.nudroidlabs.nustrim.core.model.MediaItem

data class TvHomeMedia(
    val sourceUrl: String,
    val sourceName: String,
    val item: MediaItem,
    val continueEntry: LocalMediaEntry? = null,
) {
    val stableKey: String = buildString {
        append(sourceUrl)
        append('|')
        append(item.ref?.mediaType ?: item.type.name)
        append('|')
        append(item.ref?.metaId?.takeIf { it.isNotBlank() } ?: item.id)
    }

    val backdropUrl: String
        get() = item.backgroundUrl.ifBlank { item.posterUrl }
}

data class TvHomeRow(
    val key: String,
    val title: String,
    val sourceName: String,
    val items: List<TvHomeMedia>,
)

data class TvHomeSnapshot(
    val rows: List<TvHomeRow>,
    val failedSources: Int,
    val totalSources: Int,
)

sealed interface TvHomeUiState {
    data object Loading : TvHomeUiState
    data class Ready(val snapshot: TvHomeSnapshot) : TvHomeUiState
    data class Empty(val message: String) : TvHomeUiState
    data class Error(val message: String) : TvHomeUiState
}
