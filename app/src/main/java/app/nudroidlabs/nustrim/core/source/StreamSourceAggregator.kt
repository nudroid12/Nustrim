package app.nudroidlabs.nustrim.core.source

import android.content.Context
import app.nudroidlabs.nustrim.core.model.MediaEpisode
import app.nudroidlabs.nustrim.core.model.MediaItem
import app.nudroidlabs.nustrim.core.model.StreamSource

enum class StreamAggregationPhase {
    DISCOVERING,
    SCANNING
}

data class StreamAggregationProgress(
    val phase: StreamAggregationPhase,
    val current: Int,
    val total: Int,
    val sourceName: String = "",
    val foundStreams: Int = 0
)

data class StreamAggregationResult(
    val streams: List<StreamSource>,
    val enabledSourceCount: Int,
    val streamAddonCount: Int,
    val scannedStreamAddonCount: Int,
    val openFailureCount: Int,
    val loadFailureCount: Int
)

class StreamSourceAggregator(
    context: Context,
    private val engine: SourceEngine = SourceEngine(context.applicationContext),
    private val store: InstalledSourceStore = InstalledSourceStore(context.applicationContext)
) {
    fun load(
        item: MediaItem,
        episode: MediaEpisode?,
        preferredSession: SourceSession? = null,
        onProgress: (StreamAggregationProgress) -> Unit,
        onSuccess: (StreamAggregationResult) -> Unit
    ) {
        val enabledUrls = store.sources()
            .asSequence()
            .filter { it.enabled }
            .map { it.url.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .toList()

        val streamSessions = linkedMapOf<String, SourceSession>()
        var openFailures = 0

        preferredSession
            ?.takeIf(::supportsStreams)
            ?.let { session ->
                streamSessions[session.id] = session
            }

        if (enabledUrls.isEmpty()) {
            scanSessions(
                sessions = streamSessions.values.toList(),
                item = item,
                episode = episode,
                enabledSourceCount = 0,
                openFailures = 0,
                onProgress = onProgress,
                onSuccess = onSuccess
            )
            return
        }

        var completedDiscovery = 0
        var discoveryFinished = false

        fun finishDiscoveryIfReady() {
            if (discoveryFinished || completedDiscovery < enabledUrls.size) return
            discoveryFinished = true

            scanSessions(
                sessions = streamSessions.values.toList(),
                item = item,
                episode = episode,
                enabledSourceCount = enabledUrls.size,
                openFailures = openFailures,
                onProgress = onProgress,
                onSuccess = onSuccess
            )
        }

        enabledUrls.forEach { url ->
            engine.open(
                url,
                onSuccess = { session ->
                    if (supportsStreams(session)) {
                        streamSessions.putIfAbsent(session.id, session)
                    }

                    completedDiscovery += 1
                    onProgress(
                        StreamAggregationProgress(
                            phase = StreamAggregationPhase.DISCOVERING,
                            current = completedDiscovery,
                            total = enabledUrls.size,
                            sourceName = session.displayName,
                            foundStreams = 0
                        )
                    )
                    finishDiscoveryIfReady()
                },
                onError = {
                    openFailures += 1
                    completedDiscovery += 1
                    onProgress(
                        StreamAggregationProgress(
                            phase = StreamAggregationPhase.DISCOVERING,
                            current = completedDiscovery,
                            total = enabledUrls.size,
                            sourceName = "",
                            foundStreams = 0
                        )
                    )
                    finishDiscoveryIfReady()
                }
            )
        }
    }

    private fun scanSessions(
        sessions: List<SourceSession>,
        item: MediaItem,
        episode: MediaEpisode?,
        enabledSourceCount: Int,
        openFailures: Int,
        onProgress: (StreamAggregationProgress) -> Unit,
        onSuccess: (StreamAggregationResult) -> Unit
    ) {
        if (sessions.isEmpty()) {
            onSuccess(
                StreamAggregationResult(
                    streams = emptyList(),
                    enabledSourceCount = enabledSourceCount,
                    streamAddonCount = 0,
                    scannedStreamAddonCount = 0,
                    openFailureCount = openFailures,
                    loadFailureCount = 0
                )
            )
            return
        }

        val collected = mutableListOf<StreamSource>()
        var completed = 0
        var loadFailures = 0
        var finished = false

        fun completeIfReady() {
            if (finished || completed < sessions.size) return
            finished = true

            onSuccess(
                StreamAggregationResult(
                    streams = collected.toList(),
                    enabledSourceCount = enabledSourceCount,
                    streamAddonCount = sessions.size,
                    scannedStreamAddonCount = completed,
                    openFailureCount = openFailures,
                    loadFailureCount = loadFailures
                )
            )
        }

        sessions.forEach { session ->
            session.loadStreams(
                item = item,
                episode = episode,
                onSuccess = { loaded ->
                    collected += loaded.map { stream ->
                        stream.copy(
                            providerId = stream.providerId.ifBlank { session.id },
                            providerName = stream.providerName.ifBlank {
                                session.displayName
                            }
                        )
                    }

                    completed += 1
                    onProgress(
                        StreamAggregationProgress(
                            phase = StreamAggregationPhase.SCANNING,
                            current = completed,
                            total = sessions.size,
                            sourceName = session.displayName,
                            foundStreams = collected.size
                        )
                    )
                    completeIfReady()
                },
                onError = {
                    loadFailures += 1
                    completed += 1
                    onProgress(
                        StreamAggregationProgress(
                            phase = StreamAggregationPhase.SCANNING,
                            current = completed,
                            total = sessions.size,
                            sourceName = session.displayName,
                            foundStreams = collected.size
                        )
                    )
                    completeIfReady()
                }
            )
        }
    }

    private fun supportsStreams(
        session: SourceSession
    ): Boolean {
        if (session.kind == SourceKind.NUSTRIM_JSON) return true

        return session.capabilities.resources.any { resource ->
            resource.equals("stream", ignoreCase = true)
        }
    }
}
