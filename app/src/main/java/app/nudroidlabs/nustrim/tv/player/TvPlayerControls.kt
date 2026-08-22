package app.nudroidlabs.nustrim.tv.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun TvPlayerControls(
    visible: Boolean,
    request: TvPlaybackRequest,
    runtime: TvPlayerRuntime,
    playPauseFocusRequester: FocusRequester,
    progressFocusRequester: FocusRequester,
    hasEpisodes: Boolean,
    hasNextEpisode: Boolean,
    aspectMode: TvPlayerAspectMode,
    showMoreActions: Boolean,
    onInteraction: () -> Unit,
    onHideControls: () -> Unit,
    onToggleMoreActions: () -> Unit,
    onOpenEpisodes: () -> Unit,
    onOpenSources: () -> Unit,
    onOpenAudio: () -> Unit,
    onOpenSubtitles: () -> Unit,
    onOpenSpeed: () -> Unit,
    onToggleAspect: () -> Unit,
    onPlayNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(200)),
        exit = fadeOut(animationSpec = tween(200)),
        modifier = modifier,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .height(150.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.70f),
                                Color.Transparent,
                            ),
                        ),
                    ),
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(220.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.82f),
                            ),
                        ),
                    ),
            )

            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 42.dp, vertical = 28.dp),
            ) {
                Text(
                    text = request.media.title,
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                request.episode?.let { episode ->
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = buildString {
                            val season = episode.season
                            val number = episode.episode
                            if (season != null && number != null) {
                                append("S")
                                append(season)
                                append("E")
                                append(number)
                            }
                            if (episode.title.isNotBlank()) {
                                if (isNotEmpty()) append(" · ")
                                append(episode.title)
                            }
                        },
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White.copy(alpha = 0.90f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (!runtime.isPlaying && request.streamSourceLabel.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "via ${request.streamSourceLabel.replace("\n", " · ")}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.68f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Spacer(Modifier.height(12.dp))
                TvPlayerProgressBar(
                    runtime = runtime,
                    focusRequester = progressFocusRequester,
                    downFocusRequester = playPauseFocusRequester,
                    onInteraction = onInteraction,
                    onUp = onHideControls,
                )
                Spacer(Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TvPlayerControlButton(
                            icon = if (runtime.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (runtime.isPlaying) "Pause" else "Play",
                            focusRequester = playPauseFocusRequester,
                            upFocusRequester = progressFocusRequester,
                            onFocused = onInteraction,
                            onDown = onHideControls,
                            onClick = {
                                runtime.togglePlayPause()
                                onInteraction()
                            },
                        )
                        if (hasNextEpisode) {
                            TvPlayerControlButton(
                                icon = Icons.Default.SkipNext,
                                contentDescription = "Next episode",
                                upFocusRequester = progressFocusRequester,
                                onFocused = onInteraction,
                                onDown = onHideControls,
                                onClick = onPlayNext,
                            )
                        }
                        if (runtime.subtitleTracks.isNotEmpty() || request.stream.subtitles.isNotEmpty()) {
                            TvPlayerControlButton(
                                icon = Icons.Default.ClosedCaption,
                                contentDescription = "Subtitles",
                                upFocusRequester = progressFocusRequester,
                                onFocused = onInteraction,
                                onDown = onHideControls,
                                onClick = onOpenSubtitles,
                            )
                        }
                        if (runtime.audioTracks.isNotEmpty()) {
                            TvPlayerControlButton(
                                icon = Icons.Default.VolumeUp,
                                contentDescription = "Audio",
                                upFocusRequester = progressFocusRequester,
                                onFocused = onInteraction,
                                onDown = onHideControls,
                                onClick = onOpenAudio,
                            )
                        }
                        TvPlayerControlButton(
                            icon = Icons.Default.Dns,
                            contentDescription = "Sources",
                            upFocusRequester = progressFocusRequester,
                            onFocused = onInteraction,
                            onDown = onHideControls,
                            onClick = onOpenSources,
                        )
                        if (hasEpisodes) {
                            TvPlayerControlButton(
                                icon = Icons.AutoMirrored.Filled.List,
                                contentDescription = "Episodes",
                                upFocusRequester = progressFocusRequester,
                                onFocused = onInteraction,
                                onDown = onHideControls,
                                onClick = onOpenEpisodes,
                            )
                        }
                        AnimatedVisibility(
                            visible = showMoreActions,
                            enter = fadeIn(animationSpec = tween(160)),
                            exit = fadeOut(animationSpec = tween(140)),
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                TvPlayerControlButton(
                                    icon = Icons.Default.Speed,
                                    contentDescription = "Playback speed",
                                    upFocusRequester = progressFocusRequester,
                                    onFocused = onInteraction,
                                    onDown = onHideControls,
                                    onClick = onOpenSpeed,
                                )
                                TvPlayerControlButton(
                                    icon = Icons.Default.AspectRatio,
                                    contentDescription = "Aspect ratio: ${aspectMode.label}",
                                    upFocusRequester = progressFocusRequester,
                                    onFocused = onInteraction,
                                    onDown = onHideControls,
                                    onClick = {
                                        onToggleAspect()
                                        onInteraction()
                                    },
                                )
                            }
                        }
                        TvPlayerControlButton(
                            icon = if (showMoreActions) {
                                Icons.AutoMirrored.Filled.KeyboardArrowLeft
                            } else {
                                Icons.AutoMirrored.Filled.KeyboardArrowRight
                            },
                            contentDescription = if (showMoreActions) "Close more actions" else "More actions",
                            upFocusRequester = progressFocusRequester,
                            onFocused = onInteraction,
                            onDown = onHideControls,
                            onClick = {
                                onToggleMoreActions()
                                onInteraction()
                            },
                        )
                    }
                    Text(
                        text = "${formatPlayerTime(runtime.positionMs)} / ${formatPlayerTime(runtime.durationMs)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.90f),
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
private fun TvPlayerControlButton(
    icon: ImageVector,
    contentDescription: String,
    focusRequester: FocusRequester? = null,
    upFocusRequester: FocusRequester,
    onFocused: () -> Unit,
    onDown: () -> Unit,
    onClick: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .size(48.dp)
            .onFocusChanged {
                focused = it.isFocused
                if (it.isFocused) onFocused()
            }
            .onKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                when (event.key) {
                    Key.DirectionUp -> {
                        runCatching { upFocusRequester.requestFocus() }
                        true
                    }
                    Key.DirectionDown -> {
                        onDown()
                        true
                    }
                    else -> false
                }
            }
            .clip(CircleShape)
            .background(if (focused) Color.White else Color.Transparent),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (focused) Color.Black else Color.White,
            modifier = Modifier.size(27.dp),
        )
    }
}

@Composable
private fun TvPlayerProgressBar(
    runtime: TvPlayerRuntime,
    focusRequester: FocusRequester,
    downFocusRequester: FocusRequester,
    onInteraction: () -> Unit,
    onUp: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val duration = runtime.durationMs.coerceAtLeast(1L)
    val played = (runtime.positionMs.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
    val buffered = (runtime.bufferedPositionMs.toFloat() / duration.toFloat()).coerceIn(0f, 1f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(if (focused) 12.dp else 8.dp)
            .focusRequester(focusRequester)
            .onFocusChanged {
                focused = it.isFocused
                if (it.isFocused) onInteraction()
            }
            .onKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                when (event.key) {
                    Key.DirectionLeft -> {
                        runtime.seekBy(-10_000L)
                        onInteraction()
                        true
                    }
                    Key.DirectionRight -> {
                        runtime.seekBy(10_000L)
                        onInteraction()
                        true
                    }
                    Key.DirectionDown -> {
                        runCatching { downFocusRequester.requestFocus() }
                        true
                    }
                    Key.DirectionUp -> {
                        onUp()
                        true
                    }
                    else -> false
                }
            }
            .focusable()
            .padding(vertical = 2.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (focused) 8.dp else 4.dp)
                .clip(RoundedCornerShape(99.dp))
                .background(Color.White.copy(alpha = 0.18f)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(buffered)
                    .background(Color.White.copy(alpha = 0.34f)),
            )
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(played)
                    .background(Color.White),
            )
        }
    }
}

fun formatPlayerTime(millis: Long): String {
    if (millis <= 0L) return "0:00"
    val totalSeconds = millis / 1_000L
    val hours = totalSeconds / 3_600L
    val minutes = (totalSeconds % 3_600L) / 60L
    val seconds = totalSeconds % 60L
    return if (hours > 0L) {
        String.format(java.util.Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(java.util.Locale.US, "%d:%02d", minutes, seconds)
    }
}
