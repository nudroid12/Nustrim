package app.nudroidlabs.nustrim.tv.details

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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
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
import app.nudroidlabs.nustrim.core.model.MediaEpisode
import app.nudroidlabs.nustrim.core.model.MediaItem
import app.nudroidlabs.nustrim.core.model.MediaType
import app.nudroidlabs.nustrim.core.model.StreamSource
import app.nudroidlabs.nustrim.core.source.SourceEngine
import app.nudroidlabs.nustrim.core.source.SourceSession
import app.nudroidlabs.nustrim.tv.home.TvHomeEntry
import app.nudroidlabs.nustrim.tv.player.TvPlayerScreen
import app.nudroidlabs.nustrim.tv.theme.TvColors
import coil3.compose.AsyncImage
import kotlinx.coroutines.delay

data class TvSourcePreviewRequest(
    val sourceUrl: String,
    val session: SourceSession?,
    val item: MediaItem,
    val episode: MediaEpisode?
)

@Composable
fun TvDetailsScreen(
    entry: TvHomeEntry,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val engine = remember(context) { SourceEngine(context) }
    val firstActionRequester = remember { FocusRequester() }

    var resolvedSession by remember(entry.stableKey) {
        mutableStateOf<SourceSession?>(entry.session)
    }
    var detailedItem by remember(entry.stableKey) {
        mutableStateOf(entry.item)
    }
    var loading by remember(entry.stableKey) { mutableStateOf(true) }
    var errorMessage by remember(entry.stableKey) { mutableStateOf<String?>(null) }
    var selectedSeason by remember(entry.stableKey) { mutableStateOf<Int?>(null) }
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
    var lastDetailsRequester by remember(entry.stableKey) {
        mutableStateOf<FocusRequester?>(null)
    }

    fun loadWith(session: SourceSession) {
        resolvedSession = session
        session.loadDetails(
            item = entry.item,
            onSuccess = { loaded ->
                detailedItem = loaded
                loading = false
                errorMessage = null
            },
            onError = { error ->
                loading = false
                errorMessage = error.message ?: "Could not load full details."
            }
        )
    }

    LaunchedEffect(entry.stableKey) {
        loading = true
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
                    loading = false
                    errorMessage = error.message ?: "Could not open source."
                }
            )
        } else {
            loading = false
            errorMessage = "Source information is unavailable."
        }
    }

    val sortedEpisodes = detailedItem.episodes.sortedWith(
        compareBy<MediaEpisode>(
            { it.season ?: Int.MAX_VALUE },
            { it.episode ?: Int.MAX_VALUE },
            { it.title }
        )
    )
    val rawSeasons = sortedEpisodes.mapNotNull { it.season }.distinct().sorted()
    val positiveSeasons = rawSeasons.filter { it > 0 }
    val seasons = if (positiveSeasons.isNotEmpty()) positiveSeasons else rawSeasons

    LaunchedEffect(detailedItem.id, seasons) {
        if (selectedSeason == null && seasons.isNotEmpty()) {
            selectedSeason = seasons.first()
        }
    }

    val visibleEpisodes = if (selectedSeason == null) {
        sortedEpisodes
    } else {
        sortedEpisodes.filter { it.season == selectedSeason }
    }
    val primaryEpisode = visibleEpisodes.firstOrNull() ?: sortedEpisodes.firstOrNull()
    val isSeries = detailedItem.type == MediaType.SERIES || sortedEpisodes.isNotEmpty()

    fun openSources(episode: MediaEpisode?) {
        restoreSourceFocus = false
        sourcePreview = TvSourcePreviewRequest(
            sourceUrl = entry.sourceUrl,
            session = resolvedSession,
            item = detailedItem,
            episode = episode
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

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = 60.dp,
                    end = 54.dp,
                    top = 46.dp,
                    bottom = 38.dp
                ),
            horizontalArrangement = Arrangement.spacedBy(34.dp),
            verticalAlignment = Alignment.Top
        ) {
            TvDetailsPoster(
                item = detailedItem,
                modifier = Modifier
                    .width(188.dp)
                    .height(282.dp)
            )

            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(760.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = detailedItem.title,
                    color = TvColors.TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 34.sp,
                    lineHeight = 39.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    detailedItem.releaseInfo
                        .takeIf { it.isNotBlank() }
                        ?.let {
                            TvMetaPill(it)
                        }

                    TvMetaPill(
                        when {
                            isSeries -> "Series"
                            detailedItem.type == MediaType.MOVIE -> "Movie"
                            else -> detailedItem.type.name.lowercase()
                                .replaceFirstChar { it.uppercase() }
                        }
                    )

                    if (sortedEpisodes.isNotEmpty()) {
                        TvMetaPill("${sortedEpisodes.size} episodes")
                    }
                }

                detailedItem.description
                    .takeIf { it.isNotBlank() }
                    ?.let {
                        Text(
                            text = it,
                            color = TvColors.TextPrimary.copy(alpha = 0.76f),
                            fontSize = 14.sp,
                            lineHeight = 19.sp,
                            maxLines = 4,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                if (errorMessage != null) {
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

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TvDetailsActionButton(
                        label = if (entry.continueEntry != null) "Resume" else "Play",
                        focusRequester = firstActionRequester,
                        onFocused = { lastDetailsRequester = it },
                        onClick = {
                            openSources(
                                if (isSeries) primaryEpisode else null
                            )
                        }
                    )
                }

                if (seasons.isNotEmpty()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "Seasons",
                        color = TvColors.TextPrimary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 18.sp
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(end = 18.dp)
                    ) {
                        itemsIndexed(
                            items = seasons,
                            key = { _, season -> season }
                        ) { _, season ->
                            TvSeasonChip(
                                season = season,
                                selected = season == selectedSeason,
                                onFocused = { lastDetailsRequester = it },
                                onClick = {
                                    selectedSeason = season
                                }
                            )
                        }
                    }
                }

                if (sortedEpisodes.isNotEmpty()) {
                    Text(
                        text = if (selectedSeason != null) {
                            "Episodes · Season $selectedSeason"
                        } else {
                            "Episodes"
                        },
                        color = TvColors.TextPrimary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 18.sp
                    )

                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(
                            end = 32.dp,
                            bottom = 10.dp
                        )
                    ) {
                        itemsIndexed(
                            items = visibleEpisodes,
                            key = { index, episode ->
                                "${episode.id}|${episode.season}|${episode.episode}|$index"
                            }
                        ) { _, episode ->
                            TvEpisodeCard(
                                episode = episode,
                                onFocused = { lastDetailsRequester = it },
                                onClick = {
                                    openSources(episode)
                                }
                            )
                        }
                    }
                }
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

        sourcePreview?.let { request ->
            TvSourcesStage4Modal(
                request = request,
                playerReturnToken = sourcePlayerReturnToken,
                onPlayStream = { stream ->
                    playingStream = stream
                },
                onClose = {
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

    LaunchedEffect(sourcePreview, restoreSourceFocus) {
        if (sourcePreview == null && restoreSourceFocus) {
            delay(70)
            val requester = lastDetailsRequester ?: firstActionRequester
            runCatching { requester.requestFocus() }
            restoreSourceFocus = false
        }
    }

    LaunchedEffect(entry.stableKey) {
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
    focusRequester: FocusRequester,
    onFocused: (FocusRequester) -> Unit,
    onClick: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .height(46.dp)
            .focusRequester(focusRequester)
            .onFocusChanged {
                focused = it.hasFocus
                if (it.hasFocus) onFocused(focusRequester)
            }
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
        color = if (focused) {
            TvColors.FocusRing
        } else {
            TvColors.SurfaceVariant
        },
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(
            1.dp,
            if (focused) {
                TvColors.FocusRing
            } else {
                Color.White.copy(alpha = 0.10f)
            }
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 17.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.PlayArrow,
                contentDescription = null,
                tint = if (focused) TvColors.Background else TvColors.TextPrimary,
                modifier = Modifier.size(19.dp)
            )
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
private fun TvSeasonChip(
    season: Int,
    selected: Boolean,
    onFocused: (FocusRequester) -> Unit,
    onClick: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }
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
        Box(
            modifier = Modifier.padding(horizontal = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Season $season",
                color = if (focused) TvColors.Background else TvColors.TextPrimary,
                fontSize = 13.sp,
                fontWeight = if (focused || selected) {
                    FontWeight.SemiBold
                } else {
                    FontWeight.Normal
                }
            )
        }
    }
}

@Composable
private fun TvEpisodeCard(
    episode: MediaEpisode,
    onFocused: (FocusRequester) -> Unit,
    onClick: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    val requester = remember(
        episode.id,
        episode.season,
        episode.episode
    ) { FocusRequester() }
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
            .focusRequester(requester)
            .onFocusChanged {
                focused = it.hasFocus
                if (it.hasFocus) onFocused(requester)
            }
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

            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(11.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = episode.displayTitle,
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
    val engine = remember(context) { SourceEngine(context) }
    val requestKey = remember(
        request.sourceUrl,
        request.item.id,
        request.episode?.id
    ) {
        "${request.sourceUrl}|${request.item.id}|${request.episode?.id.orEmpty()}"
    }

    var reloadToken by remember(requestKey) { mutableStateOf(0) }
    var loading by remember(requestKey) { mutableStateOf(true) }
    var errorMessage by remember(requestKey) { mutableStateOf<String?>(null) }
    var streams by remember(requestKey) { mutableStateOf<List<StreamSource>>(emptyList()) }
    var selectedStreamUrl by remember(requestKey) { mutableStateOf<String?>(null) }
    var sourceDiag by remember(requestKey) { mutableStateOf("WAITING") }

    val modalRequester = remember(requestKey) { FocusRequester() }
    val retryRequester = remember(requestKey) { FocusRequester() }
    val streamRequesters = remember(streams.map { it.url }) {
        streams.map { FocusRequester() }
    }

    fun acceptStreams(loaded: List<StreamSource>) {
        val ranked = rankTvStreams(loaded)
        val playableCount = loaded.count { stream ->
            stream.playable && stream.url.isNotBlank()
        }
        val sample = loaded
            .take(3)
            .joinToString(" | ") { stream ->
                "${stream.name.take(18)}:${stream.type}:play=${stream.playable}:url=${stream.url.isNotBlank()}"
            }
        sourceDiag = "SUCCESS raw=${loaded.size} playable=$playableCount ranked=${ranked.size}" +
            if (sample.isBlank()) "" else " • $sample"
        streams = ranked
        loading = false
        errorMessage = if (ranked.isEmpty()) {
            "No playable streams were returned by this source."
        } else {
            null
        }
        if (selectedStreamUrl !in ranked.map { it.url }) {
            selectedStreamUrl = null
        }
    }

    LaunchedEffect(requestKey, reloadToken) {
        loading = true
        errorMessage = null
        streams = emptyList()
        selectedStreamUrl = null
        sourceDiag = "START session=${request.session?.id ?: "none"} source=${request.sourceUrl.take(48)} item=${request.item.id.take(40)} ep=${request.episode?.id?.take(30) ?: "none"}"

        fun loadFrom(session: SourceSession) {
            sourceDiag = "LOAD session=${session.id} name=${session.displayName.take(28)} kind=${session.kind} item=${request.item.id.take(32)} ep=${request.episode?.id?.take(24) ?: "none"}"
            session.loadStreams(
                item = request.item,
                episode = request.episode,
                onSuccess = { loaded ->
                    acceptStreams(loaded)
                },
                onError = { error ->
                    loading = false
                    streams = emptyList()
                    val message = error.message
                        ?.takeIf { it.isNotBlank() }
                        ?: "Could not load streams."
                    errorMessage = message
                    sourceDiag = "LOAD_ERROR ${error::class.java.simpleName}: ${message.take(120)}"
                }
            )
        }

        val existing = request.session
        if (existing != null) {
            loadFrom(existing)
        } else if (request.sourceUrl.isNotBlank()) {
            engine.open(
                request.sourceUrl,
                onSuccess = { session ->
                    sourceDiag = "OPEN_OK session=${session.id} name=${session.displayName.take(28)} kind=${session.kind}"
                    loadFrom(session)
                },
                onError = { error ->
                    loading = false
                    streams = emptyList()
                    val message = error.message
                        ?.takeIf { it.isNotBlank() }
                        ?: "Could not open source."
                    errorMessage = message
                    sourceDiag = "OPEN_ERROR ${error::class.java.simpleName}: ${message.take(120)}"
                }
            )
        } else {
            loading = false
            streams = emptyList()
            errorMessage = "Source information is unavailable."
            sourceDiag = "NO_SOURCE session=null sourceUrl=blank"
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
                            text = "${streams.size} playable",
                            color = TvColors.Accent,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                    }
                }

                Text(
                    text = "DIAG • $sourceDiag",
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
                                    text = "Finding playable streams...",
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
                                        onPlayStream(stream)
                                    }
                                )
                            }
                        }

                        Text(
                            text = selectedStreamUrl
                                ?.let {
                                    "Source selected • Player connection comes in TV Stage 5"
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
