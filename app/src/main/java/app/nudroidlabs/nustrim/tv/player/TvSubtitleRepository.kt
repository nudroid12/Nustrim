package app.nudroidlabs.nustrim.tv.player

import android.content.Context
import app.nudroidlabs.nustrim.core.model.MediaEpisode
import app.nudroidlabs.nustrim.core.model.MediaItem
import app.nudroidlabs.nustrim.core.model.SubtitleSource
import app.nudroidlabs.nustrim.core.source.InstalledSource
import app.nudroidlabs.nustrim.core.source.InstalledSourceStore
import app.nudroidlabs.nustrim.core.source.SourceEngine
import app.nudroidlabs.nustrim.core.source.SourceSession
import java.util.LinkedHashMap
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

internal class TvSubtitleRepository(context: Context) {
    private val appContext = context.applicationContext
    private val sourceEngine = SourceEngine(appContext)
    private val sourceStore = InstalledSourceStore(appContext)

    suspend fun enrich(request: TvPlaybackRequest): TvPlaybackRequest {
        val fetched = externalSubtitles(
            media = request.media,
            episode = request.episode,
        )
        val merged = mergeSubtitles(
            streamSubtitles = request.stream.subtitles,
            externalSubtitles = fetched,
        )
        if (merged == request.stream.subtitles) return request
        return request.copy(
            stream = request.stream.copy(subtitles = merged),
        )
    }

    private suspend fun externalSubtitles(
        media: MediaItem,
        episode: MediaEpisode?,
    ): List<SubtitleSource> {
        val cacheKey = buildString {
            append(media.ref?.metaId ?: media.id)
            append('/')
            append(episode?.id ?: "movie")
        }
        cached(cacheKey)?.let { return it }

        val installed = sourceStore.sources()
            .filter { it.enabled }
            .sortedBy { source ->
                if (source.url == InstalledSourceStore.OPENSUBTITLES_URL) 0 else 1
            }

        val loaded = withTimeoutOrNull(OVERALL_TIMEOUT_MS) {
            supervisorScope {
                installed.map { source ->
                    async {
                        loadFromSource(
                            installed = source,
                            media = media,
                            episode = episode,
                        )
                    }
                }.awaitAll().flatten()
            }
        }.orEmpty()

        return loaded
            .filter { it.url.isNotBlank() }
            .distinctBy { subtitle ->
                listOf(
                    subtitle.url.trim(),
                    subtitle.language.trim().lowercase(),
                    subtitle.label.trim().lowercase(),
                ).joinToString("|")
            }
            .also { cache(cacheKey, it) }
    }

    private suspend fun loadFromSource(
        installed: InstalledSource,
        media: MediaItem,
        episode: MediaEpisode?,
    ): List<SubtitleSource> {
        return withTimeoutOrNull(PER_SOURCE_TIMEOUT_MS) {
            val session = sourceEngine.awaitOpen(installed.url)
            val resources = session.capabilities.resources
                .map { it.trim().lowercase() }
                .toSet()
            if ("subtitles" !in resources && "subtitle" !in resources) {
                return@withTimeoutOrNull emptyList()
            }

            val provider = session.displayName
                .ifBlank { installed.label }
                .ifBlank { "Subtitle source" }

            session.awaitSubtitles(media, episode).map { subtitle ->
                subtitle.copy(
                    label = providerAwareLabel(
                        provider = provider,
                        subtitle = subtitle,
                    ),
                )
            }
        }.orEmpty()
    }

    private fun mergeSubtitles(
        streamSubtitles: List<SubtitleSource>,
        externalSubtitles: List<SubtitleSource>,
    ): List<SubtitleSource> {
        val combined = streamSubtitles + externalSubtitles
        val seen = LinkedHashMap<String, SubtitleSource>()
        combined.forEach { subtitle ->
            if (subtitle.url.isBlank()) return@forEach
            val key = listOf(
                subtitle.url.trim(),
                subtitle.language.trim().lowercase(),
            ).joinToString("|")
            if (seen[key] == null) {
                seen[key] = subtitle
            }
        }
        return seen.values.toList()
    }

    private fun providerAwareLabel(
        provider: String,
        subtitle: SubtitleSource,
    ): String {
        val base = subtitle.label
            .ifBlank { subtitle.language }
            .ifBlank { "Subtitle" }
            .trim()
        return if (base.contains(PROVIDER_SEPARATOR)) {
            base
        } else {
            "$provider$PROVIDER_SEPARATOR$base"
        }
    }

    private fun cached(key: String): List<SubtitleSource>? = synchronized(cache) {
        val item = cache[key] ?: return@synchronized null
        if (System.currentTimeMillis() - item.createdAtMs >= CACHE_TTL_MS) {
            cache.remove(key)
            null
        } else {
            item.subtitles
        }
    }

    private fun cache(key: String, subtitles: List<SubtitleSource>) {
        synchronized(cache) {
            cache[key] = CachedSubtitles(
                createdAtMs = System.currentTimeMillis(),
                subtitles = subtitles,
            )
        }
    }

    private data class CachedSubtitles(
        val createdAtMs: Long,
        val subtitles: List<SubtitleSource>,
    )

    private companion object {
        const val PROVIDER_SEPARATOR = "|||NUSTRIM_PROVIDER|||"
        const val OVERALL_TIMEOUT_MS = 6_500L
        const val PER_SOURCE_TIMEOUT_MS = 5_000L
        const val CACHE_TTL_MS = 10 * 60_000L
        const val MAX_CACHE_ENTRIES = 24

        val cache = object : LinkedHashMap<String, CachedSubtitles>(32, 0.75f, true) {
            override fun removeEldestEntry(
                eldest: MutableMap.MutableEntry<String, CachedSubtitles>?,
            ): Boolean = size > MAX_CACHE_ENTRIES
        }
    }
}

private suspend fun SourceEngine.awaitOpen(url: String): SourceSession =
    suspendCancellableCoroutine { continuation ->
        open(
            input = url,
            onSuccess = { session ->
                if (continuation.isActive) continuation.resume(session)
            },
            onError = { error ->
                if (continuation.isActive) continuation.resumeWithException(error)
            },
        )
    }

private suspend fun SourceSession.awaitSubtitles(
    media: MediaItem,
    episode: MediaEpisode?,
): List<SubtitleSource> = suspendCancellableCoroutine { continuation ->
    loadSubtitles(
        item = media,
        episode = episode,
        onSuccess = { subtitles ->
            if (continuation.isActive) continuation.resume(subtitles)
        },
        onError = {
            if (continuation.isActive) continuation.resume(emptyList())
        },
    )
}
