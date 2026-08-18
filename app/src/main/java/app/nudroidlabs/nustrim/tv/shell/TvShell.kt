package app.nudroidlabs.nustrim.tv.shell

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import app.nudroidlabs.nustrim.tv.home.TvHomeEntry
import app.nudroidlabs.nustrim.tv.home.TvHomeScreen
import app.nudroidlabs.nustrim.tv.navigation.TvDestination
import app.nudroidlabs.nustrim.tv.theme.TvColors

@Composable
fun TvShell(
    onExit: () -> Unit
) {
    var selected by remember { mutableStateOf(TvDestination.HOME) }
    var sidebarExpanded by remember { mutableStateOf(true) }
    var contentFocusRequestToken by remember { mutableIntStateOf(0) }
    var openedHomeEntry by remember { mutableStateOf<TvHomeEntry?>(null) }

    val sidebarRequesters = remember {
        TvDestination.entries.associateWith { FocusRequester() }
    }
    val firstContentRequester = remember { FocusRequester() }

    fun focusSidebar() {
        sidebarExpanded = true
        openedHomeEntry = null
        runCatching {
            sidebarRequesters.getValue(selected).requestFocus()
        }
    }

    fun focusContent() {
        sidebarExpanded = false
        contentFocusRequestToken += 1
    }

    LaunchedEffect(Unit) {
        sidebarRequesters.getValue(selected).requestFocus()
    }

    BackHandler {
        when {
            openedHomeEntry != null -> {
                openedHomeEntry = null
                contentFocusRequestToken += 1
            }
            sidebarExpanded -> onExit()
            else -> focusSidebar()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(TvColors.Background)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 72.dp)
        ) {
            if (openedHomeEntry != null) {
                TvStageDetailsPreview(
                    entry = openedHomeEntry!!,
                    onMoveLeft = { focusSidebar() },
                    focusRequester = firstContentRequester
                )
            } else {
                when (selected) {
                    TvDestination.HOME -> TvHomeScreen(
                        contentFocusRequestToken = contentFocusRequestToken,
                        firstContentRequester = firstContentRequester,
                        onContentFocused = {
                            sidebarExpanded = false
                        },
                        onMoveLeft = { focusSidebar() },
                        onOpen = { openedHomeEntry = it }
                    )
                    else -> TvStagePlaceholder(
                        destination = selected,
                        focusRequester = firstContentRequester,
                        onMoveLeft = { focusSidebar() }
                    )
                }
            }
        }

        TvSidebar(
            selected = selected,
            expanded = sidebarExpanded,
            requesters = sidebarRequesters,
            onSelected = { destination ->
                selected = destination
                openedHomeEntry = null
                sidebarExpanded = true
            },
            onMoveRight = { focusContent() },
            modifier = Modifier
                .align(Alignment.CenterStart)
                .zIndex(10f)
        )
    }
}

@Composable
private fun TvSidebar(
    selected: TvDestination,
    expanded: Boolean,
    requesters: Map<TvDestination, FocusRequester>,
    onSelected: (TvDestination) -> Unit,
    onMoveRight: () -> Unit,
    modifier: Modifier = Modifier
) {
    val width by animateDpAsState(
        targetValue = if (expanded) 224.dp else 72.dp,
        label = "tvSidebarWidth"
    )

    Column(
        modifier = modifier
            .width(width)
            .fillMaxHeight()
            .background(
                if (expanded) {
                    TvColors.BackgroundElevated.copy(alpha = 0.98f)
                } else {
                    TvColors.BackgroundElevated.copy(alpha = 0.72f)
                }
            )
            .padding(
                horizontal = if (expanded) 12.dp else 9.dp,
                vertical = 28.dp
            ),
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        if (expanded) {
            Text(
                text = "NUSTRIM",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = TvColors.TextPrimary,
                modifier = Modifier.padding(
                    horizontal = 12.dp,
                    vertical = 5.dp
                )
            )
            Spacer(Modifier.height(14.dp))
        } else {
            Spacer(Modifier.height(46.dp))
        }

        TvDestination.entries.forEach { destination ->
            TvSidebarItem(
                destination = destination,
                selected = destination == selected,
                expanded = expanded,
                focusRequester = requesters.getValue(destination),
                onSelected = { onSelected(destination) },
                onMoveRight = onMoveRight
            )
        }
    }
}

