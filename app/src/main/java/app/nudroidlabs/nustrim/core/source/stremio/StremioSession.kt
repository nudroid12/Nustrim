package app.nudroidlabs.nustrim.core.source.stremio

import android.net.Uri
import app.nudroidlabs.nustrim.core.model.MediaCatalog
import app.nudroidlabs.nustrim.core.model.MediaEpisode
import app.nudroidlabs.nustrim.core.model.MediaItem
import app.nudroidlabs.nustrim.core.model.StreamSource
import app.nudroidlabs.nustrim.core.model.SubtitleSource
import app.nudroidlabs.nustrim.core.source.CatalogSectionSourceSession
import app.nudroidlabs.nustrim.core.source.HttpJsonClient
import app.nudroidlabs.nustrim.core.source.SearchableSourceSession
import app.nudroidlabs.nustrim.core.source.SourceCapabilities
import app.nudroidlabs.nustrim.core.source.SourceKind
import app.nudroidlabs.nustrim.core.source.SourceSession
import org.json.JSONObject

class StremioSession(
    private val manifestUrl: String,
    private val manifest: StremioManifest,
    private val http: HttpJsonClient
) : SourceSession, CatalogSectionSourceSession, SearchableSourceSession {
    override val id: String = manifest.id
    override val displayName: String = manifest.name
    override val description: String = manifest.description
    override val kind: SourceKind = SourceKind.STREMIO

    private val addonBaseUrl = manifestUrl.substringBeforeLast("/manifest.json")
    override val capabilities: SourceCapabilities = SourceCapabilities(
        resources = manifest.resources.map { it.name }.toSet(),
        searchable = manifest.catalogs.any { it.supportsSearch },
        configurable = manifest.configurable,
        configurationRequired = manifest.configurationRequired,
        configureUrl = if (manifest.configurable || manifest.configurationRequired) {
            addonBaseUrl.trimEnd('/') + "/configure"
        } else {
            ""
        }
    )

    override fun loadCatalogSections(
        onSuccess: (List<MediaCatalog>) -> Unit,
        onError: (Throwable) -> Unit
    ) {
        val catalogDefs = manifest.catalogs
            .filter { !it.requiresExtra }
            .filter { manifest.hasResource("catalog") }

        if (catalogDefs.isEmpty()) {
            onSuccess(emptyList())
            return
        }

        http.run(
            block = {
                val sections = mutableListOf<MediaCatalog>()
                catalogDefs.forEach { catalog ->
                    val result = runCatching {
                        val url = resourceUrl(addonBaseUrl, "catalog", catalog.type, catalog.id)
                        val text = http.getTextBlocking(url)
                        StremioParser.parseCatalog(text).take(MAX_ITEMS_PER_CATALOG)
                    }.getOrElse { emptyList() }

                    if (result.isNotEmpty()) {
                        sections += MediaCatalog(
                            name = catalogDisplayName(catalog),
                            version = 1,
                            items = result.distinctBy { "${it.ref?.mediaType}:${it.id}" },
                            sourceLabel = manifest.name
                        )
                    }
                }

                http.postToMain { onSuccess(sections) }
            },
            onError = onError
        )
    }

    override fun loadCatalog(onSuccess: (MediaCatalog) -> Unit, onError: (Throwable) -> Unit) {
        loadCatalogSections(
            onSuccess = { sections ->
                onSuccess(
                    MediaCatalog(
                        name = manifest.name,
                        version = 1,
                        items = sections
                            .flatMap { it.items }
                            .distinctBy { "${it.ref?.mediaType}:${it.id}" },
                        sourceLabel = "Stremio · ${manifest.name}"
                    )
                )
            },
            onError = onError
        )
    }

    override fun search(
        query: String,
        onSuccess: (MediaCatalog) -> Unit,
        onError: (Throwable) -> Unit
    ) {
        val clean = query.trim()
        if (clean.isBlank()) {
            onSuccess(MediaCatalog(name = "Search", items = emptyList(), sourceLabel = manifest.name))
            return
        }

        val searchable = manifest.catalogs
            .filter { it.supportsSearch }
            .filter { manifest.hasResource("catalog") }

        if (searchable.isEmpty()) {
            onSuccess(MediaCatalog(name = "Search", items = emptyList(), sourceLabel = manifest.name))
            return
        }

        http.run(
            block = {
                val results = mutableListOf<MediaItem>()
                chooseSearchCatalogs(searchable).forEach { catalog ->
                    val url = catalogSearchUrl(addonBaseUrl, catalog, clean)
                    val text = http.getTextBlocking(url)
                    results += StremioParser.parseCatalog(text).take(MAX_SEARCH_ITEMS_PER_CATALOG)
                }
                http.postToMain {
                    onSuccess(
                        MediaCatalog(
                            name = "Search · ${manifest.name}",
                            items = results.distinctBy { "${it.ref?.mediaType}:${it.id}" },
                            sourceLabel = manifest.name
                        )
                    )
                }
            },
            onError = onError
        )
    }

    override fun loadDetails(item: MediaItem, onSuccess: (MediaItem) -> Unit, onError: (Throwable) -> Unit) {
        val type = item.ref?.mediaType ?: item.type.name.lowercase()
        val id = item.ref?.metaId ?: item.id

        if (manifest.supports("meta", type, id)) {
            val url = resourceUrl(addonBaseUrl, "meta", type, id)
            http.getText(
                url,
                onSuccess = { text -> onSuccess(StremioParser.parseMetaResponse(text) ?: item) },
                onError = {
                    if (id.startsWith("tt")) loadCinemetaMeta(item, type, id, onSuccess)
                    else onSuccess(item)
                }
            )
        } else if (id.startsWith("tt")) {
            loadCinemetaMeta(item, type, id, onSuccess)
        } else {
            onSuccess(item)
        }
    }

    override fun loadStreams(
        item: MediaItem,
        episode: MediaEpisode?,
        onSuccess: (List<StreamSource>) -> Unit,
        onError: (Throwable) -> Unit
    ) {
        if (item.streams.isNotEmpty() && episode == null) {
            onSuccess(item.streams)
            return
        }

        val type = item.ref?.mediaType ?: item.type.name.lowercase()
        val streamId = episode?.id ?: item.ref?.metaId ?: item.id

        if (!manifest.supports("stream", type, streamId) && !manifest.hasResource("stream")) {
            onSuccess(emptyList())
            return
        }

        val url = resourceUrl(addonBaseUrl, "stream", type, streamId)
        http.getText(
            url,
            onSuccess = { text -> onSuccess(StremioParser.parseStreams(text)) },
            onError = onError
        )
    }

    override fun loadSubtitles(
        item: MediaItem,
        episode: MediaEpisode?,
        onSuccess: (List<SubtitleSource>) -> Unit,
        onError: (Throwable) -> Unit
    ) {
        val type = item.ref?.mediaType ?: item.type.name.lowercase()
        val subtitleId = episode?.id ?: item.ref?.metaId ?: item.id
        if (!manifest.supports("subtitles", type, subtitleId)) {
            onSuccess(emptyList())
            return
        }

        val url = resourceUrl(addonBaseUrl, "subtitles", type, subtitleId)
        http.getText(
            url,
            onSuccess = { text ->
                val parsed = runCatching {
                    val array = JSONObject(text).optJSONArray("subtitles")
                    buildList {
                        if (array != null) {
                            for (index in 0 until array.length()) {
                                val obj = array.optJSONObject(index) ?: continue
                                val subtitleUrl = obj.optString("url").trim()
                                if (subtitleUrl.isBlank()) continue
                                val lang = obj.optString("lang").trim()
                                add(
                                    SubtitleSource(
                                        id = obj.optString("id"),
                                        url = subtitleUrl,
                                        language = lang,
                                        label = obj.optString("name").ifBlank { lang }
                                    )
                                )
                            }
                        }
                    }
                }.getOrElse { emptyList() }
                onSuccess(parsed.distinctBy { "${it.url}|${it.language}" })
            },
            onError = { onSuccess(emptyList()) }
        )
    }

    private fun loadCinemetaMeta(
        original: MediaItem,
        type: String,
        id: String,
        onSuccess: (MediaItem) -> Unit
    ) {
        val url = resourceUrl(CINEMETA_BASE, "meta", type, id)
        http.getText(
            url,
            onSuccess = { text -> onSuccess(StremioParser.parseMetaResponse(text) ?: original) },
            onError = { onSuccess(original) }
        )
    }

    private fun catalogDisplayName(catalog: StremioCatalogDef): String {
        val typeLabel = when (catalog.type.lowercase()) {
            "movie" -> "Movies"
            "series" -> "Series"
            else -> catalog.type.replaceFirstChar { it.uppercase() }
        }
        return "${catalog.name} - $typeLabel"
    }

    private fun chooseSearchCatalogs(catalogs: List<StremioCatalogDef>): List<StremioCatalogDef> {
        val chosen = mutableListOf<StremioCatalogDef>()
        listOf("movie", "series").forEach { type ->
            catalogs.firstOrNull { it.type.equals(type, ignoreCase = true) }?.let(chosen::add)
        }
        if (chosen.isEmpty()) chosen += catalogs.take(MAX_SEARCH_CATALOGS)
        return chosen.take(MAX_SEARCH_CATALOGS)
    }

    private fun catalogSearchUrl(base: String, catalog: StremioCatalogDef, query: String): String {
        val extra = "search=${Uri.encode(query)}"
        return "${base.trimEnd('/')}/catalog/${Uri.encode(catalog.type)}/${Uri.encode(catalog.id)}/$extra.json"
    }

    private fun resourceUrl(base: String, resource: String, type: String, id: String): String =
        "${base.trimEnd('/')}/$resource/${Uri.encode(type)}/${Uri.encode(id)}.json"

    companion object {
        private const val CINEMETA_BASE = "https://v3-cinemeta.strem.io"
        private const val MAX_ITEMS_PER_CATALOG = 40
        private const val MAX_SEARCH_CATALOGS = 2
        private const val MAX_SEARCH_ITEMS_PER_CATALOG = 30
    }
}
