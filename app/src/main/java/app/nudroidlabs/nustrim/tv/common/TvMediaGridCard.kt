package app.nudroidlabs.nustrim.tv.common

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.nudroidlabs.nustrim.tv.home.TvHomeEntry
import app.nudroidlabs.nustrim.tv.theme.TvColors
import coil3.compose.AsyncImage

@Composable
fun TvMediaGridCard(
    entry: TvHomeEntry,
    focusRequester: FocusRequester? = null,
    onFocused: (FocusRequester) -> Unit,
    onMoveLeft: (() -> Unit)? = null,
    onOpen: (TvHomeEntry) -> Unit
) {
    var focused by remember(entry.stableKey) { mutableStateOf(false) }
    val requester = remember(entry.stableKey, focusRequester) {
        focusRequester ?: FocusRequester()
    }
    val scale by animateFloatAsState(
        targetValue = if (focused) 1.045f else 1f,
        label = "tvGridCardScale"
    )

    Column(
        modifier = Modifier
            .width(142.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Surface(
            modifier = Modifier
                .width(142.dp)
                .height(206.dp)
                .focusRequester(requester)
                .onFocusChanged { state ->
                    focused = state.hasFocus
                    if (state.hasFocus) onFocused(requester)
                }
                .onPreviewKeyEvent { event ->
                    when {
                        event.type == KeyEventType.KeyDown &&
                            event.key == Key.DirectionLeft &&
                            onMoveLeft != null -> {
                            onMoveLeft()
                            true
                        }

                        event.type == KeyEventType.KeyDown &&
                            (
                                event.key == Key.DirectionCenter ||
                                    event.key == Key.Enter
                                ) -> {
                            onOpen(entry)
                            true
                        }

                        else -> false
                    }
                }
                .focusable(),
            color = TvColors.Surface,
            shape = RoundedCornerShape(11.dp),
            border = if (focused) {
                BorderStroke(2.dp, TvColors.FocusRing)
            } else {
                BorderStroke(
                    1.dp,
                    Color.White.copy(alpha = 0.08f)
                )
            }
        ) {
            val image = entry.item.posterUrl
                .takeIf { it.isNotBlank() }
                ?: entry.item.backgroundUrl

            if (image.isNotBlank()) {
                AsyncImage(
                    model = image,
                    contentDescription = entry.item.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }

        Text(
            text = entry.item.title,
            color = if (focused) {
                TvColors.TextPrimary
            } else {
                TvColors.TextSecondary
            },
            fontWeight = if (focused) {
                FontWeight.SemiBold
            } else {
                FontWeight.Medium
            },
            fontSize = 13.sp,
            lineHeight = 16.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth()
        )

        entry.item.releaseInfo
            .takeIf { it.isNotBlank() }
            ?.let { release ->
                Text(
                    text = release,
                    color = TvColors.TextSecondary.copy(alpha = 0.72f),
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
    }
}
