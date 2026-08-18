package app.nudroidlabs.nustrim.tv2.details

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.LibraryAdd
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import app.nudroidlabs.nustrim.core.model.MediaEpisode
import app.nudroidlabs.nustrim.core.model.MediaItem
import coil3.compose.AsyncImage

private object Tv2DetailsColors {
    val Background = Color(0xFF090A0D)
    val Surface = Color(0xFF15171C)
    val SurfaceRaised = Color(0xFF1C1F26)
    val Text = Color(0xFFF5F6F8)
    val Muted = Color(0xFFB6BAC4)
    val Soft = Color(0xFFD8DAE0)
}

@Composable
fun Tv2DetailsHero(
    item: MediaItem,
    logoUrl: String,
    primaryLabel: String,
    playbackHint: String,
    loading: Boolean,
    saved: Boolean,
    canSave: Boolean,
    primaryFocusRequester: FocusRequester?,
    saveFocusRequester: FocusRequester?,
    trailerFocusRequester: FocusRequester?,
    onPrimaryFocused: () -> Unit,
    onSaveFocused: () -> Unit,
    onTrailerFocused: () -> Unit,
    onPrimary: () -> Unit,
    onTrailer: (() -> Unit)?,
    onToggleSaved: () -> Unit
) {
    val backdrop = item.backgroundUrl.ifBlank { item.posterUrl }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(410.dp)
            .background(Tv2DetailsColors.Background)
    ) {
        Tv2DetailsArtwork(
            url = backdrop,
            title = item.title,
            modifier = Modifier.fillMaxSize()
        )

        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Tv2DetailsColors.Background.copy(alpha = 0.99f),
                            Tv2DetailsColors.Background.copy(alpha = 0.92f),
                            Tv2DetailsColors.Background.copy(alpha = 0.62f),
                            Tv2DetailsColors.Background.copy(alpha = 0.15f),
                            Color.Transparent
                        )
                    )
                )
        )

        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Transparent,
                            Tv2DetailsColors.Background.copy(alpha = 0.45f),
                            Tv2DetailsColors.Background
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .widthIn(max = 670.dp)
                .padding(start = 40.dp, end = 28.dp, bottom = 34.dp)
        ) {
            if (logoUrl.isNotBlank()) {
                AsyncImage(
                    model = logoUrl,
                    contentDescription = item.title,
                    modifier = Modifier
                        .widthIn(max = 300.dp)
                        .height(82.dp),
                    contentScale = ContentScale.Fit
                )
            } else {
                Text(
                    text = item.title,
                    color = Tv2DetailsColors.Text,
                    fontSize = 36.sp,
                    lineHeight = 39.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (item.releaseInfo.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = item.releaseInfo,
                    color = Tv2DetailsColors.Soft,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (item.description.isNotBlank()) {
                Spacer(Modifier.height(10.dp))
                Text(
                    text = item.description,
                    color = Tv2DetailsColors.Muted,
                    fontSize = 14.sp,
                    lineHeight = 19.sp,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.height(17.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Tv2PrimaryAction(
                    label = if (loading) "Loading..." else primaryLabel,
                    enabled = !loading,
                    focusRequester = primaryFocusRequester,
                    onFocused = onPrimaryFocused,
                    onClick = onPrimary
                )

                if (onTrailer != null) {
                    Tv2RoundAction(
                        icon = Tv2RoundActionIcon.TRAILER,
                        contentDescription = "Trailer",
                        selected = false,
                        focusRequester = trailerFocusRequester,
                        onFocused = onTrailerFocused,
                        onClick = onTrailer
                    )
                }

                if (canSave) {
                    Tv2RoundAction(
                        icon = if (saved) Tv2RoundActionIcon.SAVED else Tv2RoundActionIcon.SAVE,
                        contentDescription = if (saved) "Saved" else "Save",
                        selected = saved,
                        focusRequester = saveFocusRequester,
                        onFocused = onSaveFocused,
                        onClick = onToggleSaved
                    )
                }
            }

            if (playbackHint.isNotBlank()) {
                Spacer(Modifier.height(9.dp))
                Text(
                    text = playbackHint,
                    color = Tv2DetailsColors.Muted,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun Tv2DetailsEpisodes(
    seasons: List<Int>,
    selectedSeason: Int?,
    episodes: List<MediaEpisode>,
    selectedEpisodeId: String?,
    resumeEpisodeId: String,
    resumeProgressFraction: Float,
    resumePositionMs: Long,
    seasonFocusRequesters: MutableMap<Int, FocusRequester>,
    episodeFocusRequesters: MutableMap<String, FocusRequester>,
    onSeasonFocused: (Int) -> Unit,
    onEpisodeFocused: (MediaEpisode) -> Unit,
    onSeasonSelected: (Int) -> Unit,
    onEpisodeClick: (MediaEpisode) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Tv2DetailsColors.Background)
            .padding(top = 2.dp, bottom = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 40.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Episodes",
                color = Tv2DetailsColors.Text,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
        }

        if (seasons.isNotEmpty()) {
            Spacer(Modifier.height(11.dp))
            LazyRow(
                contentPadding = PaddingValues(horizontal = 40.dp, vertical = 3.dp),
                horizontalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                items(seasons, key = { it }) { season ->
                    val requester = remember(season) {
                        seasonFocusRequesters.getOrPut(season) { FocusRequester() }
                    }
                    Tv2SeasonChip(
                        label = tv2SeasonLabel(season),
                        selected = selectedSeason == season,
                        focusRequester = requester,
                        onFocused = { onSeasonFocused(season) },
                        onClick = { onSeasonSelected(season) }
                    )
                }
            }
        }

        Spacer(Modifier.height(10.dp))
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(
                start = 40.dp,
                end = 48.dp,
                top = 6.dp,
                bottom = 16.dp
            ),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(episodes, key = { it.id }) { episode ->
                val requester = remember(episode.id) {
                    episodeFocusRequesters.getOrPut(episode.id) { FocusRequester() }
                }
                val upRequester = selectedSeason?.let(seasonFocusRequesters::get)

                Tv2EpisodeCard(
                    episode = episode,
                    selected = selectedEpisodeId == episode.id,
                    progressFraction = if (resumeEpisodeId == episode.id) {
                        resumeProgressFraction
                    } else {
                        0f
                    },
                    resumePositionMs = if (resumeEpisodeId == episode.id) {
                        resumePositionMs
                    } else {
                        0L
                    },
                    focusRequester = requester,
                    upFocusRequester = upRequester,
                    onFocused = { onEpisodeFocused(episode) },
                    onClick = { onEpisodeClick(episode) }
                )
            }
        }
    }
}

private enum class Tv2RoundActionIcon {
    TRAILER,
    SAVE,
    SAVED
}

@Composable
private fun Tv2PrimaryAction(
    label: String,
    enabled: Boolean,
    focusRequester: FocusRequester?,
    onFocused: () -> Unit,
    onClick: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (focused) 1.045f else 1f,
        animationSpec = tween(150),
        label = "tv2-details-primary-scale"
    )

    Surface(
        modifier = Modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .then(
                if (focusRequester != null) {
                    Modifier.focusRequester(focusRequester)
                } else {
                    Modifier
                }
            )
            .onFocusChanged {
                focused = it.isFocused
                if (it.isFocused) onFocused()
            }
            .onPreviewKeyEvent { event ->
                tv2ConsumeActivate(event, enabled, onClick)
            }
            .clickable(enabled = enabled, onClick = onClick)
            .focusable(enabled = enabled),
        shape = RoundedCornerShape(26.dp),
        color = if (enabled) Color.White else Color.White.copy(alpha = 0.46f),
        border = if (focused) {
            BorderStroke(3.dp, Color.White.copy(alpha = 0.95f))
        } else {
            null
        }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 23.dp, vertical = 11.dp),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.PlayArrow,
                contentDescription = null,
                tint = Color(0xFF15161A),
                modifier = Modifier.size(21.dp)
            )
            Text(
                text = label,
                color = Color(0xFF15161A),
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
private fun Tv2RoundAction(
    icon: Tv2RoundActionIcon,
    contentDescription: String,
    selected: Boolean,
    focusRequester: FocusRequester?,
    onFocused: () -> Unit,
    onClick: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (focused) 1.08f else 1f,
        animationSpec = tween(140),
        label = "tv2-details-round-scale"
    )

    Surface(
        modifier = Modifier
            .size(46.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .then(
                if (focusRequester != null) {
                    Modifier.focusRequester(focusRequester)
                } else {
                    Modifier
                }
            )
            .onFocusChanged {
                focused = it.isFocused
                if (it.isFocused) onFocused()
            }
            .onPreviewKeyEvent { event ->
                tv2ConsumeActivate(event, true, onClick)
            }
            .clickable(onClick = onClick)
            .focusable(),
        shape = CircleShape,
        color = if (focused) {
            Color(0xFFF0F1F4)
        } else {
            Color.Black.copy(alpha = 0.52f)
        },
        border = BorderStroke(
            width = if (focused) 2.dp else 1.dp,
            color = if (focused) Color.White else Color.White.copy(alpha = 0.28f)
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            val imageVector = when (icon) {
                Tv2RoundActionIcon.TRAILER -> Icons.Outlined.PlayArrow
                Tv2RoundActionIcon.SAVE -> Icons.Outlined.LibraryAdd
                Tv2RoundActionIcon.SAVED -> Icons.Outlined.CheckCircle
            }
            Icon(
                imageVector = imageVector,
                contentDescription = contentDescription,
                tint = when {
                    focused -> Color(0xFF17181B)
                    selected -> Color.White
                    else -> Color(0xFFE8E9ED)
                },
                modifier = Modifier.size(21.dp)
            )
        }
    }
}

@Composable
private fun Tv2SeasonChip(
    label: String,
    selected: Boolean,
    focusRequester: FocusRequester,
    onFocused: () -> Unit,
    onClick: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .focusRequester(focusRequester)
            .onFocusChanged {
                focused = it.isFocused
                if (it.isFocused) onFocused()
            }
            .onPreviewKeyEvent { event ->
                tv2ConsumeActivate(event, true, onClick)
            }
            .clickable(onClick = onClick)
            .focusable(),
        shape = RoundedCornerShape(20.dp),
        color = when {
            focused -> Color(0xFFF0F1F4)
            selected -> Color(0xFF343740)
            else -> Tv2DetailsColors.Surface
        },
        border = when {
            focused -> BorderStroke(2.dp, Color.White)
            selected -> BorderStroke(1.dp, Color.White.copy(alpha = 0.22f))
            else -> null
        }
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 17.dp, vertical = 8.dp),
            color = if (focused) Color(0xFF17181B) else Tv2DetailsColors.Text,
            fontSize = 13.sp,
            fontWeight = if (focused || selected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

@Composable
private fun Tv2EpisodeCard(
    episode: MediaEpisode,
    selected: Boolean,
    progressFraction: Float,
    resumePositionMs: Long,
    focusRequester: FocusRequester,
    upFocusRequester: FocusRequester?,
    onFocused: () -> Unit,
    onClick: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (focused) 1.045f else 1f,
        animationSpec = tween(155),
        label = "tv2-details-episode-scale"
    )

    Surface(
        modifier = Modifier
            .width(292.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .zIndex(if (focused) 1f else 0f)
            .focusRequester(focusRequester)
            .then(
                if (upFocusRequester != null) {
                    Modifier.focusProperties { up = upFocusRequester }
                } else {
                    Modifier
                }
            )
            .onFocusChanged {
                focused = it.isFocused
                if (it.isFocused) onFocused()
            }
            .onPreviewKeyEvent { event ->
                tv2ConsumeActivate(event, true, onClick)
            }
            .clickable(onClick = onClick)
            .focusable(),
        shape = RoundedCornerShape(13.dp),
        color = if (selected) Color(0xFF242730) else Tv2DetailsColors.Surface,
        border = when {
            focused -> BorderStroke(2.dp, Color.White)
            selected -> BorderStroke(1.dp, Color.White.copy(alpha = 0.28f))
            else -> BorderStroke(1.dp, Color.White.copy(alpha = 0.07f))
        }
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .background(Tv2DetailsColors.SurfaceRaised)
            ) {
                Tv2DetailsArtwork(
                    url = episode.thumbnailUrl,
                    title = episode.displayTitle,
                    modifier = Modifier.fillMaxSize()
                )

                if (progressFraction > 0f) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .fillMaxWidth()
                            .height(4.dp)
                            .background(Color.Black.copy(alpha = 0.42f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(progressFraction.coerceIn(0f, 1f))
                                .fillMaxHeight()
                                .background(Color.White)
                        )
                    }
                }
            }

            Column(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Text(
                    text = episode.displayTitle,
                    color = Tv2DetailsColors.Text,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                when {
                    resumePositionMs > 0L -> {
                        Spacer(Modifier.height(3.dp))
                        Text(
                            text = "Resume ${tv2FormatPlaybackTime(resumePositionMs)}",
                            color = Tv2DetailsColors.Soft,
                            fontSize = 11.sp,
                            maxLines = 1
                        )
                    }

                    episode.overview.isNotBlank() -> {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = episode.overview,
                            color = Tv2DetailsColors.Muted,
                            fontSize = 11.sp,
                            lineHeight = 15.sp,
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
private fun Tv2DetailsArtwork(
    url: String,
    title: String,
    modifier: Modifier
) {
    if (url.isBlank()) {
        Box(
            modifier = modifier.background(
                Brush.linearGradient(
                    colors = listOf(
                        Tv2DetailsColors.SurfaceRaised,
                        Tv2DetailsColors.Surface
                    )
                )
            ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = title.take(1).uppercase().ifBlank { "N" },
                color = Tv2DetailsColors.Muted,
                fontSize = 30.sp,
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

private fun tv2ConsumeActivate(
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

private fun tv2SeasonLabel(season: Int): String =
    if (season == 0) "Specials" else "Season $season"

private fun tv2FormatPlaybackTime(positionMs: Long): String {
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
