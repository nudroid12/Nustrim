package app.nudroidlabs.nustrim.tv.player

import android.net.Uri
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.nudroidlabs.nustrim.core.model.MediaEpisode
import app.nudroidlabs.nustrim.core.model.StreamSource
import app.nudroidlabs.nustrim.core.model.SubtitleSource
import app.nudroidlabs.nustrim.core.source.StreamAggregationPhase
import app.nudroidlabs.nustrim.core.source.StreamSourceAggregator
import app.nudroidlabs.nustrim.core.source.SubtitleSourceAggregator
import app.nudroidlabs.nustrim.tv.sources.TvSourcePreviewRequest
import app.nudroidlabs.nustrim.tv.theme.TvColors
import coil3.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import kotlinx.coroutines.delay
import java.util.Locale

@Composable
internal fun TvPlayerSourcesPanel(
    request: TvSourcePreviewRequest,
    currentStream: StreamSource,
    onSelect: (StreamSource) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val aggregator = remember(context) { StreamSourceAggregator(context) }
    val subtitleAggregator = remember(context) { SubtitleSourceAggregator(context) }
    val requestKey = remember(
        request.sourceUrl,
        request.item.id,
        request.episode?.id,
        currentStream.url
    ) {
        "${request.sourceUrl}|${request.item.id}|${request.episode?.id.orEmpty()}|${currentStream.url}"
    }

    var reloadToken by remember(requestKey) { mutableIntStateOf(0) }
    var streams by remember(requestKey) { mutableStateOf<List<StreamSource>>(emptyList()) }
    var subtitles by remember(requestKey) { mutableStateOf<List<SubtitleSource>>(emptyList()) }
    var loading by remember(requestKey) { mutableStateOf(true) }
    var status by remember(requestKey) { mutableStateOf("Preparing sources...") }
    var error by remember(requestKey) { mutableStateOf<String?>(null) }
    val currentRequester = remember(requestKey) { FocusRequester() }
    val firstRequester = remember(requestKey) { FocusRequester() }
    val retryRequester = remember(requestKey) { FocusRequester() }
    val listState = rememberLazyListState()

    LaunchedEffect(requestKey, reloadToken) {
        val generation = reloadToken
        streams = emptyList()
        loading = true
        error = null
        status = "Checking installed stream add-ons..."

        aggregator.load(
            item = request.item,
            episode = request.episode,
            preferredSession = request.session,
            onProgress = { progress ->
                if (generation != reloadToken) return@load
                val total = progress.total.coerceAtLeast(1)
                status = when (progress.phase) {
                    StreamAggregationPhase.DISCOVERING ->
                        "Checking add-ons ${progress.current}/$total"
                    StreamAggregationPhase.SCANNING ->
                        "Scanning ${progress.current}/$total · ${progress.foundStreams} found"
                }
            },
            onSuccess = { result ->
                if (generation != reloadToken) return@load
                streams = rankPlayerStreams(result.streams)
                loading = false
                error = if (streams.isEmpty()) {
                    "No playable sources were returned."
                } else {
                    null
                }
                status = if (streams.isEmpty()) {
                    "No playable sources"
                } else {
                    "${streams.size} playable sources"
                }
            },
            onStreamsUpdated = { partial ->
                if (generation != reloadToken) return@load
                val ranked = rankPlayerStreams(partial)
                if (ranked.isNotEmpty()) streams = ranked
            }
        )
    }

    LaunchedEffect(requestKey, reloadToken) {
        val generation = reloadToken
        subtitles = emptyList()
        subtitleAggregator.load(
            item = request.item,
            episode = request.episode,
            preferredSession = request.session,
            onSuccess = { result ->
                if (generation == reloadToken) {
                    subtitles = result.subtitles
                }
            }
        )
    }

    LaunchedEffect(requestKey, reloadToken) {
        val generation = reloadToken
        delay(15_000)
        if (generation == reloadToken && loading && streams.isEmpty()) {
            loading = false
            error = "Source scan is taking too long."
            status = "Scan timed out"
        }
    }

    LaunchedEffect(streams, currentStream.url, error) {
        delay(80)
        val currentIndex = streams.indexOfFirst { it.url == currentStream.url }
        if (currentIndex >= 0) {
            listState.scrollToItem(currentIndex)
            delay(40)
            runCatching { currentRequester.requestFocus() }
        } else if (streams.isNotEmpty()) {
            runCatching { firstRequester.requestFocus() }
        } else if (error != null) {
            runCatching { retryRequester.requestFocus() }
        }
    }

    TvPlayerRightPanelFrame(
        title = "Sources",
        subtitle = buildString {
            request.episode?.displayTitle
                ?.takeIf { it.isNotBlank() }
                ?.let {
                    append(it)
                    append(" · ")
                }
            append(status)
        },
        onClose = onClose,
        modifier = modifier
    ) {
        when {
            streams.isNotEmpty() -> {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 28.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(
                        items = streams,
                        key = { _, stream -> stream.url }
                    ) { index, stream ->
                        val isCurrent = stream.url == currentStream.url
                        TvPlayerSourceRow(
                            stream = stream,
                            current = isCurrent,
                            focusRequester = when {
                                isCurrent -> currentRequester
                                index == 0 -> firstRequester
                                else -> null
                            },
                            onSelect = {
                                onSelect(stream.withPlayerSubtitles(subtitles))
                            },
                            onClose = onClose
                        )
                    }
                }
            }
            loading -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(14.dp))
                    Text(
                        text = status,
                        color = TvColors.TextSecondary,
                        fontSize = 13.sp
                    )
                }
            }
            else -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = error ?: "No sources available.",
                        color = TvColors.TextSecondary,
                        fontSize = 14.sp
                    )
                    Spacer(Modifier.height(16.dp))
                    TvPlayerTextAction(
                        label = "Retry scan",
                        focusRequester = retryRequester,
                        onClick = { reloadToken += 1 }
                    )
                }
            }
        }
    }
}

