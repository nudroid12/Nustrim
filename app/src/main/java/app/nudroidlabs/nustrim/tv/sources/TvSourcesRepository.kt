package app.nudroidlabs.nustrim.tv.sources

import android.content.Context
import app.nudroidlabs.nustrim.core.model.MediaCatalog
import app.nudroidlabs.nustrim.core.model.MediaEpisode
import app.nudroidlabs.nustrim.core.model.MediaItem
import app.nudroidlabs.nustrim.core.model.MediaType
import app.nudroidlabs.nustrim.core.model.StreamSource
import app.nudroidlabs.nustrim.core.source.ChildSourceOpener
import app.nudroidlabs.nustrim.core.source.InstalledSourceStore
import app.nudroidlabs.nustrim.core.source.SearchableSourceSession
import app.nudroidlabs.nustrim.core.source.SourceEngine
import app.nudroidlabs.nustrim.core.source.SourceKind
import app.nudroidlabs.nustrim.core.source.SourceSession
import app.nudroidlabs.nustrim.tv.navigation.TvRoute
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import java.util.LinkedHashMap
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class TvSourcesRepository(context: Context) {
    private val appContext = context.applicationContext
    private val sourceEngine = SourceEngine(appContext)
    private val sourceStore = InstalledSourceStore(appContext)
    private val providerPerformance = TvCloudStreamPerformanceStore(appContext)
    private val cloudStreamProviderSlots = Semaphore(MAX_PARALLEL_CLOUDSTREAM_PROVIDERS)

    suspend fun load(
        route: TvRoute.Sources,
        forceRefresh: Boolean = false,
    ): TvSourcesSnapshot = loadProgressively(route, forceRefresh).last()

    fun loadProgressively(
        route: TvRoute.Sources,
        forceRefresh: Boolean = false,
    ): Flow<TvSourcesSnapshot> = channelFlow {
        if (forceRefresh) {
            synchronized(snapshotCache) { snapshotCache.remove(route.stableKey) }
            negativeProviderCache.keys
                .filter { it.startsWith("${route.stableKey}|") }
                .forEach(negativeProviderCache::remove)
        }
        if (!forceRefresh) {
            cached(route.stableKey)?.let { cachedSnapshot ->
                send(cachedSnapshot)
                return@channelFlow
            }
        }

        val sourceEntries = orderedEnabledSources(route.sourceUrl)
        if (sourceEntries.isEmpty()) {
            val emptySnapshot = TvSourcesSnapshot(
                media = route.media,
                episode = route.episode,
                attempts = emptyList(),
                streams = emptyList(),
            ).also { cache(route.stableKey, it) }
            send(emptySnapshot)
            return@channelFlow
        }

        val attempts = LinkedHashMap<String, TvSourceAttempt>()
        val streams = LinkedHashMap<String, TvSourceStream>()
        val stateLock = Mutex()

        sourceEntries.forEach { installed ->
            attempts[attemptKey(installed.displayLabel)] = TvSourceAttempt(
                sourceLabel = installed.displayLabel,
                status = TvSourceAttemptStatus.LOADING,
            )
        }

        suspend fun currentSnapshot(): TvSourcesSnapshot = stateLock.withLock {
            TvSourcesSnapshot(
                media = route.media,
                episode = route.episode,
                attempts = attempts.values.toList(),
                streams = streams.values.toList(),
            )
        }

        suspend fun publish(result: SourceResolveResult) {
            stateLock.withLock {
                result.attempts.forEach { attempt ->
                    attempts[attemptKey(attempt.sourceLabel)] = attempt
                }
                result.streams.forEach { stream ->
                    streams[stream.stableKey] = stream
                }
                send(
                    TvSourcesSnapshot(
                        media = route.media,
                        episode = route.episode,
                        attempts = attempts.values.toList(),
                        streams = streams.values.toList(),
                    ),
                )
            }
        }

        send(currentSnapshot())

        coroutineScope {
            sourceEntries.map { installed ->
                launch {
                    val result = runCatching {
                        withTimeout(SOURCE_TIMEOUT_MS) {
                            resolveInstalledSource(route, installed, forceRefresh, ::publish)
                        }
                    }.getOrElse { error ->
                        SourceResolveResult(
                            attempts = listOf(
                                TvSourceAttempt(
                                    sourceLabel = installed.displayLabel,
                                    status = TvSourceAttemptStatus.ERROR,
                                    message = error.message.orEmpty().ifBlank { error::class.java.simpleName },
                                ),
                            ),
                        )
                    }

                    if (!result.publishedIncrementally) {
                        val terminalResult = if (result.attempts.none {
                                attemptKey(it.sourceLabel) == attemptKey(installed.displayLabel)
                            }
                        ) {
                            result.copy(
                                attempts = listOf(
                                    TvSourceAttempt(
                                        sourceLabel = installed.displayLabel,
                                        status = TvSourceAttemptStatus.EMPTY,
                                    ),
                                ) + result.attempts,
                            )
                        } else {
                            result
                        }
                        publish(terminalResult)
                    } else if (result.attempts.none {
                            attemptKey(it.sourceLabel) == attemptKey(installed.displayLabel)
                        }
                    ) {
                        publish(
                            SourceResolveResult(
                                attempts = listOf(
                                    TvSourceAttempt(
                                        sourceLabel = installed.displayLabel,
                                        status = TvSourceAttemptStatus.EMPTY,
                                    ),
                                ),
                            ),
                        )
                    }
                }
            }.joinAll()
        }

        val finalSnapshot = stateLock.withLock {
            attempts.entries.toList().forEach { (key, attempt) ->
                if (attempt.status == TvSourceAttemptStatus.LOADING) {
                    attempts[key] = attempt.copy(
                        status = TvSourceAttemptStatus.ERROR,
                        message = attempt.message.ifBlank { "Provider did not complete before timeout" },
                    )
                }
            }
            TvSourcesSnapshot(
                media = route.media,
                episode = route.episode,
                attempts = attempts.values.toList(),
                streams = streams.values.toList(),
            )
        }
        cache(route.stableKey, finalSnapshot)
        send(finalSnapshot)
    }

    private fun attemptKey(label: String): String = label.trim().lowercase()

    private suspend fun resolveInstalledSource(
        route: TvRoute.Sources,
        installed: SourceEntry,
        forceRefresh: Boolean,
        onProgress: suspend (SourceResolveResult) -> Unit,
    ): SourceResolveResult {
        val session = sourceEngine.awaitOpen(installed.url, forceRefresh)
        return resolveSession(
            route = route,
            sourceUrl = installed.url,
            fallbackLabel = installed.displayLabel,
            session = session,
            depth = 0,
            onProgress = onProgress,
        )
    }

    private suspend fun resolveSession(
        route: TvRoute.Sources,
        sourceUrl: String,
        fallbackLabel: String,
        session: SourceSession,
        depth: Int,
        onProgress: suspend (SourceResolveResult) -> Unit,
    ): SourceResolveResult {
        if (depth > MAX_CHILD_DEPTH) {
            return SourceResolveResult(
                attempts = listOf(
                    TvSourceAttempt(
                        sourceLabel = session.displayName.ifBlank { fallbackLabel },
                        status = TvSourceAttemptStatus.EMPTY,
                        message = "Provider nesting limit reached",
                    ),
                ),
            )
        }

        if (session is ChildSourceOpener && session.kind == SourceKind.CLOUDSTREAM) {
            return resolveChildContainer(
                route = route,
                sourceUrl = sourceUrl,
                fallbackLabel = fallbackLabel,
                session = session,
                opener = session,
                depth = depth,
                onProgress = onProgress,
            )
        }

        if (session.kind == SourceKind.STREMIO) {
            return resolveStremioSource(
                route = route,
                sourceUrl = sourceUrl,
                fallbackLabel = fallbackLabel,
                session = session,
            )
        }

        if (session is SearchableSourceSession) {
            return resolveSearchableProvider(route, sourceUrl, session, session)
        }

        return resolveDirectSession(
            route = route,
            sourceUrl = sourceUrl,
            fallbackLabel = fallbackLabel,
            session = session,
        )
    }

    private suspend fun resolveStremioSource(
        route: TvRoute.Sources,
        sourceUrl: String,
        fallbackLabel: String,
        session: SourceSession,
    ): SourceResolveResult {
        val label = session.displayName.ifBlank { fallbackLabel }
        val resources = session.capabilities.resources
            .map { it.trim().lowercase() }
            .toSet()

        // Metadata/catalogue/subtitle-only Stremio addons are not playback sources.
        if ("stream" !in resources) {
            return SourceResolveResult()
        }

        val direct = runCatching {
            session.awaitStreams(route.media, route.episode)
        }

        direct.getOrNull()
            ?.takeIf { it.isNotEmpty() }
            ?.let { streams ->
                return SourceResolveResult(
                    attempts = listOf(
                        TvSourceAttempt(
                            sourceLabel = label,
                            status = TvSourceAttemptStatus.SUCCESS,
                            streamCount = streams.size,
                            message = "Direct stream ID",
                        ),
                    ),
                    streams = streams.map { stream ->
                        TvSourceStream(
                            sourceLabel = label,
                            sourceUrl = sourceUrl,
                            stream = stream,
                        )
                    },
                )
            }

        // Direct stream IDs are the normal Stremio path. Search is only fallback
        // for stream addons that internally use addon-native metadata IDs.
        if (session is SearchableSourceSession && session.capabilities.searchable) {
            val fallback = resolveSearchableProvider(
                route = route,
                sourceUrl = sourceUrl,
                session = session,
                searchable = session,
            )

            if (fallback.streams.isNotEmpty()) {
                return fallback.copy(
                    attempts = fallback.attempts.map { attempt ->
                        attempt.copy(
                            message = listOf(
                                "Direct stream ID returned 0",
                                attempt.message,
                            ).filter { it.isNotBlank() }.joinToString(" · "),
                        )
                    },
                )
            }

            val fallbackMessage = fallback.attempts
                .mapNotNull { it.message.takeIf(String::isNotBlank) }
                .distinct()
                .joinToString(" · ")

            return SourceResolveResult(
                attempts = listOf(
                    TvSourceAttempt(
                        sourceLabel = label,
                        status = if (direct.isFailure) {
                            TvSourceAttemptStatus.ERROR
                        } else {
                            TvSourceAttemptStatus.EMPTY
                        },
                        message = buildList {
                            direct.exceptionOrNull()?.let { error ->
                                add(
                                    "Direct stream request: " +
                                        error.message.orEmpty().ifBlank {
                                            error::class.java.simpleName
                                        }
                                )
                            } ?: add("Direct stream ID returned 0")
                            if (fallbackMessage.isNotBlank()) {
                                add("Search fallback: $fallbackMessage")
                            }
                        }.joinToString(" · "),
                    ),
                ),
            )
        }

        val directError = direct.exceptionOrNull()
        return SourceResolveResult(
            attempts = listOf(
                TvSourceAttempt(
                    sourceLabel = label,
                    status = if (directError != null) {
                        TvSourceAttemptStatus.ERROR
                    } else {
                        TvSourceAttemptStatus.EMPTY
                    },
                    message = directError?.let { error ->
                        "Direct stream request: " +
                            error.message.orEmpty().ifBlank {
                                error::class.java.simpleName
                            }
                    }.orEmpty().ifBlank { "Direct stream ID returned 0" },
                ),
            ),
        )
    }

    private suspend fun resolveDirectSession(
        route: TvRoute.Sources,
        sourceUrl: String,
        fallbackLabel: String,
        session: SourceSession,
    ): SourceResolveResult {
        val label = session.displayName.ifBlank { fallbackLabel }
        return runCatching {
            val streams = session.awaitStreams(route.media, route.episode)
            SourceResolveResult(
                attempts = listOf(
                    TvSourceAttempt(
                        sourceLabel = label,
                        status = if (streams.isEmpty()) {
                            TvSourceAttemptStatus.EMPTY
                        } else {
                            TvSourceAttemptStatus.SUCCESS
                        },
                        streamCount = streams.size,
                    ),
                ),
                streams = streams.map { stream ->
                    TvSourceStream(
                        sourceLabel = label,
                        sourceUrl = sourceUrl,
                        stream = stream,
                    )
                },
            )
        }.getOrElse { error ->
            SourceResolveResult(
                attempts = listOf(
                    TvSourceAttempt(
                        sourceLabel = label,
                        status = TvSourceAttemptStatus.ERROR,
                        message = error.message.orEmpty().ifBlank {
                            error::class.java.simpleName
                        },
                    ),
                ),
            )
        }
    }

    private suspend fun resolveChildContainer(
        route: TvRoute.Sources,
        sourceUrl: String,
        fallbackLabel: String,
        session: SourceSession,
        opener: ChildSourceOpener,
        depth: Int,
        onProgress: suspend (SourceResolveResult) -> Unit,
    ): SourceResolveResult {
        val containerLabel = session.displayName.ifBlank { fallbackLabel }
        val catalog = runCatching { session.awaitCatalog() }.getOrElse { error ->
            return SourceResolveResult(
                attempts = listOf(
                    TvSourceAttempt(
                        sourceLabel = containerLabel,
                        status = TvSourceAttemptStatus.ERROR,
                        message = error.message.orEmpty().ifBlank { error::class.java.simpleName },
                    ),
                ),
            )
        }

        val catalogItems = catalog.items
        val isLoadedPluginContainer = session.id.startsWith("cloudstream-plugin:")
        val exposesProviderCards = catalogItems.any {
            it.ref?.sourceKind == "cloudstream-loaded-provider"
        }

        if (isLoadedPluginContainer && !exposesProviderCards) {
            return runCatching {
                val directProvider = withTimeout(CHILD_OPEN_TIMEOUT_MS) {
                    opener.awaitChild(catalogItems.firstOrNull() ?: route.media)
                }
                withTimeout(PROVIDER_TIMEOUT_MS) {
                    resolveSession(
                        route = route,
                        sourceUrl = sourceUrl,
                        fallbackLabel = containerLabel,
                        session = directProvider,
                        depth = depth + 1,
                        onProgress = onProgress,
                    )
                }
            }.getOrElse { error ->
                SourceResolveResult(
                    attempts = listOf(
                        TvSourceAttempt(
                            sourceLabel = containerLabel,
                            status = TvSourceAttemptStatus.ERROR,
                            message = error.message.orEmpty().ifBlank { error::class.java.simpleName },
                        ),
                    ),
                )
            }
        }

        val children = catalogItems
            .sortedByDescending { child ->
                providerPerformance.score(sourceUrl, providerIdentity(child))
            }
            .take(MAX_CHILDREN_PER_CONTAINER)
        if (children.isEmpty()) {
            return SourceResolveResult(
                attempts = listOf(
                    TvSourceAttempt(
                        sourceLabel = containerLabel,
                        status = TvSourceAttemptStatus.EMPTY,
                    ),
                ),
            )
        }

        onProgress(
            SourceResolveResult(
                attempts = buildList {
                    add(
                        TvSourceAttempt(
                            sourceLabel = containerLabel,
                            status = TvSourceAttemptStatus.EMPTY,
                        ),
                    )
                    children.forEach { child ->
                        add(
                            TvSourceAttempt(
                                sourceLabel = child.title.ifBlank { containerLabel },
                                status = TvSourceAttemptStatus.LOADING,
                            ),
                        )
                    }
                },
                publishedIncrementally = true,
            ),
        )

        val childResults = coroutineScope {
            children.map { child ->
                async {
                    val providerId = providerIdentity(child)
                    val negativeKey = "${route.stableKey}|$sourceUrl|$providerId"
                    val cachedNegative = negativeProviderCache[negativeKey]
                        ?.takeIf { System.currentTimeMillis() - it < NEGATIVE_PROVIDER_CACHE_TTL_MS }
                    val childResult = if (cachedNegative != null) {
                        SourceResolveResult(
                            attempts = listOf(
                                TvSourceAttempt(
                                    sourceLabel = child.title.ifBlank { containerLabel },
                                    status = TvSourceAttemptStatus.EMPTY,
                                    message = "Recent no-match cache",
                                ),
                            ),
                        )
                    } else {
                        val resolveChild: suspend () -> SourceResolveResult = {
                            runCatching {
                                val childSession = withTimeout(CHILD_OPEN_TIMEOUT_MS) { opener.awaitChild(child) }
                                withTimeout(PROVIDER_TIMEOUT_MS) {
                                    resolveSession(
                                        route = route,
                                        sourceUrl = sourceUrl,
                                        fallbackLabel = child.title.ifBlank { containerLabel },
                                        session = childSession,
                                        depth = depth + 1,
                                        onProgress = onProgress,
                                    )
                                }
                            }.getOrElse { error ->
                                SourceResolveResult(
                                    attempts = listOf(
                                        TvSourceAttempt(
                                            sourceLabel = child.title.ifBlank { containerLabel },
                                            status = TvSourceAttemptStatus.ERROR,
                                            message = error.message.orEmpty().ifBlank { error::class.java.simpleName },
                                        ),
                                    ),
                                )
                            }
                        }
                        if (depth == 0) {
                            cloudStreamProviderSlots.withPermit { resolveChild() }
                        } else {
                            resolveChild()
                        }
                    }
                    if (childResult.streams.any { it.stream.playable && it.stream.url.isNotBlank() }) {
                        negativeProviderCache.remove(negativeKey)
                        providerPerformance.recordSuccess(sourceUrl, providerId)
                    } else if (childResult.attempts.any { it.status == TvSourceAttemptStatus.EMPTY }) {
                        negativeProviderCache[negativeKey] = System.currentTimeMillis()
                    }
                    childResult.also {
                        if (!childResult.publishedIncrementally) {
                            onProgress(childResult)
                        }
                    }
                }
            }.awaitAll()
        }

        return SourceResolveResult(
            attempts = childResults.flatMap { it.attempts },
            streams = childResults.flatMap { it.streams },
            publishedIncrementally = true,
        )
    }

    private suspend fun resolveSearchableProvider(
        route: TvRoute.Sources,
        sourceUrl: String,
        session: SourceSession,
        searchable: SearchableSourceSession,
    ): SourceResolveResult {
        val label = session.displayName.ifBlank { "Provider" }
        val searchCatalog = runCatching { searchable.awaitSearch(route.media.title) }.getOrElse { error ->
            return SourceResolveResult(
                attempts = listOf(
                    TvSourceAttempt(
                        sourceLabel = label,
                        status = TvSourceAttemptStatus.ERROR,
                        message = error.message.orEmpty().ifBlank { error::class.java.simpleName },
                    ),
                ),
            )
        }

        val match = chooseConservativeMatch(route.media, searchCatalog.items)
            ?: return SourceResolveResult(
                attempts = listOf(
                    TvSourceAttempt(
                        sourceLabel = label,
                        status = TvSourceAttemptStatus.EMPTY,
                        message = "No exact title match",
                    ),
                ),
            )

        val details = runCatching { session.awaitDetails(match) }.getOrElse { error ->
            return SourceResolveResult(
                attempts = listOf(
                    TvSourceAttempt(
                        sourceLabel = label,
                        status = TvSourceAttemptStatus.ERROR,
                        message = error.message.orEmpty().ifBlank { error::class.java.simpleName },
                    ),
                ),
            )
        }

        val resolvedStreams = if (route.episode == null) {
            runCatching { session.awaitStreams(details, null) }.getOrElse { emptyList() }
        } else {
            val requestedSeason = route.episode.season
            val requestedEpisode = route.episode.episode
            if (requestedSeason == null || requestedEpisode == null) {
                emptyList()
            } else {
                val matches = details.episodes.filter {
                    it.season == requestedSeason && it.episode == requestedEpisode
                }
                coroutineScope {
                    matches.map { providerEpisode ->
                        async {
                            runCatching { session.awaitStreams(details, providerEpisode) }
                                .getOrElse { emptyList() }
                        }
                    }.awaitAll().flatten()
                }
            }
        }

        return SourceResolveResult(
            attempts = listOf(
                TvSourceAttempt(
                    sourceLabel = label,
                    status = if (resolvedStreams.isEmpty()) TvSourceAttemptStatus.EMPTY else TvSourceAttemptStatus.SUCCESS,
                    streamCount = resolvedStreams.size,
                ),
            ),
            streams = resolvedStreams.map { stream ->
                TvSourceStream(
                    sourceLabel = label,
                    sourceUrl = sourceUrl,
                    stream = stream,
                )
            },
        )
    }

    private fun chooseConservativeMatch(
        requested: MediaItem,
        candidates: List<MediaItem>,
    ): MediaItem? {
        val wantedTitle = normalizeTitle(requested.title)
        if (wantedTitle.isBlank()) return null

        val exact = candidates.filter { candidate ->
            normalizeTitle(candidate.title) == wantedTitle && typeCompatible(requested.type, candidate.type)
        }
        if (exact.isEmpty()) return null
        if (exact.size == 1) return exact.first()

        val requestedYear = YEAR_REGEX.find(requested.releaseInfo)?.value
        if (requestedYear != null) {
            exact.firstOrNull { YEAR_REGEX.find(it.releaseInfo)?.value == requestedYear }?.let { return it }
        }
        return exact.first()
    }

    private fun typeCompatible(requested: MediaType, candidate: MediaType): Boolean {
        if (requested == MediaType.UNKNOWN || candidate == MediaType.UNKNOWN) return true
        val requestedSeries = requested == MediaType.SERIES || requested == MediaType.TV
        val candidateSeries = candidate == MediaType.SERIES || candidate == MediaType.TV
        if (requestedSeries || candidateSeries) return requestedSeries == candidateSeries
        return requested == candidate
    }

    private fun normalizeTitle(value: String): String = value
        .lowercase()
        .replace(NON_ALPHANUMERIC_REGEX, " ")
        .trim()
        .replace(MULTI_SPACE_REGEX, " ")

    private fun providerIdentity(item: MediaItem): String =
        item.ref?.metaId?.takeIf { it.isNotBlank() } ?: item.id.ifBlank { item.title }

    private fun orderedEnabledSources(originUrl: String): List<SourceEntry> {
        val installed = sourceStore.sources()
            .filter { it.enabled }
            .map { source -> SourceEntry(source.url, source.label.ifBlank { source.url }) }
            .toMutableList()

        if (originUrl.isNotBlank() && installed.none { it.url == originUrl }) {
            installed.add(0, SourceEntry(originUrl, "Origin"))
        }

        return installed
            .distinctBy { it.url }
            .sortedBy { if (it.url == originUrl) 0 else 1 }
    }

    private data class SourceEntry(
        val url: String,
        val displayLabel: String,
    )

    private data class SourceResolveResult(
        val attempts: List<TvSourceAttempt> = emptyList(),
        val streams: List<TvSourceStream> = emptyList(),
        val publishedIncrementally: Boolean = false,
    )

    private fun cached(key: String): TvSourcesSnapshot? = synchronized(snapshotCache) {
        val cached = snapshotCache[key] ?: return@synchronized null
        if (System.currentTimeMillis() >= cached.expiresAtMs) {
            snapshotCache.remove(key)
            null
        } else {
            cached.snapshot
        }
    }

    private fun cache(key: String, snapshot: TvSourcesSnapshot) {
        val ttlMs = if (snapshot.playableStreams.isNotEmpty()) {
            POSITIVE_CACHE_TTL_MS
        } else {
            NEGATIVE_CACHE_TTL_MS
        }
        synchronized(snapshotCache) {
            snapshotCache[key] = CachedSnapshot(System.currentTimeMillis() + ttlMs, snapshot)
        }
    }

    private data class CachedSnapshot(
        val expiresAtMs: Long,
        val snapshot: TvSourcesSnapshot,
    )

    companion object {
        private const val SOURCE_TIMEOUT_MS = 120_000L
        private const val PROVIDER_TIMEOUT_MS = 28_000L
        private const val CHILD_OPEN_TIMEOUT_MS = 18_000L
        private const val MAX_CHILD_DEPTH = 2
        private const val MAX_CHILDREN_PER_CONTAINER = 18
        private const val POSITIVE_CACHE_TTL_MS = 30L * 60L * 1_000L
        private const val NEGATIVE_CACHE_TTL_MS = 10L * 60L * 1_000L
        private const val NEGATIVE_PROVIDER_CACHE_TTL_MS = 10L * 60L * 1_000L
        private const val MAX_PARALLEL_CLOUDSTREAM_PROVIDERS = 4
        private const val MAX_CACHE_ENTRIES = 18
        private val NON_ALPHANUMERIC_REGEX = Regex("[^a-z0-9]+")
        private val MULTI_SPACE_REGEX = Regex("\\s+")
        private val YEAR_REGEX = Regex("\\b(19|20)\\d{2}\\b")
        private val negativeProviderCache = java.util.concurrent.ConcurrentHashMap<String, Long>()
        private val snapshotCache = object : LinkedHashMap<String, CachedSnapshot>(24, 0.75f, true) {
            override fun removeEldestEntry(
                eldest: MutableMap.MutableEntry<String, CachedSnapshot>?,
            ): Boolean = size > MAX_CACHE_ENTRIES
        }
    }
}

