package app.nudroidlabs.nustrim.core.library

import android.content.Context
import app.nudroidlabs.nustrim.core.model.MediaEpisode
import app.nudroidlabs.nustrim.core.model.MediaItem
import app.nudroidlabs.nustrim.core.model.MediaRef
import app.nudroidlabs.nustrim.core.model.MediaType
import org.json.JSONArray
import org.json.JSONObject

data class LocalMediaEntry(
    val key: String,
    val sourceUrl: String,
    val mediaId: String,
    val title: String,
    val type: MediaType,
    val posterUrl: String = "",
    val backgroundUrl: String = "",
    val description: String = "",
    val releaseInfo: String = "",
    val refSourceKind: String = "",
    val refMediaType: String = "",
    val refMetaId: String = "",
    val saved: Boolean = false,
    val episodeId: String = "",
    val episodeTitle: String = "",
    val season: Int? = null,
    val episode: Int? = null,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val updatedAt: Long = 0L
) {
    val progressFraction: Float
        get() = if (durationMs > 0L) (positionMs.toDouble() / durationMs.toDouble()).coerceIn(0.0, 1.0).toFloat() else 0f

    val hasProgress: Boolean
        get() = positionMs >= MIN_PROGRESS_MS && (durationMs <= 0L || progressFraction < COMPLETE_FRACTION)

    fun toMediaItem(): MediaItem = MediaItem(
        id = mediaId,
        title = title,
        description = description,
        type = type,
        posterUrl = posterUrl,
        backgroundUrl = backgroundUrl,
        releaseInfo = releaseInfo,
        ref = if (refMetaId.isBlank()) null else MediaRef(
            sourceKind = refSourceKind,
            mediaType = refMediaType,
            metaId = refMetaId
        )
    )

    fun toEpisode(): MediaEpisode? = episodeId.takeIf { it.isNotBlank() }?.let {
        MediaEpisode(
            id = it,
            title = episodeTitle.ifBlank { "Episode" },
            season = season,
            episode = episode
        )
    }

    companion object {
        private const val MIN_PROGRESS_MS = 15_000L
        private const val COMPLETE_FRACTION = 0.95f
    }
}

class LocalMediaStore(context: Context) {
    private val preferences = context.applicationContext
        .getSharedPreferences("nustrim_local_media", Context.MODE_PRIVATE)

    fun saved(): List<LocalMediaEntry> = read()
        .filter { it.saved }
        .sortedByDescending { it.updatedAt }

    fun continueWatching(): List<LocalMediaEntry> = read()
        .filter { it.hasProgress }
        .sortedByDescending { it.updatedAt }

    fun all(): List<LocalMediaEntry> = read().sortedByDescending { it.updatedAt }

    fun isSaved(sourceUrl: String, item: MediaItem): Boolean =
        read().firstOrNull { it.key == mediaKey(sourceUrl, item) }?.saved == true

    fun isWatched(sourceUrl: String, item: MediaItem): Boolean =
        mediaKey(sourceUrl, item) in preferences.getStringSet(KEY_WATCHED, emptySet()).orEmpty()

    fun setWatched(sourceUrl: String, item: MediaItem, watched: Boolean) {
        if (sourceUrl.isBlank()) return
        val key = mediaKey(sourceUrl, item)
        val current = preferences.getStringSet(KEY_WATCHED, emptySet()).orEmpty().toMutableSet()
        if (watched) current += key else current -= key
        preferences.edit().putStringSet(KEY_WATCHED, current).apply()
    }

