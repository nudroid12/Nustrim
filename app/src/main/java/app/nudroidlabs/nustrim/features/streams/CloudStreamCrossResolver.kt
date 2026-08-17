package app.nudroidlabs.nustrim.features.streams

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import app.nudroidlabs.nustrim.core.diagnostics.NustrimDiagnostics
import app.nudroidlabs.nustrim.core.model.MediaEpisode
import app.nudroidlabs.nustrim.core.model.MediaItem
import app.nudroidlabs.nustrim.core.model.MediaType
import app.nudroidlabs.nustrim.core.model.StreamSource
import app.nudroidlabs.nustrim.core.source.cloudstream.CloudStreamProviderStore
import app.nudroidlabs.nustrim.core.source.ChildSourceOpener
import app.nudroidlabs.nustrim.core.source.SearchableSourceSession
import app.nudroidlabs.nustrim.core.source.SourceSession
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Best-effort metadata-to-CloudStream bridge.
 *
 * CloudStream providers do not share Stremio IDs, so this layer discovers loaded
 * providers and only accepts strong title matches. Failures are isolated per
 * provider and never prevent Stremio streams from being returned.
 */
class CloudStreamCrossResolver(context: Context) {
    private val providerStore = CloudStreamProviderStore(context)
    fun resolve(
        repository: SourceSession,
        item: MediaItem,
        episode: MediaEpisode?,
        onSuccess: (List<StreamSource>) -> Unit,
        onProgress: (streams: List<StreamSource>, completedProviders: Int, totalProviders: Int, providerName: String) -> Unit = { _, _, _, _ -> },
        onProviderState: (providerName: String, loading: Boolean, hasSources: Boolean, failed: Boolean) -> Unit = { _, _, _, _ -> }
    ) {
        val opener = repository as? ChildSourceOpener
        if (opener == null) {
            onSuccess(emptyList())
            return
        }

        val finished = AtomicBoolean(false)
        val partialLock = Any()
        var latestPartial = emptyList<StreamSource>()
        fun finish(streams: List<StreamSource>, reason: String) {
            if (!finished.compareAndSet(false, true)) return
            NustrimDiagnostics.log(
                "CLOUDSTREAM_CROSS_FINISH",
                "repository=${repository.displayName} reason=$reason streams=${streams.size}"
            )
            onSuccess(streams)
        }
        Handler(Looper.getMainLooper()).postDelayed(
            {
                val snapshot = synchronized(partialLock) { latestPartial }
                finish(snapshot, "timeout")
            },
            RESOLVE_TIMEOUT_MS
        )

        NustrimDiagnostics.log(
            "CLOUDSTREAM_CROSS_START",
            "repository=${repository.displayName} title=${item.title} episode=${episode?.id.orEmpty()}"
        )
        repository.loadCatalog(
            onSuccess = { catalog ->
                val allPlugins = catalog.items
                val plugins = allPlugins
                    .filter { providerStore.isEnabled(repository.id, it) }
                    .take(MAX_PLUGINS)
                NustrimDiagnostics.log(
                    "CLOUDSTREAM_PROVIDER_FILTER",
                    "repository=${repository.displayName} total=${allPlugins.size} enabled=${plugins.size}"
                )
                aggregateStreams(
                    items = plugins,
                    label = { it.title },
                    launch = { plugin, done ->
                        opener.openChild(
                            plugin,
                            onSuccess = { loaded -> resolveLoaded(loaded, item, episode, 0, done) },
                            onError = { throwable ->
                                NustrimDiagnostics.error(
                                    "CLOUDSTREAM_PLUGIN_OPEN_ERROR",
                                    throwable,
                                    "repository=${repository.displayName} provider=${plugin.title}"
                                )
                                done(emptyList())
                            }
                        )
                    },
                    onPartial = { partial, completed, total, providerName ->
                        val merged = partial.distinctBy { streamKey(it) }
                        synchronized(partialLock) { latestPartial = merged }
                        NustrimDiagnostics.log(
                            "CLOUDSTREAM_PROVIDER_PROGRESS",
                            "repository=${repository.displayName} provider=$providerName completed=$completed/$total streams=${merged.size}"
                        )
                        onProgress(merged, completed, total, providerName)
                    },
                    onProviderStarted = { providerName ->
                        onProviderState(providerName, true, false, false)
                    },
                    onProviderDone = { providerName, result, reason ->
                        onProviderState(
                            providerName,
                            false,
                            result.any { it.playable && it.url.isNotBlank() },
                            reason != "callback"
                        )
                    }
                ) { streams ->
                    val merged = streams.distinctBy { streamKey(it) }
                    NustrimDiagnostics.log(
                        "CLOUDSTREAM_CROSS_DONE",
                        "repository=${repository.displayName} streams=${merged.size}"
                    )
                    finish(merged, "complete")
                }
            },
            onError = {
                NustrimDiagnostics.error("CLOUDSTREAM_CROSS_ERROR", it, repository.displayName)
                finish(emptyList(), "repository-error")
            }
        )
    }

