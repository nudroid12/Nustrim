package app.nudroidlabs.nustrim.tv.episode

import app.nudroidlabs.nustrim.core.model.MediaEpisode
import java.util.LinkedHashMap

object TvEpisodeCatalogueBuilder {
    fun build(
        parentKey: String,
        providerEpisodes: List<MediaEpisode>,
    ): TvEpisodeCatalogue {
        if (providerEpisodes.isEmpty()) {
            return TvEpisodeCatalogue(
                parentKey = parentKey,
                seasons = emptyList(),
                diagnostics = TvEpisodeDiagnostics(
                    providerEntries = 0,
                    canonicalEntries = 0,
                    duplicateProviderIdsRemoved = 0,
                    coordinateCollisionsRetained = 0,
                    unknownSeasonCount = 0,
                    unknownEpisodeCount = 0,
                    invalidCoordinateCount = 0,
                ),
            )
        }

        var invalidCoordinateCount = 0
        val candidates = providerEpisodes.mapIndexed { index, raw ->
            val cleanSeason = raw.season?.takeIf { it >= 0 }.also {
                if (raw.season != null && it == null) invalidCoordinateCount += 1
            }
            val cleanEpisode = raw.episode?.takeIf { it > 0 }.also {
                if (raw.episode != null && it == null) invalidCoordinateCount += 1
            }
            val coordinateKind = when {
                cleanSeason != null && cleanEpisode != null -> TvEpisodeCoordinateKind.PROVIDER
                cleanSeason != null || cleanEpisode != null -> TvEpisodeCoordinateKind.PARTIAL_PROVIDER
                else -> TvEpisodeCoordinateKind.UNKNOWN
            }

            TvCanonicalEpisode(
                identity = TvEpisodeIdentity(
                    parentKey = parentKey,
                    providerEpisodeId = raw.id.trim(),
                    sourceIndex = index,
                ),
                providerEpisodeId = raw.id.trim(),
                title = canonicalEpisodeTitle(
                    rawTitle = raw.title,
                    episodeNumber = cleanEpisode,
                    sourceIndex = index,
                ),
                seasonNumber = cleanSeason,
                episodeNumber = cleanEpisode,
                displaySeasonNumber = cleanSeason,
                displayEpisodeNumber = cleanEpisode,
                thumbnailUrl = raw.thumbnailUrl.trim(),
                overview = raw.overview.trim(),
                sourceIndex = index,
                coordinateKind = coordinateKind,
                providerEpisode = raw,
            )
        }

        val byProviderId = LinkedHashMap<String, TvCanonicalEpisode>()
        val idless = mutableListOf<TvCanonicalEpisode>()
        var duplicateProviderIdsRemoved = 0

        candidates.forEach { candidate ->
            val id = candidate.providerEpisodeId
            if (id.isBlank()) {
                idless += candidate
                return@forEach
            }
            val existing = byProviderId[id]
            if (existing == null) {
                byProviderId[id] = candidate
            } else {
                duplicateProviderIdsRemoved += 1
                byProviderId[id] = mergeDuplicate(existing, candidate)
            }
        }

        val unique = (byProviderId.values + idless)
            .sortedBy { it.sourceIndex }

        val coordinateCollisionsRetained = unique
            .filter { it.seasonNumber != null && it.episodeNumber != null }
            .groupBy { it.seasonNumber!! to it.episodeNumber!! }
            .values
            .sumOf { group -> (group.size - 1).coerceAtLeast(0) }

        val allSeasonUnknown = unique.isNotEmpty() && unique.all { it.seasonNumber == null }
        val seasons = if (allSeasonUnknown) {
            val virtualEpisodes = unique.mapIndexed { displayIndex, episode ->
                episode.copy(
                    displaySeasonNumber = 1,
                    displayEpisodeNumber = episode.episodeNumber ?: displayIndex + 1,
                    coordinateKind = if (episode.coordinateKind == TvEpisodeCoordinateKind.UNKNOWN) {
                        TvEpisodeCoordinateKind.VIRTUAL_DISPLAY_ONLY
                    } else {
                        episode.coordinateKind
                    },
                )
            }
            listOf(
                TvEpisodeSeason(
                    seasonNumber = null,
                    displaySeasonNumber = 1,
                    isVirtual = true,
                    episodes = virtualEpisodes,
                ),
            )
        } else {
            val knownSeasons = unique.mapNotNull { it.seasonNumber }.distinct()
            val regular = knownSeasons.filter { it > 0 }.sorted()
            val specials = knownSeasons.filter { it == 0 }
            val orderedSeasonNumbers = regular + specials

            buildList {
                orderedSeasonNumbers.forEach { season ->
                    add(
                        TvEpisodeSeason(
                            seasonNumber = season,
                            displaySeasonNumber = season,
                            isVirtual = false,
                            episodes = unique
                                .filter { it.seasonNumber == season }
                                .sortedWith(episodeOrder),
                        ),
                    )
                }
                val unknown = unique.filter { it.seasonNumber == null }
                if (unknown.isNotEmpty()) {
                    add(
                        TvEpisodeSeason(
                            seasonNumber = null,
                            displaySeasonNumber = null,
                            isVirtual = false,
                            episodes = unknown.sortedWith(episodeOrder),
                        ),
                    )
                }
            }
        }

        return TvEpisodeCatalogue(
            parentKey = parentKey,
            seasons = seasons,
            diagnostics = TvEpisodeDiagnostics(
                providerEntries = providerEpisodes.size,
                canonicalEntries = unique.size,
                duplicateProviderIdsRemoved = duplicateProviderIdsRemoved,
                coordinateCollisionsRetained = coordinateCollisionsRetained,
                unknownSeasonCount = unique.count { it.seasonNumber == null },
                unknownEpisodeCount = unique.count { it.episodeNumber == null },
                invalidCoordinateCount = invalidCoordinateCount,
            ),
        )
    }

