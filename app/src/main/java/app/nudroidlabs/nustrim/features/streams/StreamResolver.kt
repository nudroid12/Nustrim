package app.nudroidlabs.nustrim.features.streams

import android.content.Context
import app.nudroidlabs.nustrim.core.diagnostics.NustrimDiagnostics
import app.nudroidlabs.nustrim.core.model.MediaEpisode
import app.nudroidlabs.nustrim.core.model.MediaItem
import app.nudroidlabs.nustrim.core.model.StreamSource
import app.nudroidlabs.nustrim.core.source.InstalledSourceStore
import app.nudroidlabs.nustrim.core.source.SourceEngine
import app.nudroidlabs.nustrim.core.source.SourceKind
import app.nudroidlabs.nustrim.core.source.SourceSession

data class StreamProviderProgress(
    val id: String,
    val name: String,
    val loading: Boolean,
    val hasSources: Boolean,
    val failed: Boolean = false
)

class StreamResolver(context: Context) {
    private val appContext = context.applicationContext
    private val engine = SourceEngine(appContext)
    private val store = InstalledSourceStore(appContext)
    private val cloudStreamCrossResolver = CloudStreamCrossResolver(appContext)

    fun resolve(
        originSession: SourceSession,
        item: MediaItem,
        episode: MediaEpisode?,
        developerMode: Boolean,
        onSuccess: (List<StreamSource>) -> Unit,
        onError: (Throwable) -> Unit,
        onProgress: (streams: List<StreamSource>, completedTargets: Int, totalTargets: Int, lastError: Throwable?) -> Unit = { _, _, _, _ -> },
        onProviderProgress: (List<StreamProviderProgress>) -> Unit = {}
    ) {
        val installed = store.enabledUrls(developerMode)
        NustrimDiagnostics.log(
            "STREAM_RESOLVE_START",
            "origin=${originSession.displayName} originId=${originSession.id} " +
                "type=${item.type} mediaId=${item.id} metaId=${item.ref?.metaId.orEmpty()} " +
                "episode=${episode?.id.orEmpty()} enabled=${installed.size}"
        )

        val totalTargets = installed.size + 1
        val lock = Any()
        val providerLock = Any()
        val providerStates = linkedMapOf<String, StreamProviderProgress>()
        val slots = arrayOfNulls<List<StreamSource>>(totalTargets)
        val partialSlots = arrayOfNulls<List<StreamSource>>(totalTargets)
        var remaining = totalTargets
        var firstError: Throwable? = null

        fun updateProvider(
            id: String,
            name: String,
            loading: Boolean,
            hasSources: Boolean,
            failed: Boolean = false
        ) {
            if (name.isBlank()) return
            val snapshot = synchronized(providerLock) {
                providerStates[id] = StreamProviderProgress(id, name, loading, hasSources, failed)
                providerStates.values.toList()
            }
            onProviderProgress(snapshot)
        }

        fun mergedProgressLocked(): List<StreamSource> = buildList {
            slots.forEachIndexed { index, completed ->
                addAll(completed ?: partialSlots[index].orEmpty())
            }
        }.distinctBy(::streamKey)

        fun publishPartial(index: Int, session: SourceSession, streams: List<StreamSource>) {
            var progressResult: List<StreamSource>
            var completedTargets: Int
            synchronized(lock) {
                if (slots[index] != null) return
                partialSlots[index] = decorate(session, streams)
                progressResult = mergedProgressLocked()
                completedTargets = totalTargets - remaining
            }
            onProgress(progressResult, completedTargets, totalTargets, null)
        }

        fun complete(
            index: Int,
            session: SourceSession?,
            streams: List<StreamSource>,
            error: Throwable? = null
        ) {
            var finalResult: List<StreamSource>? = null
            var progressResult: List<StreamSource> = emptyList()
            var completedTargets = 0
            var terminalError: Throwable? = null

            synchronized(lock) {
                if (slots[index] != null) return
                if (error != null && firstError == null) firstError = error
                slots[index] = if (session == null) emptyList() else decorate(session, streams)
                partialSlots[index] = null
                remaining -= 1
                completedTargets = totalTargets - remaining
                progressResult = mergedProgressLocked()

                val provider = session?.displayName ?: "unopened"
                if (error == null) {
                    NustrimDiagnostics.log(
                        "STREAM_TARGET_DONE",
                        "provider=$provider kind=${session?.kind} streams=${streams.size}"
                    )
                } else {
                    NustrimDiagnostics.error("STREAM_TARGET_ERROR", error, "provider=$provider")
                }

                if (remaining == 0) {
                    val merged = slots
                        .filterNotNull()
                        .flatten()
                        .distinctBy(::streamKey)

                    if (merged.isEmpty() && firstError != null) {
                        terminalError = firstError
                    } else {
                        finalResult = merged
                    }
                }
            }

            if (
                session != null &&
                !(index != 0 && session.kind == originSession.kind && session.id == originSession.id)
            ) {
                val providerHasSources = if (session.kind == SourceKind.CLOUDSTREAM) {
                    false
                } else {
                    streams.any { it.playable && it.url.isNotBlank() }
                }
                updateProvider(
                    id = "session:${session.id}",
                    name = session.displayName,
                    loading = false,
                    hasSources = providerHasSources,
                    failed = error != null
                )
            }
            onProgress(progressResult, completedTargets, totalTargets, error)

            finalResult?.let { result ->
                NustrimDiagnostics.log(
                    "STREAM_RESOLVE_DONE",
                    "streams=${result.size} playable=${result.count { it.playable && it.url.isNotBlank() }} " +
                        "providers=${result.map { it.providerName }.filter { it.isNotBlank() }.distinct().joinToString(",")}"
                )
                result.forEachIndexed { streamIndex, stream ->
                    NustrimDiagnostics.log(
                        "STREAM",
                        "#${streamIndex + 1} provider=${stream.providerName} name=${stream.name} " +
                            "playable=${stream.playable} url=${stream.url} " +
                            "headers=${NustrimDiagnostics.headers(stream.headers)} note=${stream.note}"
                    )
                }
                onSuccess(result)
            }
            terminalError?.let { error ->
                NustrimDiagnostics.error("STREAM_RESOLVE_FAILED", error)
                onError(error)
            }
        }

        updateProvider(
            id = "session:${originSession.id}",
            name = originSession.displayName,
            loading = true,
            hasSources = false
        )
        try {
            NustrimDiagnostics.log(
                "STREAM_TARGET_START",
                "provider=${originSession.displayName} kind=${originSession.kind} origin=true"
            )
            originSession.loadStreams(
                item = item,
                episode = episode,
                onSuccess = { complete(0, originSession, it) },
                onError = { complete(0, originSession, emptyList(), it) }
            )
        } catch (throwable: Throwable) {
            complete(0, originSession, emptyList(), throwable)
        }

        val crossAddonItem = if (item.streams.isEmpty()) item else item.copy(streams = emptyList())
        installed.forEachIndexed { sourceIndex, sourceUrl ->
            val slot = sourceIndex + 1
            NustrimDiagnostics.log("STREAM_ADDON_OPEN", sourceUrl)
            engine.open(
                sourceUrl,
                onSuccess = { candidate ->
                    when {
                        candidate.kind == originSession.kind && candidate.id == originSession.id -> {
                            NustrimDiagnostics.log(
                                "STREAM_TARGET_SKIP",
                                "provider=${candidate.displayName} reason=origin-already-queried"
                            )
                            complete(slot, candidate, emptyList())
                        }
                        candidate.kind == SourceKind.STREMIO -> {
                            updateProvider(
                                id = "session:${candidate.id}",
                                name = candidate.displayName,
                                loading = true,
                                hasSources = false
                            )
                            try {
                                NustrimDiagnostics.log(
                                    "STREAM_TARGET_START",
                                    "provider=${candidate.displayName} kind=${candidate.kind} origin=false"
                                )
                                candidate.loadStreams(
                                    item = crossAddonItem,
                                    episode = episode,
                                    onSuccess = { complete(slot, candidate, it) },
                                    onError = { complete(slot, candidate, emptyList(), it) }
                                )
                            } catch (throwable: Throwable) {
                                complete(slot, candidate, emptyList(), throwable)
                            }
                        }
                        candidate.kind == SourceKind.CLOUDSTREAM -> {
                            cloudStreamCrossResolver.resolve(
                                repository = candidate,
                                item = crossAddonItem,
                                episode = episode,
                                onSuccess = { complete(slot, candidate, it) },
                                onProgress = { partial, completedProviders, totalProviders, providerName ->
                                    NustrimDiagnostics.log(
                                        "STREAM_REPO_PROGRESS",
                                        "repository=${candidate.displayName} provider=$providerName completed=$completedProviders/$totalProviders streams=${partial.size}"
                                    )
                                    publishPartial(slot, candidate, partial)
                                },
                                onProviderState = { providerName, loading, hasSources, failed ->
                                    updateProvider(
                                        id = "cloud:${candidate.id}:$providerName",
                                        name = providerName,
                                        loading = loading,
                                        hasSources = hasSources,
                                        failed = failed
                                    )
                                }
                            )
                        }
                        else -> {
                            NustrimDiagnostics.log(
                                "STREAM_TARGET_SKIP",
                                "provider=${candidate.displayName} kind=${candidate.kind} reason=unsupported-cross-resolver"
                            )
                            complete(slot, candidate, emptyList())
                        }
                    }
                },
                onError = { complete(slot, null, emptyList(), it) }
            )
        }
    }

    private fun decorate(
        session: SourceSession,
        streams: List<StreamSource>
    ): List<StreamSource> =
        streams.map { stream ->
            stream.copy(
                providerId = stream.providerId.ifBlank { session.id },
                providerName = stream.providerName.ifBlank { session.displayName }
            )
        }

    private fun streamKey(stream: StreamSource): String {
        if (stream.url.isBlank()) {
            return listOf(
                "unsupported",
                stream.providerId,
                stream.name,
                stream.note
            ).joinToString("|")
        }
        val headers = stream.headers
            .toSortedMap(String.CASE_INSENSITIVE_ORDER)
            .entries
            .joinToString("&") { "${it.key}=${it.value}" }
        return "${stream.url}|$headers"
    }
}
