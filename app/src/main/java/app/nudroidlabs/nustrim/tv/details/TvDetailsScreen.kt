package app.nudroidlabs.nustrim.tv.details
import android.view.KeyEvent as AndroidKeyEvent

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.BookmarkAdd
import androidx.compose.material.icons.outlined.BookmarkRemove
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import app.nudroidlabs.nustrim.core.library.LocalMediaStore
import app.nudroidlabs.nustrim.core.model.MediaEpisode
import app.nudroidlabs.nustrim.core.model.EpisodeEngine
import app.nudroidlabs.nustrim.core.model.MediaItem
import app.nudroidlabs.nustrim.core.model.MediaType
import app.nudroidlabs.nustrim.core.model.StreamSource
import app.nudroidlabs.nustrim.core.model.SubtitleSource
import app.nudroidlabs.nustrim.core.source.SourceEngine
import app.nudroidlabs.nustrim.core.source.SourceSession
import app.nudroidlabs.nustrim.core.source.StreamAggregationPhase
import app.nudroidlabs.nustrim.core.source.StreamSourceAggregator
import app.nudroidlabs.nustrim.core.source.SubtitleSourceAggregator
import app.nudroidlabs.nustrim.tv.home.TvHomeEntry
import app.nudroidlabs.nustrim.tv.player.TvPlayerScreen
import app.nudroidlabs.nustrim.tv.theme.TvColors
import coil3.compose.AsyncImage
import kotlinx.coroutines.delay

data class TvSourcePreviewRequest(
    val sourceUrl: String,
    val session: SourceSession?,
    val item: MediaItem,
    val episode: MediaEpisode?,
    val autoPlay: Boolean = false,
    val preferredProviderId: String = "",
    val preferredProviderName: String = "",
    val startFromBeginning: Boolean = false
)

private const val EPISODE_SCROLL_REPEAT_THROTTLE_MS = 80L

