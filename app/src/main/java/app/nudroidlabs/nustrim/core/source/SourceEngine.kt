package app.nudroidlabs.nustrim.core.source

import android.content.Context
import app.nudroidlabs.nustrim.core.diagnostics.NustrimDiagnostics
import app.nudroidlabs.nustrim.core.source.cloudstream.CloudStreamRepositorySession
import app.nudroidlabs.nustrim.core.source.cloudstream.CloudStreamRuntime
import app.nudroidlabs.nustrim.core.source.stremio.StremioManifest
import app.nudroidlabs.nustrim.core.source.stremio.StremioSession
import org.json.JSONObject

class SourceEngine(
    context: Context,
    private val http: HttpJsonClient = HttpJsonClient()
) {
    private val appContext = context.applicationContext
    private val cloudStreamRuntime = CloudStreamRuntime(appContext)
    private val healthStore = SourceHealthStore(appContext)

    fun open(
        input: String,
        onSuccess: (SourceSession) -> Unit,
        onError: (Throwable) -> Unit
    ) {
        val normalized = normalizeInput(input)
        cachedSession(normalized)?.let { session ->
            NustrimDiagnostics.log(
                "SOURCE_CACHE_HIT",
                "name=${session.displayName} id=${session.id} kind=${session.kind} input=$normalized"
            )
            healthStore.recordSuccess(normalized)
            onSuccess(session)
            return
        }

        val candidates = if (looksLikeManifestUrl(normalized)) {
            listOf(normalized)
        } else {
            listOf(normalized, normalized.trimEnd('/') + "/manifest.json")
        }
        NustrimDiagnostics.log("SOURCE_OPEN", "input=$normalized candidates=${candidates.size}")
        tryCandidates(candidates, 0, normalized, onSuccess, onError)
    }
    private fun tryCandidates(
        candidates: List<String>,
        index: Int,
        healthKey: String,
        onSuccess: (SourceSession) -> Unit,
        onError: (Throwable) -> Unit
    ) {
        if (index >= candidates.size) {
            val error = IllegalArgumentException(
                "Unsupported source. Expected a Stremio manifest, CloudStream repo.json, or Nustrim JSON repository."
            )
            NustrimDiagnostics.error("SOURCE_OPEN_ERROR", error)
            healthStore.recordFailure(healthKey, error)
            onError(error)
            return
        }
        val url = candidates[index]
        http.getText(
            url = url,
            onSuccess = { text ->
                try {
                    val session = detect(text, url)
                    NustrimDiagnostics.log(
                        "SOURCE_READY",
                        "name=${session.displayName} id=${session.id} kind=${session.kind} url=$url"
                    )
                    healthStore.recordSuccess(healthKey)
                    cacheSession(healthKey, session)
                    if (url != healthKey) cacheSession(url, session)
                    onSuccess(session)
                } catch (t: Throwable) {
                    if (index + 1 < candidates.size) {
                        tryCandidates(candidates, index + 1, healthKey, onSuccess, onError)
                    } else {
                        NustrimDiagnostics.error("SOURCE_DETECT_ERROR", t, url)
                        healthStore.recordFailure(healthKey, t)
                        onError(t)
                    }
                }
            },
            onError = {
                if (index + 1 < candidates.size) {
                    tryCandidates(candidates, index + 1, healthKey, onSuccess, onError)
                } else {
                    NustrimDiagnostics.error("SOURCE_HTTP_ERROR", it, url)
                    healthStore.recordFailure(healthKey, it)
                    onError(it)
                }
            }
        )
    }

    private fun detect(text: String, sourceUrl: String): SourceSession {
        val root = JSONObject(text)

        if (root.has("manifestVersion") && root.has("pluginLists")) {
            return CloudStreamRepositorySession.fromJson(
                root = root,
                sourceUrl = sourceUrl,
                http = http,
                runtime = cloudStreamRuntime
            )
        }

        if (root.has("items")) {
            return NustrimJsonSession.fromJson(text, sourceUrl)
        }

        if (root.has("id") && root.has("version") && root.has("name") && root.has("resources")) {
            val manifest = StremioManifest.parse(root)
            return StremioSession(
                manifestUrl = ensureManifestUrl(sourceUrl),
                manifest = manifest,
                http = http
            )
        }

        throw UnsupportedOperationException("JSON format not recognised")
    }

    private fun normalizeInput(input: String): String {
        val trimmed = input.trim()
        require(trimmed.isNotBlank()) { "Enter a source URL" }
        return when {
            trimmed.startsWith("stremio://", ignoreCase = true) ->
                "https://" + trimmed.substringAfter("stremio://")
            else -> trimmed
        }
    }

    private fun ensureManifestUrl(url: String): String =
        if (looksLikeManifestUrl(url)) url else url.trimEnd('/') + "/manifest.json"

    private fun looksLikeManifestUrl(url: String): Boolean =
        url.substringBefore('?').endsWith("/manifest.json", ignoreCase = true)

    private fun cachedSession(key: String): SourceSession? = synchronized(sessionCache) {
        val cached = sessionCache[key] ?: return@synchronized null
        val ageMs = System.currentTimeMillis() - cached.createdAtMs
        if (ageMs >= SESSION_CACHE_TTL_MS) {
            sessionCache.remove(key)
            null
        } else {
            cached.session
        }
    }

    private fun cacheSession(key: String, session: SourceSession) {
        synchronized(sessionCache) {
            sessionCache[key] = CachedSession(System.currentTimeMillis(), session)
        }
    }

    private data class CachedSession(
        val createdAtMs: Long,
        val session: SourceSession
    )

    private companion object {
        const val SESSION_CACHE_TTL_MS = 120_000L
        const val MAX_SESSION_CACHE = 24

        val sessionCache = object : LinkedHashMap<String, CachedSession>(32, 0.75f, true) {
            override fun removeEldestEntry(
                eldest: MutableMap.MutableEntry<String, CachedSession>?
            ): Boolean = size > MAX_SESSION_CACHE
        }
    }
}
