package app.nudroidlabs.nustrim.core.source

import app.nudroidlabs.nustrim.core.model.MediaCatalog
import app.nudroidlabs.nustrim.core.model.MediaEpisode
import app.nudroidlabs.nustrim.core.model.MediaItem
import app.nudroidlabs.nustrim.core.model.StreamSource
import app.nudroidlabs.nustrim.core.repository.RepositoryParser

class NustrimJsonSession(
    private val catalog: MediaCatalog,
    private val sourceUrl: String
) : SourceSession {
    override val id: String = "nustrim-json:$sourceUrl"
    override val displayName: String = catalog.name
    override val description: String = "Nustrim JSON repository"
    override val kind: SourceKind = SourceKind.NUSTRIM_JSON

    override fun loadCatalog(onSuccess: (MediaCatalog) -> Unit, onError: (Throwable) -> Unit) {
        onSuccess(catalog.copy(sourceLabel = "Nustrim JSON"))
    }

    override fun loadDetails(item: MediaItem, onSuccess: (MediaItem) -> Unit, onError: (Throwable) -> Unit) {
        onSuccess(item)
    }

    override fun loadStreams(
        item: MediaItem,
        episode: MediaEpisode?,
        onSuccess: (List<StreamSource>) -> Unit,
        onError: (Throwable) -> Unit
    ) {
        onSuccess(item.streams)
    }

    companion object {
        fun fromJson(json: String, sourceUrl: String): NustrimJsonSession =
            NustrimJsonSession(RepositoryParser.parse(json), sourceUrl)
    }
}
