package app.nudroidlabs.nustrim.tv.player

import android.graphics.Typeface
import android.text.format.DateFormat
import android.util.TypedValue
import android.view.View
import android.view.KeyEvent as AndroidKeyEvent
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Refresh
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
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
import androidx.media3.ui.CaptionStyleCompat
import androidx.media3.ui.PlayerView
import app.nudroidlabs.nustrim.tv.episode.TvCanonicalEpisode
import app.nudroidlabs.nustrim.tv.episode.TvEpisodeCatalogue
import app.nudroidlabs.nustrim.tv.sources.TvSourceStream
import app.nudroidlabs.nustrim.tv.sources.TvSourcesSnapshot
import app.nudroidlabs.nustrim.ui.SubtitleDisplayMode
import coil3.compose.AsyncImage
import kotlinx.coroutines.delay
import java.util.Date

@OptIn(UnstableApi::class)
@Composable
fun TvPlayerScreen(
    request: TvPlaybackRequest,
    runtime: TvPlayerRuntime,
    autoplayNextEpisode: Boolean,
    seekStepMs: Long,
    controlsAutoHideMs: Long,
    preferredSubtitleLanguage: String,
    secondPreferredSubtitleLanguage: String,
    subtitleDisplayMode: SubtitleDisplayMode,
    episodeCatalogue: TvEpisodeCatalogue,
    sourceSnapshot: TvSourcesSnapshot?,
    sourcesLoading: Boolean,
    sourcesError: String?,
    onRefreshSources: () -> Unit,
    onSwitchSource: (TvSourceStream) -> Unit,
    onEpisodeSelected: (TvCanonicalEpisode) -> Unit,
    onRetryPlayback: () -> Unit,
    onExitPlayer: () -> Unit,
    onReturnToDetails: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val subtitleStyleStore = remember(context.applicationContext) {
        TvSubtitleStyleStore(context.applicationContext)
    }
    val containerFocusRequester = remember { FocusRequester() }
    val playPauseFocusRequester = remember { FocusRequester() }
    val progressFocusRequester = remember { FocusRequester() }

    var showControls by remember(request.stableKey) { mutableStateOf(true) }
    var showMoreActions by remember(request.stableKey) { mutableStateOf(false) }
    var activePanel by remember(request.stableKey) { mutableStateOf<TvPlayerPanel?>(null) }
    var pauseOverlay by remember(request.stableKey) { mutableStateOf(false) }
    var aspectMode by remember(request.mediaKey) { mutableStateOf(TvPlayerAspectMode.FIT) }
    var interactionToken by remember { mutableIntStateOf(0) }
    var seekOverlayToken by remember { mutableIntStateOf(0) }
    var showSeekOverlay by remember { mutableStateOf(false) }
    var subtitleFontSizeSp by remember(request.mediaKey) {
        mutableIntStateOf(subtitleStyleStore.fontSizeSp)
    }
    var subtitleBold by remember(request.mediaKey) {
        mutableStateOf(subtitleStyleStore.bold)
    }

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
        showMoreActions = false
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
        showMoreActions = false
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
        showMoreActions -> {
            showMoreActions = false
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
        delay(controlsAutoHideMs.coerceAtLeast(MIN_CONTROL_HIDE_MS))
        showMoreActions = false
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
        delay(250)
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
                            seekHidden(-seekStepForRepeat(event.nativeKeyEvent.repeatCount, seekStepMs))
                            true
                        }
                        AndroidKeyEvent.KEYCODE_DPAD_RIGHT -> {
                            seekHidden(seekStepForRepeat(event.nativeKeyEvent.repeatCount, seekStepMs))
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
                            seekHidden(seekStepForRepeat(event.nativeKeyEvent.repeatCount, seekStepMs))
                            true
                        }
                        AndroidKeyEvent.KEYCODE_MEDIA_REWIND -> {
                            seekHidden(-seekStepForRepeat(event.nativeKeyEvent.repeatCount, seekStepMs))
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
                view.subtitleView?.apply {
                    visibility = if (activePanel == TvPlayerPanel.SUBTITLES) View.INVISIBLE else View.VISIBLE
                    setApplyEmbeddedFontSizes(false)
                    setApplyEmbeddedStyles(false)
                    setFixedTextSize(TypedValue.COMPLEX_UNIT_SP, subtitleFontSizeSp.toFloat())
                    setStyle(
                        CaptionStyleCompat(
                            android.graphics.Color.WHITE,
                            android.graphics.Color.TRANSPARENT,
                            android.graphics.Color.TRANSPARENT,
                            CaptionStyleCompat.EDGE_TYPE_OUTLINE,
                            android.graphics.Color.BLACK,
                            if (subtitleBold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT,
                        ),
                    )
                }
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
            seekStepMs = seekStepMs,
            hasEpisodes = episodeCatalogue.episodes.isNotEmpty() && request.episode != null,
            hasNextEpisode = nextEpisode != null,
            aspectMode = aspectMode,
            showMoreActions = showMoreActions,
            onInteraction = ::interact,
            onHideControls = {
                showMoreActions = false
                showControls = false
                if (!runtime.isPlaying && runtime.readyOrEnded && !runtime.ended) {
                    pauseOverlay = true
                }
            },
            onToggleMoreActions = {
                showMoreActions = !showMoreActions
                interact()
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
                    showMoreActions = false
                    showControls = true
                    interact()
                },
                modifier = Modifier.fillMaxSize(),
            )
            TvPlayerPanel.SUBTITLES -> TvPlayerSubtitlePanel(
                tracks = runtime.subtitleTracks,
                preferredLanguage = preferredSubtitleLanguage,
                secondPreferredLanguage = secondPreferredSubtitleLanguage,
                displayMode = subtitleDisplayMode,
                fontSizeSp = subtitleFontSizeSp,
                bold = subtitleBold,
                onFontSizeChange = { next ->
                    subtitleFontSizeSp = next
                    subtitleStyleStore.fontSizeSp = next
                },
                onBoldChange = { next ->
                    subtitleBold = next
                    subtitleStyleStore.bold = next
                },
                onDisable = {
                    runtime.selectSubtitle(null)
                    interact()
                },
                onSelect = { track ->
                    runtime.selectSubtitle(track)
                    interact()
                },
                modifier = Modifier.fillMaxSize(),
            )
            TvPlayerPanel.SPEED -> TvPlayerSpeedPanel(
                currentSpeed = runtime.playbackSpeed,
                onSelect = { speed ->
                    runtime.setSpeed(speed)
                    activePanel = null
                    showMoreActions = false
                    showControls = true
                    interact()
                },
                modifier = Modifier.fillMaxSize(),
            )
            null -> Unit
        }

        runtime.errorMessage?.let { message ->
            TvPlayerErrorOverlay(
                message = message,
                onRetry = onRetryPlayback,
                onBack = onExitPlayer,
            )
        }

        if (runtime.ended && runtime.errorMessage == null) {
            TvPostPlayOverlay(
                request = request,
                nextEpisode = nextEpisode,
                autoplayEnabled = autoplayNextEpisode,
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
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0f to Color.Black.copy(alpha = 0.32f),
                            0.36f to Color.Black.copy(alpha = 0.58f),
                            0.72f to Color.Black.copy(alpha = 0.80f),
                            1f to Color.Black.copy(alpha = 0.92f),
                        ),
                    ),
                ),
        )
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            request.media.logoUrl.takeIf { it.isNotBlank() }?.let { logo ->
                AsyncImage(
                    model = logo,
                    contentDescription = request.media.title,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.width(320.dp).height(160.dp),
                )
            } ?: Text(
                text = request.media.title,
                color = Color.White,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
            )
            CircularProgressIndicator(
                color = Color.White,
                modifier = Modifier.size(34.dp),
                strokeWidth = 3.dp,
            )
            Text(
                text = "Loading stream",
                color = Color.White.copy(alpha = 0.74f),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun TvPauseOverlay(request: TvPlaybackRequest) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.74f)),
    ) {
        TvPauseClock(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 40.dp, end = 54.dp),
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth(0.72f)
                .padding(start = 64.dp, end = 42.dp, bottom = 120.dp),
        ) {
            Text(
                text = "You are watching",
                color = Color.White.copy(alpha = 0.58f),
                style = MaterialTheme.typography.bodyLarge,
            )
            Spacer(Modifier.height(14.dp))
            request.media.logoUrl.takeIf { it.isNotBlank() }?.let { logo ->
                AsyncImage(
                    model = logo,
                    contentDescription = request.media.title,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.width(320.dp).height(96.dp),
                )
            } ?: Text(
                text = request.media.title,
                color = Color.White,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            val release = Regex("""\b(?:19|20)\d{2}\b""")
                .find(request.media.releaseInfo)
                ?.value
                .orEmpty()
            val episode = request.episode
            val coordinate = episode?.let { item ->
                val season = item.season
                val number = item.episode
                if (season != null && number != null) "S${season}E${number}" else ""
            }.orEmpty()
            if (release.isNotBlank() || coordinate.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = listOf(release, coordinate).filter { it.isNotBlank() }.joinToString(" · "),
                    color = Color.White.copy(alpha = 0.76f),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
            episode?.title?.takeIf { it.isNotBlank() }?.let { title ->
                Spacer(Modifier.height(14.dp))
                Text(
                    text = title,
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            val pauseDescription = episode?.overview
                ?.takeIf { it.isNotBlank() }
                ?: request.media.description
            pauseDescription.takeIf { it.isNotBlank() }?.let { description ->
                Spacer(Modifier.height(18.dp))
                Text(
                    text = description,
                    color = Color.White.copy(alpha = 0.72f),
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 22.sp,
                )
            }
        }
    }
}

@Composable
private fun TvPauseClock(modifier: Modifier = Modifier) {
    var nowMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    val context = LocalContext.current
    val formatter = remember(context) { DateFormat.getTimeFormat(context) }
    LaunchedEffect(Unit) {
        while (true) {
            val current = System.currentTimeMillis()
            nowMillis = current
            delay((60_000L - (current % 60_000L)).coerceAtLeast(1_000L))
        }
    }
    Text(
        text = formatter.format(Date(nowMillis)),
        color = Color.White.copy(alpha = 0.94f),
        style = MaterialTheme.typography.headlineSmall,
        fontSize = 34.sp,
        fontWeight = FontWeight.Normal,
        modifier = modifier,
    )
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
    onRetry: (() -> Unit)? = null,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TvPlayerErrorOverlay(message = message, onRetry = onRetry, onBack = onBack, modifier = modifier)
}

@Composable
private fun TvPlayerErrorOverlay(
    message: String,
    onRetry: (() -> Unit)? = null,
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
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (onRetry != null) {
                    Button(onClick = onRetry, modifier = Modifier.focusRequester(requester)) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Try again")
                    }
                }
                Button(
                    onClick = onBack,
                    modifier = if (onRetry == null) Modifier.focusRequester(requester) else Modifier,
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Back to sources")
                }
            }
        }
    }
}