    fun setSaved(sourceUrl: String, item: MediaItem, saved: Boolean) {
        if (sourceUrl.isBlank()) return
        val key = mediaKey(sourceUrl, item)
        val current = read().toMutableList()
        val index = current.indexOfFirst { it.key == key }
        val now = System.currentTimeMillis()
        if (index >= 0) {
            val old = current[index]
            val updated = snapshot(sourceUrl, item, old.toEpisode()).copy(
                saved = saved,
                episodeId = old.episodeId,
                episodeTitle = old.episodeTitle,
                season = old.season,
                episode = old.episode,
                positionMs = old.positionMs,
                durationMs = old.durationMs,
                updatedAt = now
            )
            if (!updated.saved && !updated.hasProgress) current.removeAt(index) else current[index] = updated
        } else if (saved) {
            current += snapshot(sourceUrl, item, null).copy(saved = true, updatedAt = now)
        }
        save(current)
    }

    fun resumePosition(sourceUrl: String, item: MediaItem, episode: MediaEpisode?): Long {
        val key = mediaKey(sourceUrl, item)
        val stored = read().firstOrNull { it.key == key } ?: return 0L
        if (episode != null && stored.episodeId != episode.id) return 0L
        return stored.positionMs.takeIf { stored.hasProgress } ?: 0L
    }

    fun recordProgress(
        sourceUrl: String,
        item: MediaItem,
        episode: MediaEpisode?,
        positionMs: Long,
        durationMs: Long,
        completed: Boolean = false
    ) {
        if (sourceUrl.isBlank()) return
        val key = mediaKey(sourceUrl, item)
        val current = read().toMutableList()
        val index = current.indexOfFirst { it.key == key }
        val old = current.getOrNull(index)
        val safeDuration = durationMs.coerceAtLeast(0L)
        val fraction = if (safeDuration > 0L) positionMs.toDouble() / safeDuration.toDouble() else 0.0
        val shouldClear = completed || (safeDuration > 0L && fraction >= COMPLETE_FRACTION)
        val updated = snapshot(sourceUrl, item, episode).copy(
            saved = old?.saved == true,
            positionMs = if (shouldClear) 0L else positionMs.coerceAtLeast(0L),
            durationMs = if (shouldClear) 0L else safeDuration,
            updatedAt = System.currentTimeMillis()
        )
        if (index >= 0) {
            if (!updated.saved && !updated.hasProgress) current.removeAt(index) else current[index] = updated
        } else if (updated.saved || updated.hasProgress) {
            current += updated
        } else {
            // Do not rewrite the JSON store for playback that has not reached
            // the Continue Watching threshold yet.
            return
        }
        save(current)
    }

    fun remove(sourceUrl: String, item: MediaItem) {
        val key = mediaKey(sourceUrl, item)
        save(read().filterNot { it.key == key })
    }

    fun exportJson(): String = encode(read()).toString()

    fun importJson(raw: String) {
        val array = JSONArray(raw)
        save(decode(array))
    }

    private fun mediaKey(sourceUrl: String, item: MediaItem): String {
        val identity = item.ref?.metaId?.takeIf { it.isNotBlank() } ?: item.id
        val type = item.ref?.mediaType?.takeIf { it.isNotBlank() } ?: item.type.name
        return "$sourceUrl|$type|$identity"
    }

    private fun snapshot(sourceUrl: String, item: MediaItem, episode: MediaEpisode?): LocalMediaEntry = LocalMediaEntry(
        key = mediaKey(sourceUrl, item),
        sourceUrl = sourceUrl,
        mediaId = item.id,
        title = item.title,
        type = item.type,
        posterUrl = item.posterUrl,
        backgroundUrl = item.backgroundUrl,
        description = item.description,
        releaseInfo = item.releaseInfo,
        refSourceKind = item.ref?.sourceKind.orEmpty(),
        refMediaType = item.ref?.mediaType.orEmpty(),
        refMetaId = item.ref?.metaId.orEmpty(),
        episodeId = episode?.id.orEmpty(),
        episodeTitle = episode?.title.orEmpty(),
        season = episode?.season,
        episode = episode?.episode,
        updatedAt = System.currentTimeMillis()
    )

