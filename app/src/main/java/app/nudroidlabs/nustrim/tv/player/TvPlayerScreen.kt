package app.nudroidlabs.nustrim.tv.player

import android.view.ViewGroup
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import app.nudroidlabs.nustrim.core.model.StreamSource
import app.nudroidlabs.nustrim.tv.theme.TvColors
import kotlinx.coroutines.delay
import kotlin.math.max
import kotlin.math.min

@OptIn(UnstableApi::class)
@Composable
fun TvPlayerScreen(
    stream: StreamSource,
    title: String,
    episodeTitle: String?,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val focusRequester = remember(stream.url) { FocusRequester() }

    val player = remember(context, stream.url, stream.headers) {
        val httpFactory = DefaultHttpDataSource.Factory()
            .setAllowCrossProtocolRedirects(true)

        if (stream.headers.isNotEmpty()) {
            httpFactory.setDefaultRequestProperties(stream.headers)
        }

        val dataSourceFactory = DefaultDataSource.Factory(
            context,
            httpFactory
        )

        ExoPlayer.Builder(context)
            .setMediaSourceFactory(
                DefaultMediaSourceFactory(dataSourceFactory)
            )
            .build()
    }

    var controlsVisible by remember(stream.url) { mutableStateOf(true) }
    var interactionToken by remember(stream.url) { mutableIntStateOf(0) }
    var isPlaying by remember(stream.url) { mutableStateOf(false) }
    var buffering by remember(stream.url) { mutableStateOf(true) }
    var playbackError by remember(stream.url) { mutableStateOf<String?>(null) }
    var positionMs by remember(stream.url) { mutableLongStateOf(0L) }
    var durationMs by remember(stream.url) { mutableLongStateOf(0L) }
    var bufferedMs by remember(stream.url) { mutableLongStateOf(0L) }

    fun revealControls() {
        controlsVisible = true
        interactionToken += 1
    }

    fun retryPlayback() {
        playbackError = null
        buffering = true
        player.prepare()
        player.playWhenReady = true
        revealControls()
    }

    fun togglePlayback() {
        if (playbackError != null) {
            retryPlayback()
            return
        }

        if (player.isPlaying) player.pause() else player.play()
        revealControls()
    }

    fun seekBy(deltaMs: Long) {
        if (playbackError != null) return

        val duration = player.duration
            .takeIf { it != C.TIME_UNSET && it > 0L }
            ?: Long.MAX_VALUE

        val target = (player.currentPosition + deltaMs)
            .coerceAtLeast(0L)
            .let {
                if (duration == Long.MAX_VALUE) it else min(it, duration)
            }

        player.seekTo(target)
        positionMs = target
        revealControls()
    }

    DisposableEffect(player, stream.url) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(value: Boolean) {
                isPlaying = value
                if (!value) controlsVisible = true
            }

            override fun onPlaybackStateChanged(state: Int) {
                buffering = state == Player.STATE_BUFFERING ||
                    state == Player.STATE_IDLE

                if (state == Player.STATE_ENDED) {
                    controlsVisible = true
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                buffering = false
                controlsVisible = true
                playbackError = error.message
                    ?.takeIf { it.isNotBlank() }
                    ?: "Playback failed."
            }
        }

        player.addListener(listener)

        val itemBuilder = MediaItem.Builder()
            .setUri(stream.url)

        val type = stream.type.lowercase()
        val url = stream.url.lowercase()
        if ("hls" in type || "m3u8" in type || ".m3u8" in url) {
            itemBuilder.setMimeType(MimeTypes.APPLICATION_M3U8)
        }

        player.setMediaItem(itemBuilder.build())
        player.prepare()
        player.playWhenReady = true

        onDispose {
            player.removeListener(listener)
            player.release()
        }
    }

    LaunchedEffect(player, stream.url) {
        while (true) {
            positionMs = max(0L, player.currentPosition)
            bufferedMs = max(positionMs, player.bufferedPosition)
            durationMs = player.duration
                .takeIf { it != C.TIME_UNSET && it > 0L }
                ?: 0L
            delay(500)
        }
    }

    LaunchedEffect(interactionToken, isPlaying, playbackError) {
        if (isPlaying && playbackError == null) {
            val token = interactionToken
            delay(3500)
            if (
                token == interactionToken &&
                isPlaying &&
                playbackError == null
            ) {
                controlsVisible = false
            }
        } else {
            controlsVisible = true
        }
    }

    LaunchedEffect(Unit) {
        delay(60)
        runCatching { focusRequester.requestFocus() }
    }

    BackHandler(onBack = onBack)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusRequester(focusRequester)
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) {
                    false
                } else {
                    when (event.key) {
                        Key.DirectionLeft -> {
                            seekBy(-10_000L)
                            true
                        }

                        Key.DirectionRight -> {
                            seekBy(10_000L)
                            true
                        }

                        Key.DirectionCenter,
                        Key.Enter -> {
                            togglePlayback()
                            true
                        }

                        else -> {
                            revealControls()
                            false
                        }
                    }
                }
            }
            .focusable()
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { viewContext ->
                PlayerView(viewContext).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    useController = false
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                    setShutterBackgroundColor(
                        android.graphics.Color.BLACK
                    )
                    this.player = player
                }
            },
            update = { view ->
                if (view.player !== player) {
                    view.player = player
                }
            }
        )

        if (buffering && playbackError == null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }

        if (playbackError != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.76f)),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    modifier = Modifier.width(560.dp),
                    color = TvColors.BackgroundElevated,
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Playback error",
                            color = TvColors.TextPrimary,
                            fontSize = 23.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = playbackError.orEmpty(),
                            color = TvColors.TextSecondary,
                            fontSize = 14.sp,
                            maxLines = 4,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "OK Retry  •  Back Sources",
                            color = TvColors.Accent,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        if (controlsVisible && playbackError == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.58f),
                                Color.Transparent,
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.82f)
                            )
                        )
                    )
            ) {
                Column(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(
                            horizontal = 34.dp,
                            vertical = 28.dp
                        ),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = title,
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    episodeTitle
                        ?.takeIf { it.isNotBlank() }
                        ?.let { episode ->
                            Text(
                                text = episode,
                                color = Color.White.copy(alpha = 0.72f),
                                fontSize = 14.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                }

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .padding(
                            horizontal = 34.dp,
                            vertical = 28.dp
                        ),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TvPlayerProgress(
                        positionMs = positionMs,
                        bufferedMs = bufferedMs,
                        durationMs = durationMs
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(20.dp)
                        ) {
                            Text(
                                text = "◀ 10s",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = if (isPlaying) "Ⅱ" else "▶",
                                color = Color.White,
                                fontSize = 25.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "10s ▶",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Text(
                            text = "${formatTvTime(positionMs)} / ${formatTvTime(durationMs)}",
                            color = Color.White.copy(alpha = 0.86f),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Text(
                        text = "${stream.name.ifBlank { "Source" }}  •  Left/Right seek  •  OK Play/Pause  •  Back Sources",
                        color = Color.White.copy(alpha = 0.68f),
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun TvPlayerProgress(
    positionMs: Long,
    bufferedMs: Long,
    durationMs: Long
) {
    val playedFraction = if (durationMs > 0L) {
        (positionMs.toFloat() / durationMs.toFloat())
            .coerceIn(0f, 1f)
    } else {
        0f
    }

    val bufferedFraction = if (durationMs > 0L) {
        (bufferedMs.toFloat() / durationMs.toFloat())
            .coerceIn(0f, 1f)
    } else {
        0f
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(5.dp)
            .background(
                Color.White.copy(alpha = 0.20f),
                RoundedCornerShape(99.dp)
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(bufferedFraction)
                .background(
                    Color.White.copy(alpha = 0.38f),
                    RoundedCornerShape(99.dp)
                )
        )

        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(playedFraction)
                .background(
                    TvColors.Accent,
                    RoundedCornerShape(99.dp)
                )
        )
    }
}

private fun formatTvTime(
    milliseconds: Long
): String {
    if (milliseconds <= 0L) return "00:00"

    val totalSeconds = milliseconds / 1000L
    val hours = totalSeconds / 3600L
    val minutes = (totalSeconds % 3600L) / 60L
    val seconds = totalSeconds % 60L

    return if (hours > 0L) {
        "%d:%02d:%02d".format(
            hours,
            minutes,
            seconds
        )
    } else {
        "%02d:%02d".format(
            minutes,
            seconds
        )
    }
}
