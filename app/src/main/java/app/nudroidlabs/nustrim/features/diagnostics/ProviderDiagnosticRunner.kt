package app.nudroidlabs.nustrim.features.diagnostics

import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import app.nudroidlabs.nustrim.core.diagnostics.NustrimDiagnostics
import app.nudroidlabs.nustrim.core.model.MediaCatalog
import app.nudroidlabs.nustrim.core.model.MediaEpisode
import app.nudroidlabs.nustrim.core.model.MediaItem
import app.nudroidlabs.nustrim.core.model.StreamSource
import app.nudroidlabs.nustrim.core.source.ChildSourceOpener
import app.nudroidlabs.nustrim.core.source.SearchableSourceSession
import app.nudroidlabs.nustrim.core.source.SourceSession
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean

/** A small end-to-end provider diagnostic used from the Addons screen. */
class ProviderDiagnosticRunner {
    fun run(
        repository: SourceSession,
        provider: MediaItem,
        onUpdate: (ProviderDiagnosticReport) -> Unit
    ) {
        val opener = repository as? ChildSourceOpener
        if (opener == null) {
            onUpdate(
                ProviderDiagnosticReport(
                    providerName = provider.title,
                    status = ProviderDiagnosticStatus.FAILED,
                    primaryError = "Repository cannot open provider packages.",
                    stages = listOf(ProviderDiagnosticStage("Load", "FAILED", "Provider opener unavailable", 0L)),
                    events = listOf("Load failed: provider opener unavailable"),
                    elapsedMs = 0L
                )
            )
            return
        }

        val startedAt = SystemClock.elapsedRealtime()
        val finished = AtomicBoolean(false)
        val stages = linkedMapOf<String, ProviderDiagnosticStage>()
        val events = mutableListOf<String>()
        var primaryError = ""
        var currentStatus = ProviderDiagnosticStatus.RUNNING

        fun emit() {
            onUpdate(
                ProviderDiagnosticReport(
                    providerName = provider.title,
                    status = currentStatus,
                    primaryError = primaryError,
                    stages = stages.values.toList(),
                    events = events.toList(),
                    elapsedMs = SystemClock.elapsedRealtime() - startedAt
                )
            )
        }

        fun event(message: String) {
            val clean = sanitize(message)
            events += clean
            NustrimDiagnostics.log("PROVIDER_DIAG", "provider=${provider.title} $clean")
            emit()
        }

        fun stage(name: String, state: String, detail: String, stageStartedAt: Long) {
            stages[name] = ProviderDiagnosticStage(
                name = name,
                state = state,
                detail = sanitize(detail),
                durationMs = (SystemClock.elapsedRealtime() - stageStartedAt).coerceAtLeast(0L)
            )
            emit()
        }

        fun finish(status: ProviderDiagnosticStatus, error: String = "") {
            if (!finished.compareAndSet(false, true)) return
            currentStatus = status
            if (error.isNotBlank()) primaryError = sanitize(error)
            emit()
        }

        Handler(Looper.getMainLooper()).postDelayed(
            {
                if (!finished.get()) {
                    val message = "Diagnostic timed out after ${TIMEOUT_MS / 1000}s"
                    event(message)
                    finish(ProviderDiagnosticStatus.FAILED, message)
                }
            },
            TIMEOUT_MS
        )

        val loadStarted = SystemClock.elapsedRealtime()
        stages["Load"] = ProviderDiagnosticStage("Load", "RUNNING", "Opening provider", 0L)
        emit()

        opener.openChild(
            provider,
            onSuccess = { child ->
                stage("Load", "OK", "${child.displayName} loaded", loadStarted)
                event("Provider loaded: ${child.displayName}")
                diagnoseCatalog(child, stages, events, startedAt, ::emit, ::stage, ::event, ::finish)
            },
            onError = { throwable ->
                val message = throwable.message ?: throwable.javaClass.simpleName
                stage("Load", "FAILED", message, loadStarted)
                event("Load error: $message")
                finish(ProviderDiagnosticStatus.FAILED, message)
            }
        )
    }

