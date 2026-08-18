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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.nudroidlabs.nustrim.tv.navigation.TvDestination
import app.nudroidlabs.nustrim.tv.theme.TvColors

@Composable
fun TvShell(
    onExit: () -> Unit
) {
    var selected by remember { mutableStateOf(TvDestination.HOME) }
    var sidebarExpanded by remember { mutableStateOf(true) }

    val sidebarRequesters = remember {
        TvDestination.entries.associateWith { FocusRequester() }
    }
    val contentRequester = remember { FocusRequester() }

    fun focusSidebar() {
        sidebarExpanded = true
        sidebarRequesters.getValue(selected).requestFocus()
    }

    fun focusContent() {
        sidebarExpanded = false
        contentRequester.requestFocus()
    }

    LaunchedEffect(Unit) {
        sidebarRequesters.getValue(selected).requestFocus()
    }

    BackHandler {
        if (sidebarExpanded) onExit() else focusSidebar()
    }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(TvColors.Background)
    ) {
        TvSidebar(
            selected = selected,
            expanded = sidebarExpanded,
            requesters = sidebarRequesters,
            onSelected = { destination ->
                selected = destination
                sidebarExpanded = true
            },
            onMoveRight = { focusContent() }
        )

        TvStagePlaceholder(
            destination = selected,
            focusRequester = contentRequester,
            onMoveLeft = { focusSidebar() },
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth()
        )
    }
}

@Composable
private fun TvSidebar(
    selected: TvDestination,
    expanded: Boolean,
    requesters: Map<TvDestination, FocusRequester>,
    onSelected: (TvDestination) -> Unit,
    onMoveRight: () -> Unit
) {
    val width by animateDpAsState(
        targetValue = if (expanded) 236.dp else 78.dp,
        label = "tvSidebarWidth"
    )

    Column(
        modifier = Modifier
            .width(width)
            .fillMaxHeight()
            .background(TvColors.BackgroundElevated)
            .padding(horizontal = 12.dp, vertical = 28.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (expanded) {
            Text(
                text = "NUSTRIM",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = TvColors.TextPrimary,
                modifier = Modifier.padding(horizontal = 12.dp)
            )
            Spacer(Modifier.height(18.dp))
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
        targetValue = if (focused || selected) {
            TvColors.FocusBackground
        } else {
            androidx.compose.ui.graphics.Color.Transparent
        },
        label = "tvSidebarItemBackground"
    )

    val ring by animateColorAsState(
        targetValue = if (focused) {
            TvColors.FocusRing
        } else {
            androidx.compose.ui.graphics.Color.Transparent
        },
        label = "tvSidebarItemRing"
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
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
        border = BorderStroke(1.dp, ring)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .background(
                        TvColors.SurfaceVariant,
                        RoundedCornerShape(9.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = destination.icon,
                    contentDescription = destination.label,
                    tint = TvColors.TextPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }

            if (expanded) {
                Text(
                    text = destination.label,
                    color = if (focused || selected) {
                        TvColors.TextPrimary
                    } else {
                        TvColors.TextSecondary
                    },
                    fontWeight = if (selected) {
                        FontWeight.SemiBold
                    } else {
                        FontWeight.Normal
                    },
                    fontSize = 16.sp
                )
            }
        }
    }
}

@Composable
private fun TvStagePlaceholder(
    destination: TvDestination,
    focusRequester: FocusRequester,
    onMoveLeft: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.padding(horizontal = 42.dp, vertical = 34.dp),
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
                    text = "TV foundation ready. Content will be connected in the next stage.",
                    color = TvColors.TextSecondary,
                    fontSize = 15.sp
                )
            }
        }
    }
}
