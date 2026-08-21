package app.nudroidlabs.nustrim.tv.details

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.nudroidlabs.nustrim.core.model.MediaType
import app.nudroidlabs.nustrim.tv.episode.TvCanonicalEpisode
import app.nudroidlabs.nustrim.tv.episode.TvEpisodeSeason
import app.nudroidlabs.nustrim.tv.focus.TvFocusRegistry
import app.nudroidlabs.nustrim.tv.focus.TvFocusRestoreEffect
import app.nudroidlabs.nustrim.tv.focus.rememberTvFocusAnchor
import app.nudroidlabs.nustrim.tv.focus.tvFocusAnchor
import coil3.compose.AsyncImage
import kotlinx.coroutines.delay

@Composable
fun TvDetailsScreen(
    state: TvDetailsUiState,
    contentKey: String,
    scopeKey: String,
    focusRegistry: TvFocusRegistry,
    focusRequestToken: Int,
    onRetry: () -> Unit,
    onPlayMovie: (TvDetailsSnapshot) -> Unit,
    onPlayEpisode: (TvDetailsSnapshot, TvCanonicalEpisode) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (state) {
        TvDetailsUiState.Loading -> DetailsLoading(modifier)
        is TvDetailsUiState.Error -> DetailsError(
            message = state.message,
            scopeKey = scopeKey,
            focusRegistry = focusRegistry,
            focusRequestToken = focusRequestToken,
            onRetry = onRetry,
            modifier = modifier,
        )
        is TvDetailsUiState.Ready -> DetailsReady(
            snapshot = state.snapshot,
            contentKey = contentKey,
            scopeKey = scopeKey,
            focusRegistry = focusRegistry,
            focusRequestToken = focusRequestToken,
            onPlayMovie = onPlayMovie,
            onPlayEpisode = onPlayEpisode,
            modifier = modifier,
        )
    }
}

@Composable
private fun DetailsReady(
    snapshot: TvDetailsSnapshot,
    contentKey: String,
    scopeKey: String,
    focusRegistry: TvFocusRegistry,
    focusRequestToken: Int,
    onPlayMovie: (TvDetailsSnapshot) -> Unit,
    onPlayEpisode: (TvDetailsSnapshot, TvCanonicalEpisode) -> Unit,
    modifier: Modifier,
) {
    val item = snapshot.item
    val seasons = snapshot.episodeCatalogue.seasons
    val isSeries = item.type == MediaType.SERIES || seasons.isNotEmpty()
    val memory = remember(contentKey) { TvDetailsSessionStore.memory(contentKey) }
    val initialSeasonIndex = remember(seasons, memory.selectedSeasonKey) {
        val remembered = memory.selectedSeasonKey?.let { key -> seasons.indexOfFirst { it.stableKey == key } } ?: -1
        if (remembered >= 0) remembered else snapshot.episodeCatalogue.firstRegularSeasonIndex
    }.coerceIn(0, (seasons.size - 1).coerceAtLeast(0))
    var selectedSeasonIndex by remember(contentKey, seasons) { mutableIntStateOf(initialSeasonIndex) }
    val selectedSeason = seasons.getOrNull(selectedSeasonIndex)
    var pendingSeasonIndex by remember(contentKey) { mutableStateOf<Int?>(null) }

    LaunchedEffect(pendingSeasonIndex) {
        val target = pendingSeasonIndex ?: return@LaunchedEffect
        delay(SEASON_FOCUS_SETTLE_MS)
        selectedSeasonIndex = target.coerceIn(0, seasons.lastIndex.coerceAtLeast(0))
        memory.selectedSeasonKey = seasons.getOrNull(selectedSeasonIndex)?.stableKey
        pendingSeasonIndex = null
    }

    val fallbackAnchor = HERO_PLAY_ANCHOR

    TvFocusRestoreEffect(
        registry = focusRegistry,
        scopeKey = scopeKey,
        fallbackAnchorKey = fallbackAnchor,
        requestToken = focusRequestToken,
    )

    Box(modifier = modifier.fillMaxSize().background(DETAIL_BACKGROUND)) {
        DetailsBackdrop(item.backgroundUrl.ifBlank { item.posterUrl })
        val listState = rememberLazyListState()
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
        ) {
            item(key = "hero") {
                DetailsHero(
                    snapshot = snapshot,
                    scopeKey = scopeKey,
                    focusRegistry = focusRegistry,
                    isSeries = isSeries,
                    selectedSeason = selectedSeason,
                    onPlay = {
                        if (!isSeries) {
                            onPlayMovie(snapshot)
                        } else {
                            val episode = selectedSeason?.episodes?.firstOrNull()
                                ?: snapshot.episodeCatalogue.episodes.firstOrNull()
                            if (episode != null) onPlayEpisode(snapshot, episode)
                        }
                    },
                )
            }

            if (isSeries && seasons.isNotEmpty()) {
                item(key = "season-tabs") {
                    SeasonTabs(
                        seasons = seasons,
                        selectedIndex = selectedSeasonIndex,
                        scopeKey = scopeKey,
                        focusRegistry = focusRegistry,
                        rememberedEpisodeKey = selectedSeason?.let { season ->
                            memory.lastEpisodeBySeason[season.stableKey]
                        },
                        onSeasonFocused = { pendingSeasonIndex = it },
                        onSeasonSelected = { index ->
                            selectedSeasonIndex = index
                            memory.selectedSeasonKey = seasons.getOrNull(index)?.stableKey
                            pendingSeasonIndex = null
                        },
                    )
                }

                selectedSeason?.let { season ->
                    item(key = "episodes:${season.stableKey}") {
                        key(season.stableKey) {
                            EpisodeRow(
                                season = season,
                                scopeKey = scopeKey,
                                focusRegistry = focusRegistry,
                                rememberedEpisodeKey = memory.lastEpisodeBySeason[season.stableKey],
                                upAnchorKey = seasonAnchorKey(season.stableKey),
                                onFocused = { episode ->
                                    memory.selectedSeasonKey = season.stableKey
                                    memory.lastEpisodeBySeason[season.stableKey] = episode.identity.stableKey
                                },
                                onOpen = { episode -> onPlayEpisode(snapshot, episode) },
                            )
                        }
                    }
                }
            }

            item(key = "details-bottom-space") { Spacer(Modifier.height(88.dp)) }
        }
    }
}

