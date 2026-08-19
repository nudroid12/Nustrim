package app.nudroidlabs.nustrim.tv.player

import android.net.Uri
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.focus.onFocusChanged
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
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import app.nudroidlabs.nustrim.core.model.StreamSource
import app.nudroidlabs.nustrim.tv.theme.TvColors
import app.nudroidlabs.nustrim.ui.SubtitleDisplayMode
import app.nudroidlabs.nustrim.ui.UiPreferences
import kotlinx.coroutines.delay
import kotlin.math.max
import kotlin.math.min
import java.util.Locale

private enum class TvPlayerTrackPanel {
    AUDIO,
    SUBTITLES
}

private data class TvPlayerTrackOption(
    val group: Tracks.Group,
    val trackIndex: Int,
    val label: String,
    val language: String,
    val selected: Boolean
)

@OptIn(UnstableApi::class)
@Composable
fun TvPlayerScreen(
    stream: StreamSource,
    title: String,
    episodeTitle: String?,
    previousEpisodeTitle: String? = null,
    nextEpisodeTitle: String? = null,
    onPreviousEpisode: (() -> Unit)? = null,
    onNextEpisode: (() -> Unit)? = null,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val preferences = remember(context) { UiPreferences(context) }
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
    var trackPanel by remember(stream.url) {
        mutableStateOf<TvPlayerTrackPanel?>(null)
    }
    var audioTracks by remember(stream.url) {
        mutableStateOf<List<TvPlayerTrackOption>>(emptyList())
    }
    var subtitleTracks by remember(stream.url) {
        mutableStateOf<List<TvPlayerTrackOption>>(emptyList())
    }
    var subtitlesDisabled by remember(stream.url) { mutableStateOf(false) }
    var autoNextCountdown by remember(stream.url) { mutableIntStateOf(-1) }

    val preferredSubtitleLanguages = remember(stream.url) {
        listOf(
            preferences.subtitlePreferredLanguage,
            preferences.subtitleSecondPreferredLanguage
        )
            .map(::normalizeTvTrackLanguage)
            .filter(String::isNotBlank)
            .distinct()
    }
    val visibleSubtitleTracks = if (
        preferences.subtitleDisplayMode == SubtitleDisplayMode.SHOW_ALL
    ) {
        subtitleTracks
    } else {
        subtitleTracks.filter { option ->
            normalizeTvTrackLanguage(option.language) in preferredSubtitleLanguages ||
                option.selected
        }
    }
    val selectedAudioLabel = audioTracks
        .firstOrNull { it.selected }
        ?.label
        ?: "Auto"
    val selectedSubtitleLabel = when {
        subtitlesDisabled -> "Off"
        else -> subtitleTracks.firstOrNull { it.selected }?.label ?: "Auto"
    }

    fun revealControls() {
        controlsVisible = true
        interactionToken += 1
    }

    fun cancelAutoNext() {
        if (autoNextCountdown >= 0) {
            autoNextCountdown = -1
        }
    }

    fun triggerPreviousEpisode() {
        val callback = onPreviousEpisode ?: return
        cancelAutoNext()
        callback()
    }

    fun triggerNextEpisode() {
        val callback = onNextEpisode ?: return
        cancelAutoNext()
        callback()
    }

    fun retryPlayback() {
        cancelAutoNext()
        playbackError = null
        buffering = true
        player.prepare()
        player.playWhenReady = true
        revealControls()
    }

    fun togglePlayback() {
        if (autoNextCountdown >= 0 && onNextEpisode != null) {
            triggerNextEpisode()
            return
        }

        if (playbackError != null) {
            retryPlayback()
            return
        }

        cancelAutoNext()
        if (player.isPlaying) player.pause() else player.play()
        revealControls()
    }

    fun seekBy(deltaMs: Long) {
        if (playbackError != null) return

        cancelAutoNext()

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

    fun openTrackPanel(panel: TvPlayerTrackPanel) {
        if (playbackError != null) return
        cancelAutoNext()
        trackPanel = panel
        revealControls()
    }

    fun selectTrack(option: TvPlayerTrackOption) {
        player.trackSelectionParameters = player.trackSelectionParameters
            .buildUpon()
            .setTrackTypeDisabled(option.group.type, false)
            .setOverrideForType(
                TrackSelectionOverride(
                    option.group.mediaTrackGroup,
                    option.trackIndex
                )
            )
            .build()

        if (option.group.type == C.TRACK_TYPE_TEXT) {
            subtitlesDisabled = false
            val language = normalizeTvTrackLanguage(option.language)
            if (language.isNotBlank()) {
                val previous = normalizeTvTrackLanguage(
                    preferences.subtitlePreferredLanguage
                )
                if (previous.isNotBlank() && previous != language) {
                    preferences.subtitleSecondPreferredLanguage = previous
                }
                preferences.subtitlePreferredLanguage = language
            }
        }

        trackPanel = null
        revealControls()
    }

    fun disableSubtitles() {
        player.trackSelectionParameters = player.trackSelectionParameters
            .buildUpon()
            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
            .build()
        subtitlesDisabled = true
        trackPanel = null
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
                    autoNextCountdown = if (onNextEpisode != null) {
                        5
                    } else {
                        -1
                    }
                }
            }

            override fun onTracksChanged(tracks: Tracks) {
                audioTracks = collectTvTrackOptions(
                    tracks = tracks,
                    trackType = C.TRACK_TYPE_AUDIO
                )
                subtitleTracks = collectTvTrackOptions(
                    tracks = tracks,
                    trackType = C.TRACK_TYPE_TEXT
                )
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

        val primarySubtitleLanguage = normalizeTvTrackLanguage(
            preferences.subtitlePreferredLanguage
        )
        val secondSubtitleLanguage = normalizeTvTrackLanguage(
            preferences.subtitleSecondPreferredLanguage
        )
        val preferredTextLanguages = listOf(
            primarySubtitleLanguage,
            secondSubtitleLanguage
        )
            .filter(String::isNotBlank)
            .distinct()

        if (preferredTextLanguages.isNotEmpty()) {
            player.trackSelectionParameters = player.trackSelectionParameters
                .buildUpon()
                .setPreferredTextLanguages(*preferredTextLanguages.toTypedArray())
                .build()
        }

        val itemBuilder = MediaItem.Builder()
            .setUri(stream.url)

        val type = stream.type.lowercase()
        val url = stream.url.lowercase()
        if ("hls" in type || "m3u8" in type || ".m3u8" in url) {
            itemBuilder.setMimeType(MimeTypes.APPLICATION_M3U8)
        }

        val subtitleConfigurations = stream.subtitles.mapNotNull { subtitle ->
            val subtitleUrl = subtitle.url.trim()
            if (subtitleUrl.isBlank()) {
                null
            } else {
                val builder = MediaItem.SubtitleConfiguration.Builder(
                    Uri.parse(subtitleUrl)
                )
                    .setMimeType(inferTvSubtitleMimeType(subtitleUrl))

                subtitle.language
                    .trim()
                    .takeIf { it.isNotBlank() }
                    ?.let(builder::setLanguage)
                subtitle.label
                    .trim()
                    .takeIf { it.isNotBlank() }
                    ?.let(builder::setLabel)

                builder.build()
            }
        }
        if (subtitleConfigurations.isNotEmpty()) {
            itemBuilder.setSubtitleConfigurations(subtitleConfigurations)
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

    LaunchedEffect(autoNextCountdown, stream.url) {
        when {
            autoNextCountdown > 0 -> {
                delay(1000)
                if (autoNextCountdown > 0) {
                    autoNextCountdown -= 1
                }
            }

            autoNextCountdown == 0 -> {
                triggerNextEpisode()
            }
        }
    }

    LaunchedEffect(
        interactionToken,
        isPlaying,
        playbackError,
        trackPanel
    ) {
        if (
            isPlaying &&
            playbackError == null &&
            trackPanel == null
        ) {
            val token = interactionToken
            delay(3500)
            if (
                token == interactionToken &&
                isPlaying &&
                playbackError == null &&
                trackPanel == null
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

    LaunchedEffect(trackPanel) {
        if (trackPanel == null) {
            delay(40)
            runCatching { focusRequester.requestFocus() }
        }
    }

    BackHandler {
        when {
            trackPanel != null -> {
                trackPanel = null
                revealControls()
            }

            autoNextCountdown >= 0 -> {
                cancelAutoNext()
                revealControls()
            }

            else -> onBack()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusRequester(focusRequester)
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) {
                    false
                } else if (trackPanel != null) {
                    false
                } else {
                    when (event.key) {
                        Key.MediaPrevious -> {
                            if (onPreviousEpisode != null) {
                                triggerPreviousEpisode()
                                true
                            } else {
                                revealControls()
                                false
                            }
                        }

                        Key.MediaNext -> {
                            if (onNextEpisode != null) {
                                triggerNextEpisode()
                                true
                            } else {
                                revealControls()
                                false
                            }
                        }

                        Key.DirectionLeft -> {
                            if (
                                onPreviousEpisode != null &&
                                positionMs <= 1_500L
                            ) {
                                triggerPreviousEpisode()
                            } else {
                                seekBy(-10_000L)
                            }
                            true
                        }

                        Key.DirectionRight -> {
                            val nearEnd = durationMs > 0L &&
                                durationMs - positionMs <= 12_000L
                            if (onNextEpisode != null && nearEnd) {
                                triggerNextEpisode()
                            } else {
                                seekBy(10_000L)
                            }
                            true
                        }

                        Key.DirectionUp -> {
                            openTrackPanel(TvPlayerTrackPanel.AUDIO)
                            true
                        }

                        Key.DirectionDown -> {
                            openTrackPanel(TvPlayerTrackPanel.SUBTITLES)
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
                        text = "Audio: $selectedAudioLabel  •  Subtitles: $selectedSubtitleLabel",
                        color = Color.White.copy(alpha = 0.78f),
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (
                        previousEpisodeTitle != null ||
                        nextEpisodeTitle != null
                    ) {
                        Text(
                            text = buildString {
                                previousEpisodeTitle
                                    ?.takeIf { it.isNotBlank() }
                                    ?.let { append("Previous: $it") }
                                if (
                                    previousEpisodeTitle != null &&
                                    nextEpisodeTitle != null
                                ) {
                                    append("  •  ")
                                }
                                nextEpisodeTitle
                                    ?.takeIf { it.isNotBlank() }
                                    ?.let { append("Next: $it") }
                            },
                            color = Color.White.copy(alpha = 0.72f),
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Text(
                        text = buildString {
                            append(stream.name.ifBlank { "Source" })
                            append("  •  ▲ Audio  •  ▼ Subtitles")
                            append("  •  Left/Right seek")
                            if (onPreviousEpisode != null || onNextEpisode != null) {
                                append("  •  Media Prev/Next episode")
                            }
                            append("  •  OK Play/Pause  •  Back Sources")
                        },
                        color = Color.White.copy(alpha = 0.68f),
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        if (
            autoNextCountdown >= 0 &&
            onNextEpisode != null &&
            playbackError == null
        ) {
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(
                        end = 34.dp,
                        bottom = 132.dp
                    )
                    .width(410.dp),
                color = TvColors.BackgroundElevated.copy(alpha = 0.97f),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(
                        horizontal = 20.dp,
                        vertical = 16.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Text(
                        text = if (autoNextCountdown > 0) {
                            "Next episode in ${autoNextCountdown}s"
                        } else {
                            "Opening next episode..."
                        },
                        color = TvColors.TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    nextEpisodeTitle
                        ?.takeIf { it.isNotBlank() }
                        ?.let { nextTitle ->
                            Text(
                                text = nextTitle,
                                color = TvColors.TextSecondary,
                                fontSize = 13.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    Text(
                        text = "OK Play now  •  Back Cancel",
                        color = TvColors.Accent,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        trackPanel?.let { panel ->
            val panelOptions = when (panel) {
                TvPlayerTrackPanel.AUDIO -> audioTracks
                TvPlayerTrackPanel.SUBTITLES -> visibleSubtitleTracks
            }
            TvPlayerTrackPicker(
                title = when (panel) {
                    TvPlayerTrackPanel.AUDIO -> "Audio"
                    TvPlayerTrackPanel.SUBTITLES -> "Subtitles"
                },
                options = panelOptions,
                showOff = panel == TvPlayerTrackPanel.SUBTITLES,
                offSelected = subtitlesDisabled,
                onSelect = ::selectTrack,
                onOff = ::disableSubtitles,
                onClose = {
                    trackPanel = null
                    revealControls()
                },
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 28.dp)
            )
        }
    }
}

@Composable
private fun TvPlayerTrackPicker(
    title: String,
    options: List<TvPlayerTrackOption>,
    showOff: Boolean,
    offSelected: Boolean,
    onSelect: (TvPlayerTrackOption) -> Unit,
    onOff: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val firstRequester = remember(title) { FocusRequester() }
    val hasFirstTarget = showOff || options.isNotEmpty()

    LaunchedEffect(title, options.size, showOff) {
        if (hasFirstTarget) {
            delay(60)
            runCatching { firstRequester.requestFocus() }
        }
    }

    Surface(
        modifier = modifier
            .width(420.dp)
            .height(480.dp),
        color = TvColors.BackgroundElevated.copy(alpha = 0.98f),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = title,
                color = TvColors.TextPrimary,
                fontSize = 23.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "OK Select  •  Left/Back Close",
                color = TvColors.TextSecondary,
                fontSize = 12.sp
            )
            if (!hasFirstTarget) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No $title tracks available.",
                        color = TvColors.TextSecondary,
                        fontSize = 14.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (showOff) {
                        item(key = "off") {
                            TvPlayerTrackRow(
                                label = "Off",
                                selected = offSelected,
                                focusRequester = firstRequester,
                                onSelect = onOff,
                                onClose = onClose
                            )
                        }
                    }
                    itemsIndexed(
                        items = options,
                        key = { index, option ->
                            "${option.group.type}|${option.language}|${option.label}|$index"
                        }
                    ) { index, option ->
                        TvPlayerTrackRow(
                            label = option.label,
                            selected = option.selected && !offSelected,
                            focusRequester = if (!showOff && index == 0) {
                                firstRequester
                            } else {
                                null
                            },
                            onSelect = { onSelect(option) },
                            onClose = onClose
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TvPlayerTrackRow(
    label: String,
    selected: Boolean,
    focusRequester: FocusRequester?,
    onSelect: () -> Unit,
    onClose: () -> Unit
) {
    val requester = remember(label, focusRequester) {
        focusRequester ?: FocusRequester()
    }
    var focused by remember(label) { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .focusRequester(requester)
            .onFocusChanged { focused = it.hasFocus }
            .onPreviewKeyEvent { event ->
                when {
                    event.type == KeyEventType.KeyDown &&
                        event.key == Key.DirectionLeft -> {
                        onClose()
                        true
                    }

                    event.type == KeyEventType.KeyDown &&
                        event.key == Key.DirectionRight -> true

                    event.type == KeyEventType.KeyDown &&
                        (
                            event.key == Key.DirectionCenter ||
                                event.key == Key.Enter
                            ) -> {
                        onSelect()
                        true
                    }

                    else -> false
                }
            }
            .focusable(),
        color = when {
            focused -> TvColors.FocusRing
            selected -> TvColors.Accent.copy(alpha = 0.76f)
            else -> TvColors.SurfaceVariant
        },
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 15.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                color = if (focused) TvColors.Background else TvColors.TextPrimary,
                fontSize = 14.sp,
                fontWeight = if (focused || selected) {
                    FontWeight.SemiBold
                } else {
                    FontWeight.Normal
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (selected) {
                Text(
                    text = "Selected",
                    color = if (focused) {
                        TvColors.Background.copy(alpha = 0.78f)
                    } else {
                        TvColors.TextSecondary
                    },
                    fontSize = 11.sp
                )
            }
        }
    }
}

private fun collectTvTrackOptions(
    tracks: Tracks,
    trackType: Int
): List<TvPlayerTrackOption> {
    val options = mutableListOf<TvPlayerTrackOption>()

    tracks.groups
        .filter { it.type == trackType }
        .forEach { group ->
            for (index in 0 until group.length) {
                if (!group.isTrackSupported(index)) continue

                val format = group.getTrackFormat(index)
                val language = normalizeTvTrackLanguage(format.language)
                val languageLabel = displayTvTrackLanguage(language)
                val detail = buildList {
                    format.label
                        ?.trim()
                        ?.takeIf { it.isNotBlank() && it != languageLabel }
                        ?.let(::add)
                    if (trackType == C.TRACK_TYPE_AUDIO) {
                        format.channelCount
                            .takeIf { it > 0 }
                            ?.let { add("${it}ch") }
                    }
                    format.codecs
                        ?.substringBefore(",")
                        ?.trim()
                        ?.takeIf { it.isNotBlank() }
                        ?.let(::add)
                }
                    .distinct()
                    .joinToString(" · ")

                options += TvPlayerTrackOption(
                    group = group,
                    trackIndex = index,
                    label = if (detail.isBlank()) {
                        languageLabel
                    } else {
                        "$languageLabel · $detail"
                    },
                    language = language,
                    selected = group.isTrackSelected(index)
                )
            }
        }

    return options
}

private fun normalizeTvTrackLanguage(
    value: String?
): String {
    val raw = value
        ?.trim()
        ?.lowercase(Locale.ROOT)
        .orEmpty()
    if (raw.isBlank() || raw == "und") return ""

    return when (raw.substringBefore("-").substringBefore("_")) {
        "may", "msa", "zsm" -> "ms"
        "eng" -> "en"
        else -> raw.substringBefore("-").substringBefore("_")
    }
}

private fun displayTvTrackLanguage(
    language: String
): String {
    if (language.isBlank()) return "Unknown"
    val locale = Locale.forLanguageTag(language)
    return locale.getDisplayLanguage(Locale.getDefault())
        .takeIf { it.isNotBlank() }
        ?.replaceFirstChar { it.uppercase() }
        ?: language.uppercase(Locale.ROOT)
}

private fun inferTvSubtitleMimeType(
    url: String
): String {
    val clean = url
        .substringBefore("?")
        .substringBefore("#")
        .lowercase(Locale.ROOT)

    return when {
        clean.endsWith(".vtt") -> "text/vtt"
        clean.endsWith(".ass") || clean.endsWith(".ssa") -> "text/x-ssa"
        clean.endsWith(".ttml") || clean.endsWith(".xml") -> "application/ttml+xml"
        else -> "application/x-subrip"
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
