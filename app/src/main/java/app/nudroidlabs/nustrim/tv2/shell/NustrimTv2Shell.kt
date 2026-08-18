package app.nudroidlabs.nustrim.tv2.shell

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import app.nudroidlabs.nustrim.tv2.design.Tv2Colors
import app.nudroidlabs.nustrim.tv2.design.Tv2Motion
import app.nudroidlabs.nustrim.tv2.design.Tv2Shapes
import app.nudroidlabs.nustrim.tv2.design.Tv2Spacing
import app.nudroidlabs.nustrim.tv2.navigation.Tv2Destination

private tailrec fun Context.findTv2Activity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findTv2Activity()
    else -> null
}

/**
 * TV2 root shell.
 *
 * Stage 1 owns only navigation rail behaviour and the content viewport.
 * Screens are rebuilt in later stages.
 */
@Composable
fun NustrimTv2Shell(
    selectedId: String,
    onSelect: (String) -> Unit,
    content: @Composable (
        contentFocusRequestToken: Int,
        selectedRailRequester: FocusRequester
    ) -> Unit
) {
    val selected = Tv2Destination.fromId(selectedId)
    val requesters = remember {
        Tv2Destination.entries.associateWith { FocusRequester() }
    }
    val focusManager = LocalFocusManager.current
    val activity = LocalContext.current.findTv2Activity()

    var railExpanded by remember { mutableStateOf(false) }
    var contentFocusRequestToken by remember { mutableIntStateOf(1) }

    val railWidth by animateDpAsState(
        targetValue = if (railExpanded) {
            Tv2Spacing.railExpandedWidth
        } else {
            Tv2Spacing.railCompactWidth
        },
        animationSpec = tween(Tv2Motion.railDurationMs),
        label = "tv2-rail-width"
    )

    fun moveToContent() {
        railExpanded = false
        focusManager.clearFocus(force = true)
        contentFocusRequestToken += 1
    }

    BackHandler {
        if (railExpanded) {
            activity?.finishAndRemoveTask() ?: activity?.finish()
        } else {
            railExpanded = true
            requesters.getValue(selected).requestFocus()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(enabled = false) {}
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = Tv2Spacing.railCompactWidth)
        ) {
            content(
                contentFocusRequestToken,
                requesters.getValue(selected)
            )
        }

        Surface(
            modifier = Modifier
                .zIndex(20f)
                .fillMaxHeight()
                .width(railWidth),
            color = Tv2Colors.rail,

        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        horizontal = Tv2Spacing.railHorizontalPadding,
                        vertical = Tv2Spacing.railVerticalPadding
                    )
            ) {
                Tv2Brand(expanded = railExpanded)

                Spacer(Modifier.weight(1f))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(Tv2Spacing.railItemGap)
                ) {
                    Tv2Destination.entries.forEach { destination ->
                        Tv2RailItem(
                            destination = destination,
                            selected = destination == selected,
                            expanded = railExpanded,
                            requester = requesters.getValue(destination),
                            onFocused = {
                                railExpanded = true
                            },
                            onMoveRight = {
                                moveToContent()
                            },
                            onClick = {
                                if (destination != selected) {
                                    onSelect(destination.id)
                                }
                                moveToContent()
                            }
                        )
                    }
                }

                Spacer(Modifier.weight(1f))

                if (railExpanded) {
                    Text(
                        text = "Nustrim TV",
                        color = Tv2Colors.textMuted,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun Tv2Brand(expanded: Boolean) {
    if (!expanded) return

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 7.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(36.dp),
            shape = Tv2Shapes.navItem,
            color = Tv2Colors.active
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = "N",
                    color = Tv2Colors.text,
                    fontWeight = FontWeight.Black,
                    fontSize = 17.sp
                )
            }
        }

        if (expanded) {
            Spacer(Modifier.width(Tv2Spacing.iconLabelGap))
            Text(
                text = "Nustrim",
                color = Tv2Colors.text,
                fontWeight = FontWeight.SemiBold,
                fontSize = 18.sp
            )
        }
    }
}

@Composable
private fun Tv2RailItem(
    destination: Tv2Destination,
    selected: Boolean,
    expanded: Boolean,
    requester: FocusRequester,
    onFocused: () -> Unit,
    onMoveRight: () -> Unit,
    onClick: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }
Surface(
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(requester)
            .onFocusChanged {
                focused = it.isFocused
                if (it.isFocused) onFocused()
            }
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
            .clip(Tv2Shapes.navItem)
            .clickable(onClick = onClick)
            .focusable(),
        shape = Tv2Shapes.navItem,
        color = if (selected) {
            androidx.compose.ui.graphics.Color.White
        } else {
            androidx.compose.ui.graphics.Color.Transparent
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = if (expanded) Tv2Spacing.itemHorizontal else 0.dp,
                    vertical = Tv2Spacing.itemVertical
                ),
            horizontalArrangement = if (expanded) {
                Arrangement.Start
            } else {
                Arrangement.Center
            },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = destination.icon,
                contentDescription = destination.label,
                modifier = Modifier.size(24.dp),
                tint = when {
                    selected -> Tv2Colors.rail
                    focused -> Tv2Colors.focusedContent
                    else -> Tv2Colors.text
                }
            )

            if (expanded) {
                Spacer(Modifier.width(Tv2Spacing.iconLabelGap))
                Text(
                    text = destination.label,
                    color = when {
                        selected -> Tv2Colors.rail
                        focused -> Tv2Colors.focusedContent
                        else -> Tv2Colors.text
                    },
                    fontWeight = if (focused || selected) {
                        FontWeight.SemiBold
                    } else {
                        FontWeight.Normal
                    },
                    fontSize = 16.sp,
                    maxLines = 1,
                    modifier = Modifier.widthIn(min = 90.dp)
                )
            }
        }
    }
}