@Composable
private fun TvPlayerSourceRow(
    stream: StreamSource,
    current: Boolean,
    focusRequester: FocusRequester?,
    onSelect: () -> Unit,
    onClose: () -> Unit
) {
    var focused by remember(stream.url) { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (focused) 1.018f else 1f,
        label = "playerSourceRowScale"
    )
    val fallbackRequester = remember(stream.url) { FocusRequester() }
    val requester = focusRequester ?: fallbackRequester

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .focusRequester(requester)
            .onFocusChanged { focused = it.hasFocus }
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) {
                    false
                } else {
                    when (event.key) {
                        Key.DirectionLeft -> {
                            onClose()
                            true
                        }
                        Key.DirectionCenter,
                        Key.Enter -> {
                            onSelect()
                            true
                        }
                        else -> false
                    }
                }
            }
            .focusable(),
        shape = RoundedCornerShape(12.dp),
        color = if (focused) Color.White else Color.White.copy(alpha = 0.065f),
        border = BorderStroke(
            if (current) 1.dp else 0.dp,
            if (current) TvColors.Accent else Color.Transparent
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 15.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .background(
                        if (current) TvColors.Accent else Color.White.copy(alpha = 0.08f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (current) Icons.Default.Check else Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = if (current) Color.Black else if (focused) Color.Black else Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = playerProviderLabel(stream),
                    color = if (focused) Color.Black else Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = buildString {
                        append(playerQualityLabel(stream))
                        stream.type.takeIf { it.isNotBlank() && it != "auto" }?.let {
                            append(" · ")
                            append(it.uppercase(Locale.ROOT))
                        }
                        if (stream.subtitles.isNotEmpty()) {
                            append(" · ")
                            append(stream.subtitles.size)
                            append(" subs")
                        }
                    },
                    color = if (focused) {
                        Color.Black.copy(alpha = 0.62f)
                    } else {
                        Color.White.copy(alpha = 0.60f)
                    },
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
internal fun TvPlayerEpisodesPanel(
    episodes: List<MediaEpisode>,
    currentEpisode: MediaEpisode?,
    isWatched: (MediaEpisode) -> Boolean,
    onSelect: (MediaEpisode) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val seasons = remember(episodes) {
        episodes.mapNotNull { it.season }.distinct().sorted()
    }
    var selectedSeason by remember(currentEpisode?.season, seasons) {
        mutableStateOf(currentEpisode?.season ?: seasons.firstOrNull())
    }
    val visible = remember(episodes, selectedSeason) {
        if (selectedSeason == null) {
            episodes
        } else {
            episodes.filter { it.season == selectedSeason }
        }
    }
    val listState = rememberLazyListState()
    val currentRequester = remember(currentEpisode?.id, selectedSeason) { FocusRequester() }
    val firstRequester = remember(selectedSeason) { FocusRequester() }

    LaunchedEffect(selectedSeason, visible, currentEpisode?.id) {
        val currentIndex = visible.indexOfFirst {
            it.id == currentEpisode?.id &&
                it.season == currentEpisode.season &&
                it.episode == currentEpisode.episode
        }
        val index = currentIndex.coerceAtLeast(0)
        if (visible.isNotEmpty()) {
            listState.scrollToItem(index)
            delay(70)
            runCatching {
                if (currentIndex >= 0) currentRequester.requestFocus()
                else firstRequester.requestFocus()
            }
        }
    }

    TvPlayerRightPanelFrame(
        title = "Episodes",
        subtitle = currentEpisode?.displayTitle.orEmpty(),
        onClose = onClose,
        modifier = modifier
    ) {
        if (seasons.size > 1) {
            LazyRow(
                contentPadding = PaddingValues(bottom = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(seasons, key = { it }) { season ->
                    TvPlayerSeasonChip(
                        label = if (season == 0) "Specials" else "Season $season",
                        selected = season == selectedSeason,
                        onClick = { selectedSeason = season }
                    )
                }
            }
        }
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            itemsIndexed(
                items = visible,
                key = { _, episode -> "${episode.id}|${episode.season}|${episode.episode}" }
            ) { index, episode ->
                val current = episode.id == currentEpisode?.id &&
                    episode.season == currentEpisode?.season &&
                    episode.episode == currentEpisode?.episode
                TvPlayerEpisodeRow(
                    episode = episode,
                    current = current,
                    watched = isWatched(episode),
                    focusRequester = when {
                        current -> currentRequester
                        index == 0 -> firstRequester
                        else -> null
                    },
                    onSelect = {
                        if (current) onClose() else onSelect(episode)
                    },
                    onClose = onClose
                )
            }
        }
    }
}

@Composable
private fun TvPlayerSeasonChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    var focused by remember(label) { mutableStateOf(false) }
    Surface(
        modifier = Modifier
            .height(38.dp)
            .onFocusChanged { focused = it.hasFocus }
            .onPreviewKeyEvent { event ->
                if (
                    event.type == KeyEventType.KeyDown &&
                    (event.key == Key.DirectionCenter || event.key == Key.Enter)
                ) {
                    onClick()
                    true
                } else {
                    false
                }
            }
            .focusable(),
        shape = RoundedCornerShape(20.dp),
        color = when {
            focused -> Color.White
            selected -> TvColors.Accent.copy(alpha = 0.82f)
            else -> Color.White.copy(alpha = 0.08f)
        }
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 15.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                color = if (focused) Color.Black else Color.White,
                fontSize = 12.sp,
                fontWeight = if (selected || focused) FontWeight.SemiBold else FontWeight.Normal
            )
        }
    }
}

