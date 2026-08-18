package app.nudroidlabs.nustrim.tv.home

import app.nudroidlabs.nustrim.core.library.LocalMediaEntry
import app.nudroidlabs.nustrim.core.model.MediaItem
import app.nudroidlabs.nustrim.core.source.SourceSession

data class TvHomeEntry(
    val sourceUrl: String,
    val session: SourceSession?,
    val item: MediaItem,
    val catalogName: String,
    val continueEntry: LocalMediaEntry? = null
) {
    val stableKey: String
        get() = continueEntry?.key
            ?: "${sourceUrl}|${item.ref?.metaId.orEmpty()}|${item.id}|${item.title}"
}

data class TvHomeSection(
    val key: String,
    val title: String,
    val entries: List<TvHomeEntry>
)
