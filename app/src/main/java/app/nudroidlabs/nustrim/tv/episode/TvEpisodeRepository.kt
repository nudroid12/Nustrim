package app.nudroidlabs.nustrim.tv.episode

import android.content.Context
import app.nudroidlabs.nustrim.core.model.MediaItem
import app.nudroidlabs.nustrim.core.model.MediaType
import app.nudroidlabs.nustrim.core.source.SourceEngine
import app.nudroidlabs.nustrim.core.source.SourceSession
import app.nudroidlabs.nustrim.tv.navigation.TvRoute
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeout

class TvEpisodeRepository(context: Context) {
    private val sourceEngine = SourceEngine(context.applicationContext)

    suspend fun load(route: TvRoute.Details): TvEpisodeSnapshot = withTimeout(DETAIL_TIMEOUT_MS) {
        val session = openSession(route.sourceUrl)
        val seed = MediaItem(
            id = route.mediaId,
            title = route.mediaId,
            type = MediaType.from(route.contentType),
        )
        val detailed = loadDetails(session, seed)
        val parentIdentity = detailed.ref?.metaId
            ?.takeIf { it.isNotBlank() }
            ?: detailed.id
        val parentKey = "${route.sourceUrl}|${detailed.type.name}|$parentIdentity"

        TvEpisodeSnapshot(
            sourceName = session.displayName,
            item = detailed,
            catalogue = TvEpisodeCatalogueBuilder.build(
                parentKey = parentKey,
                providerEpisodes = detailed.episodes,
            ),
        )
    }

    private suspend fun openSession(sourceUrl: String): SourceSession {
        val deferred = CompletableDeferred<SourceSession>()
        sourceEngine.open(
            input = sourceUrl,
            onSuccess = { session -> if (!deferred.isCompleted) deferred.complete(session) },
            onError = { error -> if (!deferred.isCompleted) deferred.completeExceptionally(error) },
        )
        return deferred.await()
    }

    private suspend fun loadDetails(
        session: SourceSession,
        seed: MediaItem,
    ): MediaItem {
        val deferred = CompletableDeferred<MediaItem>()
        session.loadDetails(
            item = seed,
            onSuccess = { item -> if (!deferred.isCompleted) deferred.complete(item) },
            onError = { error -> if (!deferred.isCompleted) deferred.completeExceptionally(error) },
        )
        return deferred.await()
    }

    private companion object {
        const val DETAIL_TIMEOUT_MS = 15_000L
    }
}
