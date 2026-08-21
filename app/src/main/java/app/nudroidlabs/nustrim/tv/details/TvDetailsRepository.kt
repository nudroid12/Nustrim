package app.nudroidlabs.nustrim.tv.details

import android.content.Context
import app.nudroidlabs.nustrim.core.model.MediaItem
import app.nudroidlabs.nustrim.core.source.SourceEngine
import app.nudroidlabs.nustrim.core.source.SourceSession
import app.nudroidlabs.nustrim.tv.episode.TvEpisodeCatalogueBuilder
import app.nudroidlabs.nustrim.tv.navigation.TvRoute
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeout

class TvDetailsRepository(context: Context) {
    private val sourceEngine = SourceEngine(context.applicationContext)

    suspend fun load(route: TvRoute.Details, forceRefresh: Boolean = false): TvDetailsSnapshot {
        val now = System.currentTimeMillis()
        val cached = synchronized(cache) { cache[route.contentKey] }
        if (!forceRefresh && cached != null && now - cached.createdAtMs < CACHE_TTL_MS) {
            return cached.snapshot
        }

        return withTimeout(DETAIL_TIMEOUT_MS) {
            val session = openSession(route.sourceUrl)
            val detailed = loadDetails(session, route.media)
            val parentIdentity = detailed.ref?.metaId?.takeIf { it.isNotBlank() } ?: detailed.id
            val parentKey = "${route.sourceUrl}|${detailed.type.name}|$parentIdentity"
            TvDetailsSnapshot(
                sourceName = session.displayName,
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

    private data class CacheEntry(
        val createdAtMs: Long,
        val snapshot: TvDetailsSnapshot,
    )

    private companion object {
        const val DETAIL_TIMEOUT_MS = 15_000L
        const val CACHE_TTL_MS = 120_000L
        val cache = object : LinkedHashMap<String, CacheEntry>(24, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, CacheEntry>?): Boolean = size > 24
        }
    }
}
