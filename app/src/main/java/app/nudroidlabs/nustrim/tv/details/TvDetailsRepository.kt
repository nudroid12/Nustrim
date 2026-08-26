package app.nudroidlabs.nustrim.tv.details

import android.content.Context
import app.nudroidlabs.nustrim.core.diagnostics.NustrimDiagnostics
import app.nudroidlabs.nustrim.core.integrations.MdbListClient
import app.nudroidlabs.nustrim.core.integrations.MdbListRating
import app.nudroidlabs.nustrim.core.integrations.TmdbClient
import app.nudroidlabs.nustrim.core.integrations.TmdbMetadata
import app.nudroidlabs.nustrim.core.model.MediaEpisode
import app.nudroidlabs.nustrim.core.model.MediaItem
import app.nudroidlabs.nustrim.core.model.MediaRef
import app.nudroidlabs.nustrim.core.model.MediaType
import app.nudroidlabs.nustrim.core.source.InstalledSourceStore
import app.nudroidlabs.nustrim.core.source.SearchableSourceSession
import app.nudroidlabs.nustrim.core.source.SourceEngine
import app.nudroidlabs.nustrim.core.source.SourceSession
import app.nudroidlabs.nustrim.tv.episode.TvEpisodeCatalogueBuilder
import app.nudroidlabs.nustrim.tv.cloudstream.TvCloudStreamBridge
import app.nudroidlabs.nustrim.tv.navigation.TvRoute
import app.nudroidlabs.nustrim.ui.UiPreferences
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull

class TvDetailsRepository(context: Context) {
    private val appContext = context.applicationContext
    private val sourceEngine = SourceEngine(appContext)
    private val cloudStreamBridge = TvCloudStreamBridge(appContext)
    private val preferences = UiPreferences(appContext)

    suspend fun load(route: TvRoute.Details, forceRefresh: Boolean = false): TvDetailsSnapshot {
        val now = System.currentTimeMillis()
        val cached = synchronized(cache) { cache[route.contentKey] }
        if (!forceRefresh && cached != null && now - cached.createdAtMs < CACHE_TTL_MS) {
            return cached.snapshot
        }

        val providerLocated = route.media.ref?.providerLocator.orEmpty().isNotBlank()
        val detailTimeoutMs = if (providerLocated) CLOUDSTREAM_DETAIL_TIMEOUT_MS else DETAIL_TIMEOUT_MS
        val (sourceName, providerDetailed) = withTimeout(detailTimeoutMs) {
            if (providerLocated) {
                val (provider, detailed) = cloudStreamBridge.openLocated(route.media)
                provider.displayName to detailed
            } else {
                val session = openSession(route.sourceUrl)
                session.displayName to loadDetails(session, route.media)
            }
        }
        val detailed = enrichFromCatalogMetadata(providerDetailed)
        val parentIdentity = detailed.ref?.metaId?.takeIf { it.isNotBlank() } ?: detailed.id
        val providerIdentity = detailed.ref?.providerLocator
            ?.takeIf { it.isNotBlank() }
            ?.hashCode()
            ?.toString(16)
            .orEmpty()
        val parentKey = listOf(route.sourceUrl, detailed.type.name, parentIdentity, providerIdentity)
            .filter { it.isNotBlank() }
            .joinToString("|")
        val wantsIntegrations = wantsTmdb() || wantsMdbList()
        return TvDetailsSnapshot(
            sourceName = sourceName,
            item = detailed,
            episodeCatalogue = TvEpisodeCatalogueBuilder.build(
                parentKey = parentKey,
                providerEpisodes = detailed.episodes,
            ),
            integrationsLoading = wantsIntegrations,
        ).also { snapshot ->
            synchronized(cache) {
                cache[route.contentKey] = CacheEntry(System.currentTimeMillis(), snapshot)
            }
        }
    }