    private fun read(): List<LocalMediaEntry> {
        val raw = preferences.getString(KEY_ENTRIES, "[]").orEmpty()
        return synchronized(cacheLock) {
            if (cachedRaw != raw) {
                cachedEntries = runCatching { decode(JSONArray(raw)) }.getOrDefault(emptyList())
                cachedRaw = raw
            }
            cachedEntries
        }
    }

    private fun save(entries: List<LocalMediaEntry>) {
        val normalized = entries.distinctBy { it.key }
        val raw = encode(normalized).toString()
        synchronized(cacheLock) {
            cachedRaw = raw
            cachedEntries = normalized
        }
        preferences.edit().putString(KEY_ENTRIES, raw).apply()
    }

    private fun encode(entries: List<LocalMediaEntry>): JSONArray = JSONArray().apply {
        entries.distinctBy { it.key }.forEach { entry ->
            put(
                JSONObject()
                    .put("key", entry.key)
                    .put("sourceUrl", entry.sourceUrl)
                    .put("mediaId", entry.mediaId)
                    .put("title", entry.title)
                    .put("type", entry.type.name)
                    .put("posterUrl", entry.posterUrl)
                    .put("backgroundUrl", entry.backgroundUrl)
                    .put("description", entry.description)
                    .put("releaseInfo", entry.releaseInfo)
                    .put("refSourceKind", entry.refSourceKind)
                    .put("refMediaType", entry.refMediaType)
                    .put("refMetaId", entry.refMetaId)
                    .put("saved", entry.saved)
                    .put("episodeId", entry.episodeId)
                    .put("episodeTitle", entry.episodeTitle)
                    .put("season", entry.season ?: JSONObject.NULL)
                    .put("episode", entry.episode ?: JSONObject.NULL)
                    .put("positionMs", entry.positionMs)
                    .put("durationMs", entry.durationMs)
                    .put("updatedAt", entry.updatedAt)
            )
        }
    }

    private fun decode(array: JSONArray): List<LocalMediaEntry> = buildList {
        for (index in 0 until array.length()) {
            val obj = array.optJSONObject(index) ?: continue
            val key = obj.optString("key")
            val sourceUrl = obj.optString("sourceUrl")
            val mediaId = obj.optString("mediaId")
            if (key.isBlank() || sourceUrl.isBlank() || mediaId.isBlank()) continue
            add(
                LocalMediaEntry(
                    key = key,
                    sourceUrl = sourceUrl,
                    mediaId = mediaId,
                    title = obj.optString("title"),
                    type = runCatching { MediaType.valueOf(obj.optString("type", MediaType.UNKNOWN.name)) }
                        .getOrDefault(MediaType.UNKNOWN),
                    posterUrl = obj.optString("posterUrl"),
                    backgroundUrl = obj.optString("backgroundUrl"),
                    description = obj.optString("description"),
                    releaseInfo = obj.optString("releaseInfo"),
                    refSourceKind = obj.optString("refSourceKind"),
                    refMediaType = obj.optString("refMediaType"),
                    refMetaId = obj.optString("refMetaId"),
                    saved = obj.optBoolean("saved", false),
                    episodeId = obj.optString("episodeId"),
                    episodeTitle = obj.optString("episodeTitle"),
                    season = obj.optInt("season").takeIf { !obj.isNull("season") },
                    episode = obj.optInt("episode").takeIf { !obj.isNull("episode") },
                    positionMs = obj.optLong("positionMs", 0L),
                    durationMs = obj.optLong("durationMs", 0L),
                    updatedAt = obj.optLong("updatedAt", 0L)
                )
            )
        }
    }.distinctBy { it.key }

    companion object {
        private const val KEY_ENTRIES = "entries_v1"
        private const val KEY_WATCHED = "watched_v1"
        private const val COMPLETE_FRACTION = 0.95

        private val cacheLock = Any()
        @Volatile private var cachedRaw: String? = null
        private var cachedEntries: List<LocalMediaEntry> = emptyList()
    }
}
