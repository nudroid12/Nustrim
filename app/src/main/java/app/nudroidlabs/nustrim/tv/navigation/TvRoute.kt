package app.nudroidlabs.nustrim.tv.navigation

import app.nudroidlabs.nustrim.core.model.MediaEpisode
import app.nudroidlabs.nustrim.core.model.MediaItem

enum class TvRootDestination {
    HOME,
    SEARCH,
    LIBRARY,
    SETTINGS,
}

data class TvReturnFocus(
    val scopeKey: String,
    val anchorKey: String? = null,
    val row: Int? = null,
    val column: Int? = null,
)

sealed interface TvRoute {
    val stableKey: String
    val focusScope: String

    data class Root(
        val destination: TvRootDestination,
    ) : TvRoute {
        override val stableKey: String = "root/${destination.name.lowercase()}"
        override val focusScope: String = stableKey
    }

    data class Details(
        val contentKey: String,
        val sourceUrl: String,
        val media: MediaItem,
        val returnFocus: TvReturnFocus? = null,
    ) : TvRoute {
        override val stableKey: String = "details/$contentKey"
        override val focusScope: String = stableKey
    }

    data class Sources(
        val mediaKey: String,
        val sourceUrl: String,
        val media: MediaItem,
        val episode: MediaEpisode? = null,
        val returnFocus: TvReturnFocus? = null,
    ) : TvRoute {
        override val stableKey: String = buildString {
            append("sources/")
            append(mediaKey)
            episode?.id?.takeIf { it.isNotBlank() }?.let {
                append("/")
                append(it)
            }
        }
        override val focusScope: String = stableKey
    }

    data class Player(
        val playbackRequestKey: String,
        val returnFocus: TvReturnFocus? = null,
    ) : TvRoute {
        override val stableKey: String = "player/$playbackRequestKey"
        override val focusScope: String = stableKey
    }
}
