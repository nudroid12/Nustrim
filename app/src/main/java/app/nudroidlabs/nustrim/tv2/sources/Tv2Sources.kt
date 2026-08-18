package app.nudroidlabs.nustrim.tv2.sources

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusGroup
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Source
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import app.nudroidlabs.nustrim.core.model.MediaEpisode
import app.nudroidlabs.nustrim.core.model.MediaItem
import app.nudroidlabs.nustrim.core.model.StreamSource
import app.nudroidlabs.nustrim.features.streams.StreamProviderProgress
import coil3.compose.AsyncImage
import kotlinx.coroutines.delay

private object Tv2SourceColors {
    val Background = Color(0xFF090A0D)
    val Surface = Color(0xFF15171C)
    val Raised = Color(0xFF20232A)
    val Text = Color(0xFFF5F6F8)
    val Muted = Color(0xFFADB1BB)
    val Error = Color(0xFFFFB6BE)
}

@Composable
fun Tv2StreamSourcePicker(
    title: String,
    item: MediaItem,
    episode: MediaEpisode?,
    resumePositionMs: Long,
    streams: List<StreamSource>,
    providerProgress: List<StreamProviderProgress>,
    loading: Boolean,
    progress: String,
    error: String,
    onDismiss: () -> Unit,
    onRefresh: () -> Unit,
    onSelect: (StreamSource) -> Unit
) {
    BackHandler(onBack = onDismiss)

    val sortedStreams = remember(streams) {
        streams.sortedWith(
            compareBy<StreamSource> { it.providerName.lowercase() }
                .thenBy { it.name.lowercase() }
        )
    }
    val providerStates = remember(providerProgress) {
        providerProgress
            .filter { it.loading || it.hasSources || it.failed }
            .groupBy { it.name.ifBlank { "Other" } }
            .mapValues { (_, states) ->
                states.reduce { first, next ->
                    first.copy(
                        loading = first.loading || next.loading,
                        hasSources = first.hasSources || next.hasSources,
                        failed = first.failed && next.failed
                    )
                }
            }
    }
    val providers = remember(sortedStreams, providerStates) {
        listOf("All") + (
            sortedStreams.map { it.providerName.ifBlank { "Other" } } + providerStates.keys
        ).distinct().sorted()
    }
    var selectedProvider by remember { mutableStateOf("All") }
    LaunchedEffect(providers) {
        if (selectedProvider !in providers) selectedProvider = "All"
    }

    val visibleStreams = remember(sortedStreams, selectedProvider) {
        if (selectedProvider == "All") sortedStreams
        else sortedStreams.filter {
            it.providerName.ifBlank { "Other" } == selectedProvider
        }
    }

    val closeRequester = remember { FocusRequester() }
    val refreshRequester = remember { FocusRequester() }
    val retryRequester = remember { FocusRequester() }
    val providerRequesters = remember(providers) {
        List(providers.size) { FocusRequester() }
    }
    val sourceKeys = remember(visibleStreams) {
        visibleStreams.map(::tv2SourceStableKey)
    }
    val sourceRequesters = remember(sourceKeys) {
        List(sourceKeys.size) { FocusRequester() }
    }

    var wasLoading by remember { mutableStateOf(loading) }
    LaunchedEffect(Unit) {
        delay(120)
        when {
            visibleStreams.isNotEmpty() -> tv2RequestFocus(sourceRequesters.firstOrNull())
            error.isNotBlank() && !loading -> tv2RequestFocus(retryRequester)
            else -> tv2RequestFocus(closeRequester)
        }
    }
    LaunchedEffect(loading, visibleStreams.size, selectedProvider) {
        if (wasLoading && !loading && visibleStreams.isNotEmpty()) {
            delay(80)
            tv2RequestFocus(sourceRequesters.firstOrNull())
        }
        wasLoading = loading
    }

    val episodeCode = episode?.let {
        if (it.season != null && it.episode != null) "S${it.season} E${it.episode}" else ""
    }.orEmpty()
    val heroTitle = episode?.title?.ifBlank { item.title } ?: item.title
    val artwork = item.backgroundUrl.ifBlank { item.posterUrl }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.72f)),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.90f)
                    .fillMaxHeight(0.84f)
                    .widthIn(max = 1120.dp),
                shape = RoundedCornerShape(22.dp),
                color = Tv2SourceColors.Surface,
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.14f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .focusGroup()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .weight(0.36f)
                            .background(Tv2SourceColors.Background)
                    ) {
                        Tv2SourceArtwork(
                            url = artwork,
                            title = heroTitle,
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(
                            Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color.Black.copy(alpha = 0.06f),
                                            Color.Black.copy(alpha = 0.36f),
                                            Tv2SourceColors.Background.copy(alpha = 0.98f)
                                        )
                                    )
                                )
                        )
                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(26.dp)
                        ) {
                            if (episodeCode.isNotBlank()) {
                                Text(
                                    episodeCode,
                                    color = Tv2SourceColors.Text.copy(alpha = 0.82f),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(Modifier.height(5.dp))
                            }
                            Text(
                                heroTitle,
                                color = Tv2SourceColors.Text,
                                fontSize = 27.sp,
                                lineHeight = 30.sp,
                                fontWeight = FontWeight.Black,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (episode != null) {
                                Spacer(Modifier.height(5.dp))
                                Text(
                                    item.title,
                                    color = Tv2SourceColors.Muted,
                                    fontSize = 13.sp,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            if (resumePositionMs > 8_000L) {
                                Spacer(Modifier.height(12.dp))
                                Surface(
                                    shape = RoundedCornerShape(18.dp),
                                    color = Color.White.copy(alpha = 0.10f)
                                ) {
                                    Text(
                                        "Resume from ${tv2FormatTime(resumePositionMs)}",
                                        modifier = Modifier.padding(horizontal = 13.dp, vertical = 8.dp),
                                        color = Tv2SourceColors.Text,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxHeight()
                            .weight(0.64f)
                            .padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    "Choose source",
                                    color = Tv2SourceColors.Text,
                                    fontSize = 25.sp,
                                    fontWeight = FontWeight.Black
                                )
                                Text(
                                    when {
                                        loading -> progress.ifBlank { "Finding playable sources..." }
                                        visibleStreams.isEmpty() -> "No playable sources yet"
                                        else -> "${visibleStreams.size} playable source${if (visibleStreams.size == 1) "" else "s"}"
                                    },
                                    color = Tv2SourceColors.Muted,
                                    fontSize = 12.sp
                                )
                            }

                            Tv2SourceHeaderButton(
                                label = "Refresh",
                                refresh = true,
                                requester = refreshRequester,
                                enabled = !loading,
                                onClick = onRefresh
                            )
                            Spacer(Modifier.width(8.dp))
                            Tv2SourceHeaderButton(
                                label = "Close",
                                refresh = false,
                                requester = closeRequester,
                                enabled = true,
                                onClick = onDismiss
                            )
                        }

                        if (providers.size > 1) {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                contentPadding = PaddingValues(vertical = 2.dp)
                            ) {
                                items(providers.indices.toList(), key = { providers[it] }) { index ->
                                    val provider = providers[index]
                                    val state = providerStates[provider]
                                    Tv2ProviderChip(
                                        label = provider,
                                        selected = selectedProvider == provider,
                                        loading = state?.loading == true,
                                        failed = state?.let { it.failed && !it.hasSources } == true,
                                        requester = providerRequesters[index],
                                        onClick = { selectedProvider = provider }
                                    )
                                }
                            }
                        }

                        if (loading) {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(13.dp),
                                color = Color.White.copy(alpha = 0.055f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp,
                                        color = Color.White
                                    )
                                    Spacer(Modifier.width(9.dp))
                                    Text(
                                        progress.ifBlank { "Finding playable sources..." },
                                        color = Tv2SourceColors.Text.copy(alpha = 0.86f),
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                        if (error.isNotBlank() && visibleStreams.isNotEmpty()) {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(13.dp),
                                color = Color(0xFF301D22),
                                border = BorderStroke(
                                    1.dp,
                                    Tv2SourceColors.Error.copy(alpha = 0.30f)
                                )
                            ) {
                                Text(
                                    error,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                    color = Tv2SourceColors.Error,
                                    fontSize = 12.sp,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            shape = RoundedCornerShape(16.dp),
                            color = Color.White.copy(alpha = 0.035f),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.06f))
                        ) {
                            when {
                                !loading && visibleStreams.isEmpty() -> {
                                    Tv2EmptySources(
                                        error = error,
                                        requester = retryRequester,
                                        onRetry = onRefresh
                                    )
                                }
                                visibleStreams.isEmpty() -> {
                                    Box(
                                        Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            CircularProgressIndicator(
                                                Modifier.size(28.dp),
                                                strokeWidth = 2.dp,
                                                color = Color.White
                                            )
                                            Text(
                                                "Waiting for sources...",
                                                color = Tv2SourceColors.Muted,
                                                fontSize = 13.sp
                                            )
                                        }
                                    }
                                }
                                else -> {
                                    LazyColumn(
                                        modifier = Modifier.fillMaxSize(),
                                        contentPadding = PaddingValues(10.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        items(
                                            visibleStreams.indices.toList(),
                                            key = { sourceKeys[it] }
                                        ) { index ->
                                            Tv2SourceRow(
                                                source = visibleStreams[index],
                                                requester = sourceRequesters[index],
                                                onSelect = onSelect
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Tv2SourceHeaderButton(
    label: String,
    refresh: Boolean,
    requester: FocusRequester,
    enabled: Boolean,
    onClick: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    Surface(
        modifier = Modifier
            .focusRequester(requester)
            .onFocusChanged { focused = it.isFocused }
            .onPreviewKeyEvent { event ->
                tv2Activate(event, enabled, onClick)
            }
            .clickable(enabled = enabled, onClick = onClick)
            .focusable(enabled),
        shape = RoundedCornerShape(13.dp),
        color = when {
            !enabled -> Color.White.copy(alpha = 0.04f)
            focused -> Color.White
            else -> Color.White.copy(alpha = 0.08f)
        },
        border = BorderStroke(
            if (focused) 2.dp else 1.dp,
            if (focused) Color.White else Color.White.copy(alpha = 0.08f)
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                if (refresh) Icons.Outlined.Refresh else Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = null,
                tint = if (focused) Color(0xFF17191E) else Tv2SourceColors.Text,
                modifier = Modifier.size(17.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                label,
                color = if (focused) Color(0xFF17191E) else Tv2SourceColors.Text,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun Tv2ProviderChip(
    label: String,
    selected: Boolean,
    loading: Boolean,
    failed: Boolean,
    requester: FocusRequester,
    onClick: () -> Unit
) {
    var focused by remember(label) { mutableStateOf(false) }
    Surface(
        modifier = Modifier
            .focusRequester(requester)
            .onFocusChanged { focused = it.isFocused }
            .onPreviewKeyEvent { event -> tv2Activate(event, true, onClick) }
            .clickable(onClick = onClick)
            .focusable(),
        shape = RoundedCornerShape(17.dp),
        color = when {
            focused -> Color.White
            selected -> Color(0xFF343740)
            else -> Color.White.copy(alpha = 0.06f)
        },
        border = BorderStroke(
            if (focused) 2.dp else 1.dp,
            if (focused) Color.White else Color.White.copy(alpha = 0.08f)
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 13.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                label,
                color = when {
                    focused -> Color(0xFF17191E)
                    failed -> Tv2SourceColors.Error
                    else -> Tv2SourceColors.Text
                },
                fontSize = 12.sp,
                fontWeight = if (selected || focused) FontWeight.Bold else FontWeight.Medium
            )
            if (loading) {
                Spacer(Modifier.width(7.dp))
                CircularProgressIndicator(
                    modifier = Modifier.size(11.dp),
                    strokeWidth = 1.5.dp,
                    color = if (focused) Color(0xFF17191E) else Color.White
                )
            }
        }
    }
}

@Composable
private fun Tv2SourceRow(
    source: StreamSource,
    requester: FocusRequester,
    onSelect: (StreamSource) -> Unit
) {
    var focused by remember(tv2SourceStableKey(source)) { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (focused) 1.012f else 1f,
        animationSpec = tween(120),
        label = "tv2-source-row-scale"
    )
    val play = { onSelect(source) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .focusRequester(requester)
            .onFocusChanged { focused = it.isFocused }
            .onPreviewKeyEvent { event -> tv2Activate(event, true, play) }
            .clickable(onClick = play)
            .focusable(),
        shape = RoundedCornerShape(13.dp),
        color = if (focused) Color.White else Tv2SourceColors.Raised,
        border = BorderStroke(
            if (focused) 2.dp else 1.dp,
            if (focused) Color.White else Color.White.copy(alpha = 0.05f)
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(
                        if (focused) Color(0xFF1B1D22) else Color.White.copy(alpha = 0.08f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.PlayArrow,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(21.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        source.providerName.ifBlank { "Stream" },
                        modifier = Modifier.weight(1f),
                        color = if (focused) Color(0xFF17191E) else Tv2SourceColors.Text,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        source.type.uppercase(),
                        color = if (focused) Color(0xFF5A5F69) else Tv2SourceColors.Muted,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (source.name.isNotBlank() && source.name != source.providerName) {
                    Text(
                        source.name,
                        color = if (focused) Color(0xFF343840) else Tv2SourceColors.Text.copy(alpha = 0.82f),
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                val meta = buildList {
                    if (source.subtitles.isNotEmpty()) add("${source.subtitles.size} subtitles")
                    if (source.note.isNotBlank()) add(source.note)
                }.joinToString("  •  ")
                if (meta.isNotBlank()) {
                    Text(
                        meta,
                        color = if (focused) Color(0xFF656A73) else Tv2SourceColors.Muted,
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun Tv2EmptySources(
    error: String,
    requester: FocusRequester,
    onRetry: () -> Unit
) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier.fillMaxWidth(0.72f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                Icons.Outlined.Source,
                contentDescription = null,
                tint = Tv2SourceColors.Muted,
                modifier = Modifier.size(34.dp)
            )
            Text(
                if (error.isNotBlank()) "Could not load sources" else "No playable sources",
                color = Tv2SourceColors.Text,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                error.ifBlank { "The enabled addons did not return a playable source." },
                color = Tv2SourceColors.Muted,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis
            )
            var focused by remember { mutableStateOf(false) }
            Surface(
                modifier = Modifier
                    .focusRequester(requester)
                    .onFocusChanged { focused = it.isFocused }
                    .onPreviewKeyEvent { event -> tv2Activate(event, true, onRetry) }
                    .clickable(onClick = onRetry)
                    .focusable(),
                shape = RoundedCornerShape(13.dp),
                color = if (focused) Color.White else Color.White.copy(alpha = 0.09f),
                border = BorderStroke(
                    if (focused) 2.dp else 1.dp,
                    if (focused) Color.White else Color.White.copy(alpha = 0.10f)
                )
            ) {
                Text(
                    "Try again",
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                    color = if (focused) Color(0xFF17191E) else Tv2SourceColors.Text,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun Tv2SourceArtwork(
    url: String,
    title: String,
    modifier: Modifier
) {
    if (url.isBlank()) {
        Box(
            modifier = modifier.background(Tv2SourceColors.Background),
            contentAlignment = Alignment.Center
        ) {
            Text(
                title.take(1).uppercase().ifBlank { "N" },
                color = Tv2SourceColors.Muted,
                fontSize = 32.sp,
                fontWeight = FontWeight.Black
            )
        }
    } else {
        AsyncImage(
            model = url,
            contentDescription = title,
            modifier = modifier,
            contentScale = ContentScale.Crop
        )
    }
}

private fun tv2Activate(
    event: androidx.compose.ui.input.key.KeyEvent,
    enabled: Boolean,
    onClick: () -> Unit
): Boolean {
    if (!enabled) return false
    val activate = event.key == Key.DirectionCenter || event.key == Key.Enter
    if (!activate) return false
    if (event.type == KeyEventType.KeyUp) onClick()
    return true
}

private fun tv2RequestFocus(requester: FocusRequester?): Boolean {
    if (requester == null) return false
    return runCatching {
        requester.requestFocus()
        true
    }.getOrDefault(false)
}

private fun tv2SourceStableKey(source: StreamSource): String =
    "${source.providerId}|${source.url}|${source.name}|${source.headers.hashCode()}"

private fun tv2FormatTime(positionMs: Long): String {
    val totalSeconds = (positionMs / 1000L).coerceAtLeast(0L)
    val hours = totalSeconds / 3600L
    val minutes = (totalSeconds % 3600L) / 60L
    val seconds = totalSeconds % 60L
    return if (hours > 0L) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%d:%02d".format(minutes, seconds)
    }
}
