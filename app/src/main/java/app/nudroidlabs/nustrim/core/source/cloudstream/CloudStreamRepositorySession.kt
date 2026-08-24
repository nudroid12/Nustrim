package app.nudroidlabs.nustrim.core.source.cloudstream

import app.nudroidlabs.nustrim.core.model.MediaCatalog
import app.nudroidlabs.nustrim.core.model.MediaEpisode
import app.nudroidlabs.nustrim.core.model.MediaItem
import app.nudroidlabs.nustrim.core.model.MediaRef
import app.nudroidlabs.nustrim.core.model.MediaType
import app.nudroidlabs.nustrim.core.model.StreamSource
import app.nudroidlabs.nustrim.core.source.ChildSourceOpener
import app.nudroidlabs.nustrim.core.source.HttpJsonClient
import app.nudroidlabs.nustrim.core.source.SourceCapabilities
import app.nudroidlabs.nustrim.core.source.SourceKind
import app.nudroidlabs.nustrim.core.source.SourceSession
import org.json.JSONArray
import org.json.JSONObject

class CloudStreamRepositorySession private constructor(
    override val displayName: String,
    override val description: String,
    private val pluginLists: List<String>,
    private val sourceUrl: String,
    private val http: HttpJsonClient,
    private val runtime: CloudStreamRuntime,
    private val cache: CloudStreamRepositoryCache,
    private val forceRefresh: Boolean,
) : SourceSession, ChildSourceOpener {
    override val id: String = "cloudstream:$sourceUrl"
    override val kind: SourceKind = SourceKind.CLOUDSTREAM
    override val capabilities: SourceCapabilities = SourceCapabilities(
        resources = setOf("providers")
    )

    override fun loadCatalog(onSuccess: (MediaCatalog) -> Unit, onError: (Throwable) -> Unit) {
        http.run(
            block = {
                val providers = mutableListOf<MediaItem>()
                pluginLists.forEach { pluginListUrl ->
                    val cached = if (forceRefresh) null else cache.readFresh(pluginListUrl)
                    val text = cached ?: runCatching { http.getTextBlocking(pluginListUrl) }
                        .onSuccess { cache.write(pluginListUrl, it) }
                        .getOrElse { error ->
                            cache.readStale(pluginListUrl) ?: throw error
                        }
                    val array = JSONArray(text)
                    for (i in 0 until array.length()) {
                        val plugin = array.optJSONObject(i) ?: continue
                        val status = plugin.optInt("status", 1)
                        if (status == 0) continue

                        val internalName = plugin.optString("internalName", "").trim()
                        val name = plugin.optString("name", internalName.ifBlank { "Provider ${i + 1}" }).trim()
                        val language = plugin.optString("language", "").trim()
                        val types = plugin.optJSONArray("tvTypes").toStringList()
                        val packageUrl = plugin.optString("url", "").trim()
                        if (packageUrl.isBlank()) continue

                        val details = buildList {
                            plugin.optString("description", "").takeIf { it.isNotBlank() }?.let(::add)
                            if (language.isNotBlank()) add("Language: $language")
                            if (types.isNotEmpty()) add("Types: ${types.joinToString()}")
                            add("Package: $packageUrl")
                            add("Status: $status")
                        }.joinToString("\n")

                        providers += MediaItem(
                            id = internalName.ifBlank { name },
                            title = name,
                            description = details,
                            type = types.firstOrNull()?.let(MediaType::from) ?: MediaType.UNKNOWN,
                            ref = MediaRef(
                                sourceKind = "cloudstream-plugin",
                                mediaType = internalName.ifBlank { name },
                                metaId = packageUrl,
                                integrity = plugin.optString("fileHash", "").trim()
                            )
                        )
                    }
                }
                http.postToMain {
                    onSuccess(
                        MediaCatalog(
                            name = displayName,
                            version = 2,
                            items = providers.distinctBy { it.ref?.metaId ?: it.id },
                            sourceLabel = "CloudStream repository · tap a provider to execute its .cs3 package"
                        )
                    )
                }
            },
            onError = onError
        )
    }

    override fun openChild(
        item: MediaItem,
        onSuccess: (SourceSession) -> Unit,
        onError: (Throwable) -> Unit
    ) {
        val ref = item.ref
        val pluginUrl = ref?.metaId?.takeIf { it.isNotBlank() }
        if (pluginUrl == null) {
            onError(IllegalArgumentException("CloudStream provider has no .cs3 package URL"))
            return
        }
        val internalName = ref.mediaType.ifBlank { item.id.ifBlank { item.title } }
        runtime.open(
            pluginUrl = pluginUrl,
            internalName = internalName,
            expectedHash = ref.integrity,
            onSuccess = onSuccess,
            onError = onError
        )
    }

    override fun loadDetails(item: MediaItem, onSuccess: (MediaItem) -> Unit, onError: (Throwable) -> Unit) {
        onSuccess(
            item.copy(
                description = item.description + "\n\nThis card represents a CloudStream plugin. Open it from the repository catalog to load and execute the .cs3 package."
            )
        )
    }

    override fun loadStreams(
        item: MediaItem,
        episode: MediaEpisode?,
        onSuccess: (List<StreamSource>) -> Unit,
        onError: (Throwable) -> Unit
    ) {
        onSuccess(emptyList())
    }

    companion object {
        fun fromJson(
            root: JSONObject,
            sourceUrl: String,
            http: HttpJsonClient,
            runtime: CloudStreamRuntime,
            cache: CloudStreamRepositoryCache,
            forceRefresh: Boolean,
        ): CloudStreamRepositorySession {
            val pluginLists = root.optJSONArray("pluginLists").toStringList()
            require(pluginLists.isNotEmpty()) { "CloudStream repository contains no pluginLists" }
            return CloudStreamRepositorySession(
                displayName = root.optString("name", "CloudStream Repository"),
                description = root.optString("description", "CloudStream extension repository"),
                pluginLists = pluginLists,
                sourceUrl = sourceUrl,
                http = http,
                runtime = runtime,
                cache = cache,
                forceRefresh = forceRefresh,
            )
        }
    }
}

private fun JSONArray?.toStringList(): List<String> = buildList {
    val array = this@toStringList ?: return@buildList
    for (i in 0 until array.length()) {
        val value = array.optString(i).trim()
        if (value.isNotBlank()) add(value)
    }
}