@Composable
private fun TvPlayerEpisodeRow(
    episode: MediaEpisode,
    current: Boolean,
    watched: Boolean,
    focusRequester: FocusRequester?,
    onSelect: () -> Unit,
    onClose: () -> Unit
) {
    var focused by remember(episode.id, episode.season, episode.episode) {
        mutableStateOf(false)
    }
    val fallbackRequester = remember(episode.id) { FocusRequester() }
    val requester = focusRequester ?: fallbackRequester
    val scale by animateFloatAsState(
        targetValue = if (focused) 1.015f else 1f,
        label = "playerEpisodeRowScale"
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(92.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .focusRequester(requester)
            .onFocusChanged { focused = it.hasFocus }
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) {
                    false
                } else {
                    when (event.key) {
                        Key.DirectionLeft -> {
                            onClose()
                            true
                        }
                        Key.DirectionCenter,
                        Key.Enter -> {
                            onSelect()
                            true
                        }
                        else -> false
                    }
                }
            }
            .focusable(),
        shape = RoundedCornerShape(12.dp),
        color = if (focused) Color.White else Color.White.copy(alpha = 0.06f),
        border = BorderStroke(
            if (current) 1.dp else 0.dp,
            if (current) TvColors.Accent else Color.Transparent
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                modifier = Modifier
                    .width(116.dp)
                    .height(66.dp),
                color = Color.Black.copy(alpha = 0.45f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    if (episode.thumbnailUrl.isNotBlank()) {
                        AsyncImage(
                            model = episode.thumbnailUrl,
                            contentDescription = episode.title,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                    if (current) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .size(28.dp)
                                .background(Color.Black.copy(alpha = 0.66f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = episode.displayTitle,
                    color = if (focused) Color.Black else Color.White,
                    fontSize = 13.sp,
                    fontWeight = if (current || focused) FontWeight.SemiBold else FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (watched) {
                    Text(
                        text = "Watched",
                        color = if (focused) Color.Black.copy(alpha = 0.6f) else TvColors.Accent,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
internal fun TvPlayerStreamInfoPanel(
    stream: StreamSource,
    videoWidth: Int,
    videoHeight: Int,
    audioLabel: String,
    subtitleLabel: String,
    playbackSpeed: Float,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infoRequester = remember(stream.url) { FocusRequester() }
    val host = remember(stream.url) {
        runCatching { Uri.parse(stream.url).host.orEmpty() }.getOrDefault("")
    }
    LaunchedEffect(stream.url) {
        delay(80)
        runCatching { infoRequester.requestFocus() }
    }
    TvPlayerRightPanelFrame(
        title = "Stream info",
        subtitle = playerProviderLabel(stream),
        onClose = onClose,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .focusRequester(infoRequester)
                .onPreviewKeyEvent { event ->
                    if (
                        event.type == KeyEventType.KeyDown &&
                        event.key == Key.DirectionLeft
                    ) {
                        onClose()
                        true
                    } else {
                        false
                    }
                }
                .focusable(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            TvPlayerInfoLine("Quality", when {
                videoWidth > 0 && videoHeight > 0 -> "${videoWidth} × $videoHeight"
                else -> playerQualityLabel(stream)
            })
            TvPlayerInfoLine("Type", stream.type.ifBlank { "Auto" }.uppercase(Locale.ROOT))
            TvPlayerInfoLine("Audio", audioLabel)
            TvPlayerInfoLine("Subtitles", subtitleLabel)
            TvPlayerInfoLine("Playback speed", "${playbackSpeed}×")
            if (host.isNotBlank()) TvPlayerInfoLine("Host", host)
            stream.note.takeIf { it.isNotBlank() }?.let {
                TvPlayerInfoLine("Source note", it)
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Back or Left to close",
                color = Color.White.copy(alpha = 0.54f),
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun TvPlayerInfoLine(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.52f),
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = value,
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
internal fun TvPlayerStillWatchingOverlay(
    title: String,
    nextEpisodeTitle: String?,
    onContinue: () -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier
) {
    val continueRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        delay(100)
        runCatching { continueRequester.requestFocus() }
    }
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color.Black.copy(alpha = 0.30f),
                        Color.Black.copy(alpha = 0.78f),
                        Color.Black.copy(alpha = 0.94f)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier.width(560.dp),
            shape = RoundedCornerShape(20.dp),
            color = Color.Black.copy(alpha = 0.86f),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 34.dp, vertical = 30.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Still watching?",
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = nextEpisodeTitle
                        ?.takeIf { it.isNotBlank() }
                        ?.let { "Continue $title with $it?" }
                        ?: "Continue watching $title?",
                    color = Color.White.copy(alpha = 0.68f),
                    fontSize = 14.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TvPlayerTextAction(
                        label = "Continue",
                        focusRequester = continueRequester,
                        onClick = onContinue
                    )
                    TvPlayerTextAction(
                        label = "Stop",
                        onClick = onStop
                    )
                }
            }
        }
    }
}

@Composable
private fun TvPlayerRightPanelFrame(
    title: String,
    subtitle: String,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.32f))
        )
        Surface(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .width(500.dp),
            color = Color.Black.copy(alpha = 0.94f),
            shape = RoundedCornerShape(topStart = 22.dp, bottomStart = 22.dp),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.07f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 26.dp, end = 24.dp, top = 28.dp, bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 25.sp,
                    fontWeight = FontWeight.SemiBold
                )
                subtitle.takeIf { it.isNotBlank() }?.let {
                    Text(
                        text = it,
                        color = Color.White.copy(alpha = 0.58f),
                        fontSize = 11.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = "Left or Back to close",
                    color = Color.White.copy(alpha = 0.42f),
                    fontSize = 10.sp
                )
                Spacer(Modifier.height(6.dp))
                content()
            }
        }
    }
}

@Composable
private fun TvPlayerTextAction(
    label: String,
    focusRequester: FocusRequester? = null,
    onClick: () -> Unit
) {
    val fallbackRequester = remember(label) { FocusRequester() }
    val requester = focusRequester ?: fallbackRequester
    var focused by remember(label) { mutableStateOf(false) }
    Surface(
        modifier = Modifier
            .height(44.dp)
            .focusRequester(requester)
            .onFocusChanged { focused = it.hasFocus }
            .onPreviewKeyEvent { event ->
                if (
                    event.type == KeyEventType.KeyDown &&
                    (event.key == Key.DirectionCenter || event.key == Key.Enter)
                ) {
                    onClick()
                    true
                } else {
                    false
                }
            }
            .focusable(),
        shape = RoundedCornerShape(22.dp),
        color = if (focused) Color.White else Color.White.copy(alpha = 0.10f)
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 20.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                color = if (focused) Color.Black else Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

private fun StreamSource.withPlayerSubtitles(extra: List<SubtitleSource>): StreamSource {
    if (extra.isEmpty()) return this
    val merged = (subtitles + extra).distinctBy { subtitle ->
        "${subtitle.url}|${subtitle.language}|${subtitle.label}"
    }
    return copy(subtitles = merged)
}

private fun rankPlayerStreams(streams: List<StreamSource>): List<StreamSource> =
    streams
        .asSequence()
        .filter { it.playable && it.url.isNotBlank() }
        .distinctBy { it.url }
        .sortedWith(
            compareByDescending<StreamSource> { playerQualityScore(it) }
                .thenBy { playerProviderLabel(it).lowercase(Locale.ROOT) }
                .thenBy { it.name.lowercase(Locale.ROOT) }
        )
        .toList()

private fun playerProviderLabel(stream: StreamSource): String =
    stream.providerName.trim().ifBlank {
        stream.providerId.trim().ifBlank {
            stream.name.trim().substringBefore(" · ").ifBlank { "Source" }
        }
    }

private fun playerQualityScore(stream: StreamSource): Int {
    val haystack = "${stream.name} ${stream.note}".lowercase(Locale.ROOT)
    return when {
        Regex("""\b(2160|4k|uhd)\b""").containsMatchIn(haystack) -> 2160
        Regex("""\b1440\b""").containsMatchIn(haystack) -> 1440
        Regex("""\b1080\b""").containsMatchIn(haystack) -> 1080
        Regex("""\b720\b""").containsMatchIn(haystack) -> 720
        Regex("""\b480\b""").containsMatchIn(haystack) -> 480
        Regex("""\b360\b""").containsMatchIn(haystack) -> 360
        else -> 0
    }
}

private fun playerQualityLabel(stream: StreamSource): String = when (playerQualityScore(stream)) {
    2160 -> "4K"
    1440 -> "1440p"
    1080 -> "1080p"
    720 -> "720p"
    480 -> "480p"
    360 -> "360p"
    else -> "Auto"
}