private suspend fun SourceEngine.awaitOpen(
    url: String,
    forceRefresh: Boolean,
): SourceSession = suspendCancellableCoroutine { continuation ->
    open(
        input = url,
        forceRefresh = forceRefresh,
        onSuccess = { if (continuation.isActive) continuation.resume(it) },
        onError = { if (continuation.isActive) continuation.resumeWithException(it) },
    )
}

private suspend fun SourceSession.awaitCatalog(): MediaCatalog = suspendCancellableCoroutine { continuation ->
    loadCatalog(
        onSuccess = { if (continuation.isActive) continuation.resume(it) },
        onError = { if (continuation.isActive) continuation.resumeWithException(it) },
    )
}

private suspend fun SearchableSourceSession.awaitSearch(query: String): MediaCatalog =
    suspendCancellableCoroutine { continuation ->
        search(
            query = query,
            onSuccess = { if (continuation.isActive) continuation.resume(it) },
            onError = { if (continuation.isActive) continuation.resumeWithException(it) },
        )
    }

private suspend fun SourceSession.awaitDetails(item: MediaItem): MediaItem = suspendCancellableCoroutine { continuation ->
    loadDetails(
        item = item,
        onSuccess = { if (continuation.isActive) continuation.resume(it) },
        onError = { if (continuation.isActive) continuation.resumeWithException(it) },
    )
}

private suspend fun SourceSession.awaitStreams(
    item: MediaItem,
    episode: MediaEpisode?,
): List<StreamSource> = suspendCancellableCoroutine { continuation ->
    loadStreams(
        item = item,
        episode = episode,
        onSuccess = { if (continuation.isActive) continuation.resume(it) },
        onError = { if (continuation.isActive) continuation.resumeWithException(it) },
    )
}

private suspend fun ChildSourceOpener.awaitChild(item: MediaItem): SourceSession =
    suspendCancellableCoroutine { continuation ->
        openChild(
            item = item,
            onSuccess = { if (continuation.isActive) continuation.resume(it) },
            onError = { if (continuation.isActive) continuation.resumeWithException(it) },
        )
    }
