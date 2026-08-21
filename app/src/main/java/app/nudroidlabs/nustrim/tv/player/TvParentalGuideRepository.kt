package app.nudroidlabs.nustrim.tv.player

import android.content.Context
import app.nudroidlabs.nustrim.BuildConfig
import app.nudroidlabs.nustrim.core.model.MediaItem
import java.net.HttpURLConnection
import java.net.URL
import java.util.LinkedHashMap
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONArray
import org.json.JSONObject

internal data class TvParentalGuideItem(
    val label: String,
    val severity: String,
)

internal data class TvParentalGuide(
    val items: List<TvParentalGuideItem>,
) {
    val hasItems: Boolean
        get() = items.isNotEmpty()
}

internal class TvParentalGuideRepository(context: Context) {
    private val appContext = context.applicationContext

    suspend fun load(media: MediaItem): TvParentalGuide? {
        val imdbId = imdbId(media) ?: return null
        cached(imdbId)?.let { return it }

        val result = withTimeoutOrNull(REQUEST_TIMEOUT_MS) {
            withContext(Dispatchers.IO) {
                fetch(imdbId)
            }
        } ?: return null

        if (result.hasItems) cache(imdbId, result)
        return result.takeIf { it.hasItems }
    }

    private fun fetch(imdbId: String): TvParentalGuide {
        val connection = (URL("$BASE_URL/titles/$imdbId/parentsGuide").openConnection() as HttpURLConnection)
        return try {
            connection.requestMethod = "GET"
            connection.connectTimeout = CONNECT_TIMEOUT_MS
            connection.readTimeout = READ_TIMEOUT_MS
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("User-Agent", "Nustrim/${BuildConfig.VERSION_NAME}")
            connection.instanceFollowRedirects = true

            if (connection.responseCode !in 200..299) {
                return TvParentalGuide(emptyList())
            }
            val raw = connection.inputStream.bufferedReader().use { it.readText() }
            parse(JSONObject(raw))
        } catch (_: Throwable) {
            TvParentalGuide(emptyList())
        } finally {
            connection.disconnect()
        }
    }

    private fun parse(root: JSONObject): TvParentalGuide {
        val categories = root.optJSONArray("parentsGuide") ?: JSONArray()
        val severities = LinkedHashMap<String, String>()

        for (index in 0 until categories.length()) {
            val category = categories.optJSONObject(index) ?: continue
            val key = category.optString("category").trim().uppercase(Locale.ROOT)
            val severity = resolveSeverity(category.optJSONArray("severityBreakdowns")) ?: continue
            severities[key] = severity
        }

        val ordered = listOf(
            "VIOLENCE" to "Violence",
            "SEXUAL_CONTENT" to "Nudity",
            "PROFANITY" to "Profanity",
            "FRIGHTENING_INTENSE_SCENES" to "Frightening",
            "ALCOHOL_DRUGS" to "Alcohol/Drugs",
        ).mapNotNull { (key, label) ->
            severities[key]?.let { severity ->
                TvParentalGuideItem(label = label, severity = severity)
            }
        }

        return TvParentalGuide(ordered)
    }

    private fun resolveSeverity(breakdowns: JSONArray?): String? {
        if (breakdowns == null) return null
        var noneVotes = 0
        var dominantLevel = ""
        var dominantVotes = -1

        for (index in 0 until breakdowns.length()) {
            val item = breakdowns.optJSONObject(index) ?: continue
            val level = item.optString("severityLevel").trim().lowercase(Locale.ROOT)
            val votes = item.optInt("voteCount", 0)
            if (level == "none") {
                noneVotes = maxOf(noneVotes, votes)
            } else if (level.isNotBlank() && votes > dominantVotes) {
                dominantLevel = level
                dominantVotes = votes
            }
        }

        if (dominantLevel.isBlank() || dominantVotes <= noneVotes) return null
        return dominantLevel
            .replace('_', ' ')
            .replaceFirstChar { char ->
                if (char.isLowerCase()) char.titlecase(Locale.ROOT) else char.toString()
            }
    }

    private fun imdbId(media: MediaItem): String? {
        val candidates = listOfNotNull(
            media.ref?.metaId,
            media.id,
        )
        return candidates.firstOrNull { IMDb_ID_REGEX.matches(it.trim()) }?.trim()
    }

    private fun cached(id: String): TvParentalGuide? = synchronized(cache) {
        val item = cache[id] ?: return@synchronized null
        if (System.currentTimeMillis() - item.createdAtMs >= CACHE_TTL_MS) {
            cache.remove(id)
            null
        } else {
            item.guide
        }
    }

    private fun cache(id: String, guide: TvParentalGuide) {
        synchronized(cache) {
            cache[id] = CachedGuide(
                createdAtMs = System.currentTimeMillis(),
                guide = guide,
            )
        }
    }

    private data class CachedGuide(
        val createdAtMs: Long,
        val guide: TvParentalGuide,
    )

    private companion object {
        const val BASE_URL = "https://api.imdbapi.dev"
        const val REQUEST_TIMEOUT_MS = 6_000L
        const val CONNECT_TIMEOUT_MS = 3_500
        const val READ_TIMEOUT_MS = 4_500
        const val CACHE_TTL_MS = 60 * 60_000L
        const val MAX_CACHE_ENTRIES = 32
        val IMDb_ID_REGEX = Regex("^tt\\d{6,12}$", RegexOption.IGNORE_CASE)

        val cache = object : LinkedHashMap<String, CachedGuide>(40, 0.75f, true) {
            override fun removeEldestEntry(
                eldest: MutableMap.MutableEntry<String, CachedGuide>?,
            ): Boolean = size > MAX_CACHE_ENTRIES
        }
    }
}
