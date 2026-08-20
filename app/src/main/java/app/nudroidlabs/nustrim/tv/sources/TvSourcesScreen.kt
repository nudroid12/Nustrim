package app.nudroidlabs.nustrim.tv.sources

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Refresh
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.nudroidlabs.nustrim.core.model.MediaEpisode
import app.nudroidlabs.nustrim.core.model.MediaItem
import app.nudroidlabs.nustrim.core.model.StreamSource
import app.nudroidlabs.nustrim.core.model.SubtitleSource
import app.nudroidlabs.nustrim.core.source.SourceSession
import app.nudroidlabs.nustrim.core.source.StreamAggregationPhase
import app.nudroidlabs.nustrim.core.source.StreamSourceAggregator
import app.nudroidlabs.nustrim.core.source.SubtitleSourceAggregator
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

private enum class TvSourceQualityFilter(val label: String) {
    ALL("All quality"),
    UHD("4K"),
    FHD("1080p"),
    HD("720p"),
    OTHER("Other")
}

@Composable
fun TvSourcesScreen(
    request: TvSourcePreviewRequest,
    playerReturnToken: Int,
    onPlayStream: (StreamSource) -> Unit,
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

    var reloadToken by remember(requestKey) { mutableIntStateOf(0) }
    var loading by remember(requestKey) { mutableStateOf(true) }
    var errorMessage by remember(requestKey) { mutableStateOf<String?>(null) }
    var streams by remember(requestKey) { mutableStateOf<List<StreamSource>>(emptyList()) }
    var aggregatedSubtitles by remember(requestKey) {
        mutableStateOf<List<SubtitleSource>>(emptyList())
    }
    var subtitleScanComplete by remember(requestKey) { mutableStateOf(false) }
    var selectedStreamUrl by remember(requestKey) { mutableStateOf<String?>(null) }
    var scanStatus by remember(requestKey) { mutableStateOf("Preparing sources...") }
    var autoPlayConsumed by remember(requestKey) { mutableStateOf(false) }
    var selectedProvider by remember(requestKey) { mutableStateOf<String?>(null) }
    var qualityFilter by remember(requestKey) { mutableStateOf(TvSourceQualityFilter.ALL) }

    val streamListState = rememberLazyListState()
    val retryRequester = remember(requestKey) { FocusRequester() }
    val streamRequesters = remember(requestKey, streams.map { it.url }) {
        streams.associate { it.url to FocusRequester() }
    }

    val providers = remember(streams) {
        streams
            .map(::tvProviderLabel)
            .filter { it.isNotBlank() }
            .distinct()
            .sortedBy { it.lowercase() }
    }
    val filteredStreams = remember(streams, selectedProvider, qualityFilter) {
        streams.filter { stream ->
            val providerMatches = selectedProvider == null ||
                tvProviderLabel(stream) == selectedProvider
            val qualityMatches = when (qualityFilter) {
                TvSourceQualityFilter.ALL -> true
                TvSourceQualityFilter.UHD -> tvStreamQualityScore(stream) >= 2160
                TvSourceQualityFilter.FHD -> tvStreamQualityScore(stream) in 1080..2159
                TvSourceQualityFilter.HD -> tvStreamQualityScore(stream) in 720..1079
                TvSourceQualityFilter.OTHER -> tvStreamQualityScore(stream) < 720
            }
            providerMatches && qualityMatches
        }
    }

    LaunchedEffect(providers, selectedProvider) {
        if (selectedProvider != null && selectedProvider !in providers) {
            selectedProvider = null
        }
    }

    LaunchedEffect(requestKey, reloadToken) {
        val generation = reloadToken
        loading = true
        errorMessage = null
        streams = emptyList()
        selectedStreamUrl = null
        selectedProvider = null
        qualityFilter = TvSourceQualityFilter.ALL
        autoPlayConsumed = false
        scanStatus = "Checking installed stream add-ons..."

        aggregator.load(
            item = request.item,
            episode = request.episode,
            preferredSession = request.session,
            onProgress = { progress ->
                if (reloadToken != generation) return@load
                scanStatus = when (progress.phase) {
                    StreamAggregationPhase.DISCOVERING -> {
                        val total = progress.total.coerceAtLeast(1)
                        "Checking add-ons ${progress.current}/$total" +
                            progress.sourceName
                                .takeIf { it.isNotBlank() }
                                ?.let { " · ${it.take(34)}" }
                                .orEmpty()
                    }
                    StreamAggregationPhase.SCANNING -> {
                        val total = progress.total.coerceAtLeast(1)
                        "Scanning ${progress.current}/$total" +
                            progress.sourceName
                                .takeIf { it.isNotBlank() }
                                ?.let { " · ${it.take(34)}" }
                                .orEmpty() +
                            " · ${progress.foundStreams} found"
                    }
                }
            },
            onStreamsUpdated = { partial ->
                if (reloadToken != generation) return@load
                val ranked = rankTvStreams(partial)
                if (ranked.isNotEmpty()) {
                    streams = ranked
                }
            },
            onSuccess = { result ->
                if (reloadToken != generation) return@load
                val ranked = rankTvStreams(result.streams)
                streams = ranked
                loading = false
                scanStatus = when {
                    result.streamAddonCount == 0 -> {
                        "${result.enabledSourceCount} enabled sources · 0 stream add-ons"
                    }
                    ranked.isEmpty() -> {
                        "Scanned ${result.scannedStreamAddonCount}/${result.streamAddonCount} add-ons · 0 playable"
                    }
                    else -> {
                        "Scanned ${result.scannedStreamAddonCount}/${result.streamAddonCount} add-ons · ${ranked.size} playable"
                    }
                }
                errorMessage = when {
                    result.streamAddonCount == 0 -> {
                        "No enabled source provides streams for this title."
                    }
                    ranked.isEmpty() -> {
                        buildString {
                            append("No playable source was returned for this title.")
                            if (result.openFailureCount > 0 || result.loadFailureCount > 0) {
                                append(
                                    " Source failures: open=${result.openFailureCount}, " +
                                        "load=${result.loadFailureCount}."
                                )
                            }
                        }
                    }
                    else -> null
                }
            }
        )
    }

    LaunchedEffect(requestKey, reloadToken) {
        val generation = reloadToken
        aggregatedSubtitles = emptyList()
        subtitleScanComplete = false
        subtitleAggregator.load(
            item = request.item,
            episode = request.episode,
            preferredSession = request.session,
            onSuccess = { result ->
                if (reloadToken != generation) return@load
                aggregatedSubtitles = result.subtitles
                subtitleScanComplete = true
            }
        )
    }

    LaunchedEffect(requestKey, reloadToken) {
        val generation = reloadToken
        delay(15000)
        if (
            reloadToken == generation &&
            loading &&
            streams.isEmpty()
        ) {
            loading = false
            errorMessage = "Source scan is taking too long. You can retry without leaving Details."
            scanStatus = "Source scan timed out"
        }
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
            delay(120)
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
                onPlayStream(preferred.withMergedSubtitles(aggregatedSubtitles))
            }
        }
    }

    BackHandler(onBack = onClose)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(TvColors.Background)
    ) {
        TvSourcesBackdrop(request.item)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = 58.dp,
                    end = 58.dp,
                    top = 42.dp,
                    bottom = 34.dp
                )
        ) {
            TvSourcesHeader(
                request = request,
                streamCount = streams.size,
                subtitleCount = aggregatedSubtitles.size,
                loading = loading,
                scanStatus = scanStatus
            )
            Spacer(Modifier.height(18.dp))

            if (streams.isNotEmpty()) {
                TvSourcesFilterRail(
                    title = "Provider",
                    labels = listOf("All") + providers,
                    selected = selectedProvider ?: "All",
                    onSelected = { label ->
                        selectedProvider = label.takeUnless { it == "All" }
                    }
                )
                Spacer(Modifier.height(10.dp))
                TvSourcesFilterRail(
                    title = "Quality",
                    labels = TvSourceQualityFilter.entries.map { it.label },
                    selected = qualityFilter.label,
                    onSelected = { label ->
                        qualityFilter = TvSourceQualityFilter.entries
                            .firstOrNull { it.label == label }
                            ?: TvSourceQualityFilter.ALL
                    }
                )
                Spacer(Modifier.height(16.dp))
            }

            when {
                loading && streams.isEmpty() -> {
                    TvSourcesLoadingState(
                        scanStatus = scanStatus,
                        modifier = Modifier.weight(1f)
                    )
                }
                errorMessage != null && streams.isEmpty() -> {
                    TvSourcesErrorState(
                        message = errorMessage.orEmpty(),
                        focusRequester = retryRequester,
                        onRetry = { reloadToken += 1 },
                        modifier = Modifier.weight(1f)
                    )
                }
                else -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(28.dp)
                    ) {
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Available sources",
                                    color = TvColors.TextPrimary,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 18.sp
                                )
                                Text(
                                    text = if (filteredStreams.size == streams.size) {
                                        "${streams.size} results"
                                    } else {
                                        "${filteredStreams.size} of ${streams.size}"
                                    },
                                    color = TvColors.TextSecondary,
                                    fontSize = 12.sp
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            if (filteredStreams.isEmpty()) {
                                TvSourcesFilteredEmpty(
                                    onReset = {
                                        selectedProvider = null
                                        qualityFilter = TvSourceQualityFilter.ALL
                                    }
                                )
                            } else {
                                LazyColumn(
                                    state = streamListState,
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    contentPadding = PaddingValues(bottom = 22.dp)
                                ) {
                                    itemsIndexed(
                                        items = filteredStreams,
                                        key = { _, stream -> stream.url }
                                    ) { index, stream ->
                                        TvSourceRow(
                                            stream = stream,
                                            index = index,
                                            selected = selectedStreamUrl == stream.url,
                                            focusRequester = streamRequesters[stream.url]
                                                ?: FocusRequester(),
                                            onSelected = {
                                                selectedStreamUrl = stream.url
                                                onPlayStream(
                                                    stream.withMergedSubtitles(
                                                        aggregatedSubtitles
                                                    )
                                                )
                                            }
                                        )
                                    }
                                }
                            }
                        }
                        TvSourcesSummaryPanel(
                            totalStreams = streams.size,
                            visibleStreams = filteredStreams.size,
                            providerCount = providers.size,
                            subtitleCount = aggregatedSubtitles.size,
                            loading = loading,
                            scanStatus = scanStatus,
                            onRetry = { reloadToken += 1 }
                        )
                    }
                }
            }
        }
    }

    LaunchedEffect(loading, errorMessage, filteredStreams.map { it.url }) {
        if (filteredStreams.isNotEmpty()) {
            delay(70)
            val target = selectedStreamUrl
                ?.takeIf { selected -> filteredStreams.any { it.url == selected } }
                ?: filteredStreams.first().url
            streamRequesters[target]?.let { requester ->
                runCatching { requester.requestFocus() }
            }
        } else if (!loading && errorMessage != null && streams.isEmpty()) {
            delay(70)
            runCatching { retryRequester.requestFocus() }
        }
    }

    LaunchedEffect(playerReturnToken) {
        if (playerReturnToken > 0 && filteredStreams.isNotEmpty()) {
            delay(80)
            val selectedIndex = filteredStreams.indexOfFirst { stream ->
                stream.url == selectedStreamUrl
            }.takeIf { it >= 0 } ?: 0
            runCatching { streamListState.scrollToItem(selectedIndex) }
            delay(50)
            val target = filteredStreams.getOrNull(selectedIndex)?.url
            target?.let(streamRequesters::get)?.let { requester ->
                runCatching { requester.requestFocus() }
            }
        }
    }
}

