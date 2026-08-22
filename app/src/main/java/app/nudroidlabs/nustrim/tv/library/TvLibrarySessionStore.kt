package app.nudroidlabs.nustrim.tv.library

internal class TvLibraryMemory {
    var section: TvLibrarySection = TvLibrarySection.SAVED
    var typeFilter: TvLibraryTypeFilter = TvLibraryTypeFilter.ALL
    var sort: TvLibrarySort = TvLibrarySort.RECENT
    var watchedFilter: TvLibraryWatchedFilter = TvLibraryWatchedFilter.ALL
    var lastMediaKey: String? = null
    var firstVisibleItemIndex: Int = 0
}

internal object TvLibrarySessionStore {
    private val memories = mutableMapOf<String, TvLibraryMemory>()

    fun memory(scopeKey: String): TvLibraryMemory =
        memories.getOrPut(scopeKey) { TvLibraryMemory() }
}
