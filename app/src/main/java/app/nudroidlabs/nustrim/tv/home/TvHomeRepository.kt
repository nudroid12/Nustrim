package app.nudroidlabs.nustrim.tv.home

import android.content.Context
import app.nudroidlabs.nustrim.core.model.MediaCatalog
import app.nudroidlabs.nustrim.core.source.CatalogSectionSourceSession
import app.nudroidlabs.nustrim.core.source.InstalledSource
import app.nudroidlabs.nustrim.core.source.InstalledSourceStore
import app.nudroidlabs.nustrim.core.source.SourceEngine
import app.nudroidlabs.nustrim.core.source.SourceSession
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withTimeoutOrNull

class TvHomeRepository(context: Context) {
    private val appContext = context.applicationContext
    private val sourceEngine = SourceEngine(appContext)
    private val sourceStore = InstalledSourceStore(appContext)

    suspend fun load(forceRefresh: Boolean = false): TvHomeSnapshot = coroutineScope {
        val enabled = sourceStore.sources().filter { it.enabled }
        val sourceSignature = enabled.joinToString("|") { it.url }
        val now = System.currentTimeMillis()
        val cached = cachedSnapshot
        if (
            !forceRefresh &&
            cached != null &&
            cachedSourceSignature == sourceSignature &&
            now - cachedAtMs < SNAPSHOT_CACHE_TTL_MS
        ) {
            return@coroutineScope cached
        }
        if (enabled.isEmpty()) {
            return@coroutineScope TvHomeSnapshot(
                rows = emptyList(),
                failedSources = 0,
                totalSources = 0,
            )
        }

        val results = enabled.mapIndexed { index, installed ->
            async {
                index to loadSource(installed)
            }
        }.awaitAll().sortedBy { it.first }

        val rows = results.flatMap { it.second.rows }
            .filter { it.items.isNotEmpty() }
            .distinctBy { it.key }

        TvHomeSnapshot(
            rows = rows,
            failedSources = results.count { it.second.failed },
            totalSources = enabled.size,
        ).also { snapshot ->
            if (snapshot.rows.isNotEmpty()) {
                cachedSnapshot = snapshot
                cachedSourceSignature = sourceSignature
                cachedAtMs = System.currentTimeMillis()
            }
        }
    }

    private suspend fun loadSource(installed: InstalledSource): SourceLoadResult {
        val result = withTimeoutOrNull(SOURCE_TIMEOUT_MS) {
            runCatching {
                val session = openSession(installed.url)
                val catalogs = loadCatalogs(session)
                SourceLoadResult(
                    rows = catalogs.mapIndexedNotNull { index, catalog ->
                        val media = catalog.items
                            .asSequence()
                            .filter { it.title.isNotBlank() }
                            .distinctBy { item ->
                                item.ref?.metaId?.takeIf { it.isNotBlank() } ?: item.id
                            }
                            .take(MAX_ITEMS_PER_ROW)
                            .map { item ->
                                TvHomeMedia(
                                    sourceUrl = installed.url,
                                    sourceName = session.displayName,
                                    item = item,
                                )
                            }
                            .toList()

                        if (media.isEmpty()) null else TvHomeRow(
                            key = buildString {
                                append(installed.url)
                                append('|')
                                append(catalog.name.ifBlank { session.displayName })
                                append('|')
                                append(index)
                            },
                            title = catalog.name.ifBlank { session.displayName },
                            sourceName = session.displayName,
                            items = media,
                        )
                    },
                    failed = false,
                )
            }.getOrElse {
                SourceLoadResult(emptyList(), failed = true)
            }
        }
        return result ?: SourceLoadResult(emptyList(), failed = true)
    }

    private suspend fun openSession(url: String): SourceSession {
        val deferred = CompletableDeferred<SourceSession>()
        sourceEngine.open(
            input = url,
            onSuccess = { session -> if (!deferred.isCompleted) deferred.complete(session) },
            onError = { error -> if (!deferred.isCompleted) deferred.completeExceptionally(error) },
        )
        return deferred.await()
    }

    private suspend fun loadCatalogs(session: SourceSession): List<MediaCatalog> {
        val deferred = CompletableDeferred<List<MediaCatalog>>()
        val success: (List<MediaCatalog>) -> Unit = { catalogs ->
            if (!deferred.isCompleted) deferred.complete(catalogs)
        }
        val failure: (Throwable) -> Unit = { error ->
            if (!deferred.isCompleted) deferred.completeExceptionally(error)
        }

        if (session is CatalogSectionSourceSession) {
            session.loadCatalogSections(success, failure)
        } else {
            session.loadCatalog(
                onSuccess = { catalog -> success(listOf(catalog)) },
                onError = failure,
            )
        }
        return deferred.await()
    }

    private data class SourceLoadResult(
        val rows: List<TvHomeRow>,
        val failed: Boolean,
    )

    private companion object {
        const val SOURCE_TIMEOUT_MS = 7_500L
        const val SNAPSHOT_CACHE_TTL_MS = 120_000L
        const val MAX_ITEMS_PER_ROW = 32

        @Volatile
        var cachedSnapshot: TvHomeSnapshot? = null

        @Volatile
        var cachedSourceSignature: String = ""

        @Volatile
        var cachedAtMs: Long = 0L
    }
}
