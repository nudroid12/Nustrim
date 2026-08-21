package app.nudroidlabs.nustrim.tv.player

import android.view.KeyEvent as AndroidKeyEvent
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView
import app.nudroidlabs.nustrim.tv.episode.TvCanonicalEpisode
import app.nudroidlabs.nustrim.tv.episode.TvEpisodeCatalogue
import app.nudroidlabs.nustrim.tv.sources.TvSourceStream
import app.nudroidlabs.nustrim.tv.sources.TvSourcesSnapshot
import coil3.compose.AsyncImage
import kotlinx.coroutines.delay

@OptIn(UnstableApi::class)
@Composable
fun TvPlayerScreen(
    request: TvPlaybackRequest,
    runtime: TvPlayerRuntime,
    episodeCatalogue: TvEpisodeCatalogue,
    sourceSnapshot: TvSourcesSnapshot?,
    sourcesLoading: Boolean,
    sourcesError: String?,
    onRefreshSources: () -> Unit,
    onSwitchSource: (TvSourceStream) -> Unit,
    onEpisodeSelected: (TvCanonicalEpisode) -> Unit,
    onExitPlayer: () -> Unit,
    onReturnToDetails: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val containerFocusRequester = remember { FocusRequester() }
    val playPauseFocusRequester = remember { FocusRequester() }
    val progressFocusRequester = remember { FocusRequester() }

    var showControls by remember(request.stableKey) { mutableStateOf(true) }
    var activePanel by remember(request.stableKey) { mutableStateOf<TvPlayerPanel?>(null) }
    var pauseOverlay by remember(request.stableKey) { mutableStateOf(false) }
    var aspectMode by remember(request.mediaKey) { mutableStateOf(TvPlayerAspectMode.FIT) }
    var interactionToken by remember { mutableIntStateOf(0) }
    var seekOverlayToken by remember { mutableIntStateOf(0) }
    var showSeekOverlay by remember { mutableStateOf(false) }

    val currentCanonical = remember(episodeCatalogue, request.episode?.id) {
        request.episode?.id?.let { id ->
            episodeCatalogue.episodes.firstOrNull { it.providerEpisodeId == id }
        }
    }
    val nextEpisode = remember(episodeCatalogue, currentCanonical?.identity?.stableKey) {
        val current = currentCanonical ?: return@remember null
        val index = episodeCatalogue.episodes.indexOfFirst {
            it.identity.stableKey == current.identity.stableKey
        }
        episodeCatalogue.episodes.getOrNull(index + 1)
    }

    fun interact() {
        interactionToken += 1
    }

    fun revealControls() {
        pauseOverlay = false
        showControls = true
        interact()
    }

    fun seekHidden(deltaMs: Long) {
        runtime.seekBy(deltaMs)
        showSeekOverlay = true
        seekOverlayToken += 1
        interact()
    }

    fun pauseIntoOverlay() {
        runtime.pause()
        showControls = false
        pauseOverlay = true
        interact()
    }

    fun resumeWithoutControls() {
        runtime.play()
        pauseOverlay = false
        showControls = false
        interact()
    }

    fun openPanel(panel: TvPlayerPanel) {
        pauseOverlay = false
        showControls = false
        activePanel = panel
        interact()
    }

    fun dismissTopLayer(): Boolean = when {
        runtime.ended -> {
            onReturnToDetails()
            true
        }
        runtime.errorMessage != null -> {
            onExitPlayer()
            true
        }
        activePanel != null -> {
            activePanel = null
            showControls = true
            interact()
            true
        }
        pauseOverlay -> {
            pauseOverlay = false
            showControls = true
            interact()
            true
        }
        showControls -> {
            showControls = false
            true
        }
        else -> false
    }

    BackHandler {
        if (!dismissTopLayer()) onExitPlayer()
    }

    DisposableEffect(lifecycleOwner, runtime) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) runtime.pause()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(runtime.isPlaying) {
        if (runtime.isPlaying) pauseOverlay = false
    }

    LaunchedEffect(
        showControls,
        interactionToken,
        activePanel,
        runtime.isPlaying,
        runtime.errorMessage,
        runtime.ended,
        runtime.playbackState,
    ) {
        if (!showControls || activePanel != null || runtime.errorMessage != null || runtime.ended || !runtime.readyOrEnded) {
            return@LaunchedEffect
        }
        delay(CONTROL_HIDE_MS)
        showControls = false
        if (!runtime.isPlaying && runtime.readyOrEnded && !runtime.ended) {
            pauseOverlay = true
        }
    }

    LaunchedEffect(seekOverlayToken) {
        if (seekOverlayToken <= 0) return@LaunchedEffect
        delay(1_250)
        showSeekOverlay = false
    }

    LaunchedEffect(showControls, activePanel, runtime.ended) {
        delay(120)
        when {
            runtime.ended -> Unit
            showControls && activePanel == null -> runCatching { playPauseFocusRequester.requestFocus() }
            !showControls && activePanel == null -> runCatching { containerFocusRequester.requestFocus() }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusRequester(containerFocusRequester)
            .focusable()
            .onPreviewKeyEvent { event ->
                if (event.nativeKeyEvent.action != AndroidKeyEvent.ACTION_DOWN) {
                    return@onPreviewKeyEvent false
                }
                if (activePanel != null || runtime.ended || runtime.errorMessage != null) {
                    return@onPreviewKeyEvent false
                }

                val keyCode = event.nativeKeyEvent.keyCode
                if (pauseOverlay) {
                    when (keyCode) {
                        AndroidKeyEvent.KEYCODE_DPAD_CENTER,
                        AndroidKeyEvent.KEYCODE_ENTER,
                        AndroidKeyEvent.KEYCODE_NUMPAD_ENTER,
                        AndroidKeyEvent.KEYCODE_MEDIA_PLAY,
                        AndroidKeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
                            runtime.play()
                            pauseOverlay = false
                            showControls = false
                        }
                        else -> revealControls()
                    }
                    return@onPreviewKeyEvent true
                }

                if (!showControls) {
                    when (keyCode) {
                        AndroidKeyEvent.KEYCODE_DPAD_CENTER,
                        AndroidKeyEvent.KEYCODE_ENTER,
                        AndroidKeyEvent.KEYCODE_NUMPAD_ENTER -> {
                            if (runtime.isPlaying) pauseIntoOverlay() else resumeWithoutControls()
                            true
                        }
                        AndroidKeyEvent.KEYCODE_DPAD_LEFT -> {
                            seekHidden(-seekStepForRepeat(event.nativeKeyEvent.repeatCount))
                            true
                        }
                        AndroidKeyEvent.KEYCODE_DPAD_RIGHT -> {
                            seekHidden(seekStepForRepeat(event.nativeKeyEvent.repeatCount))
                            true
                        }
                        AndroidKeyEvent.KEYCODE_DPAD_UP,
                        AndroidKeyEvent.KEYCODE_DPAD_DOWN -> {
                            revealControls()
                            true
                        }
                        AndroidKeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
                            if (runtime.isPlaying) pauseIntoOverlay() else resumeWithoutControls()
                            true
                        }
                        AndroidKeyEvent.KEYCODE_MEDIA_PLAY -> {
                            resumeWithoutControls()
                            true
                        }
                        AndroidKeyEvent.KEYCODE_MEDIA_PAUSE,
                        AndroidKeyEvent.KEYCODE_MEDIA_STOP -> {
                            pauseIntoOverlay()
                            true
                        }
                        AndroidKeyEvent.KEYCODE_MEDIA_FAST_FORWARD -> {
                            seekHidden(seekStepForRepeat(event.nativeKeyEvent.repeatCount))
                            true
                        }
                        AndroidKeyEvent.KEYCODE_MEDIA_REWIND -> {
                            seekHidden(-seekStepForRepeat(event.nativeKeyEvent.repeatCount))
                            true
                        }
                        AndroidKeyEvent.KEYCODE_CAPTIONS -> {
                            openPanel(TvPlayerPanel.SUBTITLES)
                            true
                        }
                        else -> false
                    }
                } else {
                    when (keyCode) {
                        AndroidKeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
                            runtime.togglePlayPause()
                            interact()
                            true
                        }
                        AndroidKeyEvent.KEYCODE_MEDIA_PLAY -> {
                            runtime.play()
                            interact()
                            true
                        }
                        AndroidKeyEvent.KEYCODE_MEDIA_PAUSE -> {
                            runtime.pause()
                            interact()
                            true
                        }
                        else -> false
                    }
                }
            },
    ) {
        AndroidView(
            factory = { context ->
                PlayerView(context).apply {
                    player = runtime.player
                    useController = false
                    resizeMode = aspectMode.resizeMode
                    keepScreenOn = true
                    setShutterBackgroundColor(android.graphics.Color.BLACK)
                }
            },
            update = { view ->
                if (view.player !== runtime.player) view.player = runtime.player
                if (view.resizeMode != aspectMode.resizeMode) view.resizeMode = aspectMode.resizeMode
                view.keepScreenOn = runtime.isPlaying || runtime.isBuffering
            },
            modifier = Modifier.fillMaxSize(),
        )

        val initialLoading = runtime.playbackState == Player.STATE_IDLE ||
            (runtime.playbackState == Player.STATE_BUFFERING && runtime.positionMs <= 0L)
        if (initialLoading && runtime.errorMessage == null) {
            TvPlayerLoadingOverlay(request = request)
        } else if (runtime.isBuffering && runtime.errorMessage == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(42.dp))
            }
        }

        TvPlayerControls(
            visible = showControls && activePanel == null && !pauseOverlay &&
                runtime.errorMessage == null && !runtime.ended && !initialLoading,
            request = request,
            runtime = runtime,
            playPauseFocusRequester = playPauseFocusRequester,
            progressFocusRequester = progressFocusRequester,
            hasEpisodes = episodeCatalogue.episodes.isNotEmpty() && request.episode != null,
            hasNextEpisode = nextEpisode != null,
            aspectMode = aspectMode,
            onInteraction = ::interact,
            onHideControls = {
                showControls = false
                if (!runtime.isPlaying && runtime.readyOrEnded && !runtime.ended) {
                    pauseOverlay = true
                }
            },
            onOpenEpisodes = { openPanel(TvPlayerPanel.EPISODES) },
            onOpenSources = { openPanel(TvPlayerPanel.SOURCES) },
            onOpenAudio = { openPanel(TvPlayerPanel.AUDIO) },
            onOpenSubtitles = { openPanel(TvPlayerPanel.SUBTITLES) },
            onOpenSpeed = { openPanel(TvPlayerPanel.SPEED) },
            onToggleAspect = {
                aspectMode = TvPlayerAspectMode.entries[
                    (aspectMode.ordinal + 1) % TvPlayerAspectMode.entries.size
                ]
            },
            onPlayNext = { nextEpisode?.let(onEpisodeSelected) },
        )

        AnimatedVisibility(
            visible = showSeekOverlay && !showControls && activePanel == null && !pauseOverlay,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            TvSeekOverlay(runtime)
        }

        if (pauseOverlay && runtime.errorMessage == null && !runtime.ended) {
            TvPauseOverlay(request = request)
        }

        when (activePanel) {
            TvPlayerPanel.EPISODES -> TvPlayerEpisodesPanel(
                catalogue = episodeCatalogue,
                currentEpisodeId = request.episode?.id,
                onEpisodeSelected = onEpisodeSelected,
                modifier = Modifier.fillMaxSize(),
            )
            TvPlayerPanel.SOURCES -> TvPlayerSourcesPanel(
                snapshot = sourceSnapshot,
                currentRequest = request,
                loading = sourcesLoading,
                error = sourcesError,
                onRefresh = onRefreshSources,
                onSourceSelected = { source ->
                    activePanel = null
                    showControls = false
                    onSwitchSource(source)
                },
                modifier = Modifier.fillMaxSize(),
            )
            TvPlayerPanel.AUDIO -> TvPlayerAudioPanel(
                tracks = runtime.audioTracks,
                onSelect = { track ->
                    runtime.selectAudio(track)
                    activePanel = null
                    showControls = true
                    interact()
                },
                modifier = Modifier.fillMaxSize(),
            )
            TvPlayerPanel.SUBTITLES -> TvPlayerSubtitlePanel(
                tracks = runtime.subtitleTracks,
                onDisable = {
                    runtime.selectSubtitle(null)
                    activePanel = null
                    showControls = true
                    interact()
                },
                onSelect = { track ->
                    runtime.selectSubtitle(track)
                    activePanel = null
                    showControls = true
                    interact()
                },
                modifier = Modifier.fillMaxSize(),
            )
            TvPlayerPanel.SPEED -> TvPlayerSpeedPanel(
                currentSpeed = runtime.playbackSpeed,
                onSelect = { speed ->
                    runtime.setSpeed(speed)
                    activePanel = null
                    showControls = true
                    interact()
                },
                modifier = Modifier.fillMaxSize(),
            )
            null -> Unit
        }

        runtime.errorMessage?.let { message ->
            TvPlayerErrorOverlay(message = message, onBack = onExitPlayer)
        }

        if (runtime.ended && runtime.errorMessage == null) {
            TvPostPlayOverlay(
                request = request,
                nextEpisode = nextEpisode,
                onNext = { nextEpisode?.let(onEpisodeSelected) },
                onBackToDetails = onReturnToDetails,
            )
        }
    }
}

