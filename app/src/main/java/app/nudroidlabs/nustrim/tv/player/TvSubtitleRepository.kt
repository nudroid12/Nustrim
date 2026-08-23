package app.nudroidlabs.nustrim.tv.player

import android.content.Context
import app.nudroidlabs.nustrim.core.diagnostics.NustrimDiagnostics
import app.nudroidlabs.nustrim.core.model.MediaEpisode
import app.nudroidlabs.nustrim.core.model.MediaItem
import app.nudroidlabs.nustrim.core.model.SubtitleSource
import app.nudroidlabs.nustrim.core.source.InstalledSource
import app.nudroidlabs.nustrim.core.source.InstalledSourceStore
import app.nudroidlabs.nustrim.core.source.SourceEngine
import app.nudroidlabs.nustrim.core.source.SourceSession
import app.nudroidlabs.nustrim.ui.SubtitleDisplayMode
import app.nudroidlabs.nustrim.ui.UiPreferences
import java.util.LinkedHashMap
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull

internal class TvSubtitleRepository(context: Context) {
    private val appContext = context.applicationContext
    private val sourceEngine = SourceEngine(appContext)
    private val sourceStore = InstalledSourceStore(appContext)
    private val preferences = UiPreferences(appContext)

    suspend fun enrich(request: TvPlaybackRequest): TvPlaybackRequest {
        val installed = sourceStore.visibleSources(preferences.developerMode)
            .filter { it.enabled }
            .sortedBy { source ->
                if (source.url == InstalledSourceStore.OPENSUBTITLES_URL) 0 else 1
            }
        val preference = SubtitlePreference(
            preferred = canonicalLanguageCode(preferences.subtitlePreferredLanguage),
            second = canonicalLanguageCode(preferences.subtitleSecondPreferredLanguage),
            displayMode = preferences.subtitleDisplayMode,
        )
        val cacheKey = cacheKey(
            media = request.media,
            episode = request.episode,
            installed = installed,
        )
        val external = cached(cacheKey) ?: externalSubtitles(
            installed = installed,
            media = request.media,
            episode = request.episode,
        ).also { cache(cacheKey, it) }
        val merged = orderByPreference(
            subtitles = mergeSubtitles(
                streamSubtitles = request.stream.subtitles,
                externalSubtitles = external,
            ),
            preference = preference,
        )

        NustrimDiagnostics.log(
            "TV_SUBTITLE_PREPARE",
            "media=${request.media.title} episode=${request.episode?.id.orEmpty()} " +
                "embedded=${request.stream.subtitles.size} external=${external.size} " +
                "visible=${merged.size} mode=${preference.displayMode}",
        )

        if (merged == request.stream.subtitles) return request
        return request.copy(stream = request.stream.copy(subtitles = merged))
    }

    private suspend fun externalSubtitles(
        installed: List<InstalledSource>,
        media: MediaItem,
        episode: MediaEpisode?,
    ): List<SubtitleSource> = supervisorScope {
        if (installed.isEmpty()) return@supervisorScope emptyList()

        val collected = mutableListOf<SubtitleSource>()
        val lock = Any()
        val jobs = installed.map { source ->
            async {
                val loaded = withTimeoutOrNull(PER_SOURCE_TIMEOUT_MS) {
                    loadFromSource(
                        installed = source,
                        media = media,
                        episode = episode,
                    )
                }.orEmpty()
                if (loaded.isNotEmpty()) {
                    synchronized(lock) { collected += loaded }
                }
            }
        }

        withTimeoutOrNull(OVERALL_TIMEOUT_MS) { jobs.awaitAll() }
        jobs.forEach { it.cancel() }
        jobs.joinAll()

        synchronized(lock) {
            collected
                .filter { it.url.isNotBlank() }
                .distinctBy(::subtitleIdentity)
        }
    }

    private suspend fun loadFromSource(
        installed: InstalledSource,
        media: MediaItem,
        episode: MediaEpisode?,
    ): List<SubtitleSource> {
        val session = sourceEngine.awaitOpen(installed.url)
        val supportsSubtitles = session.capabilities.resources.any { resource ->
            resource.equals("subtitles", ignoreCase = true) ||
                resource.equals("subtitle", ignoreCase = true)
        }
        if (!supportsSubtitles) return emptyList()

        val provider = session.displayName
            .ifBlank { installed.label }
            .ifBlank { "Subtitle source" }
        return session.awaitSubtitles(media, episode).map { subtitle ->
            subtitle.copy(
                label = providerAwareLabel(
                    provider = provider,
                    subtitle = subtitle,
                ),
            )
        }
    }

    private fun orderByPreference(
        subtitles: List<SubtitleSource>,
        preference: SubtitlePreference,
    ): List<SubtitleSource> {
        // Keep every discovered track attached to Media3. Visibility belongs to the
        // picker so a user can recover with Show All without rebuilding playback.
        return subtitles.sortedWith(
            compareBy<SubtitleSource> { subtitle -> languageRank(subtitle, preference) }
                .thenBy { subtitle -> subtitle.language.lowercase(Locale.ROOT) }
                .thenBy { subtitle -> subtitle.label.lowercase(Locale.ROOT) },
        )
    }

    private fun languageRank(
        subtitle: SubtitleSource,
        preference: SubtitlePreference,
    ): Int = when {
        subtitleMatches(subtitle, preference.preferred) -> 0
        subtitleMatches(subtitle, preference.second) -> 1
        else -> OTHER_LANGUAGE_RANK
    }

