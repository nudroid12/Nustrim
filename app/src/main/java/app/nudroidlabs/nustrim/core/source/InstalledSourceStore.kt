package app.nudroidlabs.nustrim.core.source

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

enum class SourcePreset {
    USER,
    CORE_DEFAULT,
    DEVELOPER_DEFAULT
}

data class InstalledSource(
    val url: String,
    val enabled: Boolean = true,
    val preset: SourcePreset = SourcePreset.USER,
    val label: String = ""
) {
    val isPinned: Boolean get() = preset != SourcePreset.USER
    val developerOnly: Boolean get() = preset == SourcePreset.DEVELOPER_DEFAULT
}

class InstalledSourceStore(context: Context) {
    private val preferences = context.getSharedPreferences("nustrim_installed_sources", Context.MODE_PRIVATE)

    init {
        ensureCoreDefaults()
    }

    fun sources(): List<InstalledSource> = readSources()

    fun visibleSources(developerMode: Boolean): List<InstalledSource> =
        readSources().filter { !it.developerOnly || developerMode }

    fun enabledUrls(developerMode: Boolean): List<String> =
        readSources()
            .filter { it.enabled }
            .filter { !it.developerOnly || developerMode }
            .map { it.url }

    fun add(url: String) {
        val clean = url.trim()
        if (clean.isBlank()) return
        val current = readSources().toMutableList()
        val index = current.indexOfFirst { it.url == clean }
        if (index >= 0) {
            current[index] = current[index].copy(enabled = true)
        } else {
            current += InstalledSource(url = clean)
        }
        save(current)
    }

    fun setEnabled(url: String, enabled: Boolean) {
        save(readSources().map { if (it.url == url) it.copy(enabled = enabled) else it })
    }

    fun move(url: String, delta: Int) {
        if (delta == 0) return
        val current = readSources().toMutableList()
        val from = current.indexOfFirst { it.url == url }
        if (from < 0) return
        val to = (from + delta).coerceIn(0, current.lastIndex)
        if (from == to) return
        val entry = current.removeAt(from)
        current.add(to, entry)
        save(current)
    }

    fun exportJson(): String = encode(readSources()).toString()

    fun importJson(raw: String) {
        val imported = decode(JSONArray(raw))
        save(imported)
        ensureCoreDefaults()
    }

    fun remove(url: String) {
        save(readSources().filterNot { it.url == url && !it.isPinned })
    }

    fun ensureDeveloperDefaults() {
        var current = readSources()
        var changed = false
        DEVELOPER_DEFAULTS.forEach { default ->
            if (current.none { it.url == default.url }) {
                current = current + default
                changed = true
            }
        }
        if (changed) save(current)
    }

    private fun ensureCoreDefaults() {
        var current = readSourcesInternal()
        var changed = false
        CORE_DEFAULTS.forEach { default ->
            if (current.none { it.url == default.url }) {
                current = current + default
                changed = true
            }
        }
        if (changed || !preferences.contains(KEY_SOURCES)) save(current)
    }

    private fun readSources(): List<InstalledSource> = readSourcesInternal()

    private fun readSourcesInternal(): List<InstalledSource> {
        val raw = if (preferences.contains(KEY_SOURCES)) {
            preferences.getString(KEY_SOURCES, "[]").orEmpty()
        } else {
            preferences.getString(LEGACY_KEY_URLS, "[]").orEmpty()
        }
        return runCatching { decode(JSONArray(raw)) }.getOrDefault(emptyList())
    }

    private fun decode(array: JSONArray): List<InstalledSource> = buildList {
        for (index in 0 until array.length()) {
            when (val value = array.opt(index)) {
                is JSONObject -> {
                    val url = value.optString("url").trim()
                    if (url.isBlank()) continue
                    val preset = runCatching {
                        SourcePreset.valueOf(value.optString("preset", SourcePreset.USER.name))
                    }.getOrDefault(SourcePreset.USER)
                    add(
                        InstalledSource(
                            url = url,
                            enabled = value.optBoolean("enabled", true),
                            preset = preset,
                            label = value.optString("label")
                        )
                    )
                }
                is String -> {
                    val url = value.trim()
                    if (url.isNotBlank()) add(InstalledSource(url = url))
                }
            }
        }
    }.distinctBy { it.url }

    private fun encode(sources: List<InstalledSource>): JSONArray = JSONArray().apply {
        sources.distinctBy { it.url }.forEach { source ->
            put(
                JSONObject()
                    .put("url", source.url)
                    .put("enabled", source.enabled)
                    .put("preset", source.preset.name)
                    .put("label", source.label)
            )
        }
    }

    private fun save(sources: List<InstalledSource>) {
        preferences.edit().putString(KEY_SOURCES, encode(sources).toString()).apply()
    }

    companion object {
        const val CINEMETA_URL = "https://v3-cinemeta.strem.io/"
        const val OPENSUBTITLES_URL = "https://opensubtitles-v3.strem.io/"
        const val DEV_CLOUDSTREAM_URL = "https://raw.githubusercontent.com/Luckez12/Cloudstream-Repo/refs/heads/main/repo.json"
        const val DEV_YASTREAM_URL = "https://yastream.tamthai.de/manifest.json"

        private const val KEY_SOURCES = "sources_v2"
        private const val LEGACY_KEY_URLS = "urls"

        private val CORE_DEFAULTS = listOf(
            InstalledSource(CINEMETA_URL, preset = SourcePreset.CORE_DEFAULT, label = "Cinemeta"),
            InstalledSource(OPENSUBTITLES_URL, preset = SourcePreset.CORE_DEFAULT, label = "OpenSubtitles v3")
        )

        private val DEVELOPER_DEFAULTS = listOf(
            InstalledSource(DEV_CLOUDSTREAM_URL, preset = SourcePreset.DEVELOPER_DEFAULT, label = "CloudStream Repo"),
            InstalledSource(DEV_YASTREAM_URL, preset = SourcePreset.DEVELOPER_DEFAULT, label = "Yastream")
        )
    }
}