    private fun resolveLoaded(
        session: SourceSession,
        item: MediaItem,
        episode: MediaEpisode?,
        depth: Int,
        done: (List<StreamSource>) -> Unit
    ) {
        val searchable = session as? SearchableSourceSession
        if (searchable != null) {
            resolveSearchable(session, searchable, item, episode, done)
            return
        }

        val opener = session as? ChildSourceOpener
        if (opener == null || depth >= MAX_CHILD_DEPTH) {
            done(emptyList())
            return
        }

        session.loadCatalog(
            onSuccess = { catalog ->
                val children = catalog.items.take(MAX_CHILD_PROVIDERS)
                aggregateStreams(
                    items = children,
                    label = { it.title },
                    launch = { childItem, childDone ->
                        opener.openChild(
                            childItem,
                            onSuccess = { child -> resolveLoaded(child, item, episode, depth + 1, childDone) },
                            onError = { childDone(emptyList()) }
                        )
                    }
                ) { results -> done(results) }
            },
            onError = { done(emptyList()) }
        )
    }

    private fun resolveSearchable(
        session: SourceSession,
        searchable: SearchableSourceSession,
        item: MediaItem,
        episode: MediaEpisode?,
        done: (List<StreamSource>) -> Unit
    ) {
        searchable.search(
            item.title,
            onSuccess = { catalog ->
                val best = catalog.items
                    .map { candidate -> candidate to matchScore(item, candidate) }
                    .maxByOrNull { it.second }
                    ?.takeIf { it.second >= MIN_MATCH_SCORE }
                    ?.first

                if (best == null) {
                    done(emptyList())
                    return@search
                }

                session.loadDetails(
                    best,
                    onSuccess = { detailed ->
                        val targetEpisode = chooseEpisode(detailed, episode)
                        if (episode != null && detailed.episodes.isNotEmpty() && targetEpisode == null) {
                            done(emptyList())
                            return@loadDetails
                        }
                        session.loadStreams(
                            item = detailed,
                            episode = targetEpisode,
                            onSuccess = { streams ->
                                if (streams.any { it.playable && it.url.isNotBlank() }) {
                                    NustrimDiagnostics.log(
                                        "CLOUDSTREAM_MATCH",
                                        "provider=${session.displayName} query=${item.title} match=${best.title} streams=${streams.size}"
                                    )
                                }
                                done(
                                    streams.map { stream ->
                                        stream.copy(
                                            providerId = stream.providerId.ifBlank { session.id },
                                            providerName = stream.providerName.ifBlank { session.displayName }
                                        )
                                    }
                                )
                            },
                            onError = { done(emptyList()) }
                        )
                    },
                    onError = { done(emptyList()) }
                )
            },
            onError = { done(emptyList()) }
        )
    }

    private fun chooseEpisode(detailed: MediaItem, wanted: MediaEpisode?): MediaEpisode? {
        if (wanted == null) return null
        if (detailed.episodes.isEmpty()) return null
        wanted.season?.let { season ->
            wanted.episode?.let { episode ->
                detailed.episodes.firstOrNull { it.season == season && it.episode == episode }?.let { return it }
            }
        }
        val wantedTitle = normalize(wanted.title)
        return detailed.episodes.firstOrNull { normalize(it.title) == wantedTitle }
            ?: wanted.episode?.let { number -> detailed.episodes.firstOrNull { it.episode == number } }
    }

