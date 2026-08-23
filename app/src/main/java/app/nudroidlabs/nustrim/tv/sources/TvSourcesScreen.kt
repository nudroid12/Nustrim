package app.nudroidlabs.nustrim.tv.sources

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.nudroidlabs.nustrim.core.model.MediaEpisode
import app.nudroidlabs.nustrim.core.model.MediaItem
import app.nudroidlabs.nustrim.tv.focus.TvFocusRegistry
import app.nudroidlabs.nustrim.tv.focus.TvFocusRestoreEffect
import app.nudroidlabs.nustrim.tv.focus.rememberTvFocusAnchor
import app.nudroidlabs.nustrim.tv.focus.tvFocusAnchor
import app.nudroidlabs.nustrim.tv.theme.TvTokens
import app.nudroidlabs.nustrim.tv.theme.animateTvFocusScale
import coil3.compose.AsyncImage

@Composable
fun TvSourcesScreen(
    media: MediaItem,
    episode: MediaEpisode?,
    state: TvSourcesUiState,
    routeKey: String,
    scopeKey: String,
    focusRegistry: TvFocusRegistry,
    focusRequestToken: Int,
    onRefresh: () -> Unit,
    onStreamSelected: (TvSourceStream) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SOURCES_BACKGROUND),
    ) {
        SourcesBackdrop(media)

        Row(Modifier.fillMaxSize()) {
            SourcesIdentity(
                media = media,
                episode = episode,
                modifier = Modifier
                    .weight(0.40f)
                    .fillMaxHeight(),
            )

            SourcesRightPane(
                state = state,
                routeKey = routeKey,
                scopeKey = scopeKey,
                focusRegistry = focusRegistry,
                focusRequestToken = focusRequestToken,
                onRefresh = onRefresh,
                onStreamSelected = onStreamSelected,
                modifier = Modifier
                    .weight(0.60f)
                    .fillMaxHeight(),
            )
        }
    }
}

@Composable
private fun SourcesBackdrop(media: MediaItem) {
    val image = media.backgroundUrl.ifBlank { media.posterUrl }
    if (image.isNotBlank()) {
        AsyncImage(
            model = image,
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = 0.50f },
            contentScale = ContentScale.Crop,
            alignment = Alignment.TopCenter,
        )
    }
    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.horizontalGradient(
                    colorStops = arrayOf(
                        0.00f to Color(0xF708090B),
                        0.24f to Color(0xE808090B),
                        0.48f to Color(0xC808090B),
                        0.72f to Color(0xA908090B),
                        1.00f to Color(0xD608090B),
                    ),
                ),
            ),
    )
    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    0f to Color.Transparent,
                    0.72f to Color.Transparent,
                    1f to SOURCES_BACKGROUND,
                ),
            ),
    )
}