@Composable
private fun TvSidebarItem(
    destination: TvDestination,
    selected: Boolean,
    expanded: Boolean,
    focusRequester: FocusRequester,
    onSelected: () -> Unit,
    onMoveRight: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }

    val background by animateColorAsState(
        targetValue = when {
            focused -> TvColors.FocusRing
            selected -> TvColors.FocusBackground
            else -> Color.Transparent
        },
        label = "tvSidebarItemBackground"
    )

    val foreground = when {
        focused -> TvColors.Background
        selected -> TvColors.TextPrimary
        else -> TvColors.TextSecondary
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .focusRequester(focusRequester)
            .onFocusChanged { state -> focused = state.hasFocus }
            .onPreviewKeyEvent { event ->
                if (
                    event.type == KeyEventType.KeyDown &&
                    event.key == Key.DirectionRight
                ) {
                    onMoveRight()
                    true
                } else {
                    false
                }
            }
            .clickable(onClick = onSelected)
            .focusable(),
        shape = RoundedCornerShape(12.dp),
        color = background,
        border = if (selected && !focused) {
            BorderStroke(
                1.dp,
                Color.White.copy(alpha = 0.10f)
            )
        } else {
            null
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(13.dp)
        ) {
            Icon(
                imageVector = destination.icon,
                contentDescription = destination.label,
                tint = foreground,
                modifier = Modifier.size(22.dp)
            )

            if (expanded) {
                Text(
                    text = destination.label,
                    color = foreground,
                    fontWeight = if (focused || selected) {
                        FontWeight.SemiBold
                    } else {
                        FontWeight.Normal
                    },
                    fontSize = 15.sp
                )
            }
        }
    }
}

@Composable
private fun TvStagePlaceholder(
    destination: TvDestination,
    focusRequester: FocusRequester,
    onMoveLeft: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 42.dp, vertical = 34.dp),
        contentAlignment = Alignment.TopStart
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester)
                .onPreviewKeyEvent { event ->
                    if (
                        event.type == KeyEventType.KeyDown &&
                        event.key == Key.DirectionLeft
                    ) {
                        onMoveLeft()
                        true
                    } else {
                        false
                    }
                }
                .focusable(),
            shape = RoundedCornerShape(18.dp),
            color = TvColors.Surface.copy(alpha = 0.86f),
            border = BorderStroke(
                1.dp,
                TvColors.FocusRing.copy(alpha = 0.18f)
            )
        ) {
            Column(
                modifier = Modifier.padding(
                    horizontal = 34.dp,
                    vertical = 30.dp
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = destination.label,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TvColors.TextPrimary
                )
                Text(
                    text = "This workspace lands in a later TV stage.",
                    color = TvColors.TextSecondary,
                    fontSize = 15.sp
                )
            }
        }
    }
}

@Composable
private fun TvStageDetailsPreview(
    entry: TvHomeEntry,
    onMoveLeft: () -> Unit,
    focusRequester: FocusRequester
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(TvColors.Background),
        contentAlignment = Alignment.CenterStart
    ) {
        Surface(
            modifier = Modifier
                .width(540.dp)
                .padding(start = 48.dp)
                .focusRequester(focusRequester)
                .onPreviewKeyEvent { event ->
                    if (
                        event.type == KeyEventType.KeyDown &&
                        event.key == Key.DirectionLeft
                    ) {
                        onMoveLeft()
                        true
                    } else {
                        false
                    }
                }
                .focusable(),
            shape = RoundedCornerShape(20.dp),
            color = TvColors.BackgroundElevated,
            border = BorderStroke(
                1.dp,
                Color.White.copy(alpha = 0.12f)
            )
        ) {
            Column(
                modifier = Modifier.padding(30.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = entry.item.title,
                    color = TvColors.TextPrimary,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = entry.item.description.ifBlank {
                        "Details page will be connected in TV Stage 3."
                    },
                    color = TvColors.TextSecondary,
                    fontSize = 15.sp,
                    maxLines = 5,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                Text(
                    text = "Stage 3 will replace this preview with the full details experience.",
                    color = TvColors.Accent,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }

    LaunchedEffect(entry.stableKey) {
        runCatching { focusRequester.requestFocus() }
    }
}
