package app.nudroidlabs.nustrim.tv.search

import android.content.Context
import app.nudroidlabs.nustrim.core.model.MediaCatalog
import app.nudroidlabs.nustrim.core.source.CatalogSectionSourceSession
import app.nudroidlabs.nustrim.core.source.InstalledSourceStore
import app.nudroidlabs.nustrim.core.source.SearchableSourceSession
import app.nudroidlabs.nustrim.core.source.SourceEngine
import app.nudroidlabs.nustrim.core.source.SourceKind
import app.nudroidlabs.nustrim.core.source.SourceSession
import app.nudroidlabs.nustrim.tv.cloudstream.TvCloudStreamBridge
import app.nudroidlabs.nustrim.ui.UiPreferences
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withTimeoutOrNull

class TvSearchRepository(context: Context) {
    private val appContext = context.applicationContext
    private val sourceEngine = SourceEngine(appContext)
    private val sourceStore = InstalledSourceStore(appContext)
    private val uiPreferences = UiPreferences(appContext)
    private val cloudStreamBridge = TvCloudStreamBridge(appContext)

    suspend fun search(query: String): TvSearchSnapshot = coroutineScope {
        val clean = query.trim()
        require(clean.length >= MIN_SEARCH_QUERY_LENGTH) { "Search query is too short" }
        val enabledUrls = sourceStore.enabledUrls(uiPreferences.developerMode)
        if (enabledUrls.isEmpty()) return@coroutineScope emptySnapshot()

        val results = enabledUrls.mapIndexed { index, url ->
            async { index to searchSource(url, clean) }
        }.awaitAll().sortedBy { it.first }.map { it.second }

        TvSearchSnapshot(
            rows = results.flatMap(SourceResult::rows),
            failedSources = results.count { it.failed },
            searchableSources = results.count { it.searchable },
            totalSources = enabledUrls.size,
        )
    }

    suspend fun discover(forceRefresh: Boolean = false): TvSearchSnapshot = coroutineScope {
        val enabledUrls = sourceStore.enabledUrls(uiPreferences.developerMode)
        val signature = enabledUrls.joinToString("|")
        val now = System.currentTimeMillis()
        val cached = cachedDiscover
        if (
            !forceRefresh &&
            cached != null &&
            signature == cachedDiscoverSignature &&
            now - cachedDiscoverAtMs < DISCOVER_CACHE_TTL_MS
        ) {
            return@coroutineScope cached
        }
        if (enabledUrls.isEmpty()) return@coroutineScope emptySnapshot()

        val results = enabledUrls.mapIndexed { index, url ->
            async { index to discoverSource(url) }
        }.awaitAll().sortedBy { it.first }.map { it.second }

        TvSearchSnapshot(
            rows = results.flatMap(SourceResult::rows),
            failedSources = results.count { it.failed },
            searchableSources = results.count { it.searchable },
            totalSources = enabledUrls.size,
        ).also { snapshot ->
            if (snapshot.rows.isNotEmpty()) {
                cachedDiscover = snapshot
                cachedDiscoverSignature = signature
                cachedDiscoverAtMs = System.currentTimeMillis()
            }
        }
    }

    private suspend fun searchSource(url: String, query: String): SourceResult {
        return withTimeoutOrNull(SOURCE_TIMEOUT_MS) {
            runCatching {
                val session = openSession(url)
                if (session.kind == SourceKind.CLOUDSTREAM) {
                    val groups = cloudStreamBridge.search(url, query)
                    return@runCatching SourceResult(
                        rows = groups.mapIndexedNotNull { index, group ->
                            MediaCatalog(
                                name = group.providerName,
                                items = group.items,
                                sourceLabel = group.providerName,
                            ).toRow(url, group.providerName, index, MAX_SEARCH_RESULTS)
                        },
                        failed = false,
                        searchable = true,
                    )
                }
                val searchable = session as? SearchableSourceSession
                    ?: return@runCatching SourceResult(emptyList(), failed = false, searchable = false)
                val catalog = search(searchable, query)
                SourceResult(
                    rows = listOfNotNull(catalog.toRow(url, session.displayName, 0, MAX_SEARCH_RESULTS)),
                    failed = false,
                    searchable = true,
                )
            }.getOrElse {
                SourceResult(emptyList(), failed = true, searchable = true)
            }
        } ?: SourceResult(emptyList(), failed = true, searchable = true)
    }

