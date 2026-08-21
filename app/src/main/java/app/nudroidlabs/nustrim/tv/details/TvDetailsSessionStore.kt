package app.nudroidlabs.nustrim.tv.details

internal data class TvDetailsSessionMemory(
    var selectedSeasonKey: String? = null,
    val lastEpisodeBySeason: MutableMap<String, String> = mutableMapOf(),
)

internal object TvDetailsSessionStore {
    private val sessions = object : LinkedHashMap<String, TvDetailsSessionMemory>(32, 0.75f, true) {
        override fun removeEldestEntry(
            eldest: MutableMap.MutableEntry<String, TvDetailsSessionMemory>?,
        ): Boolean = size > 32
    }

    @Synchronized
    fun memory(contentKey: String): TvDetailsSessionMemory =
        sessions.getOrPut(contentKey) { TvDetailsSessionMemory() }
}
