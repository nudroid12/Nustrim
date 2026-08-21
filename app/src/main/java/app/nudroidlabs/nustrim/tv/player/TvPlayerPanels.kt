package app.nudroidlabs.nustrim.tv.player

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.nudroidlabs.nustrim.tv.episode.TvCanonicalEpisode
import app.nudroidlabs.nustrim.tv.episode.TvEpisodeCatalogue
import app.nudroidlabs.nustrim.tv.sources.TvSourceStream
import app.nudroidlabs.nustrim.tv.sources.TvSourcesSnapshot
import coil3.compose.AsyncImage
import kotlinx.coroutines.delay

@Composable
fun TvPlayerEpisodesPanel(
    catalogue: TvEpisodeCatalogue,
    currentEpisodeId: String?,
    onEpisodeSelected: (TvCanonicalEpisode) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (catalogue.seasons.isEmpty()) {
        TvPlayerSidePanelScaffold(title = "Episodes", modifier = modifier) {
            Text("No episodes available", color = Color.White.copy(alpha = 0.72f))
        }
        return
    }

    val currentSeasonIndex = catalogue.seasons.indexOfFirst { season ->
        season.episodes.any { it.providerEpisodeId == currentEpisodeId }
    }.takeIf { it >= 0 } ?: catalogue.firstRegularSeasonIndex

    var selectedSeasonIndex by remember(catalogue.parentKey) {
        mutableIntStateOf(currentSeasonIndex.coerceIn(catalogue.seasons.indices))
    }
    var pendingSeasonIndex by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(pendingSeasonIndex) {
        val pending = pendingSeasonIndex ?: return@LaunchedEffect
        delay(150)
        if (pendingSeasonIndex == pending) selectedSeasonIndex = pending
    }

    val season = catalogue.seasons[selectedSeasonIndex]

    TvPlayerSidePanelScaffold(
        title = "Episodes",
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            catalogue.seasons.forEachIndexed { index, item ->
                TvPanelChip(
                    text = item.label,
                    selected = index == selectedSeasonIndex,
                    onFocus = { pendingSeasonIndex = index },
                    onClick = { selectedSeasonIndex = index },
                )
            }
        }
        Spacer(Modifier.height(16.dp))

        val requesters = remember(season.stableKey, season.episodes.size) {
            season.episodes.map { FocusRequester() }
        }
        val currentIndex = season.episodes.indexOfFirst { it.providerEpisodeId == currentEpisodeId }
            .takeIf { it >= 0 }
            ?: 0
        LaunchedEffect(season.stableKey) {
            delay(180)
            requesters.getOrNull(currentIndex)?.requestFocus()
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            itemsIndexed(
                items = season.episodes,
                key = { _, item -> item.identity.stableKey },
            ) { index, episode ->
                TvEpisodePanelRow(
                    episode = episode,
                    current = episode.providerEpisodeId == currentEpisodeId,
                    focusRequester = requesters[index],
                    onClick = { onEpisodeSelected(episode) },
                )
            }
        }
    }
}