@Composable
private fun TvSourcesBackdrop(item: MediaItem) {
    val image = item.backgroundUrl.takeIf { it.isNotBlank() } ?: item.posterUrl
    Box(Modifier.fillMaxSize()) {
        if (image.isNotBlank()) {
            AsyncImage(
                model = image,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = 0.30f },
                contentScale = ContentScale.Crop
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        colorStops = arrayOf(
                            0.00f to TvColors.Background,
                            0.58f to TvColors.Background.copy(alpha = 0.94f),
                            1.00f to TvColors.Background.copy(alpha = 0.78f)
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
                            0.00f to Color.Transparent,
                            0.62f to TvColors.Background.copy(alpha = 0.46f),
                            1.00f to TvColors.Background
                        )
                    )
                )
        )
    }
}

@Composable
private fun TvSourcesHeader(
    request: TvSourcePreviewRequest,
    streamCount: Int,
    subtitleCount: Int,
    loading: Boolean,
    scanStatus: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "Choose a source",
                color = TvColors.TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 30.sp
            )
            Text(
                text = request.episode?.displayTitle ?: request.item.title,
                color = TvColors.TextSecondary,
                fontSize = 15.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = scanStatus,
                color = if (loading) TvColors.Accent else TvColors.TextSecondary,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (streamCount > 0) {
            Text(
                text = buildString {
                    append("$streamCount playable")
                    if (subtitleCount > 0) append(" · $subtitleCount subtitles")
                },
                color = TvColors.TextSecondary,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun TvSourcesFilterRail(
    title: String,
    labels: List<String>,
    selected: String,
    onSelected: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = title,
            modifier = Modifier.width(66.dp),
            color = TvColors.TextSecondary,
            fontWeight = FontWeight.Medium,
            fontSize = 11.sp
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(end = 16.dp)
        ) {
            itemsIndexed(
                items = labels,
                key = { _, label -> "$title:$label" }
            ) { _, label ->
                TvSourcesFilterChip(
                    label = label,
                    selected = label == selected,
                    onClick = { onSelected(label) }
                )
            }
        }
    }
}

@Composable
private fun TvSourcesFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    var focused by remember(label) { mutableStateOf(false) }
    Surface(
        modifier = Modifier
            .height(34.dp)
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
        color = when {
            focused -> Color.White
            selected -> Color.White.copy(alpha = 0.16f)
            else -> Color.White.copy(alpha = 0.07f)
        },
        shape = RoundedCornerShape(17.dp),
        border = BorderStroke(
            1.dp,
            when {
                focused -> Color.White
                selected -> TvColors.Accent.copy(alpha = 0.75f)
                else -> Color.White.copy(alpha = 0.08f)
            }
        )
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                color = if (focused) Color.Black else TvColors.TextPrimary,
                fontWeight = if (selected || focused) FontWeight.SemiBold else FontWeight.Normal,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun TvSourceRow(
    stream: StreamSource,
    index: Int,
    selected: Boolean,
    focusRequester: FocusRequester,
    onSelected: () -> Unit
) {
    var focused by remember(stream.url) { mutableStateOf(false) }
    val quality = tvStreamQualityLabel(stream)
    val provider = tvProviderLabel(stream)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(if (stream.note.isBlank()) 76.dp else 92.dp)
            .focusRequester(focusRequester)
            .onFocusChanged { focused = it.hasFocus }
            .onPreviewKeyEvent { event ->
                if (
                    event.type == KeyEventType.KeyDown &&
                    (event.key == Key.DirectionCenter || event.key == Key.Enter)
                ) {
                    onSelected()
                    true
                } else {
                    false
                }
            }
            .focusable(),
        color = when {
            focused -> Color.White
            selected -> Color.White.copy(alpha = 0.14f)
            else -> TvColors.Surface.copy(alpha = 0.88f)
        },
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(
            if (focused) 2.dp else 1.dp,
            when {
                focused -> Color.White
                selected -> TvColors.Accent.copy(alpha = 0.70f)
                else -> Color.White.copy(alpha = 0.07f)
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "${index + 1}",
                modifier = Modifier.width(26.dp),
                color = if (focused) Color.Black.copy(alpha = 0.56f) else TvColors.TextSecondary,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stream.name.ifBlank { provider.ifBlank { "Source ${index + 1}" } },
                        modifier = Modifier.weight(1f),
                        color = if (focused) Color.Black else TvColors.TextPrimary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (quality != null) {
                        Text(
                            text = quality,
                            color = if (focused) Color.Black else TvColors.Accent,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }
                Text(
                    text = buildString {
                        append(provider.ifBlank { "Unknown provider" })
                        stream.type.takeIf { it.isNotBlank() && it != "auto" }?.let {
                            append(" · ${it.uppercase()}")
                        }
                        if (stream.subtitles.isNotEmpty()) {
                            append(" · ${stream.subtitles.size} subs")
                        }
                    },
                    color = if (focused) Color.Black.copy(alpha = 0.62f) else TvColors.TextSecondary,
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                stream.note.takeIf { it.isNotBlank() }?.let { note ->
                    Text(
                        text = note,
                        color = if (focused) Color.Black.copy(alpha = 0.58f) else TvColors.TextSecondary,
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            if (selected) {
                Text(
                    text = "NOW",
                    color = if (focused) Color.Black else TvColors.Accent,
                    fontWeight = FontWeight.Bold,
                    fontSize = 9.sp
                )
            }
        }
    }
}

@Composable
private fun TvSourcesSummaryPanel(
    totalStreams: Int,
    visibleStreams: Int,
    providerCount: Int,
    subtitleCount: Int,
    loading: Boolean,
    scanStatus: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier.width(230.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(
            color = TvColors.Surface.copy(alpha = 0.78f),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.07f))
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Source scan",
                    color = TvColors.TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
                TvSourceStat("Playable", totalStreams.toString())
                TvSourceStat("Visible", visibleStreams.toString())
                TvSourceStat("Providers", providerCount.toString())
                TvSourceStat("Subtitles", subtitleCount.toString())
                if (loading) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Text(
                            text = "Still scanning",
                            color = TvColors.Accent,
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }
        Text(
            text = scanStatus,
            color = TvColors.TextSecondary,
            fontSize = 10.sp,
            lineHeight = 14.sp,
            maxLines = 4,
            overflow = TextOverflow.Ellipsis
        )
        TvSourcesCompactButton(
            label = "Rescan",
            onClick = onRetry
        )
        Text(
            text = "OK plays source\nBack returns to Details",
            color = TvColors.TextSecondary.copy(alpha = 0.72f),
            fontSize = 10.sp,
            lineHeight = 14.sp
        )
    }
}

@Composable
private fun TvSourceStat(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = TvColors.TextSecondary, fontSize = 10.sp)
        Text(
            text = value,
            color = TvColors.TextPrimary,
            fontWeight = FontWeight.SemiBold,
            fontSize = 10.sp
        )
    }
}

@Composable
private fun TvSourcesLoadingState(
    scanStatus: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CircularProgressIndicator(modifier = Modifier.size(34.dp), strokeWidth = 3.dp)
            Text(
                text = "Finding playable sources",
                color = TvColors.TextPrimary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 17.sp
            )
            Text(
                text = scanStatus,
                color = TvColors.TextSecondary,
                fontSize = 11.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun TvSourcesErrorState(
    message: String,
    focusRequester: FocusRequester,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.width(560.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "No sources ready",
                color = TvColors.TextPrimary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 20.sp
            )
            Text(
                text = message,
                color = TvColors.TextSecondary,
                fontSize = 13.sp,
                lineHeight = 18.sp
            )
            TvSourcesCompactButton(
                label = "Retry",
                focusRequester = focusRequester,
                onClick = onRetry
            )
        }
    }
}

@Composable
private fun TvSourcesFilteredEmpty(onReset: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 60.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "No sources match these filters",
            color = TvColors.TextPrimary,
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp
        )
        TvSourcesCompactButton(label = "Show all", onClick = onReset)
    }
}

@Composable
private fun TvSourcesCompactButton(
    label: String,
    focusRequester: FocusRequester? = null,
    onClick: () -> Unit
) {
    var focused by remember(label) { mutableStateOf(false) }
    Surface(
        modifier = Modifier
            .width(156.dp)
            .height(42.dp)
            .then(
                if (focusRequester != null) Modifier.focusRequester(focusRequester)
                else Modifier
            )
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
        color = if (focused) Color.White else Color.White.copy(alpha = 0.09f),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(
            1.dp,
            if (focused) Color.White else Color.White.copy(alpha = 0.10f)
        )
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.Refresh,
                contentDescription = null,
                tint = if (focused) Color.Black else TvColors.TextPrimary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(7.dp))
            Text(
                text = label,
                color = if (focused) Color.Black else TvColors.TextPrimary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 11.sp
            )
        }
    }
}