    private suspend fun discoverSource(url: String): SourceResult {
        return withTimeoutOrNull(SOURCE_TIMEOUT_MS) {
            runCatching {
                val session = openSession(url)
                if (session.kind == SourceKind.CLOUDSTREAM) {
                    return@runCatching SourceResult(emptyList(), failed = false, searchable = true)
                }
                val catalogs = loadCatalogs(session)
                SourceResult(
                    rows = catalogs.take(MAX_DISCOVER_ROWS_PER_SOURCE).mapIndexedNotNull { index, catalog ->
                        catalog.toRow(url, session.displayName, index, MAX_DISCOVER_ITEMS_PER_ROW)
                    },
                    failed = false,
                    searchable = session is SearchableSourceSession,
                )
            }.getOrElse {
                SourceResult(emptyList(), failed = true, searchable = false)
            }
        } ?: SourceResult(emptyList(), failed = true, searchable = false)
    }

    private fun MediaCatalog.toRow(
        sourceUrl: String,
        sourceName: String,
        index: Int,
        limit: Int,
    ): TvSearchRow? {
        val media = items.asSequence()
            .filter { it.title.isNotBlank() }
            .distinctBy { it.ref?.metaId?.takeIf { id -> id.isNotBlank() } ?: it.id }
            .take(limit)
            .map { item ->
                TvSearchMedia(
                    sourceUrl = sourceUrl,
                    sourceName = sourceName,
                    item = item,
                )
            }
            .toList()
        if (media.isEmpty()) return null
        val rowTitle = name.ifBlank { sourceName }
        return TvSearchRow(
            key = "$sourceUrl|$rowTitle|$index",
            title = rowTitle,
            sourceName = sourceName,
            items = media,
        )
    }

    private suspend fun openSession(url: String): SourceSession {
        val deferred = CompletableDeferred<SourceSession>()
        sourceEngine.open(
            input = url,
            onSuccess = { if (!deferred.isCompleted) deferred.complete(it) },
            onError = { if (!deferred.isCompleted) deferred.completeExceptionally(it) },
        )
        return deferred.await()
    }

    private suspend fun search(
        session: SearchableSourceSession,
        query: String,
    ): MediaCatalog {
        val deferred = CompletableDeferred<MediaCatalog>()
        session.search(
            query = query,
            onSuccess = { if (!deferred.isCompleted) deferred.complete(it) },
            onError = { if (!deferred.isCompleted) deferred.completeExceptionally(it) },
        )
        return deferred.await()
    }

    private suspend fun loadCatalogs(session: SourceSession): List<MediaCatalog> {
        val deferred = CompletableDeferred<List<MediaCatalog>>()
        val success: (List<MediaCatalog>) -> Unit = {
            if (!deferred.isCompleted) deferred.complete(it)
        }
        val failure: (Throwable) -> Unit = {
            if (!deferred.isCompleted) deferred.completeExceptionally(it)
        }
        if (session is CatalogSectionSourceSession) {
            session.loadCatalogSections(success, failure)
        } else {
            session.loadCatalog(
                onSuccess = { success(listOf(it)) },
                onError = failure,
            )
        }
        return deferred.await()
    }

    private fun emptySnapshot() = TvSearchSnapshot(
        rows = emptyList(),
        failedSources = 0,
        searchableSources = 0,
        totalSources = 0,
    )

    private data class SourceResult(
        val rows: List<TvSearchRow>,
        val failed: Boolean,
        val searchable: Boolean,
    )

    private companion object {
        const val SOURCE_TIMEOUT_MS = 45_000L
        const val DISCOVER_CACHE_TTL_MS = 120_000L
        const val MAX_SEARCH_RESULTS = 32
        const val MAX_DISCOVER_ROWS_PER_SOURCE = 6
        const val MAX_DISCOVER_ITEMS_PER_ROW = 24

        @Volatile
        var cachedDiscover: TvSearchSnapshot? = null

        @Volatile
        var cachedDiscoverSignature: String = ""

        @Volatile
        var cachedDiscoverAtMs: Long = 0L
    }
}
