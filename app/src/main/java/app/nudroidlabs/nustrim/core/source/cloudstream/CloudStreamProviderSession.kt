package app.nudroidlabs.nustrim.core.source.cloudstream

import android.os.Handler
import android.os.Looper
import app.nudroidlabs.nustrim.core.diagnostics.NustrimDiagnostics
import app.nudroidlabs.nustrim.core.model.MediaCatalog
import app.nudroidlabs.nustrim.core.model.MediaEpisode
import app.nudroidlabs.nustrim.core.model.MediaItem
import app.nudroidlabs.nustrim.core.model.MediaRef
import app.nudroidlabs.nustrim.core.model.MediaType
import app.nudroidlabs.nustrim.core.model.StreamSource
import app.nudroidlabs.nustrim.core.source.ChildSourceOpener
import app.nudroidlabs.nustrim.core.source.SearchableSourceSession
import app.nudroidlabs.nustrim.core.source.SourceKind
import app.nudroidlabs.nustrim.core.source.SourceSession
import com.lagradost.cloudstream3.AnimeLoadResponse
import com.lagradost.cloudstream3.LiveStreamLoadResponse
import com.lagradost.cloudstream3.LoadResponse
import com.lagradost.cloudstream3.MainAPI
import com.lagradost.cloudstream3.MainPageRequest
import com.lagradost.cloudstream3.MovieLoadResponse
import com.lagradost.cloudstream3.SearchResponse
import com.lagradost.cloudstream3.TorrentLoadResponse
import com.lagradost.cloudstream3.TvSeriesLoadResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap

/**
 * Container for a .cs3 package. Most packages register exactly one MainAPI.
 * Packages that register multiple providers expose them as child source cards.
 */
class CloudStreamProviderContainerSession(
    private val pluginUrl: String,
    private val pluginName: String,
    private val providers: List<MainAPI>
) : SourceSession, ChildSourceOpener {
    val providerNames: List<String> = providers.map { it.name }
    override val id: String = "cloudstream-plugin:$pluginUrl"
    override val displayName: String = if (providers.size == 1) providers.first().name else pluginName
    override val description: String = if (providers.size == 1) {
        "CloudStream .cs3 · ${providers.first().lang} · ${providers.first().mainUrl}"
    } else {
        "CloudStream .cs3 · ${providers.size} provider(s)"
    }
    override val kind: SourceKind = SourceKind.CLOUDSTREAM

    private val directSession: CloudStreamProviderSession? = providers.singleOrNull()?.let {
        CloudStreamProviderSession(pluginUrl, it)
    }

    override fun loadCatalog(onSuccess: (MediaCatalog) -> Unit, onError: (Throwable) -> Unit) {
        val direct = directSession
        if (direct != null) {
            direct.loadCatalog(onSuccess, onError)
            return
        }

        val items = providers.mapIndexed { index, provider ->
            MediaItem(
                id = "$pluginUrl#$index",
                title = provider.name,
                description = "Language: ${provider.lang}\nURL: ${provider.mainUrl}",
                type = provider.supportedTypes.firstOrNull()?.name?.let(MediaType::from) ?: MediaType.UNKNOWN,
                ref = MediaRef(
                    sourceKind = "cloudstream-loaded-provider",
                    mediaType = index.toString(),
                    metaId = pluginUrl
                )
            )
        }
        onSuccess(
            MediaCatalog(
                name = pluginName,
                items = items,
                sourceLabel = "CloudStream .cs3 · multiple MainAPI providers"
            )
        )
    }

    override fun openChild(item: MediaItem, onSuccess: (SourceSession) -> Unit, onError: (Throwable) -> Unit) {
        if (directSession != null) {
            onSuccess(directSession)
            return
        }
        val index = item.ref?.mediaType?.toIntOrNull()
        val provider = index?.let(providers::getOrNull)
        if (provider == null) {
            onError(IllegalArgumentException("CloudStream provider reference is invalid"))
        } else {
            onSuccess(CloudStreamProviderSession(pluginUrl, provider))
        }
    }

    fun openProvider(
        providerName: String,
        onSuccess: (CloudStreamProviderSession) -> Unit,
        onError: (Throwable) -> Unit,
    ) {
        val provider = providers.firstOrNull { it.name == providerName }
            ?: providers.singleOrNull()
        if (provider == null) {
            onError(IllegalArgumentException("CloudStream provider is unavailable: $providerName"))
        } else {
            onSuccess(CloudStreamProviderSession(pluginUrl, provider))
        }
    }

    override fun loadDetails(item: MediaItem, onSuccess: (MediaItem) -> Unit, onError: (Throwable) -> Unit) {
        directSession?.loadDetails(item, onSuccess, onError) ?: onSuccess(item)
    }

    override fun loadStreams(
        item: MediaItem,
        episode: MediaEpisode?,
        onSuccess: (List<StreamSource>) -> Unit,
        onError: (Throwable) -> Unit
    ) {
        directSession?.loadStreams(item, episode, onSuccess, onError) ?: onSuccess(emptyList())
    }
}