@Composable
fun TvDetailsScreen(
    entry: TvHomeEntry,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val engine = remember(context) { SourceEngine(context) }
    val mediaStore = remember(context) { LocalMediaStore(context) }
    val firstActionRequester = remember { FocusRequester() }
    val manualPlayRequester = remember { FocusRequester() }
    val libraryActionRequester = remember { FocusRequester() }
    val watchedActionRequester = remember { FocusRequester() }
    val detailsListState = rememberLazyListState()
    val seasonListState = rememberLazyListState()
    val episodeListState = rememberLazyListState()
    var lastEpisodeRepeatAt by remember(entry.stableKey) { mutableStateOf(0L) }
    val episodeFocusRequesters = remember(entry.stableKey) {
        mutableMapOf<String, FocusRequester>()
    }

    var lastFocusedEpisodeKey by remember(entry.stableKey) {
        mutableStateOf(
            entry.continueEntry
                ?.toEpisode()
                ?.let(::episodeFocusKey)
        )
    }
    var pendingEpisodeFocusRestoreKey by remember(entry.stableKey) {
        mutableStateOf<String?>(null)
    }
    var pendingSeasonSelection by remember(entry.stableKey) {
        mutableStateOf<Int?>(null)
    }

    var resolvedSession by remember(entry.stableKey) {
        mutableStateOf<SourceSession?>(entry.session)
    }
    var detailedItem by remember(entry.stableKey) {
        mutableStateOf(entry.item)
    }
    var loading by remember(entry.stableKey) { mutableStateOf(true) }
    var detailsResolved by remember(entry.stableKey) { mutableStateOf(false) }
    var errorMessage by remember(entry.stableKey) { mutableStateOf<String?>(null) }
    var selectedSeason by remember(entry.stableKey) { mutableStateOf<Int?>(null) }

    LaunchedEffect(pendingSeasonSelection) {
        val targetSeason = pendingSeasonSelection
            ?: return@LaunchedEffect
        delay(150)
        selectedSeason = targetSeason
        lastFocusedEpisodeKey = null
        pendingEpisodeFocusRestoreKey = null
        pendingSeasonSelection = null
    }

    var sourcePreview by remember(entry.stableKey) {
        mutableStateOf<TvSourcePreviewRequest?>(null)
    }
    var restoreSourceFocus by remember(entry.stableKey) {
        mutableStateOf(false)
    }
    var playingStream by remember(entry.stableKey) {
        mutableStateOf<StreamSource?>(null)
    }
    var sourcePlayerReturnToken by remember(entry.stableKey) {
        mutableIntStateOf(0)
    }
    var episodeWatchedRevision by remember(entry.stableKey) {
        mutableIntStateOf(0)
    }
    var episodeOptions by remember(entry.stableKey) {
        mutableStateOf<MediaEpisode?>(null)
    }
    var seasonOptions by remember(entry.stableKey) {
        mutableStateOf<Int?>(null)
    }
    var lastDetailsRequester by remember(entry.stableKey) {
        mutableStateOf<FocusRequester?>(null)
    }

    var savedToLibrary by remember(entry.stableKey) {
        mutableStateOf(mediaStore.isSaved(entry.sourceUrl, entry.item))
    }
    var movieWatched by remember(entry.stableKey) {
        mutableStateOf(mediaStore.isWatched(entry.sourceUrl, entry.item))
    }
    var logoLoadFailed by remember(entry.stableKey) {
        mutableStateOf(false)
    }

    fun loadWith(session: SourceSession) {
        resolvedSession = session
        session.loadDetails(
            item = entry.item,
            onSuccess = { loaded ->
                detailedItem = loaded
                savedToLibrary = mediaStore.isSaved(entry.sourceUrl, loaded)
                movieWatched = mediaStore.isWatched(entry.sourceUrl, loaded)
                logoLoadFailed = false
                detailsResolved = true
                loading = false
                errorMessage = null
            },
            onError = { error ->
                detailsResolved = true
                loading = false
                errorMessage = error.message ?: "Could not load full details."
            }
        )
    }

    LaunchedEffect(entry.stableKey) {
        loading = true
        detailsResolved = false
        errorMessage = null

        val existing = entry.session
        if (existing != null) {
            loadWith(existing)
        } else if (entry.sourceUrl.isNotBlank()) {
            engine.open(
                entry.sourceUrl,
                onSuccess = { session ->
                    loadWith(session)
                },
                onError = { error ->
                    detailsResolved = true
                    loading = false
                    errorMessage = error.message ?: "Could not open source."
                }
            )
        } else {
            detailsResolved = true
            loading = false
            errorMessage = "Source information is unavailable."
        }
    }

    val resolvedEpisodes = if (detailsResolved) {
        detailedItem.episodes
    } else {
        emptyList()
    }
    val sortedEpisodes = remember(resolvedEpisodes) {
        EpisodeEngine.polish(resolvedEpisodes)
    }
    val rawSeasons = sortedEpisodes.mapNotNull { it.season }.distinct().sorted()
    val regularSeasons = rawSeasons.filter { it > 0 }
    val specialSeasons = rawSeasons.filter { it == 0 }
    val seasons = (regularSeasons + specialSeasons).ifEmpty { rawSeasons }
    val localContinueEntry = remember(
        entry.sourceUrl,
        detailedItem.id,
        detailedItem.ref?.metaId,
        sourcePlayerReturnToken,
        episodeWatchedRevision
    ) {
        val detailMetaId = detailedItem.ref?.metaId.orEmpty()
        mediaStore.all().firstOrNull { stored ->
            stored.sourceUrl == entry.sourceUrl &&
                stored.hasContinueState &&
                (
                    stored.mediaId == detailedItem.id ||
                        (
                            detailMetaId.isNotBlank() &&
                                stored.refMetaId == detailMetaId
                            )
                    )
        }
    }
    val effectiveContinueEntry = if (sourcePlayerReturnToken > 0) {
        localContinueEntry
    } else {
        localContinueEntry ?: entry.continueEntry
    }
    val storedContinueEpisode = effectiveContinueEntry
        ?.toEpisode()
        ?.let { stored ->
            sortedEpisodes.firstOrNull { candidate ->
                candidate.id == stored.id
            } ?: stored
        }
    val storedContinueSeason = storedContinueEpisode?.season
        ?.takeIf { it in seasons }

    LaunchedEffect(
        detailedItem.id,
        seasons,
        effectiveContinueEntry?.episodeId
    ) {
        selectedSeason = storedContinueSeason ?: seasons.firstOrNull()
    }

    val visibleEpisodes = if (selectedSeason == null) {
        sortedEpisodes
    } else {
        sortedEpisodes.filter { it.season == selectedSeason }
    }
    val primaryEpisode = visibleEpisodes.firstOrNull() ?: sortedEpisodes.firstOrNull()
    val isSeries = detailedItem.type == MediaType.SERIES || sortedEpisodes.isNotEmpty()
    val resumeEpisode = if (isSeries) {
        storedContinueEpisode ?: primaryEpisode
    } else {
        null
    }
    val resumePositionMs = mediaStore.resumePosition(
        sourceUrl = entry.sourceUrl,
        item = detailedItem,
        episode = resumeEpisode
    )
    val resumeActionLabel = when {
        effectiveContinueEntry?.nextUp == true && resumeEpisode != null -> "Next episode"
        resumePositionMs > 0L -> "Resume"
        else -> "Play"
    }
    val resumeContextLabel = when {
        effectiveContinueEntry?.nextUp == true && resumeEpisode != null -> {
            "Next: ${resumeEpisode.displayTitle}"
        }

        resumePositionMs > 0L && resumeEpisode != null -> {
            "Resume: ${resumeEpisode.displayTitle}"
        }

        else -> null
    }

    val heroTargetEpisode = if (isSeries) resumeEpisode ?: primaryEpisode else null
    val creditLine = remember(
        detailedItem.director,
        detailedItem.writer,
        isSeries
    ) {
        val directors = detailedItem.director.joinToString(", ").takeIf { it.isNotBlank() }
        val writers = detailedItem.writer.joinToString(", ").takeIf { it.isNotBlank() }
        when {
            directors != null -> {
                val label = if (isSeries) "Creator" else "Director"
                "$label: $directors"
            }

            writers != null -> "Writer: $writers"
            else -> null
        }
    }
    val metaLabels = remember(
        detailedItem.releaseInfo,
        detailedItem.runtime,
        detailedItem.rating,
        isSeries,
        sortedEpisodes.size
    ) {
        buildList {
            detailedItem.releaseInfo
                .trim()
                .takeIf { it.isNotBlank() }
                ?.let(::add)
            add(if (isSeries) "Series" else "Movie")
            detailedItem.runtime
                .trim()
                .takeIf { it.isNotBlank() }
                ?.let(::add)
            detailedItem.rating
                .trim()
                .takeIf { it.isNotBlank() }
                ?.let { add("IMDb $it") }
            if (isSeries && sortedEpisodes.isNotEmpty()) {
                add("${sortedEpisodes.size} episodes")
            }
        }
    }
    val genreLine = remember(detailedItem.genres) {
        detailedItem.genres
            .filter { it.isNotBlank() }
            .take(5)
            .joinToString(" • ")
    }

    val localPlaybackEntry = effectiveContinueEntry
    LaunchedEffect(sourcePlayerReturnToken, detailedItem.id) {
        movieWatched = mediaStore.isWatched(
            sourceUrl = entry.sourceUrl,
            item = detailedItem
        )
    }

    val watchedEpisodeKeys = remember(
        entry.stableKey,
        sourcePlayerReturnToken,
        episodeWatchedRevision
    ) {
        mediaStore.watchedEpisodeKeys(
            sourceUrl = entry.sourceUrl,
            item = detailedItem
        )
    }

    val selectedSeasonEpisodes = remember(selectedSeason, sortedEpisodes) {
        selectedSeason?.let { season ->
            sortedEpisodes.filter { it.season == season }
        }.orEmpty()
    }
    val selectedSeasonWatchedCount = remember(
        selectedSeasonEpisodes,
        watchedEpisodeKeys
    ) {
        selectedSeasonEpisodes.count { episode ->
            episodeHasWatchedMark(episode, watchedEpisodeKeys)
        }
    }
    fun markEpisodeWatched(
        episode: MediaEpisode,
        watched: Boolean
    ) {
        mediaStore.setEpisodeWatched(
            sourceUrl = entry.sourceUrl,
            item = detailedItem,
            episode = episode,
            watched = watched
        )

        val isContinueEpisode = effectiveContinueEntry?.let { stored ->
            stored.episodeId == episode.id
        } == true

        if (watched && isContinueEpisode) {
            val currentIndex = sortedEpisodes.indexOfFirst {
                episodeFocusKey(it) == episodeFocusKey(episode)
            }
            val next = currentIndex
                .takeIf { it >= 0 }
                ?.let { sortedEpisodes.getOrNull(it + 1) }

            mediaStore.recordProgress(
                sourceUrl = entry.sourceUrl,
                item = detailedItem,
                episode = episode,
                positionMs = 0L,
                durationMs = 0L,
                completed = true,
                nextEpisode = next
            )
            if (next == null) {
                mediaStore.setWatched(
                    sourceUrl = entry.sourceUrl,
                    item = detailedItem,
                    watched = true
                )
            }
        } else if (!watched) {
            mediaStore.setWatched(
                sourceUrl = entry.sourceUrl,
                item = detailedItem,
                watched = false
            )
        }

        episodeWatchedRevision += 1
    }

    fun markSeasonWatched(
        season: Int,
        watched: Boolean
    ) {
        val seasonEpisodes = sortedEpisodes.filter { it.season == season }
        if (seasonEpisodes.isEmpty()) return

        mediaStore.setSeasonWatched(
            sourceUrl = entry.sourceUrl,
            item = detailedItem,
            episodes = seasonEpisodes,
            watched = watched
        )

        val continueEpisode = storedContinueEpisode
        if (watched && continueEpisode?.season == season) {
            val lastSeasonIndex = sortedEpisodes.indexOfLast { it.season == season }
            val next = lastSeasonIndex
                .takeIf { it >= 0 }
                ?.let { sortedEpisodes.getOrNull(it + 1) }

            mediaStore.recordProgress(
                sourceUrl = entry.sourceUrl,
                item = detailedItem,
                episode = continueEpisode,
                positionMs = 0L,
                durationMs = 0L,
                completed = true,
                nextEpisode = next
            )
            if (next == null) {
                mediaStore.setWatched(
                    sourceUrl = entry.sourceUrl,
                    item = detailedItem,
                    watched = true
                )
            }
        } else if (!watched) {
            mediaStore.setWatched(
                sourceUrl = entry.sourceUrl,
                item = detailedItem,
                watched = false
            )
        }

        episodeWatchedRevision += 1
    }

    fun markPreviousEpisodesWatched(episode: MediaEpisode) {
        val targetNumber = episode.episode ?: return
        val season = episode.season ?: return
        val previous = sortedEpisodes.filter { candidate ->
            candidate.season == season &&
                candidate.episode != null &&
                candidate.episode < targetNumber
        }
        if (previous.isEmpty()) return

        mediaStore.setSeasonWatched(
            sourceUrl = entry.sourceUrl,
            item = detailedItem,
            episodes = previous,
            watched = true
        )
        episodeWatchedRevision += 1
    }

    fun markPreviousSeasonsWatched(season: Int) {
        val previous = sortedEpisodes.filter { episode ->
            val episodeSeason = episode.season
            episodeSeason != null &&
                episodeSeason > 0 &&
                episodeSeason < season
        }
        if (previous.isEmpty()) return

        mediaStore.setSeasonWatched(
            sourceUrl = entry.sourceUrl,
            item = detailedItem,
            episodes = previous,
            watched = true
        )
        episodeWatchedRevision += 1
    }

    LaunchedEffect(seasons, selectedSeason) {
        val index = seasons.indexOf(selectedSeason)
        if (index >= 0) {
            seasonListState.scrollToItem(index)
        }
    }

    LaunchedEffect(selectedSeason) {
        if (
            pendingEpisodeFocusRestoreKey == null &&
            visibleEpisodes.isNotEmpty()
        ) {
            episodeListState.scrollToItem(0)
        }
    }

    fun openSources(
        episode: MediaEpisode?,
        autoPlay: Boolean = false,
        preferredProviderId: String = "",
        preferredProviderName: String = "",
        startFromBeginning: Boolean = false
    ) {
        restoreSourceFocus = false
        sourcePreview = TvSourcePreviewRequest(
            sourceUrl = entry.sourceUrl,
            session = resolvedSession,
            item = detailedItem,
            episode = episode,
            autoPlay = autoPlay,
            preferredProviderId = preferredProviderId,
            preferredProviderName = preferredProviderName,
            startFromBeginning = startFromBeginning
        )
    }

    val activeEpisode = sourcePreview?.episode
    val activeEpisodeIndex = activeEpisode?.let { current ->
        sortedEpisodes.indexOfFirst { candidate ->
            candidate.id == current.id &&
                candidate.season == current.season &&
                candidate.episode == current.episode
        }
    } ?: -1
    val previousEpisode = activeEpisodeIndex
        .takeIf { it > 0 }
        ?.let { sortedEpisodes[it - 1] }
    val nextEpisode = activeEpisodeIndex
        .takeIf { it >= 0 && it + 1 < sortedEpisodes.size }
        ?.let { sortedEpisodes[it + 1] }

    fun switchPlayerEpisode(episode: MediaEpisode) {
        val currentStream = playingStream
        episode.season
            ?.takeIf { it in seasons }
            ?.let { selectedSeason = it }
        lastFocusedEpisodeKey = episodeFocusKey(episode)
        pendingEpisodeFocusRestoreKey = episodeFocusKey(episode)
        playingStream = null
        openSources(
            episode = episode,
            autoPlay = true,
            preferredProviderId = currentStream?.providerId.orEmpty(),
            preferredProviderName = currentStream?.providerName.orEmpty()
        )
    }

    BackHandler(enabled = sourcePreview == null) {
        onBack()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(TvColors.Background)
    ) {
        TvDetailsBackdrop(
            item = detailedItem,
            modifier = Modifier.fillMaxSize()
        )

        LazyColumn(
            state = detailsListState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 60.dp,
                end = 54.dp,
                bottom = 44.dp
            ),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            item(key = "hero") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(500.dp)
                        .padding(
                            end = 270.dp,
                            bottom = 26.dp
                        ),
                    verticalArrangement = Arrangement.Bottom
                ) {
                    val shouldShowLogo =
                        detailedItem.logoUrl.isNotBlank() && !logoLoadFailed

                    if (shouldShowLogo) {
                        AsyncImage(
                            model = detailedItem.logoUrl,
                            contentDescription = detailedItem.title,
                            onError = { logoLoadFailed = true },
                            modifier = Modifier
                                .width(360.dp)
                                .height(92.dp),
                            contentScale = ContentScale.Fit,
                            alignment = Alignment.CenterStart
                        )
                        Spacer(Modifier.height(12.dp))
                    } else {
                        Text(
                            text = detailedItem.title,
                            color = TvColors.TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 38.sp,
                            lineHeight = 43.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(12.dp))
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TvDetailsActionButton(
                            label = resumeActionLabel,
                            focusRequester = firstActionRequester,
                            onFocused = { lastDetailsRequester = it },
                            primary = true,
                            onLongPress = {
                                if (!loading) {
                                    openSources(
                                        episode = heroTargetEpisode,
                                        autoPlay = false
                                    )
                                }
                            },
                            onClick = {
                                if (!loading) {
                                    openSources(
                                        episode = heroTargetEpisode,
                                        autoPlay = true
                                    )
                                }
                            }
                        )

                        TvDetailsActionButton(
                            label = "Play manually",
                            icon = Icons.Outlined.PlayArrow,
                            focusRequester = manualPlayRequester,
                            onFocused = { lastDetailsRequester = it },
                            onClick = {
                                if (!loading) {
                                    openSources(
                                        episode = heroTargetEpisode,
                                        autoPlay = false
                                    )
                                }
                            }
                        )

                        TvDetailsActionButton(
                            label = if (savedToLibrary) "In Library" else "Library",
                            icon = if (savedToLibrary) {
                                Icons.Outlined.BookmarkRemove
                            } else {
                                Icons.Outlined.BookmarkAdd
                            },
                            focusRequester = libraryActionRequester,
                            onFocused = { lastDetailsRequester = it },
                            onClick = {
                                val nextSaved = !savedToLibrary
                                mediaStore.setSaved(
                                    entry.sourceUrl,
                                    detailedItem,
                                    nextSaved
                                )
                                savedToLibrary = nextSaved
                            }
                        )

                        if (!isSeries) {
                            TvDetailsActionButton(
                                label = if (movieWatched) "Watched" else "Mark watched",
                                icon = null,
                                focusRequester = watchedActionRequester,
                                onFocused = { lastDetailsRequester = it },
                                onClick = {
                                    val nextWatched = !movieWatched
                                    if (nextWatched) {
                                        mediaStore.recordProgress(
                                            sourceUrl = entry.sourceUrl,
                                            item = detailedItem,
                                            episode = null,
                                            positionMs = 0L,
                                            durationMs = 0L,
                                            completed = true
                                        )
                                    } else {
                                        mediaStore.setWatched(
                                            sourceUrl = entry.sourceUrl,
                                            item = detailedItem,
                                            watched = false
                                        )
                                    }
                                    movieWatched = nextWatched
                                }
                            )
                        }
                    }

                    resumeContextLabel?.let {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = it,
                            color = TvColors.TextSecondary,
                            fontWeight = FontWeight.Medium,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(Modifier.height(15.dp))

                    creditLine?.let {
                        Text(
                            text = it,
                            color = TvColors.TextSecondary,
                            fontWeight = FontWeight.Medium,
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(8.dp))
                    }

                    detailedItem.description
                        .takeIf { it.isNotBlank() }
                        ?.let {
                            Text(
                                text = it,
                                color = TvColors.TextPrimary.copy(alpha = 0.82f),
                                fontSize = 14.sp,
                                lineHeight = 19.sp,
                                maxLines = 6,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(Modifier.height(10.dp))
                        }

                    if (metaLabels.isNotEmpty()) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            metaLabels.take(5).forEach { label ->
                                TvMetaPill(label)
                            }
                        }
                    }

                    if (genreLine.isNotBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = genreLine,
                            color = TvColors.TextSecondary.copy(alpha = 0.92f),
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (errorMessage != null) {
                        Spacer(Modifier.height(10.dp))
                        Surface(
                            color = TvColors.BackgroundElevated.copy(alpha = 0.92f),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(
                                1.dp,
                                Color.White.copy(alpha = 0.10f)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(
                                    horizontal = 12.dp,
                                    vertical = 8.dp
                                ),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Info,
                                    contentDescription = null,
                                    tint = TvColors.TextSecondary,
                                    modifier = Modifier.size(17.dp)
                                )
                                Text(
                                    text = errorMessage.orEmpty(),
                                    color = TvColors.TextSecondary,
                                    fontSize = 12.sp,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            if (seasons.isNotEmpty()) {
                item(key = "seasons") {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Seasons",
                            color = TvColors.TextPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 19.sp
                        )

                        LazyRow(
                            state = seasonListState,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(end = 18.dp)
                        ) {
                            itemsIndexed(
                                items = seasons,
                                key = { _, season -> season }
                            ) { _, season ->
                                val seasonEpisodes = sortedEpisodes.filter { it.season == season }
                                val seasonFullyWatched = seasonEpisodes.isNotEmpty() &&
                                    seasonEpisodes.all { episode ->
                                        episodeHasWatchedMark(episode, watchedEpisodeKeys)
                                    }

                                TvSeasonChip(
                                    season = season,
                                    selected = season == selectedSeason,
                                    fullyWatched = seasonFullyWatched,
                                    onFocused = { requester ->
                                        lastDetailsRequester = requester
                                        if (season != selectedSeason) {
                                            pendingSeasonSelection = season
                                        }
                                    },
                                    onClick = {
                                        pendingSeasonSelection = null
                                        selectedSeason = season
                                        lastFocusedEpisodeKey = null
                                        pendingEpisodeFocusRestoreKey = null
                                    },
                                    onLongPress = {
                                        pendingSeasonSelection = null
                                        selectedSeason = season
                                        seasonOptions = season
                                    }
                                )
                            }
                        }
                    }
                }
            }

            if (sortedEpisodes.isNotEmpty()) {
                item(key = "episodes_$selectedSeason") {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = when {
                                selectedSeason == null -> "Episodes"
                                selectedSeason == 0 && selectedSeasonEpisodes.isEmpty() -> "Episodes · Specials"
                                selectedSeason == 0 -> {
                                    "Episodes · Specials · " +
                                        "$selectedSeasonWatchedCount/${selectedSeasonEpisodes.size} watched"
                                }
                                selectedSeasonEpisodes.isEmpty() -> "Episodes · Season $selectedSeason"
                                else -> {
                                    "Episodes · Season $selectedSeason · " +
                                        "$selectedSeasonWatchedCount/${selectedSeasonEpisodes.size} watched"
                                }
                            },
                            color = TvColors.TextPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 19.sp
                        )

                        LazyRow(
                            state = episodeListState,
                            modifier = Modifier
                                .fillMaxWidth()
                                .onPreviewKeyEvent { event ->
                                    val native = event.nativeKeyEvent
                                    val horizontal =
                                        native.keyCode == AndroidKeyEvent.KEYCODE_DPAD_LEFT ||
                                            native.keyCode == AndroidKeyEvent.KEYCODE_DPAD_RIGHT
                                    if (
                                        horizontal &&
                                        native.action == AndroidKeyEvent.ACTION_DOWN &&
                                        native.repeatCount > 0
                                    ) {
                                        val now = System.currentTimeMillis()
                                        if (
                                            now - lastEpisodeRepeatAt <
                                            EPISODE_SCROLL_REPEAT_THROTTLE_MS
                                        ) {
                                            true
                                        } else {
                                            lastEpisodeRepeatAt = now
                                            false
                                        }
                                    } else {
                                        false
                                    }
                                },
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(
                                end = 32.dp,
                                bottom = 10.dp
                            )
                        ) {
                            itemsIndexed(
                                items = visibleEpisodes,
                                key = { _, episode ->
                                    episodeFocusKey(episode)
                                }
                            ) { _, episode ->
                                val focusKey = episodeFocusKey(episode)
                                val episodeRequester = remember(focusKey) {
                                    FocusRequester().also { requester ->
                                        episodeFocusRequesters[focusKey] = requester
                                    }
                                }
                                val isPlaybackEpisode = localPlaybackEntry?.let { stored ->
                                    stored.episodeId == episode.id
                                } == true
                                val progressFraction = if (
                                    isPlaybackEpisode &&
                                    localPlaybackEntry?.hasProgress == true
                                ) {
                                    localPlaybackEntry.progressFraction
                                } else {
                                    0f
                                }
                                val episodeWatched =
                                    episodeHasWatchedMark(episode, watchedEpisodeKeys)
                                val statusLabel = when {
                                    isPlaybackEpisode &&
                                        localPlaybackEntry?.nextUp == true -> {
                                        "Next up"
                                    }

                                    progressFraction > 0f -> {
                                        "Resume · ${(progressFraction * 100f).toInt()}%"
                                    }

                                    episodeWatched -> "Watched"
                                    else -> null
                                }

                                TvEpisodeCard(
                                    episode = episode,
                                    focusRequester = episodeRequester,
                                    progressFraction = progressFraction,
                                    statusLabel = statusLabel,
                                    watched = episodeWatched,
                                    onFocused = { requester ->
                                        lastDetailsRequester = requester
                                        lastFocusedEpisodeKey = focusKey
                                    },
                                    onClick = {
                                        lastFocusedEpisodeKey = focusKey
                                        pendingEpisodeFocusRestoreKey = focusKey
                                        openSources(
                                            episode = episode,
                                            autoPlay = true
                                        )
                                    },
                                    onLongPress = {
                                        lastFocusedEpisodeKey = focusKey
                                        pendingEpisodeFocusRestoreKey = focusKey
                                        episodeOptions = episode
                                    }
                                )
                            }
                        }
                    }
                }
            }

            if (detailedItem.cast.isNotEmpty()) {
                item(key = "cast") {
                    TvCastSection(
                        cast = detailedItem.cast
                    )
                }
            }

            item(key = "details_bottom_space") {
                Spacer(Modifier.height(26.dp))
            }
        }

        if (loading) {
            Surface(
                modifier = Modifier.align(Alignment.Center),
                color = TvColors.BackgroundElevated.copy(alpha = 0.96f),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(
                    1.dp,
                    Color.White.copy(alpha = 0.10f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(
                        horizontal = 20.dp,
                        vertical = 14.dp
                    ),
                    horizontalArrangement = Arrangement.spacedBy(11.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(21.dp),
                        strokeWidth = 2.dp
                    )
                    Text(
                        text = "Loading details...",
                        color = TvColors.TextSecondary,
                        fontSize = 14.sp
                    )
                }
            }
        }

        episodeOptions?.let { episode ->
            val watched = episodeHasWatchedMark(episode, watchedEpisodeKeys)
            val progress = mediaStore.resumePosition(
                sourceUrl = entry.sourceUrl,
                item = detailedItem,
                episode = episode
            )
            val episodeSeason = episode.season
            val seasonEpisodes = episodeSeason?.let { season ->
                sortedEpisodes.filter { it.season == season }
            }.orEmpty()
            val seasonFullyWatched = seasonEpisodes.isNotEmpty() &&
                seasonEpisodes.all { candidate ->
                    episodeHasWatchedMark(candidate, watchedEpisodeKeys)
                }
            val hasPreviousEpisodes = episode.episode?.let { episodeNumber ->
                seasonEpisodes.any { candidate ->
                    candidate.episode != null &&
                        candidate.episode < episodeNumber
                }
            } == true

            TvEpisodeOptionsOverlay(
                episode = episode,
                watched = watched,
                hasProgress = progress > 0L,
                seasonFullyWatched = seasonFullyWatched,
                hasPreviousEpisodes = hasPreviousEpisodes,
                onDismiss = {
                    episodeOptions = null
                    restoreSourceFocus = true
                },
                onPlay = {
                    episodeOptions = null
                    openSources(
                        episode = episode,
                        autoPlay = true
                    )
                },
                onManualPlay = {
                    episodeOptions = null
                    openSources(
                        episode = episode,
                        autoPlay = false
                    )
                },
                onStartFromBeginning = {
                    episodeOptions = null
                    openSources(
                        episode = episode,
                        autoPlay = true,
                        startFromBeginning = true
                    )
                },
                onToggleWatched = {
                    markEpisodeWatched(
                        episode = episode,
                        watched = !watched
                    )
                    episodeOptions = null
                    restoreSourceFocus = true
                },
                onToggleSeasonWatched = {
                    episodeSeason?.let { season ->
                        markSeasonWatched(
                            season = season,
                            watched = !seasonFullyWatched
                        )
                    }
                    episodeOptions = null
                    restoreSourceFocus = true
                },
                onMarkPreviousEpisodesWatched = {
                    markPreviousEpisodesWatched(episode)
                    episodeOptions = null
                    restoreSourceFocus = true
                },
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(25f)
            )
        }

        seasonOptions?.let { season ->
            val seasonEpisodes = sortedEpisodes.filter { it.season == season }
            val seasonFullyWatched = seasonEpisodes.isNotEmpty() &&
                seasonEpisodes.all { episode ->
                    episodeHasWatchedMark(episode, watchedEpisodeKeys)
                }
            val hasPreviousSeasons = seasons.any { candidate ->
                candidate > 0 && candidate < season
            }

            TvSeasonOptionsOverlay(
                season = season,
                fullyWatched = seasonFullyWatched,
                hasPreviousSeasons = hasPreviousSeasons,
                onDismiss = {
                    seasonOptions = null
                    restoreSourceFocus = true
                },
                onToggleSeasonWatched = {
                    markSeasonWatched(
                        season = season,
                        watched = !seasonFullyWatched
                    )
                    seasonOptions = null
                    restoreSourceFocus = true
                },
                onMarkPreviousSeasonsWatched = {
                    markPreviousSeasonsWatched(season)
                    seasonOptions = null
                    restoreSourceFocus = true
                },
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(25f)
            )
        }

        sourcePreview?.let { request ->
            TvSourcesStage4Modal(
                request = request,
                playerReturnToken = sourcePlayerReturnToken,
                onPlayStream = { stream ->
                    playingStream = stream
                },
                onClose = {
                    val returningEpisode = sourcePreview?.episode
                    returningEpisode
                        ?.season
                        ?.takeIf { it in seasons }
                        ?.let { selectedSeason = it }
                    pendingEpisodeFocusRestoreKey = returningEpisode
                        ?.let(::episodeFocusKey)
                        ?: lastFocusedEpisodeKey
                    sourcePreview = null
                    restoreSourceFocus = true
                },
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(30f)
            )
        }

        playingStream?.let { stream ->
            TvPlayerScreen(
                stream = stream,
                title = detailedItem.title,
                episodeTitle = sourcePreview?.episode?.displayTitle,
                previousEpisodeTitle = previousEpisode?.displayTitle,
                nextEpisodeTitle = nextEpisode?.displayTitle,
                onPreviousEpisode = previousEpisode?.let { episode ->
                    {
                        switchPlayerEpisode(episode)
                    }
                },
                onNextEpisode = nextEpisode?.let { episode ->
                    {
                        switchPlayerEpisode(episode)
                    }
                },
                startPositionMs = if (sourcePreview?.startFromBeginning == true) {
                    0L
                } else {
                    mediaStore.resumePosition(
                        sourceUrl = entry.sourceUrl,
                        item = detailedItem,
                        episode = sourcePreview?.episode
                    )
                },
                onProgress = { position, duration, completed ->
                    mediaStore.recordProgress(
                        sourceUrl = entry.sourceUrl,
                        item = detailedItem,
                        episode = sourcePreview?.episode,
                        positionMs = position,
                        durationMs = duration,
                        completed = completed,
                        nextEpisode = if (completed) nextEpisode else null
                    )
                    if (completed && nextEpisode == null) {
                        mediaStore.setWatched(
                            sourceUrl = entry.sourceUrl,
                            item = detailedItem,
                            watched = true
                        )
                    }
                },
                onBack = {
                    playingStream = null
                    sourcePlayerReturnToken += 1
                },
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(40f)
            )
        }
    }

    LaunchedEffect(
        sourcePreview,
        restoreSourceFocus,
        selectedSeason,
        sourcePlayerReturnToken
    ) {
        if (sourcePreview == null && restoreSourceFocus) {
            val restoreKey = pendingEpisodeFocusRestoreKey
                ?: lastFocusedEpisodeKey
            val restoreIndex = restoreKey?.let { key ->
                visibleEpisodes.indexOfFirst {
                    episodeFocusKey(it) == key
                }
            } ?: -1

            var focusRestored = false
            if (restoreIndex >= 0 && restoreKey != null) {
                episodeListState.scrollToItem(restoreIndex)
                delay(90)
                focusRestored = runCatching {
                    episodeFocusRequesters[restoreKey]?.let { requester ->
                        requester.requestFocus()
                        true
                    } ?: false
                }.getOrDefault(false)
            }

            if (!focusRestored) {
                delay(40)
                focusRestored = lastDetailsRequester?.let { requester ->
                    runCatching {
                        requester.requestFocus()
                        true
                    }.getOrDefault(false)
                } == true
            }

            if (!focusRestored) {
                runCatching { firstActionRequester.requestFocus() }
            }

            pendingEpisodeFocusRestoreKey = null
            restoreSourceFocus = false
        }
    }

    LaunchedEffect(entry.stableKey) {
        runCatching { detailsListState.scrollToItem(0) }
        delay(70)
        runCatching { firstActionRequester.requestFocus() }
    }
}

