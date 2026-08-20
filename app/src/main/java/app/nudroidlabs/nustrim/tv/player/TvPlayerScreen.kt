package app.nudroidlabs.nustrim.tv.player

import android.net.Uri
import android.text.format.DateFormat
import android.view.ViewGroup
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import app.nudroidlabs.nustrim.core.model.MediaEpisode
import app.nudroidlabs.nustrim.core.model.StreamSource
import app.nudroidlabs.nustrim.tv.sources.TvSourcePreviewRequest
import app.nudroidlabs.nustrim.tv.theme.TvColors
import app.nudroidlabs.nustrim.ui.SubtitleDisplayMode
import app.nudroidlabs.nustrim.ui.UiPreferences
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import java.util.Date
import java.util.Locale

private enum class TvPlayerTrackPanel {
    AUDIO,
    SUBTITLES
}

private enum class TvPlayerSidePanel {
    SOURCES,
    EPISODES,
    STREAM_INFO
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
    sourceRequest: TvSourcePreviewRequest? = null,
    episodes: List<MediaEpisode> = emptyList(),
    currentEpisode: MediaEpisode? = null,
    isEpisodeWatched: (MediaEpisode) -> Boolean = { false },
    onSelectSource: ((StreamSource) -> Unit)? = null,
    onSelectEpisode: ((MediaEpisode) -> Unit)? = null,
    onSelectEpisodeSource: ((MediaEpisode, StreamSource) -> Unit)? = null,
    onPreviousEpisode: (() -> Unit)? = null,
    onNextEpisode: (() -> Unit)? = null,
    onAutoNextEpisode: (() -> Unit)? = null,
    stillWatchingGate: Boolean = false,
    onStillWatchingContinue: (() -> Unit)? = null,
    startPositionMs: Long = 0L,
    onProgress: ((Long, Long, Boolean) -> Unit)? = null,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val preferences = remember(context) { UiPreferences(context) }
    val playerScope = rememberCoroutineScope()
    val latestOnProgress by rememberUpdatedState(onProgress)
    val originalExternalSubtitles = remember(stream.url) { stream.subtitles }
    val focusRequester = remember(stream.url) { FocusRequester() }
    val playControlRequester = remember(stream.url) { FocusRequester() }
    val progressControlRequester = remember(stream.url) { FocusRequester() }
    val audioControlRequester = remember(stream.url) { FocusRequester() }
    val subtitleControlRequester = remember(stream.url) { FocusRequester() }
    val episodesControlRequester = remember(stream.url) { FocusRequester() }
    val sourcesControlRequester = remember(stream.url) { FocusRequester() }
    val infoControlRequester = remember(stream.url) { FocusRequester() }
    val speedControlRequester = remember(stream.url) { FocusRequester() }