@Composable
private fun DetailsBackdrop(url: String) {
    Box(Modifier.fillMaxSize()) {
        if (url.isNotBlank()) {
            AsyncImage(
                model = url,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                alpha = 0.78f,
            )
        }
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        0f to DETAIL_BACKGROUND.copy(alpha = 0.99f),
                        0.42f to DETAIL_BACKGROUND.copy(alpha = 0.84f),
                        0.72f to DETAIL_BACKGROUND.copy(alpha = 0.28f),
                        1f to Color.Transparent,
                    ),
                ),
        )
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        0.50f to Color.Transparent,
                        0.78f to DETAIL_BACKGROUND.copy(alpha = 0.78f),
                        1f to DETAIL_BACKGROUND,
                    ),
                ),
        )
    }
}

@Composable
private fun DetailsHero(
    snapshot: TvDetailsSnapshot,
    scopeKey: String,
    focusRegistry: TvFocusRegistry,
    isSeries: Boolean,
    selectedSeason: TvEpisodeSeason?,
    onPlay: () -> Unit,
) {
    val item = snapshot.item
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(540.dp)
            .padding(start = 56.dp, end = 56.dp, bottom = 34.dp),
        verticalArrangement = Arrangement.Bottom,
    ) {
        if (item.logoUrl.isNotBlank()) {
            AsyncImage(
                model = item.logoUrl,
                contentDescription = item.title,
                modifier = Modifier
                    .width(430.dp)
                    .height(100.dp),
                contentScale = ContentScale.Fit,
                alignment = Alignment.CenterStart,
            )
        } else {
            Text(
                text = item.title,
                color = Color.White,
                fontSize = 44.sp,
                lineHeight = 48.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(0.62f),
            )
        }
        Spacer(Modifier.height(18.dp))

        val heroEpisode = if (isSeries) selectedSeason?.episodes?.firstOrNull() else null
        PlayButton(
            label = if (heroEpisode != null && heroEpisode.seasonNumber != null && heroEpisode.episodeNumber != null) {
                "Play S${heroEpisode.seasonNumber} E${heroEpisode.episodeNumber}"
            } else {
                "Play"
            },
            scopeKey = scopeKey,
            focusRegistry = focusRegistry,
            onClick = onPlay,
        )
        Spacer(Modifier.height(22.dp))

        val creditLine = when {
            item.director.isNotEmpty() -> (if (isSeries) "Creator: " else "Director: ") + item.director.joinToString(", ")
            item.writer.isNotEmpty() -> "Writer: " + item.writer.joinToString(", ")
            else -> ""
        }
        if (creditLine.isNotBlank()) {
            Text(
                text = creditLine,
                color = Color(0xFFB9BBC1),
                fontSize = 15.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(0.62f),
            )
            Spacer(Modifier.height(10.dp))
        }

        if (item.description.isNotBlank()) {
            Text(
                text = item.description,
                color = Color(0xFFF0F0F2),
                fontSize = 16.sp,
                lineHeight = 22.sp,
                maxLines = 6,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(0.62f),
            )
            Spacer(Modifier.height(12.dp))
        }

        MetaLine(snapshot)
    }
}

