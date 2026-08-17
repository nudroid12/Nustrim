package app.nudroidlabs.nustrim.core.model

data class MediaCatalog(
    val name: String,
    val version: Int = 1,
    val items: List<MediaItem>,
    val sourceLabel: String = ""
)

data class MediaItem(
    val id: String,
    val title: String,
    val description: String = "",
    val type: MediaType = MediaType.UNKNOWN,
    val streams: List<StreamSource> = emptyList(),
    val posterUrl: String = "",
    val backgroundUrl: String = "",
    val releaseInfo: String = "",
    val episodes: List<MediaEpisode> = emptyList(),
    val ref: MediaRef? = null
)

data class MediaEpisode(
    val id: String,
    val title: String,
    val season: Int? = null,
    val episode: Int? = null,
    val thumbnailUrl: String = "",
    val overview: String = ""
) {
    val displayTitle: String
        get() = when {
            season != null && episode != null -> "S${season}E${episode} · $title"
            else -> title
        }
}

data class MediaRef(
    val sourceKind: String,
    val mediaType: String,
    val metaId: String,
    val integrity: String = ""
)

enum class MediaType {
    MOVIE,
    SERIES,
    LIVE,
    CHANNEL,
    TV,
    UNKNOWN;

    companion object {
        fun from(value: String?): MediaType = when (value?.trim()?.lowercase()) {
            "movie", "movies", "animemovie" -> MOVIE
            "series", "tvseries", "anime", "asiandrama", "ova" -> SERIES
            "live" -> LIVE
            "channel" -> CHANNEL
            "tv" -> TV
            else -> UNKNOWN
        }
    }
}

data class SubtitleSource(
    val id: String = "",
    val url: String,
    val language: String = "",
    val label: String = "",
    val headers: Map<String, String> = emptyMap()
)

data class StreamSource(
    val name: String,
    val url: String = "",
    val type: String = "auto",
    val headers: Map<String, String> = emptyMap(),
    val playable: Boolean = url.isNotBlank(),
    val note: String = "",
    val providerId: String = "",
    val providerName: String = "",
    val subtitles: List<SubtitleSource> = emptyList()
)
