package app.nudroidlabs.nustrim.core.model

/**
 * Normalises provider episode metadata before Details, resume and player navigation use it.
 * Explicit provider season/episode numbers always win. Missing values are inferred only from
 * common deterministic Stremio/video-id and title forms.
 */
object EpisodeEngine {
    fun polish(episodes: List<MediaEpisode>): List<MediaEpisode> {
        if (episodes.isEmpty()) return emptyList()

        val firstPass = episodes.mapIndexed { index, original ->
            val pair = pairHint(original.id)
                ?: pairHint(original.title)
            val season = original.season?.takeIf { it >= 0 }
                ?: pair?.first
            val episodeNumber = original.episode?.takeIf { it >= 0 }
                ?: pair?.second
                ?: episodeHint(original.title)
                ?: episodeHint(original.id)

            Candidate(
                sourceIndex = index,
                episode = if (
                    season == original.season &&
                    episodeNumber == original.episode
                ) {
                    original
                } else {
                    original.copy(
                        season = season,
                        episode = episodeNumber
                    )
                }
            )
        }

        val knownRegularSeasons = firstPass
            .mapNotNull { it.episode.season }
            .filter { it > 0 }
            .distinct()
        val singleRegularSeason = knownRegularSeasons.singleOrNull()

        val contextual = firstPass.map { candidate ->
            val episode = candidate.episode
            if (
                episode.season == null &&
                episode.episode != null &&
                singleRegularSeason != null
            ) {
                candidate.copy(
                    episode = episode.copy(season = singleRegularSeason)
                )
            } else {
                candidate
            }
        }

        return contextual
            .sortedWith(
                compareBy<Candidate>(
                    { seasonSortKey(it.episode.season) },
                    { it.episode.episode ?: Int.MAX_VALUE },
                    { it.sourceIndex }
                )
            )
            .distinctBy(::identityKey)
            .map { it.episode }
    }

    private fun pairHint(value: String): Pair<Int, Int>? {
        if (value.isBlank()) return null

        val match = COLON_PAIR.find(value)
            ?: SEASON_EPISODE.find(value)
            ?: X_PAIR.find(value)
            ?: return null

        val season = match.groupValues.getOrNull(1)
            ?.toIntOrNull()
            ?.takeIf { it >= 0 }
            ?: return null
        val episode = match.groupValues.getOrNull(2)
            ?.toIntOrNull()
            ?.takeIf { it > 0 }
            ?: return null
        return season to episode
    }

    private fun episodeHint(value: String): Int? {
        if (value.isBlank()) return null
        return EPISODE_ONLY.find(value)
            ?.groupValues
            ?.getOrNull(1)
            ?.toIntOrNull()
            ?.takeIf { it > 0 }
    }

    private fun seasonSortKey(season: Int?): Int = when {
        season == null -> UNKNOWN_SEASON_SORT_KEY
        season == 0 -> SPECIALS_SORT_KEY
        season > 0 -> season
        else -> UNKNOWN_SEASON_SORT_KEY
    }

    private fun identityKey(candidate: Candidate): String {
        val episode = candidate.episode
        return if (episode.season != null && episode.episode != null) {
            "slot:${episode.season}:${episode.episode}"
        } else if (episode.id.isNotBlank()) {
            "id:${episode.id}"
        } else {
            "source:${candidate.sourceIndex}"
        }
    }

    private data class Candidate(
        val sourceIndex: Int,
        val episode: MediaEpisode
    )

    private const val SPECIALS_SORT_KEY = 1_000_000
    private const val UNKNOWN_SEASON_SORT_KEY = 2_000_000

    // Stremio/Cinemeta style, for example tt10986410:1:10.
    private val COLON_PAIR = Regex("""(?:^|:)(\d{1,3}):(\d{1,4})$""")

    // S01E10, S1 E10, Season 1 Episode 10.
    private val SEASON_EPISODE = Regex(
        """(?i)(?:^|[^a-z0-9])s(?:eason)?\s*0*(\d{1,3})\s*e(?:pisode)?\s*0*(\d{1,4})(?:[^0-9]|$)"""
    )

    // 1x10.
    private val X_PAIR = Regex(
        """(?i)(?:^|[^0-9])0*(\d{1,3})x0*(\d{1,4})(?:[^0-9]|$)"""
    )

    // Episode 10, Ep 10, E10.
    private val EPISODE_ONLY = Regex(
        """(?i)(?:^|[^a-z0-9])(?:episode|ep\.?|e)\s*0*(\d{1,4})(?:[^0-9]|$)"""
    )
}
