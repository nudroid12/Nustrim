package app.nudroidlabs.nustrim.tv.settings

internal class TvSettingsMemory {
    var selectedCategory: TvSettingsCategory = TvSettingsCategory.PLAYBACK
    var contentManagerSection: TvContentManagerSection = TvContentManagerSection.ADDONS
    val lastDetailAnchor = mutableMapOf<TvSettingsCategory, String>()
}

internal object TvSettingsSessionStore {
    private val memories = mutableMapOf<String, TvSettingsMemory>()

    fun memory(scopeKey: String): TvSettingsMemory =
        memories.getOrPut(scopeKey) { TvSettingsMemory() }
}