@Composable
private fun TvPostPlayOverlay(
    request: TvPlaybackRequest,
    nextEpisode: TvCanonicalEpisode?,
    autoplayEnabled: Boolean,
    onNext: () -> Unit,
    onBackToDetails: () -> Unit,
) {
    val nextRequester = remember { FocusRequester() }
    val backRequester = remember { FocusRequester() }
    var autoplayCancelled by remember(nextEpisode?.identity?.stableKey, autoplayEnabled) {
        mutableStateOf(false)
    }
    var countdownSeconds by remember(nextEpisode?.identity?.stableKey, autoplayEnabled) {
        mutableIntStateOf(if (autoplayEnabled && nextEpisode != null) POST_PLAY_COUNTDOWN_SECONDS else 0)
    }

    LaunchedEffect(nextEpisode?.identity?.stableKey, autoplayEnabled, autoplayCancelled) {
        if (!autoplayEnabled || nextEpisode == null || autoplayCancelled) return@LaunchedEffect
        for (remaining in POST_PLAY_COUNTDOWN_SECONDS downTo 1) {
            countdownSeconds = remaining
            delay(1_000L)
        }
        if (!autoplayCancelled) onNext()
    }
    LaunchedEffect(nextEpisode?.identity?.stableKey) {
        delay(180)
        if (nextEpisode != null) nextRequester.requestFocus() else backRequester.requestFocus()
    }

    Box(Modifier.fillMaxSize()) {
        if (nextEpisode != null) {
            var focused by remember { mutableStateOf(false) }
            Row(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 26.dp, bottom = 30.dp)
                    .width(420.dp)
                    .focusRequester(nextRequester)
                    .onFocusChanged { focused = it.isFocused }
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xE3191919))
                    .border(
                        if (focused) 2.dp else 1.dp,
                        if (focused) Color.White else Color.White.copy(alpha = 0.16f),
                        RoundedCornerShape(14.dp),
                    )
                    .clickable(onClick = onNext)
                    .focusable()
                    .padding(horizontal = 10.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AsyncImage(
                    model = nextEpisode.thumbnailUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .width(118.dp)
                        .height(70.dp)
                        .clip(RoundedCornerShape(10.dp)),
                )
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
                    Text(
                        text = "Next episode",
                        color = Color.White.copy(alpha = 0.78f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = "${nextEpisode.coordinateLabel} · ${nextEpisode.title}",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = if (autoplayEnabled && !autoplayCancelled) {
                            "Playing in ${countdownSeconds.coerceAtLeast(1)}s"
                        } else {
                            "Press OK to play"
                        },
                        color = Color.White.copy(alpha = 0.58f),
                        fontSize = 11.sp,
                    )
                }
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp),
                )
            }
            var backFocused by remember { mutableStateOf(false) }
            Row(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 26.dp, bottom = 132.dp)
                    .focusRequester(backRequester)
                    .onFocusChanged {
                        backFocused = it.isFocused
                        if (it.isFocused) autoplayCancelled = true
                    }
                    .clip(RoundedCornerShape(22.dp))
                    .background(if (backFocused) Color.White else Color(0xE3191919))
                    .border(
                        if (backFocused) 2.dp else 1.dp,
                        if (backFocused) Color.White else Color.White.copy(alpha = 0.16f),
                        RoundedCornerShape(22.dp),
                    )
                    .clickable(onClick = onBackToDetails)
                    .focusable()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = null,
                    tint = if (backFocused) Color.Black else Color.White,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    text = "Back to details",
                    color = if (backFocused) Color.Black else Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        } else {
            var focused by remember { mutableStateOf(false) }
            Column(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 26.dp, bottom = 30.dp)
                    .width(360.dp)
                    .focusRequester(backRequester)
                    .onFocusChanged { focused = it.isFocused }
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xE3191919))
                    .border(
                        if (focused) 2.dp else 1.dp,
                        if (focused) Color.White else Color.White.copy(alpha = 0.16f),
                        RoundedCornerShape(14.dp),
                    )
                    .clickable(onClick = onBackToDetails)
                    .focusable()
                    .padding(16.dp),
            ) {
                Text("Playback finished", color = Color.White.copy(alpha = 0.76f), fontSize = 12.sp)
                Spacer(Modifier.height(5.dp))
                Text(
                    request.media.title,
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(5.dp))
                Text("Press OK to return to details", color = Color.White.copy(alpha = 0.58f), fontSize = 11.sp)
            }
        }
    }
}

private fun seekStepForRepeat(repeatCount: Int, baseStepMs: Long): Long = when {
    repeatCount >= 14 -> baseStepMs * 6
    repeatCount >= 8 -> baseStepMs * 3
    repeatCount >= 4 -> baseStepMs * 2
    else -> baseStepMs
}

private const val MIN_CONTROL_HIDE_MS = 1_000L
private const val POST_PLAY_COUNTDOWN_SECONDS = 5