    private fun canonicalEpisodeTitle(
        rawTitle: String,
        episodeNumber: Int?,
        sourceIndex: Int,
    ): String {
        val clean = rawTitle.trim()
        if (clean.isBlank()) {
            return episodeNumber?.let { "Episode $it" } ?: "Episode ${sourceIndex + 1}"
        }

        val genericNumber = genericEpisodeTitle
            .matchEntire(clean)
            ?.groupValues
            ?.getOrNull(1)
            ?.toIntOrNull()

        return if (genericNumber != null && episodeNumber != null && genericNumber != episodeNumber) {
            "Episode $episodeNumber"
        } else {
            clean
        }
    }

    private val genericEpisodeTitle = Regex("(?i)^episode\\s+(\\d+)$")

    private fun mergeEpisodeTitle(first: String, later: String): String = when {
        first.isBlank() -> later
        later.isBlank() -> first
        genericEpisodeTitle.matches(first) && !genericEpisodeTitle.matches(later) -> later
        else -> first
    }

    private fun mergeDuplicate(
        first: TvCanonicalEpisode,
        later: TvCanonicalEpisode,
    ): TvCanonicalEpisode = first.copy(
        title = mergeEpisodeTitle(first.title, later.title),
        seasonNumber = first.seasonNumber ?: later.seasonNumber,
        episodeNumber = first.episodeNumber ?: later.episodeNumber,
        displaySeasonNumber = first.displaySeasonNumber ?: later.displaySeasonNumber,
        displayEpisodeNumber = first.displayEpisodeNumber ?: later.displayEpisodeNumber,
        thumbnailUrl = first.thumbnailUrl.ifBlank { later.thumbnailUrl },
        overview = first.overview.ifBlank { later.overview },
        coordinateKind = when {
            (first.seasonNumber ?: later.seasonNumber) != null &&
                (first.episodeNumber ?: later.episodeNumber) != null -> TvEpisodeCoordinateKind.PROVIDER
            (first.seasonNumber ?: later.seasonNumber) != null ||
                (first.episodeNumber ?: later.episodeNumber) != null -> TvEpisodeCoordinateKind.PARTIAL_PROVIDER
            else -> TvEpisodeCoordinateKind.UNKNOWN
        },
    )

    private val episodeOrder = compareBy<TvCanonicalEpisode>(
        { it.episodeNumber ?: Int.MAX_VALUE },
        { it.sourceIndex },
    )
}