    suspend fun enrichIntegrations(
        snapshot: TvDetailsSnapshot,
        forceRefresh: Boolean = false,
    ): TvDetailsSnapshot {
        val wantsTmdb = wantsTmdb()
        val wantsMdbList = wantsMdbList()
        if (!wantsTmdb && !wantsMdbList) {
            return snapshot.copy(integrationsLoading = false)
        }

        val cacheKey = integrationCacheKey(snapshot)
        if (!forceRefresh) {
            synchronized(integrationCache) {
                integrationCache[cacheKey]
                    ?.takeIf { System.currentTimeMillis() - it.createdAtMs < INTEGRATION_CACHE_TTL_MS }
            }?.let { cached ->
                return snapshot.copy(
                    item = cached.item,
                    tmdbMetadata = cached.tmdbMetadata,
                    mdbListRatings = cached.mdbListRatings,
                    integrationsLoading = false,
                    integrationMessage = cached.message,
                )
            }
        }

        val errors = mutableListOf<String>()
        var tmdbMetadata: TmdbMetadata? = null
        if (wantsTmdb) {
            val result = withTimeoutOrNull(TMDB_TIMEOUT_MS) {
                TmdbClient.metadata(snapshot.item, preferences.tmdbApiKey)
            }
            when {
                result == null -> {
                    errors += "TMDB request timed out"
                    NustrimDiagnostics.log(
                        "TV_TMDB_TIMEOUT",
                        "title=${snapshot.item.title}",
                    )
                }
                result.isSuccess -> {
                    tmdbMetadata = result.getOrNull()
                    NustrimDiagnostics.log(
                        "TV_TMDB_READY",
                        "title=${snapshot.item.title} tmdbId=${tmdbMetadata?.tmdbId ?: 0}",
                    )
                }
                else -> {
                    val error = result.exceptionOrNull()
                    errors += "TMDB: ${error.toDisplayMessage()}"
                }
            }
        }

        val enrichedItem = snapshot.item.withTmdbMetadata(tmdbMetadata)
        var ratings = emptyList<MdbListRating>()
        if (wantsMdbList) {
            val result = withTimeoutOrNull(MDBLIST_TIMEOUT_MS) {
                MdbListClient.ratings(
                    item = snapshot.item,
                    apiKey = preferences.mdbListApiKey,
                    tmdb = tmdbMetadata,
                )
            }
            when {
                result == null -> {
                    errors += "MDBList request timed out"
                    NustrimDiagnostics.log(
                        "TV_MDBLIST_TIMEOUT",
                        "title=${snapshot.item.title}",
                    )
                }
                result.isSuccess -> {
                    ratings = result.getOrDefault(emptyList())
                        .filter { preferences.isDisplayedMdbRating(it.source) }
                    NustrimDiagnostics.log(
                        "TV_MDBLIST_READY",
                        "title=${snapshot.item.title} ratings=${ratings.size}",
                    )
                }
                else -> {
                    val error = result.exceptionOrNull()
                    errors += "MDBList: ${error.toDisplayMessage()}"
                }
            }
        }

        val message = errors.joinToString(" · ")
        val enriched = snapshot.copy(
            item = enrichedItem,
            tmdbMetadata = tmdbMetadata,
            mdbListRatings = ratings,
            integrationsLoading = false,
            integrationMessage = message,
        )
        synchronized(integrationCache) {
            integrationCache[cacheKey] = IntegrationCacheEntry(
                createdAtMs = System.currentTimeMillis(),
                item = enrichedItem,
                tmdbMetadata = tmdbMetadata,
                mdbListRatings = ratings,
                message = message,
            )
        }
        return enriched
    }

    private fun wantsTmdb(): Boolean =
        preferences.tmdbEnrichmentEnabled && preferences.tmdbApiKey.isNotBlank()

    private fun wantsMdbList(): Boolean =
        preferences.mdbListRatingsEnabled && preferences.mdbListApiKey.isNotBlank()

    private fun integrationCacheKey(snapshot: TvDetailsSnapshot): String = listOf(
        snapshot.item.id,
        snapshot.item.ref?.metaId.orEmpty(),
        snapshot.item.ref?.providerLocator.orEmpty(),
        wantsTmdb().toString(),
        preferences.tmdbApiKey.hashCode().toString(16),
        wantsMdbList().toString(),
        preferences.mdbListApiKey.hashCode().toString(16),
        MDBLIST_PROVIDER_IDS.filter(preferences::isMdbListProviderEnabled).joinToString(","),
    ).joinToString("|")

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

