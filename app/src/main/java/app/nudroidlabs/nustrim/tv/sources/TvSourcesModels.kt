package app.nudroidlabs.nustrim.tv.sources

import app.nudroidlabs.nustrim.core.model.MediaEpisode
import app.nudroidlabs.nustrim.core.model.MediaItem
import app.nudroidlabs.nustrim.core.model.StreamSource

enum class TvSourceAttemptStatus {
    LOADING,
    SUCCESS,
    EMPTY,
    ERROR,
}

data class TvSourceAttempt(
    val sourceLabel: String,
    val status: TvSourceAttemptStatus,
    val streamCount: Int = 0,
    val message: String = "",
)

data class TvSourceStream(
    val sourceLabel: String,
    val sourceUrl: String,
    val stream: StreamSource,
) {
    val stableKey: String = buildString {
        append(sourceLabel.hashCode().toString(16))
        append(':')
        append(stream.providerId.hashCode().toString(16))
        append(':')
        append(stream.name.hashCode().toString(16))
        append(':')
        append(stream.url.hashCode().toString(16))
    }

    val playable: Boolean
        get() = stream.playable && stream.url.isNotBlank()

    val qualityLabel: String
        get() = QUALITY_REGEX.find(stream.name)?.value.orEmpty()

    val transportLabel: String
        get() = stream.type.trim().takeIf { it.isNotBlank() && !it.equals("auto", true) }
            ?.uppercase()
            .orEmpty()

    companion object {
        private val QUALITY_REGEX = Regex("(?i)(2160p|1440p|1080p|720p|480p|360p)")
    }
}

data class TvSourcesSnapshot(
    val media: MediaItem,
    val episode: MediaEpisode?,
    val attempts: List<TvSourceAttempt>,
    val streams: List<TvSourceStream>,
) {
    val loadingProviderCount: Int = attempts.count { it.status == TvSourceAttemptStatus.LOADING }

    val playableStreams: List<TvSourceStream> = streams.filter { it.playable }

    val sourceLabels: List<String> = playableStreams
        .map { it.sourceLabel }
        .filter { it.isNotBlank() }
        .distinct()

    fun filtered(sourceLabel: String?): List<TvSourceStream> =
        if (sourceLabel == null) playableStreams else playableStreams.filter { it.sourceLabel == sourceLabel }
}

sealed interface TvSourcesUiState {
    data class Loading(val snapshot: TvSourcesSnapshot? = null) : TvSourcesUiState
    data class Ready(val snapshot: TvSourcesSnapshot) : TvSourcesUiState
    data class Empty(val snapshot: TvSourcesSnapshot) : TvSourcesUiState
    data class Error(val message: String) : TvSourcesUiState
}
