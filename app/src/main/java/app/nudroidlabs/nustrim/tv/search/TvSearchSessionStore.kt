package app.nudroidlabs.nustrim.tv.search

class TvSearchMemory internal constructor() {
    var query: String = ""
    var discoverSnapshot: TvSearchSnapshot? = null
    var searchQuery: String = ""
    var searchSnapshot: TvSearchSnapshot? = null
    var focusedRowIndex: Int = 0
    var lastFocusedRowKey: String? = null
    var lastFocusedMediaKey: String? = null
    val rowItemPositions: MutableMap<String, Int> = mutableMapOf()
}

object TvSearchSessionStore {
    private const val MAX_ENTRIES = 8

    private val memories = object : LinkedHashMap<String, TvSearchMemory>(12, 0.75f, true) {
        override fun removeEldestEntry(
            eldest: MutableMap.MutableEntry<String, TvSearchMemory>?,
        ): Boolean = size > MAX_ENTRIES
    }

    @Synchronized
    fun memory(scopeKey: String): TvSearchMemory =
        memories.getOrPut(scopeKey) { TvSearchMemory() }
}