    private data class IntegrationCacheEntry(
        val createdAtMs: Long,
        val item: MediaItem,
        val tmdbMetadata: TmdbMetadata?,
        val mdbListRatings: List<MdbListRating>,
        val message: String,
    )

    private companion object {
        const val DETAIL_TIMEOUT_MS = 15_000L
        const val CLOUDSTREAM_DETAIL_TIMEOUT_MS = 45_000L
        const val CATALOG_METADATA_TIMEOUT_MS = 8_000L
        const val CACHE_TTL_MS = 120_000L
        const val TMDB_TIMEOUT_MS = 28_000L
        const val MDBLIST_TIMEOUT_MS = 20_000L
        const val INTEGRATION_CACHE_TTL_MS = 15L * 60L * 1_000L
        val IMDB_ID = Regex("tt\\d+", RegexOption.IGNORE_CASE)
        val GENERIC_EPISODE_TITLE = Regex("(?i)^episode\\s+\\d+(?:\\s*[·|-].*)?$")
        val cache = object : LinkedHashMap<String, CacheEntry>(24, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, CacheEntry>?): Boolean = size > 24
        }
        val integrationCache = object : LinkedHashMap<String, IntegrationCacheEntry>(32, 0.75f, true) {
            override fun removeEldestEntry(
                eldest: MutableMap.MutableEntry<String, IntegrationCacheEntry>?,
            ): Boolean = size > 32
        }
        val MDBLIST_PROVIDER_IDS = listOf(
            "imdb",
            "tmdb",
            "tomatoes",
            "audience",
            "metacritic",
            "metacriticuser",
            "trakt",
            "letterboxd",
            "mal",
            "rogerebert",
        )
    }
}

private fun UiPreferences.isDisplayedMdbRating(source: String): Boolean {
    val providerId = when (source.trim().lowercase()) {
        "imdb" -> "imdb"
        "tmdb" -> "tmdb"
        "tomatoes", "rottentomatoes" -> "tomatoes"
        "popcorn", "tomatoesaudience", "rottentomatoesaudience", "audience" -> "audience"
        "metacritic" -> "metacritic"
        "metacriticuser", "metacritic_user" -> "metacriticuser"
        "trakt" -> "trakt"
        "letterboxd" -> "letterboxd"
        "myanimelist", "mal" -> "mal"
        "rogerebert", "roger_ebert" -> "rogerebert"
        else -> source.trim().lowercase()
    }
    return isMdbListProviderEnabled(providerId)
}

private fun MediaItem.withTmdbMetadata(metadata: TmdbMetadata?): MediaItem {
    metadata ?: return this
    return copy(
        title = metadata.title.ifBlank { title },
        description = metadata.overview.ifBlank { description },
        posterUrl = metadata.posterUrl.ifBlank { posterUrl },
        backgroundUrl = metadata.backdropUrl.ifBlank { backgroundUrl },
        logoUrl = metadata.logoUrl.ifBlank { logoUrl },
        releaseInfo = metadata.releaseYear.ifBlank { releaseInfo },
        genres = metadata.genres.ifEmpty { genres },
        runtime = metadata.runtimeMinutes
            ?.takeIf { it > 0 }
            ?.let { "$it min" }
            ?: runtime,
        cast = metadata.cast.ifEmpty { cast },
    )
}

private fun Throwable?.toDisplayMessage(): String =
    this?.message.orEmpty().ifBlank { this?.javaClass?.simpleName ?: "request failed" }

private fun normalizeTitle(value: String): String = value
    .lowercase()
    .replace(Regex("[^\\p{L}\\p{N}]+"), " ")
    .trim()

private fun releaseYear(value: String): String =
    Regex("\\b(?:19|20)\\d{2}\\b").find(value)?.value.orEmpty()