    private fun matchScore(target: MediaItem, candidate: MediaItem): Int {
        val a = normalize(target.title)
        val b = normalize(candidate.title)
        if (a.isBlank() || b.isBlank()) return 0
        var score = when {
            a == b -> 100
            a.length >= 6 && b.length >= 6 && (a.contains(b) || b.contains(a)) -> 82
            else -> 0
        }
        if (target.type != MediaType.UNKNOWN && candidate.type != MediaType.UNKNOWN) {
            score += if (typeCompatible(target.type, candidate.type)) 8 else -25
        }
        val targetYear = year(target.releaseInfo)
        val candidateYear = year(candidate.releaseInfo)
        if (targetYear != null && candidateYear != null) {
            score += if (targetYear == candidateYear) 6 else -10
        }
        return score
    }

    private fun typeCompatible(a: MediaType, b: MediaType): Boolean {
        val movieA = a == MediaType.MOVIE
        val movieB = b == MediaType.MOVIE
        if (movieA || movieB) return movieA == movieB
        return true
    }

    private fun normalize(value: String): String = value
        .lowercase(Locale.ROOT)
        .replace(Regex("[^a-z0-9]+"), " ")
        .trim()
        .replace(Regex("\\s+"), " ")

    private fun year(value: String): Int? = Regex("(?:19|20)\\d{2}")
        .find(value)?.value?.toIntOrNull()

    private fun streamKey(stream: StreamSource): String = if (stream.url.isNotBlank()) {
        stream.url + "|" + stream.headers.toSortedMap(String.CASE_INSENSITIVE_ORDER)
    } else {
        "${stream.providerId}|${stream.name}|${stream.note}"
    }

    private fun <T> aggregateStreams(
        items: List<T>,
        label: (T) -> String,
        launch: (T, (List<StreamSource>) -> Unit) -> Unit,
        onPartial: (List<StreamSource>, Int, Int, String) -> Unit = { _, _, _, _ -> },
        onProviderStarted: (String) -> Unit = {},
        onProviderDone: (String, List<StreamSource>, String) -> Unit = { _, _, _ -> },
        done: (List<StreamSource>) -> Unit
    ) {
        if (items.isEmpty()) {
            done(emptyList())
            return
        }
        val lock = Any()
        val slots = arrayOfNulls<List<StreamSource>>(items.size)
        var remaining = items.size
        items.forEachIndexed { index, item ->
            val called = AtomicBoolean(false)
            val providerName = label(item)
            onProviderStarted(providerName)
            val providerStartedAt = SystemClock.elapsedRealtime()
            fun callback(result: List<StreamSource>, reason: String) {
                if (!called.compareAndSet(false, true)) return
                var final: List<StreamSource>? = null
                var partial = emptyList<StreamSource>()
                var completed = 0
                synchronized(lock) {
                    slots[index] = result
                    remaining -= 1
                    completed = items.size - remaining
                    partial = slots.filterNotNull().flatten().distinctBy { streamKey(it) }
                    if (remaining == 0) final = partial
                }
                val elapsedMs = (SystemClock.elapsedRealtime() - providerStartedAt).coerceAtLeast(0L)
                NustrimDiagnostics.log(
                    "CLOUDSTREAM_PROVIDER_DONE",
                    "provider=$providerName reason=$reason streams=${result.size} completed=$completed/${items.size} elapsedMs=$elapsedMs"
                )
                onProviderDone(providerName, result, reason)
                onPartial(partial, completed, items.size, providerName)
                final?.let(done)
            }
            Handler(Looper.getMainLooper()).postDelayed(
                { callback(emptyList(), "provider-timeout") },
                PROVIDER_TIMEOUT_MS
            )
            runCatching { launch(item) { callback(it, "callback") } }
                .onFailure { throwable ->
                    NustrimDiagnostics.error(
                        "CLOUDSTREAM_PROVIDER_LAUNCH_ERROR",
                        throwable,
                        "provider=${label(item)}"
                    )
                    callback(emptyList(), "launch-error")
                }
        }
    }

    companion object {
        private const val MAX_PLUGINS = 20
        private const val MAX_CHILD_PROVIDERS = 12
        private const val MAX_CHILD_DEPTH = 2
        private const val MIN_MATCH_SCORE = 82
        private const val RESOLVE_TIMEOUT_MS = 135_000L
        private const val PROVIDER_TIMEOUT_MS = 65_000L
    }
}
