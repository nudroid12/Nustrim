package app.nudroidlabs.nustrim.tv.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import app.nudroidlabs.nustrim.core.library.LocalMediaStore
import app.nudroidlabs.nustrim.tv.episode.TvCanonicalEpisode
import app.nudroidlabs.nustrim.tv.episode.TvEpisodeCatalogueBuilder
import app.nudroidlabs.nustrim.tv.navigation.TvRoute
import app.nudroidlabs.nustrim.tv.sources.TvSourceStream
import app.nudroidlabs.nustrim.tv.sources.TvSourcesRepository
import app.nudroidlabs.nustrim.tv.sources.TvSourcesSnapshot
import app.nudroidlabs.nustrim.ui.UiPreferences
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlin.math.abs

@Composable
fun TvPlayerEntry(
    route: TvRoute.Player,
    onExitPlayer: () -> Unit,
    onReturnToDetails: () -> Unit,
    onOpenEpisode: (TvCanonicalEpisode) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val sourcesRepository = remember(context.applicationContext) {
        TvSourcesRepository(context.applicationContext)
    }
    val subtitleRepository = remember(context.applicationContext) {
        TvSubtitleRepository(context.applicationContext)
    }
    val mediaStore = remember(context.applicationContext) {
        LocalMediaStore(context.applicationContext)
    }
    val preferences = remember(context.applicationContext) {
        UiPreferences(context.applicationContext)
    }

    var activeRequest by remember(route.request.stableKey) {
        mutableStateOf(route.request)
    }
    var resumePositionMs by remember(route.request.stableKey) {
        mutableStateOf(
            mediaStore.resumePosition(
                sourceUrl = route.request.sourceUrl,
                item = route.request.media,
                episode = route.request.episode,
            ),
        )
    }
    var retryToken by remember(route.request.stableKey) { mutableIntStateOf(0) }
    var preparedRequest by remember(activeRequest.stableKey) {
        mutableStateOf<TvPlaybackRequest?>(null)
    }
    var sourceSnapshot by remember(activeRequest.mediaKey, activeRequest.episode?.id) {
        mutableStateOf<TvSourcesSnapshot?>(null)
    }
    var sourcesLoading by remember(activeRequest.mediaKey, activeRequest.episode?.id) {
        mutableStateOf(true)
    }
    var sourcesError by remember(activeRequest.mediaKey, activeRequest.episode?.id) {
        mutableStateOf<String?>(null)
    }
    var refreshToken by remember(activeRequest.mediaKey, activeRequest.episode?.id) {
        mutableIntStateOf(0)
    }

    val episodeCatalogue = remember(activeRequest.mediaKey, activeRequest.media.episodes) {
        TvEpisodeCatalogueBuilder.build(
            parentKey = activeRequest.mediaKey,
            providerEpisodes = activeRequest.media.episodes,
        )
    }
    val nextEpisode = remember(episodeCatalogue, activeRequest.episode?.id) {
        val currentId = activeRequest.episode?.id ?: return@remember null
        val currentIndex = episodeCatalogue.episodes.indexOfFirst {
            it.providerEpisodeId == currentId
        }
        currentIndex.takeIf { it >= 0 }
            ?.let { episodeCatalogue.episodes.getOrNull(it + 1)?.providerEpisode }
    }

    val sourceRoute = remember(
        activeRequest.mediaKey,
        activeRequest.sourceUrl,
        activeRequest.media,
        activeRequest.episode,
    ) {
        TvRoute.Sources(
            mediaKey = activeRequest.mediaKey,
            sourceUrl = activeRequest.sourceUrl,
            media = activeRequest.media,
            episode = activeRequest.episode,
        )
    }

    LaunchedEffect(sourceRoute.stableKey, refreshToken) {
        sourcesLoading = true
        sourcesError = null
        runCatching {
            sourcesRepository
                .loadProgressively(sourceRoute, forceRefresh = refreshToken > 0)
                .collect { snapshot ->
                    sourceSnapshot = snapshot
                    sourcesLoading = snapshot.loadingProviderCount > 0
                }
        }
            .onFailure { error ->
                sourcesError = error.message.orEmpty().ifBlank { error::class.java.simpleName }
            }
        sourcesLoading = false
    }

    LaunchedEffect(activeRequest.stableKey) {
        preparedRequest = runCatching {
            subtitleRepository.enrich(activeRequest)
        }.getOrElse {
            activeRequest
        }
    }

    val playbackRequest = preparedRequest
    if (playbackRequest == null) {
        TvSubtitleLoading(modifier = modifier)
    } else {
        val subtitleFingerprint = remember(playbackRequest.stream.subtitles) {
            playbackRequest.stream.subtitles.joinToString("|") { subtitle ->
                "${subtitle.url}#${subtitle.language}#${subtitle.label}"
            }.hashCode()
        }
        val runtimeKey = "${playbackRequest.stableKey}/subtitles/$subtitleFingerprint/retry/$retryToken"

        key(runtimeKey) {
            val runtimeResult = remember(runtimeKey, resumePositionMs) {
                runCatching {
                    TvPlayerRuntime(
                        context = context,
                        request = playbackRequest,
                        startPositionMs = resumePositionMs,
                        preferredSubtitleLanguage = preferences.subtitlePreferredLanguage,
                    )
                }
            }
            val runtime = runtimeResult.getOrNull()

            if (runtime == null) {
                TvPlayerFatalError(
                    message = runtimeResult.exceptionOrNull()?.message
                        .orEmpty()
                        .ifBlank { "Unable to create player" },
                    onRetry = { retryToken += 1 },
                    onBack = onExitPlayer,
                    modifier = modifier,
                )
            } else {
                DisposableEffect(runtime, playbackRequest.stableKey) {
                    onDispose {
                        if (!runtime.ended) {
                            mediaStore.recordProgress(
                                sourceUrl = playbackRequest.sourceUrl,
                                item = playbackRequest.media,
                                episode = playbackRequest.episode,
                                positionMs = runtime.positionMs,
                                durationMs = runtime.durationMs,
                            )
                        }
                        runtime.release()
                    }
                }

                LaunchedEffect(runtime, playbackRequest.stableKey) {
                    var lastSavedPositionMs = resumePositionMs
                    while (true) {
                        runtime.syncTimeline()
                        val position = runtime.positionMs
                        if (position > 0L && abs(position - lastSavedPositionMs) >= PROGRESS_SAVE_INTERVAL_MS) {
                            mediaStore.recordProgress(
                                sourceUrl = playbackRequest.sourceUrl,
                                item = playbackRequest.media,
                                episode = playbackRequest.episode,
                                positionMs = position,
                                durationMs = runtime.durationMs,
                            )
                            lastSavedPositionMs = position
                        }
                        delay(TIMELINE_SYNC_INTERVAL_MS)
                    }
                }

                LaunchedEffect(runtime.ended, playbackRequest.stableKey) {
                    if (runtime.ended) {
                        mediaStore.recordProgress(
                            sourceUrl = playbackRequest.sourceUrl,
                            item = playbackRequest.media,
                            episode = playbackRequest.episode,
                            positionMs = runtime.positionMs,
                            durationMs = runtime.durationMs,
                            completed = true,
                            nextEpisode = nextEpisode,
                        )
                    }
                }

                TvPlayerScreen(
                    request = playbackRequest,
                    runtime = runtime,
                    autoplayNextEpisode = preferences.autoplayNextEpisode,
                    seekStepMs = preferences.tvSeekStepSeconds * 1_000L,
                    controlsAutoHideMs = preferences.tvControlsAutoHideSeconds * 1_000L,
                    preferredSubtitleLanguage = preferences.subtitlePreferredLanguage,
                    secondPreferredSubtitleLanguage = preferences.subtitleSecondPreferredLanguage,
                    subtitleDisplayMode = preferences.subtitleDisplayMode,
                    episodeCatalogue = episodeCatalogue,
                    sourceSnapshot = sourceSnapshot,
                    sourcesLoading = sourcesLoading,
                    sourcesError = sourcesError,
                    onRefreshSources = {
                        refreshToken += 1
                    },
                    onSwitchSource = { selected ->
                        switchPlayerSource(
                            currentRequest = activeRequest,
                            currentPositionMs = runtime.positionMs,
                            selected = selected,
                            onResumePosition = { resumePositionMs = it },
                            onRequestChanged = { activeRequest = it },
                        )
                    },
                    onEpisodeSelected = onOpenEpisode,
                    onRetryPlayback = {
                        resumePositionMs = runtime.positionMs
                        retryToken += 1
                    },
                    onExitPlayer = onExitPlayer,
                    onReturnToDetails = onReturnToDetails,
                    modifier = modifier,
                )
            }
        }
    }
}

private const val TIMELINE_SYNC_INTERVAL_MS = 500L
private const val PROGRESS_SAVE_INTERVAL_MS = 10_000L

@Composable
private fun TvSubtitleLoading(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(color = Color.White)
    }
}

private fun switchPlayerSource(
    currentRequest: TvPlaybackRequest,
    currentPositionMs: Long,
    selected: TvSourceStream,
    onResumePosition: (Long) -> Unit,
    onRequestChanged: (TvPlaybackRequest) -> Unit,
) {
    val next = currentRequest.copy(
        stream = selected.stream,
        streamSourceLabel = selected.sourceLabel,
    )
    if (next.stableKey == currentRequest.stableKey) return
    onResumePosition(currentPositionMs)
    onRequestChanged(next)
}