@Composable
private fun PlayButton(
    label: String,
    scopeKey: String,
    focusRegistry: TvFocusRegistry,
    onClick: () -> Unit,
) {
    val anchor = rememberTvFocusAnchor(focusRegistry, scopeKey, HERO_PLAY_ANCHOR)
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (focused) 1.035f else 1f, label = "details-play-scale")
    Row(
        modifier = Modifier
            .scale(scale)
            .clip(RoundedCornerShape(28.dp))
            .background(Color.White)
            .border(if (focused) 2.dp else 0.dp, if (focused) Color(0xFFE6E6E8) else Color.Transparent, RoundedCornerShape(28.dp))
            .tvFocusAnchor(anchor)
            .onFocusChanged { focused = it.isFocused }
            .onKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown && (event.key == Key.DirectionCenter || event.key == Key.Enter)) {
                    onClick()
                    true
                } else false
            }
            .focusable()
            .padding(horizontal = 24.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color(0xFF101114), modifier = Modifier.size(20.dp))
        Text(label, color = Color(0xFF101114), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun MetaLine(snapshot: TvDetailsSnapshot) {
    val item = snapshot.item
    val parts = buildList {
        item.releaseInfo.takeIf { it.isNotBlank() }?.let(::add)
        item.runtime.takeIf { it.isNotBlank() }?.let(::add)
        item.rating.takeIf { it.isNotBlank() }?.let { add("★ $it") }
        item.genres.take(3).forEach { add(it) }
    }
    if (parts.isNotEmpty()) {
        Text(
            text = parts.joinToString("  •  "),
            color = Color(0xFFC6C8CD),
            fontSize = 14.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth(0.68f),
        )
    }
}

@Composable
private fun SeasonTabs(
    seasons: List<TvEpisodeSeason>,
    selectedIndex: Int,
    scopeKey: String,
    focusRegistry: TvFocusRegistry,
    rememberedEpisodeKey: String?,
    onSeasonFocused: (Int) -> Unit,
    onSeasonSelected: (Int) -> Unit,
) {
    val initialIndex = selectedIndex.coerceIn(0, seasons.lastIndex.coerceAtLeast(0))
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialIndex)
    LazyRow(
        state = listState,
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 56.dp, vertical = 18.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        itemsIndexed(seasons, key = { _, season -> season.stableKey }) { index, season ->
            SeasonTab(
                season = season,
                selected = index == selectedIndex,
                scopeKey = scopeKey,
                focusRegistry = focusRegistry,
                downAnchorKey = if (index == selectedIndex) {
                    val target = season.episodes.firstOrNull { it.identity.stableKey == rememberedEpisodeKey }
                        ?: season.episodes.firstOrNull()
                    target?.let { episodeAnchorKey(it.identity.stableKey) }
                } else {
                    null
                },
                onFocused = { onSeasonFocused(index) },
                onClick = { onSeasonSelected(index) },
            )
        }
    }
}