@Composable
private fun SourcesIdentity(
    media: MediaItem,
    episode: MediaEpisode?,
    modifier: Modifier,
) {
    Column(
        modifier = modifier.padding(start = 54.dp, end = 32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        var logoFailed by remember(media.logoUrl) { mutableStateOf(false) }
        if (media.logoUrl.isNotBlank() && !logoFailed) {
            AsyncImage(
                model = media.logoUrl,
                contentDescription = media.title,
                onError = { logoFailed = true },
                modifier = Modifier
                    .height(104.dp)
                    .widthIn(max = 300.dp),
                contentScale = ContentScale.Fit,
            )
        } else {
            Text(
                text = media.title,
                color = Color.White,
                fontSize = 34.sp,
                lineHeight = 39.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Spacer(Modifier.height(18.dp))
        if (episode != null) {
            val coordinate = when {
                episode.season != null && episode.episode != null -> "S${episode.season}E${episode.episode}"
                else -> "Episode"
            }
            Text(
                text = coordinate,
                color = Color(0xFFE8E9EB),
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            )
            if (episode.title.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = episode.title,
                    color = Color(0xFFBFC1C7),
                    fontSize = 15.sp,
                    lineHeight = 20.sp,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        } else {
            val info = buildList {
                media.releaseInfo.trim().takeIf { it.isNotBlank() }?.let(::add)
                media.runtime.trim().takeIf { it.isNotBlank() }?.let(::add)
                media.genres.take(2).map { it.trim() }.filter { it.isNotBlank() }.forEach(::add)
            }.joinToString(" • ")
            if (info.isNotBlank()) {
                Text(
                    text = info,
                    color = Color(0xFFBFC1C7),
                    fontSize = 15.sp,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun SourcesRightPane(
    state: TvSourcesUiState,
    routeKey: String,
    scopeKey: String,
    focusRegistry: TvFocusRegistry,
    focusRequestToken: Int,
    onRefresh: () -> Unit,
    onStreamSelected: (TvSourceStream) -> Unit,
    modifier: Modifier,
) {
    val snapshot = when (state) {
        is TvSourcesUiState.Loading -> state.snapshot
        is TvSourcesUiState.Ready -> state.snapshot
        is TvSourcesUiState.Empty -> state.snapshot
        else -> null
    }
    val memory = remember(routeKey) { TvSourcesSessionStore.memory(routeKey) }
    var selectedSource by remember(routeKey) { mutableStateOf(memory.selectedSourceLabel) }
    var filterChangeToken by remember(routeKey) { mutableIntStateOf(0) }

    val availableSources = snapshot?.sourceLabels.orEmpty()
    LaunchedEffect(availableSources) {
        if (selectedSource != null && selectedSource !in availableSources) {
            selectedSource = null
            memory.selectedSourceLabel = null
        }
    }

    val visibleStreams = remember(snapshot, selectedSource) {
        snapshot?.filtered(selectedSource).orEmpty()
    }

    val fallbackAnchor = when {
        visibleStreams.isNotEmpty() -> {
            val remembered = memory.lastFocusedStreamKey
                ?.takeIf { key -> visibleStreams.any { it.stableKey == key } }
            streamAnchorKey(remembered ?: visibleStreams.first().stableKey)
        }
        state is TvSourcesUiState.Error -> RETRY_ANCHOR
        else -> REFRESH_ANCHOR
    }

    TvFocusRestoreEffect(
        registry = focusRegistry,
        scopeKey = scopeKey,
        fallbackAnchorKey = fallbackAnchor,
        requestToken = focusRequestToken,
    )

    LaunchedEffect(filterChangeToken, selectedSource, visibleStreams) {
        if (filterChangeToken <= 0 || visibleStreams.isEmpty()) return@LaunchedEffect
        val first = visibleStreams.first()
        memory.lastFocusedStreamKey = first.stableKey
        repeat(2) { withFrameNanos { } }
        focusRegistry.requestAnchor(scopeKey, streamAnchorKey(first.stableKey))
    }

    fun selectFilter(label: String?) {
        if (selectedSource == label) return
        selectedSource = label
        memory.selectedSourceLabel = label
        memory.firstVisibleItemIndex = 0
        memory.lastFocusedStreamKey = snapshot?.filtered(label)?.firstOrNull()?.stableKey
        filterChangeToken += 1
    }

    Column(
        modifier = modifier.padding(top = 48.dp, end = 48.dp, bottom = 48.dp),
    ) {
        SourceFilterRow(
            snapshot = snapshot,
            selectedSource = selectedSource,
            visibleStreams = visibleStreams,
            rememberedStreamKey = memory.lastFocusedStreamKey
                ?.takeIf { key -> visibleStreams.any { it.stableKey == key } }
                ?: visibleStreams.firstOrNull()?.stableKey,
            scopeKey = scopeKey,
            focusRegistry = focusRegistry,
            onRefresh = onRefresh,
            onSelectFilter = ::selectFilter,
        )
        snapshot?.loadingProviderCount
            ?.takeIf { it > 0 }
            ?.let { loadingCount ->
                Spacer(Modifier.height(8.dp))
                Text(
                    text = if (loadingCount == 1) {
                        "1 provider still loading"
                    } else {
                        "$loadingCount providers still loading"
                    },
                    color = Color(0xFFB7B9BF),
                    fontSize = 12.sp,
                )
            }
        Spacer(Modifier.height(18.dp))
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(22.dp))
                .background(Color(0xA1121419))
                .border(1.dp, Color(0x443D414A), RoundedCornerShape(22.dp)),
        ) {
            when (state) {
                is TvSourcesUiState.Loading -> SourcesLoading(state.snapshot?.loadingProviderCount ?: 0)
                is TvSourcesUiState.Error -> SourcesError(
                    message = state.message,
                    scopeKey = scopeKey,
                    focusRegistry = focusRegistry,
                    onRetry = onRefresh,
                )
                is TvSourcesUiState.Empty -> SourcesEmpty(state.snapshot.attempts)
                is TvSourcesUiState.Ready -> {
                    if (visibleStreams.isEmpty()) {
                        SourcesEmpty(snapshot?.attempts.orEmpty())
                    } else {
                        SourcesList(
                            streams = visibleStreams,
                            selectedSource = selectedSource,
                            filterChangeToken = filterChangeToken,
                            availableSources = availableSources,
                            memory = memory,
                            scopeKey = scopeKey,
                            focusRegistry = focusRegistry,
                            onSelectFilter = ::selectFilter,
                            onStreamSelected = onStreamSelected,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SourceFilterRow(
    snapshot: TvSourcesSnapshot?,
    selectedSource: String?,
    visibleStreams: List<TvSourceStream>,
    rememberedStreamKey: String?,
    scopeKey: String,
    focusRegistry: TvFocusRegistry,
    onRefresh: () -> Unit,
    onSelectFilter: (String?) -> Unit,
) {
    val attempts = snapshot?.attempts.orEmpty()
    val labels = remember(snapshot) {
        buildList {
            snapshot?.sourceLabels.orEmpty().forEach { if (it !in this) add(it) }
            attempts
                .filter { it.status != TvSourceAttemptStatus.EMPTY }
                .map { it.sourceLabel }
                .filter { it.isNotBlank() }
                .forEach { if (it !in this) add(it) }
        }.take(MAX_FILTER_CHIPS)
    }
    val attemptByLabel = remember(attempts) {
        attempts.groupBy { it.sourceLabel }.mapValues { (_, values) ->
            values.firstOrNull { it.status == TvSourceAttemptStatus.SUCCESS }
                ?: values.firstOrNull { it.status == TvSourceAttemptStatus.ERROR }
                ?: values.first()
        }
    }
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(horizontal = 2.dp, vertical = 2.dp),
    ) {
        item(key = "refresh") {
            SourceChip(
                text = "↻",
                selected = false,
                enabled = true,
                anchorKey = REFRESH_ANCHOR,
                downStreamKey = rememberedStreamKey,
                scopeKey = scopeKey,
                focusRegistry = focusRegistry,
                onClick = onRefresh,
            )
        }
        item(key = "all") {
            SourceChip(
                text = "All",
                selected = selectedSource == null,
                enabled = true,
                anchorKey = ALL_ANCHOR,
                downStreamKey = rememberedStreamKey,
                scopeKey = scopeKey,
                focusRegistry = focusRegistry,
                onClick = { onSelectFilter(null) },
            )
        }
        itemsIndexed(labels, key = { _, label -> label }) { _, label ->
            val hasStreams = snapshot?.streams?.any { it.sourceLabel == label } == true
            val attempt = attemptByLabel[label]
            val suffix = when (attempt?.status) {
                TvSourceAttemptStatus.LOADING -> "  …"
                TvSourceAttemptStatus.ERROR -> "  ×"
                else -> ""
            }
            SourceChip(
                text = label + suffix,
                selected = selectedSource == label,
                enabled = hasStreams,
                anchorKey = sourceChipAnchorKey(label),
                downStreamKey = if (selectedSource == label) rememberedStreamKey else null,
                scopeKey = scopeKey,
                focusRegistry = focusRegistry,
                onClick = { if (hasStreams) onSelectFilter(label) },
            )
        }
    }
}

@Composable
private fun SourceChip(
    text: String,
    selected: Boolean,
    enabled: Boolean,
    anchorKey: String,
    downStreamKey: String?,
    scopeKey: String,
    focusRegistry: TvFocusRegistry,
    onClick: () -> Unit,
) {
    val anchor = rememberTvFocusAnchor(focusRegistry, scopeKey, anchorKey)
    var focused by remember(anchorKey) { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .height(42.dp)
            .clip(RoundedCornerShape(21.dp))
            .background(
                when {
                    focused -> Color(0xFFF3F3F4)
                    selected -> Color(0xFF31343B)
                    else -> Color(0xB317191E)
                },
            )
            .border(
                width = if (focused) 2.dp else 1.dp,
                color = if (focused) Color.White else Color(0xFF43464F),
                shape = RoundedCornerShape(21.dp),
            )
            .tvFocusAnchor(anchor)
            .onFocusChanged { focused = it.isFocused }
            .onKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                when (event.key) {
                    Key.DirectionDown -> {
                        val streamKey = downStreamKey ?: return@onKeyEvent false
                        focusRegistry.requestAnchor(scopeKey, streamAnchorKey(streamKey))
                    }
                    Key.DirectionCenter, Key.Enter -> {
                        if (enabled) onClick()
                        true
                    }
                    else -> false
                }
            }
            .focusable()
            .padding(horizontal = if (text == "↻") 14.dp else 18.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = when {
                focused -> Color(0xFF101114)
                enabled -> Color.White
                else -> Color(0xFF777A82)
            },
            fontSize = 14.sp,
            fontWeight = if (selected || focused) FontWeight.SemiBold else FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun SourcesList(
    streams: List<TvSourceStream>,
    selectedSource: String?,
    filterChangeToken: Int,
    availableSources: List<String>,
    memory: TvSourcesMemory,
    scopeKey: String,
    focusRegistry: TvFocusRegistry,
    onSelectFilter: (String?) -> Unit,
    onStreamSelected: (TvSourceStream) -> Unit,
) {
    val initialIndex = remember(streams, memory.lastFocusedStreamKey) {
        streams.indexOfFirst { it.stableKey == memory.lastFocusedStreamKey }
            .takeIf { it >= 0 }
            ?: memory.firstVisibleItemIndex.coerceIn(0, streams.lastIndex.coerceAtLeast(0))
    }
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialIndex)
    LaunchedEffect(filterChangeToken) {
        if (filterChangeToken > 0 && streams.isNotEmpty()) {
            listState.scrollToItem(0)
        }
    }
    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        itemsIndexed(
            items = streams,
            key = { _, stream -> stream.stableKey },
        ) { index, stream ->
            SourceStreamCard(
                stream = stream,
                scopeKey = scopeKey,
                focusRegistry = focusRegistry,
                onFocused = {
                    memory.lastFocusedStreamKey = stream.stableKey
                    memory.firstVisibleItemIndex = index
                },
                onUp = if (index == 0) {
                    {
                        val chipAnchor = selectedSource?.let(::sourceChipAnchorKey) ?: ALL_ANCHOR
                        focusRegistry.requestAnchor(scopeKey, chipAnchor)
                    }
                } else null,
                onCycleFilter = { delta ->
                    val options = listOf<String?>(null) + availableSources
                    val current = options.indexOf(selectedSource).coerceAtLeast(0)
                    val target = current + delta
                    if (target in options.indices) {
                        onSelectFilter(options[target])
                        true
                    } else {
                        false
                    }
                },
                onOpen = { if (stream.playable) onStreamSelected(stream) },
            )
        }
    }
}

@Composable
private fun SourceStreamCard(
    stream: TvSourceStream,
    scopeKey: String,
    focusRegistry: TvFocusRegistry,
    onFocused: () -> Unit,
    onUp: (() -> Boolean)?,
    onCycleFilter: (Int) -> Boolean,
    onOpen: () -> Unit,
) {
    val anchor = rememberTvFocusAnchor(focusRegistry, scopeKey, streamAnchorKey(stream.stableKey))
    var focused by remember(stream.stableKey) { mutableStateOf(false) }
    val scale = animateTvFocusScale(
        focused = focused,
        focusedScale = TvTokens.SubtleFocusScale,
        label = "source-stream-scale",
    )
    val background = when {
        focused -> Color(0xFFF0F0F2)
        else -> Color(0xC51A1C21)
    }
    val primary = if (focused) Color(0xFF111216) else Color.White
    val secondary = if (focused) Color(0xFF51535A) else Color(0xFFAEB0B6)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(90.dp)
            .scale(scale)
            .clip(RoundedCornerShape(15.dp))
            .background(background)
            .border(
                width = if (focused) 2.dp else 1.dp,
                color = if (focused) Color.White else Color(0xFF363942),
                shape = RoundedCornerShape(15.dp),
            )
            .tvFocusAnchor(anchor)
            .onFocusChanged {
                focused = it.isFocused
                if (it.isFocused) onFocused()
            }
            .onKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                when (event.key) {
                    Key.DirectionLeft -> onCycleFilter(-1)
                    Key.DirectionRight -> onCycleFilter(1)
                    Key.DirectionUp -> onUp?.invoke() ?: false
                    Key.DirectionCenter, Key.Enter -> {
                        if (stream.playable) onOpen()
                        true
                    }
                    else -> false
                }
            }
            .focusable()
            .padding(horizontal = 20.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(if (focused) Color(0xFF24262C) else Color(0xFF292C33)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stream.sourceLabel.take(1).uppercase().ifBlank { "S" },
                color = Color.White,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stream.stream.name.ifBlank { "Unknown stream" },
                    color = primary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                val badges = buildList {
                    stream.qualityLabel.takeIf { it.isNotBlank() }?.let(::add)
                    stream.transportLabel.takeIf { it.isNotBlank() }?.let(::add)
                }
                if (badges.isNotEmpty()) {
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = badges.joinToString("  "),
                        color = secondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
            Spacer(Modifier.height(5.dp))
            Text(
                text = buildString {
                    append(stream.sourceLabel)
                    if (!stream.playable) {
                        append(" • Unsupported")
                    } else if (stream.stream.providerName.isNotBlank() &&
                        !stream.stream.providerName.equals(stream.sourceLabel, ignoreCase = true)
                    ) {
                        append(" • ")
                        append(stream.stream.providerName)
                    }
                },
                color = secondary,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (!stream.playable && stream.stream.note.isNotBlank()) {
                Text(
                    text = stream.stream.note,
                    color = secondary,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun SourcesLoading(loadingProviderCount: Int) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        repeat(5) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(90.dp)
                    .clip(RoundedCornerShape(15.dp))
                    .background(Color(0xAA1C1E24)),
            )
        }
    }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = Color(0xFFE6E7E9), strokeWidth = 2.dp)
            if (loadingProviderCount > 0) {
                Spacer(Modifier.height(14.dp))
                Text(
                    text = if (loadingProviderCount == 1) {
                        "Loading 1 provider"
                    } else {
                        "Loading $loadingProviderCount providers"
                    },
                    color = Color(0xFFB7B9BF),
                    fontSize = 13.sp,
                )
            }
        }
    }
}

@Composable
private fun SourcesError(
    message: String,
    scopeKey: String,
    focusRegistry: TvFocusRegistry,
    onRetry: () -> Unit,
) {
    val anchor = rememberTvFocusAnchor(focusRegistry, scopeKey, RETRY_ANCHOR)
    var focused by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(42.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Sources unavailable", color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(10.dp))
        Text(
            text = message,
            color = Color(0xFFB7B9BF),
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(24.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(22.dp))
                .background(if (focused) Color.White else Color(0xFF292C33))
                .tvFocusAnchor(anchor)
                .onFocusChanged { focused = it.isFocused }
                .onKeyEvent { event ->
                    if (event.type == KeyEventType.KeyDown &&
                        (event.key == Key.DirectionCenter || event.key == Key.Enter)
                    ) {
                        onRetry()
                        true
                    } else {
                        false
                    }
                }
                .focusable()
                .padding(horizontal = 24.dp, vertical = 11.dp),
        ) {
            Text(
                text = "Retry",
                color = if (focused) Color(0xFF101114) else Color.White,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun SourcesEmpty(attempts: List<TvSourceAttempt>) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(42.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "No streams found",
            color = Color.White,
            fontSize = 25.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(9.dp))
        Text(
            text = "Try Refresh or check your installed stream sources.",
            color = Color(0xFFAFB1B7),
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
        )
        if (attempts.isNotEmpty()) {
            Spacer(Modifier.height(18.dp))
            Text(
                text = attempts
                    .take(5)
                    .joinToString("\n") { attempt ->
                        buildString {
                            append(attempt.sourceLabel)
                            append(": ")
                            append(
                                when (attempt.status) {
                                    TvSourceAttemptStatus.LOADING ->
                                        "loading"
                                    TvSourceAttemptStatus.SUCCESS ->
                                        "${attempt.streamCount} stream(s)"
                                    TvSourceAttemptStatus.EMPTY ->
                                        "0 streams"
                                    TvSourceAttemptStatus.ERROR ->
                                        "error"
                                }
                            )
                            if (attempt.message.isNotBlank()) {
                                append(" · ")
                                append(attempt.message)
                            }
                        }
                    },
                color = Color.White.copy(alpha = 0.64f),
                fontSize = 12.sp,
                lineHeight = 16.sp,
                textAlign = TextAlign.Center,
                maxLines = 10,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private fun streamAnchorKey(streamKey: String) = "sources:stream:$streamKey"
private fun sourceChipAnchorKey(sourceLabel: String) = "sources:chip:${sourceLabel.hashCode().toString(16)}"
private const val REFRESH_ANCHOR = "sources:chip:refresh"
private const val ALL_ANCHOR = "sources:chip:all"
private const val RETRY_ANCHOR = "sources:error:retry"
private const val MAX_FILTER_CHIPS = 18
private val SOURCES_BACKGROUND = Color(0xFF08090B)
