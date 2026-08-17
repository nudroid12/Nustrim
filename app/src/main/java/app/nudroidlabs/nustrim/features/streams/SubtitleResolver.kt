package app.nudroidlabs.nustrim.features.streams

import android.os.Handler
import android.os.Looper
import android.content.Context
import app.nudroidlabs.nustrim.core.diagnostics.NustrimDiagnostics
import app.nudroidlabs.nustrim.core.model.MediaEpisode
import app.nudroidlabs.nustrim.core.model.MediaItem
import app.nudroidlabs.nustrim.core.model.SubtitleSource
import app.nudroidlabs.nustrim.core.source.InstalledSourceStore
import app.nudroidlabs.nustrim.core.source.SourceEngine
import app.nudroidlabs.nustrim.core.source.SourceKind
import app.nudroidlabs.nustrim.core.source.SourceSession
import java.util.concurrent.atomic.AtomicBoolean

class SubtitleResolver(context: Context) {
    private val appContext = context.applicationContext
    private val engine = SourceEngine(appContext)
    private val store = InstalledSourceStore(appContext)

    fun resolve(
        originSession: SourceSession,
        item: MediaItem,
        episode: MediaEpisode?,
        developerMode: Boolean,
        onSuccess: (List<SubtitleSource>) -> Unit
    ) {
        val urls = store.enabledUrls(developerMode)
        val targets = urls.size + 1
        val lock = Any()
        val slots = arrayOfNulls<List<SubtitleSource>>(targets)
        var remaining = targets
        val finished = AtomicBoolean(false)

        fun finish(result: List<SubtitleSource>, reason: String) {
            if (!finished.compareAndSet(false, true)) return
            NustrimDiagnostics.log(
                "SUBTITLE_RESOLVE",
                "count=${result.size} media=${item.title} episode=${episode?.id.orEmpty()} reason=$reason"
            )
            onSuccess(result)
        }

        Handler(Looper.getMainLooper()).postDelayed({
            val partial = synchronized(lock) {
                slots.filterNotNull().flatten().distinctBy { "${it.url}|${it.language}" }
            }
            finish(partial, "timeout")
        }, RESOLVE_TIMEOUT_MS)

        fun complete(index: Int, subtitles: List<SubtitleSource>) {
            var final: List<SubtitleSource>? = null
            synchronized(lock) {
                if (slots[index] != null) return
                slots[index] = subtitles
                remaining -= 1
                if (remaining == 0) {
                    final = slots.filterNotNull().flatten().distinctBy { "${it.url}|${it.language}" }
                }
            }
            final?.let { finish(it, "complete") }
        }

        fun query(index: Int, session: SourceSession) {
            if (session.kind != SourceKind.STREMIO) {
                complete(index, emptyList())
                return
            }
            val providerName = session.displayName.ifBlank { "Subtitle addon" }
            runCatching {
                session.loadSubtitles(
                    item = item,
                    episode = episode,
                    onSuccess = { subtitles ->
                        complete(
                            index,
                            subtitles.map { subtitle ->
                                val baseLabel = subtitle.label
                                    .ifBlank { subtitle.language.ifBlank { "Subtitle" } }
                                val taggedLabel = if (baseLabel.contains(PROVIDER_SEPARATOR)) {
                                    baseLabel
                                } else {
                                    "$providerName$PROVIDER_SEPARATOR$baseLabel"
                                }
                                subtitle.copy(label = taggedLabel)
                            }
                        )
                    },
                    onError = { complete(index, emptyList()) }
                )
            }.onFailure { complete(index, emptyList()) }
        }

        query(0, originSession)
        urls.forEachIndexed { sourceIndex, url ->
            val slot = sourceIndex + 1
            engine.open(
                url,
                onSuccess = { candidate ->
                    if (candidate.kind == originSession.kind && candidate.id == originSession.id) {
                        complete(slot, emptyList())
                    } else {
                        query(slot, candidate)
                    }
                },
                onError = { complete(slot, emptyList()) }
            )
        }
    }

    companion object {
        private const val RESOLVE_TIMEOUT_MS = 3_000L
        private const val PROVIDER_SEPARATOR = "|||NUSTRIM_PROVIDER|||"
    }
}
