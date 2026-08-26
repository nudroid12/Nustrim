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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
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
import app.nudroidlabs.nustrim.ui.SubtitleDisplayMode
import coil3.compose.AsyncImage
import kotlinx.coroutines.delay

@Composable
fun TvPlayerEpisodesPanel(
    catalogue: TvEpisodeCatalogue,
    currentEpisodeId: String?,
    loading: Boolean,
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

    val seasonKeys = remember(catalogue.seasons) { catalogue.seasons.map { it.stableKey } }
    var selectedSeasonIndex by remember(catalogue.parentKey, seasonKeys) {
        mutableIntStateOf(currentSeasonIndex.coerceIn(catalogue.seasons.indices))
    }
    var pendingSeasonIndex by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(pendingSeasonIndex) {
        val pending = pendingSeasonIndex ?: return@LaunchedEffect
        delay(150)
        if (pendingSeasonIndex == pending) selectedSeasonIndex = pending
    }

    val season = catalogue.seasons[selectedSeasonIndex]
    val seasonRequesters = remember(catalogue.parentKey, seasonKeys) {
        catalogue.seasons.map { FocusRequester() }
    }

    TvPlayerSidePanelScaffold(
        title = "Episodes",
        modifier = modifier,
    ) {
        if (loading) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircularProgressIndicator(
                    color = Color.White,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(22.dp),
                )
                Text(
                    text = "Loading full episode list...",
                    color = Color.White.copy(alpha = 0.78f),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Spacer(Modifier.height(14.dp))
        }
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
                    upFocusRequester = seasonRequesters[selectedSeasonIndex].takeIf { index == 0 },
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
    preferredLanguage: String,
    secondPreferredLanguage: String,
    displayMode: SubtitleDisplayMode,
    fontSizeSp: Int,
    bold: Boolean,
    onFontSizeChange: (Int) -> Unit,
    onBoldChange: (Boolean) -> Unit,
    onDisable: () -> Unit,
    onSelect: (TvPlayerTrack) -> Unit,
    modifier: Modifier = Modifier,
) {
    val offRequester = remember { FocusRequester() }
    val preferredTracks = remember(tracks, preferredLanguage, secondPreferredLanguage, displayMode) {
        if (displayMode == SubtitleDisplayMode.PREFERRED_ONLY) {
            tracks.filter { track ->
                track.languageCode.equals(preferredLanguage, ignoreCase = true) ||
                    track.languageCode.equals(secondPreferredLanguage, ignoreCase = true)
            }
        } else {
            tracks
        }
    }
    val languages = remember(preferredTracks) {
        preferredTracks
            .groupBy { track -> track.language.ifBlank { "Unknown" } }
            .toList()
    }
    val activeLanguage = tracks.firstOrNull { it.selected }?.language?.ifBlank { "Unknown" }
    var selectedLanguage by remember(tracks.map { it.key }, activeLanguage, languages) {
        mutableStateOf(
            activeLanguage?.takeIf { active -> languages.any { it.first == active } }
                ?: languages.firstOrNull()?.first,
        )
    }
    val visibleTracks = remember(preferredTracks, selectedLanguage) {
        preferredTracks.filter { track ->
            track.language.ifBlank { "Unknown" } == selectedLanguage
        }
    }
    val requesters = remember(visibleTracks.map { it.key }) {
        visibleTracks.map { FocusRequester() }
    }
    val selectedIndex = visibleTracks.indexOfFirst { it.selected }

    LaunchedEffect(visibleTracks.map { it.key }, selectedLanguage) {
        delay(180)
        if (selectedIndex >= 0) {
            requesters.getOrNull(selectedIndex)?.requestFocus()
        } else if (requesters.isNotEmpty()) {
            requesters.first().requestFocus()
        } else {
            offRequester.requestFocus()
        }
    }

    TvPlayerBottomOverlayScaffold(
        title = "Subtitles",
        width = 860.dp,
        strongScrim = true,
        modifier = modifier,
    ) {
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
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Column(
                    modifier = Modifier.width(175.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text("Options", color = Color.White.copy(alpha = 0.54f), fontSize = 12.sp)
                    TvPanelTextRow(
                        title = "Off",
                        subtitle = "Disable subtitles",
                        selected = tracks.none { it.selected },
                        focusRequester = offRequester,
                        compact = true,
                        onClick = onDisable,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text("Languages", color = Color.White.copy(alpha = 0.54f), fontSize = 12.sp)
                    if (languages.isEmpty()) {
                        Text(
                            text = if (displayMode == SubtitleDisplayMode.PREFERRED_ONLY) {
                                "No preferred tracks. Choose Show all in Settings."
                            } else {
                                "No subtitle languages"
                            },
                            color = Color.White.copy(alpha = 0.64f),
                            fontSize = 12.sp,
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.height(284.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            contentPadding = PaddingValues(bottom = 6.dp),
                        ) {
                            items(languages, key = { it.first }) { (language, languageTracks) ->
                                TvPanelTextRow(
                                    title = language,
                                    subtitle = "${languageTracks.size} track(s)",
                                    selected = selectedLanguage == language,
                                    compact = true,
                                    onClick = { selectedLanguage = language },
                                )
                            }
                        }
                    }
                }
                Column(modifier = Modifier.width(390.dp)) {
                    Text("Tracks", color = Color.White.copy(alpha = 0.54f), fontSize = 12.sp)
                    Spacer(Modifier.height(8.dp))
                    if (visibleTracks.isEmpty()) {
                        Text(
                            text = "No tracks for this language",
                            color = Color.White.copy(alpha = 0.66f),
                            modifier = Modifier.padding(vertical = 12.dp),
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.height(318.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            contentPadding = PaddingValues(bottom = 6.dp),
                        ) {
                            itemsIndexed(visibleTracks, key = { _, track -> track.key }) { index, track ->
                                TvPanelTextRow(
                                    title = subtitleTrackTitle(track),
                                    subtitle = subtitleTrackDescription(track),
                                    selected = track.selected,
                                    focusRequester = requesters[index],
                                    compact = true,
                                    onClick = { onSelect(track) },
                                )
                            }
                        }
                    }
                }
                Column(
                    modifier = Modifier.width(190.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text("Style", color = Color.White.copy(alpha = 0.54f), fontSize = 12.sp)
                    TvSubtitleSizeControl(
                        fontSizeSp = fontSizeSp,
                        onDecrease = {
                            onFontSizeChange(
                                (fontSizeSp - TvSubtitleStyleStore.FONT_SIZE_STEP)
                                    .coerceAtLeast(TvSubtitleStyleStore.MIN_FONT_SIZE),
                            )
                        },
                        onIncrease = {
                            onFontSizeChange(
                                (fontSizeSp + TvSubtitleStyleStore.FONT_SIZE_STEP)
                                    .coerceAtMost(TvSubtitleStyleStore.MAX_FONT_SIZE),
                            )
                        },
                    )
                    TvPanelTextRow(
                        title = "Bold",
                        subtitle = if (bold) "On" else "Off",
                        selected = bold,
                        compact = true,
                        onClick = { onBoldChange(!bold) },
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
    upFocusRequester: FocusRequester?,
    onClick: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(focusRequester)
            .then(
                if (upFocusRequester != null) Modifier.focusProperties { up = upFocusRequester }
                else Modifier,
            )
            .onFocusChanged { focused = it.isFocused }
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
    compact: Boolean = false,
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
            .padding(horizontal = if (compact) 12.dp else 14.dp, vertical = if (compact) 9.dp else 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                color = Color.White,
                fontWeight = FontWeight.Medium,
                fontSize = if (compact) 14.sp else 16.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            subtitle.takeIf { it.isNotBlank() }?.let {
                Spacer(Modifier.height(2.dp))
                Text(
                    it,
                    color = Color.White.copy(alpha = if (focused) 0.78f else 0.56f),
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
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
private fun TvSubtitleSizeControl(
    fontSizeSp: Int,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xE61B1D22))
            .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(12.dp))
            .padding(10.dp),
    ) {
        Text("Text size", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TvPanelCompactButton(
                label = "−",
                enabled = fontSizeSp > TvSubtitleStyleStore.MIN_FONT_SIZE,
                onClick = onDecrease,
            )
            Text(
                text = "$fontSizeSp sp",
                color = Color.White,
                fontSize = 12.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
            TvPanelCompactButton(
                label = "+",
                enabled = fontSizeSp < TvSubtitleStyleStore.MAX_FONT_SIZE,
                onClick = onIncrease,
            )
        }
    }
}

@Composable
private fun TvPanelCompactButton(
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .size(40.dp)
            .onFocusChanged { focused = it.isFocused }
            .clip(RoundedCornerShape(10.dp))
            .background(
                when {
                    !enabled -> Color.White.copy(alpha = 0.04f)
                    focused -> Color.White
                    else -> Color.White.copy(alpha = 0.10f)
                },
            )
            .border(
                width = if (focused) 2.dp else 1.dp,
                color = if (focused) Color.White else Color.White.copy(alpha = 0.14f),
                shape = RoundedCornerShape(10.dp),
            )
            .clickable(enabled = enabled, onClick = onClick)
            .focusable(enabled)
            .padding(6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = if (focused) Color.Black else Color.White, fontSize = 20.sp)
    }
}

private fun subtitleTrackTitle(track: TvPlayerTrack): String {
    val raw = track.label.trim()
    val alias = TvSubtitleLanguage.canonicalCode(raw)
    return if (raw.isBlank() || alias == track.languageCode.lowercase()) {
        track.language.ifBlank { "Unknown subtitle" }
    } else {
        raw
    }
}

private fun subtitleTrackDescription(track: TvPlayerTrack): String = buildString {
    append(track.provider.ifBlank { "Embedded" })
    if (track.languageCode.isNotBlank()) {
        append(" · ")
        append(track.languageCode.uppercase())
    }
}

@Composable
private fun TvPanelChip(
    text: String,
    selected: Boolean,
    focusRequester: FocusRequester? = null,
    onFocus: () -> Unit,
    onClick: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .then(focusRequester?.let { Modifier.focusRequester(it) } ?: Modifier)
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
    strongScrim: Boolean = false,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.horizontalGradient(
                    if (strongScrim) {
                        listOf(
                            Color.Black.copy(alpha = 0.95f),
                            Color.Black.copy(alpha = 0.78f),
                            Color.Black.copy(alpha = 0.22f),
                        )
                    } else {
                        listOf(
                            Color.Black.copy(alpha = 0.84f),
                            Color.Black.copy(alpha = 0.58f),
                            Color.Transparent,
                        )
                    },
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
