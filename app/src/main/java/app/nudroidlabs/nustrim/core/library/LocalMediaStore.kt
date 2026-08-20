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
    val nextUp: Boolean = false,
    val updatedAt: Long = 0L
) {
    val progressFraction: Float
        get() = if (durationMs > 0L) (positionMs.toDouble() / durationMs.toDouble()).coerceIn(0.0, 1.0).toFloat() else 0f

    val hasProgress: Boolean
        get() = positionMs >= MIN_PROGRESS_MS && (durationMs <= 0L || progressFraction < COMPLETE_FRACTION)

    val hasContinueState: Boolean
        get() = hasProgress || nextUp

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
        .filter { it.hasContinueState }
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

    fun watchedEpisodeKeys(sourceUrl: String, item: MediaItem): Set<String> {
        if (sourceUrl.isBlank()) return emptySet()
        val prefix = "${mediaKey(sourceUrl, item)}|episode|"
        return preferences
            .getStringSet(KEY_EPISODE_WATCHED, emptySet())
            .orEmpty()
            .asSequence()
            .filter { it.startsWith(prefix) }
            .map { it.removePrefix(prefix) }
            .toSet()
    }

    fun isEpisodeWatched(
        sourceUrl: String,
        item: MediaItem,
        episode: MediaEpisode
    ): Boolean {
        if (sourceUrl.isBlank()) return false
        val current = preferences.getStringSet(KEY_EPISODE_WATCHED, emptySet()).orEmpty()
        val exact = episodeWatchedStorageKey(sourceUrl, item, episode)
        if (exact in current) return true
        val prefix = episodeWatchedStorageIdPrefix(sourceUrl, item, episode)
        return prefix.isNotBlank() && current.any { it.startsWith(prefix) }
    }

    fun setEpisodeWatched(
        sourceUrl: String,
        item: MediaItem,
        episode: MediaEpisode,
        watched: Boolean
    ) {
        if (sourceUrl.isBlank()) return
        val key = episodeWatchedStorageKey(sourceUrl, item, episode)
        val prefix = episodeWatchedStorageIdPrefix(sourceUrl, item, episode)
        val current = preferences
            .getStringSet(KEY_EPISODE_WATCHED, emptySet())
            .orEmpty()
            .toMutableSet()
        if (prefix.isNotBlank()) current.removeAll { it.startsWith(prefix) }
        if (watched) current += key
        preferences.edit().putStringSet(KEY_EPISODE_WATCHED, current).apply()
    }

    fun setSeasonWatched(
        sourceUrl: String,
        item: MediaItem,
        episodes: List<MediaEpisode>,
        watched: Boolean
    ) {
        if (sourceUrl.isBlank()) return
        val current = preferences
            .getStringSet(KEY_EPISODE_WATCHED, emptySet())
            .orEmpty()
            .toMutableSet()
        episodes.forEach { episode ->
            val key = episodeWatchedStorageKey(sourceUrl, item, episode)
            val prefix = episodeWatchedStorageIdPrefix(sourceUrl, item, episode)
            if (prefix.isNotBlank()) current.removeAll { it.startsWith(prefix) }
            if (watched) current += key
        }
        preferences.edit().putStringSet(KEY_EPISODE_WATCHED, current).apply()
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
                nextUp = old.nextUp,
                updatedAt = now
            )
            if (!updated.saved && !updated.hasContinueState) {
                current.removeAt(index)
            } else {
                current[index] = updated
            }
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
        completed: Boolean = false,
        nextEpisode: MediaEpisode? = null
    ) {
        if (sourceUrl.isBlank()) return

        val key = mediaKey(sourceUrl, item)
        val current = read().toMutableList()
        val index = current.indexOfFirst { it.key == key }
        val old = current.getOrNull(index)
        val safePosition = positionMs.coerceAtLeast(0L)
        val safeDuration = durationMs.coerceAtLeast(0L)

        if (
            !completed &&
            safePosition < MIN_PROGRESS_MS &&
            old?.nextUp == true &&
            old.episodeId == episode?.id
        ) {
            return
        }

        val fraction = if (safeDuration > 0L) {
            safePosition.toDouble() / safeDuration.toDouble()
        } else {
            0.0
        }
        val reachedEnd = completed ||
            (safeDuration > 0L && fraction >= COMPLETE_FRACTION)
        val continueEpisode = if (reachedEnd) nextEpisode else episode
        val shouldQueueNext = reachedEnd && nextEpisode != null

        if (reachedEnd) {
            if (episode != null) {
                setEpisodeWatched(
                    sourceUrl = sourceUrl,
                    item = item,
                    episode = episode,
                    watched = true
                )
            } else {
                setWatched(
                    sourceUrl = sourceUrl,
                    item = item,
                    watched = true
                )
            }
        }

        val updated = snapshot(
            sourceUrl = sourceUrl,
            item = item,
            episode = continueEpisode
        ).copy(
            saved = old?.saved == true,
            positionMs = if (reachedEnd) 0L else safePosition,
            durationMs = if (reachedEnd) 0L else safeDuration,
            nextUp = shouldQueueNext,
            updatedAt = System.currentTimeMillis()
        )

        if (index >= 0) {
            if (!updated.saved && !updated.hasContinueState) {
                current.removeAt(index)
            } else {
                current[index] = updated
            }
        } else if (updated.saved || updated.hasContinueState) {
            current += updated
        } else {
            // Ignore very short playback so accidental starts do not pollute
            // Continue Watching.
            return
        }

        save(current)
    }

    fun clearContinueWatching(sourceUrl: String, item: MediaItem) {
        if (sourceUrl.isBlank()) return
        val key = mediaKey(sourceUrl, item)
        val current = read().toMutableList()
        val index = current.indexOfFirst { it.key == key }
        if (index < 0) return

        val old = current[index]
        val updated = old.copy(
            episodeId = "",
            episodeTitle = "",
            season = null,
            episode = null,
            positionMs = 0L,
            durationMs = 0L,
            nextUp = false,
            updatedAt = System.currentTimeMillis()
        )

        if (updated.saved) {
            current[index] = updated
        } else {
            current.removeAt(index)
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

    private fun episodeWatchedStorageKey(
        sourceUrl: String,
        item: MediaItem,
        episode: MediaEpisode
    ): String = "${mediaKey(sourceUrl, item)}|episode|${episodeWatchIdentity(episode)}"

    private fun episodeWatchedStorageIdPrefix(
        sourceUrl: String,
        item: MediaItem,
        episode: MediaEpisode
    ): String = if (episode.id.isBlank()) {
        ""
    } else {
        "${mediaKey(sourceUrl, item)}|episode|${episode.id}|"
    }

    private fun episodeWatchIdentity(episode: MediaEpisode): String =
        buildString {
            append(episode.id)
            append('|')
            append(episode.season ?: -1)
            append('|')
            append(episode.episode ?: -1)
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
                    .put("nextUp", entry.nextUp)
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
                    nextUp = obj.optBoolean("nextUp", false),
                    updatedAt = obj.optLong("updatedAt", 0L)
                )
            )
        }
    }.distinctBy { it.key }

    companion object {
        private const val KEY_ENTRIES = "entries_v1"
        private const val KEY_WATCHED = "watched_v1"
        private const val KEY_EPISODE_WATCHED = "episode_watched_v1"
        private const val MIN_PROGRESS_MS = 15_000L
        private const val COMPLETE_FRACTION = 0.95

        private val cacheLock = Any()
        @Volatile private var cachedRaw: String? = null
        private var cachedEntries: List<LocalMediaEntry> = emptyList()
    }
}
