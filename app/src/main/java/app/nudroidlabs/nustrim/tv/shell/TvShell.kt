package app.nudroidlabs.nustrim.tv.shell

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.unit.dp
import app.nudroidlabs.nustrim.tv.details.TvDetailsEntry
import app.nudroidlabs.nustrim.tv.focus.TvFocusRegistry
import app.nudroidlabs.nustrim.tv.home.TvHomeEntry
import app.nudroidlabs.nustrim.tv.library.TvLibraryEntry
import app.nudroidlabs.nustrim.tv.navigation.TvBackAction
import app.nudroidlabs.nustrim.tv.navigation.TvNavigator
import app.nudroidlabs.nustrim.tv.navigation.TvRootDestination
import app.nudroidlabs.nustrim.tv.navigation.TvReturnFocus
import app.nudroidlabs.nustrim.tv.navigation.TvRoute
import app.nudroidlabs.nustrim.tv.navigation.resolveTvBackAction
import app.nudroidlabs.nustrim.tv.search.TvSearchEntry
import app.nudroidlabs.nustrim.tv.sources.TvSourcesEntry
import app.nudroidlabs.nustrim.tv.player.TvPlaybackRequest
import app.nudroidlabs.nustrim.tv.player.TvPlayerEntry
import app.nudroidlabs.nustrim.tv.settings.TvSettingsEntry
import app.nudroidlabs.nustrim.tv.theme.TvTokens

@Composable
fun TvShell(
    navigator: TvNavigator,
    focusRegistry: TvFocusRegistry,
    onExit: () -> Unit,
) {
    val focusManager = LocalFocusManager.current
    var sidebarOpen by remember { mutableStateOf(false) }
    var sidebarFocusRequestToken by remember { mutableIntStateOf(0) }
    var contentFocusRequestToken by remember { mutableIntStateOf(0) }

    val currentRoute = navigator.currentRoute
    val rootRoute = currentRoute as? TvRoute.Root
    val showSidebar = rootRoute != null

    fun openSidebar() {
        if (!showSidebar) return
        sidebarOpen = true
        sidebarFocusRequestToken += 1
    }

    fun closeSidebarAndRestoreContent() {
        if (!sidebarOpen) return
        sidebarOpen = false
        contentFocusRequestToken += 1
    }

    LaunchedEffect(currentRoute.stableKey) {
        sidebarOpen = false
        contentFocusRequestToken += 1
    }

    BackHandler {
        when (
            resolveTvBackAction(
                currentRoute = currentRoute,
                canPop = navigator.canPop,
                sidebarOpen = sidebarOpen,
            )
        ) {
            TvBackAction.POP_ROUTE -> {
                navigator.pop()
                contentFocusRequestToken += 1
            }

            TvBackAction.OPEN_SIDEBAR -> openSidebar()
            TvBackAction.EXIT_APP -> onExit()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(androidx.compose.material3.MaterialTheme.colorScheme.background),
    ) {
        AnimatedContent(
            targetState = currentRoute,
            transitionSpec = {
                fadeIn(tween(TvTokens.MediumMotionMillis)) togetherWith
                    fadeOut(tween(TvTokens.FastMotionMillis))
            },
            contentKey = { route -> route.stableKey },
            label = "tv-route-transition",
        ) { route ->
            TvRouteContent(
                route = route,
                focusRegistry = focusRegistry,
                focusRequestToken = contentFocusRequestToken,
                focusManager = focusManager,
                sidebarOpen = sidebarOpen,
                navigator = navigator,
                onOpenSidebar = ::openSidebar,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = if (route is TvRoute.Root) TvTokens.SidebarCollapsedWidth else 0.dp),
            )
        }

        if (showSidebar) {
            TvSidebar(
                expanded = sidebarOpen,
                selected = rootRoute.destination,
                focusRequestToken = sidebarFocusRequestToken,
                onSelect = { destination ->
                    val changed = destination != navigator.activeRoot
                    if (changed) {
                        navigator.navigateRoot(destination)
                    }
                    sidebarOpen = false
                    contentFocusRequestToken += 1
                },
                onCloseToContent = ::closeSidebarAndRestoreContent,
            )
        }
    }
}