@Composable
fun TvPlayerSourcesPanel(
    snapshot: TvSourcesSnapshot?,
    currentRequest: TvPlaybackRequest,
    loading: Boolean,
    error: String?,
    onRefresh: () -> Unit,
    onSourceSelected: (TvSourceStream) -> Unit,
    modifier: Modifier = Modifier,
) {
    TvPlayerSidePanelScaffold(
        title = "Sources",
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = currentRequest.streamSourceLabel.ifBlank { "Current source" },
                color = Color.White.copy(alpha = 0.72f),
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            TvPanelAction(
                label = "Refresh",
                icon = Icons.Default.Refresh,
                onClick = onRefresh,
            )
        }
        Spacer(Modifier.height(14.dp))

        when {
            loading && snapshot == null -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color.White)
                }
            }
            snapshot?.streams.isNullOrEmpty() -> {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("No alternate streams", color = Color.White, fontWeight = FontWeight.SemiBold)
                    error?.takeIf { it.isNotBlank() }?.let {
                        Text(it, color = Color.White.copy(alpha = 0.65f), fontSize = 12.sp)
                    }
                    snapshot?.attempts
                        ?.take(6)
                        ?.forEach { attempt ->
                            Text(
                                text = "${attempt.sourceLabel}: ${attempt.streamCount} stream(s)${attempt.message.takeIf { it.isNotBlank() }?.let { " · $it" }.orEmpty()}",
                                color = Color.White.copy(alpha = 0.58f),
                                fontSize = 11.sp,
                            )
                        }
                }
            }
            else -> {
                val streams = snapshot!!.streams.filter { it.playable }
                val currentIndex = streams.indexOfFirst {
                    it.stream.url == currentRequest.stream.url &&
                        it.sourceLabel == currentRequest.streamSourceLabel
                }.takeIf { it >= 0 } ?: 0
                val requesters = remember(snapshot.streams, currentRequest.stableKey) {
                    streams.map { FocusRequester() }
                }
                LaunchedEffect(snapshot, currentRequest.stableKey) {
                    delay(180)
                    requesters.getOrNull(currentIndex)?.requestFocus()
                }
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    itemsIndexed(
                        items = streams,
                        key = { _, item -> item.stableKey },
                    ) { index, item ->
                        TvSourcePanelRow(
                            source = item,
                            current = item.stream.url == currentRequest.stream.url &&
                                item.sourceLabel == currentRequest.streamSourceLabel,
                            focusRequester = requesters[index],
                            onClick = { onSourceSelected(item) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TvPlayerAudioPanel(
    tracks: List<TvPlayerTrack>,
    onSelect: (TvPlayerTrack) -> Unit,
    modifier: Modifier = Modifier,
) {
    val requesters = remember(tracks.map { it.key }) { tracks.map { FocusRequester() } }
    val selectedIndex = tracks.indexOfFirst { it.selected }.takeIf { it >= 0 } ?: 0
    LaunchedEffect(tracks.map { it.key }) {
        delay(160)
        requesters.getOrNull(selectedIndex)?.requestFocus()
    }
    TvPlayerSidePanelScaffold(title = "Audio", modifier = modifier) {
        if (tracks.isEmpty()) {
            Text("No selectable audio tracks", color = Color.White.copy(alpha = 0.72f))
        } else {
            TvTrackList(tracks = tracks, requesters = requesters, onSelect = onSelect)
        }
    }
}

@Composable
fun TvPlayerSubtitlePanel(
    tracks: List<TvPlayerTrack>,
    onDisable: () -> Unit,
    onSelect: (TvPlayerTrack) -> Unit,
    modifier: Modifier = Modifier,
) {
    val offRequester = remember { FocusRequester() }
    val requesters = remember(tracks.map { it.key }) { tracks.map { FocusRequester() } }
    val selectedIndex = tracks.indexOfFirst { it.selected }
    LaunchedEffect(tracks.map { it.key }) {
        delay(160)
        if (selectedIndex >= 0) {
            requesters.getOrNull(selectedIndex)?.requestFocus()
        } else {
            offRequester.requestFocus()
        }
    }
    TvPlayerSidePanelScaffold(title = "Subtitles", modifier = modifier) {
        TvPanelTextRow(
            title = "Off",
            subtitle = "Disable subtitles",
            selected = tracks.none { it.selected },
            focusRequester = offRequester,
            onClick = onDisable,
        )
        Spacer(Modifier.height(8.dp))
        if (tracks.isEmpty()) {
            Text("No subtitle tracks", color = Color.White.copy(alpha = 0.72f))
        } else {
            TvTrackList(tracks = tracks, requesters = requesters, onSelect = onSelect)
        }
    }
}

@Composable
fun TvPlayerSpeedPanel(
    currentSpeed: Float,
    onSelect: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val speeds = remember { listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f) }
    val requesters = remember { speeds.map { FocusRequester() } }
    val selectedIndex = speeds.indices.minByOrNull { index ->
        kotlin.math.abs(currentSpeed - speeds[index])
    } ?: 2
    LaunchedEffect(currentSpeed) {
        delay(160)
        requesters.getOrNull(selectedIndex)?.requestFocus()
    }
    TvPlayerSidePanelScaffold(title = "Playback speed", modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            speeds.forEachIndexed { index, speed ->
                TvPanelTextRow(
                    title = if (speed == 1f) "Normal" else String.format(java.util.Locale.US, "%.2fx", speed),
                    subtitle = "",
                    selected = kotlin.math.abs(currentSpeed - speed) < 0.01f,
                    focusRequester = requesters[index],
                    onClick = { onSelect(speed) },
                )
            }
        }
    }
}

@Composable
private fun TvTrackList(
    tracks: List<TvPlayerTrack>,
    requesters: List<FocusRequester>,
    onSelect: (TvPlayerTrack) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        tracks.forEachIndexed { index, track ->
            TvPanelTextRow(
                title = track.label,
                subtitle = track.language.uppercase(),
                selected = track.selected,
                focusRequester = requesters.getOrNull(index),
                onClick = { onSelect(track) },
            )
        }
    }
}

@Composable
private fun TvEpisodePanelRow(
    episode: TvCanonicalEpisode,
    current: Boolean,
    focusRequester: FocusRequester,
    onClick: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(focusRequester)
            .onFocusChanged { focused = it.isFocused }
            .clip(RoundedCornerShape(14.dp))
            .background(
                when {
                    focused -> Color.White
                    current -> Color.White.copy(alpha = 0.15f)
                    else -> Color.White.copy(alpha = 0.06f)
                },
            )
            .border(
                1.dp,
                if (focused) Color.White else Color.White.copy(alpha = 0.15f),
                RoundedCornerShape(14.dp),
            )
            .clickable(onClick = onClick)
            .focusable()
            .padding(10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = episode.thumbnailUrl,
            contentDescription = null,
            modifier = Modifier
                .width(116.dp)
                .height(66.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(Color.Black.copy(alpha = 0.35f)),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "${episode.coordinateLabel} · ${episode.title}",
                color = if (focused) Color.Black else Color.White,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (episode.overview.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = episode.overview,
                    color = if (focused) Color.Black.copy(alpha = 0.70f) else Color.White.copy(alpha = 0.62f),
                    fontSize = 11.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (current) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Current episode",
                tint = if (focused) Color.Black else Color.White,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun TvSourcePanelRow(
    source: TvSourceStream,
    current: Boolean,
    focusRequester: FocusRequester,
    onClick: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(focusRequester)
            .onFocusChanged { focused = it.isFocused }
            .clip(RoundedCornerShape(14.dp))
            .background(
                when {
                    focused -> Color.White
                    current -> Color.White.copy(alpha = 0.15f)
                    else -> Color.White.copy(alpha = 0.06f)
                },
            )
            .border(
                1.dp,
                if (focused) Color.White else Color.White.copy(alpha = 0.14f),
                RoundedCornerShape(14.dp),
            )
            .clickable(onClick = onClick)
            .focusable()
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = source.stream.name.ifBlank { source.sourceLabel },
                color = if (focused) Color.Black else Color.White,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = buildString {
                    append(source.sourceLabel)
                    if (source.qualityLabel.isNotBlank()) append(" · ${source.qualityLabel}")
                    if (source.transportLabel.isNotBlank()) append(" · ${source.transportLabel}")
                },
                color = if (focused) Color.Black.copy(alpha = 0.70f) else Color.White.copy(alpha = 0.62f),
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (current) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Current source",
                tint = if (focused) Color.Black else Color.White,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun TvPanelTextRow(
    title: String,
    subtitle: String,
    selected: Boolean,
    focusRequester: FocusRequester? = null,
    onClick: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .onFocusChanged { focused = it.isFocused }
            .clip(RoundedCornerShape(12.dp))
            .background(
                when {
                    focused -> Color.White
                    selected -> Color.White.copy(alpha = 0.15f)
                    else -> Color.White.copy(alpha = 0.06f)
                },
            )
            .clickable(onClick = onClick)
            .focusable()
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                color = if (focused) Color.Black else Color.White,
                fontWeight = FontWeight.Medium,
            )
            subtitle.takeIf { it.isNotBlank() }?.let {
                Spacer(Modifier.height(2.dp))
                Text(
                    it,
                    color = if (focused) Color.Black.copy(alpha = 0.65f) else Color.White.copy(alpha = 0.56f),
                    fontSize = 11.sp,
                )
            }
        }
        if (selected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Selected",
                tint = if (focused) Color.Black else Color.White,
            )
        }
    }
}

@Composable
private fun TvPanelChip(
    text: String,
    selected: Boolean,
    onFocus: () -> Unit,
    onClick: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .onFocusChanged {
                focused = it.isFocused
                if (it.isFocused) onFocus()
            }
            .clip(RoundedCornerShape(99.dp))
            .background(
                when {
                    focused -> Color.White
                    selected -> Color.White.copy(alpha = 0.20f)
                    else -> Color.White.copy(alpha = 0.08f)
                },
            )
            .clickable(onClick = onClick)
            .focusable()
            .padding(horizontal = 15.dp, vertical = 9.dp),
    ) {
        Text(
            text = text,
            color = if (focused) Color.Black else Color.White,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            fontSize = 12.sp,
            maxLines = 1,
        )
    }
}

@Composable
private fun TvPanelAction(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .onFocusChanged { focused = it.isFocused }
            .clip(RoundedCornerShape(99.dp))
            .background(if (focused) Color.White else Color.White.copy(alpha = 0.08f))
            .clickable(onClick = onClick)
            .focusable()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = label, tint = if (focused) Color.Black else Color.White, modifier = Modifier.size(16.dp))
        Text(label, color = if (focused) Color.Black else Color.White, fontSize = 11.sp)
    }
}

@Composable
private fun TvPlayerSidePanelScaffold(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.horizontalGradient(
                    listOf(Color.Transparent, Color.Black.copy(alpha = 0.45f), Color.Black.copy(alpha = 0.94f)),
                ),
            ),
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .width(580.dp)
                .background(Color(0xEF101114))
                .padding(horizontal = 26.dp, vertical = 28.dp),
        ) {
            Text(
                text = title,
                color = Color.White,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(18.dp))
            content()
        }
    }
}