    val player = remember(
        context,
        stream.url,
        stream.headers,
        stream.subtitles.map { it.url }
    ) {
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
    var positionMs by remember(stream.url, startPositionMs) {
        mutableLongStateOf(startPositionMs.coerceAtLeast(0L))
    }
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
    var playbackCompleted by remember(stream.url) { mutableStateOf(false) }
    var lastProgressReportMs by remember(stream.url) {
        mutableLongStateOf(startPositionMs.coerceAtLeast(0L))
    }
    var focusedControlId by remember(stream.url) { mutableStateOf<String?>(null) }
    var pendingControlFocus by remember(stream.url) { mutableStateOf(false) }
    var panelReturnFocus by remember(stream.url) {
        mutableStateOf<TvPlayerTrackPanel?>(null)
    }
    var moreExpanded by remember(stream.url) { mutableStateOf(false) }
    var speedPanelVisible by remember(stream.url) { mutableStateOf(false) }
    var pendingSpeedFocus by remember(stream.url) { mutableStateOf(false) }
    var playbackSpeed by remember(stream.url) { mutableStateOf(1f) }
    var videoResizeMode by remember(stream.url) {
        mutableIntStateOf(AspectRatioFrameLayout.RESIZE_MODE_FIT)
    }
    var aspectIndicatorText by remember(stream.url) { mutableStateOf<String?>(null) }
    var seekOverlayVisible by remember(stream.url) { mutableStateOf(false) }
    var seekOverlayToken by remember(stream.url) { mutableIntStateOf(0) }
    var seekPreviewMs by remember(stream.url) { mutableStateOf<Long?>(null) }
    var seekRepeatDirection by remember(stream.url) { mutableIntStateOf(0) }
    var seekRepeatCount by remember(stream.url) { mutableIntStateOf(0) }
    var pauseOverlayVisible by remember(stream.url) { mutableStateOf(false) }
    var sidePanel by remember(stream.url) {
        mutableStateOf<TvPlayerSidePanel?>(null)
    }
    var pendingSidePanelFocus by remember(stream.url) {
        mutableStateOf<TvPlayerSidePanel?>(null)
    }
    var pendingEpisodeForSources by remember(stream.url) {
        mutableStateOf<MediaEpisode?>(null)
    }
    var stillWatchingVisible by remember(stream.url) { mutableStateOf(false) }
    var subtitleDelayMs by remember(stream.url) { mutableLongStateOf(0L) }
    var subtitleDelayBusy by remember(stream.url) { mutableStateOf(false) }
    var subtitleSizeIndex by remember(stream.url) { mutableIntStateOf(1) }
    var playerVideoWidth by remember(stream.url) { mutableIntStateOf(0) }
    var playerVideoHeight by remember(stream.url) { mutableIntStateOf(0) }

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
    val seekStepSeconds = preferences.tvSeekStepSeconds
    val seekStepMs = seekStepSeconds * 1_000L
    val controlsAutoHideMs = preferences.tvControlsAutoHideSeconds * 1_000L
    val subtitleTextFraction = when (subtitleSizeIndex) {
        0 -> 0.043f
        2 -> 0.061f
        else -> 0.052f
    }
    val focusedControlLabel = when (focusedControlId) {
        "previous" -> previousEpisodeTitle
            ?.takeIf { it.isNotBlank() }
            ?.let { "Previous · $it" }
            ?: "Previous episode"
        "rewind" -> "Rewind ${seekStepSeconds}s"
        "play" -> if (isPlaying) "Pause" else "Play"
        "forward" -> "Forward ${seekStepSeconds}s"
        "next" -> nextEpisodeTitle
            ?.takeIf { it.isNotBlank() }
            ?.let { "Next · $it" }
            ?: "Next episode"
        "subtitles" -> "Subtitles · $selectedSubtitleLabel"
        "audio" -> "Audio · $selectedAudioLabel"
        "episodes" -> "Episodes"
        "sources" -> "Sources"
        "speed" -> "Playback speed · ${formatTvSpeed(playbackSpeed)}"
        "aspect" -> "Aspect ratio"
        "subtitle-style" -> "Subtitle size · ${listOf("Small", "Medium", "Large")[subtitleSizeIndex]}"
        "info" -> "Stream info"
        "more" -> if (moreExpanded) "Close more actions" else "More actions"
        else -> null
    }

    fun revealControls() {
        seekOverlayVisible = false
        seekPreviewMs = null
        seekRepeatDirection = 0
        seekRepeatCount = 0
        controlsVisible = true
        interactionToken += 1
    }

    fun requestControlFocus() {
        revealControls()
        pendingControlFocus = true
    }

    fun restoreVideoFocus() {
        focusedControlId = null
        pendingControlFocus = false
        revealControls()
        runCatching { focusRequester.requestFocus() }
    }

    fun hideControlsFromRow() {
        focusedControlId = null
        pendingControlFocus = false
        moreExpanded = false
        seekOverlayVisible = false
        seekPreviewMs = null
        seekRepeatDirection = 0
        seekRepeatCount = 0
        controlsVisible = false
        interactionToken += 1
        runCatching { focusRequester.requestFocus() }
    }

    fun openSpeedPanel() {
        speedPanelVisible = true
        revealControls()
    }

    fun closeSpeedPanel() {
        speedPanelVisible = false
        pendingSpeedFocus = true
        revealControls()
    }

    fun setSpeed(speed: Float) {
        playbackSpeed = speed
        player.setPlaybackSpeed(speed)
        closeSpeedPanel()
    }

    fun cycleAspectRatio() {
        videoResizeMode = when (videoResizeMode) {
            AspectRatioFrameLayout.RESIZE_MODE_FIT -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
            AspectRatioFrameLayout.RESIZE_MODE_ZOOM -> AspectRatioFrameLayout.RESIZE_MODE_FILL
            else -> AspectRatioFrameLayout.RESIZE_MODE_FIT
        }
        aspectIndicatorText = when (videoResizeMode) {
            AspectRatioFrameLayout.RESIZE_MODE_ZOOM -> "Zoom"
            AspectRatioFrameLayout.RESIZE_MODE_FILL -> "Fill"
            else -> "Fit"
        }
        revealControls()
    }

    fun cancelAutoNext() {
        if (autoNextCountdown >= 0) {
            autoNextCountdown = -1
        }
    }

    fun reportProgress(
        completed: Boolean = false,
        force: Boolean = false
    ) {
        if (playbackCompleted && !completed) return

        val current = max(0L, player.currentPosition)
        val duration = player.duration
            .takeIf { it != C.TIME_UNSET && it > 0L }
            ?: durationMs
            .coerceAtLeast(0L)

        val enoughDelta = abs(current - lastProgressReportMs) >= 5_000L
        val eligible = current >= 15_000L

        if (completed || force || (eligible && enoughDelta)) {
            latestOnProgress?.invoke(
                current,
                duration,
                completed
            )
            lastProgressReportMs = current
        }

        if (completed) {
            playbackCompleted = true
        }
    }

    fun triggerPreviousEpisode() {
        val callback = onPreviousEpisode ?: return
        cancelAutoNext()
        reportProgress(force = true)
        callback()
    }

    fun triggerNextEpisode() {
        val callback = onNextEpisode ?: return
        cancelAutoNext()
        stillWatchingVisible = false
        reportProgress(force = true)
        callback()
    }

    fun triggerAutoNextEpisode() {
        val callback = onAutoNextEpisode ?: onNextEpisode ?: return
        cancelAutoNext()
        stillWatchingVisible = false
        reportProgress(force = true)
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
        pauseOverlayVisible = false
        if (player.playbackState == Player.STATE_ENDED) {
            playbackCompleted = false
            player.seekTo(0L)
            positionMs = 0L
        }
        if (player.isPlaying) player.pause() else player.play()
        revealControls()
    }

    fun resumeFromPauseOverlay() {
        if (playbackError != null) return
        pauseOverlayVisible = false
        controlsVisible = false
        moreExpanded = false
        focusedControlId = null
        interactionToken += 1
        player.play()
        runCatching { focusRequester.requestFocus() }
    }

    fun performSeek(deltaMs: Long, showControlsAfter: Boolean) {
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
        seekPreviewMs = null
        seekRepeatDirection = 0
        seekRepeatCount = 0
        if (showControlsAfter) {
            revealControls()
        } else {
            controlsVisible = false
            moreExpanded = false
            seekOverlayVisible = true
            seekOverlayToken += 1
        }
    }

    fun seekBy(deltaMs: Long) {
        performSeek(deltaMs = deltaMs, showControlsAfter = true)
    }

    fun previewSeekBy(direction: Int) {
        if (playbackError != null || direction == 0) return

        cancelAutoNext()
        if (seekRepeatDirection == direction) {
            seekRepeatCount += 1
        } else {
            seekRepeatDirection = direction
            seekRepeatCount = 1
        }

        val stepMs = when {
            seekRepeatCount >= 9 -> seekStepMs * 3L
            seekRepeatCount >= 5 -> seekStepMs * 2L
            else -> seekStepMs
        }
        val duration = player.duration
            .takeIf { it != C.TIME_UNSET && it > 0L }
            ?: durationMs.takeIf { it > 0L }
            ?: Long.MAX_VALUE
        val base = seekPreviewMs ?: max(0L, player.currentPosition)
        val target = (base + (stepMs * direction))
            .coerceAtLeast(0L)
            .let {
                if (duration == Long.MAX_VALUE) it else min(it, duration)
            }

        seekPreviewMs = target
        controlsVisible = false
        moreExpanded = false
        seekOverlayVisible = true
        seekOverlayToken += 1
    }

    fun commitPreviewSeek() {
        val target = seekPreviewMs ?: return
        player.seekTo(target)
        positionMs = target
        seekPreviewMs = null
        seekRepeatDirection = 0
        seekRepeatCount = 0
        seekOverlayVisible = true
        seekOverlayToken += 1
        interactionToken += 1
    }

    fun openTrackPanel(panel: TvPlayerTrackPanel) {
        if (playbackError != null) return
        cancelAutoNext()
        panelReturnFocus = if (focusedControlId != null) panel else null
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

    fun openSidePanel(panel: TvPlayerSidePanel) {
        cancelAutoNext()
        stillWatchingVisible = false
        if (panel != TvPlayerSidePanel.SOURCES) {
            pendingEpisodeForSources = null
        }
        sidePanel = panel
        controlsVisible = false
        moreExpanded = false
        focusedControlId = null
        pendingControlFocus = false
    }

    fun closeSidePanel() {
        if (
            sidePanel == TvPlayerSidePanel.SOURCES &&
            pendingEpisodeForSources != null
        ) {
            pendingEpisodeForSources = null
            sidePanel = TvPlayerSidePanel.EPISODES
            controlsVisible = false
            return
        }
        val closing = sidePanel
        pendingEpisodeForSources = null
        sidePanel = null
        pendingSidePanelFocus = closing
        controlsVisible = true
        interactionToken += 1
    }

    fun dismissSidePanelForPlayback() {
        val closing = sidePanel
        pendingEpisodeForSources = null
        sidePanel = null
        pendingSidePanelFocus = closing
        controlsVisible = false
        focusedControlId = null
        interactionToken += 1
    }

    fun applySubtitleDelay(targetMs: Long) {
        if (subtitleDelayBusy || onSelectSource == null) return
        val safe = targetMs.coerceIn(-10_000L, 10_000L)
        if (safe == subtitleDelayMs) return
        if (originalExternalSubtitles.isEmpty()) {
            subtitleDelayMs = 0L
            return
        }
        subtitleDelayBusy = true
        playerScope.launch {
            val shifted = runCatching {
                TvSubtitleShiftManager.shift(
                    context = context,
                    subtitles = originalExternalSubtitles,
                    offsetMs = safe
                )
            }.getOrElse { originalExternalSubtitles }
            reportProgress(force = true)
            subtitleDelayMs = safe
            subtitleDelayBusy = false
            onSelectSource(
                stream.copy(subtitles = shifted)
            )
        }
    }

    fun cycleSubtitleSize() {
        subtitleSizeIndex = (subtitleSizeIndex + 1) % 3
        revealControls()
    }

    DisposableEffect(player, stream.url) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(value: Boolean) {
                isPlaying = value
                if (value) {
                    pauseOverlayVisible = false
                } else {
                    controlsVisible = true
                }
            }

            override fun onPlaybackStateChanged(state: Int) {
                buffering = state == Player.STATE_BUFFERING ||
                    state == Player.STATE_IDLE

                if (state == Player.STATE_ENDED) {
                    reportProgress(
                        completed = true,
                        force = true
                    )
                    val hasNextEpisode =
                        onNextEpisode != null && preferences.autoplayNextEpisode
                    val needsStillWatching =
                        hasNextEpisode && stillWatchingGate && onStillWatchingContinue != null
                    stillWatchingVisible = needsStillWatching
                    controlsVisible = !hasNextEpisode && !needsStillWatching
                    moreExpanded = false
                    focusedControlId = null
                    autoNextCountdown = if (hasNextEpisode && !needsStillWatching) {
                        5
                    } else {
                        -1
                    }
                    if (hasNextEpisode || needsStillWatching) {
                        runCatching { focusRequester.requestFocus() }
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

            override fun onVideoSizeChanged(videoSize: VideoSize) {
                playerVideoWidth = videoSize.width
                playerVideoHeight = videoSize.height
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
        if (startPositionMs > 0L) {
            player.seekTo(startPositionMs)
            positionMs = startPositionMs
        }
        player.prepare()
        player.playWhenReady = true

        onDispose {
            if (!playbackCompleted) {
                reportProgress(force = true)
            }
            player.removeListener(listener)
            player.release()
        }
    }

    LaunchedEffect(player, stream.url) {
        while (true) {
            if (seekPreviewMs == null) {
                positionMs = max(0L, player.currentPosition)
            }
            bufferedMs = max(positionMs, player.bufferedPosition)
            durationMs = player.duration
                .takeIf { it != C.TIME_UNSET && it > 0L }
                ?: 0L
            if (player.isPlaying) {
                reportProgress()
            }
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
                triggerAutoNextEpisode()
            }
        }
    }

    LaunchedEffect(
        isPlaying,
        buffering,
        playbackError,
        autoNextCountdown,
        trackPanel,
        speedPanelVisible,
        sidePanel,
        stillWatchingVisible,
        stream.url
    ) {
        if (
            !isPlaying &&
            !buffering &&
            playbackError == null &&
            autoNextCountdown < 0 &&
            trackPanel == null &&
            !speedPanelVisible &&
            sidePanel == null &&
            !stillWatchingVisible &&
            player.playbackState == Player.STATE_READY
        ) {
            delay(5000)
            if (
                !isPlaying &&
                !buffering &&
                playbackError == null &&
                autoNextCountdown < 0 &&
                trackPanel == null &&
                !speedPanelVisible &&
                sidePanel == null &&
                !stillWatchingVisible &&
                player.playbackState == Player.STATE_READY
            ) {
                pauseOverlayVisible = true
                controlsVisible = false
                moreExpanded = false
                focusedControlId = null
                runCatching { focusRequester.requestFocus() }
            }
        } else {
            pauseOverlayVisible = false
        }
    }

    LaunchedEffect(
        interactionToken,
        isPlaying,
        playbackError,
        trackPanel,
        speedPanelVisible,
        sidePanel,
        stillWatchingVisible,
        focusedControlId
    ) {
        if (
            isPlaying &&
            playbackError == null &&
            trackPanel == null &&
            !speedPanelVisible &&
            sidePanel == null &&
            !stillWatchingVisible &&
            focusedControlId == null
        ) {
            val token = interactionToken
            delay(controlsAutoHideMs)
            if (
                token == interactionToken &&
                isPlaying &&
                playbackError == null &&
                trackPanel == null &&
                !speedPanelVisible &&
                sidePanel == null &&
                !stillWatchingVisible &&
                focusedControlId == null
            ) {
                controlsVisible = false
            }
        } else if (
            playbackError != null ||
            trackPanel != null ||
            speedPanelVisible ||
            sidePanel != null ||
            stillWatchingVisible ||
            focusedControlId != null
        ) {
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
            when (panelReturnFocus) {
                TvPlayerTrackPanel.AUDIO -> {
                    runCatching { audioControlRequester.requestFocus() }
                }

                TvPlayerTrackPanel.SUBTITLES -> {
                    runCatching { subtitleControlRequester.requestFocus() }
                }

                null -> {
                    runCatching { focusRequester.requestFocus() }
                }
            }
            panelReturnFocus = null
        }
    }

    LaunchedEffect(controlsVisible, pendingControlFocus) {
        if (controlsVisible && pendingControlFocus) {
            // Let the 200 ms controls fade complete before moving TV focus.
            delay(240)
            runCatching { playControlRequester.requestFocus() }
            pendingControlFocus = false
        }
    }

    LaunchedEffect(controlsVisible, pendingSpeedFocus) {
        if (controlsVisible && pendingSpeedFocus) {
            delay(40)
            runCatching { speedControlRequester.requestFocus() }
            pendingSpeedFocus = false
        }
    }

    LaunchedEffect(sidePanel, pendingSidePanelFocus, controlsVisible) {
        if (sidePanel == null && pendingSidePanelFocus != null && controlsVisible) {
            delay(80)
            when (pendingSidePanelFocus) {
                TvPlayerSidePanel.SOURCES ->
                    runCatching { sourcesControlRequester.requestFocus() }
                TvPlayerSidePanel.EPISODES ->
                    runCatching { episodesControlRequester.requestFocus() }
                TvPlayerSidePanel.STREAM_INFO ->
                    runCatching { infoControlRequester.requestFocus() }
                null -> Unit
            }
            pendingSidePanelFocus = null
        }
    }

    LaunchedEffect(aspectIndicatorText) {
        if (aspectIndicatorText != null) {
            delay(1600)
            aspectIndicatorText = null
        }
    }

    LaunchedEffect(seekOverlayToken, controlsVisible) {
        if (controlsVisible) {
            seekOverlayVisible = false
        } else if (seekOverlayVisible) {
            val token = seekOverlayToken
            delay(1200)
            if (token == seekOverlayToken && !controlsVisible) {
                seekOverlayVisible = false
            }
        }
    }

    BackHandler {
        when {
            sidePanel != null -> {
                closeSidePanel()
            }

            stillWatchingVisible -> {
                stillWatchingVisible = false
                controlsVisible = true
                requestControlFocus()
            }

            playbackError != null -> {
                reportProgress(force = true)
                onBack()
            }

            pauseOverlayVisible -> {
                pauseOverlayVisible = false
                controlsVisible = false
                focusedControlId = null
                pendingControlFocus = false
                moreExpanded = false
                runCatching { focusRequester.requestFocus() }
            }

            speedPanelVisible -> {
                closeSpeedPanel()
            }

            trackPanel != null -> {
                trackPanel = null
                revealControls()
            }

            autoNextCountdown >= 0 -> {
                cancelAutoNext()
                revealControls()
            }

            moreExpanded -> {
                moreExpanded = false
                revealControls()
            }

            focusedControlId != null -> {
                hideControlsFromRow()
            }

            controlsVisible -> {
                controlsVisible = false
                moreExpanded = false
                runCatching { focusRequester.requestFocus() }
            }

            seekPreviewMs != null || seekOverlayVisible -> {
                seekPreviewMs = null
                seekRepeatDirection = 0
                seekRepeatCount = 0
                seekOverlayVisible = false
            }

            else -> {
                reportProgress(force = true)
                onBack()
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusRequester(focusRequester)
            .onPreviewKeyEvent { event ->
                if (event.key == Key.Back) {
                    // Back belongs to BackHandler. Do not let generic TV input reveal
                    // controls before the back dispatcher evaluates the current layer.
                    false
                } else if (pauseOverlayVisible) {
                    if (event.type == KeyEventType.KeyDown) {
                        when (event.key) {
                            Key.DirectionCenter,
                            Key.Enter -> resumeFromPauseOverlay()

                            else -> {
                                pauseOverlayVisible = false
                                requestControlFocus()
                            }
                        }
                    }
                    true
                } else if (playbackError != null && event.type == KeyEventType.KeyDown) {
                    when (event.key) {
                        Key.DirectionCenter,
                        Key.Enter -> {
                            retryPlayback()
                            true
                        }
                        Key.DirectionRight -> {
                            if (sourceRequest != null && onSelectSource != null) {
                                openSidePanel(TvPlayerSidePanel.SOURCES)
                                true
                            } else {
                                false
                            }
                        }
                        else -> false
                    }
                } else if (autoNextCountdown >= 0 && event.type == KeyEventType.KeyDown) {
                    when (event.key) {
                        Key.DirectionCenter,
                        Key.Enter -> {
                            triggerNextEpisode()
                            true
                        }

                        Key.DirectionLeft,
                        Key.DirectionRight,
                        Key.DirectionUp,
                        Key.DirectionDown -> {
                            cancelAutoNext()
                            requestControlFocus()
                            true
                        }

                        else -> false
                    }
                } else if (event.type == KeyEventType.KeyUp &&
                    focusedControlId == null &&
                    (event.key == Key.DirectionLeft || event.key == Key.DirectionRight) &&
                    seekPreviewMs != null
                ) {
                    commitPreviewSeek()
                    true
                } else if (event.type != KeyEventType.KeyDown) {
                    false
                } else if (
                    trackPanel != null ||
                    speedPanelVisible ||
                    sidePanel != null ||
                    stillWatchingVisible
                ) {
                    false
                } else if (focusedControlId != null) {
                    when (event.key) {
                        Key.MediaPrevious -> {
                            if (onPreviousEpisode != null) {
                                triggerPreviousEpisode()
                                true
                            } else {
                                false
                            }
                        }

                        Key.MediaNext -> {
                            if (onNextEpisode != null) {
                                triggerNextEpisode()
                                true
                            } else {
                                false
                            }
                        }

                        else -> {
                            revealControls()
                            false
                        }
                    }
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
                            previewSeekBy(-1)
                            true
                        }

                        Key.DirectionRight -> {
                            previewSeekBy(1)
                            true
                        }

                        Key.DirectionUp,
                        Key.DirectionDown -> {
                            requestControlFocus()
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
                    resizeMode = videoResizeMode
                    setShutterBackgroundColor(
                        android.graphics.Color.BLACK
                    )
                    this.player = player
                    subtitleView?.setFractionalTextSize(subtitleTextFraction)
                }
            },
            update = { view ->
                if (view.player !== player) {
                    view.player = player
                }
                if (view.resizeMode != videoResizeMode) {
                    view.resizeMode = videoResizeMode
                }
                view.subtitleView?.setFractionalTextSize(subtitleTextFraction)
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
                            text = if (sourceRequest != null && onSelectSource != null) {
                                "OK Retry  •  Right Sources  •  Back Exit"
                            } else {
                                "OK Retry  •  Back Exit"
                            },
                            color = TvColors.Accent,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = pauseOverlayVisible && playbackError == null && !buffering,
            enter = fadeIn(animationSpec = tween(200)),
            exit = fadeOut(animationSpec = tween(160)),
            modifier = Modifier.fillMaxSize()
        ) {
            TvPlayerPauseOverlay(
                title = title,
                episodeTitle = episodeTitle,
                positionMs = positionMs,
                durationMs = durationMs
            )
        }

        AnimatedVisibility(
            visible = controlsVisible &&
                playbackError == null &&
                !pauseOverlayVisible &&
                autoNextCountdown < 0 &&
                trackPanel == null &&
                !speedPanelVisible &&
                !buffering,
            enter = fadeIn(animationSpec = tween(150)),
            exit = fadeOut(animationSpec = tween(150)),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 30.dp, end = 34.dp)
        ) {
            TvPlayerClockOverlay(
                positionMs = positionMs,
                durationMs = durationMs,
                playbackSpeed = playbackSpeed
            )
        }

        AnimatedVisibility(
            visible = controlsVisible && playbackError == null && !pauseOverlayVisible && autoNextCountdown < 0,
            enter = fadeIn(animationSpec = tween(200)),
            exit = fadeOut(animationSpec = tween(200)),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .align(Alignment.TopCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.70f),
                                    Color.Transparent
                                )
                            )
                        )
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.80f)
                                )
                            )
                        )
                )
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .padding(
                            horizontal = 34.dp,
                            vertical = 28.dp
                        ),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = title,
                            color = Color.White,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        episodeTitle
                            ?.takeIf { it.isNotBlank() }
                            ?.let { episode ->
                                Text(
                                    text = episode,
                                    color = Color.White.copy(alpha = 0.90f),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                    }

                    TvPlayerProgress(
                        positionMs = positionMs,
                        bufferedMs = bufferedMs,
                        durationMs = durationMs,
                        seekStepMs = seekStepMs,
                        focusRequester = progressControlRequester,
                        focused = focusedControlId == "progress",
                        onFocusedChange = { focused ->
                            when {
                                focused -> focusedControlId = "progress"
                                focusedControlId == "progress" -> focusedControlId = null
                            }
                        },
                        onSeekBy = ::seekBy,
                        onDown = {
                            runCatching { playControlRequester.requestFocus() }
                        },
                        onUp = ::hideControlsFromRow
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (onPreviousEpisode != null) {
                                TvPlayerControlButton(
                                    id = "previous",
                                    icon = Icons.Default.SkipPrevious,
                                    label = "Previous episode",
                                    upFocusRequester = progressControlRequester,
                                    focusedId = focusedControlId,
                                    onFocusedIdChange = { focusedControlId = it },
                                    onClick = ::triggerPreviousEpisode,
                                    onDownKey = ::hideControlsFromRow
                                )
                            }
                            TvPlayerControlButton(
                                id = "rewind",
                                icon = Icons.Default.KeyboardArrowLeft,
                                label = "Rewind ${seekStepSeconds}s",
                                upFocusRequester = progressControlRequester,
                                focusedId = focusedControlId,
                                onFocusedIdChange = { focusedControlId = it },
                                onClick = { seekBy(-seekStepMs) },
                                onDownKey = ::hideControlsFromRow
                            )
                            TvPlayerControlButton(
                                id = "play",
                                icon = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                label = if (isPlaying) "Pause" else "Play",
                                focusRequester = playControlRequester,
                                upFocusRequester = progressControlRequester,
                                focusedId = focusedControlId,
                                onFocusedIdChange = { focusedControlId = it },
                                onClick = ::togglePlayback,
                                onDownKey = ::hideControlsFromRow
                            )
                            TvPlayerControlButton(
                                id = "forward",
                                icon = Icons.Default.KeyboardArrowRight,
                                label = "Forward ${seekStepSeconds}s",
                                upFocusRequester = progressControlRequester,
                                focusedId = focusedControlId,
                                onFocusedIdChange = { focusedControlId = it },
                                onClick = { seekBy(seekStepMs) },
                                onDownKey = ::hideControlsFromRow
                            )
                            if (onNextEpisode != null) {
                                TvPlayerControlButton(
                                    id = "next",
                                    icon = Icons.Default.SkipNext,
                                    label = "Next episode",
                                    upFocusRequester = progressControlRequester,
                                    focusedId = focusedControlId,
                                    onFocusedIdChange = { focusedControlId = it },
                                    onClick = ::triggerNextEpisode,
                                    onDownKey = ::hideControlsFromRow
                                )
                            }
                            TvPlayerControlButton(
                                id = "subtitles",
                                icon = Icons.Default.ClosedCaption,
                                label = "Subtitles: $selectedSubtitleLabel",
                                focusRequester = subtitleControlRequester,
                                upFocusRequester = progressControlRequester,
                                focusedId = focusedControlId,
                                onFocusedIdChange = { focusedControlId = it },
                                onClick = {
                                    openTrackPanel(TvPlayerTrackPanel.SUBTITLES)
                                },
                                onDownKey = ::hideControlsFromRow
                            )
                            TvPlayerControlButton(
                                id = "audio",
                                icon = Icons.AutoMirrored.Filled.VolumeUp,
                                label = "Audio: $selectedAudioLabel",
                                focusRequester = audioControlRequester,
                                upFocusRequester = progressControlRequester,
                                focusedId = focusedControlId,
                                onFocusedIdChange = { focusedControlId = it },
                                onClick = {
                                    openTrackPanel(TvPlayerTrackPanel.AUDIO)
                                },
                                onDownKey = ::hideControlsFromRow
                            )
                            if (episodes.isNotEmpty() && currentEpisode != null && onSelectEpisode != null) {
                                TvPlayerControlButton(
                                    id = "episodes",
                                    icon = Icons.Default.List,
                                    label = "Episodes",
                                    focusRequester = episodesControlRequester,
                                    upFocusRequester = progressControlRequester,
                                    focusedId = focusedControlId,
                                    onFocusedIdChange = { focusedControlId = it },
                                    onClick = {
                                        openSidePanel(TvPlayerSidePanel.EPISODES)
                                    },
                                    onDownKey = ::hideControlsFromRow
                                )
                            }
                            if (sourceRequest != null && onSelectSource != null) {
                                TvPlayerControlButton(
                                    id = "sources",
                                    icon = Icons.Default.SwapHoriz,
                                    label = "Sources",
                                    focusRequester = sourcesControlRequester,
                                    upFocusRequester = progressControlRequester,
                                    focusedId = focusedControlId,
                                    onFocusedIdChange = { focusedControlId = it },
                                    onClick = {
                                        openSidePanel(TvPlayerSidePanel.SOURCES)
                                    },
                                    onDownKey = ::hideControlsFromRow
                                )
                            }
                            AnimatedVisibility(
                                visible = moreExpanded,
                                enter = slideInHorizontally(
                                    animationSpec = tween(180),
                                    initialOffsetX = { it / 2 }
                                ) + fadeIn(animationSpec = tween(180)),
                                exit = slideOutHorizontally(
                                    animationSpec = tween(160),
                                    targetOffsetX = { it / 2 }
                                ) + fadeOut(animationSpec = tween(160))
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    TvPlayerControlButton(
                                        id = "speed",
                                        icon = Icons.Default.Speed,
                                        label = "Playback speed ${formatTvSpeed(playbackSpeed)}",
                                        focusRequester = speedControlRequester,
                                        upFocusRequester = progressControlRequester,
                                        focusedId = focusedControlId,
                                        onFocusedIdChange = { focusedControlId = it },
                                        onClick = ::openSpeedPanel,
                                        onDownKey = ::hideControlsFromRow
                                    )
                                    TvPlayerControlButton(
                                        id = "aspect",
                                        icon = Icons.Default.AspectRatio,
                                        label = "Aspect ratio",
                                        upFocusRequester = progressControlRequester,
                                        focusedId = focusedControlId,
                                        onFocusedIdChange = { focusedControlId = it },
                                        onClick = ::cycleAspectRatio,
                                        onDownKey = ::hideControlsFromRow
                                    )
                                    TvPlayerControlButton(
                                        id = "subtitle-style",
                                        icon = Icons.Default.ClosedCaption,
                                        label = "Subtitle size",
                                        upFocusRequester = progressControlRequester,
                                        focusedId = focusedControlId,
                                        onFocusedIdChange = { focusedControlId = it },
                                        onClick = ::cycleSubtitleSize,
                                        onDownKey = ::hideControlsFromRow
                                    )
                                    TvPlayerControlButton(
                                        id = "info",
                                        icon = Icons.Default.Info,
                                        label = "Stream info",
                                        focusRequester = infoControlRequester,
                                        upFocusRequester = progressControlRequester,
                                        focusedId = focusedControlId,
                                        onFocusedIdChange = { focusedControlId = it },
                                        onClick = {
                                            openSidePanel(TvPlayerSidePanel.STREAM_INFO)
                                        },
                                        onDownKey = ::hideControlsFromRow
                                    )
                                }
                            }
                            TvPlayerControlButton(
                                id = "more",
                                icon = if (moreExpanded) {
                                    Icons.Default.KeyboardArrowLeft
                                } else {
                                    Icons.Default.KeyboardArrowRight
                                },
                                label = if (moreExpanded) "Close more actions" else "More actions",
                                upFocusRequester = progressControlRequester,
                                focusedId = focusedControlId,
                                onFocusedIdChange = { focusedControlId = it },
                                onClick = {
                                    moreExpanded = !moreExpanded
                                    revealControls()
                                },
                                onDownKey = ::hideControlsFromRow
                            )
                        }

                        Text(
                            text = "${formatTvTime(positionMs)} / ${formatTvTime(durationMs)}",
                            color = Color.White.copy(alpha = 0.90f),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Text(
                        text = focusedControlLabel
                            ?: "OK Play/Pause  •  Left/Right Seek  •  Down Hide controls",
                        color = Color.White.copy(alpha = if (focusedControlLabel != null) 0.92f else 0.58f),
                        fontSize = 12.sp,
                        fontWeight = if (focusedControlLabel != null) FontWeight.SemiBold else FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = seekOverlayVisible && !controlsVisible && playbackError == null,
            enter = fadeIn(animationSpec = tween(150)),
            exit = fadeOut(animationSpec = tween(150)),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            TvPlayerSeekOverlay(
                positionMs = seekPreviewMs ?: positionMs,
                bufferedMs = bufferedMs,
                durationMs = durationMs
            )
        }

        aspectIndicatorText?.let { label ->
            Surface(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 72.dp),
                color = Color.Black.copy(alpha = 0.86f),
                shape = RoundedCornerShape(24.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.AspectRatio,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = label,
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        if (
            autoNextCountdown >= 0 &&
            onNextEpisode != null &&
            playbackError == null
        ) {
            TvPlayerPostPlayOverlay(
                countdown = autoNextCountdown,
                nextEpisodeTitle = nextEpisodeTitle,
                onPlayNow = ::triggerNextEpisode,
                onCancel = {
                    cancelAutoNext()
                    requestControlFocus()
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 34.dp, bottom = 42.dp)
            )
        }

        if (speedPanelVisible) {
            TvPlayerSpeedPicker(
                selectedSpeed = playbackSpeed,
                onSelect = ::setSpeed,
                onClose = ::closeSpeedPanel,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 28.dp)
            )
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
                subtitleDelayMs = if (panel == TvPlayerTrackPanel.SUBTITLES) {
                    subtitleDelayMs
                } else {
                    null
                },
                subtitleDelayBusy = subtitleDelayBusy,
                subtitleSizeLabel = if (panel == TvPlayerTrackPanel.SUBTITLES) {
                    listOf("Small", "Medium", "Large")[subtitleSizeIndex]
                } else {
                    null
                },
                onSubtitleDelayAdjust = { delta ->
                    applySubtitleDelay(subtitleDelayMs + delta)
                },
                onSubtitleDelayReset = {
                    applySubtitleDelay(0L)
                },
                onSubtitleSizeCycle = ::cycleSubtitleSize,
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

        when (sidePanel) {
            TvPlayerSidePanel.SOURCES -> {
                val request = sourceRequest
                val select = onSelectSource
                if (request != null && select != null) {
                    val panelRequest = pendingEpisodeForSources?.let { episode ->
                        request.copy(
                            episode = episode,
                            autoPlay = false,
                            preferredProviderId = stream.providerId,
                            preferredProviderName = stream.providerName
                        )
                    } ?: request
                    TvPlayerSourcesPanel(
                        request = panelRequest,
                        currentStream = stream,
                        onSelect = { selected ->
                            val pendingEpisode = pendingEpisodeForSources
                            reportProgress(force = true)
                            dismissSidePanelForPlayback()
                            if (pendingEpisode != null && onSelectEpisodeSource != null) {
                                onSelectEpisodeSource(pendingEpisode, selected)
                            } else {
                                select(selected)
                            }
                        },
                        onClose = ::closeSidePanel,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            TvPlayerSidePanel.EPISODES -> {
                val selectEpisode = onSelectEpisode
                if (episodes.isNotEmpty() && currentEpisode != null && selectEpisode != null) {
                    TvPlayerEpisodesPanel(
                        episodes = episodes,
                        currentEpisode = currentEpisode,
                        isWatched = isEpisodeWatched,
                        onSelect = { episode ->
                            if (onSelectEpisodeSource != null && sourceRequest != null) {
                                pendingEpisodeForSources = episode
                                sidePanel = TvPlayerSidePanel.SOURCES
                                controlsVisible = false
                            } else {
                                closeSidePanel()
                                reportProgress(force = true)
                                selectEpisode(episode)
                            }
                        },
                        onClose = ::closeSidePanel,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            TvPlayerSidePanel.STREAM_INFO -> {
                TvPlayerStreamInfoPanel(
                    stream = stream,
                    videoWidth = playerVideoWidth,
                    videoHeight = playerVideoHeight,
                    audioLabel = selectedAudioLabel,
                    subtitleLabel = selectedSubtitleLabel,
                    playbackSpeed = playbackSpeed,
                    onClose = ::closeSidePanel,
                    modifier = Modifier.fillMaxSize()
                )
            }

            null -> Unit
        }

        if (stillWatchingVisible && onStillWatchingContinue != null) {
            TvPlayerStillWatchingOverlay(
                title = title,
                nextEpisodeTitle = nextEpisodeTitle,
                onContinue = {
                    stillWatchingVisible = false
                    onStillWatchingContinue()
                },
                onStop = {
                    stillWatchingVisible = false
                    controlsVisible = true
                    requestControlFocus()
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}


@Composable
private fun TvPlayerControlButton(
    id: String,
    icon: ImageVector,
    label: String,
    enabled: Boolean = true,
    focusRequester: FocusRequester? = null,
    upFocusRequester: FocusRequester? = null,
    focusedId: String?,
    onFocusedIdChange: (String?) -> Unit,
    onClick: () -> Unit,
    onDownKey: () -> Unit
) {
    val fallbackRequester = remember(id) { FocusRequester() }
    val requester = focusRequester ?: fallbackRequester
    var focused by remember(id) { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .size(52.dp)
            .focusRequester(requester)
            .onFocusChanged { state ->
                focused = state.hasFocus
                when {
                    state.hasFocus -> onFocusedIdChange(id)
                    focusedId == id -> onFocusedIdChange(null)
                }
            }
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) {
                    false
                } else {
                    when (event.key) {
                        Key.DirectionUp -> {
                            if (upFocusRequester != null) {
                                runCatching { upFocusRequester.requestFocus() }
                                true
                            } else {
                                false
                            }
                        }

                        Key.DirectionDown -> {
                            onDownKey()
                            true
                        }

                        Key.DirectionCenter,
                        Key.Enter -> {
                            if (enabled) onClick()
                            true
                        }

                        else -> false
                    }
                }
            }
            .focusable(enabled = enabled),
        color = when {
            focused -> Color.White
            !enabled -> Color.White.copy(alpha = 0.05f)
            else -> Color.Transparent
        },
        shape = CircleShape
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = when {
                    focused -> Color.Black
                    enabled -> Color.White
                    else -> Color.White.copy(alpha = 0.32f)
                },
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@Composable
private fun TvPlayerSpeedPicker(
    selectedSpeed: Float,
    onSelect: (Float) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val speeds = remember { listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f) }
    val selectedRequester = remember(selectedSpeed) { FocusRequester() }

    LaunchedEffect(selectedSpeed) {
        delay(60)
        runCatching { selectedRequester.requestFocus() }
    }

    Surface(
        modifier = modifier
            .width(360.dp)
            .height(430.dp),
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
                text = "Playback speed",
                color = TvColors.TextPrimary,
                fontSize = 23.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = if (hasSubtitleControls) {
                    "Left/Right adjusts timing  •  OK selects or resets  •  Back closes"
                } else {
                    "OK Select  •  Left/Back Close"
                },
                color = TvColors.TextSecondary,
                fontSize = 12.sp
            )
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(
                    items = speeds,
                    key = { _, speed -> speed }
                ) { _, speed ->
                    TvPlayerSpeedRow(
                        speed = speed,
                        selected = kotlin.math.abs(speed - selectedSpeed) < 0.001f,
                        focusRequester = if (kotlin.math.abs(speed - selectedSpeed) < 0.001f) {
                            selectedRequester
                        } else {
                            null
                        },
                        onSelect = { onSelect(speed) },
                        onClose = onClose
                    )
                }
            }
        }
    }
}

@Composable
private fun TvPlayerSpeedRow(
    speed: Float,
    selected: Boolean,
    focusRequester: FocusRequester?,
    onSelect: () -> Unit,
    onClose: () -> Unit
) {
    var focused by remember(speed) { mutableStateOf(false) }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .then(
                if (focusRequester != null) {
                    Modifier.focusRequester(focusRequester)
                } else {
                    Modifier
                }
            )
            .onFocusChanged { focused = it.hasFocus }
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) {
                    false
                } else {
                    when (event.key) {
                        Key.DirectionLeft -> {
                            onClose()
                            true
                        }
                        Key.DirectionCenter,
                        Key.Enter -> {
                            onSelect()
                            true
                        }
                        else -> false
                    }
                }
            }
            .focusable(),
        color = when {
            focused -> TvColors.FocusRing
            selected -> TvColors.Accent.copy(alpha = 0.18f)
            else -> Color.White.copy(alpha = 0.04f)
        },
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = formatTvSpeed(speed),
                color = if (focused) TvColors.Background else TvColors.TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
            if (selected) {
                Text(
                    text = "Selected",
                    color = if (focused) {
                        TvColors.Background.copy(alpha = 0.72f)
                    } else {
                        TvColors.Accent
                    },
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun TvPlayerTrackPicker(
    title: String,
    options: List<TvPlayerTrackOption>,
    showOff: Boolean,
    offSelected: Boolean,
    subtitleDelayMs: Long? = null,
    subtitleDelayBusy: Boolean = false,
    subtitleSizeLabel: String? = null,
    onSubtitleDelayAdjust: (Long) -> Unit = {},
    onSubtitleDelayReset: () -> Unit = {},
    onSubtitleSizeCycle: () -> Unit = {},
    onSelect: (TvPlayerTrackOption) -> Unit,
    onOff: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val firstRequester = remember(title) { FocusRequester() }
    val hasSubtitleControls = subtitleDelayMs != null
    val hasFirstTarget = hasSubtitleControls || showOff || options.isNotEmpty()

    LaunchedEffect(title, options.size, showOff, subtitleDelayMs) {
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
                    if (subtitleDelayMs != null) {
                        item(key = "subtitle-delay") {
                            TvPlayerSubtitleDelayRow(
                                delayMs = subtitleDelayMs,
                                busy = subtitleDelayBusy,
                                focusRequester = firstRequester,
                                onAdjust = onSubtitleDelayAdjust,
                                onReset = onSubtitleDelayReset
                            )
                        }
                        item(key = "subtitle-size") {
                            TvPlayerSubtitleSizeRow(
                                label = subtitleSizeLabel ?: "Medium",
                                onCycle = onSubtitleSizeCycle
                            )
                        }
                    }
                    if (showOff) {
                        item(key = "off") {
                            TvPlayerTrackRow(
                                label = "Off",
                                selected = offSelected,
                                focusRequester = if (hasSubtitleControls) null else firstRequester,
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
                            focusRequester = if (!hasSubtitleControls && !showOff && index == 0) {
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
private fun TvPlayerSubtitleDelayRow(
    delayMs: Long,
    busy: Boolean,
    focusRequester: FocusRequester,
    onAdjust: (Long) -> Unit,
    onReset: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
            .focusRequester(focusRequester)
            .onFocusChanged { focused = it.hasFocus }
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown || busy) {
                    busy
                } else {
                    when (event.key) {
                        Key.DirectionLeft -> {
                            onAdjust(-250L)
                            true
                        }
                        Key.DirectionRight -> {
                            onAdjust(250L)
                            true
                        }
                        Key.DirectionCenter,
                        Key.Enter -> {
                            onReset()
                            true
                        }
                        else -> false
                    }
                }
            }
            .focusable(),
        color = if (focused) TvColors.FocusRing else TvColors.SurfaceVariant,
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 15.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "Subtitle timing",
                    color = if (focused) TvColors.Background else TvColors.TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = if (busy) "Applying external subtitle timing..." else "Left earlier · Right later · OK reset",
                    color = if (focused) {
                        TvColors.Background.copy(alpha = 0.64f)
                    } else {
                        TvColors.TextSecondary
                    },
                    fontSize = 10.sp
                )
            }
            Text(
                text = formatTvSubtitleDelay(delayMs),
                color = if (focused) TvColors.Background else TvColors.Accent,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun TvPlayerSubtitleSizeRow(
    label: String,
    onCycle: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .onFocusChanged { focused = it.hasFocus }
            .onPreviewKeyEvent { event ->
                if (
                    event.type == KeyEventType.KeyDown &&
                    (event.key == Key.DirectionCenter || event.key == Key.Enter)
                ) {
                    onCycle()
                    true
                } else {
                    false
                }
            }
            .focusable(),
        color = if (focused) TvColors.FocusRing else TvColors.SurfaceVariant,
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
                text = "Subtitle size",
                color = if (focused) TvColors.Background else TvColors.TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = label,
                color = if (focused) TvColors.Background else TvColors.Accent,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

private fun formatTvSubtitleDelay(delayMs: Long): String {
    if (delayMs == 0L) return "0.00s"
    val sign = if (delayMs > 0L) "+" else "-"
    return "$sign${"%.2f".format(Locale.US, kotlin.math.abs(delayMs) / 1000.0)}s"
}

@Composable
private fun TvPlayerTrackRow(
    label: String,
    selected: Boolean,
    focusRequester: FocusRequester?,
    onSelect: () -> Unit,
    onClose: () -> Unit
) {
    val fallbackRequester = remember(label) { FocusRequester() }
    val requester = focusRequester ?: fallbackRequester
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
    durationMs: Long,
    seekStepMs: Long,
    focusRequester: FocusRequester,
    focused: Boolean,
    onFocusedChange: (Boolean) -> Unit,
    onSeekBy: (Long) -> Unit,
    onDown: () -> Unit,
    onUp: () -> Unit
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
    val animatedPlayedFraction by animateFloatAsState(
        targetValue = playedFraction,
        animationSpec = tween(100),
        label = "tv-player-played-progress"
    )
    val animatedBufferedFraction by animateFloatAsState(
        targetValue = bufferedFraction,
        animationSpec = tween(200),
        label = "tv-player-buffered-progress"
    )
    val animatedHeight by animateDpAsState(
        targetValue = if (focused) 8.dp else 5.dp,
        animationSpec = tween(120),
        label = "tv-player-progress-height"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(animatedHeight)
            .focusRequester(focusRequester)
            .onFocusChanged { onFocusedChange(it.hasFocus) }
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) {
                    false
                } else {
                    when (event.key) {
                        Key.DirectionLeft -> {
                            onSeekBy(-seekStepMs)
                            true
                        }
                        Key.DirectionRight -> {
                            onSeekBy(seekStepMs)
                            true
                        }
                        Key.DirectionDown -> {
                            onDown()
                            true
                        }
                        Key.DirectionUp -> {
                            onUp()
                            true
                        }
                        else -> false
                    }
                }
            }
            .focusable()
            .background(
                Color.White.copy(alpha = if (focused) 0.45f else 0.30f),
                RoundedCornerShape(99.dp)
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(animatedBufferedFraction)
                .background(
                    Color.White.copy(alpha = 0.38f),
                    RoundedCornerShape(99.dp)
                )
        )

        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(animatedPlayedFraction)
                .background(
                    TvColors.Accent,
                    RoundedCornerShape(99.dp)
                )
        )
    }
}

@Composable
private fun TvPlayerClockOverlay(
    positionMs: Long,
    durationMs: Long,
    playbackSpeed: Float
) {
    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val context = LocalContext.current
    val formatter = remember(context) { DateFormat.getTimeFormat(context) }

    LaunchedEffect(Unit) {
        while (true) {
            val current = System.currentTimeMillis()
            nowMs = current
            delay((1_000L - (current % 1_000L)).coerceAtLeast(250L))
        }
    }

    val remainingMediaMs = (durationMs - positionMs).coerceAtLeast(0L)
    val effectiveSpeed = playbackSpeed.takeIf { it > 0f } ?: 1f
    val remainingRealMs = (remainingMediaMs.toDouble() / effectiveSpeed.toDouble()).toLong()
    val endsAt = if (durationMs > 0L) {
        formatter.format(Date(nowMs + remainingRealMs))
    } else {
        "--:--"
    }

    Column(
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(1.dp)
    ) {
        Text(
            text = formatter.format(Date(nowMs)),
            color = Color.White.copy(alpha = 0.96f),
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = "Ends at $endsAt",
            color = Color.White.copy(alpha = 0.76f),
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun TvPlayerPostPlayOverlay(
    countdown: Int,
    nextEpisodeTitle: String?,
    onPlayNow: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val progress = (countdown.coerceIn(0, 5) / 5f).coerceIn(0f, 1f)

    Surface(
        modifier = modifier.width(410.dp),
        color = Color.Black.copy(alpha = 0.90f),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = if (countdown > 0) {
                    "Up next in ${countdown}s"
                } else {
                    "Opening next episode..."
                },
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            nextEpisodeTitle
                ?.takeIf { it.isNotBlank() }
                ?.let { nextTitle ->
                    Text(
                        text = nextTitle,
                        color = Color.White.copy(alpha = 0.82f),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(
                        Color.White.copy(alpha = 0.22f),
                        RoundedCornerShape(2.dp)
                    )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(progress)
                        .background(
                            Color.White,
                            RoundedCornerShape(2.dp)
                        )
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    onClick = onPlayNow,
                    color = Color.White,
                    contentColor = Color.Black,
                    shape = RoundedCornerShape(22.dp)
                ) {
                    Text(
                        text = "Play now",
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 9.dp),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = "OK Play now  •  Back Cancel",
                    color = Color.White.copy(alpha = 0.62f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun TvPlayerPauseOverlay(
    title: String,
    episodeTitle: String?,
    positionMs: Long,
    durationMs: Long
) {
    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val context = LocalContext.current
    val formatter = remember(context) { DateFormat.getTimeFormat(context) }

    LaunchedEffect(Unit) {
        while (true) {
            val current = System.currentTimeMillis()
            nowMs = current
            delay((60_000L - (current % 60_000L)).coerceAtLeast(1_000L))
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.Black.copy(alpha = 0.24f),
                        Color.Black.copy(alpha = 0.50f),
                        Color.Black.copy(alpha = 0.88f)
                    )
                )
            )
    ) {
        Text(
            text = formatter.format(Date(nowMs)),
            color = Color.White.copy(alpha = 0.95f),
            fontSize = 34.sp,
            fontWeight = FontWeight.Normal,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 40.dp, end = 64.dp)
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth(0.72f)
                .padding(start = 64.dp, end = 48.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "You are watching",
                color = Color.White.copy(alpha = 0.58f),
                fontSize = 17.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = title,
                color = Color.White,
                fontSize = 34.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (!episodeTitle.isNullOrBlank()) {
                Text(
                    text = episodeTitle,
                    color = Color.White.copy(alpha = 0.92f),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = "Paused  •  ${formatTvTime(positionMs)} / ${formatTvTime(durationMs)}",
                color = Color.White.copy(alpha = 0.70f),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "OK to resume  •  Any direction for controls",
                color = Color.White.copy(alpha = 0.54f),
                fontSize = 13.sp
            )
        }
    }
}

@Composable
private fun TvPlayerSeekOverlay(
    positionMs: Long,
    bufferedMs: Long,
    durationMs: Long
) {
    val playedFraction = if (durationMs > 0L) {
        (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }
    val bufferedFraction = if (durationMs > 0L) {
        (bufferedMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }
    val animatedPlayedFraction by animateFloatAsState(
        targetValue = playedFraction,
        animationSpec = tween(100),
        label = "tv-player-seek-overlay-played"
    )
    val animatedBufferedFraction by animateFloatAsState(
        targetValue = bufferedFraction,
        animationSpec = tween(200),
        label = "tv-player-seek-overlay-buffered"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color.Black.copy(alpha = 0.80f)
                    )
                )
            )
            .padding(horizontal = 34.dp, vertical = 28.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(5.dp)
                .background(
                    Color.White.copy(alpha = 0.30f),
                    RoundedCornerShape(99.dp)
                )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(animatedBufferedFraction)
                    .background(
                        TvColors.Accent.copy(alpha = 0.35f),
                        RoundedCornerShape(99.dp)
                    )
            )
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(animatedPlayedFraction)
                    .background(
                        TvColors.Accent,
                        RoundedCornerShape(99.dp)
                    )
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${formatTvTime(positionMs)} / ${formatTvTime(durationMs)}",
                color = Color.White.copy(alpha = 0.90f),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

private fun formatTvSpeed(speed: Float): String {
    return if (kotlin.math.abs(speed - speed.toInt().toFloat()) < 0.001f) {
        "${speed.toInt()}x"
    } else {
        "${speed}x"
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
