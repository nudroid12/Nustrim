package app.nudroidlabs.nustrim.core.source.cloudstream

import android.content.Context
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest

/**
 * Small disk cache for CloudStream repository and plugin-list JSON.
 *
 * Repository metadata changes much less frequently than provider pages. Keeping
 * it in app-private cache lets Sources open immediately on repeat visits while
 * Refresh can still force a network request through SourceEngine.
 */
class CloudStreamRepositoryCache(context: Context) {
    private val directory = File(context.applicationContext.cacheDir, "cloudstream_repository_json")

    fun readFresh(url: String, maxAgeMs: Long = FRESH_TTL_MS): String? =
        read(url, maxAgeMs)

    fun readStale(url: String): String? =
        read(url, STALE_FALLBACK_TTL_MS)

    fun write(url: String, text: String) {
        if (url.isBlank() || text.isBlank()) return
        synchronized(lock) {
            runCatching {
                if (!directory.exists()) require(directory.mkdirs()) {
                    "Unable to create CloudStream repository cache"
                }
                val target = cacheFile(url)
                val temporary = File(directory, target.name + ".tmp")
                temporary.writeText(
                    JSONObject()
                        .put("url", url)
                        .put("storedAtMs", System.currentTimeMillis())
                        .put("payload", text)
                        .toString(),
                )
                if (target.exists()) target.delete()
                require(temporary.renameTo(target)) {
                    "Unable to commit CloudStream repository cache"
                }
            }
        }
    }

    private fun read(url: String, maxAgeMs: Long): String? = synchronized(lock) {
        runCatching {
            val root = JSONObject(cacheFile(url).readText())
            if (root.optString("url") != url) return@runCatching null
            val ageMs = System.currentTimeMillis() - root.optLong("storedAtMs", 0L)
            if (ageMs !in 0 until maxAgeMs) return@runCatching null
            root.optString("payload").takeIf { it.isNotBlank() }
        }.getOrNull()
    }

    private fun cacheFile(url: String): File {
        val key = MessageDigest.getInstance("SHA-256")
            .digest(url.toByteArray())
            .joinToString("") { "%02x".format(it) }
        return File(directory, "$key.json")
    }

    companion object {
        const val FRESH_TTL_MS = 24L * 60L * 60L * 1_000L
        private const val STALE_FALLBACK_TTL_MS = 7L * 24L * 60L * 60L * 1_000L
        private val lock = Any()
    }
}