class CloudStreamProviderSession(
    private val pluginUrl: String,
    private val provider: MainAPI
) : SourceSession, SearchableSourceSession {
    override val id: String = "cloudstream-provider:$pluginUrl#${provider.name}"
    override val displayName: String = provider.name
    override val description: String = "CloudStream .cs3 · ${provider.lang} · ${provider.mainUrl}"
    override val kind: SourceKind = SourceKind.CLOUDSTREAM

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val main = Handler(Looper.getMainLooper())
    private val itemLinkData = ConcurrentHashMap<String, String>()
    private val itemLoadUrl = ConcurrentHashMap<String, String>()
    private val itemUnsupportedTransport = ConcurrentHashMap<String, String>()

    override fun loadCatalog(onSuccess: (MediaCatalog) -> Unit, onError: (Throwable) -> Unit) {
        launch(onSuccess, onError) {
            val mainPages = provider.mainPage.filter { it.data.isNotBlank() }
            if (!provider.hasMainPage || mainPages.isEmpty()) {
                return@launch MediaCatalog(
                    name = provider.name,
                    items = emptyList(),
                    sourceLabel = "CloudStream .cs3 · use Search"
                )
            }

            val mapped = mutableListOf<MediaItem>()
            for ((requestIndex, page) in mainPages.withIndex()) {
                if (provider.sequentialMainPage && requestIndex > 0 && provider.sequentialMainPageDelay > 0) {
                    delay(provider.sequentialMainPageDelay)
                }
                val response = provider.getMainPage(
                    1,
                    MainPageRequest(page.name, page.data, page.horizontalImages)
                ) ?: continue
                response.items.forEach { homeList ->
                    homeList.list.forEach { search ->
                        mapped += mapSearch(search, homeList.name)
                    }
                }
            }

            MediaCatalog(
                name = provider.name,
                items = mapped.distinctBy { it.id },
                sourceLabel = "CloudStream .cs3 · ${provider.lang} · ${provider.mainUrl}"
            )
        }
    }

    override fun search(query: String, onSuccess: (MediaCatalog) -> Unit, onError: (Throwable) -> Unit) {
        val clean = query.trim()
        if (clean.isBlank()) {
            loadCatalog(onSuccess, onError)
            return
        }
        launch(onSuccess, onError) {
            val results = provider.search(clean).orEmpty().map { mapSearch(it, "Search") }
            MediaCatalog(
                name = "${provider.name} · Search: $clean",
                items = results.distinctBy { it.id },
                sourceLabel = "CloudStream search"
            )
        }
    }

    override fun loadDetails(item: MediaItem, onSuccess: (MediaItem) -> Unit, onError: (Throwable) -> Unit) {
        launch(onSuccess, onError) {
            val loadUrl = item.ref?.metaId?.takeIf { it.isNotBlank() }
                ?: itemLoadUrl[item.id]
                ?: item.id
            val response = provider.load(loadUrl)
                ?: error("${provider.name} returned no detail response")
            itemLoadUrl[item.id] = loadUrl
            mapLoad(item, response)
        }
    }

    override fun loadStreams(
        item: MediaItem,
        episode: MediaEpisode?,
        onSuccess: (List<StreamSource>) -> Unit,
        onError: (Throwable) -> Unit
    ) {
        launch(onSuccess, onError) {
            itemUnsupportedTransport[item.id]?.let { reason ->
                NustrimDiagnostics.log(
                    "CLOUDSTREAM_UNSUPPORTED_TRANSPORT",
                    "provider=${provider.name} reason=$reason",
                )
                return@launch listOf(
                    StreamSource(
                        name = "Unsupported CloudStream transport",
                        playable = false,
                        note = reason
                    )
                )
            }

            val data = episode?.id?.takeIf { it.isNotBlank() }
                ?: itemLinkData[item.id]
                ?: resolveLinkData(item)

            itemUnsupportedTransport[item.id]?.let { reason ->
                return@launch listOf(
                    StreamSource(
                        name = "Unsupported CloudStream transport",
                        playable = false,
                        note = reason
                    )
                )
            }

            if (data.isNullOrBlank()) {
                NustrimDiagnostics.log(
                    "CLOUDSTREAM_LOADLINKS_NO_DATA",
                    "provider=${provider.name} item=${item.title}",
                )
                return@launch listOf(
                    StreamSource(
                        name = "No direct loadLinks data",
                        playable = false,
                        note = "This CloudStream item did not expose playable link data."
                    )
                )
            }

            val links = Collections.synchronizedList(mutableListOf<com.lagradost.cloudstream3.utils.ExtractorLink>())
            val subtitles = Collections.synchronizedList(mutableListOf<com.lagradost.cloudstream3.SubtitleFile>())

            NustrimDiagnostics.log(
                "CLOUDSTREAM_LOADLINKS_START",
                "provider=${provider.name} mode=${if (episode == null) "item" else "episode"}",
            )
            val accepted = try {
                provider.loadLinks(
                    data = data,
                    isCasting = false,
                    subtitleCallback = { subtitles += it },
                    callback = { links += it }
                )
            } catch (error: Throwable) {
                NustrimDiagnostics.error(
                    "CLOUDSTREAM_LOADLINKS_ERROR",
                    error,
                    "provider=${provider.name}",
                )
                throw error
            }
            if (accepted && links.isEmpty()) {
                var waits = 0
                while (links.isEmpty() && waits < LATE_CALLBACK_WAIT_STEPS) {
                    delay(LATE_CALLBACK_WAIT_STEP_MS)
                    waits += 1
                }
                if (links.isNotEmpty()) {
                    NustrimDiagnostics.log(
                        "CLOUDSTREAM_LOADLINKS_LATE_CALLBACK",
                        "provider=${provider.name} waits=$waits links=${links.size}",
                    )
                }
            }
            NustrimDiagnostics.log(
                "CLOUDSTREAM_LOADLINKS_RESULT",
                "provider=${provider.name} accepted=$accepted links=${links.size} subtitles=${subtitles.size}",
            )

            val mapped = links.map { link ->
                val typeName = link.type.name.lowercase()
                val directPlayable = link.url.isNotBlank() &&
                    typeName !in setOf("torrent", "magnet")
                val headers = buildMap<String, String> {
                    putAll(link.headers)
                    if (link.referer.isNotBlank() && keys.none { it.equals("Referer", ignoreCase = true) }) {
                        put("Referer", link.referer)
                    }
                }
                val quality = link.quality.takeIf { it > 0 }?.let { " · ${it}p" }.orEmpty()
                val mappedSubtitles = subtitles.mapNotNull { subtitle ->
                    subtitle.url.takeIf { it.isNotBlank() }?.let { url ->
                        app.nudroidlabs.nustrim.core.model.SubtitleSource(
                            url = url,
                            language = subtitle.lang,
                            label = subtitle.lang,
                        )
                    }
                }.distinctBy { "${it.url}|${it.language}" }
                StreamSource(
                    name = link.name + quality,
                    url = link.url,
                    type = when (typeName) {
                        "m3u8" -> "hls"
                        "dash" -> "dash"
                        else -> typeName
                    },
                    headers = headers,
                    playable = directPlayable,
                    note = if (directPlayable) "" else "CloudStream transport $typeName is not supported by Media3 in this build.",
                    subtitles = mappedSubtitles,
                )
            }

            if (mapped.isNotEmpty()) {
                mapped
            } else if (subtitles.isNotEmpty()) {
                listOf(
                    StreamSource(
                        name = "No video stream",
                        playable = false,
                        note = "Provider returned ${subtitles.size} subtitle file(s), but no video link."
                    )
                )
            } else {
                emptyList()
            }
        }
    }

    private suspend fun resolveLinkData(item: MediaItem): String? {
        val loadUrl = item.ref?.metaId?.takeIf { it.isNotBlank() }
            ?: itemLoadUrl[item.id]
            ?: item.id
        val response = provider.load(loadUrl) ?: return null
        mapLoad(item, response)
        return itemLinkData[item.id]
    }

    private fun mapSearch(search: SearchResponse, category: String): MediaItem {
        val stableId = search.url.ifBlank { "${provider.name}:${search.name}:${search.id ?: 0}" }
        itemLoadUrl[stableId] = search.url
        return MediaItem(
            id = stableId,
            title = search.name,
            description = if (category.isBlank()) "" else category,
            type = MediaType.from(search.type?.name),
            posterUrl = search.posterUrl.orEmpty(),
            releaseInfo = search.quality?.name.orEmpty(),
            ref = MediaRef(
                sourceKind = "cloudstream-item",
                mediaType = search.type?.name.orEmpty(),
                metaId = search.url
            )
        )
    }

    private fun mapLoad(original: MediaItem, response: LoadResponse): MediaItem {
        val episodes = when (response) {
            is TvSeriesLoadResponse -> response.episodes.mapIndexed { index, episode ->
                mapEpisode(episode, index, null)
            }
            is AnimeLoadResponse -> response.episodes.flatMap { (dubStatus, list) ->
                list.mapIndexed { index, episode -> mapEpisode(episode, index, dubStatus.name) }
            }
            else -> emptyList()
        }

        when (response) {
            is MovieLoadResponse -> itemLinkData[original.id] = response.dataUrl
            is LiveStreamLoadResponse -> itemLinkData[original.id] = response.dataUrl
            is TorrentLoadResponse -> {
                itemLinkData.remove(original.id)
                itemUnsupportedTransport[original.id] =
                    "This provider returned torrent or magnet metadata. Torrent transport is not enabled in Nustrim v2."
            }
        }

        return original.copy(
            title = response.name.ifBlank { original.title },
            description = response.plot.orEmpty(),
            type = MediaType.from(response.type.name),
            posterUrl = response.posterUrl.orEmpty(),
            backgroundUrl = response.backgroundPosterUrl.orEmpty(),
            releaseInfo = response.year?.toString().orEmpty(),
            episodes = episodes,
            ref = MediaRef(
                sourceKind = "cloudstream-loaded-item",
                mediaType = response.type.name,
                metaId = response.url,
                providerLocator = original.ref?.providerLocator.orEmpty(),
            )
        )
    }

    private fun mapEpisode(
        episode: com.lagradost.cloudstream3.Episode,
        index: Int,
        variant: String?
    ): MediaEpisode {
        val baseTitle = episode.name?.takeIf { it.isNotBlank() }
            ?: episode.episode?.let { "Episode $it" }
            ?: "Episode ${index + 1}"
        val title = variant?.takeIf { it.isNotBlank() && !it.equals("None", true) }
            ?.let { "$baseTitle · $it" }
            ?: baseTitle
        return MediaEpisode(
            id = episode.data,
            title = title,
            season = episode.season,
            episode = episode.episode,
            thumbnailUrl = episode.posterUrl.orEmpty(),
            overview = episode.description.orEmpty()
        )
    }

    private fun <T> launch(
        onSuccess: (T) -> Unit,
        onError: (Throwable) -> Unit,
        block: suspend () -> T
    ) {
        scope.launch {
            try {
                val result = block()
                main.post { onSuccess(result) }
            } catch (t: Throwable) {
                main.post { onError(t) }
            }
        }
    }

    private companion object {
        const val LATE_CALLBACK_WAIT_STEPS = 6
        const val LATE_CALLBACK_WAIT_STEP_MS = 250L
    }
}
