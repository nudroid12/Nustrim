package app.nudroidlabs.nustrim.tv.player

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import app.nudroidlabs.nustrim.tv.episode.TvCanonicalEpisode
import app.nudroidlabs.nustrim.tv.episode.TvEpisodeCatalogueBuilder
import app.nudroidlabs.nustrim.tv.navigation.TvRoute
import app.nudroidlabs.nustrim.tv.sources.TvSourceStream
import app.nudroidlabs.nustrim.tv.sources.TvSourcesRepository
import app.nudroidlabs.nustrim.tv.sources.TvSourcesSnapshot
import kotlinx.coroutines.delay

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
    val parentalGuideRepository = remember(context.applicationContext) {
        TvParentalGuideRepository(context.applicationContext)
    }

    var activeRequest by remember(route.request.stableKey) {
        mutableStateOf(route.request)
    }
    var resumePositionMs by remember(route.request.stableKey) {
        mutableStateOf(0L)
    }
    var preparedRequest by remember(activeRequest.stableKey) {
        mutableStateOf<TvPlaybackRequest?>(null)
    }
    var parentalGuide by remember(activeRequest.mediaKey) {
        mutableStateOf<TvParentalGuide?>(null)
    }
    var parentalGuideConsumed by remember(
        route.request.mediaKey,
        route.request.episode?.id,
    ) {
        mutableStateOf(false)
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

    LaunchedEffect(activeRequest.stableKey) {
        preparedRequest = runCatching {
            subtitleRepository.enrich(activeRequest)
        }.getOrElse {
            activeRequest
        }
    }

    LaunchedEffect(activeRequest.mediaKey) {
        parentalGuide = parentalGuideRepository.load(activeRequest.media)
    }

    LaunchedEffect(sourceRoute.stableKey, refreshToken) {
        sourcesLoading = true
        sourcesError = null
        runCatching {
            sourcesRepository.load(
                sourceRoute,
                forceRefresh = refreshToken > 0,
            )
        }.onSuccess { sourceSnapshot = it }
            .onFailure { error ->
                sourcesError = error.message.orEmpty().ifBlank {
                    error::class.java.simpleName
                }
            }
        sourcesLoading = false
    }

    val playbackRequest = preparedRequest
    if (playbackRequest == null) {
        TvPlayerLoadingOverlay(
            request = activeRequest,
            modifier = modifier,
        )
    } else {
        val subtitleFingerprint = remember(playbackRequest.stream.subtitles) {
            playbackRequest.stream.subtitles.joinToString("|") { subtitle ->
                "${subtitle.url}#${subtitle.language}#${subtitle.label}"
            }.hashCode()
        }
        val runtimeKey = "${playbackRequest.stableKey}/subs/$subtitleFingerprint"

        key(runtimeKey) {
            val runtimeResult = remember(runtimeKey, resumePositionMs) {
                runCatching {
                    TvPlayerRuntime(
                        context = context,
                        request = playbackRequest,
                        startPositionMs = resumePositionMs,
                    )
                }
            }
            val runtime = runtimeResult.getOrNull()

            if (runtime == null) {
                TvPlayerFatalError(
                    message = runtimeResult.exceptionOrNull()?.message
                        .orEmpty()
                        .ifBlank { "Unable to create player" },
                    onBack = onExitPlayer,
                    modifier = modifier,
                )
            } else {
                DisposableEffect(runtime) {
                    onDispose { runtime.release() }
                }

                LaunchedEffect(runtime) {
                    while (true) {
                        runtime.syncTimeline()
                        delay(250)
                    }
                }

                TvPlayerScreen(
                    request = playbackRequest,
                    runtime = runtime,
                    episodeCatalogue = episodeCatalogue,
                    sourceSnapshot = sourceSnapshot,
                    sourcesLoading = sourcesLoading,
                    sourcesError = sourcesError,
                    parentalGuide = parentalGuide,
                    showParentalGuideOnStart = !parentalGuideConsumed &&
                        resumePositionMs < PARENTAL_GUIDE_RESUME_LIMIT_MS,
                    onParentalGuideShown = {
                        parentalGuideConsumed = true
                    },
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
                    onExitPlayer = onExitPlayer,
                    onReturnToDetails = onReturnToDetails,
                    modifier = modifier,
                )
            }
        }
    }
}

private const val PARENTAL_GUIDE_RESUME_LIMIT_MS = 15_000L

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