    private fun diagnoseCatalog(
        session: SourceSession,
        stages: LinkedHashMap<String, ProviderDiagnosticStage>,
        events: MutableList<String>,
        startedAt: Long,
        emit: () -> Unit,
        stage: (String, String, String, Long) -> Unit,
        event: (String) -> Unit,
        finish: (ProviderDiagnosticStatus, String) -> Unit
    ) {
        val catalogStarted = SystemClock.elapsedRealtime()
        stages["Catalog"] = ProviderDiagnosticStage("Catalog", "RUNNING", "Loading sample catalog", 0L)
        emit()
        session.loadCatalog(
            onSuccess = { catalog ->
                val catalogState = if (catalog.items.isEmpty()) "EMPTY" else "OK"
                stage("Catalog", catalogState, "${catalog.items.size} item(s)", catalogStarted)
                event("Catalog returned ${catalog.items.size} item(s)")
                if (catalog.items.isEmpty()) {
                    event("Catalog is empty; trying search fallback when available")
                }
                val sample = catalog.items.firstOrNull()
                diagnoseSearch(session, sample, stages, emit, stage, event) { selected ->
                    diagnoseDetails(session, selected, stages, emit, stage, event, finish)
                }
            },
            onError = { throwable ->
                val message = throwable.message ?: throwable.javaClass.simpleName
                stage("Catalog", "FAILED", message, catalogStarted)
                event("Catalog error: $message")
                diagnoseSearch(session, null, stages, emit, stage, event) { selected ->
                    diagnoseDetails(session, selected, stages, emit, stage, event, finish)
                }
            }
        )
    }

    private fun diagnoseSearch(
        session: SourceSession,
        catalogSample: MediaItem?,
        stages: LinkedHashMap<String, ProviderDiagnosticStage>,
        emit: () -> Unit,
        stage: (String, String, String, Long) -> Unit,
        event: (String) -> Unit,
        done: (MediaItem?) -> Unit
    ) {
        val searchable = session as? SearchableSourceSession
        if (searchable == null) {
            stages["Search"] = ProviderDiagnosticStage("Search", "SKIP", "Search not exposed", 0L)
            if (catalogSample == null) {
                event("Search is not exposed and no catalog sample is available")
            }
            emit()
            done(catalogSample)
            return
        }
        val query = catalogSample?.title?.takeIf { it.isNotBlank() } ?: FALLBACK_QUERY
        val searchStarted = SystemClock.elapsedRealtime()
        stages["Search"] = ProviderDiagnosticStage("Search", "RUNNING", "Query: $query", 0L)
        emit()
        searchable.search(
            query,
            onSuccess = { results ->
                stage("Search", "OK", "${results.items.size} result(s)", searchStarted)
                event("Search '$query' returned ${results.items.size} result(s)")
                done(bestSample(catalogSample, results))
            },
            onError = { throwable ->
                val message = throwable.message ?: throwable.javaClass.simpleName
                stage("Search", "FAILED", message, searchStarted)
                event("Search error: $message")
                done(catalogSample)
            }
        )
    }

    private fun bestSample(catalogSample: MediaItem?, results: MediaCatalog): MediaItem? {
        if (catalogSample == null) return results.items.firstOrNull()
        val normalized = normalize(catalogSample.title)
        return results.items.firstOrNull { normalize(it.title) == normalized }
            ?: results.items.firstOrNull()
            ?: catalogSample
    }

    private fun diagnoseDetails(
        session: SourceSession,
        sample: MediaItem?,
        stages: LinkedHashMap<String, ProviderDiagnosticStage>,
        emit: () -> Unit,
        stage: (String, String, String, Long) -> Unit,
        event: (String) -> Unit,
        finish: (ProviderDiagnosticStatus, String) -> Unit
    ) {
        if (sample == null) {
            stages["Details"] = ProviderDiagnosticStage("Details", "SKIP", "No sample item available", 0L)
            stages["Episodes"] = ProviderDiagnosticStage("Episodes", "SKIP", "No sample item", 0L)
            stages["Sources"] = ProviderDiagnosticStage("Sources", "SKIP", "No sample item", 0L)
            event("No testable sample was obtained; playback capability was not tested")
            emit()
            finish(
                ProviderDiagnosticStatus.PARTIAL,
                "No testable sample was obtained; playback capability was not tested"
            )
            return
        }

        val detailsStarted = SystemClock.elapsedRealtime()
        stages["Details"] = ProviderDiagnosticStage("Details", "RUNNING", sample.title, 0L)
        emit()
        session.loadDetails(
            sample,
            onSuccess = { detailed ->
                stage("Details", "OK", detailed.title, detailsStarted)
                val episodes = detailed.episodes
                stages["Episodes"] = ProviderDiagnosticStage(
                    "Episodes",
                    "OK",
                    if (episodes.isEmpty()) "Movie or no episodes" else "${episodes.size} episode(s)",
                    0L
                )
                emit()
                event("Details loaded: ${detailed.title}; episodes=${episodes.size}")
                diagnoseStreams(session, detailed, stage, event, finish)
            },
            onError = { throwable ->
                val message = throwable.message ?: throwable.javaClass.simpleName
                stage("Details", "FAILED", message, detailsStarted)
                event("Details error: $message")
                finish(ProviderDiagnosticStatus.FAILED, message)
            }
        )
    }

