package app.nudroidlabs.nustrim.tv2.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Tv2PlayerText = Color(0xFFF6F6F8)
private val Tv2PlayerMuted = Color(0xFFB5B7C0)

@Composable
fun Tv2PlayerControls(
    visible: Boolean,
    title: String,
    providerLabel: String,
    isPlaying: Boolean,
    isBuffering: Boolean,
    positionMs: Long,
    durationMs: Long,
    primaryFocusRequester: FocusRequester,
    controlFocusRequesters: List<FocusRequester>,
    hasEpisodes: Boolean,
    resizeLabel: String,
    speedLabel: String,
    onBack: () -> Unit,
    onPlayPause: () -> Unit,
    onSeekBy: (Long) -> Unit,
    onResize: () -> Unit,
    onSpeed: () -> Unit,
    onSubtitles: () -> Unit,
    onAudio: () -> Unit,
    onSources: () -> Unit,
    onEpisodes: () -> Unit,
    onActivity: () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(120)),
        exit = fadeOut(tween(180)),
        modifier = Modifier.fillMaxSize()
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .onPreviewKeyEvent {
                    if (it.type == KeyEventType.KeyDown) onActivity()
                    false
                }
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Black.copy(alpha = 0.34f),
                                Color.Transparent,
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.86f)
                            )
                        )
                    )
            )

            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .fillMaxWidth()
                    .padding(horizontal = 34.dp, vertical = 24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Tv2RoundButton(onBack)
                Spacer(Modifier.width(15.dp))
                Column {
                    Text(
                        title,
                        color = Tv2PlayerText,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (providerLabel.isNotBlank()) {
                        Text(
                            providerLabel,
                            color = Tv2PlayerMuted,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 42.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val progress = if (durationMs > 0L) {
                    (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
                } else {
                    0f
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .background(Color.White.copy(alpha = 0.25f), RoundedCornerShape(3.dp))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress)
                            .fillMaxHeight()
                            .background(Color.White, RoundedCornerShape(3.dp))
                    )
                }

                Spacer(Modifier.height(7.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(tv2Time(positionMs), color = Tv2PlayerText, fontSize = 11.sp)
                    Spacer(Modifier.weight(1f))
                    if (isBuffering) {
                        Text("Buffering", color = Tv2PlayerMuted, fontSize = 11.sp)
                        Spacer(Modifier.width(12.dp))
                    }
                    Text(tv2Time(durationMs), color = Tv2PlayerText, fontSize = 11.sp)
                }

                Spacer(Modifier.height(13.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(9.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Tv2Action("-10", controlFocusRequesters.getOrNull(0)) { onSeekBy(-10_000L) }
                    Tv2Primary(isPlaying, primaryFocusRequester, onPlayPause)
                    Tv2Action("+10", controlFocusRequesters.getOrNull(1)) { onSeekBy(10_000L) }
                    Tv2Action("Subs", controlFocusRequesters.getOrNull(2), onSubtitles)
                    Tv2Action("Audio", controlFocusRequesters.getOrNull(3), onAudio)
                    Tv2Action("Sources", controlFocusRequesters.getOrNull(4), onSources)
                    if (hasEpisodes) {
                        Tv2Action("Episodes", controlFocusRequesters.getOrNull(5), onEpisodes)
                    }
                    Tv2Action(resizeLabel, null, onResize)
                    Tv2Action(speedLabel, null, onSpeed)
                }
            }
        }
    }
}

@Composable
private fun Tv2Primary(
    isPlaying: Boolean,
    requester: FocusRequester,
    onClick: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        if (focused) 1.08f else 1f,
        tween(120),
        label = "tv2-player-primary"
    )

    Surface(
        modifier = Modifier
            .size(58.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .focusRequester(requester)
            .onFocusChanged { focused = it.isFocused }
            .onPreviewKeyEvent { event -> activate(event, onClick) }
            .clickable(onClick = onClick)
            .focusable(),
        shape = CircleShape,
        color = if (focused) Color.White else Color.Black.copy(alpha = 0.52f),
        border = BorderStroke(
            if (focused) 3.dp else 1.dp,
            if (focused) Color.White else Color.White.copy(alpha = 0.25f)
        )
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (isPlaying) {
                Text(
                    "Ⅱ",
                    color = if (focused) Color(0xFF17181B) else Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black
                )
            } else {
                Icon(
                    Icons.Outlined.PlayArrow,
                    contentDescription = "Play",
                    tint = if (focused) Color(0xFF17181B) else Color.White,
                    modifier = Modifier.size(30.dp)
                )
            }
        }
    }
}

@Composable
private fun Tv2Action(
    label: String,
    requester: FocusRequester?,
    onClick: () -> Unit
) {
    var focused by remember(label) { mutableStateOf(false) }
    val base = if (requester != null) Modifier.focusRequester(requester) else Modifier

    Surface(
        modifier = base
            .onFocusChanged { focused = it.isFocused }
            .onPreviewKeyEvent { event -> activate(event, onClick) }
            .clickable(onClick = onClick)
            .focusable(),
        shape = RoundedCornerShape(18.dp),
        color = if (focused) Color.White else Color.Black.copy(alpha = 0.54f),
        border = BorderStroke(
            if (focused) 2.dp else 1.dp,
            if (focused) Color.White else Color.White.copy(alpha = 0.15f)
        )
    ) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 13.dp, vertical = 8.dp),
            color = if (focused) Color(0xFF17181B) else Tv2PlayerText,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun Tv2RoundButton(onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .size(42.dp)
            .onFocusChanged { focused = it.isFocused }
            .onPreviewKeyEvent { event -> activate(event, onClick) }
            .clickable(onClick = onClick)
            .focusable(),
        shape = CircleShape,
        color = if (focused) Color.White else Color.Black.copy(alpha = 0.54f),
        border = BorderStroke(
            if (focused) 2.dp else 1.dp,
            if (focused) Color.White else Color.White.copy(alpha = 0.16f)
        )
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Icon(
                Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = "Back",
                tint = if (focused) Color(0xFF17181B) else Color.White,
                modifier = Modifier.size(21.dp)
            )
        }
    }
}

private fun activate(
    event: androidx.compose.ui.input.key.KeyEvent,
    onClick: () -> Unit
): Boolean {
    val ok = event.key == Key.DirectionCenter || event.key == Key.Enter
    if (!ok) return false
    if (event.type == KeyEventType.KeyUp) onClick()
    return true
}

private fun tv2Time(ms: Long): String {
    val total = ms.coerceAtLeast(0L) / 1_000L
    val hours = total / 3_600L
    val minutes = (total % 3_600L) / 60L
    val seconds = total % 60L
    return if (hours > 0L) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%d:%02d".format(minutes, seconds)
    }
}
