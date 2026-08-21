package app.nudroidlabs.nustrim.tv.episode

import app.nudroidlabs.nustrim.core.model.MediaEpisode
import app.nudroidlabs.nustrim.core.model.MediaItem

enum class TvEpisodeCoordinateKind {
    PROVIDER,
    PARTIAL_PROVIDER,
    VIRTUAL_DISPLAY_ONLY,
    UNKNOWN,
}

data class TvEpisodeIdentity(
    val parentKey: String,
    val providerEpisodeId: String,
    val sourceIndex: Int,
) {
    val stableKey: String = if (providerEpisodeId.isNotBlank()) {
        "$parentKey|provider:$providerEpisodeId"
    } else {
        "$parentKey|source-index:$sourceIndex"
    }
}

data class TvCanonicalEpisode(
    val identity: TvEpisodeIdentity,
    val providerEpisodeId: String,
    val title: String,
    val seasonNumber: Int?,
    val episodeNumber: Int?,
    val displaySeasonNumber: Int?,
    val displayEpisodeNumber: Int?,
    val thumbnailUrl: String,
    val overview: String,
    val sourceIndex: Int,
    val coordinateKind: TvEpisodeCoordinateKind,
    val providerEpisode: MediaEpisode,
) {
    val coordinateLabel: String
        get() = when {
            seasonNumber != null && episodeNumber != null -> "S${seasonNumber}E${episodeNumber}"
            displaySeasonNumber != null && displayEpisodeNumber != null -> "Episode $displayEpisodeNumber"
            episodeNumber != null -> "Episode $episodeNumber"
            else -> "Episode"
        }
}

data class TvEpisodeSeason(
    val seasonNumber: Int?,
    val displaySeasonNumber: Int?,
    val isVirtual: Boolean,
    val episodes: List<TvCanonicalEpisode>,
) {
    val stableKey: String = when {
        seasonNumber == 0 -> "season:specials"
        seasonNumber != null -> "season:$seasonNumber"
        isVirtual -> "season:virtual"
        else -> "season:unknown"
    }

    val label: String = when {
        seasonNumber == 0 -> "Specials"
        seasonNumber != null -> "Season $seasonNumber"
        isVirtual -> "Episodes"
        else -> "Other episodes"
    }
}

data class TvEpisodeDiagnostics(
    val providerEntries: Int,
    val canonicalEntries: Int,
    val duplicateProviderIdsRemoved: Int,
    val coordinateCollisionsRetained: Int,
    val unknownSeasonCount: Int,
    val unknownEpisodeCount: Int,
    val invalidCoordinateCount: Int,
)

data class TvEpisodeCatalogue(
    val parentKey: String,
    val seasons: List<TvEpisodeSeason>,
    val diagnostics: TvEpisodeDiagnostics,
) {
    val episodes: List<TvCanonicalEpisode> = seasons.flatMap { it.episodes }
    val firstRegularSeasonIndex: Int = seasons.indexOfFirst { (it.seasonNumber ?: 0) > 0 }
        .takeIf { it >= 0 }
        ?: 0
}

data class TvEpisodeSnapshot(
    val sourceName: String,
    val item: MediaItem,
    val catalogue: TvEpisodeCatalogue,
)

sealed interface TvEpisodeUiState {
    data object Loading : TvEpisodeUiState
    data class Ready(val snapshot: TvEpisodeSnapshot) : TvEpisodeUiState
    data class Empty(val snapshot: TvEpisodeSnapshot, val message: String) : TvEpisodeUiState
    data class Error(val message: String) : TvEpisodeUiState
}
