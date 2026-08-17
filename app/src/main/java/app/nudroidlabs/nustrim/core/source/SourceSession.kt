package app.nudroidlabs.nustrim.core.source

import app.nudroidlabs.nustrim.core.model.MediaCatalog
import app.nudroidlabs.nustrim.core.model.MediaEpisode
import app.nudroidlabs.nustrim.core.model.MediaItem
import app.nudroidlabs.nustrim.core.model.StreamSource
import app.nudroidlabs.nustrim.core.model.SubtitleSource

enum class SourceKind {
    NUSTRIM_JSON,
    STREMIO,
    CLOUDSTREAM
}

data class SourceCapabilities(
    val resources: Set<String> = emptySet(),
    val searchable: Boolean = false,
    val configurable: Boolean = false,
    val configurationRequired: Boolean = false,
    val configureUrl: String = ""
)

interface SourceSession {
    val id: String
    val displayName: String
    val description: String
    val kind: SourceKind
    val capabilities: SourceCapabilities
        get() = SourceCapabilities()

    fun loadCatalog(
        onSuccess: (MediaCatalog) -> Unit,
        onError: (Throwable) -> Unit
    )

    fun loadDetails(
        item: MediaItem,
        onSuccess: (MediaItem) -> Unit,
        onError: (Throwable) -> Unit
    )

    fun loadStreams(
        item: MediaItem,
        episode: MediaEpisode? = null,
        onSuccess: (List<StreamSource>) -> Unit,
        onError: (Throwable) -> Unit
    )

    fun loadSubtitles(
        item: MediaItem,
        episode: MediaEpisode? = null,
        onSuccess: (List<SubtitleSource>) -> Unit,
        onError: (Throwable) -> Unit
    ) {
        onSuccess(emptyList())
    }
}

/**
 * A source that exposes multiple independent home/catalog rows.
 *
 * This is intentionally separate from SourceSession.loadCatalog() so old
 * CloudStream/Nustrim providers keep working while Stremio addons can preserve
 * their manifest catalog identity all the way to Home.
 */
interface CatalogSectionSourceSession {
    fun loadCatalogSections(
        onSuccess: (List<MediaCatalog>) -> Unit,
        onError: (Throwable) -> Unit
    )
}

/** A source that contains child sources, such as a CloudStream repository containing .cs3 plugins. */
interface ChildSourceOpener {
    fun openChild(
        item: MediaItem,
        onSuccess: (SourceSession) -> Unit,
        onError: (Throwable) -> Unit
    )
}

/** Optional search capability exposed by a source/provider. */
interface SearchableSourceSession {
    fun search(
        query: String,
        onSuccess: (MediaCatalog) -> Unit,
        onError: (Throwable) -> Unit
    )
}
