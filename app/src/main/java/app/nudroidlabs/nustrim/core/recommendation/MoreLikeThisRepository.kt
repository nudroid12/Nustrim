package app.nudroidlabs.nustrim.core.recommendation

import app.nudroidlabs.nustrim.core.model.MediaCatalog
import app.nudroidlabs.nustrim.core.model.MediaItem
import app.nudroidlabs.nustrim.core.model.MediaType
import app.nudroidlabs.nustrim.core.source.CatalogSectionSourceSession
import app.nudroidlabs.nustrim.core.source.SourceSession
import kotlin.math.abs

object MoreLikeThisRepository {
    private const val CACHE_TTL_MS = 10 * 60 * 1000L
    private const val MAX_CACHE_ENTRIES = 48

    private data class CacheEntry(
        val createdAtMs: Long,
        val items: List<MediaItem>
    )

    private val cache = object : LinkedHashMap<String, CacheEntry>(64, 0.75f, true) {
        override fun removeEldestEntry(
            eldest: MutableMap.MutableEntry<String, CacheEntry>?
        ): Boolean = size > MAX_CACHE_ENTRIES
    }

    fun load(
        session: SourceSession,
        current: MediaItem,
        limit: Int = 18,
        onSuccess: (List<MediaItem>) -> Unit,
        onError: (Throwable) -> Unit
    ) {
        val cacheKey = buildCacheKey(session, current, limit)
        synchronized(cache) {
            val cached = cache[cacheKey]
            if (cached != null) {
                if (System.currentTimeMillis() - cached.createdAtMs < CACHE_TTL_MS) {
                    onSuccess(cached.items)
                    return
                }
                cache.remove(cacheKey)
            }
        }

        fun complete(catalogs: List<MediaCatalog>) {
            val ranked = MoreLikeThisEngine.rank(
                current = current,
                catalogs = catalogs,
                limit = limit
            )
            synchronized(cache) {
                cache[cacheKey] = CacheEntry(
                    createdAtMs = System.currentTimeMillis(),
                    items = ranked
                )
            }
            onSuccess(ranked)
        }

        val sectioned = session as? CatalogSectionSourceSession
        if (sectioned != null) {
            sectioned.loadCatalogSections(
                onSuccess = ::complete,
                onError = onError
            )
        } else {
            session.loadCatalog(
                onSuccess = { complete(listOf(it)) },
                onError = onError
            )
        }
    }

    private fun buildCacheKey(
        session: SourceSession,
        current: MediaItem,
        limit: Int
    ): String {
        val mediaIdentity = current.ref?.metaId
            ?.takeIf { it.isNotBlank() }
            ?: current.id
                .takeIf { it.isNotBlank() }
            ?: current.title.trim().lowercase()

        return "${session.id}|$mediaIdentity|$limit"
    }
}

object MoreLikeThisEngine {
    private data class ScoredItem(
        val item: MediaItem,
        val score: Int,
        val catalogIndex: Int,
        val itemIndex: Int
    )