@Composable
private fun TvPlayerLoadingOverlay(request: TvPlaybackRequest) {
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        AsyncImage(
            model = request.media.backgroundUrl.ifBlank { request.media.posterUrl },
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        listOf(Color.Black.copy(alpha = 0.92f), Color.Black.copy(alpha = 0.54f), Color.Black.copy(alpha = 0.76f)),
                    ),
                ),
        )
        Column(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .width(520.dp)
                .padding(start = 54.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            request.media.logoUrl.takeIf { it.isNotBlank() }?.let { logo ->
                AsyncImage(
                    model = logo,
                    contentDescription = request.media.title,
                    modifier = Modifier.width(260.dp).height(96.dp),
                )
            } ?: Text(
                text = request.media.title,
                color = Color.White,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
            )
            request.episode?.let { episode ->
                Text(
                    text = buildString {
                        val season = episode.season
                        val number = episode.episode
                        if (season != null && number != null) append("S${season}E${number} · ")
                        append(episode.title)
                    },
                    color = Color.White.copy(alpha = 0.78f),
                    fontSize = 15.sp,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(28.dp), strokeWidth = 3.dp)
                Text("Loading stream", color = Color.White.copy(alpha = 0.78f), fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun TvPauseOverlay(request: TvPlaybackRequest) {
    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.horizontalGradient(
                    listOf(Color.Black.copy(alpha = 0.94f), Color.Black.copy(alpha = 0.65f), Color.Transparent),
                ),
            ),
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .width(560.dp)
                .padding(start = 54.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            request.media.logoUrl.takeIf { it.isNotBlank() }?.let { logo ->
                AsyncImage(model = logo, contentDescription = request.media.title, modifier = Modifier.width(260.dp).height(92.dp))
            } ?: Text(
                request.media.title,
                color = Color.White,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
            )
            request.episode?.let { episode ->
                Text(
                    text = buildString {
                        val season = episode.season
                        val number = episode.episode
                        if (season != null && number != null) append("S${season}E${number} · ")
                        append(episode.title)
                    },
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            request.media.description.takeIf { it.isNotBlank() }?.let {
                Text(
                    text = it,
                    color = Color.White.copy(alpha = 0.70f),
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 20.sp,
                )
            }
            Text(
                text = "Press OK to resume",
                color = Color.White.copy(alpha = 0.52f),
                fontSize = 12.sp,
            )
        }
    }
}

@Composable
private fun TvSeekOverlay(runtime: TvPlayerRuntime) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.86f))),
            )
            .padding(horizontal = 44.dp, vertical = 28.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        val duration = runtime.durationMs.coerceAtLeast(1L)
        val fraction = (runtime.positionMs.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
        Box(
            Modifier
                .fillMaxWidth()
                .height(5.dp)
                .clip(RoundedCornerShape(99.dp))
                .background(Color.White.copy(alpha = 0.22f)),
        ) {
            Box(Modifier.fillMaxWidth(fraction).height(5.dp).background(Color.White))
        }
        Text(
            "${formatPlayerTime(runtime.positionMs)} / ${formatPlayerTime(runtime.durationMs)}",
            color = Color.White.copy(alpha = 0.88f),
            fontSize = 13.sp,
        )
    }
}

