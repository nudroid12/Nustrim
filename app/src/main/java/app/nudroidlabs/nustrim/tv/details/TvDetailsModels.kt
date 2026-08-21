package app.nudroidlabs.nustrim.tv.details

import app.nudroidlabs.nustrim.core.model.MediaItem
import app.nudroidlabs.nustrim.tv.episode.TvEpisodeCatalogue

data class TvDetailsSnapshot(
    val sourceName: String,
    val item: MediaItem,
    val episodeCatalogue: TvEpisodeCatalogue,
)

sealed interface TvDetailsUiState {
    data object Loading : TvDetailsUiState
    data class Ready(val snapshot: TvDetailsSnapshot) : TvDetailsUiState
    data class Error(val message: String) : TvDetailsUiState
}