    fun rank(
        current: MediaItem,
        catalogs: List<MediaCatalog>,
        limit: Int = 18
    ): List<MediaItem> {
        if (limit <= 0) return emptyList()

        val currentIdentity = identity(current)
        val currentGenres = normalizedSet(current.genres)
        val currentDirectors = normalizedSet(current.director)
        val currentCast = normalizedSet(current.cast)
        val currentYear = extractYear(current.releaseInfo)

        return catalogs
            .flatMapIndexed { catalogIndex, catalog ->
                catalog.items.mapIndexed { itemIndex, candidate ->
                    Triple(catalogIndex, itemIndex, candidate)
                }
            }
            .asSequence()
            .filter { (_, _, candidate) ->
                identity(candidate) != currentIdentity &&
                    candidate.title.isNotBlank() &&
                    candidate.type != MediaType.LIVE &&
                    candidate.type != MediaType.CHANNEL &&
                    (
                        current.type == MediaType.UNKNOWN ||
                            candidate.type == MediaType.UNKNOWN ||
                            candidate.type == current.type
                        )
            }
            .distinctBy { (_, _, candidate) -> identity(candidate) }
            .map { (catalogIndex, itemIndex, candidate) ->
                ScoredItem(
                    item = candidate,
                    score = score(
                        current = current,
                        candidate = candidate,
                        currentGenres = currentGenres,
                        currentDirectors = currentDirectors,
                        currentCast = currentCast,
                        currentYear = currentYear
                    ),
                    catalogIndex = catalogIndex,
                    itemIndex = itemIndex
                )
            }
            .filter { scored ->
                scored.score >= minimumUsefulScore(current, scored.item)
            }
            .sortedWith(
                compareByDescending<ScoredItem> { it.score }
                    .thenBy { it.catalogIndex }
                    .thenBy { it.itemIndex }
                    .thenBy { it.item.title.lowercase() }
            )
            .take(limit)
            .map { it.item }
            .toList()
    }

    private fun score(
        current: MediaItem,
        candidate: MediaItem,
        currentGenres: Set<String>,
        currentDirectors: Set<String>,
        currentCast: Set<String>,
        currentYear: Int?
    ): Int {
        var score = 0

        if (
            current.type != MediaType.UNKNOWN &&
            candidate.type == current.type
        ) {
            score += 34
        } else if (
            current.type == MediaType.UNKNOWN ||
            candidate.type == MediaType.UNKNOWN
        ) {
            score += 10
        }

        val candidateGenres = normalizedSet(candidate.genres)
        val sharedGenres = currentGenres intersect candidateGenres
        score += sharedGenres.size * 18

        if (currentGenres.isNotEmpty() && candidateGenres.isNotEmpty()) {
            val unionSize = (currentGenres union candidateGenres).size.coerceAtLeast(1)
            score += ((sharedGenres.size.toFloat() / unionSize.toFloat()) * 24f).toInt()
        }

        val candidateDirectors = normalizedSet(candidate.director)
        if ((currentDirectors intersect candidateDirectors).isNotEmpty()) {
            score += 28
        }

        val candidateCast = normalizedSet(candidate.cast)
        score += (currentCast intersect candidateCast).size.coerceAtMost(3) * 7

        val candidateYear = extractYear(candidate.releaseInfo)
        if (currentYear != null && candidateYear != null) {
            score += when (abs(currentYear - candidateYear)) {
                0, 1, 2 -> 14
                in 3..5 -> 10
                in 6..10 -> 5
                else -> 0
            }
        }

        candidate.rating
            .trim()
            .toDoubleOrNull()
            ?.let { rating ->
                score += when {
                    rating >= 8.0 -> 8
                    rating >= 7.0 -> 5
                    rating >= 6.0 -> 2
                    else -> 0
                }
            }

        if (candidate.posterUrl.isNotBlank()) score += 4
        if (candidate.backgroundUrl.isNotBlank()) score += 2

        return score
    }

    private fun minimumUsefulScore(
        current: MediaItem,
        candidate: MediaItem
    ): Int {
        return if (
            current.type != MediaType.UNKNOWN &&
            candidate.type == current.type
        ) {
            34
        } else {
            22
        }
    }

    private fun identity(item: MediaItem): String {
        val ref = item.ref?.metaId
            ?.trim()
            ?.lowercase()
            .orEmpty()
        if (ref.isNotBlank()) return "ref:$ref"

        val id = item.id.trim().lowercase()
        if (id.isNotBlank()) return "id:$id"

        return "title:${item.title.trim().lowercase()}|${item.type}"
    }

    private fun normalizedSet(values: List<String>): Set<String> =
        values.asSequence()
            .map { it.trim().lowercase() }
            .filter { it.isNotBlank() }
            .toSet()

    private fun extractYear(value: String): Int? =
        Regex("""\b(19|20)\d{2}\b""")
            .find(value)
            ?.value
            ?.toIntOrNull()
}