@Composable
private fun SeasonTab(
    season: TvEpisodeSeason,
    selected: Boolean,
    scopeKey: String,
    focusRegistry: TvFocusRegistry,
    downAnchorKey: String?,
    onFocused: () -> Unit,
    onClick: () -> Unit,
) {
    val anchorKey = seasonAnchorKey(season.stableKey)
    val anchor = rememberTvFocusAnchor(focusRegistry, scopeKey, anchorKey)
    var focused by remember(anchorKey) { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(
                when {
                    focused -> Color(0xFFF1F1F3)
                    selected -> Color(0xFF292B31)
                    else -> Color(0xFF17191E)
                },
            )
            .border(if (focused) 2.dp else 0.dp, if (focused) Color.White else Color.Transparent, RoundedCornerShape(20.dp))
            .tvFocusAnchor(anchor)
            .onFocusChanged {
                focused = it.isFocused
                if (it.isFocused) onFocused()
            }
            .onKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                when (event.key) {
                    Key.DirectionDown -> {
                        val target = downAnchorKey ?: return@onKeyEvent false
                        focusRegistry.requestAnchor(scopeKey, target)
                    }
                    Key.DirectionCenter, Key.Enter -> {
                        onClick()
                        true
                    }
                    else -> false
                }
            }
            .focusable()
            .padding(horizontal = 20.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = season.label,
            color = if (focused) Color(0xFF101114) else Color.White,
            fontSize = 16.sp,
            fontWeight = if (selected || focused) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}

@Composable
private fun EpisodeRow(
    season: TvEpisodeSeason,
    scopeKey: String,
    focusRegistry: TvFocusRegistry,
    rememberedEpisodeKey: String?,
    upAnchorKey: String,
    onFocused: (TvCanonicalEpisode) -> Unit,
    onOpen: (TvCanonicalEpisode) -> Unit,
) {
    val metrics = rememberEpisodeMetrics()
    val initialIndex = season.episodes.indexOfFirst { it.identity.stableKey == rememberedEpisodeKey }
        .takeIf { it >= 0 }
        ?: 0
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialIndex)
    Column(Modifier.fillMaxWidth()) {
        Text(
            text = season.label,
            color = Color.White,
            fontSize = 22.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(start = metrics.rowPadding, top = 2.dp, bottom = 2.dp),
        )
        LazyRow(
            state = listState,
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = metrics.rowPadding, vertical = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(metrics.gap),
        ) {
            itemsIndexed(
                items = season.episodes,
                key = { _, episode -> episode.identity.stableKey },
            ) { _, episode ->
                EpisodeCard(
                    episode = episode,
                    metrics = metrics,
                    scopeKey = scopeKey,
                    focusRegistry = focusRegistry,
                    upAnchorKey = upAnchorKey,
                    onFocused = { onFocused(episode) },
                    onOpen = { onOpen(episode) },
                )
            }
        }
    }
}

private data class EpisodeMetrics(
    val rowPadding: Dp,
    val gap: Dp,
    val width: Dp,
    val height: Dp,
    val radius: Dp,
    val contentPadding: Dp,
)

@Composable
private fun rememberEpisodeMetrics(): EpisodeMetrics {
    val width = LocalConfiguration.current.screenWidthDp
    return remember(width) {
        when {
            width >= 1300 -> EpisodeMetrics(64.dp, 20.dp, 400.dp, 263.dp, 20.dp, 20.dp)
            width >= 1000 -> EpisodeMetrics(52.dp, 18.dp, 360.dp, 235.dp, 18.dp, 18.dp)
            width >= 760 -> EpisodeMetrics(48.dp, 16.dp, 320.dp, 207.dp, 16.dp, 16.dp)
            else -> EpisodeMetrics(40.dp, 14.dp, 280.dp, 179.dp, 14.dp, 14.dp)
        }
    }
}