@Composable
private fun TvDetailsBackdrop(
    item: MediaItem,
    modifier: Modifier = Modifier
) {
    val image = item.backgroundUrl
        .takeIf { it.isNotBlank() }
        ?: item.posterUrl

    Box(modifier = modifier) {
        if (image.isNotBlank()) {
            AsyncImage(
                model = image,
                contentDescription = item.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        colorStops = arrayOf(
                            0.00f to TvColors.Background.copy(alpha = 0.99f),
                            0.54f to TvColors.Background.copy(alpha = 0.76f),
                            1.00f to TvColors.Background.copy(alpha = 0.36f)
                        )
                    )
                )
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0.00f to TvColors.Background.copy(alpha = 0.16f),
                            0.65f to TvColors.Background.copy(alpha = 0.42f),
                            1.00f to TvColors.Background
                        )
                    )
                )
        )
    }
}

@Composable
private fun TvDetailsPoster(
    item: MediaItem,
    modifier: Modifier = Modifier
) {
    val image = item.posterUrl
        .takeIf { it.isNotBlank() }
        ?: item.backgroundUrl

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = TvColors.Surface,
        border = BorderStroke(
            1.dp,
            Color.White.copy(alpha = 0.10f)
        )
    ) {
        if (image.isNotBlank()) {
            AsyncImage(
                model = image,
                contentDescription = item.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
    }
}

@Composable
private fun TvMetaPill(label: String) {
    Surface(
        color = TvColors.SurfaceVariant.copy(alpha = 0.82f),
        shape = RoundedCornerShape(7.dp),
        border = BorderStroke(
            1.dp,
            Color.White.copy(alpha = 0.08f)
        )
    ) {
        Text(
            text = label,
            color = TvColors.TextPrimary.copy(alpha = 0.84f),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(
                horizontal = 9.dp,
                vertical = 5.dp
            )
        )
    }
}

@Composable
private fun TvDetailsActionButton(
    label: String,
    icon: ImageVector? = Icons.Outlined.PlayArrow,
    focusRequester: FocusRequester,
    onFocused: (FocusRequester) -> Unit,
    primary: Boolean = false,
    onLongPress: (() -> Unit)? = null,
    onClick: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    var longPressTriggered by remember(label) { mutableStateOf(false) }

    val containerColor = when {
        primary -> Color.White
        focused -> TvColors.FocusRing
        else -> TvColors.SurfaceVariant
    }
    val contentColor = when {
        primary -> Color.Black
        focused -> TvColors.Background
        else -> TvColors.TextPrimary
    }

    Surface(
        modifier = Modifier
            .height(46.dp)
            .focusRequester(focusRequester)
            .onFocusChanged {
                focused = it.hasFocus
                if (it.hasFocus) onFocused(focusRequester)
            }
            .onPreviewKeyEvent { event ->
                val native = event.nativeKeyEvent
                val isSelect =
                    native.keyCode == AndroidKeyEvent.KEYCODE_DPAD_CENTER ||
                        native.keyCode == AndroidKeyEvent.KEYCODE_ENTER ||
                        native.keyCode == AndroidKeyEvent.KEYCODE_NUMPAD_ENTER

                when {
                    onLongPress != null &&
                        native.action == AndroidKeyEvent.ACTION_DOWN &&
                        native.keyCode == AndroidKeyEvent.KEYCODE_MENU -> {
                        longPressTriggered = true
                        onLongPress()
                        true
                    }

                    onLongPress != null &&
                        isSelect &&
                        native.action == AndroidKeyEvent.ACTION_DOWN &&
                        native.repeatCount >= 2 &&
                        !longPressTriggered -> {
                        longPressTriggered = true
                        onLongPress()
                        true
                    }

                    isSelect && native.action == AndroidKeyEvent.ACTION_DOWN -> true

                    isSelect && native.action == AndroidKeyEvent.ACTION_UP -> {
                        if (longPressTriggered) {
                            longPressTriggered = false
                        } else {
                            onClick()
                        }
                        true
                    }

                    else -> false
                }
            }
            .focusable(),
        color = containerColor,
        shape = RoundedCornerShape(23.dp),
        border = BorderStroke(
            if (focused) 2.dp else 1.dp,
            when {
                focused -> TvColors.FocusRing
                primary -> Color.White.copy(alpha = 0.92f)
                else -> Color.White.copy(alpha = 0.10f)
            }
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 17.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            icon?.let {
                Icon(
                    imageVector = it,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(19.dp)
                )
            }
            Text(
                text = label,
                color = contentColor,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp
            )
        }
    }
}


@Composable
private fun TvCastSection(
    cast: List<String>
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "Cast",
            color = TvColors.TextPrimary,
            fontWeight = FontWeight.SemiBold,
            fontSize = 19.sp
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(end = 28.dp)
        ) {
            itemsIndexed(
                items = cast.take(20),
                key = { index, name -> "$index|$name" }
            ) { _, name ->
                Surface(
                    modifier = Modifier
                        .width(156.dp)
                        .height(56.dp),
                    color = TvColors.SurfaceVariant.copy(alpha = 0.90f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(
                        1.dp,
                        Color.White.copy(alpha = 0.08f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            modifier = Modifier.size(32.dp),
                            color = TvColors.BackgroundElevated,
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(
                                1.dp,
                                Color.White.copy(alpha = 0.08f)
                            )
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = name
                                        .trim()
                                        .firstOrNull()
                                        ?.uppercase()
                                        .orEmpty(),
                                    color = TvColors.TextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }

                        Text(
                            text = name,
                            color = TvColors.TextPrimary.copy(alpha = 0.90f),
                            fontWeight = FontWeight.Medium,
                            fontSize = 12.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TvSeasonChip(
    season: Int,
    selected: Boolean,
    fullyWatched: Boolean,
    onFocused: (FocusRequester) -> Unit,
    onClick: () -> Unit,
    onLongPress: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    var longPressTriggered by remember(season) { mutableStateOf(false) }
    val requester = remember(season) { FocusRequester() }

    Surface(
        modifier = Modifier
            .height(38.dp)
            .focusRequester(requester)
            .onFocusChanged {
                focused = it.hasFocus
                if (it.hasFocus) onFocused(requester)
            }
            .onPreviewKeyEvent { event ->
                val native = event.nativeKeyEvent
                val isSelect = native.keyCode == AndroidKeyEvent.KEYCODE_DPAD_CENTER ||
                    native.keyCode == AndroidKeyEvent.KEYCODE_ENTER ||
                    native.keyCode == AndroidKeyEvent.KEYCODE_NUMPAD_ENTER

                when {
                    native.action == AndroidKeyEvent.ACTION_DOWN &&
                        native.keyCode == AndroidKeyEvent.KEYCODE_MENU -> {
                        longPressTriggered = true
                        onLongPress()
                        true
                    }

                    isSelect &&
                        native.action == AndroidKeyEvent.ACTION_DOWN &&
                        native.repeatCount >= 2 &&
                        !longPressTriggered -> {
                        longPressTriggered = true
                        onLongPress()
                        true
                    }

                    isSelect &&
                        native.action == AndroidKeyEvent.ACTION_DOWN -> true

                    isSelect &&
                        native.action == AndroidKeyEvent.ACTION_UP -> {
                        if (longPressTriggered) {
                            longPressTriggered = false
                        } else {
                            onClick()
                        }
                        true
                    }

                    else -> false
                }
            }
            .focusable(),
        color = when {
            focused -> TvColors.FocusRing
            selected -> TvColors.Accent.copy(alpha = 0.82f)
            else -> TvColors.SurfaceVariant
        },
        shape = RoundedCornerShape(9.dp),
        border = BorderStroke(
            1.dp,
            if (focused || selected) {
                Color.White.copy(alpha = 0.52f)
            } else {
                Color.White.copy(alpha = 0.08f)
            }
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (season == 0) "Specials" else "Season $season",
                color = if (focused) TvColors.Background else TvColors.TextPrimary,
                fontSize = 13.sp,
                fontWeight = if (focused || selected) {
                    FontWeight.SemiBold
                } else {
                    FontWeight.Normal
                }
            )
            if (fullyWatched) {
                Text(
                    text = "✓",
                    color = if (focused) TvColors.Background else TvColors.TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

private fun episodeWatchKey(episode: MediaEpisode): String =
    buildString {
        append(episode.id)
        append('|')
        append(episode.season ?: -1)
        append('|')
        append(episode.episode ?: -1)
    }

private fun episodeHasWatchedMark(
    episode: MediaEpisode,
    watchedKeys: Set<String>
): Boolean {
    val exact = episodeWatchKey(episode)
    if (exact in watchedKeys) return true
    val idPrefix = "${episode.id}|"
    return episode.id.isNotBlank() && watchedKeys.any { it.startsWith(idPrefix) }
}

private fun episodeFocusKey(episode: MediaEpisode): String =
    buildString {
        append(episode.id)
        append('|')
        append(episode.season ?: -1)
        append('|')
        append(episode.episode ?: -1)
    }

@Composable
private fun TvEpisodeCard(
    episode: MediaEpisode,
    focusRequester: FocusRequester,
    progressFraction: Float = 0f,
    statusLabel: String? = null,
    watched: Boolean = false,
    onFocused: (FocusRequester) -> Unit,
    onClick: () -> Unit,
    onLongPress: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    var longPressTriggered by remember(episodeFocusKey(episode)) {
        mutableStateOf(false)
    }
    val scale by animateFloatAsState(
        targetValue = if (focused) 1.035f else 1f,
        label = "episodeScale"
    )

    Surface(
        modifier = Modifier
            .width(278.dp)
            .height(154.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .focusRequester(focusRequester)
            .onFocusChanged {
                focused = it.hasFocus
                if (it.hasFocus) onFocused(focusRequester)
            }
            .onPreviewKeyEvent { event ->
                val native = event.nativeKeyEvent
                val isSelect = native.keyCode == AndroidKeyEvent.KEYCODE_DPAD_CENTER ||
                    native.keyCode == AndroidKeyEvent.KEYCODE_ENTER ||
                    native.keyCode == AndroidKeyEvent.KEYCODE_NUMPAD_ENTER

                when {
                    native.action == AndroidKeyEvent.ACTION_DOWN &&
                        native.keyCode == AndroidKeyEvent.KEYCODE_MENU -> {
                        longPressTriggered = true
                        onLongPress()
                        true
                    }

                    isSelect &&
                        native.action == AndroidKeyEvent.ACTION_DOWN &&
                        native.repeatCount >= 2 &&
                        !longPressTriggered -> {
                        longPressTriggered = true
                        onLongPress()
                        true
                    }

                    isSelect &&
                        native.action == AndroidKeyEvent.ACTION_DOWN -> {
                        true
                    }

                    isSelect &&
                        native.action == AndroidKeyEvent.ACTION_UP -> {
                        if (longPressTriggered) {
                            longPressTriggered = false
                        } else {
                            onClick()
                        }
                        true
                    }

                    else -> false
                }
            }
            .focusable(),
        color = TvColors.Surface,
        shape = RoundedCornerShape(11.dp),
        border = BorderStroke(
            if (focused) 2.dp else 1.dp,
            if (focused) {
                TvColors.FocusRing
            } else {
                Color.White.copy(alpha = 0.08f)
            }
        )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (episode.thumbnailUrl.isNotBlank()) {
                AsyncImage(
                    model = episode.thumbnailUrl,
                    contentDescription = episode.displayTitle,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Transparent,
                                Color.Transparent,
                                TvColors.Background.copy(alpha = 0.96f)
                            )
                        )
                    )
            )

            statusLabel?.let { label ->
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(9.dp),
                    color = Color.Black.copy(alpha = 0.72f),
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(
                        1.dp,
                        Color.White.copy(alpha = 0.12f)
                    )
                ) {
                    Text(
                        text = label,
                        color = TvColors.TextPrimary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(
                            horizontal = 8.dp,
                            vertical = 4.dp
                        )
                    )
                }
            }

            if (watched) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(9.dp),
                    color = Color.Black.copy(alpha = 0.72f),
                    shape = RoundedCornerShape(99.dp),
                    border = BorderStroke(
                        1.dp,
                        Color.White.copy(alpha = 0.14f)
                    )
                ) {
                    Text(
                        text = "✓",
                        color = TvColors.TextPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(
                            horizontal = 7.dp,
                            vertical = 3.dp
                        )
                    )
                }
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(
                        start = 11.dp,
                        end = 11.dp,
                        bottom = if (progressFraction > 0f) 15.dp else 11.dp
                    ),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                val episodeNumberLabel = when {
                    episode.season != null && episode.episode != null -> {
                        "S${episode.season} · E${episode.episode}"
                    }

                    episode.episode != null -> "Episode ${episode.episode}"
                    else -> ""
                }

                if (episodeNumberLabel.isNotBlank()) {
                    Text(
                        text = episodeNumberLabel,
                        color = TvColors.TextSecondary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1
                    )
                }

                Text(
                    text = episode.title.ifBlank { episode.displayTitle },
                    color = TvColors.TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                episode.overview
                    .takeIf { it.isNotBlank() }
                    ?.let {
                        Text(
                            text = it,
                            color = TvColors.TextSecondary,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
            }

            if (progressFraction > 0f) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .height(4.dp)
                        .background(Color.White.copy(alpha = 0.18f))
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth(progressFraction.coerceIn(0f, 1f))
                        .height(4.dp)
                        .background(TvColors.FocusRing)
                )
            }
        }
    }
}

@Composable
private fun TvEpisodeOptionsOverlay(
    episode: MediaEpisode,
    watched: Boolean,
    hasProgress: Boolean,
    seasonFullyWatched: Boolean,
    hasPreviousEpisodes: Boolean,
    onDismiss: () -> Unit,
    onPlay: () -> Unit,
    onManualPlay: () -> Unit,
    onStartFromBeginning: () -> Unit,
    onToggleWatched: () -> Unit,
    onToggleSeasonWatched: () -> Unit,
    onMarkPreviousEpisodesWatched: () -> Unit,
    modifier: Modifier = Modifier
) {
    val watchedRequester = remember(episodeFocusKey(episode), watched) { FocusRequester() }
    val seasonRequester = remember(
        episodeFocusKey(episode),
        seasonFullyWatched
    ) { FocusRequester() }
    val previousRequester = remember(episodeFocusKey(episode)) { FocusRequester() }
    val playRequester = remember(episodeFocusKey(episode), hasProgress) { FocusRequester() }
    val manualRequester = remember(episodeFocusKey(episode)) { FocusRequester() }
    val startRequester = remember(episodeFocusKey(episode), hasProgress) { FocusRequester() }

    BackHandler(onBack = onDismiss)

    Box(
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.64f)),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier.width(430.dp),
            color = TvColors.BackgroundElevated.copy(alpha = 0.98f),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(
                1.dp,
                Color.White.copy(alpha = 0.12f)
            )
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = episode.displayTitle,
                    color = TvColors.TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Episode options",
                    color = TvColors.TextSecondary,
                    fontSize = 12.sp
                )

                TvEpisodeOptionButton(
                    label = if (watched) "Mark as unwatched" else "Mark as watched",
                    focusRequester = watchedRequester,
                    onClick = onToggleWatched
                )
                TvEpisodeOptionButton(
                    label = if (seasonFullyWatched) {
                        "Mark season as unwatched"
                    } else {
                        "Mark season as watched"
                    },
                    focusRequester = seasonRequester,
                    onClick = onToggleSeasonWatched
                )
                if (hasPreviousEpisodes) {
                    TvEpisodeOptionButton(
                        label = "Mark previous episodes watched",
                        focusRequester = previousRequester,
                        onClick = onMarkPreviousEpisodesWatched
                    )
                }
                TvEpisodeOptionButton(
                    label = if (hasProgress) "Resume" else "Play",
                    focusRequester = playRequester,
                    onClick = onPlay
                )
                TvEpisodeOptionButton(
                    label = "Play manually",
                    focusRequester = manualRequester,
                    onClick = onManualPlay
                )
                if (hasProgress) {
                    TvEpisodeOptionButton(
                        label = "Start from beginning",
                        focusRequester = startRequester,
                        onClick = onStartFromBeginning
                    )
                }

                Text(
                    text = "Hold OK for options · Back to close",
                    color = TvColors.TextSecondary.copy(alpha = 0.78f),
                    fontSize = 11.sp
                )
            }
        }
    }

    LaunchedEffect(episodeFocusKey(episode), watched) {
        delay(80)
        runCatching { watchedRequester.requestFocus() }
    }
}

@Composable
private fun TvSeasonOptionsOverlay(
    season: Int,
    fullyWatched: Boolean,
    hasPreviousSeasons: Boolean,
    onDismiss: () -> Unit,
    onToggleSeasonWatched: () -> Unit,
    onMarkPreviousSeasonsWatched: () -> Unit,
    modifier: Modifier = Modifier
) {
    val primaryRequester = remember(season, fullyWatched) { FocusRequester() }
    val previousRequester = remember(season) { FocusRequester() }

    BackHandler(onBack = onDismiss)

    Box(
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.64f)),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier.width(410.dp),
            color = TvColors.BackgroundElevated.copy(alpha = 0.98f),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(
                1.dp,
                Color.White.copy(alpha = 0.12f)
            )
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = if (season == 0) "Specials" else "Season $season",
                    color = TvColors.TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
                Text(
                    text = "Season options",
                    color = TvColors.TextSecondary,
                    fontSize = 12.sp
                )

                TvEpisodeOptionButton(
                    label = if (fullyWatched) {
                        "Mark season as unwatched"
                    } else {
                        "Mark season as watched"
                    },
                    focusRequester = primaryRequester,
                    onClick = onToggleSeasonWatched
                )
                if (hasPreviousSeasons && season > 0) {
                    TvEpisodeOptionButton(
                        label = "Mark previous seasons watched",
                        focusRequester = previousRequester,
                        onClick = onMarkPreviousSeasonsWatched
                    )
                }

                Text(
                    text = "Hold OK on a season for options · Back to close",
                    color = TvColors.TextSecondary.copy(alpha = 0.78f),
                    fontSize = 11.sp
                )
            }
        }
    }

    LaunchedEffect(season, fullyWatched) {
        delay(80)
        runCatching { primaryRequester.requestFocus() }
    }
}

