package app.nudroidlabs.nustrim.tv.cloudstream

import android.content.Context
import app.nudroidlabs.nustrim.core.model.MediaCatalog
import app.nudroidlabs.nustrim.core.model.MediaItem
import app.nudroidlabs.nustrim.core.source.ChildSourceOpener
import app.nudroidlabs.nustrim.core.source.SearchableSourceSession
import app.nudroidlabs.nustrim.core.source.SourceEngine
import app.nudroidlabs.nustrim.core.source.SourceKind
import app.nudroidlabs.nustrim.core.source.SourceSession
import app.nudroidlabs.nustrim.core.source.cloudstream.CloudStreamProviderContainerSession
import app.nudroidlabs.nustrim.core.source.cloudstream.CloudStreamProviderLocator
import app.nudroidlabs.nustrim.core.source.cloudstream.CloudStreamProviderSession
import app.nudroidlabs.nustrim.core.source.cloudstream.CloudStreamProviderStore
import app.nudroidlabs.nustrim.core.source.cloudstream.CloudStreamRuntime
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

data class TvCloudStreamSearchGroup(
    val providerName: String,
    val items: List<MediaItem>,
)

class TvCloudStreamBridge(context: Context) {
    private val appContext = context.applicationContext
    private val engine = SourceEngine(appContext)
    private val runtime = CloudStreamRuntime(appContext)
    private val providerStore = CloudStreamProviderStore(appContext)
    private val slots = Semaphore(MAX_PARALLEL_PROVIDERS)

    suspend fun search(repositoryUrl: String, query: String): List<TvCloudStreamSearchGroup> =
        withTimeout(SEARCH_TIMEOUT_MS) {
            val repository = engine.awaitOpen(repositoryUrl)
            if (repository.kind != SourceKind.CLOUDSTREAM || repository !is ChildSourceOpener) {
                return@withTimeout emptyList()
            }
            val plugins = repository.awaitCatalog().items
                .filter { providerStore.isEnabled(repository.id, it) }
                .take(MAX_PLUGINS)
            coroutineScope {
                plugins.map { plugin ->
                    async {
                        slots.withPermit {
                            runCatching {
                                val container = repository.awaitChild(plugin) as? CloudStreamProviderContainerSession
                                    ?: return@runCatching emptyList()
                                container.providerNames.mapNotNull { providerName ->
                                    val provider = container.awaitProvider(providerName)
                                    val searchable = provider as? SearchableSourceSession
                                        ?: return@mapNotNull null
                                    val locator = CloudStreamProviderLocator.fromPlugin(plugin, provider.displayName)
                                        ?: return@mapNotNull null
                                    val catalog = searchable.awaitSearch(query)
                                    TvCloudStreamSearchGroup(
                                        providerName = provider.displayName,
                                        items = catalog.items.map(locator::attach),
                                    ).takeIf { it.items.isNotEmpty() }
                                }
                            }.getOrDefault(emptyList())
                        }
                    }
                }.awaitAll().flatten()
            }
        }

    suspend fun openLocated(item: MediaItem): Pair<CloudStreamProviderSession, MediaItem> {
        val locator = CloudStreamProviderLocator.decode(item.ref?.providerLocator.orEmpty())
            ?: error("CloudStream provider route is missing")
        val container = runtime.awaitOpen(locator)
        val provider = container.awaitProvider(locator.providerName)
        val detailed = provider.awaitDetails(item)
        return provider to detailed
    }

    private companion object {
        const val MAX_PARALLEL_PROVIDERS = 4
        const val MAX_PLUGINS = 18
        const val SEARCH_TIMEOUT_MS = 90_000L
    }
}

private suspend fun SourceEngine.awaitOpen(url: String): SourceSession =
    suspendCancellableCoroutine { continuation ->
        open(
            input = url,
            onSuccess = { if (continuation.isActive) continuation.resume(it) },
            onError = { if (continuation.isActive) continuation.resumeWithException(it) },
        )
    }

private suspend fun SourceSession.awaitCatalog(): MediaCatalog =
    suspendCancellableCoroutine { continuation ->
        loadCatalog(
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

private suspend fun CloudStreamProviderContainerSession.awaitProvider(
    providerName: String,
): CloudStreamProviderSession = suspendCancellableCoroutine { continuation ->
    openProvider(
        providerName = providerName,
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

private suspend fun CloudStreamRuntime.awaitOpen(
    locator: CloudStreamProviderLocator,
): CloudStreamProviderContainerSession = suspendCancellableCoroutine { continuation ->
    open(
        pluginUrl = locator.pluginUrl,
        internalName = locator.internalName,
        expectedHash = locator.expectedHash,
        onSuccess = { if (continuation.isActive) continuation.resume(it) },
        onError = { if (continuation.isActive) continuation.resumeWithException(it) },
    )
}

private suspend fun CloudStreamProviderSession.awaitDetails(item: MediaItem): MediaItem =
    suspendCancellableCoroutine { continuation ->
        loadDetails(
            item = item,
            onSuccess = { if (continuation.isActive) continuation.resume(it) },
            onError = { if (continuation.isActive) continuation.resumeWithException(it) },
        )
    }