    private fun diagnoseStreams(
        session: SourceSession,
        item: MediaItem,
        stage: (String, String, String, Long) -> Unit,
        event: (String) -> Unit,
        finish: (ProviderDiagnosticStatus, String) -> Unit
    ) {
        val episode = defaultEpisode(item.episodes)
        val sourceStarted = SystemClock.elapsedRealtime()
        event(
            if (episode == null) "Resolving movie sources"
            else "Resolving S${episode.season ?: 0}E${episode.episode ?: 0} sources"
        )
        session.loadStreams(
            item = item,
            episode = episode,
            onSuccess = { streams ->
                val playable = streams.filter { it.playable && it.url.isNotBlank() }
                val hosts = playable.mapNotNull { streamHost(it) }.distinct().take(6)
                val detail = buildString {
                    append("${playable.size}/${streams.size} playable")
                    if (hosts.isNotEmpty()) append(" · ${hosts.joinToString(", ")}")
                }
                stage("Sources", if (playable.isNotEmpty()) "OK" else "EMPTY", detail, sourceStarted)
                event("Sources: $detail")
                if (playable.isNotEmpty()) {
                    finish(ProviderDiagnosticStatus.WORKING, "")
                } else {
                    finish(ProviderDiagnosticStatus.PARTIAL, "No playable stream URL returned")
                }
            },
            onError = { throwable ->
                val message = throwable.message ?: throwable.javaClass.simpleName
                stage("Sources", "FAILED", message, sourceStarted)
                event("Source error: $message")
                finish(ProviderDiagnosticStatus.PARTIAL, message)
            }
        )
    }

    private fun defaultEpisode(episodes: List<MediaEpisode>): MediaEpisode? =
        episodes
            .sortedWith(
                compareBy<MediaEpisode> { if ((it.season ?: Int.MAX_VALUE) == 0) Int.MAX_VALUE else (it.season ?: Int.MAX_VALUE) }
                    .thenBy { it.episode ?: Int.MAX_VALUE }
            )
            .firstOrNull()

    private fun streamHost(stream: StreamSource): String? = runCatching {
        Uri.parse(stream.url).host?.takeIf { it.isNotBlank() }
    }.getOrNull()

    private fun normalize(value: String): String = value
        .lowercase(Locale.ROOT)
        .replace(Regex("[^a-z0-9]+"), " ")
        .trim()

    private fun sanitize(value: String): String = value
        .replace(Regex("(?i)(cookie|authorization|token|api[_-]?key)=([^\\s&]+)"), "$1=[redacted]")
        .replace(Regex("([?&](?:token|key|api_key|apikey|auth|signature|sig)=)[^&\\s]+", RegexOption.IGNORE_CASE), "$1[redacted]")
        .take(1000)

    companion object {
        private const val FALLBACK_QUERY = "One Piece"
        private const val TIMEOUT_MS = 65_000L
    }
}

enum class ProviderDiagnosticStatus { RUNNING, WORKING, PARTIAL, FAILED }

data class ProviderDiagnosticStage(
    val name: String,
    val state: String,
    val detail: String,
    val durationMs: Long
)

data class ProviderDiagnosticReport(
    val providerName: String,
    val status: ProviderDiagnosticStatus,
    val primaryError: String,
    val stages: List<ProviderDiagnosticStage>,
    val events: List<String>,
    val elapsedMs: Long
) {
    fun copyText(): String = buildString {
        appendLine("NUSTRIM PROVIDER DIAGNOSE")
        appendLine("=========================")
        appendLine("Provider: $providerName")
        appendLine("Status: $status")
        appendLine("Elapsed: ${elapsedMs}ms")
        if (primaryError.isNotBlank()) appendLine("Primary error: $primaryError")
        appendLine()
        appendLine("STAGES")
        stages.forEach { appendLine("${it.state.padEnd(7)} ${it.name}: ${it.detail} (${it.durationMs}ms)") }
        appendLine()
        appendLine("EVENTS")
        events.forEach { appendLine("- $it") }
    }
}
