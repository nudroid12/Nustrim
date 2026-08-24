package app.nudroidlabs.nustrim.tv.details

import android.content.Context
import app.nudroidlabs.nustrim.core.model.MediaEpisode
import app.nudroidlabs.nustrim.core.model.MediaItem
import app.nudroidlabs.nustrim.core.model.MediaRef
import app.nudroidlabs.nustrim.core.model.MediaType
import app.nudroidlabs.nustrim.core.source.InstalledSourceStore
import app.nudroidlabs.nustrim.core.source.SearchableSourceSession
import app.nudroidlabs.nustrim.core.source.SourceEngine
import app.nudroidlabs.nustrim.core.source.SourceSession
import app.nudroidlabs.nustrim.tv.episode.TvEpisodeCatalogueBuilder
import app.nudroidlabs.nustrim.tv.navigation.TvRoute
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull

class TvDetailsRepository(context: Context) {
    private val sourceEngine = SourceEngine(context.applicationContext)

    suspend fun load(route: TvRoute.Details, forceRefresh: Boolean = false): TvDetailsSnapshot {
        val now = System.currentTimeMillis()
        val cached = synchronized(cache) { cache[route.contentKey] }
        if (!forceRefresh && cached != null && now - cached.createdAtMs < CACHE_TTL_MS) {
            return cached.snapshot
        }

        val (sourceName, providerDetailed) = withTimeout(DETAIL_TIMEOUT_MS) {
            val session = openSession(route.sourceUrl)
            session.displayName to loadDetails(session, route.media)
        }
        val detailed = enrichFromCatalogMetadata(providerDetailed)
        val parentIdentity = detailed.ref?.metaId?.takeIf { it.isNotBlank() } ?: detailed.id
        val parentKey = "${route.sourceUrl}|${detailed.type.name}|$parentIdentity"
        return TvDetailsSnapshot(
            sourceName = sourceName,
            item = detailed,
            episodeCatalogue = TvEpisodeCatalogueBuilder.build(
                parentKey = parentKey,
                providerEpisodes = detailed.episodes,
            ),
        ).also { snapshot ->
            synchronized(cache) {
                cache[route.contentKey] = CacheEntry(System.currentTimeMillis(), snapshot)
            }
        }
    }

    private suspend fun openSession(sourceUrl: String): SourceSession {
        val deferred = CompletableDeferred<SourceSession>()
        sourceEngine.open(
            input = sourceUrl,
            onSuccess = { if (!deferred.isCompleted) deferred.complete(it) },
            onError = { if (!deferred.isCompleted) deferred.completeExceptionally(it) },
        )
        return deferred.await()
    }

    private suspend fun loadDetails(session: SourceSession, seed: MediaItem): MediaItem {
        val deferred = CompletableDeferred<MediaItem>()
        session.loadDetails(
            item = seed,
            onSuccess = { if (!deferred.isCompleted) deferred.complete(it) },
            onError = { if (!deferred.isCompleted) deferred.completeExceptionally(it) },
        )
        return deferred.await()
    }

    private suspend fun enrichFromCatalogMetadata(item: MediaItem): MediaItem {
        if (item.episodes.none(::needsCatalogTitle)) return item
        return withTimeoutOrNull(CATALOG_METADATA_TIMEOUT_MS) {
            val cinemeta = openSession(InstalledSourceStore.CINEMETA_URL)
            val seed = directCinemetaSeed(item) ?: findCinemetaMatch(cinemeta, item)
                ?: return@withTimeoutOrNull item
            val catalogItem = loadDetails(cinemeta, seed)
            mergeCatalogEpisodes(item, catalogItem.episodes)
        } ?: item
    }

    private suspend fun findCinemetaMatch(session: SourceSession, item: MediaItem): MediaItem? {
        val searchable = session as? SearchableSourceSession ?: return null
        val deferred = CompletableDeferred<List<MediaItem>>()
        searchable.search(
            query = item.title,
            onSuccess = { if (!deferred.isCompleted) deferred.complete(it.items) },
            onError = { if (!deferred.isCompleted) deferred.complete(emptyList()) },
        )
        val wantedTitle = normalizeTitle(item.title)
        val wantedYear = releaseYear(item.releaseInfo)
        return deferred.await()
            .filter { normalizeTitle(it.title) == wantedTitle }
            .filter { candidate ->
                item.type !in setOf(MediaType.SERIES, MediaType.TV) ||
                    candidate.type in setOf(MediaType.SERIES, MediaType.TV)
            }
            .sortedByDescending { candidate ->
                if (wantedYear.isNotBlank() && releaseYear(candidate.releaseInfo) == wantedYear) 1 else 0
            }
            .firstOrNull()
    }

    private fun directCinemetaSeed(item: MediaItem): MediaItem? {
        val imdbId = sequenceOf(item.ref?.metaId, item.id)
            .filterNotNull()
            .map { it.trim() }
            .firstOrNull { it.matches(IMDB_ID) }
            ?: return null
        return item.copy(
            id = imdbId,
            ref = MediaRef(
                sourceKind = "stremio",
                mediaType = "series",
                metaId = imdbId,
            ),
        )
    }

    private fun mergeCatalogEpisodes(
        providerItem: MediaItem,
        catalogEpisodes: List<MediaEpisode>,
    ): MediaItem {
        val catalogByCoordinate = catalogEpisodes
            .mapNotNull { episode ->
                val season = episode.season
                val number = episode.episode
                if (season == null || number == null) null else (season to number) to episode
            }
            .toMap()
        if (catalogByCoordinate.isEmpty()) return providerItem
        return providerItem.copy(
            episodes = providerItem.episodes.map { providerEpisode ->
                val season = providerEpisode.season
                val number = providerEpisode.episode
                val catalogEpisode = if (season != null && number != null) {
                    catalogByCoordinate[season to number]
                } else {
                    null
                }
                if (catalogEpisode == null) {
                    providerEpisode
                } else {
                    providerEpisode.copy(
                        title = catalogEpisode.title.ifBlank { providerEpisode.title },
                        thumbnailUrl = catalogEpisode.thumbnailUrl.ifBlank { providerEpisode.thumbnailUrl },
                        overview = catalogEpisode.overview.ifBlank { providerEpisode.overview },
                    )
                }
            },
        )
    }

    private fun needsCatalogTitle(episode: MediaEpisode): Boolean =
        episode.title.isBlank() || GENERIC_EPISODE_TITLE.matches(episode.title.trim())

    private data class CacheEntry(
        val createdAtMs: Long,
        val snapshot: TvDetailsSnapshot,
    )

    private companion object {
        const val DETAIL_TIMEOUT_MS = 15_000L
        const val CATALOG_METADATA_TIMEOUT_MS = 8_000L
        const val CACHE_TTL_MS = 120_000L
        val IMDB_ID = Regex("tt\\d+", RegexOption.IGNORE_CASE)
        val GENERIC_EPISODE_TITLE = Regex("(?i)^episode\\s+\\d+(?:\\s*[·|-].*)?$")
        val cache = object : LinkedHashMap<String, CacheEntry>(24, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, CacheEntry>?): Boolean = size > 24
        }
    }
}

private fun normalizeTitle(value: String): String = value
    .lowercase()
    .replace(Regex("[^\\p{L}\\p{N}]+"), " ")
    .trim()

private fun releaseYear(value: String): String =
    Regex("\\b(?:19|20)\\d{2}\\b").find(value)?.value.orEmpty()