@Composable
private fun TvRouteContent(
    route: TvRoute,
    focusRegistry: TvFocusRegistry,
    focusRequestToken: Int,
    focusManager: FocusManager,
    sidebarOpen: Boolean,
    navigator: TvNavigator,
    onOpenSidebar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val rootModifier = modifier.onKeyEvent { event ->
        if (
            route is TvRoute.Root &&
            !sidebarOpen &&
            event.type == KeyEventType.KeyDown &&
            event.key == Key.DirectionLeft
        ) {
            val moved = focusManager.moveFocus(FocusDirection.Left)
            if (!moved) onOpenSidebar()
            true
        } else {
            false
        }
    }

    when (route) {
        is TvRoute.Root -> when (route.destination) {
            TvRootDestination.HOME -> TvHomeEntry(
                scopeKey = route.focusScope,
                focusRegistry = focusRegistry,
                focusRequestToken = focusRequestToken,
                onOpen = { media, rowIndex, itemIndex ->
                    navigator.push(
                        TvRoute.Details(
                            contentKey = media.stableKey,
                            sourceUrl = media.sourceUrl,
                            media = media.item,
                            returnFocus = TvReturnFocus(
                                scopeKey = route.focusScope,
                                row = rowIndex,
                                column = itemIndex,
                            ),
                        ),
                    )
                },
                modifier = rootModifier,
            )

            TvRootDestination.SEARCH -> TvSearchEntry(
                scopeKey = route.focusScope,
                focusRegistry = focusRegistry,
                focusRequestToken = focusRequestToken,
                onOpen = { media, _, _ ->
                    navigator.push(
                        TvRoute.Details(
                            contentKey = media.stableKey,
                            sourceUrl = media.sourceUrl,
                            media = media.item,
                            returnFocus = TvReturnFocus(
                                scopeKey = route.focusScope,
                                anchorKey = focusRegistry.lastFocused(route.focusScope),
                            ),
                        ),
                    )
                },
                modifier = rootModifier,
            )

            TvRootDestination.LIBRARY -> TvLibraryEntry(
                scopeKey = route.focusScope,
                focusRegistry = focusRegistry,
                focusRequestToken = focusRequestToken,
                onOpen = { media ->
                    navigator.push(
                        TvRoute.Details(
                            contentKey = media.stableKey,
                            sourceUrl = media.entry.sourceUrl,
                            media = media.entry.toMediaItem(),
                            returnFocus = TvReturnFocus(
                                scopeKey = route.focusScope,
                                anchorKey = focusRegistry.lastFocused(route.focusScope),
                            ),
                        ),
                    )
                },
                modifier = rootModifier,
            )

            TvRootDestination.SETTINGS -> TvSettingsEntry(
                scopeKey = route.focusScope,
                focusRegistry = focusRegistry,
                focusRequestToken = focusRequestToken,
                modifier = rootModifier,
            )
        }

        is TvRoute.Details -> TvDetailsEntry(
            route = route,
            focusRegistry = focusRegistry,
            focusRequestToken = focusRequestToken,
            onPlayMovie = { snapshot ->
                navigator.push(
                    TvRoute.Sources(
                        mediaKey = route.contentKey,
                        sourceUrl = route.sourceUrl,
                        media = snapshot.item,
                        returnFocus = TvReturnFocus(
                            scopeKey = route.focusScope,
                            anchorKey = focusRegistry.lastFocused(route.focusScope),
                        ),
                    ),
                )
            },
            onPlayEpisode = { snapshot, episode ->
                navigator.push(
                    TvRoute.Sources(
                        mediaKey = route.contentKey,
                        sourceUrl = route.sourceUrl,
                        media = snapshot.item,
                        episode = episode.providerEpisode,
                        returnFocus = TvReturnFocus(
                            scopeKey = route.focusScope,
                            anchorKey = focusRegistry.lastFocused(route.focusScope),
                        ),
                    ),
                )
            },
            modifier = rootModifier,
        )

        is TvRoute.Sources -> TvSourcesEntry(
            route = route,
            focusRegistry = focusRegistry,
            focusRequestToken = focusRequestToken,
            onStreamSelected = { selected ->
                navigator.push(
                    TvRoute.Player(
                        request = TvPlaybackRequest(
                            mediaKey = route.mediaKey,
                            sourceUrl = route.sourceUrl,
                            media = route.media,
                            episode = route.episode,
                            stream = selected.stream,
                            streamSourceLabel = selected.sourceLabel,
                        ),
                        returnFocus = TvReturnFocus(
                            scopeKey = route.focusScope,
                            anchorKey = focusRegistry.lastFocused(route.focusScope),
                        ),
                    ),
                )
            },
            modifier = rootModifier,
        )

        is TvRoute.Player -> TvPlayerEntry(
            route = route,
            onExitPlayer = {
                navigator.pop()
            },
            onReturnToDetails = {
                navigator.pop()
                navigator.pop()
            },
            onOpenEpisode = { episode ->
                navigator.pop()
                navigator.replace(
                    TvRoute.Sources(
                        mediaKey = route.request.mediaKey,
                        sourceUrl = route.request.sourceUrl,
                        media = route.request.media,
                        episode = episode.providerEpisode,
                        returnFocus = TvReturnFocus(
                            scopeKey = "details/${route.request.mediaKey}",
                            anchorKey = "details:episode:${episode.identity.stableKey}",
                        ),
                    ),
                )
            },
            modifier = rootModifier,
        )
    }
}