@Composable
fun TvPlayerFatalError(
    message: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TvPlayerErrorOverlay(message = message, onBack = onBack, modifier = modifier)
}

@Composable
private fun TvPlayerErrorOverlay(
    message: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val requester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        delay(120)
        requester.requestFocus()
    }
    Box(
        modifier = modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.94f)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("Playback error", color = Color.White, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
            Text(
                text = message,
                color = Color.White.copy(alpha = 0.70f),
                modifier = Modifier.width(640.dp),
                maxLines = 5,
                overflow = TextOverflow.Ellipsis,
            )
            Button(onClick = onBack, modifier = Modifier.focusRequester(requester)) {
                Icon(Icons.Default.ArrowBack, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Back to sources")
            }
        }
    }
}

@Composable
private fun TvPostPlayOverlay(
    request: TvPlaybackRequest,
    nextEpisode: TvCanonicalEpisode?,
    onNext: () -> Unit,
    onBackToDetails: () -> Unit,
) {
    val nextRequester = remember { FocusRequester() }
    val backRequester = remember { FocusRequester() }
    LaunchedEffect(nextEpisode?.identity?.stableKey) {
        delay(160)
        if (nextEpisode != null) nextRequester.requestFocus() else backRequester.requestFocus()
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.horizontalGradient(
                    listOf(Color.Black.copy(alpha = 0.88f), Color.Black.copy(alpha = 0.70f), Color.Black.copy(alpha = 0.82f)),
                ),
            ),
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .width(560.dp)
                .padding(end = 54.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = if (nextEpisode != null) "Up next" else "Playback finished",
                color = Color.White.copy(alpha = 0.72f),
                fontSize = 14.sp,
            )
            if (nextEpisode != null) {
                AsyncImage(
                    model = nextEpisode.thumbnailUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxWidth().height(220.dp).clip(RoundedCornerShape(18.dp)),
                )
                Text(
                    text = "${nextEpisode.coordinateLabel} · ${nextEpisode.title}",
                    color = Color.White,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                nextEpisode.overview.takeIf { it.isNotBlank() }?.let {
                    Text(it, color = Color.White.copy(alpha = 0.68f), maxLines = 3, overflow = TextOverflow.Ellipsis)
                }
            } else {
                Text(request.media.title, color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (nextEpisode != null) {
                    Button(
                        onClick = onNext,
                        modifier = Modifier.focusRequester(nextRequester),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Play next")
                    }
                }
                Button(
                    onClick = onBackToDetails,
                    modifier = Modifier.focusRequester(backRequester),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.14f), contentColor = Color.White),
                ) {
                    Text("Back to details")
                }
            }
        }
    }
}

private fun seekStepForRepeat(repeatCount: Int): Long = when {
    repeatCount >= 14 -> 60_000L
    repeatCount >= 8 -> 30_000L
    repeatCount >= 4 -> 20_000L
    else -> 10_000L
}

private const val CONTROL_HIDE_MS = 4_500L