@Composable
private fun EpisodeCard(
    episode: TvCanonicalEpisode,
    metrics: EpisodeMetrics,
    scopeKey: String,
    focusRegistry: TvFocusRegistry,
    upAnchorKey: String,
    onFocused: () -> Unit,
    onOpen: () -> Unit,
) {
    val anchorKey = episodeAnchorKey(episode.identity.stableKey)
    val anchor = rememberTvFocusAnchor(focusRegistry, scopeKey, anchorKey)
    var focused by remember(anchorKey) { mutableStateOf(false) }
    val scale by animateFloatAsState(if (focused) 1.03f else 1f, label = "details-episode-scale")
    Box(
        modifier = Modifier
            .width(metrics.width)
            .height(metrics.height)
            .scale(scale)
            .clip(RoundedCornerShape(metrics.radius))
            .background(Color(0xFF17191E))
            .border(if (focused) 2.dp else 1.dp, if (focused) Color.White else Color(0xFF34363D), RoundedCornerShape(metrics.radius))
            .tvFocusAnchor(anchor)
            .onFocusChanged {
                focused = it.isFocused
                if (it.isFocused) onFocused()
            }
            .onKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                when (event.key) {
                    Key.DirectionUp -> focusRegistry.requestAnchor(scopeKey, upAnchorKey)
                    Key.DirectionCenter, Key.Enter -> {
                        onOpen()
                        true
                    }
                    else -> false
                }
            }
            .focusable(),
    ) {
        if (episode.thumbnailUrl.isNotBlank()) {
            AsyncImage(
                model = episode.thumbnailUrl,
                contentDescription = episode.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        }
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        0.34f to Color.Black.copy(alpha = 0.10f),
                        0.70f to Color.Black.copy(alpha = 0.72f),
                        1f to Color.Black.copy(alpha = 0.94f),
                    ),
                ),
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(metrics.contentPadding),
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.Black.copy(alpha = 0.65f))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            ) {
                Text(
                    text = episode.coordinateLabel,
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = episode.title,
                color = Color.White,
                fontSize = 18.sp,
                lineHeight = 21.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (episode.overview.isNotBlank()) {
                Spacer(Modifier.height(5.dp))
                Text(
                    text = episode.overview,
                    color = Color(0xFFD0D1D5),
                    fontSize = 13.sp,
                    lineHeight = 17.sp,
                    maxLines = if (metrics.width >= 360.dp) 3 else 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun DetailsLoading(modifier: Modifier) {
    Box(modifier.fillMaxSize().background(DETAIL_BACKGROUND)) {
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 56.dp, bottom = 72.dp),
        ) {
            Box(Modifier.width(350.dp).height(42.dp).background(Color(0xFF202228), RoundedCornerShape(8.dp)))
            Spacer(Modifier.height(20.dp))
            Box(Modifier.width(130.dp).height(48.dp).background(Color(0xFF292B31), RoundedCornerShape(24.dp)))
            Spacer(Modifier.height(24.dp))
            Box(Modifier.width(520.dp).height(16.dp).background(Color(0xFF202228), RoundedCornerShape(6.dp)))
            Spacer(Modifier.height(8.dp))
            Box(Modifier.width(460.dp).height(16.dp).background(Color(0xFF202228), RoundedCornerShape(6.dp)))
        }
        CircularProgressIndicator(
            modifier = Modifier.align(Alignment.Center).size(34.dp),
            color = Color(0xFFE5E5E7),
            strokeWidth = 2.dp,
        )
    }
}

@Composable
private fun DetailsError(
    message: String,
    scopeKey: String,
    focusRegistry: TvFocusRegistry,
    focusRequestToken: Int,
    onRetry: () -> Unit,
    modifier: Modifier,
) {
    val anchorKey = "details:error:retry"
    val anchor = rememberTvFocusAnchor(focusRegistry, scopeKey, anchorKey)
    var focused by remember { mutableStateOf(false) }
    TvFocusRestoreEffect(
        registry = focusRegistry,
        scopeKey = scopeKey,
        fallbackAnchorKey = anchorKey,
        requestToken = focusRequestToken,
    )
    Column(
        modifier = modifier.fillMaxSize().background(DETAIL_BACKGROUND).padding(horizontal = 56.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Details unavailable", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(10.dp))
        Text(message, color = Color(0xFFB9BBC1), fontSize = 16.sp)
        Spacer(Modifier.height(26.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(24.dp))
                .background(if (focused) Color.White else Color(0xFF292B31))
                .tvFocusAnchor(anchor)
                .onFocusChanged { focused = it.isFocused }
                .onKeyEvent { event ->
                    if (event.type == KeyEventType.KeyDown && (event.key == Key.DirectionCenter || event.key == Key.Enter)) {
                        onRetry()
                        true
                    } else false
                }
                .focusable()
                .padding(horizontal = 24.dp, vertical = 12.dp),
        ) {
            Text("Retry", color = if (focused) Color(0xFF101114) else Color.White, fontWeight = FontWeight.SemiBold)
        }
    }
}

private fun seasonAnchorKey(seasonKey: String) = "details:season:$seasonKey"
private fun episodeAnchorKey(episodeKey: String) = "details:episode:$episodeKey"
private const val HERO_PLAY_ANCHOR = "details:hero:play"
private const val SEASON_FOCUS_SETTLE_MS = 150L
private val DETAIL_BACKGROUND = Color(0xFF08090B)
