package app.nudroidlabs.nustrim.tv.player

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.runtime.mutableStateMapOf
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
import androidx.compose.ui.layout.ContentScale
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
    var pendingSeasonIndex by remember(catalogue.parentKey) { mutableStateOf<Int?>(null) }
    val lastFocusedEpisodeBySeason = remember(catalogue.parentKey) {
        mutableStateMapOf<String, String>()
    }
    val seasonRequesters = remember(catalogue.parentKey, catalogue.seasons.size) {
        catalogue.seasons.map { FocusRequester() }
    }

    LaunchedEffect(currentEpisodeId, catalogue.parentKey) {
        val currentSeason = catalogue.seasons.firstOrNull { candidate ->
            candidate.episodes.any { it.providerEpisodeId == currentEpisodeId }
        }
        val currentEpisode = currentSeason?.episodes?.firstOrNull {
            it.providerEpisodeId == currentEpisodeId
        }
        if (currentSeason != null && currentEpisode != null) {
            lastFocusedEpisodeBySeason[currentSeason.stableKey] = currentEpisode.identity.stableKey
        }
    }

    LaunchedEffect(pendingSeasonIndex) {
        val pending = pendingSeasonIndex ?: return@LaunchedEffect
        delay(150)
        if (pendingSeasonIndex == pending) {
            selectedSeasonIndex = pending.coerceIn(catalogue.seasons.indices)
        }
    }

    val season = catalogue.seasons[selectedSeasonIndex]
    val episodeRequesters = remember(season.stableKey, season.episodes.size) {
        season.episodes.map { FocusRequester() }
    }
    val initialEpisodeIndex = season.episodes.indexOfFirst {
        it.providerEpisodeId == currentEpisodeId
    }.takeIf { it >= 0 } ?: 0

    LaunchedEffect(catalogue.parentKey, currentEpisodeId) {
        delay(180)
        episodeRequesters.getOrNull(initialEpisodeIndex)?.requestFocus()
    }

    fun rememberEpisode(episode: TvCanonicalEpisode) {
        lastFocusedEpisodeBySeason[season.stableKey] = episode.identity.stableKey
    }

    fun restoreEpisodeForActiveSeason(): Boolean {
        val rememberedKey = lastFocusedEpisodeBySeason[season.stableKey]
            ?: season.episodes.firstOrNull { it.providerEpisodeId == currentEpisodeId }
                ?.identity?.stableKey
            ?: season.episodes.firstOrNull()?.identity?.stableKey
            ?: return false
        val index = season.episodes.indexOfFirst { it.identity.stableKey == rememberedKey }
            .takeIf { it >= 0 }
            ?: return false
        return runCatching {
            episodeRequesters[index].requestFocus()
            true
        }.getOrDefault(false)
    }

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
                    focusRequester = seasonRequesters[index],
                    onFocus = { pendingSeasonIndex = index },
                    onDown = if (index == selectedSeasonIndex) {
                        { restoreEpisodeForActiveSeason() }
                    } else {
                        null
                    },
                    onClick = {
                        pendingSeasonIndex = null
                        selectedSeasonIndex = index
                    },
                )
            }
        }
        Spacer(Modifier.height(16.dp))

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
                    focusRequester = episodeRequesters[index],
                    upFocusRequester = seasonRequesters[selectedSeasonIndex],
                    onFocused = { rememberEpisode(episode) },
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
    var selectedFilter by remember(snapshot?.sourceLabels, currentRequest.mediaKey) {
        mutableStateOf<String?>(null)
    }
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
                color = Color.White.copy(alpha = 0.68f),
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(10.dp))
            TvPanelAction(
                label = "Reload",
                icon = Icons.Default.Refresh,
                onClick = onRefresh,
            )
        }
        val labels = snapshot?.sourceLabels.orEmpty()
        if (labels.isNotEmpty()) {
            Spacer(Modifier.height(14.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 2.dp),
            ) {
                item {
                    TvPanelChip(
                        text = "All",
                        selected = selectedFilter == null,
                        onFocus = {},
                        onClick = { selectedFilter = null },
                    )
                }
                items(labels, key = { it }) { label ->
                    TvPanelChip(
                        text = label,
                        selected = selectedFilter == label,
                        onFocus = {},
                        onClick = { selectedFilter = label },
                    )
                }
            }
        }
        Spacer(Modifier.height(16.dp))

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
                val streams = snapshot!!.filtered(selectedFilter).filter { it.playable }
                if (streams.isEmpty()) {
                    Text("No streams for this source", color = Color.White.copy(alpha = 0.70f))
                } else {
                    val currentIndex = streams.indexOfFirst {
                        it.stream.url == currentRequest.stream.url &&
                            it.sourceLabel == currentRequest.streamSourceLabel
                    }.takeIf { it >= 0 } ?: 0
                    val requesters = remember(streams.map { it.stableKey }, currentRequest.stableKey) {
                        streams.map { FocusRequester() }
                    }
                    LaunchedEffect(streams.map { it.stableKey }, currentRequest.stableKey) {
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
        delay(180)
        requesters.getOrNull(selectedIndex)?.requestFocus()
    }
    TvPlayerBottomOverlayScaffold(title = "Audio", width = 620.dp, modifier = modifier) {
        if (tracks.isEmpty()) {
            Text("No selectable audio tracks", color = Color.White.copy(alpha = 0.72f))
        } else {
            Text(
                text = "Tracks",
                color = Color.White.copy(alpha = 0.54f),
                fontSize = 12.sp,
            )
            Spacer(Modifier.height(8.dp))
            TvTrackList(
                tracks = tracks,
                requesters = requesters,
                onSelect = onSelect,
                modifier = Modifier.width(540.dp).height(330.dp),
            )
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
    val languages = tracks
        .map { it.language.trim() }
        .filter { it.isNotBlank() }
        .distinct()
    val showLanguageRail = languages.size > 1

    LaunchedEffect(tracks.map { it.key }) {
        delay(180)
        if (selectedIndex >= 0) {
            requesters.getOrNull(selectedIndex)?.requestFocus()
        } else {
            offRequester.requestFocus()
        }
    }

    TvPlayerBottomOverlayScaffold(title = "Subtitles", width = 760.dp, modifier = modifier) {
        if (tracks.isEmpty()) {
            TvPanelTextRow(
                title = "Off",
                subtitle = "No subtitle tracks available",
                selected = true,
                focusRequester = offRequester,
                onClick = onDisable,
            )
        } else {
            Row(
                horizontalArrangement = Arrangement.spacedBy(18.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Column(
                    modifier = Modifier.width(if (showLanguageRail) 220.dp else 180.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("Options", color = Color.White.copy(alpha = 0.54f), fontSize = 12.sp)
                    TvPanelTextRow(
                        title = "Off",
                        subtitle = "Disable subtitles",
                        selected = tracks.none { it.selected },
                        focusRequester = offRequester,
                        onClick = onDisable,
                    )
                    if (showLanguageRail) {
                        Spacer(Modifier.height(4.dp))
                        Text("Languages", color = Color.White.copy(alpha = 0.54f), fontSize = 12.sp)
                        languages.take(7).forEach { language ->
                            val isSelected = tracks.any { it.selected && it.language == language }
                            TvPanelTextRow(
                                title = language,
                                subtitle = "",
                                selected = isSelected,
                                onClick = {
                                    tracks.firstOrNull { it.language == language }?.let(onSelect)
                                },
                            )
                        }
                    }
                }
                Column(modifier = Modifier.width(if (showLanguageRail) 500.dp else 540.dp)) {
                    Text("Tracks", color = Color.White.copy(alpha = 0.54f), fontSize = 12.sp)
                    Spacer(Modifier.height(8.dp))
                    TvTrackList(
                        tracks = tracks,
                        requesters = requesters,
                        onSelect = onSelect,
                        modifier = Modifier.height(330.dp),
                    )
                }
            }
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
    TvPlayerBottomOverlayScaffold(title = "Playback speed", width = 380.dp, modifier = modifier) {
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
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(bottom = 8.dp),
    ) {
        itemsIndexed(tracks, key = { _, track -> track.key }) { index, track ->
            TvPanelTextRow(
                title = track.label,
                subtitle = track.language
                    .takeIf { it.isNotBlank() && !it.equals(track.label, ignoreCase = true) }
                    .orEmpty(),
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
    upFocusRequester: FocusRequester,
    onFocused: () -> Unit,
    onClick: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(focusRequester)
            .onFocusChanged {
                focused = it.isFocused
                if (it.isFocused) onFocused()
            }
            .onKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                if (event.key == Key.DirectionUp) {
                    onFocused()
                    runCatching { upFocusRequester.requestFocus() }
                    true
                } else {
                    false
                }
            }
            .clip(RoundedCornerShape(16.dp))
            .background(
                when {
                    focused -> Color.White.copy(alpha = 0.16f)
                    current -> Color.White.copy(alpha = 0.10f)
                    else -> Color.White.copy(alpha = 0.04f)
                },
            )
            .border(
                if (focused) 2.dp else 1.dp,
                if (focused) Color.White else Color.White.copy(alpha = 0.12f),
                RoundedCornerShape(16.dp),
            )
            .clickable(onClick = onClick)
            .focusable()
            .padding(10.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .width(130.dp)
                .height(90.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color.White.copy(alpha = 0.05f)),
        ) {
            AsyncImage(
                model = episode.thumbnailUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(7.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.Black.copy(alpha = 0.76f))
                    .padding(horizontal = 7.dp, vertical = 4.dp),
            ) {
                Text(episode.coordinateLabel, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Medium)
            }
            if (current) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .size(22.dp)
                        .clip(RoundedCornerShape(11.dp))
                        .background(Color.White),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Default.Check, contentDescription = "Current episode", tint = Color.Black, modifier = Modifier.size(14.dp))
                }
            }
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(
                text = episode.title,
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (episode.overview.isNotBlank()) {
                Text(
                    text = episode.overview,
                    color = Color.White.copy(alpha = 0.66f),
                    fontSize = 12.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
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
                    focused -> Color.White.copy(alpha = 0.18f)
                    current -> Color.White.copy(alpha = 0.12f)
                    else -> Color.White.copy(alpha = 0.05f)
                },
            )
            .border(
                1.dp,
                if (focused) Color.White.copy(alpha = 0.92f) else Color.White.copy(alpha = 0.12f),
                RoundedCornerShape(14.dp),
            )
            .clickable(onClick = onClick)
            .focusable()
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = source.stream.name.ifBlank { source.sourceLabel },
                color = Color.White,
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
                color = Color.White.copy(alpha = if (focused) 0.82f else 0.62f),
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (current) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Current source",
                tint = Color.White,
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
                    focused -> Color.White.copy(alpha = 0.18f)
                    selected -> Color.White.copy(alpha = 0.12f)
                    else -> Color.White.copy(alpha = 0.05f)
                },
            )
            .border(
                width = if (focused) 2.dp else 1.dp,
                color = if (focused) Color.White.copy(alpha = 0.92f) else Color.Transparent,
                shape = RoundedCornerShape(12.dp),
            )
            .clickable(onClick = onClick)
            .focusable()
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                color = Color.White,
                fontWeight = FontWeight.Medium,
            )
            subtitle.takeIf { it.isNotBlank() }?.let {
                Spacer(Modifier.height(2.dp))
                Text(
                    it,
                    color = Color.White.copy(alpha = if (focused) 0.78f else 0.56f),
                    fontSize = 11.sp,
                )
            }
        }
        if (selected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Selected",
                tint = Color.White,
            )
        }
    }
}

@Composable
private fun TvPanelChip(
    text: String,
    selected: Boolean,
    focusRequester: FocusRequester? = null,
    onFocus: () -> Unit,
    onDown: (() -> Boolean)? = null,
    onClick: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .then(
                if (focusRequester != null) {
                    Modifier.focusRequester(focusRequester)
                } else {
                    Modifier
                },
            )
            .onFocusChanged {
                focused = it.isFocused
                if (it.isFocused) onFocus()
            }
            .onKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                if (event.key == Key.DirectionDown && onDown != null) {
                    onDown()
                } else {
                    false
                }
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
            .background(Color.Black.copy(alpha = 0.45f)),
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .width(520.dp)
                .clip(RoundedCornerShape(topStart = 20.dp, bottomStart = 20.dp))
                .background(Color(0xF217181D))
                .padding(24.dp),
        ) {
            Text(
                text = title,
                color = Color.White,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Medium,
            )
            Spacer(Modifier.height(18.dp))
            content()
        }
    }
}

@Composable
private fun TvPlayerBottomOverlayScaffold(
    title: String,
    width: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.horizontalGradient(
                    listOf(
                        Color.Black.copy(alpha = 0.84f),
                        Color.Black.copy(alpha = 0.58f),
                        Color.Transparent,
                    ),
                ),
            ),
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .width(width)
                .padding(start = 44.dp, end = 24.dp, bottom = 64.dp),
        ) {
            Text(
                text = title,
                color = Color.White,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Medium,
            )
            Spacer(Modifier.height(14.dp))
            content()
        }
    }
}
