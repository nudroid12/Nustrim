package app.nudroidlabs.nustrim.tv.shell

import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.nudroidlabs.nustrim.tv.navigation.TvRootDestination
import app.nudroidlabs.nustrim.tv.theme.TvColors
import app.nudroidlabs.nustrim.tv.theme.TvTokens
import app.nudroidlabs.nustrim.tv.theme.animateTvFocusScale

private data class SidebarItem(
    val destination: TvRootDestination,
    val label: String,
    val icon: ImageVector,
)

private val SidebarItems = listOf(
    SidebarItem(TvRootDestination.HOME, "Home", Icons.Outlined.Home),
    SidebarItem(TvRootDestination.SEARCH, "Search", Icons.Outlined.Search),
    SidebarItem(TvRootDestination.LIBRARY, "Library", Icons.Outlined.VideoLibrary),
    SidebarItem(TvRootDestination.SETTINGS, "Settings", Icons.Outlined.Settings),
)

@Composable
fun TvSidebar(
    expanded: Boolean,
    selected: TvRootDestination,
    focusRequestToken: Int,
    onSelect: (TvRootDestination) -> Unit,
    onCloseToContent: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sidebarTransition = updateTransition(
        targetState = expanded,
        label = "tv-sidebar-transition",
    )
    val width by sidebarTransition.animateDp(
        transitionSpec = { tween(TvTokens.MediumMotionMillis) },
        label = "tv-sidebar-width",
    ) { isExpanded ->
        if (isExpanded) TvTokens.SidebarExpandedWidth else TvTokens.SidebarCollapsedWidth
    }
    val itemWidth by sidebarTransition.animateDp(
        transitionSpec = { tween(TvTokens.MediumMotionMillis) },
        label = "tv-sidebar-item-width",
    ) { isExpanded ->
        if (isExpanded) 224.dp else 52.dp
    }
    val labelAlpha by sidebarTransition.animateFloat(
        transitionSpec = {
            if (targetState) {
                tween(
                    durationMillis = TvTokens.FastMotionMillis,
                    delayMillis = TvTokens.FastMotionMillis / 2,
                )
            } else {
                tween(TvTokens.FastMotionMillis)
            }
        },
        label = "tv-sidebar-label-alpha",
    ) { isExpanded ->
        if (isExpanded) 1f else 0f
    }
    val labelOffset by sidebarTransition.animateDp(
        transitionSpec = { tween(TvTokens.MediumMotionMillis) },
        label = "tv-sidebar-label-offset",
    ) { isExpanded ->
        if (isExpanded) 0.dp else (-8).dp
    }
    val requesters = remember { TvRootDestination.entries.associateWith { FocusRequester() } }

    LaunchedEffect(expanded, selected, focusRequestToken) {
        if (!expanded) return@LaunchedEffect
        repeat(TvTokens.FocusRestoreAttempts) {
            withFrameNanos { }
            val restored = runCatching { requesters[selected]?.requestFocus() == true }.getOrDefault(false)
            if (restored) return@LaunchedEffect
        }
    }

    Box(
        modifier = modifier
            .width(width)
            .fillMaxHeight()
            .clipToBounds()
            .background(
                if (expanded) TvColors.SidebarExpanded else TvColors.SidebarCollapsed,
            )
            .onPreviewKeyEvent { event ->
                if (
                    expanded &&
                    event.type == KeyEventType.KeyDown &&
                    event.key == Key.DirectionRight
                ) {
                    onCloseToContent()
                    true
                } else {
                    false
                }
            },
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(horizontal = 10.dp, vertical = 24.dp)
                .focusGroup(),
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                modifier = Modifier.height(42.dp),
                contentAlignment = Alignment.TopStart,
            ) {
                Text(
                    text = "NUSTRIM",
                    modifier = Modifier
                        .padding(start = 14.dp)
                        .alpha(labelAlpha)
                        .offset(x = labelOffset),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Clip,
                )
            }

            SidebarItems.forEach { item ->
                val requester = requesters.getValue(item.destination)
                SidebarNavigationItem(
                    item = item,
                    expanded = expanded,
                    itemWidth = itemWidth,
                    labelAlpha = labelAlpha,
                    labelOffset = labelOffset,
                    selected = item.destination == selected,
                    requester = requester,
                    onSelect = { onSelect(item.destination) },
                )
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun SidebarNavigationItem(
    item: SidebarItem,
    expanded: Boolean,
    itemWidth: Dp,
    labelAlpha: Float,
    labelOffset: Dp,
    selected: Boolean,
    requester: FocusRequester,
    onSelect: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val scale = animateTvFocusScale(
        focused = focused,
        label = "sidebar-item-scale",
    )
    val synchronizedScale = 1f + ((scale - 1f) * labelAlpha)

    Row(
        modifier = Modifier
            .width(itemWidth)
            .height(TvTokens.SidebarItemHeight)
            .clipToBounds()
            .scale(synchronizedScale)
            .background(
                color = when {
                    focused -> TvColors.FocusSurface
                    selected && expanded -> TvColors.SurfaceSelected
                    else -> androidx.compose.ui.graphics.Color.Transparent
                },
                shape = RoundedCornerShape(10.dp),
            )
            .focusRequester(requester)
            .focusProperties { canFocus = expanded }
            .onFocusChanged { focused = it.isFocused }
            .onKeyEvent { event ->
                if (
                    event.type == KeyEventType.KeyDown &&
                    (event.key == Key.DirectionCenter || event.key == Key.Enter)
                ) {
                    onSelect()
                    true
                } else {
                    false
                }
            }
            .focusable(enabled = expanded)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = item.icon,
            contentDescription = item.label,
            modifier = Modifier.size(24.dp),
            tint = if (focused) TvColors.TextInverse else MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.width(16.dp))
        Text(
            text = item.label,
            modifier = Modifier
                .alpha(labelAlpha)
                .offset(x = labelOffset),
            color = if (focused) TvColors.TextInverse else MaterialTheme.colorScheme.onSurface,
            fontWeight = if (selected || focused) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Clip,
        )
    }
}
