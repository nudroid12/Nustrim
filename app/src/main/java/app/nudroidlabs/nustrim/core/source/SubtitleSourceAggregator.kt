package app.nudroidlabs.nustrim.core.source

import android.content.Context
import app.nudroidlabs.nustrim.core.model.MediaEpisode
import app.nudroidlabs.nustrim.core.model.MediaItem
import app.nudroidlabs.nustrim.core.model.SubtitleSource

enum class SubtitleAggregationPhase {
    DISCOVERING,
    SCANNING
}

data class SubtitleAggregationProgress(
    val phase: SubtitleAggregationPhase,
    val current: Int,
    val total: Int,
    val sourceName: String = "",
    val foundSubtitles: Int = 0
)

data class SubtitleAggregationResult(
    val subtitles: List<SubtitleSource>,
    val enabledSourceCount: Int,
    val subtitleAddonCount: Int,
    val scannedSubtitleAddonCount: Int,
    val openFailureCount: Int,
    val loadFailureCount: Int
)

class SubtitleSourceAggregator(
    context: Context,
    private val engine: SourceEngine = SourceEngine(context.applicationContext),
    private val store: InstalledSourceStore = InstalledSourceStore(context.applicationContext)
) {
    fun load(
        item: MediaItem,
        episode: MediaEpisode?,
        preferredSession: SourceSession? = null,
        onProgress: (SubtitleAggregationProgress) -> Unit = {},
        onSuccess: (SubtitleAggregationResult) -> Unit
    ) {
        val enabledUrls = store.sources()
            .asSequence()
            .filter { it.enabled }
            .map { it.url.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .toList()

        val subtitleSessions = linkedMapOf<String, SourceSession>()
        var openFailures = 0

        preferredSession
            ?.takeIf(::supportsSubtitles)
            ?.let { session ->
                subtitleSessions[session.id] = session
            }

        if (enabledUrls.isEmpty()) {
            scanSessions(
                sessions = subtitleSessions.values.toList(),
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
            if (
                discoveryFinished ||
                completedDiscovery < enabledUrls.size
            ) {
                return
            }
            discoveryFinished = true

            scanSessions(
                sessions = subtitleSessions.values.toList(),
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
                    if (
                        supportsSubtitles(session) &&
                        !subtitleSessions.containsKey(session.id)
                    ) {
                        subtitleSessions[session.id] = session
                    }

                    completedDiscovery += 1
                    onProgress(
                        SubtitleAggregationProgress(
                            phase = SubtitleAggregationPhase.DISCOVERING,
                            current = completedDiscovery,
                            total = enabledUrls.size,
                            sourceName = session.displayName,
                            foundSubtitles = 0
                        )
                    )
                    finishDiscoveryIfReady()
                },
                onError = {
                    openFailures += 1
                    completedDiscovery += 1
                    onProgress(
                        SubtitleAggregationProgress(
                            phase = SubtitleAggregationPhase.DISCOVERING,
                            current = completedDiscovery,
                            total = enabledUrls.size,
                            sourceName = "",
                            foundSubtitles = 0
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
        onProgress: (SubtitleAggregationProgress) -> Unit,
        onSuccess: (SubtitleAggregationResult) -> Unit
    ) {
        if (sessions.isEmpty()) {
            onSuccess(
                SubtitleAggregationResult(
                    subtitles = emptyList(),
                    enabledSourceCount = enabledSourceCount,
                    subtitleAddonCount = 0,
                    scannedSubtitleAddonCount = 0,
                    openFailureCount = openFailures,
                    loadFailureCount = 0
                )
            )
            return
        }

        val collected = mutableListOf<SubtitleSource>()
        var completed = 0
        var loadFailures = 0
        var finished = false

        fun completeIfReady() {
            if (finished || completed < sessions.size) return
            finished = true

            onSuccess(
                SubtitleAggregationResult(
                    subtitles = collected
                        .distinctBy { subtitle ->
                            "${subtitle.url}|${subtitle.language}|${subtitle.label}"
                        },
                    enabledSourceCount = enabledSourceCount,
                    subtitleAddonCount = sessions.size,
                    scannedSubtitleAddonCount = completed,
                    openFailureCount = openFailures,
                    loadFailureCount = loadFailures
                )
            )
        }

        sessions.forEach { session ->
            session.loadSubtitles(
                item = item,
                episode = episode,
                onSuccess = { loaded ->
                    collected += loaded
                    completed += 1
                    onProgress(
                        SubtitleAggregationProgress(
                            phase = SubtitleAggregationPhase.SCANNING,
                            current = completed,
                            total = sessions.size,
                            sourceName = session.displayName,
                            foundSubtitles = collected.size
                        )
                    )
                    completeIfReady()
                },
                onError = {
                    loadFailures += 1
                    completed += 1
                    onProgress(
                        SubtitleAggregationProgress(
                            phase = SubtitleAggregationPhase.SCANNING,
                            current = completed,
                            total = sessions.size,
                            sourceName = session.displayName,
                            foundSubtitles = collected.size
                        )
                    )
                    completeIfReady()
                }
            )
        }
    }

    private fun supportsSubtitles(
        session: SourceSession
    ): Boolean {
        return session.capabilities.resources.any { resource ->
            resource.equals("subtitles", ignoreCase = true)
        }
    }
}