private fun StreamSource.withMergedSubtitles(
    extra: List<SubtitleSource>
): StreamSource {
    return copy(
        subtitles = (subtitles + extra)
            .distinctBy { subtitle ->
                "${subtitle.url}|${subtitle.language}|${subtitle.label}"
            }
    )
}

private fun rankTvStreams(streams: List<StreamSource>): List<StreamSource> {
    return streams
        .asSequence()
        .filter { stream -> stream.playable && stream.url.isNotBlank() }
        .distinctBy(StreamSource::url)
        .sortedWith(
            compareByDescending<StreamSource>(::tvStreamQualityScore)
                .thenBy { tvProviderLabel(it).lowercase() }
                .thenBy { it.name.lowercase() }
        )
        .toList()
}

private fun tvProviderLabel(stream: StreamSource): String {
    return stream.providerName
        .takeIf { it.isNotBlank() }
        ?: stream.providerId.takeIf { it.isNotBlank() }
        ?: "Other"
}

private fun tvStreamQualityScore(stream: StreamSource): Int {
    val value = "${stream.name} ${stream.note} ${stream.url}".lowercase()
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

private fun tvStreamQualityLabel(stream: StreamSource): String? {
    return when (val score = tvStreamQualityScore(stream)) {
        2160 -> "4K"
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
