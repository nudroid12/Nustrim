package app.nudroidlabs.nustrim.tv.sources

import java.util.LinkedHashMap

class TvSourcesMemory internal constructor() {
    var selectedSourceLabel: String? = null
    var lastFocusedStreamKey: String? = null
    var firstVisibleItemIndex: Int = 0
}

object TvSourcesSessionStore {
    private const val MAX_ENTRIES = 20

    private val memories = object : LinkedHashMap<String, TvSourcesMemory>(24, 0.75f, true) {
        override fun removeEldestEntry(
            eldest: MutableMap.MutableEntry<String, TvSourcesMemory>?,
        ): Boolean = size > MAX_ENTRIES
    }

    @Synchronized
    fun memory(routeKey: String): TvSourcesMemory =
        memories.getOrPut(routeKey) { TvSourcesMemory() }
}
