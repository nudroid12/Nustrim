package app.nudroidlabs.nustrim.tv.player

import app.nudroidlabs.nustrim.core.model.MediaEpisode
import app.nudroidlabs.nustrim.core.model.MediaItem
import app.nudroidlabs.nustrim.core.model.StreamSource

data class TvPlaybackRequest(
    val mediaKey: String,
    val sourceUrl: String,
    val media: MediaItem,
    val episode: MediaEpisode?,
    val stream: StreamSource,
    val streamSourceLabel: String,
) {
    val stableKey: String = buildString {
        append(mediaKey)
        append('/')
        append(episode?.id ?: "movie")
        append('/')
        append(streamSourceLabel.hashCode().toString(16))
        append('/')
        append(stream.url.hashCode().toString(16))
    }
}