    private fun subtitleMatches(subtitle: SubtitleSource, target: String): Boolean {
        if (target.isBlank()) return false
        if (canonicalLanguageCode(subtitle.language) == target) return true
        val label = subtitle.label.lowercase(Locale.ROOT)
        return LANGUAGE_LABELS[target].orEmpty().any { alias ->
            Regex("(^|[^a-z])${Regex.escape(alias)}([^a-z]|$)").containsMatchIn(label)
        }
    }

    private fun canonicalLanguageCode(raw: String): String {
        val base = raw.trim().lowercase(Locale.ROOT).substringBefore('-').substringBefore('_')
        return when (base) {
            "eng", "english" -> "en"
            "msa", "may", "malay", "melayu" -> "ms"
            "ind", "indonesian" -> "id"
            "spa", "spanish" -> "es"
            "por", "portuguese" -> "pt"
            "fra", "fre", "french" -> "fr"
            "deu", "ger", "german" -> "de"
            "ita", "italian" -> "it"
            "jpn", "japanese" -> "ja"
            "kor", "korean" -> "ko"
            "zho", "chi", "chinese" -> "zh"
            "ara", "arabic" -> "ar"
            "", "und", "unknown", "mul", "zxx" -> ""
            else -> base
        }
    }

    private fun mergeSubtitles(
        streamSubtitles: List<SubtitleSource>,
        externalSubtitles: List<SubtitleSource>,
    ): List<SubtitleSource> {
        val seen = LinkedHashMap<String, SubtitleSource>()
        (streamSubtitles + externalSubtitles).forEach { subtitle ->
            if (subtitle.url.isBlank()) return@forEach
            val identity = subtitleIdentity(subtitle)
            if (!seen.containsKey(identity)) seen[identity] = subtitle
        }
        return seen.values.toList()
    }

    private fun providerAwareLabel(
        provider: String,
        subtitle: SubtitleSource,
    ): String {
        val base = subtitle.label
            .ifBlank { subtitle.language }
            .ifBlank { "Subtitle" }
            .trim()
        return if (base.contains(PROVIDER_SEPARATOR)) {
            base
        } else {
            "$provider$PROVIDER_SEPARATOR$base"
        }
    }

    private fun cacheKey(
        media: MediaItem,
        episode: MediaEpisode?,
        installed: List<InstalledSource>,
    ): String = buildString {
        append(media.ref?.metaId ?: media.id)
        append('/')
        append(episode?.id ?: "movie")
        append('/')
        installed.forEach { source ->
            append(source.url)
            append('|')
        }
    }

    private fun subtitleIdentity(subtitle: SubtitleSource): String = listOf(
        subtitle.url.trim(),
        canonicalLanguageCode(subtitle.language),
        subtitle.label.trim().lowercase(Locale.ROOT),
    ).joinToString("|")

    private fun cached(key: String): List<SubtitleSource>? = synchronized(cache) {
        val item = cache[key] ?: return@synchronized null
        if (System.currentTimeMillis() - item.createdAtMs >= CACHE_TTL_MS) {
            cache.remove(key)
            null
        } else {
            item.subtitles
        }
    }

    private fun cache(key: String, subtitles: List<SubtitleSource>) {
        synchronized(cache) {
            cache[key] = CachedSubtitles(
                createdAtMs = System.currentTimeMillis(),
                subtitles = subtitles,
            )
        }
    }

    private data class SubtitlePreference(
        val preferred: String,
        val second: String,
        val displayMode: SubtitleDisplayMode,
    )

    private data class CachedSubtitles(
        val createdAtMs: Long,
        val subtitles: List<SubtitleSource>,
    )

    private companion object {
        const val PROVIDER_SEPARATOR = "|||NUSTRIM_PROVIDER|||"
        const val OVERALL_TIMEOUT_MS = 3_500L
        const val PER_SOURCE_TIMEOUT_MS = 3_000L
        const val CACHE_TTL_MS = 10 * 60_000L
        const val MAX_CACHE_ENTRIES = 32
        const val OTHER_LANGUAGE_RANK = 2

        val LANGUAGE_LABELS = mapOf(
            "en" to listOf("english", "eng"),
            "ms" to listOf("malay", "melayu", "bahasa melayu", "msa", "may"),
            "id" to listOf("indonesian", "indonesia", "ind"),
            "es" to listOf("spanish", "espanol", "spa"),
            "pt" to listOf("portuguese", "por"),
            "fr" to listOf("french", "francais", "fra", "fre"),
            "de" to listOf("german", "deutsch", "deu", "ger"),
            "it" to listOf("italian", "ita"),
            "ja" to listOf("japanese", "jpn"),
            "ko" to listOf("korean", "kor"),
            "zh" to listOf("chinese", "mandarin", "zho", "chi"),
            "ar" to listOf("arabic", "ara"),
        )

        val cache = object : LinkedHashMap<String, CachedSubtitles>(40, 0.75f, true) {
            override fun removeEldestEntry(
                eldest: MutableMap.MutableEntry<String, CachedSubtitles>?,
            ): Boolean = size > MAX_CACHE_ENTRIES
        }
    }
}

private suspend fun SourceEngine.awaitOpen(url: String): SourceSession =
    suspendCancellableCoroutine { continuation ->
        open(
            input = url,
            onSuccess = { session ->
                if (continuation.isActive) continuation.resume(session)
            },
            onError = { error ->
                if (continuation.isActive) continuation.resumeWithException(error)
            },
        )
    }

private suspend fun SourceSession.awaitSubtitles(
    media: MediaItem,
    episode: MediaEpisode?,
): List<SubtitleSource> = suspendCancellableCoroutine { continuation ->
    loadSubtitles(
        item = media,
        episode = episode,
        onSuccess = { subtitles ->
            if (continuation.isActive) continuation.resume(subtitles)
        },
        onError = {
            if (continuation.isActive) continuation.resume(emptyList())
        },
    )
}