@Composable
private fun TvEpisodeOptionButton(
    label: String,
    focusRequester: FocusRequester,
    onClick: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(46.dp)
            .focusRequester(focusRequester)
            .onFocusChanged { focused = it.hasFocus }
            .onPreviewKeyEvent { event ->
                if (
                    event.type == KeyEventType.KeyDown &&
                    (
                        event.key == Key.DirectionCenter ||
                            event.key == Key.Enter
                        )
                ) {
                    onClick()
                    true
                } else {
                    false
                }
            }
            .focusable(),
        color = if (focused) TvColors.FocusRing else TvColors.SurfaceVariant,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(
            1.dp,
            if (focused) TvColors.FocusRing else Color.White.copy(alpha = 0.10f)
        )
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 16.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                text = label,
                color = if (focused) TvColors.Background else TvColors.TextPrimary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
private fun TvSourcesStage4Modal(
    request: TvSourcePreviewRequest,
    playerReturnToken: Int,
    onPlayStream: (StreamSource) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val aggregator = remember(context) { StreamSourceAggregator(context) }
    val subtitleAggregator = remember(context) {
        SubtitleSourceAggregator(context)
    }
    val requestKey = remember(
        request.sourceUrl,
        request.item.id,
        request.episode?.id,
        request.autoPlay,
        request.preferredProviderId,
        request.preferredProviderName
    ) {
        buildString {
            append(request.sourceUrl)
            append('|')
            append(request.item.id)
            append('|')
            append(request.episode?.id.orEmpty())
            append('|')
            append(request.autoPlay)
            append('|')
            append(request.preferredProviderId)
            append('|')
            append(request.preferredProviderName)
        }
    }

    var reloadToken by remember(requestKey) { mutableStateOf(0) }
    var loading by remember(requestKey) { mutableStateOf(true) }
    var errorMessage by remember(requestKey) { mutableStateOf<String?>(null) }
    var streams by remember(requestKey) { mutableStateOf<List<StreamSource>>(emptyList()) }
    var aggregatedSubtitles by remember(requestKey) {
        mutableStateOf<List<SubtitleSource>>(emptyList())
    }
    var subtitleScanComplete by remember(requestKey) { mutableStateOf(false) }
    var selectedStreamUrl by remember(requestKey) { mutableStateOf<String?>(null) }
    var scanStatus by remember(requestKey) {
        mutableStateOf("Preparing installed addons...")
    }
    var autoPlayConsumed by remember(requestKey) { mutableStateOf(false) }

    val modalRequester = remember(requestKey) { FocusRequester() }
    val retryRequester = remember(requestKey) { FocusRequester() }
    val streamRequesters = remember(streams.map { it.url }) {
        streams.map { FocusRequester() }
    }

    LaunchedEffect(requestKey, reloadToken) {
        loading = true
        errorMessage = null
        streams = emptyList()
        selectedStreamUrl = null
        scanStatus = "Checking installed addons..."

        aggregator.load(
            item = request.item,
            episode = request.episode,
            preferredSession = request.session,
            onProgress = { progress ->
                scanStatus = when (progress.phase) {
                    StreamAggregationPhase.DISCOVERING -> {
                        val total = progress.total.coerceAtLeast(1)
                        "Checking addons ${progress.current}/$total" +
                            progress.sourceName
                                .takeIf { it.isNotBlank() }
                                ?.let { " · ${it.take(30)}" }
                                .orEmpty()
                    }

                    StreamAggregationPhase.SCANNING -> {
                        val total = progress.total.coerceAtLeast(1)
                        "Scanning ${progress.current}/$total" +
                            progress.sourceName
                                .takeIf { it.isNotBlank() }
                                ?.let { " · ${it.take(30)}" }
                                .orEmpty() +
                            " · ${progress.foundStreams} found"
                    }
                }
            },
            onSuccess = { result ->
                val ranked = rankTvStreams(result.streams)
                streams = ranked
                loading = false

                scanStatus = when {
                    result.streamAddonCount == 0 -> {
                        "${result.enabledSourceCount} enabled sources · 0 stream addons"
                    }

                    ranked.isEmpty() -> {
                        "Scanned ${result.scannedStreamAddonCount}/${result.streamAddonCount} stream addons · 0 playable"
                    }

                    else -> {
                        "Scanned ${result.scannedStreamAddonCount}/${result.streamAddonCount} stream addons · ${ranked.size} playable"
                    }
                }

                errorMessage = when {
                    result.streamAddonCount == 0 -> {
                        "No stream addons are installed or enabled. " +
                            "Add a Stremio addon that supports the stream resource, then retry."
                    }

                    ranked.isEmpty() -> {
                        buildString {
                            append(
                                "All ${result.streamAddonCount} stream addons were scanned, " +
                                    "but none returned playable streams for this title."
                            )
                            if (
                                result.openFailureCount > 0 ||
                                result.loadFailureCount > 0
                            ) {
                                append(
                                    " Failures: open=${result.openFailureCount}, " +
                                        "load=${result.loadFailureCount}."
                                )
                            }
                        }
                    }

                    else -> null
                }

                if (selectedStreamUrl !in ranked.map { it.url }) {
                    selectedStreamUrl = null
                }
            }
        )
    }

    LaunchedEffect(requestKey, reloadToken) {
        aggregatedSubtitles = emptyList()
        subtitleScanComplete = false
        subtitleAggregator.load(
            item = request.item,
            episode = request.episode,
            preferredSession = request.session,
            onSuccess = { result ->
                aggregatedSubtitles = result.subtitles
                subtitleScanComplete = true
            }
        )
    }

    LaunchedEffect(
        requestKey,
        request.autoPlay,
        streams,
        aggregatedSubtitles,
        subtitleScanComplete
    ) {
        if (
            request.autoPlay &&
            streams.isNotEmpty() &&
            subtitleScanComplete &&
            !autoPlayConsumed
        ) {
            delay(150)
            if (!autoPlayConsumed) {
                val preferred = streams.firstOrNull { stream ->
                    request.preferredProviderId.isNotBlank() &&
                        stream.providerId == request.preferredProviderId
                } ?: streams.firstOrNull { stream ->
                    request.preferredProviderName.isNotBlank() &&
                        stream.providerName.equals(
                            request.preferredProviderName,
                            ignoreCase = true
                        )
                } ?: streams.first()

                autoPlayConsumed = true
                selectedStreamUrl = preferred.url

                val mergedSubtitles = (
                    preferred.subtitles + aggregatedSubtitles
                    )
                    .distinctBy { subtitle ->
                        "${subtitle.url}|${subtitle.language}|${subtitle.label}"
                    }

                onPlayStream(
                    preferred.copy(
                        subtitles = mergedSubtitles
                    )
                )
            }
        }
    }

    BackHandler(onBack = onClose)

    Box(
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.76f)),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .width(760.dp)
                .height(540.dp)
                .focusRequester(modalRequester)
                .focusable(),
            color = TvColors.BackgroundElevated,
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(
                1.dp,
                Color.White.copy(alpha = 0.14f)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        horizontal = 28.dp,
                        vertical = 24.dp
                    ),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text(
                            text = "Sources",
                            color = TvColors.TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 25.sp
                        )
                        Text(
                            text = request.episode?.displayTitle
                                ?: request.item.title,
                            color = TvColors.TextSecondary,
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (!loading && streams.isNotEmpty()) {
                        Text(
                            text = buildString {
                                append("${streams.size} playable")
                                if (aggregatedSubtitles.isNotEmpty()) {
                                    append(" · ${aggregatedSubtitles.size} subs")
                                }
                            },
                            color = TvColors.Accent,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                    }
                }

                Text(
                    text = scanStatus,
                    modifier = Modifier.fillMaxWidth(),
                    color = TvColors.TextSecondary,
                    fontSize = 10.sp,
                    lineHeight = 13.sp,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )

                when {
                    loading -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(380.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(32.dp),
                                    strokeWidth = 3.dp
                                )
                                Text(
                                    text = "Scanning installed stream addons...",
                                    color = TvColors.TextSecondary,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }

                    errorMessage != null -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(380.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                modifier = Modifier.width(520.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Text(
                                    text = "No sources ready",
                                    color = TvColors.TextPrimary,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 20.sp
                                )
                                Text(
                                    text = errorMessage.orEmpty(),
                                    color = TvColors.TextSecondary,
                                    fontSize = 14.sp,
                                    lineHeight = 19.sp
                                )
                                TvSourcesRetryButton(
                                    focusRequester = retryRequester,
                                    onRetry = {
                                        reloadToken += 1
                                    }
                                )
                            }
                        }
                    }

                    else -> {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(380.dp),
                            verticalArrangement = Arrangement.spacedBy(9.dp),
                            contentPadding = PaddingValues(
                                top = 2.dp,
                                bottom = 8.dp
                            )
                        ) {
                            itemsIndexed(
                                items = streams,
                                key = { _, stream ->
                                    "${stream.name}|${stream.url}"
                                }
                            ) { index, stream ->
                                TvStreamRow(
                                    stream = stream,
                                    index = index,
                                    selected = selectedStreamUrl == stream.url,
                                    focusRequester = streamRequesters[index],
                                    onSelected = {
                                        selectedStreamUrl = stream.url
                                        val mergedSubtitles = (
                                            stream.subtitles + aggregatedSubtitles
                                            )
                                            .distinctBy { subtitle ->
                                                "${subtitle.url}|${subtitle.language}|${subtitle.label}"
                                            }
                                        onPlayStream(
                                            stream.copy(
                                                subtitles = mergedSubtitles
                                            )
                                        )
                                    }
                                )
                            }
                        }

                        Text(
                            text = selectedStreamUrl
                                ?.let {
                                    "Source selected • Opening TV player"
                                }
                                ?: "Choose a source with OK • Back returns to Details",
                            color = if (selectedStreamUrl != null) {
                                TvColors.Accent
                            } else {
                                TvColors.TextSecondary
                            },
                            fontWeight = if (selectedStreamUrl != null) {
                                FontWeight.SemiBold
                            } else {
                                FontWeight.Normal
                            },
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }

    LaunchedEffect(requestKey) {
        delay(50)
        runCatching { modalRequester.requestFocus() }
    }

    LaunchedEffect(loading, errorMessage, streams.size) {
        delay(70)
        when {
            !loading && streams.isNotEmpty() -> {
                streamRequesters.firstOrNull()?.let { requester ->
                    runCatching { requester.requestFocus() }
                }
            }

            !loading && errorMessage != null -> {
                runCatching { retryRequester.requestFocus() }
            }
        }
    }

    LaunchedEffect(playerReturnToken) {
        if (
            playerReturnToken > 0 &&
            !loading &&
            streams.isNotEmpty()
        ) {
            delay(80)
            val selectedIndex = streams.indexOfFirst { stream ->
                stream.url == selectedStreamUrl
            }.takeIf { it >= 0 } ?: 0

            streamRequesters
                .getOrNull(selectedIndex)
                ?.let { requester ->
                    runCatching { requester.requestFocus() }
                }
        }
    }
}

@Composable
private fun TvSourcesRetryButton(
    focusRequester: FocusRequester,
    onRetry: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .width(180.dp)
            .height(48.dp)
            .focusRequester(focusRequester)
            .onFocusChanged { focused = it.hasFocus }
            .onPreviewKeyEvent { event ->
                if (
                    event.type == KeyEventType.KeyDown &&
                    (
                        event.key == Key.DirectionCenter ||
                            event.key == Key.Enter
                        )
                ) {
                    onRetry()
                    true
                } else {
                    false
                }
            }
            .focusable(),
        color = if (focused) {
            TvColors.FocusRing
        } else {
            TvColors.FocusBackground
        },
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(
            1.dp,
            Color.White.copy(alpha = if (focused) 0.35f else 0.12f)
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Retry",
                color = if (focused) {
                    TvColors.Background
                } else {
                    TvColors.TextPrimary
                },
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
private fun TvStreamRow(
    stream: StreamSource,
    index: Int,
    selected: Boolean,
    focusRequester: FocusRequester,
    onSelected: () -> Unit
) {
    var focused by remember(stream.url) { mutableStateOf(false) }
    val quality = tvStreamQualityLabel(stream)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .focusRequester(focusRequester)
            .onFocusChanged { focused = it.hasFocus }
            .onPreviewKeyEvent { event ->
                if (
                    event.type == KeyEventType.KeyDown &&
                    (
                        event.key == Key.DirectionCenter ||
                            event.key == Key.Enter
                        )
                ) {
                    onSelected()
                    true
                } else {
                    false
                }
            }
            .focusable(),
        color = when {
            focused -> TvColors.FocusRing
            selected -> TvColors.FocusBackground
            else -> TvColors.Surface
        },
        shape = RoundedCornerShape(13.dp),
        border = when {
            focused -> BorderStroke(
                1.dp,
                Color.White.copy(alpha = 0.42f)
            )

            selected -> BorderStroke(
                1.dp,
                TvColors.Accent.copy(alpha = 0.65f)
            )

            else -> BorderStroke(
                1.dp,
                Color.White.copy(alpha = 0.07f)
            )
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "${index + 1}",
                color = if (focused) {
                    TvColors.Background
                } else {
                    TvColors.TextSecondary
                },
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                modifier = Modifier.width(28.dp)
            )

            Column(
                modifier = Modifier.width(520.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = stream.name.ifBlank {
                        "Stream ${index + 1}"
                    },
                    color = if (focused) {
                        TvColors.Background
                    } else {
                        TvColors.TextPrimary
                    },
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = quality ?: "Auto quality",
                    color = if (focused) {
                        TvColors.Background.copy(alpha = 0.72f)
                    } else {
                        TvColors.TextSecondary
                    },
                    fontSize = 11.sp
                )
            }

            if (selected) {
                Text(
                    text = "SELECTED",
                    color = if (focused) {
                        TvColors.Background
                    } else {
                        TvColors.Accent
                    },
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp
                )
            }
        }
    }
}

private fun rankTvStreams(
    streams: List<StreamSource>
): List<StreamSource> {
    return streams
        .asSequence()
        .filter { stream ->
            stream.playable && stream.url.isNotBlank()
        }
        .distinctBy { stream ->
            stream.url
        }
        .sortedWith(
            compareByDescending<StreamSource> { stream ->
                tvStreamQualityScore(stream)
            }.thenBy { stream ->
                stream.name.lowercase()
            }
        )
        .toList()
}

private fun tvStreamQualityScore(
    stream: StreamSource
): Int {
    val value = "${stream.name} ${stream.url}".lowercase()

    return when {
        "2160" in value || "4k" in value -> 2160
        "1440" in value -> 1440
        "1080" in value -> 1080
        "720" in value -> 720
        "576" in value -> 576
        "480" in value -> 480
        "360" in value -> 360
        "240" in value -> 240
        else -> 0
    }
}

private fun tvStreamQualityLabel(
    stream: StreamSource
): String? {
    return when (val score = tvStreamQualityScore(stream)) {
        2160 -> "4K / 2160p"
        1440 -> "1440p"
        1080 -> "1080p"
        720 -> "720p"
        576 -> "576p"
        480 -> "480p"
        360 -> "360p"
        240 -> "240p"
        0 -> null
        else -> "${score}p"
    }
}
