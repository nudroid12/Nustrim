package app.nudroidlabs.nustrim.tv2.home

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil3.compose.AsyncImage
import kotlinx.coroutines.delay

private object Tv2HomeColors {
    val Background = Color(0xFF090A0D)
    val Surface = Color(0xFF121419)
    val SurfaceRaised = Color(0xFF1A1D24)
    val ContentPrimary = Color(0xFFF4F5F7)
    val ContentSecondary = Color(0xFFB3B7C2)
}

private const val TV2_HOME_FOCUS_MILLIS = 180
private const val TV2_HOME_FOCUS_SCALE = 1.055f
private val TV2_HOME_SMALL_RADIUS = 10.dp

/**
 * TV2 Home presentation layer.
 *
 * This file intentionally accepts primitive UI values and focus requesters
 * instead of source/session models. Data loading, playback, addon, Trakt,
 * library and updater behaviour remain owned by the existing Nustrim core.
 */
@Composable
fun Tv2FocusedBackdrop(
    artworkUrl: String,
    title: String,
    metadata: String,
    description: String,
    height: Dp,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .background(Tv2HomeColors.Background)
    ) {
        Tv2Artwork(
            url = artworkUrl,
            title = title,
            modifier = Modifier.fillMaxSize()
        )

        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Tv2HomeColors.Background.copy(alpha = 0.99f),
                            Tv2HomeColors.Background.copy(alpha = 0.93f),
                            Tv2HomeColors.Background.copy(alpha = 0.66f),
                            Tv2HomeColors.Background.copy(alpha = 0.16f),
                            Color.Transparent
                        )
                    )
                )
        )

        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Transparent,
                            Tv2HomeColors.Background.copy(alpha = 0.54f),
                            Tv2HomeColors.Background.copy(alpha = 0.94f),
                            Tv2HomeColors.Background
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .fillMaxWidth(0.44f)
                .padding(start = 38.dp, end = 18.dp, top = 48.dp)
        ) {
            Text(
                text = title,
                color = Tv2HomeColors.ContentPrimary,
                fontSize = 32.sp,
                lineHeight = 35.sp,
                fontWeight = FontWeight.Black,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            if (metadata.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = metadata,
                    color = Tv2HomeColors.ContentPrimary.copy(alpha = 0.90f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (description.isNotBlank()) {
                Spacer(Modifier.height(9.dp))
                Text(
                    text = description,
                    color = Tv2HomeColors.ContentSecondary,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.height(12.dp))
            Button(
                onClick = onOpen,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Tv2HomeColors.ContentPrimary,
                    contentColor = Tv2HomeColors.Background
                ),
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 7.dp),
                modifier = Modifier.widthIn(min = 132.dp)
            ) {
                Text(
                    text = "View Details",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * TV-only catalog card.
 *
 * Preserves Nustrim's existing remote contract:
 * 1. Focus remains portrait initially.
 * 2. A focused card expands to landscape after 3 seconds.
 * 3. Losing focus immediately cancels landscape preview.
 * 4. OK short press opens details.
 * 5. OK long press at 650 ms opens the action menu.
 * 6. Left from the first card can return focus to the TV rail.
 */
@Composable
fun Tv2PosterCard(
    itemKey: String,
    posterUrl: String,
    landscapeUrl: String,
    title: String,
    releaseInfo: String,
    reduceMotion: Boolean,
    requester: FocusRequester?,
    previousRequester: FocusRequester?,
    nextRequester: FocusRequester?,
    sidebarRequester: FocusRequester?,
    onFocused: () -> Unit,
    onLongPress: () -> Unit,
    onClick: () -> Unit
) {
    var focused by remember(itemKey) { mutableStateOf(false) }
    var landscapePreview by remember(itemKey) { mutableStateOf(false) }
    var pressStartedAtMs by remember(itemKey) { mutableStateOf(0L) }

    LaunchedEffect(focused, itemKey) {
        if (focused) {
            delay(3_000)
            if (focused) landscapePreview = true
        } else {
            landscapePreview = false
        }
    }

    val targetWidth = if (landscapePreview) 286.dp else 110.dp
    val width by animateDpAsState(
        targetValue = targetWidth,
        animationSpec = tween(durationMillis = TV2_HOME_FOCUS_MILLIS),
        label = "tv2-home-poster-width"
    )
    val scale by animateFloatAsState(
        targetValue = if (focused && !reduceMotion) TV2_HOME_FOCUS_SCALE else 1f,
        animationSpec = tween(durationMillis = TV2_HOME_FOCUS_MILLIS),
        label = "tv2-home-poster-scale"
    )
    val requesterModifier = requester?.let { Modifier.focusRequester(it) } ?: Modifier

    fun openContext() {
        landscapePreview = false
        onLongPress()
    }

    Column(
        modifier = Modifier
            .width(width)
            .then(requesterModifier)
            .zIndex(if (focused) 1f else 0f)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .onFocusChanged {
                focused = it.isFocused
                if (it.isFocused) {
                    onFocused()
                } else {
                    landscapePreview = false
                    pressStartedAtMs = 0L
                }
            }
            .onPreviewKeyEvent { event ->
                when {
                    isActivateKey(event.key) && event.type == KeyEventType.KeyDown -> {
                        if (pressStartedAtMs == 0L) {
                            pressStartedAtMs = System.currentTimeMillis()
                        }
                        true
                    }

                    isActivateKey(event.key) && event.type == KeyEventType.KeyUp -> {
                        val heldMs = if (pressStartedAtMs > 0L) {
                            System.currentTimeMillis() - pressStartedAtMs
                        } else {
                            0L
                        }
                        pressStartedAtMs = 0L
                        if (heldMs >= 650L) openContext() else onClick()
                        true
                    }

                    event.type == KeyEventType.KeyDown &&
                        event.key == Key.DirectionLeft -> {
                        when {
                            previousRequester != null -> requestFocusSafely(previousRequester)
                            sidebarRequester != null -> requestFocusSafely(sidebarRequester)
                            else -> false
                        }
                    }

                    event.type == KeyEventType.KeyDown &&
                        event.key == Key.DirectionRight &&
                        nextRequester != null -> requestFocusSafely(nextRequester)

                    else -> false
                }
            }
            .pointerInput(itemKey) {
                detectTapGestures(
                    onTap = { onClick() },
                    onLongPress = { openContext() }
                )
            }
            .focusable()
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(165.dp),
            shape = RoundedCornerShape(TV2_HOME_SMALL_RADIUS),
            border = if (focused) {
                BorderStroke(2.dp, Tv2HomeColors.ContentPrimary.copy(alpha = 0.96f))
            } else {
                null
            },
            colors = CardDefaults.cardColors(containerColor = Tv2HomeColors.SurfaceRaised)
        ) {
            Tv2Artwork(
                url = if (landscapePreview) {
                    landscapeUrl.ifBlank { posterUrl }
                } else {
                    posterUrl
                },
                title = title,
                modifier = Modifier.fillMaxSize()
            )
        }

        Spacer(Modifier.height(8.dp))
        Text(
            text = title,
            color = Tv2HomeColors.ContentPrimary,
            fontSize = 12.sp,
            fontWeight = if (focused) FontWeight.SemiBold else FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        if (releaseInfo.isNotBlank()) {
            Spacer(Modifier.height(2.dp))
            Text(
                text = releaseInfo,
                color = Tv2HomeColors.ContentSecondary,
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun Tv2Artwork(
    url: String,
    title: String,
    modifier: Modifier
) {
    if (url.isBlank()) {
        Box(
            modifier = modifier.background(
                Brush.linearGradient(
                    listOf(Tv2HomeColors.SurfaceRaised, Tv2HomeColors.Surface)
                )
            ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = title.take(1).uppercase().ifBlank { "N" },
                color = Tv2HomeColors.ContentSecondary,
                fontWeight = FontWeight.Black,
                fontSize = 30.sp
            )
        }
    } else {
        AsyncImage(
            model = url,
            contentDescription = title,
            modifier = modifier,
            contentScale = ContentScale.Crop
        )
    }
}

private fun isActivateKey(key: Key): Boolean =
    key == Key.DirectionCenter || key == Key.Enter

private fun requestFocusSafely(requester: FocusRequester?): Boolean {
    if (requester == null) return false
    return runCatching {
        requester.requestFocus()
        true
    }.getOrDefault(false)
}
