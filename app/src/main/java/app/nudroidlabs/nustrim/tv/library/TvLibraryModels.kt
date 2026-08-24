package app.nudroidlabs.nustrim.tv.library

import app.nudroidlabs.nustrim.core.library.LocalMediaEntry

enum class TvLibrarySection(val label: String) {
    SAVED("Saved"),
}

enum class TvLibraryTypeFilter(val label: String) {
    ALL("All"),
    MOVIES("Movies"),
    SERIES("Series"),
}

enum class TvLibrarySort(val label: String) {
    RECENT("Recently added"),
    OLDEST("Oldest added"),
    TITLE_ASC("A to Z"),
    TITLE_DESC("Z to A"),
}

enum class TvLibraryWatchedFilter(val label: String) {
    ALL("All status"),
    WATCHED("Watched"),
    UNWATCHED("Unwatched"),
}

data class TvLibraryMedia(
    val entry: LocalMediaEntry,
    val watched: Boolean,
) {
    val stableKey: String = entry.key
}
